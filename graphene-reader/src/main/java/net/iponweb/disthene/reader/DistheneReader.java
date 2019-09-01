package net.iponweb.disthene.reader;

import net.iponweb.disthene.reader.config.DistheneReaderConfiguration;
import net.iponweb.disthene.reader.config.ThrottlingConfiguration;
import net.iponweb.disthene.reader.handler.*;
import net.iponweb.disthene.reader.server.ReaderServer;
import net.iponweb.disthene.reader.service.index.ElasticsearchIndexService;
import net.iponweb.disthene.reader.service.metric.CassandraMetricService;
import net.iponweb.disthene.reader.service.stats.StatsService;
import net.iponweb.disthene.reader.service.stats.NoopStatsService;
import net.iponweb.disthene.reader.service.stats.GraphiteStatsService;
import net.iponweb.disthene.reader.service.store.CassandraService;
import net.iponweb.disthene.reader.service.throttling.ThrottlingService;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/** @author Andrei Ivanov */
public class DistheneReader {

  private static final String DEFAULT_CONFIG_LOCATION = "/etc/disthene-reader/disthene-reader.yaml";
  private static final String DEFAULT_LOG_CONFIG_LOCATION =
      "/etc/disthene-reader/disthene-reader-log4j.xml";
  private static final String DEFAULT_THROTTLING_CONFIG_LOCATION =
      "/etc/disthene-reader/throttling.yaml";

  private static final String METRICS_PATH = "^/metrics\\/?$";
  private static final String PATHS_PATH = "^/paths\\/?$";
  private static final String METRICS_FIND_PATH = "^/metrics/find\\/?$";
  private static final String PING_PATH = "^/ping\\/?$";
  private static final String RENDER_PATH = "^/render\\/?$";
  private static final String SEARCH_PATH = "^/search\\/?$";
  private static final String PATHS_STATS_PATH = "^/path_stats\\/?$";

  private static Logger logger;

  private String configLocation;
  private String throttlingConfigLocation;
  private ReaderServer readerServer;
  private ElasticsearchIndexService elasticsearchIndexService;
  private CassandraService cassandraService;
  private CassandraMetricService cassandraMetricService;
  private StatsService statsService;
  private ThrottlingService throttlingService;

  private DistheneReader(String configLocation, String throttlingConfigLocation) {
    this.configLocation = configLocation;
    this.throttlingConfigLocation = throttlingConfigLocation;
  }

  private void run() {
    try {
      Yaml yaml = new Yaml();
      InputStream in = Files.newInputStream(Paths.get(configLocation));
      DistheneReaderConfiguration distheneReaderConfiguration =
          yaml.loadAs(in, DistheneReaderConfiguration.class);
      in.close();
      logger.info("Running with the following config: " + distheneReaderConfiguration.toString());

      ThrottlingConfiguration throttlingConfiguration;
      File file = new File(throttlingConfigLocation);
      if (file.exists() && !file.isDirectory()) {
        logger.info("Loading throttling rules");
        in = Files.newInputStream(Paths.get(throttlingConfigLocation));
        throttlingConfiguration = yaml.loadAs(in, ThrottlingConfiguration.class);
        in.close();
      } else {
        throttlingConfiguration = new ThrottlingConfiguration();
      }

      logger.debug(
          "Running with the following throttling configuration: "
              + throttlingConfiguration.toString());
      logger.info("Creating throttling");
      throttlingService = new ThrottlingService(throttlingConfiguration);

      logger.info("Creating statsService");
      statsService = createStatsService(distheneReaderConfiguration);
      logger.info("Created statsService : " + statsService.getClass().getSimpleName());

      logger.info("Creating reader");
      readerServer = new ReaderServer(distheneReaderConfiguration.getReader());

      logger.info("Creating index service");
      elasticsearchIndexService =
          new ElasticsearchIndexService(distheneReaderConfiguration.getIndex());

      logger.info("Creating C* service");
      cassandraService = new CassandraService(distheneReaderConfiguration.getStore());

      logger.info("Creating metric service");
      cassandraMetricService =
          new CassandraMetricService(
              elasticsearchIndexService,
              cassandraService,
              statsService,
              distheneReaderConfiguration);

      logger.info("Creating paths handler");
//      PathsHandler pathsHandler = new PathsHandler(elasticsearchIndexService, statsService);
//      readerServer.registerHandler(PATHS_PATH, pathsHandler);

      logger.info("Creating metrics handler");
      MetricsHandler metricsHandler = new MetricsHandler(cassandraMetricService);
      readerServer.registerHandler(METRICS_PATH, metricsHandler);

      logger.info("Creating metrics/find handler");
      readerServer.registerHandler(
          METRICS_FIND_PATH, new MetricsFindHandler(elasticsearchIndexService));

      logger.info("Creating ping handler");
      PingHandler pingHandler = new PingHandler();
      readerServer.registerHandler(PING_PATH, pingHandler);

      logger.info("Creating render handler");
      RenderHandler renderHandler =
          new RenderHandler(
              cassandraMetricService,
              elasticsearchIndexService,
              statsService,
              throttlingService,
              distheneReaderConfiguration.getReader());
      readerServer.registerHandler(RENDER_PATH, renderHandler);

      logger.info("Creating search handler");
      SearchHandler searchHandler = new SearchHandler(elasticsearchIndexService, statsService);
      readerServer.registerHandler(SEARCH_PATH, searchHandler);

      logger.info("Creating path stats handler");
      PathStatsHandler pathStatsHandler =
          new PathStatsHandler(elasticsearchIndexService, statsService);
      readerServer.registerHandler(PATHS_STATS_PATH, pathStatsHandler);

      logger.info("Starting Graphene Reader");
      readerServer.run();
      logger.info("Started Graphene Reader");

      registerShutdownHook();
    } catch (IOException e) {
      logger.error(e);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void registerShutdownHook() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              public void run() {
                logger.info("Shutting down carbon server");
                readerServer.shutdown();

                logger.info("Shutting down index service");
                elasticsearchIndexService.shutdown();

                logger.info("Shutting down C* service");
                cassandraService.shutdown();

                logger.info("Shutting down stats service");
                statsService.shutdown();

                logger.info("Shutdown complete");

                System.exit(0);
              }
            });
  }

  private StatsService createStatsService(DistheneReaderConfiguration distheneReaderConfiguration) {
    return distheneReaderConfiguration.getStats().isEnabled()
        ? new GraphiteStatsService(distheneReaderConfiguration.getStats())
        : new NoopStatsService();
  }

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("c", "config", true, "config location");
    options.addOption("l", "log-config", true, "log config location");
    options.addOption("t", "throttling-config", true, "throttling config location");

    CommandLineParser parser = new GnuParser();

    try {
      CommandLine commandLine = parser.parse(options, args);
      System.getProperties()
          .setProperty(
              "log4j.configuration",
              "file:" + commandLine.getOptionValue("l", DEFAULT_LOG_CONFIG_LOCATION));
      logger = Logger.getLogger(DistheneReader.class);

      new DistheneReader(
              commandLine.getOptionValue("c", DEFAULT_CONFIG_LOCATION),
              commandLine.getOptionValue("t", DEFAULT_THROTTLING_CONFIG_LOCATION))
          .run();

    } catch (ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Disthene", options);
    } catch (Exception e) {
      System.out.println("Start failed");
      e.printStackTrace();
    }
  }
}

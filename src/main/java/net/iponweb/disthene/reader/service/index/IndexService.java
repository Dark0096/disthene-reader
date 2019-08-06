package net.iponweb.disthene.reader.service.index;

import com.google.common.base.Joiner;
import net.iponweb.disthene.reader.config.IndexConfiguration;
import net.iponweb.disthene.reader.exceptions.TooMuchDataExpectedException;
import net.iponweb.disthene.reader.handler.response.HierarchyMetricPath;
import net.iponweb.disthene.reader.utils.WildcardUtil;
import org.apache.log4j.Logger;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.*;

/** @author Andrei Ivanov */
public class IndexService {
  static final Logger logger = Logger.getLogger(IndexService.class);

  private IndexConfiguration indexConfiguration;
  private TransportClient client;
  private Joiner joiner = Joiner.on(",").skipNulls();

  public IndexService(IndexConfiguration indexConfiguration) {
    this.indexConfiguration = indexConfiguration;

    // https://www.elastic.co/guide/en/elasticsearch/client/java-api/2.0/transport-client.html
    Settings settings =
        ImmutableSettings.settingsBuilder()
            .put("cluster.name", indexConfiguration.getName())
            .put("client.transport.sniff", true)
            .put("client.transport.ping_timeout", "5s")
            .put("client.transport.nodes_sampler_interval", "5s")
            .build();
    client = new TransportClient(settings);

    for (String node : indexConfiguration.getCluster()) {
      client.addTransportAddress(
          new InetSocketTransportAddress(node, indexConfiguration.getPort()));
    }
  }

  public List<String> getPaths(String tenant, List<String> wildcards)
      throws TooMuchDataExpectedException {
    List<String> regExs = new ArrayList<>();
    List<String> result = new ArrayList<>();

    for (String wildcard : wildcards) {
      if (WildcardUtil.isPlainPath(wildcard)) {
        result.add(wildcard);
      } else {
        regExs.add(WildcardUtil.getPathsRegExFromWildcard(wildcard));
      }
    }

    logger.debug("getPaths plain paths: " + result.size() + ", wildcard paths: " + regExs.size());

    if (regExs.size() > 0) {
      String regEx = Joiner.on("|").skipNulls().join(regExs);

      // Why secondary searchPaths to elasticsearch?
      // Reason : https://stackoverflow.com/questions/18239537/scroll-searchresponse-not-iterable-when-there-are-less-results-than-the-scrollsi
      SearchResponse response = searchPaths(regEx);

      while (response.getHits().getHits().length > 0) {
        for (SearchHit hit : response.getHits()) {
          result.add((String) hit.sourceAsMap().get("path"));
        }

        response = searchScroll(response);
      }
    }

    return result;
  }

  public Set<HierarchyMetricPath> getPathsAsHierarchyMetricPath(String tenant, String query)
      throws TooMuchDataExpectedException {
    Set<HierarchyMetricPath> hierarchyMetricPaths = new HashSet<>();
    try {
      String regEx = WildcardUtil.getPathsRegExFromWildcard(query);

      // Why secondary searchPaths to elasticsearch?
      // Reason : https://stackoverflow.com/questions/18239537/scroll-searchresponse-not-iterable-when-there-are-less-results-than-the-scrollsi
      SearchResponse response = searchPaths(regEx);

      hierarchyMetricPaths = new HashSet<>();
      while (0 < response.getHits().getHits().length) {
        for (SearchHit hit : response.getHits()) {
          hierarchyMetricPaths.add(mapToHierarchyMetricPath(hit));
        }
        response = searchScroll(response);
      }
    } catch (Exception e) {
      logger.error("Fail to get paths : " + e.getMessage(), e);
    }

    return hierarchyMetricPaths;
  }

  private SearchResponse searchPaths(String regEx) throws TooMuchDataExpectedException {
    SearchResponse response = client.prepareSearch(indexConfiguration.getIndex())
            .setTypes("path")
            .setSearchType(SearchType.SCAN)
            .setScroll(TimeValue.timeValueMinutes(1))
            .setSize(1000)
            .setQuery(QueryBuilders.regexpQuery("path", regEx))
            .execute()
            .actionGet();

    response = searchScroll(response);

    // if total hits exceeds maximum - abort right away returning empty array
    if (response.getHits().totalHits() > indexConfiguration.getMaxPaths()) {
      logger.debug("Total number of paths exceeds the limit: " + response.getHits().totalHits());
      throw new TooMuchDataExpectedException(
              "Total number of paths exceeds the limit: "
                      + response.getHits().totalHits()
                      + " (the limit is "
                      + indexConfiguration.getMaxPaths()
                      + ")");
    }
    return response;
  }

  private SearchResponse searchScroll(SearchResponse response) {
    return client.prepareSearchScroll(response.getScrollId())
            .setScroll(TimeValue.timeValueMinutes(1))
            .execute()
            .actionGet();
  }

  private HierarchyMetricPath mapToHierarchyMetricPath(SearchHit hit) {
    Map<String, Object> source = hit.sourceAsMap();

    String path = (String) source.get("path");
    Integer depth = (Integer) source.get("depth");
    Boolean leaf = (Boolean) source.get("leaf");

    return HierarchyMetricPath.of(path, depth, leaf);
  }

  public String getPathsAsJsonArray(String tenant, String wildcard)
      throws TooMuchDataExpectedException {
    String regEx = WildcardUtil.getPathsRegExFromWildcard(wildcard);

    SearchResponse response =
        client
            .prepareSearch(indexConfiguration.getIndex())
            .setScroll(new TimeValue(indexConfiguration.getTimeout()))
            .setSize(indexConfiguration.getScroll())
            .setQuery(
                QueryBuilders.filteredQuery(
                    QueryBuilders.regexpQuery("path", regEx),
                    FilterBuilders.termFilter("tenant", tenant)))
            .execute()
            .actionGet();

    // if total hits exceeds maximum - abort right away returning empty array
    if (response.getHits().totalHits() > indexConfiguration.getMaxPaths()) {
      logger.debug("Total number of paths exceeds the limit: " + response.getHits().totalHits());
      throw new TooMuchDataExpectedException(
          "Total number of paths exceeds the limit: "
              + response.getHits().totalHits()
              + " (the limit is "
              + indexConfiguration.getMaxPaths()
              + ")");
    }

    List<String> paths = new ArrayList<>();
    while (response.getHits().getHits().length > 0) {
      for (SearchHit hit : response.getHits()) {
        paths.add(hit.getSourceAsString());
      }
      response =
          client
              .prepareSearchScroll(response.getScrollId())
              .setScroll(new TimeValue(indexConfiguration.getTimeout()))
              .execute()
              .actionGet();
    }

    return "[" + joiner.join(paths) + "]";
  }

  public String getSearchPathsAsString(String tenant, String regEx, int limit) {
    SearchResponse response =
        client
            .prepareSearch(indexConfiguration.getIndex())
            .setScroll(new TimeValue(indexConfiguration.getTimeout()))
            .setSize(limit)
            .setQuery(
                QueryBuilders.filteredQuery(
                    QueryBuilders.regexpQuery("path", regEx),
                    FilterBuilders.termFilter("tenant", tenant)))
            .addField("path")
            .execute()
            .actionGet();

    List<String> paths = new ArrayList<>();
    for (SearchHit hit : response.getHits()) {
      paths.add((String) hit.field("path").getValue());
    }

    return Joiner.on(",").skipNulls().join(paths);
  }

  public String getPathsWithStats(String tenant, String wildcard)
      throws TooMuchDataExpectedException {
    String regEx = WildcardUtil.getPathsRegExFromWildcard(wildcard);

    SearchResponse response =
        client
            .prepareSearch(indexConfiguration.getIndex())
            .setScroll(new TimeValue(indexConfiguration.getTimeout()))
            .setSize(indexConfiguration.getScroll())
            .setQuery(
                QueryBuilders.filteredQuery(
                    QueryBuilders.regexpQuery("path", regEx),
                    FilterBuilders.termFilter("tenant", tenant)))
            .addField("path")
            .execute()
            .actionGet();

    // if total hits exceeds maximum - abort right away returning empty array
    if (response.getHits().totalHits() > indexConfiguration.getMaxPaths()) {
      logger.debug("Total number of paths exceeds the limit: " + response.getHits().totalHits());
      throw new TooMuchDataExpectedException(
          "Total number of paths exceeds the limit: "
              + response.getHits().totalHits()
              + " (the limit is "
              + indexConfiguration.getMaxPaths()
              + ")");
    }

    List<String> paths = new ArrayList<>();
    while (response.getHits().getHits().length > 0) {
      for (SearchHit hit : response.getHits()) {
        paths.add(String.valueOf(hit.field("path").getValue()));
      }
      response =
          client
              .prepareSearchScroll(response.getScrollId())
              .setScroll(new TimeValue(indexConfiguration.getTimeout()))
              .execute()
              .actionGet();
    }

    Collections.sort(paths);

    // we got the paths. Now let's get the counts
    List<String> result = new ArrayList<>();
    for (String path : paths) {
      CountResponse countResponse =
          client
              .prepareCount(indexConfiguration.getIndex())
              .setQuery(
                  QueryBuilders.filteredQuery(
                      QueryBuilders.regexpQuery("path", path + "\\..*"),
                      FilterBuilders.boolFilter()
                          .must(FilterBuilders.termFilter("tenant", tenant))
                          .must(FilterBuilders.termFilter("leaf", true))))
              .execute()
              .actionGet();
      long count = countResponse.getCount();
      result.add("{\"path\": \"" + path + "\",\"count\":" + countResponse.getCount() + "}");
    }

    return "[" + joiner.join(result) + "]";
  }

  public void shutdown() {
    client.close();
  }
}

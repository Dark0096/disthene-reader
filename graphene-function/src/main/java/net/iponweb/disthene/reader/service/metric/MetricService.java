package net.iponweb.disthene.reader.service.metric;

import net.iponweb.disthene.reader.beans.TimeSeries;
import net.iponweb.disthene.reader.config.Rollup;
import net.iponweb.disthene.reader.exceptions.TooMuchDataExpectedException;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface MetricService {

  String getMetricsAsJson(String tenant, List<String> wildcards, long from, long to) throws ExecutionException, InterruptedException, TooMuchDataExpectedException;

  List<TimeSeries> getMetricsAsList(String tenant, List<String> wildcards, long from, long to) throws ExecutionException, InterruptedException, TooMuchDataExpectedException;

  Rollup getRollup(long from);
}

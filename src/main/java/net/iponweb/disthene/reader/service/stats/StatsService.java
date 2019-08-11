package net.iponweb.disthene.reader.service.stats;

public interface StatsService {

  void incRenderRequests(String tenant);

  void incRenderPointsRead(String tenant, int inc);

  void incRenderPathsRead(String tenant, int inc);

  void incPathsRequests(String tenant);

  void incThrottleTime(String tenant, double value);

  void incTimedOutRequests(String tenant);

  void shutdown();

}

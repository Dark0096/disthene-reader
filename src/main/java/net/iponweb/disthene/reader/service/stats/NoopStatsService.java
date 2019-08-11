package net.iponweb.disthene.reader.service.stats;

public class NoopStatsService implements StatsService {

  @Override
  public void incRenderRequests(String tenant) {
    // noop
  }

  @Override
  public void incRenderPointsRead(String tenant, int inc) {
    // noop
  }

  @Override
  public void incRenderPathsRead(String tenant, int inc) {
    // noop
  }

  @Override
  public void incPathsRequests(String tenant) {
    // noop
  }

  @Override
  public void incThrottleTime(String tenant, double value) {
    // noop
  }

  @Override
  public void incTimedOutRequests(String tenant) {
    // noop
  }

  @Override
  public void shutdown() {
    // noop
  }
}

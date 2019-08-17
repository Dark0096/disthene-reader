package net.iponweb.disthene.reader.service.index;

import net.iponweb.disthene.reader.exceptions.TooMuchDataExpectedException;
import net.iponweb.disthene.reader.handler.response.HierarchyMetricPath;

import java.util.List;
import java.util.Set;

public interface IndexService {
  Set<String> getPaths(String tenant, List<String> wildcards) throws TooMuchDataExpectedException;

  Set<HierarchyMetricPath> getPathsAsHierarchyMetricPath(String tenant, String query) throws TooMuchDataExpectedException;
}

package net.iponweb.disthene.reader.handler.response;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class HierarchyMetricPathTest {

  @Test
  public void shouldInitialize1() {
    // when
    HierarchyMetricPath hierarchyMetricPath = HierarchyMetricPath.of("a.b.c", 3, true);

    // then
    assertThat(hierarchyMetricPath.getId(), is("a.b.c"));
    assertThat(hierarchyMetricPath.getAllowChildren(), is(0));
    assertThat(hierarchyMetricPath.getExpandable(), is(0));
    assertThat(hierarchyMetricPath.getLeaf(), is(0));
    assertThat(hierarchyMetricPath.getText(), is("c"));
  }

  @Test
  public void shouldInitialize2() {
    // when
    HierarchyMetricPath hierarchyMetricPath = HierarchyMetricPath.of("a.b", 2, false);

    // then
    assertThat(hierarchyMetricPath.getId(), is("a.b"));
    assertThat(hierarchyMetricPath.getAllowChildren(), is(1));
    assertThat(hierarchyMetricPath.getExpandable(), is(1));
    assertThat(hierarchyMetricPath.getLeaf(), is(1));
    assertThat(hierarchyMetricPath.getText(), is("b"));
  }

  @Test
  public void shouldSameIfPathAndDepthSame() {
    assertEquals(HierarchyMetricPath.of("a.b.c", 3, true), HierarchyMetricPath.of("a.b.c", 3, true));

    assertNotSame(HierarchyMetricPath.of("a.b.c", 3, true), HierarchyMetricPath.of("a.b", 2, false));
  }

}
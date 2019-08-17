package net.iponweb.disthene.reader.utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WildcardUtilTest {

  @Test
  public void shouldReturnEncodedMetricPath() {
    assertThat(WildcardUtil.getPathsRegExFromWildcard("hosts.service.role"), is("hosts\\.service\\.role"));
    assertThat(WildcardUtil.getPathsRegExFromWildcard("hosts.service.role.*"), is("hosts\\.service\\.role\\.[^\\.]*"));
  }
}

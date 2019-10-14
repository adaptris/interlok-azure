package com.adaptris.interlok.azure.cosmosdb;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class ResourceTypeHelperTest extends ResourceTypeHelper {


  @Test
  public void testService() throws Exception {
    assertNotNull(getResourceTypes());
    for (String s : getResourceTypes()) {
      System.err.println(s);
    }
  }
}

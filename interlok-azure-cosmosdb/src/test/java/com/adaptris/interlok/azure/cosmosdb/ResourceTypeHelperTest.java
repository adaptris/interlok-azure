package com.adaptris.interlok.azure.cosmosdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;

import org.junit.jupiter.api.Test;

public class ResourceTypeHelperTest extends ResourceTypeHelper {

  @Test
  public void testResourceTypes() throws Exception {
    assertNotNull(getResourceTypes());
    for (String s : getResourceTypes()) {
      System.err.println(s);
    }
  }

  @Test
  public void testResourceType() throws Exception {
    assertEquals("", getResourceType(new URL("https://azuredb.microsoft.com/")));
    assertEquals("dbs", getResourceType(new URL("https://azuredb.microsoft.com/dbs")));
    assertEquals("docs", getResourceType(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs")));
    assertEquals("docs", getResourceType(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/")));
    assertEquals("docs", getResourceType(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/MyName")));
    assertEquals("colls", getResourceType(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls")));
  }

  @Test
  public void testGetResourceID() throws Exception {
    assertEquals("", getResourceID(new URL("https://azuredb.microsoft.com/")));
    assertEquals("", getResourceID(new URL("https://azuredb.microsoft.com/dbs")));
    assertEquals("dbs/tempdb/colls/tempcoll", getResourceID(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs")));
    assertEquals("dbs/tempdb/colls/tempcoll", getResourceID(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/")));
    assertEquals("dbs/tempdb/colls/tempcoll/docs/MyName",
        getResourceID(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/MyName")));
    assertEquals("dbs/tempdb", getResourceID(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls")));
  }

}

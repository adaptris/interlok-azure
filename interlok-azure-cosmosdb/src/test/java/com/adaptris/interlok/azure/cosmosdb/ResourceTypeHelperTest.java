package com.adaptris.interlok.azure.cosmosdb;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    assertEquals("dbs/tempdb/colls/tempcoll",
        getResourceID(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs")));
    assertEquals("dbs/tempdb/colls/tempcoll",
        getResourceID(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/")));
    assertEquals("dbs/tempdb/colls/tempcoll/docs/MyName",
        getResourceID(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/MyName")));
    assertEquals("dbs/tempdb", getResourceID(new URL("https://azuredb.microsoft.com/dbs/tempdb/colls")));
  }

}

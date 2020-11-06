package com.adaptris.interlok.azure.cosmosdb;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.ServiceException;
import org.junit.Test;

import java.net.URLDecoder;

import static com.adaptris.core.ServiceCase.execute;
import static com.adaptris.interlok.azure.cosmosdb.CosmosAuthorizationHeaderTest.DUMMY_MASTER_KEY;
import static org.junit.Assert.assertTrue;

public class CosmosAuthorizationHeaderFromUrlTest {

  @Test
  public void testService() throws Exception {
    CosmosAuthorizationHeaderFromUrl service =
        new CosmosAuthorizationHeaderFromUrl()
            .withCosmosEndpointUrl("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs")
            .withHttpVerb("PUT").withTargetKey("AuthToken").withMasterKey(DUMMY_MASTER_KEY);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    execute(service, msg);
    assertTrue(msg.headersContainsKey("x-ms-date"));
    assertTrue(msg.headersContainsKey("AuthToken"));
    String authToken = msg.getMetadataValue("AuthToken");
    assertTrue(authToken.startsWith("type%3D"));
    assertTrue(URLDecoder.decode(authToken, "UTF-8").startsWith("type="));
  }

  @Test(expected = ServiceException.class)
  public void testService_Exception() throws Exception {
    CosmosAuthorizationHeaderFromUrl service =
        new CosmosAuthorizationHeaderFromUrl().withCosmosEndpointUrl("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs")
            .withHttpVerb("PUT").withTargetKey("AuthToken").withMasterKey("PW:XXX");
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();

    execute(service, msg);
  }
}

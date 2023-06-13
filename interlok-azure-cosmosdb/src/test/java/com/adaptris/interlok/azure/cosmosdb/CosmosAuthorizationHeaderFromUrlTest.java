package com.adaptris.interlok.azure.cosmosdb;

import static com.adaptris.interlok.azure.cosmosdb.CosmosAuthorizationHeaderTest.DUMMY_MASTER_KEY;
import static com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase.execute;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;

import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.ServiceException;

public class CosmosAuthorizationHeaderFromUrlTest {

  @Test
  public void testService() throws Exception {
    CosmosAuthorizationHeaderFromUrl service = new CosmosAuthorizationHeaderFromUrl()
        .withCosmosEndpointUrl("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs").withHttpVerb("PUT")
        .withTargetKey("AuthToken").withMasterKey(DUMMY_MASTER_KEY);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    execute(service, msg);
    assertTrue(msg.headersContainsKey("x-ms-date"));
    assertTrue(msg.headersContainsKey("AuthToken"));
    String authToken = msg.getMetadataValue("AuthToken");
    assertTrue(authToken.startsWith("type%3D"));
    assertTrue(URLDecoder.decode(authToken, "UTF-8").startsWith("type="));
  }

  @Test
  public void testService_Exception() throws Exception {
    CosmosAuthorizationHeaderFromUrl service = new CosmosAuthorizationHeaderFromUrl()
        .withCosmosEndpointUrl("https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs").withHttpVerb("PUT")
        .withTargetKey("AuthToken").withMasterKey("PW:XXX");
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();

    assertThrows(ServiceException.class, () -> execute(service, msg));
  }

}

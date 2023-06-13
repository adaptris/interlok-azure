package com.adaptris.interlok.azure.cosmosdb;

import static com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase.execute;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.ServiceException;

public class CosmosAuthorizationHeaderTest {

  public static final String DUMMY_MASTER_KEY = Base64.getEncoder().encodeToString("my-master-key".getBytes(StandardCharsets.UTF_8));

  @Test
  public void testService() throws Exception {
    CosmosAuthorizationHeader service = new CosmosAuthorizationHeader().withResourceId("dbs/MyDatabase/colls/MyCollection")
        .withResourceType("colls").withMasterKey(DUMMY_MASTER_KEY).withHttpVerb("PUT").withTargetKey("AuthToken");
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    execute(service, msg);
    assertTrue(msg.headersContainsKey("x-ms-date"));
    assertTrue(msg.getMetadataValue("x-ms-date").endsWith("GMT"));
    assertTrue(msg.headersContainsKey("AuthToken"));
    String authToken = msg.getMetadataValue("AuthToken");
    assertTrue(authToken.startsWith("type%3D"));
    assertTrue(URLDecoder.decode(authToken, "UTF-8").startsWith("type="));
  }

  @Test
  public void testService_Exception() throws Exception {
    CosmosAuthorizationHeader service = new CosmosAuthorizationHeader().withResourceId("dbs/MyDatabase/colls/MyCollection")
        .withResourceType("colls").withMasterKey("PW:XXX").withHttpVerb("PUT").withTargetKey("AuthToken");
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();

    assertThrows(ServiceException.class, () -> execute(service, msg));
  }

}

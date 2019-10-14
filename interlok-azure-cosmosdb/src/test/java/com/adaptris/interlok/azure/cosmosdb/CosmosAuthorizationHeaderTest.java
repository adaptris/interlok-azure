package com.adaptris.interlok.azure.cosmosdb;

import static com.adaptris.core.ServiceCase.execute;
import static org.junit.Assert.assertTrue;
import java.net.URLDecoder;
import org.junit.Test;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.ServiceException;

public class CosmosAuthorizationHeaderTest {


  @Test
  public void testService() throws Exception {
    CosmosAuthorizationHeader service =
        new CosmosAuthorizationHeader().withMasterKey("my-master-key (could be encoded)")
            .withResourceId("dbs/MyDatabase/colls/MyCollection").withResourceType("colls")
            .withHttpVerb("PUT").withTargetKey("AuthToken");
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
    CosmosAuthorizationHeader service =
        new CosmosAuthorizationHeader().withMasterKey("PW:XXX")
            .withResourceId("dbs/MyDatabase/colls/MyCollection").withResourceType("colls")
            .withHttpVerb("PUT").withTargetKey("AuthToken");
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();

    execute(service, msg);
  }
}

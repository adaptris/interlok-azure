package com.adaptris.interlok.azure.cosmosdb;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.message.BasicHttpRequest;
import org.junit.jupiter.api.Test;

public class CosmosAuthorizationInterceptorTest {

  @Test
  public void testInterceptor() throws Exception {

    CosmosAuthorizationInterceptor builder = new CosmosAuthorizationInterceptor();
    builder.setMasterKey("master");

    HttpRequestInterceptor interceptor = builder.build();

    HttpRequest request = new BasicHttpRequest("GET", "https://www.example.com/");

    interceptor.process(request, null);
  }

}

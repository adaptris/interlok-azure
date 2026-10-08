package com.adaptris.interlok.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.sun.net.httpserver.HttpServer;

public class AzureHttpClientTest {

  @Test
  public void testNettyHttpClient() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      byte[] body = "azure-http-client".getBytes(StandardCharsets.UTF_8);
      try {
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
      } finally {
        exchange.close();
      }
    });
    server.start();
    try {
      HttpClient client = new NettyAsyncHttpClientBuilder().build();
      HttpRequest request = new HttpRequest(HttpMethod.GET,
          "http://127.0.0.1:" + server.getAddress().getPort() + "/");
      try (HttpResponse response = client.send(request).block(Duration.ofSeconds(10))) {
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("azure-http-client", response.getBodyAsString().block(Duration.ofSeconds(10)));
      }
    } finally {
      server.stop(0);
    }
  }
}

package com.adaptris.interlok.azure.cosmosdb;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.http.apache.request.RequestInterceptorBuilder;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.security.password.Password;
import com.microsoft.azure.documentdb.internal.BaseAuthorizationTokenProvider;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import javax.validation.constraints.NotBlank;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.adaptris.interlok.azure.cosmosdb.CosmosAuthorizationHeader.DEFAULT_DATE_FORMAT;
import static com.adaptris.interlok.azure.cosmosdb.CosmosAuthorizationHeader.DEFAULT_METADATA_KEY;
import static com.adaptris.interlok.azure.cosmosdb.CosmosAuthorizationHeader.DEFAULT_TIMEZONE;
import static com.adaptris.interlok.azure.cosmosdb.CosmosAuthorizationHeader.X_MS_DATE;

@NoArgsConstructor
@XStreamAlias("cosmosdb-authorization-interceptor")
@ComponentProfile(summary = "Builds an authorization header for Azure CosmosDB", since = "3.9.2", tag = "azure,cosmosdb,cosmos,interceptor")
@DisplayOrder(order = {"masterKey", "targetKey"})
public class CosmosAuthorizationInterceptor implements HttpRequestInterceptor, RequestInterceptorBuilder {

  /**
   * Your master key token.
   *
   */
  @Getter
  @Setter
  @NonNull
  @NotBlank
  @InputFieldHint(style = "password", external = true, expression = true)
  private String masterKey;

  @Override
  public HttpRequestInterceptor build() {
    return this;
  }

  @Override
  public void process(HttpRequest request, HttpContext context) throws HttpException {
    try {

      String now = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).format(ZonedDateTime.now(ZoneId.of(DEFAULT_TIMEZONE)));

      URL url = new URL(request.getRequestLine().getUri());
      String resourceId = ResourceTypeHelper.getResourceID(url);
      String resourceType = ResourceTypeHelper.getResourceType(url);

      Map<String, String> headers = new HashMap<>();
      headers.put(CosmosAuthorizationHeaderImpl.X_MS_DATE, now);
      String masterKey = Password.decode(ExternalResolver.resolve(getMasterKey()));
      BaseAuthorizationTokenProvider provider = new BaseAuthorizationTokenProvider(masterKey, null);
      String header = provider.generateKeyAuthorizationSignature(request.getRequestLine().getMethod(), resourceId, resourceType, headers);

      request.addHeader(DEFAULT_METADATA_KEY, URLEncoder.encode(header, StandardCharsets.UTF_8.name()));
      request.addHeader(X_MS_DATE, now);

    } catch (Exception e) {
      throw new HttpException("Could not process HTTP intercept as expected!", e);
    }
  }
}

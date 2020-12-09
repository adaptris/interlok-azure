package com.adaptris.interlok.azure.cosmosdb;

import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceException;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.interlok.util.Args;
import com.adaptris.security.exc.PasswordException;
import com.adaptris.security.password.Password;
import com.adaptris.validation.constraints.UrlExpression;
import com.microsoft.azure.documentdb.internal.BaseAuthorizationTokenProvider;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.adaptris.interlok.azure.cosmosdb.CosmosAuthorizationHeader.DEFAULT_DATE_FORMAT;

/**
 * Builds an authorization header for Azure CosmosDB from a URL.
 * 
 * <p>
 * This differs from {@link CosmosAuthorizationHeader} in that it parses the URL for an appropriate ResourceID and ResourceType.
 * Once it has figured out an appropriate resourceID and ResourceType it builds an instance of {@link CosmosAuthorizationHeader} and
 * executes it. <strong>Note that if the URL convention changes for Azure CosmosDB, then this service may well generate an incorrect
 * ResourceType/ResourceID</strong>; you may still need to use {@link CosmosAuthorizationHeader} directly.
 * </p>
 * <p>
 * Given this example list of URLS; then then corresponding ResourceType / ResourceIDs will be generated. Essentially if the number
 * of path fragments is {@code even} then the ResourceID is full path, and the ResourceType is the penultimate fragment; if the
 * number of path fragments is {@code odd} then the ResourceID is everything up to the last {@code /} and the ResoucrceType is the
 * last fragment.
 * <table>
 * <th>URL example</th>
 * <th>ResourceType</th>
 * <th>ResourceID</th>
 * <tr>
 * <td>https://azuredb.microsoft.com/</td>
 * <td>{@code ""} (the service will fail; since ResourceType must be set)</td>
 * <td>{@code ""}</td>
 * </tr>
 * <tr>
 * <td>https://azuredb.microsoft.com/dbs</td>
 * <td>{@code dbs}</td>
 * <td>{@code ""}</td>
 * </tr>
 * <tr>
 * <td>https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs</td>
 * <td>{@code docs}</td>
 * <td>{@code dbs/tmpdb/colls/tempcoll}</td>
 * </tr>
 * <tr>
 * <td>https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/MyName</td>
 * <td>{@code docs}</td>
 * <td>{@code dbs/tempdb/colls/tempcoll/docs/MyName}</td>
 * </tr>
 * <tr>
 * <td>https://azuredb.microsoft.com/dbs/tempdb/colls</td>
 * <td>{@code colls}</td>
 * <td>{@code dbs/tempdb}</td>
 * </tr>
 * </table>
 * 
 * @config cosmosdb-authorization-header-from-url
 */
@NoArgsConstructor
@XStreamAlias("cosmosdb-authorization-header-from-url")
@ComponentProfile(summary = "Builds an authorization header for Azure CosmosDB", since = "3.9.2", tag = "azure,cosmosdb,cosmos")
@DisplayOrder(order = {"masterKey", "httpVerb", "cosmosEndpoint", "targetKey"})
public class CosmosAuthorizationHeaderFromUrl extends CosmosAuthorizationHeaderImpl implements HttpRequestInterceptor
{

  /**
   * The Cosmos URL Endpoint that you will be hitting with your REST request.
   * 
   */
  @Getter
  @Setter
  @NonNull
  @NotBlank
  @InputFieldHint(expression = true)
  @UrlExpression
  private String cosmosEndpointUrl;


  @Override
  public void prepare() throws CoreException {
    super.prepare();
    Args.notBlank(getCosmosEndpointUrl(), "cosmos-endpoint");
  }

  @Override
  public void doService(AdaptrisMessage msg) throws ServiceException {
    try {
      buildAndExecute(msg);
    } catch (Exception e) {
      throw ExceptionHelper.wrapServiceException(e);
    }
  }

  private void buildAndExecute(AdaptrisMessage msg) throws Exception {
    URL url = new URL(msg.resolve(getCosmosEndpointUrl()));
    String resourceID = ResourceTypeHelper.getResourceID(url);
    String resourceType = ResourceTypeHelper.getResourceType(url);
    CosmosAuthorizationHeader service = new CosmosAuthorizationHeader().withResourceId(resourceID).withResourceType(resourceType)
        .withHttpVerb(getHttpVerb()).withMasterKey(getMasterKey()).withTargetKey(getTargetKey());
    try {
      LifecycleHelper.initAndStart(service, false);
      service.doService(msg);
    } finally {
      LifecycleHelper.stopAndClose(service, false);
    }
  }


  public CosmosAuthorizationHeaderFromUrl withCosmosEndpointUrl(String s) {
    setCosmosEndpointUrl(s);
    return this;
  }

  @Override
  public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
    try {

      String now = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).format(ZonedDateTime.now(ZoneId.of(CosmosAuthorizationHeader.DEFAULT_TIMEZONE)));

      URL url = new URL(getCosmosEndpointUrl());
      String resourceId = ResourceTypeHelper.getResourceID(url);
      String resourceType = ResourceTypeHelper.getResourceType(url);

      Map<String, String> headers = new HashMap<>();
      headers.put(X_MS_DATE, now);
      String masterKey = Password.decode(ExternalResolver.resolve(getMasterKey()));
      BaseAuthorizationTokenProvider provider = new BaseAuthorizationTokenProvider(masterKey, null);
      String header = provider.generateKeyAuthorizationSignature(getHttpVerb(), resourceId, resourceType, headers);

      request.addHeader(targetKey(), URLEncoder.encode(header, StandardCharsets.UTF_8.name()));
      request.addHeader(X_MS_DATE, now);

    } catch (PasswordException e) {
      log.error("Could not decode password", e);
    }
  }
}

package com.adaptris.interlok.azure.cosmosdb;

import java.net.URL;
import javax.validation.constraints.NotBlank;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceException;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.util.Args;
import com.adaptris.validation.constraints.UrlExpression;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

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
public class CosmosAuthorizationHeaderFromUrl extends CosmosAuthorizationHeaderImpl {

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

}

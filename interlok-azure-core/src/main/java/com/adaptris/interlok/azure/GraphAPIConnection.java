package com.adaptris.interlok.azure;

import java.util.Collections;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.CoreException;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Azure connection to use the Graph API.
 */
@XStreamAlias("azure-graph-api-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to an Azure tenant and access the Graph API", tag = "connections,azure,graph api,graph")
@DisplayOrder(order = { "applicationId", "tenantId", "clientSecret" })
public class GraphAPIConnection extends AzureConnection<GraphServiceClient<?>> {

  // TODO Should we let the user the ability to change the scope?
  private static final String SCOPE = "https://graph.microsoft.com/.default";

  private transient TokenCredentialAuthProvider tokenCredentialAuthProvider;
  private transient GraphServiceClient<?> clientConnection;

  @Override
  protected void initConnection() throws CoreException {
    try {
      ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
          .clientId(applicationId)
          .clientSecret(clientSecret())
          .tenantId(getTenantId())
          .build();
      tokenCredentialAuthProvider = new TokenCredentialAuthProvider(Collections.singletonList(SCOPE), clientSecretCredential);
    } catch (Exception e) {
      log.error("Could not identify Azure application or tenant", e);
      throw new CoreException(e);
    }
  }

  @Override
  protected void startConnection() throws CoreException {
    clientConnection = GraphServiceClient.builder().authenticationProvider(tokenCredentialAuthProvider).buildClient();
  }

  @Override
  public GraphServiceClient<?> getClientConnection() {
    return clientConnection;
  }

}

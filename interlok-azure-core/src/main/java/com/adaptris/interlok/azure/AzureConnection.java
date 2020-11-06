package com.adaptris.interlok.azure;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisConnectionImp;
import com.adaptris.core.CoreException;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientSecret;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.util.Collections;

@XStreamAlias("azure-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to an Azure tenant", tag = "connections,azure")
@DisplayOrder(order = { "applicationId", "tenantId", "clientSecret" })
public class AzureConnection extends AdaptrisConnectionImp
{
  private static final String SCOPE = "https://graph.microsoft.com/.default";

  @Getter
  @Setter
  @NotBlank
  private String applicationId;

  @Getter
  @Setter
  @NotBlank
  private String tenantId;

  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(style = "PASSWORD", external = true)
  private String clientSecret;

  @Getter
  private transient ConfidentialClientApplication confidentialClientApplication;

  @Getter
  private transient ClientSecretCredential clientSecretCredential;

  @Getter
  private transient String accessToken;

  @Override
  protected void prepareConnection()
  {
    /* do nothing */
  }

  /**
   * Initialise the underlying connection.
   *
   * @throws CoreException wrapping any exception.
   */
  @Override
  protected void initConnection() throws CoreException
  {
    try
    {
      confidentialClientApplication = ConfidentialClientApplication.builder(applicationId,
          ClientCredentialFactory.createFromSecret(clientSecret()))
          .authority(tenant())
          .build();

      /* For some reason these are different; not yet sure how that's going to affect things later... */
      clientSecretCredential = new ClientSecretCredentialBuilder()
          .clientId(applicationId)
          .clientSecret(clientSecret())
          .tenantId(tenantId)
          .build();
    }
    catch (Exception e)
    {
      log.error("Could not identify Azure application or tenant", e);
      throw new CoreException(e);
    }
  }

  /**
   * Start the underlying connection.
   *
   * @throws CoreException wrapping any exception.
   */
  @Override
  protected void startConnection() throws CoreException
  {
    try
    {
      IAuthenticationResult iAuthResult = confidentialClientApplication.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE)).build()).join();
      accessToken = iAuthResult.accessToken();
    }
    catch (Exception e)
    {
      log.error("Could not acquire access token", e);
      throw new CoreException(e);
    }
  }

  /**
   * Stop the underlying connection.
   */
  @Override
  protected void stopConnection()
  {
    /* do nothing */
  }

  /**
   * Close the underlying connection.
   */
  @Override
  protected void closeConnection()
  {
    /* do nothing */
  }

  public IGraphServiceClient getClient()
  {
    return GraphServiceClient.builder().authenticationProvider(request -> request.addHeader("Authorization", "Bearer " + accessToken)).buildClient();
  }

  private String tenant()
  {
    return String.format("https://login.microsoftonline.com/%s", tenantId);
  }

  private String clientSecret()
  {
    return ExternalResolver.resolve(clientSecret);
  }
}

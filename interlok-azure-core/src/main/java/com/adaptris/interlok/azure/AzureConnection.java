package com.adaptris.interlok.azure;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisConnectionImp;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * Base Azure connection.
 */
@XStreamAlias("azure-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to an Azure tenant", tag = "connections,azure")
@DisplayOrder(order = { "applicationId", "tenantId", "clientSecret" })
public abstract class AzureConnection<C> extends AdaptrisConnectionImp
{
  /**
   * The ID of the Azure application.
   */
  @Getter
  @Setter
  @NotBlank
  protected String applicationId;

  /**
   * The tenant ID.
   */
  @Getter
  @Setter
  @NotBlank
  protected String tenantId;

  /**
   * The application client secret.
   */
  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(style = "PASSWORD", external = true)
  protected String clientSecret;

  public abstract C getClientConnection();

  /**
   * {@inheritDoc}.
   */
  @Override
  protected void prepareConnection()
  {
    /* do nothing */
  }

  /**
   * {@inheritDoc}.
   */
  /**
   * Stop the underlying connection.
   */
  @Override
  protected void stopConnection()
  {
    /* do nothing */
  }

  /**
   * {@inheritDoc}.
   */
  /**
   * Close the underlying connection.
   */
  @Override
  protected void closeConnection()
  {
    /* do nothing */
  }

  protected String tenant()
  {
    return String.format("https://login.microsoftonline.com/%s", tenantId);
  }

  protected String clientSecret()
  {
    return ExternalResolver.resolve(clientSecret);
  }
}

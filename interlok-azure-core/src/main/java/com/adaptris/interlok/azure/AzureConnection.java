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

@XStreamAlias("azure-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to an Azure tenant", tag = "connections,azure")
@DisplayOrder(order = { "applicationId", "tenantId", "clientSecret" })
public abstract class AzureConnection<C> extends AdaptrisConnectionImp
{
  @Getter
  @Setter
  @NotBlank
  protected String applicationId;

  @Getter
  @Setter
  @NotBlank
  protected String tenantId;

  @Getter
  @Setter
  @NotBlank
  @InputFieldHint(style = "PASSWORD", external = true)
  protected String clientSecret;

  public abstract C getClientConnection();

  @Override
  protected void prepareConnection()
  {
    /* do nothing */
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

  protected String tenant()
  {
    return String.format("https://login.microsoftonline.com/%s", tenantId);
  }

  protected String clientSecret()
  {
    return ExternalResolver.resolve(clientSecret);
  }
}

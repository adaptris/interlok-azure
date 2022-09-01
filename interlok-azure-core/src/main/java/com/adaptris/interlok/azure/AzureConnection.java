package com.adaptris.interlok.azure;

import javax.validation.constraints.NotBlank;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisConnectionImp;
import com.adaptris.core.CoreException;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Getter;
import lombok.Setter;

/**
 * Base Azure connection.
 */
@XStreamAlias("azure-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to an Azure tenant", tag = "connections,azure")
public abstract class AzureConnection<C> extends AdaptrisConnectionImp {
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
  protected void prepareConnection() {
    /* do nothing */
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  protected void stopConnection() {
    /* do nothing */
  }

  /**
   * {@inheritDoc}.
   */
  /**
   * Close the underlying connection.
   */
  @Override
  protected void closeConnection() {
    /* do nothing */
  }

  protected String clientSecret() {
    return ExternalResolver.resolve(clientSecret);
  }
}

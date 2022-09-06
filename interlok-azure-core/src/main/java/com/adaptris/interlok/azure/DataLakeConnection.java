package com.adaptris.interlok.azure;

import javax.validation.constraints.NotBlank;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.CoreException;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Getter;
import lombok.Setter;

@XStreamAlias("azure-data-lake-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to an Azure tenant and access the Data Lake", tag = "connections,azure,data lake,data,lake")
@DisplayOrder(order = { "applicationId", "tenantId", "clientSecret" })
public class DataLakeConnection extends AzureConnection<DataLakeServiceClient> {
  /**
   * The Azure account to use to access the Data Lake.
   */
  @Getter
  @Setter
  @NotBlank
  private String account;

  private transient ClientSecretCredential clientSecretCredential;
  private transient DataLakeServiceClient clientConnection;

  /**
   * Initialise the underlying connection.
   *
   * @throws CoreException
   *           wrapping any exception.
   */
  @Override
  protected void initConnection() throws CoreException {
    try {
      clientSecretCredential = new ClientSecretCredentialBuilder().clientId(applicationId).clientSecret(clientSecret())
          .tenantId(getTenantId()).build();
    } catch (Exception e) {
      log.error("Could not identify Azure application or tenant", e);
      throw new CoreException(e);
    }
  }

  /**
   * Start the underlying connection.
   *
   * @throws CoreException
   *           wrapping any exception.
   */
  @Override
  protected void startConnection() {
    clientConnection = new DataLakeServiceClientBuilder().credential(clientSecretCredential)
        .endpoint(String.format("https://%s.dfs.core.windows.net", account)).buildClient();
  }

  @Override
  public DataLakeServiceClient getClientConnection() {
    // Should we create the client in startConnection?
    return clientConnection;
  }
}

package com.adaptris.interlok.azure;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.core.CoreException;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@XStreamAlias("data-lake-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to an Azure tenant and access the Data Lake", tag = "connections,azure,data lake,data,lake")
public class DataLakeConnection extends AzureConnection<DataLakeServiceClient>
{
	@Getter
	@Setter
	@NotBlank
	private String account;

	private transient ClientSecretCredential clientSecretCredential;

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
	protected void startConnection()
	{
		/* do nothing */
	}

	@Override
	public DataLakeServiceClient getClientConnection()
	{
		return new DataLakeServiceClientBuilder().credential(clientSecretCredential).endpoint(String.format("https://%s.dfs.core.windows.net", account)).buildClient();
	}
}

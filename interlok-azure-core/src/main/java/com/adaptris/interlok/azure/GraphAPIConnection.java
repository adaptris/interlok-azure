package com.adaptris.interlok.azure;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.core.CoreException;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Collections;

@XStreamAlias("graph-api-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to an Azure tenant and access the Graph API", tag = "connections,azure,graph api,graph")
public class GraphAPIConnection extends AzureConnection<IGraphServiceClient>
{
	private static final String SCOPE = "https://graph.microsoft.com/.default";

	private transient ConfidentialClientApplication confidentialClientApplication;

	private transient String accessToken;

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

	@Override
	public IGraphServiceClient getClientConnection()
	{
		return GraphServiceClient.builder().authenticationProvider(request -> request.addHeader("Authorization", "Bearer " + accessToken)).buildClient();
	}

}


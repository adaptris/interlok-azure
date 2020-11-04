package com.adaptris.interlok.azure;

import com.adaptris.core.AdaptrisConnectionImp;
import com.adaptris.core.CoreException;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.util.Collections;

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
	private String clientSecret;

	private transient ConfidentialClientApplication confidentialClientApplication;

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
					ClientCredentialFactory.createFromSecret(clientSecret))
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

	private String tenant()
	{
		return String.format("https://login.microsoftonline.com/%s", tenantId);
	}
}

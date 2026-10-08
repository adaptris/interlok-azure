package com.adaptris.interlok.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import com.adaptris.core.CoreException;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;

public class DataLakeConnectionTest {

  @Test
  public void testLifecycle() throws Exception {
    DataLakeConnection connection = newConnection();
    assertEquals("testaccount", connection.getAccount());
    assertNull(connection.getClientConnection());

    try {
      connection.prepareConnection();
      connection.initConnection();
      assertNull(connection.getClientConnection());
      connection.startConnection();
      assertNotNull(connection.getClientConnection());
      assertEquals("https://testaccount.dfs.core.windows.net", connection.getClientConnection().getAccountUrl());
    } finally {
      connection.stopConnection();
      connection.closeConnection();
    }
  }

  @Test
  public void testCredentialConfigurationAndEndpoint() throws Exception {
    DataLakeConnection connection = newConnection();
    connection.setClientSecret("external-secret-reference");
    ClientSecretCredential credential = mock(ClientSecretCredential.class);
    DataLakeServiceClient client = mock(DataLakeServiceClient.class);

    try (MockedStatic<ExternalResolver> resolver = mockStatic(ExternalResolver.class);
        MockedConstruction<ClientSecretCredentialBuilder> credentials = mockConstruction(ClientSecretCredentialBuilder.class,
            org.mockito.Mockito.withSettings().defaultAnswer(Answers.RETURNS_SELF),
            (builder, context) -> when(builder.build()).thenReturn(credential));
        MockedConstruction<DataLakeServiceClientBuilder> clients = mockConstruction(DataLakeServiceClientBuilder.class,
            org.mockito.Mockito.withSettings().defaultAnswer(Answers.RETURNS_SELF),
            (builder, context) -> when(builder.buildClient()).thenReturn(client))) {
      resolver.when(() -> ExternalResolver.resolve("external-secret-reference")).thenReturn("resolved-test-secret");

      connection.initConnection();
      connection.startConnection();

      assertEquals(1, credentials.constructed().size());
      ClientSecretCredentialBuilder credentialBuilder = credentials.constructed().get(0);
      verify(credentialBuilder).clientId("application-id");
      verify(credentialBuilder).tenantId("tenant-id");
      verify(credentialBuilder).clientSecret("resolved-test-secret");
      verify(credentialBuilder).build();
      resolver.verify(() -> ExternalResolver.resolve("external-secret-reference"));
      assertEquals(1, clients.constructed().size());
      DataLakeServiceClientBuilder clientBuilder = clients.constructed().get(0);
      verify(clientBuilder).credential(credential);
      verify(clientBuilder).endpoint("https://testaccount.dfs.core.windows.net");
      verify(clientBuilder).buildClient();
      assertSame(client, connection.getClientConnection());
    }
  }

  @Test
  public void testInitWrapsInvalidCredentials() {
    DataLakeConnection connection = newConnection();
    connection.setClientSecret(null);

    CoreException exception = assertThrows(CoreException.class, connection::initConnection);

    assertNotNull(exception.getCause());
    assertNull(connection.getClientConnection());
  }

  private DataLakeConnection newConnection() {
    DataLakeConnection connection = new DataLakeConnection();
    connection.setApplicationId("application-id");
    connection.setTenantId("tenant-id");
    connection.setClientSecret("unit-test-secret");
    connection.setAccount("testaccount");
    return connection;
  }
}

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

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import com.adaptris.core.CoreException;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;

public class GraphAPIConnectionTest {

  @Test
  public void testLifecycle() throws Exception {
    GraphAPIConnection connection = newConnection();
    assertEquals("application-id", connection.getApplicationId());
    assertEquals("tenant-id", connection.getTenantId());
    assertEquals("unit-test-secret", connection.getClientSecret());
    assertNull(connection.getClientConnection());

    try {
      connection.prepareConnection();
      connection.initConnection();
      assertNull(connection.getClientConnection());
      connection.startConnection();
      assertNotNull(connection.getClientConnection());
    } finally {
      connection.stopConnection();
      connection.closeConnection();
    }
  }

  @Test
  public void testCredentialConfigurationAndScope() throws Exception {
    GraphAPIConnection connection = newConnection();
    connection.setClientSecret("external-secret-reference");
    ClientSecretCredential credential = mock(ClientSecretCredential.class);

    try (MockedStatic<ExternalResolver> resolver = mockStatic(ExternalResolver.class);
        MockedConstruction<ClientSecretCredentialBuilder> builders = mockConstruction(ClientSecretCredentialBuilder.class,
            org.mockito.Mockito.withSettings().defaultAnswer(Answers.RETURNS_SELF),
            (builder, context) -> when(builder.build()).thenReturn(credential));
        MockedConstruction<TokenCredentialAuthProvider> providers = mockConstruction(TokenCredentialAuthProvider.class,
            (provider, context) -> {
              assertEquals(Collections.singletonList("https://graph.microsoft.com/.default"), context.arguments().get(0));
              assertSame(credential, context.arguments().get(1));
            })) {
      resolver.when(() -> ExternalResolver.resolve("external-secret-reference")).thenReturn("resolved-test-secret");

      connection.initConnection();
      connection.startConnection();

      assertEquals(1, builders.constructed().size());
      ClientSecretCredentialBuilder builder = builders.constructed().get(0);
      verify(builder).clientId("application-id");
      verify(builder).tenantId("tenant-id");
      verify(builder).clientSecret("resolved-test-secret");
      verify(builder).build();
      resolver.verify(() -> ExternalResolver.resolve("external-secret-reference"));
      assertEquals(1, providers.constructed().size());
      assertNotNull(connection.getClientConnection());
    }
  }

  @Test
  public void testInitWrapsInvalidCredentials() {
    GraphAPIConnection connection = newConnection();
    connection.setClientSecret(null);

    CoreException exception = assertThrows(CoreException.class, connection::initConnection);

    assertNotNull(exception.getCause());
    assertNull(connection.getClientConnection());
  }

  private GraphAPIConnection newConnection() {
    GraphAPIConnection connection = new GraphAPIConnection();
    connection.setApplicationId("application-id");
    connection.setTenantId("tenant-id");
    connection.setClientSecret("unit-test-secret");
    return connection;
  }
}

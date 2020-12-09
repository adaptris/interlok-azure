package com.adaptris.interlok.azure.cosmosdb;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.http.apache.request.RequestInterceptorBuilder;
import com.adaptris.validation.constraints.UrlExpression;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.http.HttpRequestInterceptor;

import javax.validation.constraints.NotBlank;

@NoArgsConstructor
@XStreamAlias("cosmosdb-authorization-interceptor")
@ComponentProfile(summary = "Builds an authorization header for Azure CosmosDB", since = "3.9.2", tag = "azure,cosmosdb,cosmos,interceptor")
@DisplayOrder(order = {"masterKey", "httpVerb", "cosmosEndpoint", "targetKey"})
public class CosmosAuthorizationInterceptor implements RequestInterceptorBuilder {

  @Getter
  @Setter
  @NonNull
  @NotBlank
  @InputFieldHint(style = "password", external = true, expression = true)
  private String masterKey;

  @Getter
  @Setter
  @NonNull
  @NotBlank
  @InputFieldHint(expression = true, style = "com.adaptris.core.http.client.RequestMethodProvider.RequestMethod")
  private String httpVerb;

  @Getter
  @Setter
  @NonNull
  @NotBlank
  @InputFieldHint(expression = true)
  @UrlExpression
  private String cosmosEndpoint;

  @Getter
  @Setter
  @InputFieldDefault(value = CosmosAuthorizationHeaderImpl.DEFAULT_METADATA_KEY)
  @AdvancedConfig
  private String targetKey;

  @Override
  public HttpRequestInterceptor build() {
    CosmosAuthorizationHeaderFromUrl interceptor = new CosmosAuthorizationHeaderFromUrl();

    interceptor.setMasterKey(masterKey);
    interceptor.setHttpVerb(httpVerb);
    interceptor.setCosmosEndpointUrl(cosmosEndpoint);
    interceptor.setTargetKey(targetKey);

    return interceptor;
  }
}

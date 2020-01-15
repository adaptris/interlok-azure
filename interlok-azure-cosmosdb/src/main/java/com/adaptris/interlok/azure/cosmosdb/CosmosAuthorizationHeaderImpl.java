package com.adaptris.interlok.azure.cosmosdb;

import javax.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceImp;
import com.adaptris.interlok.util.Args;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * Abstract base class for generating Authorization headers.
 * 
 */
@NoArgsConstructor
public abstract class CosmosAuthorizationHeaderImpl extends ServiceImp {
  /**
   * The default metadata key for the {@link #setTargetKey(String)}; set to be {@value #DEFAULT_METADATA_KEY}.
   * 
   */
  public static final String DEFAULT_METADATA_KEY = "Authorization";
  /**
   * The {@value #X_MS_DATE} header, added as metadata by the services.
   * 
   */
  public static final String X_MS_DATE = "x-ms-date";

  /**
   * The metadata key that will hold the Authorization output.
   * 
   */
  @Getter
  @Setter
  @InputFieldDefault(value = DEFAULT_METADATA_KEY)
  @AdvancedConfig
  private String targetKey;

  /**
   * The Verb portion of the hashed token signature is the HTTP verb, such as GET, POST, or PUT.
   * 
   */
  @Getter
  @Setter
  @NonNull
  @NotBlank
  @InputFieldHint(expression = true, style = "com.adaptris.core.http.client.RequestMethodProvider.RequestMethod")
  private String httpVerb;

  /**
   * Your master key token.
   * 
   */
  @Getter
  @Setter
  @NonNull
  @NotBlank
  @InputFieldHint(style = "password", external = true, expression = true)
  private String masterKey;

  @Override
  public void prepare() throws CoreException {
    Args.notBlank(getMasterKey(), "master-key");
    Args.notBlank(getHttpVerb(), "verb");
  }

  @Override
  protected void initService() throws CoreException {}

  @Override
  protected void closeService() {}


  public <T extends CosmosAuthorizationHeaderImpl> T withMasterKey(String s) {
    setMasterKey(s);
    return (T) this;
  }

  public <T extends CosmosAuthorizationHeaderImpl> T withTargetKey(String s) {
    setTargetKey(s);
    return (T) this;
  }

  public <T extends CosmosAuthorizationHeaderImpl> T withHttpVerb(String s) {
    setHttpVerb(s);
    return (T) this;
  }

  protected String targetKey() {
    return StringUtils.defaultIfEmpty(getTargetKey(), DEFAULT_METADATA_KEY);
  }

}

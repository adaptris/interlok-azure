package com.adaptris.interlok.azure.cosmosdb;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceException;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.interlok.util.Args;
import com.adaptris.security.password.Password;
import com.microsoft.azure.documentdb.internal.BaseAuthorizationTokenProvider;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds an authorization header for Azure CosmosDB from an explicit ResourceID / ResourceType
 * 
 * <p>
 * Builds an authorization header using master keys based on the
 * <a href="https://docs.microsoft.com/en-us/rest/api/cosmos-db/access-control-on-cosmosdb-resources">CosmosDB documentation</a>.
 * Once the key is built, then you can send it as your {@code Authorization} header. Under the covers we actually use
 * {@code com.microsoft.azure.documentdb.internal.BaseAuthorizationTokenProvider} from the Cosmos Java SDK; but effectively what we
 * do is this (you could do this in an {@link com.adaptris.core.services.EmbeddedScriptingService} or similar).
 * </p>
 * 
 * <pre>
 * {@code
    verb = "GET";
    resourceType = "the resource type";
    resourceId = "the resource id"; 
    // From java.time
    date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT"));
    key = "The key";
    keyType = "master";
    tokenVersion = "1.0";
    stringToSign = verb.toLowerCase() + "\n" 
          + resourceType.toLowerCase() + "\n" 
          + resourceId
          + "\n" + date.toLowerCase() + "\n" 
          + "" + "\n";
    // From org.apache.commons.codec, unbase64 the key, and use it to hmac, then back to base64 again.
    sigBytes = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, Base64.decodeBase64(key))
            .hmac(stringToSign.getBytes("UTF-8"));
    signature = Base64.encodeBase64String(sigBytes);
    // from java.net
    authorization = URLEncoder.encode("type=" + keyType + "&ver=" + tokenVersion + "&sig=" + signature, "UTF-8");
 * }
 * </pre>
 * <p>
 * Since the date is also part of the signed data, it will also add the date it used against the key {@code "x-ms-date"} so that you
 * can send it as a HTTP request header.
 * </p>
 */
@NoArgsConstructor
@XStreamAlias("cosmosdb-authorization-header")
@ComponentProfile(summary = "Builds an authorization header for Azure CosmosDB", since = "3.9.2", tag = "azure,cosmosdb,cosmos")
@DisplayOrder(order = {"masterKey", "httpVerb", "resourceType", "resourceId", "targetKey", "timezone"})
public class CosmosAuthorizationHeader extends CosmosAuthorizationHeaderImpl {

  /**
   * The default date format: {@value #DEFAULT_DATE_FORMAT}.
   * 
   */
  public static final String DEFAULT_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  public static final String DEFAULT_TIMEZONE = "GMT";

  /**
   * The ResourceType portion of the string identifies the type of resource that the request is for, Eg. "dbs", "colls", "docs".
   * 
   */
  @Getter
  @Setter
  @NonNull
  @NotBlank
  @InputFieldHint(expression = true, style = "com.adaptris.interlok.azure.cosmosdb.ResourceTypeHelper#getResourceTypes")
  private String resourceType;

  /**
   * The ResourceLink portion of the string is the identity property of the resource that the request is directed at and is case
   * sensitive. For example, a collection it might look like: "dbs/MyDatabase/colls/MyCollection".
   * 
   */
  @Getter
  @Setter
  @InputFieldHint(expression = true)
  private String resourceId;

  /**
   * Set the timezone as required.
   * <p>
   * By default it is set to GMT, as this is the expected date style for CosmosDB. You shouldn't need to change it.
   * </p>
   */
  @Getter
  @Setter
  @InputFieldDefault(value = "GMT")
  @AdvancedConfig(rare = true)
  private String timezone;

  /**
   * Override the default date format.
   * <p>
   * By default it is set to be {@value #DEFAULT_DATE_FORMAT} which imposes a leading digit on the day if required;
   * {@link DateTimeFormatter#RFC_1123_DATE_TIME} doesn't appear suitable for use with Azure CosmosDB as it doesn't provide the
   * leading digit.
   * </p>
   */
  @Getter
  @Setter
  @InputFieldDefault(value = DEFAULT_DATE_FORMAT)
  @AdvancedConfig(rare = true)
  private String dateFormat;

  @Override
  public void prepare() throws CoreException {
    super.prepare();
    Args.notBlank(getResourceType(), "resource-type");
  }

  @Override
  public void doService(AdaptrisMessage msg) throws ServiceException {
    try {
      String now = DateTimeFormatter.ofPattern(dateFormat()).format(ZonedDateTime.now(ZoneId.of(timezone())));
      String v = msg.resolve(getHttpVerb());
      String id = msg.resolve(getResourceId());
      String type = msg.resolve(getResourceType());

      log.trace("Creating header for verb {}, resourceType {}, resourceId {}, date {}", v, type, id, now);

      Map<String, String> headers = new HashMap<String, String>();
      headers.put(X_MS_DATE, now);
      String masterKey = Password.decode(ExternalResolver.resolve(msg.resolve(getMasterKey())));
      BaseAuthorizationTokenProvider provider = new BaseAuthorizationTokenProvider(masterKey, null);
      String header = provider.generateKeyAuthorizationSignature(v, resourceId, resourceType, headers);
      msg.addMessageHeader(targetKey(), URLEncoder.encode(header, StandardCharsets.UTF_8.name()));
      msg.addMessageHeader(X_MS_DATE, now);
    } catch (Exception e) {
      throw ExceptionHelper.wrapServiceException(e);
    }
  }

  public CosmosAuthorizationHeader withResourceId(String s) {
    setResourceId(s);
    return this;
  }

  public CosmosAuthorizationHeader withResourceType(String s) {
    setResourceType(s);
    return this;
  }

  private String timezone() {
    return StringUtils.defaultIfBlank(getTimezone(), DEFAULT_TIMEZONE);
  }

  private String dateFormat() {
    return StringUtils.defaultIfBlank(getDateFormat(), DEFAULT_DATE_FORMAT);
  }
}

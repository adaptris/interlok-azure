package com.adaptris.interlok.azure.cosmosdb;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.lang3.BooleanUtils;

import com.microsoft.azure.documentdb.internal.Paths;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * This is to assist the UI with type-ahead on the ResourceTypes/ResourceSegments
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceTypeHelper {

  private static final String[] declaredConstants;

  static {
    declaredConstants = Arrays.stream(Paths.class.getDeclaredFields())
        .filter(field -> BooleanUtils
            .and(new boolean[] {Modifier.isStatic(field.getModifiers()), Modifier.isPublic(field.getModifiers())}))
        .map(field -> get(field))
        .filter(name -> !name.startsWith("/")) // gets rid of Paths#MEDIA_ROOT and stuffs.
        .toArray(String[]::new);
  }

  /**
   * Used by the InputFieldHint annotation for type-ahead.
   *
   */
  public static String[] getResourceTypes() {
    return Arrays.copyOf(declaredConstants, declaredConstants.length);
  }

  // Should never give an IllegalAccess since we're checking public statics...
  @SneakyThrows
  private static String get(Field f) {
    return (String) f.get(Paths.class);
  }


  /**
   * Parsing the ResourceType out of the URL.
   * <p>
   * Odd number of uri fragments, it's the last one, even number of uri fragments it's the penultimate one.
   * <ul>
   * <li>https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/ -&gt; docs</li>
   * <li>https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/MyName -&gt; docs</li>
   * <li>
   * </ul>
   * </p>
   *
   * @param url
   *          the URL endpoint
   * @return the ResourceType
   * @see CosmosAuthorizationHeaderFromUrl
   */
  public static String getResourceType(URL url) {
    String path = stripEnd(stripStart(defaultIfEmpty(url.getPath(), ""), "/"), "/");
    if (isEmpty(path)) {
      return "";
    }
    String[] fragments = path.split("/");
    if (fragments.length % 2 == 0) {
      return trimToEmpty(fragments[fragments.length - 2]);
    }
    return trimToEmpty(fragments[fragments.length - 1]);
  }

  /**
   * Parsing the ResourceID out of the URL.
   * <p>
   * odd number of uri fragments, then it's everything but the last one; even number of uri fragments it's all of them.
   * <ul>
   * <li>https://azuredb.microsoft.com/dbs/tempdb/colls -&gt; dbs/tempdb</li>
   * <li>https://azuredb.microsoft.com/dbs/tempdb/colls/tempcoll/docs/MyName -&gt; dbs/tempdb/colls/tempcoll/docs/MyName</li>
   * <li>
   * </ul>
   * </p>
   *
   * @param url
   *          the URL endpoint
   * @return the ResourceID.
   * @see CosmosAuthorizationHeaderFromUrl
   */
  public static String getResourceID(URL url) {
    String path = stripEnd(stripStart(defaultIfEmpty(url.getPath(), ""), "/"), "/");
    if (isEmpty(path)) {
      return "";
    }
    String[] fragments = path.split("/");
    if (fragments.length % 2 == 0) {
      return path;
    }
    // There's no path, so it's just /dbs -> which means the resourceID must be ""
    return path.lastIndexOf("/") > 0 ? path.substring(0, path.lastIndexOf("/")) : "";
  }

}



package com.adaptris.interlok.azure.cosmosdb;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

  public static final String[] declaredConstants;

  static {
    declaredConstants = Arrays.stream(Paths.class.getDeclaredFields())
        .filter(field -> BooleanUtils
            .and(new boolean[] {Modifier.isStatic(field.getModifiers()), Modifier.isPublic(field.getModifiers())}))
        .map(field -> get(field))
        .filter(name -> !name.startsWith("/")) // gets rid of Paths#MEDIA_ROOT and stuffs.
        .toArray(String[]::new);
  }

  public static String[] getResourceTypes() {
    return Arrays.copyOf(declaredConstants, declaredConstants.length);
  }

  // Should never give an IllegalAccess since we're checking public statics...
  @SneakyThrows
  private static String get(Field f) {
    return (String) f.get(Paths.class);
  }
}



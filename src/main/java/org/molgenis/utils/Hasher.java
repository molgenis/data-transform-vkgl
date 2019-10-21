package org.molgenis.utils;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

public class Hasher {

  private Hasher() {
    throw new IllegalStateException("Util class should not be instantiated");
  }

  public static String hash(String inputValue) {
    return Hashing.sha256()
        .hashString(inputValue, StandardCharsets.UTF_8)
        .toString();
  }
}

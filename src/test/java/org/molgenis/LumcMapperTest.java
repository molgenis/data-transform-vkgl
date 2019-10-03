package org.molgenis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class LumcMapperTest {

  private LumcMapper lumc = new LumcMapper();
  private HashMap<String, String> body = new HashMap<>();

  @Test
  void mapClassificationSuccessTest() {
    String original = "-?";
    lumc.mapClassification(body, original);
    assertEquals("lb", body.get("significance"));
  }

  @Test
  void mapClassificationErrorTest() {
    String original = "onzin";
    lumc.mapClassification(body, original);
    assertEquals("Unknown significance: onzin", body.get("error"));
  }
}
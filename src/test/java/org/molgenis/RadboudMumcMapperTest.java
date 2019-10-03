package org.molgenis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class RadboudMumcMapperTest {

  private RadboudMumcMapper radboudMumc = new RadboudMumcMapper();
  private HashMap<String, String> body = new HashMap<>();

  @Test
  void mapClassificationSuccessTest() {
    String original = "class 4";
    radboudMumc.mapClassification(body, original);
    assertEquals("lp", body.get("significance"));
  }

  @Test
  void mapClassificationErrorTest() {
    String original = "onzin";
    radboudMumc.mapClassification(body, original);
    assertEquals("Unknown significance: onzin", body.get("error"));
  }
}
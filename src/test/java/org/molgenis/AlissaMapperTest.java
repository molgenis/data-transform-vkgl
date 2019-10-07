package org.molgenis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class AlissaMapperTest {

  private AlissaMapper alissa = new AlissaMapper();
  private HashMap<String, String> body = new HashMap<>();

  @Test
  void mapClassificationSuccessTest() {
    String original = "BENIGN";
    alissa.mapClassification(body, original);
    assertEquals("b", body.get("significance"));
  }

  @Test
  void mapClassificationErrorTest() {
    String original = "onzin";
    alissa.mapClassification(body, original);
    assertEquals("Unknown significance: onzin", body.get("error"));
  }
}
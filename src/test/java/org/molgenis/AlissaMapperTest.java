package org.molgenis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AlissaMapperTest {

  private AlissaMapper alissa = new AlissaMapper(new HgvsService());
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

  @Test
  void mapDataTest() {
    Map<String, Object> body = new HashMap<>();
    body.put("classification", "BENIGN");
    body.put("start", "124");
    body.put("ref", "A");
    body.put("alt", "G");
    body.put("chromosome", "X");
    body.put("transcript", "NM_1234.5");
    body.put("c_nomen", "c.1234A>G");
    body.put("p_nomen", "NULL");
    body.put("stop", "124");

    alissa.mapData(body);

    assertFalse(body.containsKey("ref"));
    assertFalse(body.containsKey("alt"));
    assertEquals("A", body.get("ref_orig"));
    assertEquals("G", body.get("alt_orig"));
    assertEquals("", body.get("p_nomen"));
    assertEquals("NC_000023.10:g.124A>G", body.get("hgvs_normalized_vkgl"));
  }
}
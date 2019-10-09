package org.molgenis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
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

  @Test
  void mapDataTest() {
    Map<String, Object> body = new HashMap<>();
    body.put("classification", "b");
    body.put("start", "124");
    body.put("ref_orig", "A");
    body.put("alt_orig", "G");
    body.put("chromosome_orig", "chrX");
    body.put("stop", "124");

    radboudMumc.mapData(body);

    assertEquals("NC_000023.10:g.124A>G", body.get("hgvs_normalized_vkgl"));
  }
}
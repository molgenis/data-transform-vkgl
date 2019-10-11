package org.molgenis.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.utils.HgvsService;

class LumcMapperTest {

  private LumcMapper lumc = new LumcMapper(new HgvsService());
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

  @Test
  void mapDataTest() {
    Map<String, Object> body = new HashMap<>();
    body.put("variant_effect", "-");
    body.put("gDNA_normalized", "NC_000023.10:g.124A>G");

    lumc.mapData(body);

    assertFalse(body.containsKey("gDNA_normalized"));
    assertEquals("NC_000023.10:g.124A>G", body.get("hgvs_normalized_vkgl"));
  }
}
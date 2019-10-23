package org.molgenis.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LumcVkglTableMapperTest {

  private LumcVkglTableMapper lumc = new LumcVkglTableMapper();

  @Test
  void getTranscriptAndCdnaTest() {
    String cDNA = "NM_152486.2:c.580C>T";
    String[] transcriptAndCDna = lumc.getTranscriptAndCdna(cDNA);
    assertEquals("NM_152486.2", transcriptAndCDna[0]);
    assertEquals("c.580C>T", transcriptAndCDna[1]);
  }

  @Test
  void mapLineTest() {
    Map<String, Object> body = new HashMap<>();
    body.put("ref", "AA");
    body.put("alt", "GG");
    body.put("type", "sub");
    body.put("chrom", "X");
    body.put("pos", 124);
    body.put("gene", "ABCD1");
    body.put("significance", "b");
    body.put("hgvs_normalized_vkgl", "NC_000023.10:g.124A>G");
    body.put("cDNA", "NM_1234.5:c.1234A>G");

    lumc.mapLine(body);

    assertFalse(body.containsKey("protein"));

    assertEquals("A", body.get("ref"));
    assertEquals("G", body.get("alt"));
    assertEquals("73c7e515962203d90dc9a86d7b1040747db0ee5918660a88758ade3a7ae13d0f",
        body.get("id"));
    assertEquals("b", body.get("classification"));
    assertEquals("NC_000023.10:g.124A>G", body.get("hgvs_g"));
    assertEquals("125", body.get("stop"));
    assertEquals("NM_1234.5", body.get("transcript"));
    assertEquals("c.1234A>G", body.get("c_dna"));
  }

}
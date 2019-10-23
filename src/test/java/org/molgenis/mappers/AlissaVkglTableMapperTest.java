package org.molgenis.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AlissaVkglTableMapperTest {

  private AlissaVkglTableMapper alissa = new AlissaVkglTableMapper();

  @Test
  void getCorrectedRefAndAltSubTest() {
    String ref = "AA";
    String alt = "CC";
    String type = "sub";
    int start = 123;
    Map observed = alissa.getCorrectedRefAndAlt(ref, alt, type, start);
    Map<String, String> expected = new HashMap<String, String>() {{
      put("ref", "A");
      put("alt", "C");
      put("start", "124");
    }};
    assertEquals(expected, observed);
  }

  @Test
  void getCorrectedRefAndAltDelTest() {
    String ref = "AA";
    String alt = "A";
    String type = "del";
    int start = 123;
    Map observed = alissa.getCorrectedRefAndAlt(ref, alt, type, start);
    Map<String, String> expected = new HashMap<String, String>() {{
      put("ref", "AA");
      put("alt", "A");
      put("start", "123");
    }};
    assertEquals(expected, observed);
  }

  @Test
  void getStopPositionTest() {
    int start = 123;
    String ref = "AA";
    String actual = alissa.getStopPosition(start, ref);
    assertEquals("124", actual);
  }

  @Test
  void getHgvsTypeCTest() {
    String hgvs = "NM_1234.5:c.1234A>G";
    String actual = alissa.getHgvsType(hgvs);
    assertEquals("hgvs_c", actual);
  }

  @Test
  void getHgvsTypeGTest() {
    String hgvs = "NC_000023.10:g.124A>G";
    String actual = alissa.getHgvsType(hgvs);
    assertEquals("hgvs_g", actual);
  }

  @Test
  void getIdTest() {
    String ref = "T";
    String alt = "TTG";
    String chr = "1";
    String pos = "12345";
    String gene = "ABCD3";
    String actual = alissa.getId(ref, alt, chr, pos, gene);
    // Hash generated with python to make sure it is compatible
    assertEquals("d090e9ab54023091bc4b2f6de3312795cb1da4889f6eeb5b91ed87ee1ed082e4", actual);
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
    body.put("c_nomen", "c.1234A>G");
    body.put("transcript", "NM_1234.5");
    body.put("p_nomen", "NULL");

    alissa.mapLine(body);

    // Make sure the key is not the alissa key, but the vkgl one (issue #6)
    assertFalse(body.containsKey("last_updated_on"));

    assertEquals("", body.get("protein"));
    assertEquals("", body.get("location"));
    assertEquals("", body.get("exon"));
    assertEquals("", body.get("effect"));
    assertEquals("", body.get("lab_upload_date"));
    assertEquals("", body.get("protein"));
    assertEquals("A", body.get("ref"));
    assertEquals("G", body.get("alt"));
    assertEquals("73c7e515962203d90dc9a86d7b1040747db0ee5918660a88758ade3a7ae13d0f",
        body.get("id"));
    assertEquals("b", body.get("classification"));
    assertEquals("NC_000023.10:g.124A>G", body.get("hgvs_g"));
    assertEquals("125", body.get("stop"));
  }

}
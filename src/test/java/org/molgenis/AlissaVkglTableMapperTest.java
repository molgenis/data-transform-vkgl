package org.molgenis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
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
    ArrayList observed = alissa.getCorrectedRefAndAlt(ref, alt, type);
    ArrayList<String> expected = new ArrayList<String>() {{
      add("A");
      add("C");
    }};
    assertEquals(expected, observed);
  }

  @Test
  void getCorrectedRefAndAltDelTest() {
    String ref = "AA";
    String alt = "A";
    String type = "del";
    ArrayList observed = alissa.getCorrectedRefAndAlt(ref, alt, type);
    ArrayList<String> expected = new ArrayList<String>() {{
      add("AA");
      add("A");
    }};
    assertEquals(expected, observed);
  }

  @Test
  void getStopPositionTest() {
    String start = "123";
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
    assertEquals("1_12345_T_TTG_ABCD3", actual);
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

    assertFalse(body.containsKey("protein"));
    assertFalse(body.containsKey("location"));
    assertFalse(body.containsKey("exon"));
    assertFalse(body.containsKey("effect"));
    assertFalse(body.containsKey("last_updated_on"));

    assertEquals("A", body.get("ref"));
    assertEquals("G", body.get("alt"));
    assertEquals("X_124_A_G_ABCD1", body.get("id"));
    assertEquals("b", body.get("classification"));
    assertEquals("NC_000023.10:g.124A>G", body.get("hgvs_g"));
    assertEquals("124", body.get("stop"));
  }

}
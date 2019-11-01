package org.molgenis.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.validators.ReferenceSequenceValidator.matchesOriginalRef;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReferenceSequenceValidatorTest {

  ReferenceSequenceValidator validator = new ReferenceSequenceValidator();

  @Test
  void matchesOriginalRefTestFalse() {
    boolean actual = matchesOriginalRef("AAATAAAGA", "GATTATTCAC", 179486179, 179486178);
    assertFalse(actual, "Original ref does not match generated ref");
  }

  @Test
  void matchesOriginalRefTestTrueLargeOrig() {
    boolean actual = matchesOriginalRef("GGGCACGTGCACGAACAACACGGGACGCGCGCA", "CG", 78064064,
        78064063);
    assertTrue(actual, "Original ref does match generated ref");
  }

  @Test
  void matchesOriginalRefTestTrueLargeRefs() {
    boolean actual = matchesOriginalRef("CTCCCAGCCTCATTTCCGGCCTGCCCTGCAGCATGGTGGACTCCCTGGCCAAAC",
        "CTCCCAGCCTCATTTCCGGCCTGCCCTGCAGCATGGTGGACTCCCTGGCCAAAC", 36018570,
        36018570);
    assertTrue(actual, "Original ref does match generated ref");
  }

  @Test
  void validateOriginalRefErrorTest() {
    Map<String, Object> body = new HashMap<String, Object>() {{
      put("ref", "A");
      put("ref_orig", "C");
      put("start", "123");
      put("pos", 123);
    }};
    validator.validateOriginalRef(body);
    assertEquals("Incorrect original reference", body.get("error"));
  }

  @Test
  void validateOriginalRefSuccessTest() {
    Map<String, Object> body = new HashMap<String, Object>() {{
      put("ref", "C");
      put("ref_orig", "C");
      put("start", "123");
      put("pos", 123);
    }};
    validator.validateOriginalRef(body);
    assertFalse(body.containsKey("error"));
  }

  @Test
  void validateOriginalRefMTTest() {
    Map<String, Object> body = new HashMap<String, Object>() {{
      put("ref", "C");
      put("ref_orig", "C");
      put("start", "123");
      put("pos", 123);
      put("chromosome", "MT");
    }};
    validator.validateOriginalRef(body);
    assertFalse(body.containsKey("error"));
  }

  @Test
  void validateOriginalRefMErrorTest() {
    Map<String, Object> body = new HashMap<String, Object>() {{
      put("ref", "C");
      put("ref_orig", ".");
      put("start", "123");
      put("pos", 123);
      put("chromosome", "MT");
    }};
    validator.validateOriginalRef(body);
    assertFalse(body.containsKey("error"));
  }
}
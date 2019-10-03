package org.molgenis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.ReferenceSequenceValidator.matchesOriginalRef;

import org.junit.jupiter.api.Test;

class ReferenceSequenceValidatorTest {

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
}
package org.molgenis;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class UniquenessCheckerTest {

  UniquenessChecker uniquenessChecker = new UniquenessChecker();

  HashMap<String, Object> getVariant(String chromosome, int position, String ref, String alt,
      String gene,
      String hgvs) {
    HashMap<String, Object> variant = new HashMap<>();
    variant.put("chrom", chromosome);
    variant.put("pos", position);
    variant.put("ref", ref);
    variant.put("alt", alt);
    variant.put("gene", gene);
    variant.put("hgvs_normalized_vkgl", hgvs);
    return variant;
  }

  @Test
  void getUniqueVariantsListTest() {
    HashMap variantA = getVariant("1", 1234, "A", "G", "ABCD1", "NC_000001.10:g.1234A>G");
    HashMap variantB = getVariant("2", 12345, "G", "T", "ABCD2", "NC_000001.10:g.12345G>T");
    HashMap variantC = getVariant("1", 1234, "A", "G", "ABCD1", "NC_000001.10:g.1234A>G");
    HashMap<String, Object> variantD = getVariant("3", 123456, "C", "T", "ABCD3",
        "NC_000001.10:g.123456C>T");
    HashMap variantE = getVariant("1", 1234, "A", "G", "ABCD1", "NC_000001.10:g.1234A>G");
    variantD.put("error", "some error message");
    List body = new ArrayList<>(Arrays.asList(variantA, variantB, variantC, variantD, variantE));
    List<HashMap> actual = uniquenessChecker.getUniqueVariantsList(body);

    HashMap variantAError = getVariant("1", 1234, "A", "G", "ABCD1", "NC_000001.10:g.1234A>G");
    variantAError.put("error",
        "Variant duplicated: NC_000001.10:g.1234A>G,NC_000001.10:g.1234A>G,NC_000001.10:g.1234A>G");
    List<HashMap> expected = new ArrayList<>(
        Arrays.asList(variantD, variantAError, variantB));
    assertThat(actual, is(expected));
  }
}
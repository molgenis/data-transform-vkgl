package org.molgenis.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.utils.InvalidGeneException;

class HgncGeneValidatorTest {

  HgncGeneValidator service = new HgncGeneValidator(getGenes(), getGeneAlternatives());

  private static Map<String, String> getGeneAlternatives() {
    Map<String, String> geneAlternatives = new HashMap<>();
    //previous NCRNA00181
    geneAlternatives.put("ncrna00181", "a1bg-as1");
    //alias FLJ23569
    geneAlternatives.put("flj23569", "a1bg-as1");
    //alias ABCB5alpha
    geneAlternatives.put("abcb5alpha", "abcb5");
    //alias
    geneAlternatives.put("umat", "abcb6");
    //alias bA453N3.6
    geneAlternatives.put("ba453n3.6", "abcd1p2");
    //alias Em:AC012044.1
    geneAlternatives.put("em:ac012044.1", "agap4");
    //previous IGHV2@
    geneAlternatives.put("ighv2@", "ighvor15@");
    //alias IGHV
    geneAlternatives.put("ighv", "ighvor15@");
    return geneAlternatives;
  }

  private static Map<String, String> getGeneInfo(String hgncId, String status,
      String approvedSymbol, String chromosome) {
    Map<String, String> geneInfo = new HashMap<>();
    geneInfo.put("hgnc_id", hgncId);
    geneInfo.put("status", status);
    geneInfo.put("approved_symbol", approvedSymbol);
    geneInfo.put("chromosome", chromosome);
    return geneInfo;
  }

  private static Map<String, Map<String, String>> getGenes() {
    Map<String, Map<String, String>> genes = new HashMap<>();
    genes.put("a1bg-as1", getGeneInfo("HGNC:37133", "Approved", "A1BG-AS1", "19"));
    genes.put("abcb5", getGeneInfo("HGNC:46", "Approved", "ABCB5", "7"));
    genes.put("abcb6", getGeneInfo("HGNC:47", "Approved", "ABCB6", "2"));
    genes.put("abcd1p2", getGeneInfo("HGNC:63", "Approved", "ABCD1P2", "10"));
    genes.put("agap4", getGeneInfo("HGNC:23459", "Approved", "AGAP4", "10"));
    genes.put("acad", getGeneInfo("HGNC:86", "Entry Withdrawn", "ACAD", "NA"));
    genes.put("ighvor15@", getGeneInfo("HGNC:5711", "Entry Withdrawn", "IGHVOR15@", "15"));
    return genes;
  }

  @Test
  void isValidGeneValid() {
    assertTrue(service.isValidGene("ABCB5"));
  }

  @Test
  void isValidGeneInValid() {
    assertFalse(service.isValidGene("IGHVOR15@"));
  }

  @Test
  void getGeneStatusApproved() {
    assertEquals("approved", service.getGeneStatus("A1BG-AS1"));
  }

  @Test
  void getGeneStatusWithdrawn() {
    assertEquals("entry withdrawn", service.getGeneStatus("ACAD"));
  }

  @Test
  void translateGeneValid() {
    assertEquals("A1BG-AS1", service.translateGene("NCRNA00181"));
  }

  @Test
  void translateGeneInvalid() {
    assertNull(service.translateGene("March-1"));
  }

  @Test
  void getValidatedGeneApproved() throws InvalidGeneException {
    assertEquals("ABCD1P2", service.getValidatedGene("ABCD1P2"));
  }

  @Test
  void getValidatedGeneTranslated() throws InvalidGeneException {
    assertEquals("AGAP4", service.getValidatedGene("Em:AC012044.1"));
  }

  @Test
  void getValidatedGeneInvalid() {
    assertThrows(InvalidGeneException.class, () -> service.getValidatedGene("ACDC"),
        "No valid gene symbol can be found for: ACDC");
  }

  @Test
  void getValidatedGeneWithdrawn() {
    assertThrows(InvalidGeneException.class, () -> service.getValidatedGene("ACAD"),
        "ACAD has been found with status: Entry Withdrawn");
  }

  @ParameterizedTest
  @ValueSource(strings = {"APPL", "APPL1", "ZWINTAS", "ZWINT", "MPP5"})
  void getValidatedGeneEntryWithdrawn(String hgncGeneSymbol) {
    Map<String, String> zwintasGene = new HashMap<>();
    zwintasGene.put("hgnc_id", "HGNC:13196");
    zwintasGene.put("previous_symbol", "");
    zwintasGene.put("locus_group", "non-coding RNA");
    zwintasGene.put("ensembl_gene_id", "");
    zwintasGene.put("chromosome", "10");
    zwintasGene.put("ucsc_gene_id", "");
    zwintasGene.put("approved_symbol", "ZWINTAS");
    zwintasGene.put("approved_name", "ZWINT antisense RNA");
    zwintasGene.put("alias_symbol", "MPP5");
    zwintasGene.put("status", "Entry Withdrawn");
    zwintasGene.put("chromosome_location", "10q21.1");
    zwintasGene.put("ncbi_gene_id", "");

    Map<String, String> applGene = new HashMap<>();
    applGene.put("hgnc_id", "HGNC:623");
    applGene.put("previous_symbol", "APPL1");
    applGene.put("locus_group", "other");
    applGene.put("ensembl_gene_id", "");
    applGene.put("chromosome", "9");
    applGene.put("ucsc_gene_id", "");
    applGene.put("approved_symbol", "APPL");
    applGene.put("approved_name", "amyloid beta (A4) precursor protein-like");
    applGene.put("alias_symbol", "");
    applGene.put("status", "Entry Withdrawn");
    applGene.put("chromosome_location", "9q31-qter");
    applGene.put("ncbi_gene_id", "");

    Map<String, Map<String, String>> genes = new HashMap<>();
    genes.put("zwintas", zwintasGene);
    genes.put("appl", applGene);

    Map<String, String> previousGeneAliases = new HashMap<>();
    previousGeneAliases.put("mpp5", "zwintas");
    previousGeneAliases.put("appl1", "appl");

    HgncGeneValidator hgncGeneValidator = new HgncGeneValidator(genes,
        previousGeneAliases);

    assertThrows(InvalidGeneException.class,
        () -> hgncGeneValidator.getValidatedGene(hgncGeneSymbol));
  }
}
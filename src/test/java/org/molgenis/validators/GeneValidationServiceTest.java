package org.molgenis.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.molgenis.utils.InvalidGeneException;

class GeneValidationServiceTest {

  GeneValidationService service = new GeneValidationService(getGenes(), getGeneAlternatives());

  private static HashMap<String, String> getGeneAlternatives() {
    HashMap<String, String> geneAlternatives = new HashMap<>();
    //previous NCRNA00181
    geneAlternatives.put("ncrna00181", "A1BG-AS1");
    //alias FLJ23569
    geneAlternatives.put("flj23569", "A1BG-AS1");
    //alias ABCB5alpha
    geneAlternatives.put("abcb5alpha", "ABCB5");
    //alias
    geneAlternatives.put("umat", "ABCB6");
    //alias bA453N3.6
    geneAlternatives.put("ba453n3.6", "ABCD1P2");
    //alias Em:AC012044.1
    geneAlternatives.put("em:ac012044.1", "AGAP4");
    //previous IGHV2@
    geneAlternatives.put("ighv2@", "IGHVOR15@");
    //alias IGHV
    geneAlternatives.put("ighv", "IGHVOR15@");
    return geneAlternatives;
  }

  private static HashMap<String, String> getGeneInfo(String hgncId, String status,
      String approvedSymbol, String chromosome) {
    HashMap<String, String> geneInfo = new HashMap<>();
    geneInfo.put("hgnc_id", hgncId);
    geneInfo.put("status", status);
    geneInfo.put("approved_symbol", approvedSymbol);
    geneInfo.put("chromosome", chromosome);
    return geneInfo;
  }

  private static HashMap<String, HashMap<String, String>> getGenes() {
    HashMap<String, HashMap<String, String>> genes = new HashMap<>();
    genes.put("A1BG-AS1", getGeneInfo("HGNC:37133", "Approved", "A1BG-AS1", "19"));
    genes.put("ABCB5", getGeneInfo("HGNC:46", "Approved", "ABCB5", "7"));
    genes.put("ABCB6", getGeneInfo("HGNC:47", "Approved", "ABCB6", "2"));
    genes.put("ABCD1P2", getGeneInfo("HGNC:63", "Approved", "ABCD1P2", "10"));
    genes.put("AGAP4", getGeneInfo("HGNC:23459", "Approved", "AGAP4", "10"));
    genes.put("ACAD", getGeneInfo("HGNC:86", "Entry Withdrawn", "ACAD", "NA"));
    genes.put("IGHVOR15@", getGeneInfo("HGNC:5711", "Entry Withdrawn", "IGHVOR15@", "15"));
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
}
package org.molgenis.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HgvsServiceTest {

  HgvsService hgvsService = new HgvsService();

  @Test
  void getHgvsGInsertionTest() {
    String actual = hgvsService
        .getHgvsG("T", "TCTCCCGACACCACCTCCCAGGAGCCTCCCGACACCACCTCCCAGGAGC", "chr19", 501743,
            501743);
    assertEquals("NC_000019.9:g.501744_501745insCTCCCGACACCACCTCCCAGGAGCCTCCCGACACCACCTCCCAGGAGC",
        actual,
        "Hgvs is created for insertion");
  }

  @Test
  void getHgvsGInsertionWrittenAsIndelTest() {
    String actual = hgvsService
        .getHgvsG("T", "CT", "chr1", 160109411,
            160109412);
    assertEquals("NC_000001.10:g.160109410_160109411insC",
        actual,
        "Hgvs is created for insertion with correct position");
  }

  @Test
  void getHgvsGInsertionWithMissingRefTest() {
    String actual = hgvsService
        .getHgvsG(".", "CTG", "chr19", 13318672,
            501743);
    assertEquals("NC_000019.9:g.13318672_13318673insCTG",
        actual,
        "Hgvs is created for insertion without ref");
  }

  @Test
  void getHgvsGDeletionTest() {
    String actual = hgvsService
        .getHgvsG("GA", "G", "chr17", 11887519,
            11887520);
    assertEquals("NC_000017.10:g.11887520_11887520delA",
        actual,
        "Hgvs is created for insertion without ref");
  }

  @Test
  void getHgvsGDeletionWithMissingAltTest() {
    String actual = hgvsService
        .getHgvsG("GGCCG", ".", "chr19", 47249352,
            47249356);
    assertEquals("NC_000019.9:g.47249352_47249356delGGCCG",
        actual,
        "Hgvs is created for insertion without ref");
  }

  @Test
  void getHgvsGSnpTest() {
    String actual = hgvsService
        .getHgvsG("C", "A", "chr2", 179523777,
            179523777);
    assertEquals("NC_000002.11:g.179523777C>A",
        actual,
        "Hgvs is created for SNP");
  }

  @Test
  void getHgvsGDelinsTest() {
    String actual = hgvsService
        .getHgvsG("AAATAAAAGA", "TT", "chr2", 179486188,
            179523777);
    assertEquals("NC_000002.11:g.179486188_179523777delinsTT",
        actual,
        "Hgvs is created for Delins");
  }

  @Test
  void getHgvsGShortDelinsTest() {
    String actual = hgvsService
        .getHgvsG("A", "TT", "chr2", 179486188,
            179486189);
    assertEquals("NC_000002.11:g.179486188_179486189delinsTT",
        actual,
        "Hgvs is created for Delins");
  }

  @Test
  void getHgvsMissingRefAndAltTest() {
    String actual = hgvsService
        .getHgvs("NM_001267550.1", "c.38214=", ".", ".",
            179523777, 179523777, "chr2");
    assertEquals("NM_001267550.1:c.38214=",
        actual,
        "Hgvs is created using NM and c.");
  }

  @Test
  void getHgvsMissingTranscriptTest() {
    String actual = hgvsService
        .getHgvs("", "c.37408G>T", "C", "A",
            179519254, 179519254, "chr2");
    assertEquals("NC_000002.11:g.179519254C>A",
        actual,
        "Hgvs is created using NC, pos, ref and alt");
  }

  @Test
  void getHgvsMissingCDnaTest() {
    String actual = hgvsService
        .getHgvs("NM_001267550.1", "", "C", "A",
            179519254, 179519254, "chr2");
    assertEquals("NC_000002.11:g.179519254C>A",
        actual,
        "Hgvs is created using NC, pos, ref and alt");
  }

  @Test
  void getHgvsStartingWithNCTest() {
    String actual = hgvsService
        .getHgvs("NC_000002.11", "c.37408G>T", "C", "A",
            179519254, 179519254, "chr2");
    assertEquals("NC_000002.11:g.179519254C>A",
        actual,
        "Hgvs is created using NC, pos, ref and alt");
  }

  @Test
  void getHgvsMTTest() {
    String actual = hgvsService
        .getHgvs("NC_012920.1", "m.15326A>G", "A", "G",
            15326, 15326, "chrMT");
    assertEquals("NC_012920.1:m.15326A>G",
        actual,
        "Hgvs is created using NC, pos, ref and alt");
  }
}
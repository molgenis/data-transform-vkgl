package org.molgenis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HgvsServiceTest {

  HgvsService hgvsService = new HgvsService();

  @Test
  void getHgvsGInsertionTest() {
    String actual = hgvsService
        .getHgvsG("T", "TCTCCCGACACCACCTCCCAGGAGCCTCCCGACACCACCTCCCAGGAGC", "chr19", 501743,
            501743);
    assertEquals(actual,
        "NC_000019.9:g.501744_501745insCTCCCGACACCACCTCCCAGGAGCCTCCCGACACCACCTCCCAGGAGC",
        "Hgvs is created for insertion");
  }
}

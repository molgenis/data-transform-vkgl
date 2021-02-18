package org.molgenis.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HgncFileTest {

  HgncFile hgncFile = new HgncFile("src/test/resources/hgnc_test_file.tsv");

  HgncFileTest() throws IOException {
  }

  @Test
  void getGenes() {
    Map<String, Map<String, String>> genes = hgncFile.getGenes();
    Set<String> actualGeneSymbols = new HashSet<>(genes.keySet());
    Set<String> expectedGeneSymbols;
    expectedGeneSymbols = new HashSet<>();
    expectedGeneSymbols.add("A1BG");
    expectedGeneSymbols.add("A1BG-AS1");
    expectedGeneSymbols.add("A1CF");
    expectedGeneSymbols.add("A2M");
    expectedGeneSymbols.add("A2M-AS1");
    expectedGeneSymbols.add("A2ML1");
    expectedGeneSymbols.add("A2ML1-AS1");
    expectedGeneSymbols.add("A2ML1-AS2");
    expectedGeneSymbols.add("A2MP1");
    expectedGeneSymbols.add("A3GALT2");
    expectedGeneSymbols.add("A4GALT");
    expectedGeneSymbols.add("A4GNT");
    expectedGeneSymbols.add("A12M1");
    expectedGeneSymbols.add("A12M2");
    expectedGeneSymbols.add("A12M3");
    expectedGeneSymbols.add("A12M4");
    expectedGeneSymbols.add("AAAS");
    expectedGeneSymbols.add("AABT");
    expectedGeneSymbols.add("AACS");
    expectedGeneSymbols.add("AACSP1");
    expectedGeneSymbols.add("AADAC");
    expectedGeneSymbols.add("AADACL2");
    expectedGeneSymbols.add("AADACL2-AS1");
    expectedGeneSymbols.add("AADACL3");
    expectedGeneSymbols.add("AADACL4");
    expectedGeneSymbols.add("AADACP1");
    expectedGeneSymbols.add("AADAT");
    expectedGeneSymbols.add("AAGAB");
    expectedGeneSymbols.add("AAK1");
    expectedGeneSymbols.add("AAMDC");
    assertEquals(expectedGeneSymbols, actualGeneSymbols);
  }

  @Test
  void getAlternativeGeneNames() {
    Map<String, String> alternativeGeneNames = hgncFile.getAlternativeGeneNames();
    Set<String> actualAlternatives = new HashSet<>(alternativeGeneNames.keySet());
    Set<String> expectedAlternatives = new HashSet<>();
    expectedAlternatives.add("flj23569");
    expectedAlternatives.add("ncrna00181");
    expectedAlternatives.add("a1bgas");
    expectedAlternatives.add("a1bg-as");
    expectedAlternatives.add("acf");
    expectedAlternatives.add("asp");
    expectedAlternatives.add("acf64");
    expectedAlternatives.add("acf65");
    expectedAlternatives.add("apobec1cf");
    expectedAlternatives.add("fwp007");
    expectedAlternatives.add("s863-7");
    expectedAlternatives.add("cpamd5");
    expectedAlternatives.add("flj25179");
    expectedAlternatives.add("cpamd9");
    expectedAlternatives.add("p170");
    expectedAlternatives.add("a2mp");
    expectedAlternatives.add("igbs3s");
    expectedAlternatives.add("a3galt2p");
    expectedAlternatives.add("igb3s");
    expectedAlternatives.add("a14galt");
    expectedAlternatives.add("p1");
    expectedAlternatives.add("gb3s");
    expectedAlternatives.add("p(k)");
    expectedAlternatives.add("alpha4gnt");
    expectedAlternatives.add("flj12389");
    expectedAlternatives.add("sur-5");
    expectedAlternatives.add("acsf1");
    expectedAlternatives.add("aacsl");
    expectedAlternatives.add("dac");
    expectedAlternatives.add("ces5a1");
    expectedAlternatives.add("mgc72001");
    expectedAlternatives.add("otthumg00000001887");
    expectedAlternatives.add("otthumg00000001889");
    expectedAlternatives.add("katii");
    expectedAlternatives.add("kat2");
    expectedAlternatives.add("kyat2");
    expectedAlternatives.add("flj11506");
    expectedAlternatives.add("p34");
    expectedAlternatives.add("kiaa1048");
    expectedAlternatives.add("dkfzp686k16132");
    expectedAlternatives.add("ptd015");
    expectedAlternatives.add("c11orf67");
    expectedAlternatives.add("flj21035");
    assertEquals(expectedAlternatives, actualAlternatives);
  }
}
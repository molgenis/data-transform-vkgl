package org.molgenis.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HgncGenesParserTest {

  HgncGenes hgncGenes = new HgncGenes();
  HgncGenesParser hgncGenesParser = new HgncGenesParser("src/test/resources/hgnc_test_file.tsv",
      hgncGenes);

  HgncGenesParserTest() throws IOException {
  }

  @Test
  void getGenes() {
    Map<String, Map<String, String>> genes = hgncGenes.getGenes();
    Set<String> actualGeneSymbols = new HashSet<>(genes.keySet());
    Set<String> expectedGeneSymbols;
    expectedGeneSymbols = new HashSet<>();
    expectedGeneSymbols.add("a1bg");
    expectedGeneSymbols.add("a1bg-as1");
    expectedGeneSymbols.add("a1cf");
    expectedGeneSymbols.add("a2m");
    expectedGeneSymbols.add("a2m-as1");
    expectedGeneSymbols.add("a2ml1");
    expectedGeneSymbols.add("a2ml1-as1");
    expectedGeneSymbols.add("a2ml1-as2");
    expectedGeneSymbols.add("a2mp1");
    expectedGeneSymbols.add("a3galt2");
    expectedGeneSymbols.add("a4galt");
    expectedGeneSymbols.add("a4gnt");
    expectedGeneSymbols.add("a12m1");
    expectedGeneSymbols.add("a12m2");
    expectedGeneSymbols.add("a12m3");
    expectedGeneSymbols.add("a12m4");
    expectedGeneSymbols.add("aaas");
    expectedGeneSymbols.add("aabt");
    expectedGeneSymbols.add("aacs");
    expectedGeneSymbols.add("aacsp1");
    expectedGeneSymbols.add("aadac");
    expectedGeneSymbols.add("aadacl2");
    expectedGeneSymbols.add("aadacl2-as1");
    expectedGeneSymbols.add("aadacl3");
    expectedGeneSymbols.add("aadacl4");
    expectedGeneSymbols.add("aadacp1");
    expectedGeneSymbols.add("aadat");
    expectedGeneSymbols.add("aagab");
    expectedGeneSymbols.add("aak1");
    expectedGeneSymbols.add("aamdc");
    assertEquals(expectedGeneSymbols, actualGeneSymbols);
  }

  @Test
  void getAlternativeGeneNames() {
    Map<String, String> previousGeneAliases = hgncGenes.getPreviousGeneAliases();
    Set<String> actualAlternatives = new HashSet<>(previousGeneAliases.keySet());
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
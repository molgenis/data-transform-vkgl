package org.molgenis.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HgncGenes {

  private final Map<String, Map<String, String>> genes = new HashMap<>();
  private final Map<String, String> previousGeneAliases = new HashMap<>();

  private static final String SYMBOL = "approved_symbol";

  public Map<String, Map<String, String>> getGenes() {
    return genes;
  }

  public void addToGenes(Map<String, String> geneInfo) {
    this.genes.put(geneInfo.get(SYMBOL).toLowerCase(), geneInfo);
  }

  public Map<String, String> getPreviousGeneAliases() {
    return previousGeneAliases;
  }

  public void addToPreviousGeneAliases(String previousAlias, String currentSymbol) {
    this.previousGeneAliases
        .put(previousAlias.toLowerCase(), currentSymbol.toLowerCase(Locale.ROOT));
  }

}

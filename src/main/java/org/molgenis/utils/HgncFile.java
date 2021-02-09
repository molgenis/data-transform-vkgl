package org.molgenis.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HgncFile {

  private static final String PREVIOUS = "previous_symbol";
  private static final String ALIAS = "alias_symbol";
  private static final String SYMBOL = "approved_symbol";
  private static final String SEPARATOR = "\t";
  private static final String EMPTY = "";

  private HashMap<String, Integer> headerPositions;
  private HashMap<String, String> alternativeGeneNames;
  private HashMap<String, HashMap<String, String>> genes;

  public HgncFile(String geneFilePath) throws IOException {
    this.setAlternativeGeneNames(new HashMap<>());
    this.setHeaderPositions(new HashMap<>());
    this.setGenes(new HashMap<>());
    this.getGeneData(geneFilePath);
  }

  public Map<String, HashMap<String, String>> getGenes() {
    return this.genes;
  }

  private void setGenes(HashMap<String, HashMap<String, String>> genes) {
    this.genes = genes;
  }

  private void addToGenes(HashMap<String, String> geneInfo) {
    this.genes.put(geneInfo.get(SYMBOL), geneInfo);
  }

  private Map<String, Integer> getHeaderPositions() {
    return this.headerPositions;
  }

  private void setHeaderPositions(HashMap<String, Integer> headerPositions) {
    this.headerPositions = headerPositions;
  }

  private void addToHeaderPositions(String header, Integer position) {
    this.headerPositions.put(header, position);
  }

  public Map<String, String> getAlternativeGeneNames() {
    return this.alternativeGeneNames;
  }

  private void addToAlternativeGeneName(String alternative, String original) {
    this.alternativeGeneNames.put(alternative.toLowerCase(), original);
  }

  private void setAlternativeGeneNames(HashMap<String, String> alternativeGeneNames) {
    this.alternativeGeneNames = alternativeGeneNames;
  }


  private void setHeaderPositions(String header) {
    String[] columns = header.toLowerCase().replace(" ", "_").split(SEPARATOR);
    int position = 0;
    for (String key : columns) {
      this.addToHeaderPositions(key, position);
      position++;
    }
  }

  private void getGeneData(String geneFilePath) throws IOException {
    File geneFile = new File(geneFilePath);
    try (BufferedReader br = new BufferedReader(new FileReader(geneFile))) {
      String header = br.readLine();
      this.setHeaderPositions(header);

      String line;
      while ((line = br.readLine()) != null) {
        List<String> geneInfo = Arrays.asList(line.split(SEPARATOR, -1));
        String gene = geneInfo.get(this.getHeaderPositions().get(SYMBOL));
        if (!this.getGenes().containsKey(gene)) {
          HashMap<String, String> geneProps = new HashMap<>();

          for (Map.Entry<String, Integer> stringIntegerEntry : this.getHeaderPositions()
              .entrySet()) {
            try {
              geneProps
                  .put(stringIntegerEntry.getKey(), geneInfo.get(stringIntegerEntry.getValue()));
            } catch (Exception ex) {
              // if value was empty it will be eaten by the readline
              geneProps.put(stringIntegerEntry.getKey(), EMPTY);
            }
          }
          this.addToGenes(geneProps);
        }
        addAltGeneIfNotExists(geneInfo, gene, ALIAS);
        addAltGeneIfNotExists(geneInfo, gene, PREVIOUS);
      }
    }
  }

  private void addAltGeneIfNotExists(List<String> geneInfo, String gene, String alt) {
    Integer position = this.getHeaderPositions().get(alt);
    if (geneInfo.size() > position) {
      String alternative = geneInfo.get(position);
      if (!alternative.equals(EMPTY) && !this.getAlternativeGeneNames().containsKey(alternative)) {
        this.addToAlternativeGeneName(alternative, gene);
      }
    }
  }
}

package org.molgenis.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HgncGenesParser {

  private static final String PREVIOUS = "previous_symbol";
  private static final String ALIAS = "alias_symbol";
  private static final String SYMBOL = "approved_symbol";
  private static final String SEPARATOR = "\t";
  private static final String EMPTY = "";

  private final Map<String, Integer> headerPositions = new HashMap<>();

  HgncGenes hgncGenes;

  public HgncGenesParser(String geneFilePath, HgncGenes hgncGenes) throws IOException {
    this.hgncGenes = hgncGenes;
    this.getGeneData(geneFilePath);
  }

  private Map<String, Integer> getHeaderPositions() {
    return this.headerPositions;
  }

  private void addToHeaderPositions(String header, Integer position) {
    this.headerPositions.put(header, position);
  }

  private void setHeaderPositions(String header) {
    String[] columns = header.toLowerCase().replace(" ", "_").split(SEPARATOR);
    int position = 0;
    for (String key : columns) {
      this.addToHeaderPositions(key, position);
      position++;
    }
  }

  private void getGeneData(String geneFilePathLocation) throws IOException {
    Path geneFilePath = Paths.get(geneFilePathLocation);
    try (BufferedReader br = Files.newBufferedReader(geneFilePath, StandardCharsets.UTF_8)) {
      String header = br.readLine();
      this.setHeaderPositions(header);

      String line;
      while ((line = br.readLine()) != null) {
        List<String> geneInfo = Arrays.asList(line.split(SEPARATOR, -1));
        String gene = geneInfo.get(this.getHeaderPositions().get(SYMBOL));
        if (!this.hgncGenes.getGenes().containsKey(gene)) {
          Map<String, String> geneProps = new HashMap<>();

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
          this.hgncGenes.addToGenes(geneProps);
        }
        addAltGene(geneInfo, gene, ALIAS);
        addAltGene(geneInfo, gene, PREVIOUS);
      }
    } catch (NoSuchFileException ex) {
      throw new NoSuchFileException("HGNC Genes file does not exist on: [" + ex.getMessage() + "]");
    }
  }

  private void addAltGene(List<String> geneInfo, String gene, String alt) {
    Integer position = this.getHeaderPositions().get(alt);
    if (geneInfo.size() > position) {
      String alternative = geneInfo.get(position);
      if (!alternative.equals(EMPTY) && !this.hgncGenes.getPreviousGeneAliases()
          .containsKey(alternative)) {
        this.hgncGenes.addToPreviousGeneAliases(alternative, gene);
      }
    }
  }
}

package org.molgenis.validators;

import java.util.HashMap;
import java.util.Map;
import org.molgenis.utils.InvalidGeneException;

public class GeneValidationService {

  private static final String STATUS = "status";
  private static final String APPROVED = "approved";

  Map<String, HashMap<String, String>> genes;
  Map<String, String> geneAlternatives;

  private Map<String, HashMap<String, String>> getGenes() {
    return genes;
  }

  private Map<String, String> getGeneAlternatives() {
    return geneAlternatives;
  }

  private void setInput(Map<String, HashMap<String, String>> genes,
      Map<String, String> geneAlternatives) {
    this.genes = genes;
    this.geneAlternatives = geneAlternatives;
  }

  public GeneValidationService(Map<String, HashMap<String, String>> genes,
      Map<String, String> geneAlternatives) {
    this.setInput(genes, geneAlternatives);
  }

  protected boolean isValidGene(String gene) {
    return this.getGeneStatus(gene).equals(APPROVED);
  }

  protected String getGeneStatus(String gene) {
    HashMap<String, String> geneInfo = this.getGenes().get(gene);
    return geneInfo.get(STATUS).toLowerCase();
  }

  protected String translateGene(String gene) {
    return this.getGeneAlternatives().get(gene.toLowerCase());
  }

  public String getValidatedGene(String gene) throws InvalidGeneException {
    if (this.getGenes().containsKey(gene)) {
      if (this.isValidGene(gene)) {
        return gene;
      } else {
        throw new InvalidGeneException(
            gene + " has been found with status: " + this.getGeneStatus(gene));
      }
    } else {
      String translatedGene = this.translateGene(gene);
      if (translatedGene != null) {
        return translatedGene;
      } else {
        throw new InvalidGeneException("No valid gene symbol can be found for: " + gene);
      }
    }
  }
}

package org.molgenis.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.molgenis.utils.InvalidGeneException;

public class GeneValidationService {

  private static final String STATUS = "status";
  private static final String APPROVED = "approved";
  private static final String ERROR = "error";

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

  protected String getValidatedGene(String gene) throws InvalidGeneException {
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

  List<Map<String, String>> getVariantsWithCorrectGenesList(List<Map<String, String>> body) {
    List<Map<String, String>> validatedList = new ArrayList<>();
    for (Map<String, String> variant : body) {
      String gene = variant.get("gene_orig");
      try {
        String validatedGene = getValidatedGene(gene);
        variant.put("gene", validatedGene);
      } catch (InvalidGeneException ex) {
        variant.put(ERROR, ex.getMessage());
      }
      validatedList.add(variant);
    }
    return validatedList;
  }

  public void getVariantsWithCorrectGenes(Exchange exchange) {
    List<Map<String, String>> body = exchange.getIn().getBody(List.class);
    List<Map<String, String>> listOfUniqueVariants = getVariantsWithCorrectGenesList(body);
    exchange.getIn().setBody(listOfUniqueVariants);
  }
}

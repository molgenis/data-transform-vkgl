package org.molgenis.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.molgenis.utils.InvalidGeneException;

public class HgncGeneValidator {

  private static final String STATUS = "status";
  private static final String APPROVED = "approved";
  private static final String ERROR = "error";
  private static final String SYMBOL = "approved_symbol";

  Map<String, Map<String, String>> genes;
  Map<String, String> previousGeneAliases;

  private Map<String, Map<String, String>> getGenes() {
    return genes;
  }

  private Map<String, String> getPreviousGeneAliases() {
    return previousGeneAliases;
  }

  private void setInput(Map<String, Map<String, String>> genes,
      Map<String, String> previousGeneAliases) {
    this.genes = genes;
    this.previousGeneAliases = previousGeneAliases;
  }

  public HgncGeneValidator(Map<String, Map<String, String>> genes,
      Map<String, String> geneAlternatives) {
    this.setInput(genes, geneAlternatives);
  }

  protected boolean isValidGene(String gene) {
    return this.getGeneStatus(gene.toLowerCase()).equals(APPROVED);
  }

  protected String getGeneStatus(String gene) {
    Map<String, String> geneInfo = this.getGenes().get(gene.toLowerCase());
    return geneInfo.get(STATUS).toLowerCase();
  }

  protected String translateGene(String gene) {
    String translation = this.getPreviousGeneAliases().get(gene.toLowerCase());
    if (translation != null) {
      return this.getGenes().get(translation).get(SYMBOL);
    }
    return null;
  }

  protected String getValidatedGene(String gene) throws InvalidGeneException {
    if (this.getGenes().containsKey(gene.toLowerCase())) {
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

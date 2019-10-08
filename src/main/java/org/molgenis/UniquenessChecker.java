package org.molgenis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.camel.Exchange;

public class UniquenessChecker {

  private static HashMap<String, HashMap> uniqueVariants = new HashMap<>();

  public void getUniqueVariants(Exchange exchange) {
    List<HashMap> newExchange = new ArrayList<>();
    for (HashMap variant : (List<HashMap>) exchange.getIn().getBody(List.class)) {
      if (!variant.containsKey("error")) {
        String id = variant.get("chrom") + Integer.toString((Integer) variant.get("pos")) +
            variant.get("ref") + variant.get("alt") + variant.get("gene");
        if (uniqueVariants.containsKey(id)) {
          HashMap uniqueVariant = uniqueVariants.get(id);
          if (uniqueVariant.containsKey("error")) {
            String error = (String) uniqueVariant.get("error");
            uniqueVariant.put("error", error + "," + variant.get("hgvs_normalized_vkgl"));
          } else {
            uniqueVariant.put("error",
                "Variant duplicated: " + uniqueVariant.get("hgvs_normalized_vkgl") + "," + variant
                    .get("hgvs_normalized_vkgl"));
          }
        } else {
          uniqueVariants.put(id, variant);
        }
      } else {
        newExchange.add(variant);
      }
    }
    newExchange.addAll(uniqueVariants.values());
    exchange.getIn().setBody(newExchange);
  }
}

package org.molgenis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component
class UniquenessChecker {

  List<HashMap> getUniqueVariantsList(List<HashMap> body) {
    HashMap<String, HashMap> uniqueVariants = new HashMap<>();
    String errorKey = "error";
    String hgvsKey = "hgvs_normalized_vkgl";
    List<HashMap> listOfUniqueVariants = new ArrayList<>();
    for (HashMap variant : body) {
      if (!variant.containsKey(errorKey)) {
        String id = variant.get("chrom") + Integer.toString((Integer) variant.get("pos")) +
            variant.get("ref") + variant.get("alt") + variant.get("gene");
        if (uniqueVariants.containsKey(id)) {
          HashMap uniqueVariant = uniqueVariants.get(id);
          if (uniqueVariant.containsKey(errorKey)) {
            String error = (String) uniqueVariant.get(errorKey);
            uniqueVariant.put(errorKey, error + "," + variant.get(hgvsKey));
          } else {
            uniqueVariant.put(errorKey,
                "Variant duplicated: " + uniqueVariant.get(hgvsKey) + "," + variant
                    .get(hgvsKey));
          }
        } else {
          uniqueVariants.put(id, variant);
        }
      } else {
        listOfUniqueVariants.add(variant);
      }
    }
    listOfUniqueVariants.addAll(uniqueVariants.values());
    return listOfUniqueVariants;
  }

  void getUniqueVariants(Exchange exchange) {
    List<HashMap> body = (List<HashMap>) exchange.getIn().getBody(List.class);
    List<HashMap> listOfUniqueVariants = getUniqueVariantsList(body);
    exchange.getIn().setBody(listOfUniqueVariants);
  }
}

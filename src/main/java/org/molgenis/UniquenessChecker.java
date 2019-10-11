package org.molgenis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component
class UniquenessChecker {

  static final String ERROR = "error";

  List<Map> getUniqueVariantsList(List<Map> body) {
    Map<String, Map> uniqueVariants = new HashMap<>();
    String hgvsKey = "hgvs_normalized_vkgl";
    List<Map> listOfUniqueVariants = new ArrayList<>();
    for (Map variant : body) {
      if (!variant.containsKey(ERROR)) {
        String id = variant.get("chrom") + Integer.toString((Integer) variant.get("pos")) +
            variant.get("ref") + variant.get("alt") + variant.get("gene");
        if (uniqueVariants.containsKey(id)) {
          HashMap uniqueVariant = (HashMap) uniqueVariants.get(id);
          if (uniqueVariant.containsKey(ERROR)) {
            String error = (String) uniqueVariant.get(ERROR);
            uniqueVariant.put(ERROR, error + "," + variant.get(hgvsKey));
          } else {
            uniqueVariant.put(ERROR,
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
    List<Map> body = exchange.getIn().getBody(List.class);
    List<Map> listOfUniqueVariants = getUniqueVariantsList(body);
    exchange.getIn().setBody(listOfUniqueVariants);
  }
}

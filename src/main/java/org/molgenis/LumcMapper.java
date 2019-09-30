package org.molgenis;

import java.util.Map;

public class LumcMapper extends InputDataMapper {

  @Override
  public void mapClassification(Map body) {
    switch (body.get("variant_effect").toString()) {
      case "-":
        body.put("significance", "b");
        break;
      case "-?":
        body.put("significance", "lb");
        break;
      case "+?":
        body.put("significance", "lp");
        break;
      case "+":
        body.put("significance", "p");
        break;
      case "?":
        body.put("significance", "vus");
        break;
      default:
        body.put("error", "Unknown significance: " + body.get("variant_effect").toString());
    }
  }

  @Override
  public String getHeader() {
    return "refseq_build\tchromosome\thgvs_normalized\tvariant_effect\tgeneid\tcDNA\tProtein";
  }

  @Override
  public void mapData(Map body) {
    mapClassification(body);
    body.put("hgvs_normalized", body.get("gDNA_normalized"));
    body.remove("gDNA_normalized");
  }
}

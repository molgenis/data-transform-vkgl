package org.molgenis;

import java.util.Map;

public class RadboudMumcMapper extends InputDataMapper {

  @Override
  public void mapClassification(Map body) {
    String significance = "significance";
    switch (body.get("classification").toString()) {
      case "class 1":
        body.put(significance, "b");
        break;
      case "class 2":
        body.put(significance, "lb");
        break;
      case "class 3":
        body.put(significance, "vus");
        break;
      case "class 4":
        body.put(significance, "lp");
        break;
      case "class 5":
        body.put(significance, "p");
        break;
      default:
        body.put("error", "Unknown significance: " + body.get("classification").toString());
    }
  }

  @Override
  public String getHeader() {
    return "chromosome_orig\tstart\tstop\tref_orig\talt_orig\tgene\ttranscript\tprotein\tempty1\texon\tempty2\tclassification";
  }

  @Override
  public void mapData(Map body) {
    String start = (String) body.get("start");
    String ref = (String) body.get("ref_orig");
    String alt = (String) body.get("alt_orig");
    String chromosome = (String) body.get("chromosome_orig");
    String stop = (String) body.get("stop");

    String hgvsG = hgvsRetriever.getHgvsG(ref, alt, chromosome, getIntFromString(start),
        getIntFromString(stop));

    body.put("hgvs_normalized_vkgl", hgvsG);
  }
}

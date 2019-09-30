package org.molgenis;

import java.util.Map;

public class AlissaMapper extends InputDataMapper {

  @Override
  public void mapClassification(Map body) {
    switch (body.get("classification").toString()) {
      case "BENIGN":
        body.put("significance", "b");
        break;
      case "LIKELY_BENIGN":
        body.put("significance", "lb");
        break;
      case "VOUS":
        body.put("significance", "vus");
        break;
      case "LIKELY_PATHOGENIC":
        body.put("significance", "lp");
        break;
      case "PATHOGENIC":
        body.put("significance", "p");
        break;
      default:
        body.put("error", "Unknown significance: " + body.get("classification").toString());
    }
  }

  @Override
  public String getHeader() {
    return "timestamp\tid\tchromosome\tstart\tstop\tref_orig\talt_orig\tgene\ttranscript\tc_nomen\tp_nomen\texon\tvariant_type\tlocation\teffect\tclassification\tlast_updated_by\tlast_updated_on";
  }

  @Override
  public void mapData(Map body) {
    mapClassification(body);
    String start = (String) body.get("start");
    // Ref and alt are specified as such in alissa file
    String ref = (String) body.get("ref");
    String alt = (String) body.get("alt");
    // Save the original ref and alt in the body again, by another key
    body.put("ref_orig", ref);
    body.put("alt_orig", alt);
    // Remove the original keys to be able to use them again in the output
    body.remove("ref");
    body.remove("alt");
    String chromosome = (String) body.get("chromosome");
    String transcript = (String) body.get("transcript");
    String cDNA = (String) body.get("c_nomen");
    String protein = (String) body.get("p_nomen");
    if (protein.equals("NULL")) {
      protein = "";
    }
    String stop = (String) body.get("stop");
    String hgvs = hgvsRetriever
        .getHgvs(transcript, cDNA, ref, alt, super.getIntFromString(start), getIntFromString(stop),
            "chr" + chromosome);
    body.put("hgvs_normalized_vkgl", hgvs);
  }
}

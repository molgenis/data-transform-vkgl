package org.molgenis;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AlissaMapper extends InputDataMapper {

  public static final String ALISSA_HEADERS = "timestamp\tid\tchromosome\tstart\tstop\tref_orig\talt_orig\tgene\ttranscript\tc_nomen\tp_nomen\texon\tvariant_type\tlocation\teffect\tclassification\tlast_updated_by\tlast_updated_on";

  AlissaMapper(HgvsService hgvsService) {
    super(hgvsService);
    classificationTranslation = new HashMap<>();
    classificationTranslation.put("BENIGN", "b");
    classificationTranslation.put("LIKELY_BENIGN", "lb");
    classificationTranslation.put("VOUS", "v");
    classificationTranslation.put("LIKELY_PATHOGENIC", "lp");
    classificationTranslation.put("PATHOGENIC", "p");
  }

  @Override
  public void mapData(Map body) {
    String originalClassification = body.get("classification").toString();
    mapClassification(body, originalClassification);
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
    body.put("p_nomen", protein);
    String stop = (String) body.get("stop");
    String hgvs = hgvsService
        .getHgvs(transcript, cDNA, ref, alt, getIntFromString(start), getIntFromString(stop),
            "chr" + chromosome);
    body.put("hgvs_normalized_vkgl", hgvs);
  }
}

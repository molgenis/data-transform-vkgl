package org.molgenis.mappers;

import java.util.HashMap;
import java.util.Map;
import org.molgenis.utils.HgvsService;
import org.springframework.stereotype.Component;

@Component
public class RadboudMumcMapper extends InputDataMapper {

  public static final String RADBOUD_HEADERS = "chromosome_orig\tstart\tstop\tref_orig\talt_orig\tgene\ttranscript\tprotein\tempty1\texon\tempty2\tclassification";

  RadboudMumcMapper(HgvsService hgvsService) {
    super(hgvsService);
    classificationTranslation = new HashMap<>();
    classificationTranslation.put("class 1", "b");
    classificationTranslation.put("class 2", "lb");
    classificationTranslation.put("class 3", "vus");
    classificationTranslation.put("class 4", "lp");
    classificationTranslation.put("class 5", "p");
  }

  @Override
  public void mapData(Map body) {
    String start = (String) body.get("start");
    String ref = (String) body.get("ref_orig");
    String alt = (String) body.get("alt_orig");
    String chromosome = (String) body.get("chromosome_orig");
    String stop = (String) body.get("stop");

    String originalClassification = body.get("classification").toString();
    mapClassification(body, originalClassification);

    String hgvsG = hgvsService.getHgvsG(ref, alt, chromosome, getIntFromString(start),
        getIntFromString(stop));

    body.put("hgvs_normalized_vkgl", hgvsG);
  }
}

package org.molgenis.mappers;

import java.util.HashMap;
import java.util.Map;
import org.molgenis.utils.HgvsService;
import org.springframework.stereotype.Component;

@Component
public class LumcMapper extends InputDataMapper {

  public static final String LUMC_HEADERS = "refseq_build\tchromosome\thgvs_normalized\tvariant_effect\tgeneid\tcDNA\tProtein";

  LumcMapper(HgvsService hgvsService) {
    super(hgvsService);
    classificationTranslation = new HashMap<>();
    classificationTranslation.put("-", "b");
    classificationTranslation.put("-?", "lb");
    classificationTranslation.put("?", "vus");
    classificationTranslation.put("+?", "lp");
    classificationTranslation.put("+", "p");
  }

  @Override
  public void mapData(Map body) {
    String originalClassification = body.get("variant_effect").toString();
    mapClassification(body, originalClassification);
    body.put("hgvs_normalized_vkgl", body.get("gDNA_normalized"));
    body.remove("gDNA_normalized");
    body.put("gene", body.get("geneid"));
  }
}

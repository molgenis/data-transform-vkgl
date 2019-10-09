package org.molgenis;

import java.util.Map;

public class LumcVkglTableMapper implements VkglTableMapper {

  String[] getTranscriptAndCdna(String cDna) {
    return cDna.split(":");
  }

  @Override
  public void mapLine(Map body) {
    mapGenericPart(body);
    if (body.containsKey("cDNA")) {
      String[] transcriptAndCdna = getTranscriptAndCdna((String) body.get("cDNA"));
      body.put("c_dna", transcriptAndCdna[1]);
      body.put("transcript", transcriptAndCdna[0]);
    }
    addIfNotNull(body, "Protein", "protein");
  }
}

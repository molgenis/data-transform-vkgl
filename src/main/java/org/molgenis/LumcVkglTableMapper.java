package org.molgenis;

import java.util.Map;

public class LumcVkglTableMapper extends VkglTableMapper {

  @Override
  void addIfNotNull(Map body, String labKey, String targetKey) {
    if (body.containsKey(labKey)) {
      body.put(targetKey, body.get(labKey));
    }
  }

  private String[] getTranscriptAndCdna(String cDna) {
    return cDna.split(":");
  }

  @Override
  public void mapLine(Map body) {
    mapGenericPart(body);
    String[] transcriptAndCdna = getTranscriptAndCdna((String) body.get("cDNA"));
    body.put("c_dna", transcriptAndCdna[1]);
    body.put("transcript", transcriptAndCdna[0]);
    addIfNotNull(body, "Protein", "protein");
  }
}

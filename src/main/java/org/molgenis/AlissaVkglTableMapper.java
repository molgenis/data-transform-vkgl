package org.molgenis;

import java.util.Map;

public class AlissaVkglTableMapper implements VkglTableMapper {

  @Override
  public void addIfNotNull(Map body, String labKey, String targetKey) {
    if (body.containsKey(labKey) && !body.get(labKey).equals("NULL")) {
      body.put(targetKey, body.get(labKey));
    }
  }

  @Override
  public void mapLine(Map body) {
    mapGenericPart(body);
    addIfNotNull(body, "c_nomen", "c_dna");
    addIfNotNull(body, "transcript", "transcript");
    addIfNotNull(body, "p_nomen", "protein");
    addIfNotNull(body, "location", "location");
    addIfNotNull(body, "exon", "exon");
    addIfNotNull(body, "effect", "effect");
    addIfNotNull(body, "lab_upload_date", "last_updated_on");
  }
}

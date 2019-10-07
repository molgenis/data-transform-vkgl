package org.molgenis;

import java.util.Map;

public class RadboudMumcVkglTableMapper extends VkglTableMapper {

  @Override
  void addIfNotNull(Map body, String labKey, String targetKey) {
    if (body.containsKey(labKey)) {
      body.put(targetKey, body.get(labKey));
    }
  }

  @Override
  public void mapLine(Map body) {
    mapGenericPart(body);
    addIfNotNull(body, "transcript", "transcript");
    addIfNotNull(body, "protein", "protein");
  }
}

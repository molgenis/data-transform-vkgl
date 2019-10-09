package org.molgenis;

import java.util.Map;

public class RadboudMumcVkglTableMapper extends VkglTableMapper {

  @Override
  public void mapLine(Map body) {
    mapGenericPart(body);
  }
}

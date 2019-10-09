package org.molgenis;

import java.util.Map;

public class RadboudMumcVkglTableMapper implements VkglTableMapper {

  @Override
  public void mapLine(Map body) {
    mapGenericPart(body);
  }
}

package org.molgenis.mappers;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RadboudMumcVkglTableMapper implements VkglTableMapper {

  @Override
  public void mapLine(Map body) {
    mapGenericPart(body);
  }
}

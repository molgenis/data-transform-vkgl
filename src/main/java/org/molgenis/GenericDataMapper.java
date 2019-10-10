package org.molgenis;

import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component
class GenericDataMapper {
  private final AlissaMapper alissaMapper;
  private final LumcMapper lumcMapper;
  private final RadboudMumcMapper radboudMumcMapper;

  public GenericDataMapper(AlissaMapper alissaMapper, LumcMapper lumcMapper,
      RadboudMumcMapper radboudMumcMapper) {
    this.alissaMapper = alissaMapper;
    this.lumcMapper = lumcMapper;
    this.radboudMumcMapper = radboudMumcMapper;
  }

  static String getType(Set<String> headers) {
    if (headers.contains("gDNA_normalized")) {
      return "lumc";
    } else if (headers.contains("empty1")) {
      return "radboud";
    } else {
      return "alissa";
    }
  }

  void mapData(Exchange exchange) {
    Map<String, Object> body = (Map<String, Object>) exchange.getIn().getBody();
    Set<String> headers = body.keySet();
    String labType = getType(headers);
    exchange.getIn().getHeaders().put("labType", labType);

    if (labType.equals("lumc")) {
      lumcMapper.mapData(body);
    } else if (labType.equals("radboud")) {
      radboudMumcMapper.mapData(body);
    } else {
      alissaMapper.mapData(body);
    }
  }
}

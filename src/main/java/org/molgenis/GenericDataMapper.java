package org.molgenis;

import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;

class GenericDataMapper {

  private static final AlissaMapper alissaMapper = new AlissaMapper();
  private static final LumcMapper lumcMapper = new LumcMapper();
  private static final RadboudMumcMapper radboudMumcMapper = new RadboudMumcMapper();

  static final String LUMC_HEADERS = lumcMapper.getHeader();
  static final String RADBOUD_HEADERS = radboudMumcMapper.getHeader();
  static final String ALISSA_HEADERS = alissaMapper.getHeader();

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

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

  void mapData(Exchange exchange) {
    Map<String, Object> body = (Map<String, Object>) exchange.getIn().getBody();
    Set<String> headers = body.keySet();
    String labType = "labType";
    if (headers.contains("gDNA_normalized")) {
      lumcMapper.mapData(body);
      exchange.getIn().getHeaders().put(labType, "lumc");
    } else if (headers.contains("empty1")) {
      radboudMumcMapper.mapData(body);
      exchange.getIn().getHeaders().put(labType, "radboud");
    } else {
      alissaMapper.mapData(body);
      exchange.getIn().getHeaders().put(labType, "alissa");
    }
  }
}

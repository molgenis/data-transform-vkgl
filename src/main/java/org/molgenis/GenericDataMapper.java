package org.molgenis;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javax.validation.UnexpectedTypeException;
import org.apache.camel.Exchange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
class GenericDataMapper {

  private final AlissaMapper alissaMapper;
  private final LumcMapper lumcMapper;
  private final RadboudMumcMapper radboudMumcMapper;

  private static Log log = LogFactory.getLog(FileCreator.class);

  public GenericDataMapper(AlissaMapper alissaMapper, LumcMapper lumcMapper,
      RadboudMumcMapper radboudMumcMapper) {
    this.alissaMapper = alissaMapper;
    this.lumcMapper = lumcMapper;
    this.radboudMumcMapper = radboudMumcMapper;
  }

  private static Boolean isTypeHeader(Set<String> headers, String typeHeader) {
    return headers.containsAll(Arrays.asList(typeHeader.split("\t")));
  }

  static String getType(Set<String> headers) {
    if (isTypeHeader(headers,
        LumcMapper.LUMC_HEADERS
            .replace("hgvs_normalized", "gDNA_normalized"))) {
      return "lumc";
    } else if (isTypeHeader(headers, RadboudMumcMapper.RADBOUD_HEADERS)) {
      return "radboud";
    } else if (isTypeHeader(headers,
        AlissaMapper.ALISSA_HEADERS
            .replace("_orig", ""))) {
      return "alissa";
    } else {
      throw new UnexpectedTypeException(
          "Lab type not recognized, check headers with headers of alissa, radboud, and lumc");
    }
  }

  void mapData(Exchange exchange) {
    Map<String, Object> body = (Map<String, Object>) exchange.getIn().getBody();
    Set<String> headers = body.keySet();
    try {
      String labType = getType(headers);
      exchange.getIn().getHeaders().put("labType", labType);

      if (labType.equals("lumc")) {
        lumcMapper.mapData(body);
      } else if (labType.equals("radboud")) {
        radboudMumcMapper.mapData(body);
      } else {
        alissaMapper.mapData(body);
      }
    } catch (UnexpectedTypeException ex) {
      log.error(ex);
    }
  }
}

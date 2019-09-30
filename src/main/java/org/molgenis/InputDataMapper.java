package org.molgenis;

import java.util.Map;

abstract class InputDataMapper {

  static final HgvsService hgvsRetriever = new HgvsService();

  abstract void mapClassification(Map body);

  abstract String getHeader();

  abstract void mapData(Map body);

  int getIntFromString(String stringToConvert) {
    return Integer.parseInt(stringToConvert, 10);
  }
}

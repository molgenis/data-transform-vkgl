package org.molgenis;

import java.util.Map;

abstract class InputDataMapper {

  protected final HgvsService hgvsService;

  protected InputDataMapper(HgvsService hgvsService) {
    this.hgvsService = hgvsService;
  }

  abstract void mapData(Map body);

  int getIntFromString(String stringToConvert) {
    return Integer.parseInt(stringToConvert, 10);
  }

  Map<String, String> classificationTranslation;

  void mapClassification(Map body, String originalClassification) {
    String significance = classificationTranslation.get(originalClassification);
    if (significance != null) {
      body.put("significance", significance);
    } else {
      body.put("error", "Unknown significance: " + originalClassification);
    }
  }
}

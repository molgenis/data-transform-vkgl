package org.molgenis;

import java.util.Map;

public class ReferenceSequenceValidator {

  static boolean matchesOriginalRef(String refOrig, String ref, int startOrig, int start) {
    int diff = startOrig - start;
    if (!refOrig.equals(".") && (diff == 0 || diff == 1)) {
      for (int i = 0; i < ref.length() - diff; i++) {
        if (ref.charAt(i + diff) != refOrig.charAt(i)) {
          return false;
        }
      }
    }
    return true;
  }

  void validateOriginalRef(Map<String, Object> body) {
    if (!body.containsKey("error")) {
      String ref = (String) body.get("ref");
      String refOrig = (String) body.get("ref_orig");
      int startOrig = Integer.parseInt((String) body.get("start"), 10);
      int start = (int) body.get("pos");
      if (!matchesOriginalRef(refOrig, ref, startOrig, start)) {
        body.put("error", "Incorrect original reference");
      }
    }
  }
}

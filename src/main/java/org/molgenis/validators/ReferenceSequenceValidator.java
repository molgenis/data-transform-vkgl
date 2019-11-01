package org.molgenis.validators;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReferenceSequenceValidator {

  static final String ERROR = "error";

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

  public void validateOriginalRef(Map<String, Object> body) {
    if (!body.containsKey(ERROR) && body.containsKey("ref_orig")) {
      String ref = (String) body.get("ref");
      String refOrig = (String) body.get("ref_orig");
      int startOrig = Integer.parseInt((String) body.get("start"), 10);
      int start = (int) body.get("pos");
      if (!matchesOriginalRef(refOrig, ref, startOrig, start)) {
        body.put(ERROR, "Incorrect original reference");
      }
    } else if (body.containsKey(ERROR) && body.get("chromosome").equals("MT")) {
      String ref = (String) body.get("ref_orig");
      String alt = (String) body.get("alt_orig");
      if (!ref.equals(".") && !alt.equals(".")) {
        body.remove(ERROR);
        body.put("chrom", body.get("chromosome"));
        body.put("pos", Integer.parseInt((String) body.get("start"), 10));
        body.put("ref", ref);
        body.put("alt", alt);
        if (ref.length() == 1 && alt.length() == 1) {
          body.put("type", "sub");
        } else if (ref.length() > alt.length()) {
          body.put("type", "del");
        } else if (alt.length() > ref.length()) {
          body.put("type", "ins");
        } else {
          body.put("type", "delins");
        }
      } else {
        body.put(ERROR, "Cannot retrieve anchor for variants on MT chromosome");
      }
    }
  }
}

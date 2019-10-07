package org.molgenis;

import java.util.ArrayList;
import java.util.Map;

public abstract class VkglTableMapper {

  private String getId(String ref, String alt, String chr, String pos, String gene) {
    return chr + "_" + pos + "_" + ref + "_" + alt + "_" + gene;
  }

  private ArrayList<String> getCorrectedRefAndAlt(String ref, String alt, String type) {
    ArrayList<String> corrected = new ArrayList<>();
    if (type.equals("sub")) {
      corrected.add(ref.substring(1));
      corrected.add(alt.substring(1));
    } else {
      corrected.add(ref);
      corrected.add(alt);
    }
    return corrected;
  }

  private String getHgvsType(String hgvs) {
    if (hgvs.startsWith("NC")) {
      return "hgvs_g";
    } else {
      return "hgvs_c";
    }
  }

  private String getStopPosition(String startPosition, String ref) {
    int stop = Integer.parseInt(startPosition) + ref.length() - 1;
    return Integer.toString(stop);
  }

  abstract void addIfNotNull(Map body, String labKey, String targetKey);

  public abstract void mapLine(Map body);

  void mapGenericPart(Map body) {
    String ref_orig = (String) body.get("ref");
    String alt_orig = (String) body.get("alt");

    String type = (String) body.get("type");
    String chromosome = (String) body.get("chrom");
    String start = Integer.toString((int) body.get("pos"));
    String gene = (String) body.get("gene");
    body.put("type", type);
    body.put("chromosome", chromosome);
    body.put("start", start);

    ArrayList<String> refAndAlt = getCorrectedRefAndAlt(ref_orig, alt_orig, type);
    String ref = refAndAlt.get(0);
    body.put("ref", ref);
    String alt = refAndAlt.get(1);
    body.put("alt", alt);
    String id = getId(ref, alt, chromosome, start, gene);
    body.put("id", id);

    String classification = (String) body.get("significance");
    body.put("classification", classification);

    String hgvs = (String) body.get("hgvs_normalized_vkgl");
    body.put(getHgvsType(hgvs), hgvs);

    String stop = getStopPosition(start, ref);
    body.put("stop", stop);
  }
}

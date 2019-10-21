package org.molgenis.mappers;

import java.util.HashMap;
import java.util.Map;
import org.molgenis.utils.Hasher;

public interface VkglTableMapper {

  default String getId(String ref, String alt, String chr, String pos, String gene) {
    String id = chr + "_" + pos + "_" + ref + "_" + alt + "_" + gene;
    return Hasher.hash(id);
  }

  default Map<String, String> getCorrectedRefAndAlt(String ref, String alt, String type,
      int start) {
    HashMap<String, String> corrected = new HashMap<>();
    if (type.equals("sub") && ref.length() == 2 && alt.length() == 2) {
      ref = ref.substring(1);
      alt = alt.substring(1);
      start += 1;
    }
    corrected.put("ref", ref);
    corrected.put("alt", alt);
    corrected.put("start", Integer.toString(start));
    return corrected;
  }

  default String getHgvsType(String hgvs) {
    if (hgvs.startsWith("NC")) {
      return "hgvs_g";
    } else {
      return "hgvs_c";
    }
  }

  default String getStopPosition(int startPosition, String ref) {
    int stop = startPosition + ref.length() - 1;
    return Integer.toString(stop);
  }

  default void addIfNotNull(Map body, String labKey, String targetKey) {
    if (body.containsKey(labKey)) {
      body.put(targetKey, body.get(labKey));
    }
  }

  void mapLine(Map body);

  default void mapGenericPart(Map<String, Object> body) {
    String refOrig = (String) body.get("ref");
    String altOrig = (String) body.get("alt");

    String type = (String) body.get("type");
    String chromosome = (String) body.get("chrom");
    int start = (int) body.get("pos");
    String gene = (String) body.get("gene");
    body.put("type", type);
    body.put("chromosome", chromosome);

    Map<String, String> corrected = getCorrectedRefAndAlt(refOrig, altOrig, type, start);
    body.putAll(corrected);

    String id = getId(corrected.get("ref"), corrected.get("alt"), chromosome,
        Integer.toString(start), gene);
    body.put("id", id);

    String classification = (String) body.get("significance");
    body.put("classification", classification);

    String hgvs = (String) body.get("hgvs_normalized_vkgl");
    body.put(getHgvsType(hgvs), hgvs);

    String stop = getStopPosition(start, corrected.get("ref"));
    body.put("stop", stop);
  }
}

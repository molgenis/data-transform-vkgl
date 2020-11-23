package org.molgenis.utils;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HgvsService {

  private static Map<String, String> chromosomeTranscripts;
  private static final String DELINS = "delins";

  static {
    chromosomeTranscripts = new HashMap<>();
    chromosomeTranscripts.put("chr1", "NC_000001.10");
    chromosomeTranscripts.put("chr2", "NC_000002.11");
    chromosomeTranscripts.put("chr3", "NC_000003.11");
    chromosomeTranscripts.put("chr4", "NC_000004.11");
    chromosomeTranscripts.put("chr5", "NC_000005.9");
    chromosomeTranscripts.put("chr6", "NC_000006.11");
    chromosomeTranscripts.put("chr7", "NC_000007.13");
    chromosomeTranscripts.put("chr8", "NC_000008.10");
    chromosomeTranscripts.put("chr9", "NC_000009.11");
    chromosomeTranscripts.put("chr10", "NC_000010.10");
    chromosomeTranscripts.put("chr11", "NC_000011.9");
    chromosomeTranscripts.put("chr12", "NC_000012.11");
    chromosomeTranscripts.put("chr13", "NC_000013.10");
    chromosomeTranscripts.put("chr14", "NC_000014.8");
    chromosomeTranscripts.put("chr15", "NC_000015.9");
    chromosomeTranscripts.put("chr16", "NC_000016.9");
    chromosomeTranscripts.put("chr17", "NC_000017.10");
    chromosomeTranscripts.put("chr18", "NC_000018.9");
    chromosomeTranscripts.put("chr19", "NC_000019.9");
    chromosomeTranscripts.put("chr20", "NC_000020.10");
    chromosomeTranscripts.put("chr21", "NC_000021.8");
    chromosomeTranscripts.put("chr22", "NC_000022.10");
    chromosomeTranscripts.put("chrX", "NC_000023.10");
    chromosomeTranscripts.put("chrY", "NC_000024.9");
  }

  private String getTranscriptFromChromosome(String chromosome) {
    return chromosomeTranscripts.get(chromosome);
  }

  private String getHgvsGForSnp(String transcript, String start, String ref, String alt) {
    return transcript + ":g." + start + ref + ">" + alt;
  }

  private String getHgvsGForDelIns(String transcript, String start, String seq, String stop,
      String type) {
    return transcript + ":g." + start + "_" + stop + type + seq;
  }

  private boolean isEmptyValue(String value) {
    return value.isEmpty() || value.equals("NULL") || value.equals(".");
  }

  @SuppressWarnings("squid:CommentedOutCodeLine")
  public String getHgvs(String nmTranscript, String cDNA, String ref, String alt, int start,
      int stop, String chromosome) {
    String hgvs;
    // if (ref and alt not empty) or (transcript+cDNA would be invalid) and chromosome != MT
    if ((!(isEmptyValue(ref) && isEmptyValue(alt)) || (isEmptyValue(nmTranscript) || isEmptyValue(
        cDNA) || nmTranscript.startsWith("NC"))) && !"chrMT".equals(chromosome)) {
      hgvs = getHgvsG(ref, alt, chromosome, start, stop);
    } else {
      hgvs = nmTranscript + ":" + cDNA;
    }
    return hgvs;
  }

  private String getHgvsGForDeletions(String ref, String alt, int start, int stop,
      String transcript) {
    if (!alt.equals(".")) {
      if (alt.charAt(0) == ref.charAt(0)) {
        start = start + 1;
        ref = ref.substring(1);
      } else if (ref.charAt(ref.length() - 1) == alt.charAt(0)) {
        stop = stop - 1;
        ref = ref.substring(0, ref.length() - 1);
      } else {
        return getHgvsGForDelIns(transcript, Integer.toString(start), alt,
            Integer.toString(stop), DELINS);
      }
    }
    return getHgvsGForDelIns(transcript, Integer.toString(start), ref, Integer.toString(stop),
        "del");
  }

  private String getHgvsGForInsertions(String ref, String alt, int start, String transcript) {
    if (!ref.equals(".")) {
      if (alt.charAt(0) == ref.charAt(0)) {
        alt = alt.substring(1);
        start = start + 1;
      } else if (alt.charAt(alt.length() - 1) == ref.charAt(0)) {
        alt = alt.substring(0, alt.length() - 1);
        start = start - 1;
      } else {
        int stop = start;
        return getHgvsGForDelIns(transcript, Integer.toString(start), alt,
            Integer.toString(stop), DELINS);
      }
    }
    // prevent insertion length must be 1 error
    int stop = start + 1;
    return getHgvsGForDelIns(transcript, Integer.toString(start), alt,
        Integer.toString(stop), "ins");
  }

  public String getHgvsG(String ref, String alt, String chromosome, int start, int stop) {
    String transcript = getTranscriptFromChromosome(chromosome);
    if (ref.length() == 1 && alt.length() == 1 && !isEmptyValue(ref) && !isEmptyValue(alt)) {
      return getHgvsGForSnp(transcript, Integer.toString(start), ref, alt);
    } else if ((ref.length() > 1 && alt.length() == 1) || alt.equals(".")) {
      return getHgvsGForDeletions(ref, alt, start, stop, transcript);
    } else if ((ref.length() == 1 && alt.length() > 1) || ref.equals(".")) {
      return getHgvsGForInsertions(ref, alt, start, transcript);
    } else {
      return getHgvsGForDelIns(transcript, Integer.toString(start), alt,
          Integer.toString(stop), DELINS);
    }
  }
}

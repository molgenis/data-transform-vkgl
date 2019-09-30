package org.molgenis;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.apache.camel.util.toolbox.AggregationStrategies.groupedBody;

import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;

@Component
public class MySpringBootRouter extends RouteBuilder {

  private static final String LUMC_HEADERS = "refseq_build\tchromosome\thgvs_normalized\tvariant_effect\tgeneid\tcDNA\tProtein";

  private static final String VCF_HEADERS = "hgvs_normalized_vkgl\tchrom\tpos\tref\talt\tsignificance";

  private static final String RADBOUD_HEADERS = "chromosome_orig\tstart_orig\tstop_orig\tref_orig\talt_orig\tgene\ttranscript\tprotein\tempty1\texon\tempty2\tclassification";

  private static final String ALISSA_HEADERS = "timestamp\tid\tchromosome\tstart\tstop\tref_orig\talt_orig\tgene\ttranscript\tc_nomen\tp_nomen\texon\tvariant_type\tlocation\teffect\tclassification\tlast_updated_by\tlast_updated_on";

  private static final HgvsService HgvsRetriever = new HgvsService();

  private Exchange mergeLists(Exchange variantExchange, Exchange responseExchange) {
    List<Map<String, Object>> variants = variantExchange.getIn().getBody(List.class);
    List<Map<String, Object>> validationResults = responseExchange.getIn().getBody(List.class);
    for (int i = 0; i < variants.size(); i++) {
      variants.get(i).putAll(validationResults.get(i));
    }
    return variantExchange;
  }

  private void lumc(Map body) {
    switch (body.get("variant_effect").toString()) {
      case "-":
        body.put("significance", "b");
        break;
      case "-?":
        body.put("significance", "lb");
        break;
      case "+?":
        body.put("significance", "lp");
        break;
      case "+":
        body.put("significance", "p");
        break;
      case "?":
        body.put("significance", "vus");
        break;
      default:
        body.put("error", "Unknown significance: " + body.get("variant_effect").toString());
    }
  }

  public static boolean matchesOriginalRef(String refOrig, String ref, int startOrig, int start) {
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

  private void validateOriginalRef(Map<String, Object> body) {
    String ref = (String) body.get("ref");
    String refOrig = (String) body.get("ref_orig");
    int startOrig = Integer.parseInt((String) body.get("start"), 10);
    int start = (int) body.get("pos");
    if (!matchesOriginalRef(refOrig, ref, startOrig, start)) {
      body.put("error", "Incorrect original reference");
    }
  }

  private void radboud(Map body) {
    switch (body.get("classification").toString()) {
      case "class 1":
        body.put("significance", "b");
        break;
      case "class 2":
        body.put("significance", "lb");
        break;
      case "class 3":
        body.put("significance", "vus");
        break;
      case "class 4":
        body.put("significance", "lp");
        break;
      case "class 5":
        body.put("significance", "p");
        break;
      default:
        body.put("error", "Unknown significance: " + body.get("classification").toString());
    }

    String start = (String) body.get("start_orig");
    String ref = (String) body.get("ref_orig");
    String alt = (String) body.get("alt_orig");
    String chromosome = (String) body.get("chromosome_orig");
    String stop = (String) body.get("stop_orig");

    String hgvs_g = HgvsRetriever.getHgvsG(ref, alt, chromosome, getIntFromString(start),
        getIntFromString(stop));

    body.put("hgvs_normalized_vkgl", hgvs_g);
  }

  private int getIntFromString(String stringToConvert) {
    return Integer.parseInt(stringToConvert, 10);
  }

  private void alissa(Map body) {
    switch (body.get("classification").toString()) {
      case "BENIGN":
        body.put("significance", "b");
        break;
      case "LIKELY_BENIGN":
        body.put("significance", "lb");
        break;
      case "VOUS":
        body.put("significance", "vus");
        break;
      case "LIKELY_PATHOGENIC":
        body.put("significance", "lp");
        break;
      case "PATHOGENIC":
        body.put("significance", "p");
        break;
      default:
        body.put("error", "Unknown significance: " + body.get("classification").toString());
    }
    String start = (String) body.get("start");
    // Ref and alt are specified as such in alissa file
    String ref = (String) body.get("ref");
    String alt = (String) body.get("alt");
    // Save the original ref and alt in the body again, under another key
    body.put("ref_orig", ref);
    body.put("alt_orig", alt);
    // Remove the original keys to be able to use them again in the output
    body.remove("ref");
    body.remove("alt");
    String chromosome = (String) body.get("chromosome");
    String transcript = (String) body.get("transcript");
    String cDNA = (String) body.get("c_nomen");
    String protein = (String) body.get("p_nomen");
    if (protein.equals("NULL")) {
      protein = "";
    }
    String stop = (String) body.get("stop");
    String hgvs = HgvsRetriever
        .getHgvs(transcript, cDNA, ref, alt, getIntFromString(start), getIntFromString(stop),
            "chr" + chromosome);
    body.put("hgvs_normalized_vkgl", hgvs);
  }

  @Override
  public void configure() {
    from("direct:write-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(
                    (ALISSA_HEADERS + "\thgvs_normalized_vkgl\tcdna_patched\terror").split("\t"))
                .setHeaderDisabled(true))
        .to("file:result?fileExist=Append");

    from("direct:write-result")
        .aggregate(header(FILE_NAME))
        .strategy(groupedBody())
        .completionTimeout(30000)
        .to("log:done")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((ALISSA_HEADERS + '\t' + VCF_HEADERS).split("\t")))
        .to("file:result");

    from("direct:h2v")
        .to("log:httprequest")
        .transform()
        .jsonpath("$[*].hgvs_normalized_vkgl")
        .marshal()
        .json(Jackson)
        .setHeader(HTTP_METHOD, constant("POST"))
        .to("https4://variants.edge.molgenis.org/h2v?keep_left_anchor=True&strict=True")
        .unmarshal()
        .json(Jackson)
        .to("log:httpresponse");

    from("direct:hgvs2vcf")
        .description("Validates the normalized gDNA.")
        .aggregate(header(FILE_NAME), groupedBody())
        .completionSize(1000)
        .completionTimeout(1000)
        .enrich("direct:h2v", this::mergeLists)
        .split()
        .body()
        // here postprocessing
        .process().body(Map.class, this::validateOriginalRef)
        .choice().when(simple("${body['error']} != null"))
        .to("log:error")
        .setHeader(FILE_NAME, constant("test_error.txt"))
        .to("direct:write-error")
        .otherwise()
        .to("direct:write-result")
        .end()
        .end();

//    from("file:src/test/resources/?fileName=radboud.tsv&noop=true")
//        .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true).setHeader(
//            RADBOUD_HEADERS.split("\t")))
//        .split().body()
//        .process().body(Map.class, this::radboud)
//        .to("direct:hgvs2vcf");

//    from("file:src/test/resources/?fileName=lumc.txt&noop=true")
//        .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true))
//        .split().body()
//        .process().body(Map.class, this::lumc)
//        .to("direct:hgvs2vcf");

    from("file:src/test/resources/?fileName=test.txt&noop=true")
        .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true))
        .split().body()
        .process().body(Map.class, this::alissa)
        .to("direct:hgvs2vcf");
  }
}

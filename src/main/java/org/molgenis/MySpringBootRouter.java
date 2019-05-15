package org.molgenis;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.apache.camel.util.toolbox.AggregationStrategies.groupedBody;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;

@Component
public class MySpringBootRouter extends RouteBuilder {

  private static final String LUMC_HEADERS = "refseq_build\tchromosome\tgDNA_normalized\tvariant_effect\tgeneid\tcDNA\tProtein";

  private static final String VCF_HEADERS = "chrom\tpos\tref\talt\tsignificance";

  private static final String RADBOUD_HEADERS = "chromosome_orig\tstart_orig\tstop_orig\tref_orig\talt_orig\tgene\tcdna\ttranscript\tprotein\tempty1\tempty2\texon\tempty3\tclassification";

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
        break;default:
      body.put("error", "Unknown significance: " + body.get("variant_effect").toString());
    }
  }

  Pattern p = Pattern.compile("[A-Z]{2}_\\d+\\.\\d+(\\(.+\\)):[ncg].*");

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
    String hgvc = (String) body.get("cdna");
    Matcher matcher = p.matcher(hgvc);
    if(matcher.matches()){
      hgvc = hgvc.substring(matcher.start(), matcher.start(1)) +
          hgvc.substring(matcher.end(1), matcher.end());
    }
    body.put("cdna_patched", hgvc);
  }

  @Override
  public void configure() {

    from("direct:write-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader((RADBOUD_HEADERS + "\tcdna_patched\terror").split("\t"))
                .setHeaderDisabled(true))
        .to("file:result?fileExist=Append");

    from("direct:write-result")
        .aggregate(header(FILE_NAME))
        .strategy(groupedBody())
        .completionTimeout(30000)
        .to("log:done")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((RADBOUD_HEADERS+"\tcdna_patched"+VCF_HEADERS).split("\t")))
        .to("file:result");

    from("direct:h2v")
        .to("log:httprequest")
        .transform()
        .jsonpath("$[*].cdna_patched")
        .marshal()
        .json(Jackson)
        .setHeader(HTTP_METHOD, constant("POST"))
        .to("http4://localhost:1234/h2v?keep_left_anchor=False")
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
        .choice().when(simple("${body['error']} != null"))
          .to("log:error")
          .setHeader(FILE_NAME, constant("error.txt"))
          .to("direct:write-error")
        .otherwise()
          .to("direct:write-result")
        .end()
        .end();

    from("file:src/test/resources/?fileName=radboud.tsv&noop=true")
        .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true).setHeader(
            RADBOUD_HEADERS.split("\t")))
        .split().body()
        .process().body(Map.class, this::radboud)
        .to("direct:hgvs2vcf");

//    from("file:src/test/resources/?fileName=lumc.txt&noop=true")
//        .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true))
//        .split().body()
//        .process().body(Map.class, this::lumc)
//        .to("direct:hgvs2vcf");
  }
}

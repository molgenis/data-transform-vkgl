package org.molgenis;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.apache.camel.util.toolbox.AggregationStrategies.groupedBody;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;

@Component
public class MySpringBootRouter extends RouteBuilder {

  private static final String[] HEADERS =
      "geneid\tchrom\tpos\tref\talt\tsignificance\tgenomic_variant_error".split("\t");

  /**
   * Adds the validation response to the variant map.
   *
   * @param variantExchange Exchange containing a variant in a Map in the body
   * @param responseExchange Exchange containing the validation response in a Map in the body
   * @return variantExchange with the validation response added to the body
   */
  private Exchange putAllFromResponse(Exchange variantExchange, Exchange responseExchange) {
    Map variant = variantExchange.getIn().getBody(Map.class);
    Map validationResult = responseExchange.getIn().getBody(Map.class);
    String gDNA = (String) variant.get("gDNA_normalized");
    Map response = (Map) ((Map) validationResult.get(gDNA)).get(gDNA);
    variant.putAll(response);
    return variantExchange;
  }

  private void splitVcf(Map body) {
    String[] parts = body.get("p_vcf").toString().split(":");
    body.put("chrom", parts[0]);
    body.put("pos", parts[1]);
    body.put("ref", parts[2]);
    body.put("alt", parts[3]);
  }

  private void significance(Map body) {
    switch (body.get("variant_effect").toString()) {
      //TODO: use proper conversion here
      case "-":
        body.put("significance", "pathogenic");
        break;
      case "-?":
        body.put("significance", "likely_pathogenic");
        break;
      case "+?":
        body.put("significance", "likely_benign");
        break;
      case "+":
        body.put("significance", "benign");
        break;
      case "?":
        body.put("significance", "vus");
        break;
    }
  }

  @Override
  public void configure() {
    from("direct:validate")
        .description("Validates the normalized gDNA.")
        .transform(jsonpath("gDNA_normalized"))
        .setHeader(HTTP_METHOD, constant("GET"))
        .toD("https4://rest.variantvalidator.org/variantformatter/GRCh37/${body}/refseq/None/True")
        .unmarshal().json(Jackson);

    from("file:src/test/resources/?fileName=lumc-head.tsv&noop=true")
        .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true))
        .split(body())
        .process().body(Map.class, this::significance)
        .enrich("direct:validate", this::putAllFromResponse)
        .choice().when().jsonpath("$.genomic_variant_error")
        .setHeader(FILE_NAME, constant("lumc-error.tsv"))
        .otherwise()
          .process().body(Map.class, this::splitVcf)
        .end()
        .aggregate()
          .header(FILE_NAME)
          .completionTimeout(100000)
          .aggregationStrategy(groupedBody())
          .to("log:grouped")
          .marshal(new CsvDataFormat().setDelimiter('\t').setHeader(HEADERS))
          .to("file:result")
        .end();
  }
}

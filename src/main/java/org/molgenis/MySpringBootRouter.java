package org.molgenis;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.apache.camel.util.toolbox.AggregationStrategies.groupedBody;
import static org.molgenis.GenericDataMapper.ALISSA_HEADERS;
import static org.molgenis.GenericDataMapper.LUMC_HEADERS;
import static org.molgenis.GenericDataMapper.RADBOUD_HEADERS;

import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;

@Component
public class MySpringBootRouter extends RouteBuilder {

  private static final String VCF_HEADERS = "hgvs_normalized_vkgl\tchrom\tpos\tref\talt\ttype\tsignificance";
  public static final String ERROR_HEADERS = "hgvs_normalized_vkgl\tcdna_patched\terror";

  private static final ReferenceSequenceValidator refValidator = new ReferenceSequenceValidator();

  private static final GenericDataMapper genericMapper = new GenericDataMapper();

  private Exchange mergeLists(Exchange variantExchange, Exchange responseExchange) {
    List<Map<String, Object>> variants = variantExchange.getIn().getBody(List.class);
    List<Map<String, Object>> validationResults = responseExchange.getIn().getBody(List.class);
    for (int i = 0; i < variants.size(); i++) {
      variants.get(i).putAll(validationResults.get(i));
    }
    return variantExchange;
  }

  @Override
  public void configure() {
    from("direct:write-alissa-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(
                    (ALISSA_HEADERS + "\t" + ERROR_HEADERS).split("\t"))
                .setHeaderDisabled(true))
        .to("file:result?fileExist=Append");

    from("direct:write-radboud-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(
                    (RADBOUD_HEADERS + "\t" + ERROR_HEADERS).split("\t"))
                .setHeaderDisabled(true))
        .to("file:result?fileExist=Append");

    from("direct:write-lumc-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(
                    (LUMC_HEADERS + "\t" + ERROR_HEADERS).split("\t"))
                .setHeaderDisabled(true))
        .to("file:result?fileExist=Append");

    from("direct:write-error")
        .setHeader(FILE_NAME, simple("${header.CamelFileName}.error"))
        .recipientList(simple("direct:write-${header.labType}-error"));

    from("direct:marshall-alissa-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((ALISSA_HEADERS + '\t' + VCF_HEADERS).split("\t")))
        .to("file:result");

    from("direct:marshall-radboud-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((RADBOUD_HEADERS + '\t' + VCF_HEADERS).split("\t")))
        .to("file:result");

    from("direct:marshall-lumc-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((LUMC_HEADERS + '\t' + VCF_HEADERS).split("\t")))
        .to("file:result");

    from("direct:write-result")
        .aggregate(header(FILE_NAME))
        .strategy(groupedBody())
        .completionTimeout(60000)
        .to("log:done")
        .recipientList(simple("direct:marshall-${header.labType}-result"));

    from("direct:h2v")
        .to("log:httprequest")
        .transform()
        .jsonpath("$[*].hgvs_normalized_vkgl")
        .marshal()
        .json(Jackson)
        .setHeader(HTTP_METHOD, constant("POST"))
        .to("https4://variants.molgenis.org/h2v?keep_left_anchor=True&strict=True")
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
        .process().body(Map.class, refValidator::validateOriginalRef)
        .choice().when(simple("${body['error']} != null"))
        .to("log:error")
        .to("direct:write-error")
        .otherwise()
        .to("direct:write-result")
        .end()
        .end();

    from("file:src/test/inbox/")
        .choice().when(simple("${header.CamelFileName} contains 'radboud'"))
        .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true).setHeader(
            RADBOUD_HEADERS.split("\t"))).otherwise()
        .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true))
        .end()
        .split().body()
        .process().exchange(genericMapper::mapData)
        .to("direct:hgvs2vcf");
  }
}

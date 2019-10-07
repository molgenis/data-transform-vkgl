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

  private static final int COMPLETION_TIMEOUT = 10000;
  private static final String VCF_HEADERS = "hgvs_normalized_vkgl\tchrom\tpos\tref\talt\ttype\tsignificance";
  private static final String ERROR_HEADERS = "hgvs_normalized_vkgl\tcdna_patched\terror";
  private static final String VKGL_HEADERS = "id\tchromosome\tstart\tstop\tref\talt\tgene\tc_dna\thgvs_g\thgvs_c\ttranscript\tprotein\ttype\tlocation\texon\teffect\tclassification\tcomments\tis_legacy";

  private static final ReferenceSequenceValidator refValidator = new ReferenceSequenceValidator();

  private static final GenericDataMapper genericMapper = new GenericDataMapper();

  private static final AlissaVkglTableMapper alissaTableMapper = new AlissaVkglTableMapper();
  private static final RadboudMumcVkglTableMapper radboudMumcTableMapper = new RadboudMumcVkglTableMapper();
  private static final LumcVkglTableMapper lumcTableMapper = new LumcVkglTableMapper();

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
    String resultFile = "file:result";
    String errorFile = "file:result?fileExist=Append";
    from("direct:write-alissa-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(
                    (ALISSA_HEADERS + "\t" + ERROR_HEADERS).split("\t"))
                .setHeaderDisabled(true))
        .to(errorFile);

    from("direct:write-radboud-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(
                    (RADBOUD_HEADERS + "\t" + ERROR_HEADERS).split("\t"))
                .setHeaderDisabled(true))
        .to(errorFile);

    from("direct:write-lumc-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(
                    (LUMC_HEADERS + "\t" + ERROR_HEADERS).split("\t"))
                .setHeaderDisabled(true))
        .to(errorFile);

    from("direct:write-error")
        .setHeader(FILE_NAME, simple("vkgl_${file:name.noext}_error.txt"))
        .recipientList(simple("direct:write-${header.labType}-error"));

    from("direct:marshal-alissa-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((ALISSA_HEADERS + '\t' + VCF_HEADERS).split("\t")))
        .to(resultFile);

    from("direct:marshal-radboud-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((RADBOUD_HEADERS + '\t' + VCF_HEADERS).split("\t")))
        .to(resultFile);

    from("direct:marshal-lumc-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((LUMC_HEADERS + '\t' + VCF_HEADERS).split("\t")))
        .to(resultFile);

    from("direct:marshal-vkgl-result")
        .setHeader(FILE_NAME, simple("vkgl_${file:name.noext}.tsv"))
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((VKGL_HEADERS).split("\t")))
        .to(resultFile);

    from("direct:map-alissa-result")
        .split()
        .body()
        .process().body(Map.class, alissaTableMapper::mapLine)
        .to("direct:marshal-vkgl-result");

    from("direct:map-lumc-result")
        .split()
        .body()
        .process().body(Map.class, lumcTableMapper::mapLine)
        .to("direct:marshal-vkgl-result");

    from("direct:map-radboud-result")
        .split()
        .body()
        .process().body(Map.class, radboudMumcTableMapper::mapLine)
        .to("direct:marshal-vkgl-result");

    from("direct:write-result")
        .aggregate(header(FILE_NAME))
        .strategy(groupedBody())
        .completionTimeout(COMPLETION_TIMEOUT)
        .to("log:done")
        .recipientList(simple(
            "direct:marshal-${header.labType}-result,direct:map-${header.labType}-result"));

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

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

  private static final int FILE_COMPLETION_TIMEOUT = 60000;
  private static final int DEFAULT_TIMEOUT = 1000;
  private static final String VCF_HEADERS = "hgvs_normalized_vkgl\tchrom\tpos\tref\talt\ttype\tsignificance";
  private static final String ERROR_HEADERS = "hgvs_normalized_vkgl\tcdna_patched\terror";
  private static final String VKGL_HEADERS = "id\tchromosome\tstart\tstop\tref\talt\tgene\tc_dna\thgvs_g\thgvs_c\ttranscript\tprotein\ttype\tlocation\texon\teffect\tclassification\tcomments\tis_legacy";


  private Exchange mergeLists(Exchange variantExchange, Exchange responseExchange) {
    List<Map<String, Object>> variants = variantExchange.getIn().getBody(List.class);
    List<Map<String, Object>> validationResults = responseExchange.getIn().getBody(List.class);
    for (int i = 0; i < variants.size(); i++) {
      variants.get(i).putAll(validationResults.get(i));
    }
    return variantExchange;
  }

  private String[] getSplittedHeaders(String customHeaders, String defaultHeaders) {
    return (customHeaders + "\t" + defaultHeaders).split("\t");
  }

  @Override
  public void configure() {
    String resultFile = "file:result";
    String appendErrorFile = "file:result?fileName=vkgl_${file:name.noext}_error.txt&fileExist=Append";
    String marshalVkglResults = "direct:marshal-vkgl-result";

    ReferenceSequenceValidator refValidator = new ReferenceSequenceValidator();
    GenericDataMapper genericMapper = new GenericDataMapper();
    AlissaVkglTableMapper alissaTableMapper = new AlissaVkglTableMapper();
    RadboudMumcVkglTableMapper radboudMumcTableMapper = new RadboudMumcVkglTableMapper();
    LumcVkglTableMapper lumcTableMapper = new LumcVkglTableMapper();
    UniquenessChecker uniquenessChecker = new UniquenessChecker();

    from("direct:write-alissa-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(getSplittedHeaders(ALISSA_HEADERS, ERROR_HEADERS))
                .setHeaderDisabled(true))
        .to(appendErrorFile);

    from("direct:write-radboud-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(getSplittedHeaders(RADBOUD_HEADERS, ERROR_HEADERS))
                .setHeaderDisabled(true))
        .to(appendErrorFile);

    from("direct:write-lumc-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(getSplittedHeaders(LUMC_HEADERS, ERROR_HEADERS))
                .setHeaderDisabled(true))
        .to(appendErrorFile);

    from("direct:write-error")
        .recipientList(simple("direct:write-${header.labType}-error"));

    from("direct:marshal-alissa-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader(getSplittedHeaders(ALISSA_HEADERS, VCF_HEADERS)))
        .to(resultFile);

    from("direct:marshal-radboud-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader(getSplittedHeaders(RADBOUD_HEADERS, VCF_HEADERS)))
        .to(resultFile);

    from("direct:marshal-lumc-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader(getSplittedHeaders(LUMC_HEADERS, VCF_HEADERS)))
        .to(resultFile);

    from("direct:marshal-vkgl-result")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((VKGL_HEADERS).split("\t"))
            .setHeaderDisabled(true))
        .to("file:result?fileName=vkgl_${file:name.noext}.tsv&fileExist=Append");

    from("direct:map-alissa-result")
        .split().body()
        .process().body(Map.class, alissaTableMapper::mapLine)
        .to(marshalVkglResults);

    from("direct:map-lumc-result")
        .split().body()
        .process().body(Map.class, lumcTableMapper::mapLine)
        .to(marshalVkglResults);

    from("direct:map-radboud-result")
        .split().body()
        .process().body(Map.class, radboudMumcTableMapper::mapLine)
        .to(marshalVkglResults);

    from("direct:write-result")
        .aggregate(header(FILE_NAME))
        .strategy(groupedBody())
        .completionTimeout(DEFAULT_TIMEOUT)
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
        .completionSize(DEFAULT_TIMEOUT)
        .completionTimeout(DEFAULT_TIMEOUT)
        .enrich("direct:h2v", this::mergeLists)
        .split().body()
        .process().body(Map.class, refValidator::validateOriginalRef)
        .aggregate(header(FILE_NAME))
        .strategy(groupedBody())
        .completionTimeout(FILE_COMPLETION_TIMEOUT)
        .process(uniquenessChecker::getUniqueVariants)
        .split().body()
        .choice().when(simple("${body['error']} != null"))
        .to("log:error")
        .to("direct:write-error")
        .otherwise()
        .to("direct:write-result")
        .end()
        .end();

    from("file:src/test/inbox/")
        .bean(FileCreator.class, "createOutputFile(\"result/vkgl_\"${file:name.noext}\".tsv\"," +
            VKGL_HEADERS + ")")
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

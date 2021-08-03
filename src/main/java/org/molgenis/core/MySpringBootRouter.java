package org.molgenis.core;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.builder.AggregationStrategies.groupedBody;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.molgenis.mappers.AlissaMapper.ALISSA_HEADERS;
import static org.molgenis.mappers.LumcMapper.LUMC_HEADERS;
import static org.molgenis.mappers.RadboudMumcMapper.RADBOUD_HEADERS;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.processor.aggregate.GroupedBodyAggregationStrategy;
import org.molgenis.mappers.AlissaVkglTableMapper;
import org.molgenis.mappers.GenericDataMapper;
import org.molgenis.mappers.LumcVkglTableMapper;
import org.molgenis.mappers.RadboudMumcVkglTableMapper;
import org.molgenis.utils.FileCreator;
import org.molgenis.utils.HgncGenes;
import org.molgenis.utils.HgncGenesParser;
import org.molgenis.validators.HgncGeneValidator;
import org.molgenis.validators.ReferenceSequenceValidator;
import org.molgenis.validators.UniquenessChecker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ComponentScan(basePackages = {"org.molgenis"})
@Component
public class MySpringBootRouter extends RouteBuilder {

  @Value("${completion.timeout}")
  private int fileCompletionTimeout;
  private static final int DEFAULT_TIMEOUT = 1000;
  private static final int COMPLETION_SIZE = 1000;
  private static final String VCF_HEADERS = "hgvs_normalized_vkgl\tgene\tchrom\tpos\tref\talt\ttype\tsignificance";
  private static final String ERROR_HEADERS = "hgvs_normalized_vkgl\tcdna_patched\terror";
  private static final String VKGL_HEADERS = "id\tchromosome\tstart\tstop\tref\talt\tgene\tc_dna\thgvs_g\thgvs_c\ttranscript\tprotein\ttype\tlocation\texon\teffect\tclassification\tcomments\tis_legacy\tlab_upload_date";
  private static final String TAB = "\t";

  private final ReferenceSequenceValidator refValidator;
  private final GenericDataMapper genericMapper;
  private final AlissaVkglTableMapper alissaTableMapper;
  private final RadboudMumcVkglTableMapper radboudMumcTableMapper;
  private final LumcVkglTableMapper lumcTableMapper;
  private final UniquenessChecker uniquenessChecker;

  @Value("${hgnc.genes}")
  String hgncGenesFile;

  public MySpringBootRouter(ReferenceSequenceValidator refValidator,
      GenericDataMapper genericMapper, AlissaVkglTableMapper alissaTableMapper,
      RadboudMumcVkglTableMapper radboudMumcTableMapper,
      LumcVkglTableMapper lumcTableMapper, UniquenessChecker uniquenessChecker) {
    this.refValidator = refValidator;
    this.genericMapper = genericMapper;
    this.alissaTableMapper = alissaTableMapper;
    this.radboudMumcTableMapper = radboudMumcTableMapper;
    this.lumcTableMapper = lumcTableMapper;
    this.uniquenessChecker = uniquenessChecker;
  }

  private Exchange mergeLists(Exchange variantExchange, Exchange responseExchange) {
    List<Map<String, Object>> variants = variantExchange.getIn().getBody(List.class);
    List<Map<String, Object>> validationResults = responseExchange.getIn().getBody(List.class);
    for (int i = 0; i < variants.size(); i++) {
      variants.get(i).putAll(validationResults.get(i));
    }
    return variantExchange;
  }

  private String[] getSplittedHeaders(String customHeaders, String defaultHeaders) {
    return (customHeaders + TAB + defaultHeaders).split(TAB);
  }

  private HgncGeneValidator getGeneValidator(String genesFile) throws IOException {
    HgncGenes hgncGenes = new HgncGenes();
    new HgncGenesParser(genesFile, hgncGenes);
    return new HgncGeneValidator(hgncGenes.getGenes(),
        hgncGenes.getPreviousGeneAliases());
  }

  private CsvDataFormat getNewCsv(String[] header, boolean useMaps, boolean headerDisabled)
      throws IOException {
    try (CsvDataFormat csv = new CsvDataFormat().setDelimiter('\t')) {
      if (header != null) {
        csv.setHeader(header);
      }
      if (useMaps) {
        csv.setUseMaps(true);
      }
      if (headerDisabled) {
        csv.setHeaderDisabled(true);
      }
      return csv;
    }
  }

  @Override
  @SuppressWarnings("squid:S1192") // String literals should not be duplicated
  public void configure() throws IOException {
    HgncGeneValidator geneValidator = this.getGeneValidator(hgncGenesFile);
    String resultFile = "file:result";

    from("direct:append-error")
        .routeId("appendErrorRoute")
        .to("file:result?fileName=vkgl_${file:name.noext}_error.txt&fileExist=Append");

    from("direct:write-alissa-error")
        .routeId("alissaErrorRoute")
        .marshal(getNewCsv(getSplittedHeaders(ALISSA_HEADERS, ERROR_HEADERS), false, true))
        .to("direct:append-error");

    from("direct:write-radboud-error")
        .routeId("radboudErrorRoute")
        .marshal(getNewCsv(getSplittedHeaders(RADBOUD_HEADERS, ERROR_HEADERS), false, true))
        .to("direct:append-error");

    from("direct:write-lumc-error")
        .routeId("lumcErrorRoute")
        .marshal(getNewCsv(getSplittedHeaders(LUMC_HEADERS, ERROR_HEADERS), false, true))
        .to("direct:append-error");

    from("direct:write-error")
        .routeId("writeErrorRoute")
        .recipientList(simple("direct:write-${header.labType}-error"));

    from("direct:marshal-alissa-result")
        .routeId("marshalAlissaRoute")
        .marshal(getNewCsv(getSplittedHeaders(ALISSA_HEADERS, VCF_HEADERS), false, false))
        .to(resultFile);

    from("direct:marshal-radboud-result")
        .routeId("marshalRadboudRoute")
        .marshal(getNewCsv(getSplittedHeaders(RADBOUD_HEADERS, VCF_HEADERS), false, false))
        .to(resultFile);

    from("direct:marshal-lumc-result")
        .routeId("marshalLumcRoute")
        .marshal(getNewCsv(getSplittedHeaders(LUMC_HEADERS, VCF_HEADERS), false, false))
        .to(resultFile);

    from("direct:marshal-vkgl-result")
        .routeId("marshalVkglRoute")
        .marshal(getNewCsv((VKGL_HEADERS).split(TAB), false, true))
        .to("file:result?fileName=vkgl_${file:name.noext}.tsv&fileExist=Append");

    from("direct:map-alissa-result")
        .routeId("mapAlissaRoute")
        .split().body()
        .process().body(Map.class, alissaTableMapper::mapLine)
        .to("direct:marshal-vkgl-result");

    from("direct:map-lumc-result")
        .routeId("mapLumcRoute")
        .split().body()
        .process().body(Map.class, lumcTableMapper::mapLine)
        .to("direct:marshal-vkgl-result");

    from("direct:map-radboud-result")
        .routeId("mapRaboudRoute")
        .split().body()
        .process().body(Map.class, radboudMumcTableMapper::mapLine)
        .to("direct:marshal-vkgl-result");

    from("direct:write-result")
        .routeId("writeResultRoute")
        .aggregate(header(FILE_NAME), new GroupedBodyAggregationStrategy())
        .completionTimeout(DEFAULT_TIMEOUT)
        .recipientList(simple(
            "direct:marshal-${header.labType}-result,direct:map-${header.labType}-result"))
        .delay(10000)
        .to("log:done");

    from("direct:check_unique")
        .routeId("checkUniqueRoute")
        .process(uniquenessChecker::getUniqueVariants)
        .split().body()
        .choice()
        .when(simple("${body['error']} != null"))
        .to("log:error")
        .to("direct:write-error")
        .otherwise()
        .to("direct:write-result")
        .end()
        .end();

    from("direct:validate")
        .routeId("validateRoute")
        .process()
        .body(Map.class, refValidator::validateOriginalRef)
        .aggregate(header(FILE_NAME), new GroupedBodyAggregationStrategy())
        .completionTimeout(fileCompletionTimeout)
        .process(geneValidator::getVariantsWithCorrectGenes)
        .to("direct:check_unique");

    from("direct:h2v")
        .routeId("h2vRoute")
        .to("log:httprequest")
        .transform()
        .jsonpath("$[*].hgvs_normalized_vkgl")
        .marshal()
        .json(Jackson)
        .setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http.HttpMethods.POST))
        .to("https://variants.molgenis.org/h2v?keep_left_anchor=True&strict=True")
        .id("variantFormatter")
        .unmarshal()
        .json(Jackson)
        .to("log:httpresponse");

    from("direct:hgvs2vcf")
        .routeId("vcfRoute")
        .description("Validates the normalized gDNA.")
        .aggregate(header(FILE_NAME), groupedBody())
        .completionSize(COMPLETION_SIZE)
        .completionTimeout(DEFAULT_TIMEOUT)
        .enrich("direct:h2v", this::mergeLists)
        .split().body()
        .to("direct:validate");

    from("direct:map_data")
        .routeId("mappingRoute")
        .split().body()
        .process().exchange(genericMapper::mapData)
        .to("direct:hgvs2vcf");

    from("file:src" + File.separator + "test" + File.separator + "inbox" + File.separator)
        .routeId("outputFileRoute")
        .bean(FileCreator.class,
            "createOutputFile(\"result" + File.separator + "vkgl_\"${file:name.noext}\".tsv\"," +
                VKGL_HEADERS + ")")
        .choice().when(simple("${header.CamelFileName} contains 'radboud'"))
        .unmarshal(getNewCsv(RADBOUD_HEADERS.split(TAB), true, false))
        .otherwise()
        .unmarshal(getNewCsv(null, true, false))
        .end()
        .to("direct:map_data");
  }
}

package org.molgenis;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.apache.camel.util.toolbox.AggregationStrategies.groupedBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;

@Component
public class MySpringBootRouter extends RouteBuilder {

  private static final AlissaMapper alissaMapper = new AlissaMapper();
  private static final LumcMapper lumcMapper = new LumcMapper();
  private static final RadboudMumcMapper radboudMumcMapper = new RadboudMumcMapper();

  private static final String LUMC_HEADERS = lumcMapper.getHeader();
  private static final String RADBOUD_HEADERS = radboudMumcMapper.getHeader();
  private static final String ALISSA_HEADERS = alissaMapper.getHeader();

  private static final String VCF_HEADERS = "hgvs_normalized_vkgl\tchrom\tpos\tref\talt\tsignificance";

  private static final ReferenceSequenceValidator refValidator = new ReferenceSequenceValidator();

  private Exchange mergeLists(Exchange variantExchange, Exchange responseExchange) {
    List<Map<String, Object>> variants = variantExchange.getIn().getBody(List.class);
    List<Map<String, Object>> validationResults = responseExchange.getIn().getBody(List.class);
    for (int i = 0; i < variants.size(); i++) {
      variants.get(i).putAll(validationResults.get(i));
    }
    return variantExchange;
  }

  private void writeError(String headers) {
    from("direct:write-error")
        .marshal(
            new CsvDataFormat()
                .setDelimiter('\t')
                .setHeader(
                    (headers + "\thgvs_normalized_vkgl\tcdna_patched\terror").split("\t"))
                .setHeaderDisabled(true))
        .to("file:result?fileExist=Append");
  }

  private void writeResult(String header) {
    from("direct:write-result")
        .aggregate(header(FILE_NAME))
        .strategy(groupedBody())
        .completionTimeout(30000)
        .to("log:done")
        .marshal(new CsvDataFormat().setDelimiter('\t')
            .setHeader((header + '\t' + VCF_HEADERS).split("\t")))
        .to("file:result");
  }

  private void normalizeData() {
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
  }

  private void convertHgvsToVcf(String errorFile) {
    from("direct:hgvs2vcf")
        .description("Validates the normalized gDNA.")
        .aggregate(header(FILE_NAME), groupedBody())
        .completionSize(1000)
        .completionTimeout(1000)
        .enrich("direct:h2v", this::mergeLists)
        .split()
        .body()
        .choice()
        .when(simple("${body['ref_orig']} != null"))
        .process().body(Map.class, refValidator::validateOriginalRef)
        .end()
        .choice().when(simple("${body['error']} != null"))
        .to("log:error")
        .setHeader(FILE_NAME, constant(errorFile))
        .to("direct:write-error")
        .otherwise()
        .to("direct:write-result")
        .end()
        .end();
  }

  @Override
  public void configure() {
    String fileName = "lumc.txt";
    String errorFile = fileName.split("\\.")[0] + "_error." + fileName.split("\\.")[1];
    String fileAddress = "./src/test/resources/" + fileName;
    Path filePath = Paths.get(fileAddress);
    String header = null;
    try {
      header = Files.lines(filePath)
          .findFirst()
          .get();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (header.equals(ALISSA_HEADERS
        .replace("ref_orig", "ref")
        .replace("alt_orig", "alt"))) {
      writeError(ALISSA_HEADERS);
      writeResult(ALISSA_HEADERS);
      normalizeData();
      convertHgvsToVcf(errorFile);
      from("file:src/test/resources/?fileName=" + fileName + "&noop=true")
          .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true))
          .split().body()
          .process().body(Map.class, alissaMapper::mapData)
          .to("direct:hgvs2vcf");
    } else if (header.equals(LUMC_HEADERS
        .replace("hgvs_normalized", "gDNA_normalized"))) {
      writeError(LUMC_HEADERS);
      writeResult(LUMC_HEADERS);
      normalizeData();
      convertHgvsToVcf(errorFile);
      from("file:src/test/resources/?fileName=" + fileName + "&noop=true")
          .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true))
          .split().body()
          .process().body(Map.class, lumcMapper::mapData)
          .to("direct:hgvs2vcf");
      // Radboud/MUMC data doesn't have a header
    } else if (header.startsWith("chr")) {
      writeError(RADBOUD_HEADERS);
      writeResult(RADBOUD_HEADERS);
      normalizeData();
      convertHgvsToVcf(errorFile);
      from("file:src/test/resources/?fileName=" + fileName + "&noop=true")
          .unmarshal(new CsvDataFormat().setDelimiter('\t').setUseMaps(true).setHeader(
              RADBOUD_HEADERS.split("\t")))
          .split().body()
          .process().body(Map.class, radboudMumcMapper::mapData)
          .to("direct:hgvs2vcf");
    } else {
      System.err.printf("File not recognized: %s%n", fileName);
    }
//    String resourcePath = "./src/test/resources/";
//    File[] dataDir = new File(resourcePath).listFiles();
//    for (File file : dataDir) {
//      if (!file.isDirectory()) {
//        String fileName = file.getName();
//        String extension = fileName.substring(fileName.lastIndexOf("."));
//        if (extension.equals(".txt") || extension.equals(".tsv")) {
//          // Process stuff
//        }
//
//      }
//    }
  }
}

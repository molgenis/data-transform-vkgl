package org.molgenis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.molgenis.core.MySpringBootRouter;
import org.molgenis.mappers.AlissaMapper;
import org.molgenis.mappers.AlissaVkglTableMapper;
import org.molgenis.mappers.GenericDataMapper;
import org.molgenis.mappers.LumcMapper;
import org.molgenis.mappers.LumcVkglTableMapper;
import org.molgenis.mappers.RadboudMumcMapper;
import org.molgenis.mappers.RadboudMumcVkglTableMapper;
import org.molgenis.utils.HgvsService;
import org.molgenis.validators.ReferenceSequenceValidator;
import org.molgenis.validators.UniquenessChecker;

@RunWith(JUnitParamsRunner.class)
public class MyFirstCamelTest extends CamelTestSupport {

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    HgvsService hgvsService = new HgvsService();
    AlissaMapper alissaMapper = new AlissaMapper(hgvsService);
    LumcMapper lumcMapper = new LumcMapper(hgvsService);
    RadboudMumcMapper radboudMumcMapper = new RadboudMumcMapper(hgvsService);
    ReferenceSequenceValidator refValidator = new ReferenceSequenceValidator();
    GenericDataMapper genericMapper = new GenericDataMapper(alissaMapper, lumcMapper,
        radboudMumcMapper);
    AlissaVkglTableMapper alissaTableMapper = new AlissaVkglTableMapper();
    RadboudMumcVkglTableMapper radboudMumcTableMapper = new RadboudMumcVkglTableMapper();
    LumcVkglTableMapper lumcTableMapper = new LumcVkglTableMapper();
    UniquenessChecker uniquenessChecker = new UniquenessChecker();
    return new MySpringBootRouter(refValidator, genericMapper, alissaTableMapper,
        radboudMumcTableMapper, lumcTableMapper, uniquenessChecker);
  }

  private File getInputFile(String name) throws URISyntaxException, IOException {
    return FileUtils.getFile("src", "test", "resources", name);
  }

  private String getHeader(File textFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(textFile));
    return br.readLine();
  }

  @Test
  @Parameters(method = "parametersForOutputHeader")
  public void testOutputHeader(String inputFileName, String outputFileName, String expectedHeader)
      throws Exception {
    MockEndpoint result = getMockEndpoint("mock:output");
    File inputFile = getInputFile(inputFileName);
    context.getRouteDefinition("createOutputFile")
        .adviceWith(context, new AdviceWithRouteBuilder() {
          @Override
          public void configure() throws Exception {
            interceptSendToEndpoint("direct:map_data")
                .skipSendToOriginalEndpoint()
                .to("mock:output");
          }
        });
    context.start();
    FileUtils.copyFile(inputFile, new File(
        "src" + File.separator + "test" + File.separator + "inbox" + File.separator
            + inputFileName));
    result.setResultWaitTime(20000);
    result.assertIsSatisfied();
    String header = getHeader(FileUtils.getFile("result", outputFileName));
    assert (header.equals(expectedHeader));
    context.stop();
  }

  private Object[] parametersForOutputHeader() {
    return new Object[]{
        new Object[]{
            "test_alissa.txt", "vkgl_test_alissa.tsv",
            "id\tchromosome\tstart\tstop\tref\talt\tgene\tc_dna\thgvs_g\thgvs_c\ttranscript\tprotein\ttype\tlocation\texon\teffect\tclassification\tcomments\tis_legacy\tlab_upload_date"
        }
    };
  }

  @Test
  public void testAlissaHeader() throws Exception {
    MockEndpoint result = getMockEndpoint("mock:output");
    File inputFile = getInputFile("test_alissa.txt");

    context.getRouteDefinition("createOutputFile")
        .adviceWith(context, new AdviceWithRouteBuilder() {
          @Override
          public void configure() throws Exception {
            interceptSendToEndpoint("direct:map_data")
                .skipSendToOriginalEndpoint()
                .to("mock:output");
          }
        });
    context.start();
    FileUtils.copyFile(inputFile, new File(
        "src" + File.separator + "test" + File.separator + "inbox" + File.separator
            + "test_alissa.txt"));
    result.setResultWaitTime(20000);
    result.assertIsSatisfied();
    String header = getHeader(FileUtils.getFile("result", "vkgl_test_alissa.tsv"));
    assert (header.equals(
        "id\tchromosome\tstart\tstop\tref\talt\tgene\tc_dna\thgvs_g\thgvs_c\ttranscript\tprotein\ttype\tlocation\texon\teffect\tclassification\tcomments\tis_legacy\tlab_upload_date"));
    context.stop();
  }

}

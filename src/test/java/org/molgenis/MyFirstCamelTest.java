package org.molgenis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.molgenis.core.MySpringBootRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = MySpringBootRouter.class)
public class MyFirstCamelTest {

  @EndpointInject(uri = MOCK_RESULT)
  private MockEndpoint resultEndpoint;

  @Autowired
  private CamelContext camelContext;

  private static final String MOCK_RESULT = "mock:output";
  private static final String MOCK_VKGL = "direct:map_data";

  private File getInputFile(String name) throws URISyntaxException, IOException {
    return FileUtils.getFile("src", "test", "resources", name);
  }

  private String getHeader(File textFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(textFile));
    return br.readLine();
  }

  private void testHeader(String inputFileName, String outputFileName) throws Exception {
    File inputFile = getInputFile(inputFileName);

    camelContext.getRouteDefinition("outputFileRoute")
        .adviceWith(camelContext, new AdviceWithRouteBuilder() {
          @Override
          public void configure() throws Exception {
            interceptSendToEndpoint(MOCK_VKGL)
                .skipSendToOriginalEndpoint()
                .to(MOCK_RESULT);
          }
        });
    camelContext.start();
    File testInput = new File(
        "src" + File.separator + "test" + File.separator + "inbox" + File.separator
            + inputFileName);
    FileUtils.copyFile(inputFile, testInput);
    resultEndpoint.setResultWaitTime(20000);
    resultEndpoint.assertIsSatisfied();
    String header = getHeader(FileUtils.getFile("result", outputFileName));
    assert (header.equals(
        "id\tchromosome\tstart\tstop\tref\talt\tgene\tc_dna\thgvs_g\thgvs_c\ttranscript\tprotein\ttype\tlocation\texon\teffect\tclassification\tcomments\tis_legacy\tlab_upload_date"));
    resultEndpoint.expectedBodiesReceived(1);
    camelContext.stop();
    testInput.delete();
  }

  @Test
  public void testAlissaHeader() throws Exception {
    testHeader("test_alissa.txt", "vkgl_test_alissa.tsv");
  }

  @Test
  public void testRadboudMumcHeader() throws Exception {
    testHeader("test_radboud_mumc.tsv", "vkgl_test_radboud_mumc.tsv");
  }

  @Test
  public void testLumcHeader() throws Exception {
    testHeader("test_lumc.tsv", "vkgl_test_lumc.tsv");
  }
}

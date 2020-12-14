package org.molgenis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.DisableJmx;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.molgenis.core.MySpringBootRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = MySpringBootRouter.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@DisableJmx
public class MyFirstCamelTest {

  @EndpointInject(uri = MOCK_RESULT)
  private MockEndpoint resultEndpoint;

  @Autowired
  private CamelContext camelContext;

  private static final String MOCK_RESULT = "mock:output";

  private File getInputFile(String name) {
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
          public void configure() {
            interceptSendToEndpoint("direct:map_data")
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
    resultEndpoint.expectedMessageCount(1);
    camelContext.stop();
    String header = getHeader(FileUtils.getFile("result", outputFileName));
    assert (header.equals(
        "id\tchromosome\tstart\tstop\tref\talt\tgene\tc_dna\thgvs_g\thgvs_c\ttranscript\tprotein\ttype\tlocation\texon\teffect\tclassification\tcomments\tis_legacy\tlab_upload_date"));
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

  @Test
  public void testRoute() throws Exception {
    File inputFile = getInputFile("test_alissa.txt");
    InputStream mockResponse = new FileInputStream("src/test/resources/alissa_mock.json");
    MockEndpoint errorEndpoint = camelContext.getEndpoint("mock:error", MockEndpoint.class);
    MockEndpoint alissaEndpoint = camelContext.getEndpoint("mock:alissa", MockEndpoint.class);

    // Mock the api response
    camelContext.getRouteDefinition("h2vRoute")
        .adviceWith(camelContext, new AdviceWithRouteBuilder() {
          @Override
          public void configure() {
            weaveById("variantFormatter")
                .replace().setBody(constant(mockResponse));
          }
        });
    // Add mock endpoint after resultRoute to test messages are received
    camelContext.getRouteDefinition("writeResultRoute")
        .adviceWith(camelContext, new AdviceWithRouteBuilder() {
          @Override
          public void configure() throws Exception {
            weaveAddLast().to("mock:output");
          }
        });
    // Add mock endpoint after errorRoute to test messages are received
    camelContext.getRouteDefinition("writeErrorRoute")
        .adviceWith(camelContext, new AdviceWithRouteBuilder() {
          @Override
          public void configure() throws Exception {
            weaveAddLast().to("mock:error");
          }
        });
    // Add mock endpoint after marshalAlissaRoute to test whether alissa endpoint is reached
    camelContext.getRouteDefinition("marshalAlissaRoute")
        .adviceWith(camelContext, new AdviceWithRouteBuilder() {
          @Override
          public void configure() throws Exception {
            weaveAddLast().to("mock:alissa");
          }
        });

    camelContext.start();
    File testInput = new File(
        "src" + File.separator + "test" + File.separator + "inbox" + File.separator
            + "test_alissa.txt");
    FileUtils.copyFile(inputFile, testInput);
    resultEndpoint.setResultWaitTime(120000);
    errorEndpoint.expectedMessageCount(3);
    resultEndpoint.expectedMessageCount(15);
    alissaEndpoint.expectedMessageCount(1);
    resultEndpoint.assertIsSatisfied();
    errorEndpoint.assertIsSatisfied();
    alissaEndpoint.assertIsSatisfied();
    camelContext.stop();
  }
}

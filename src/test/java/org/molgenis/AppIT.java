package org.molgenis;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
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
public class AppIT {

  @EndpointInject(uri = MOCK_RESULT)
  private MockEndpoint resultEndpoint;

  @EndpointInject(uri = MOCK_ERROR)
  private MockEndpoint errorEndpoint;

  @EndpointInject(uri = MOCK_LAB)
  private MockEndpoint labSpecificEndpoint;

  @Autowired
  private CamelContext camelContext;

  private static final String MOCK_RESULT = "mock:output";
  private static final String MOCK_ERROR = "mock:error";
  private static final String MOCK_LAB = "mock:lab";

  private void setMockEndpoint(String mockUri, String route) throws Exception {
    camelContext.getRouteDefinition(route)
        .adviceWith(camelContext, new AdviceWithRouteBuilder() {
          @Override
          public void configure() throws Exception {
            weaveAddLast().to(mockUri);
          }
        });
  }

  private void testRoute(String inputFileName, String lab, String labRoute,
      int correctVariants, int errorVariants) throws Exception {
    File inputFile = FileUtils.getFile("src", "test", "resources", inputFileName);
    // Delete old error file if it exists (errors will be added to existing error file)
    File errorFile = FileUtils.getFile("result", "vkgl_test_" + lab + "_error.txt");
    Files.deleteIfExists(errorFile.toPath());
    InputStream mockResponse = new FileInputStream(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + lab
            + "_mock.json");

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
    setMockEndpoint(MOCK_RESULT, "writeResultRoute");
    // Add mock endpoint after errorRoute to test messages are received
    setMockEndpoint(MOCK_ERROR, "writeErrorRoute");
    // Add mock endpoint after labRoute to test whether correct lab endpoint is reached
    setMockEndpoint(MOCK_LAB, labRoute);

    camelContext.start();

    // Copy inputfile to input folder to start the camel route
    File testInput = new File(
        "src" + File.separator + "test" + File.separator + "inbox" + File.separator
            + inputFileName);
    FileUtils.copyFile(inputFile, testInput);

    // If tests succeed within 30 seconds, tests will continue
    resultEndpoint.setResultWaitTime(30000);
    errorEndpoint.setResultWaitTime(30000);
    labSpecificEndpoint.setResultWaitTime(30000);

    // Assert the number of messages received by each route
    resultEndpoint.expectedMessageCount(correctVariants);
    errorEndpoint.expectedMessageCount(errorVariants);
    labSpecificEndpoint.expectedMessageCount(1);

    // Assert all endpoints are satisfied
    resultEndpoint.assertIsSatisfied();
    errorEndpoint.assertIsSatisfied();
    labSpecificEndpoint.assertIsSatisfied();

    camelContext.stop();

    // Assert output file is still the same as the snapshot
    File snapshot = FileUtils.getFile("src", "test", "resources", "snapshot_" + lab + ".tsv");
    File actual = FileUtils.getFile("result", "vkgl_test_" + lab + ".tsv");
    assertTrue(FileUtils.contentEquals(snapshot, actual));
  }

  @Test
  public void testAlissaRoute() throws Exception {
    testRoute("test_alissa.txt", "alissa", "marshalAlissaRoute", 17, 5);
  }

  @Test
  public void testLumcRoute() throws Exception {
    testRoute("test_lumc.tsv", "lumc", "marshalLumcRoute", 3, 0);
  }

  @Test
  public void testRadboudMumcRoute() throws Exception {
    testRoute("test_radboud_mumc.tsv", "radboud_mumc", "marshalRadboudRoute", 4, 0);
  }
}

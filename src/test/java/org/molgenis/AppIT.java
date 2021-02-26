package org.molgenis;

import static org.apache.camel.builder.Builder.constant;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.molgenis.core.MySpringBootRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;

@CamelSpringTest
@ContextConfiguration
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(classes = MySpringBootRouter.class)
public class AppIT {

  @EndpointInject(MOCK_RESULT)
  private MockEndpoint resultEndpoint;

  @EndpointInject(MOCK_ERROR)
  private MockEndpoint errorEndpoint;

  @EndpointInject(MOCK_LAB)
  private MockEndpoint labSpecificEndpoint;

  @Autowired
  private CamelContext camelContext;

  private static final String MOCK_RESULT = "mock:output";
  private static final String MOCK_ERROR = "mock:error";
  private static final String MOCK_LAB = "mock:lab";

  private void setMockEndpoint(String mockUri, String route) throws Exception {
    AdviceWith.adviceWith(camelContext, route, a -> {
      a.weaveAddLast().to(mockUri);
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
    AdviceWith.adviceWith(camelContext, "h2vRoute", a -> {
      a.weaveById("variantFormatter").replace().setBody(constant(mockResponse));
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
  void testAlissaRoute() throws Exception {
    testRoute("test_alissa.txt", "alissa", "marshalAlissaRoute", 17, 5);
  }

  @Test
  void testLumcRoute() throws Exception {
    testRoute("test_lumc.tsv", "lumc", "marshalLumcRoute", 3, 0);
  }

  @Test
  void testRadboudMumcRoute() throws Exception {
    testRoute("test_radboud_mumc.tsv", "radboud_mumc", "marshalRadboudRoute", 4, 0);
  }
}

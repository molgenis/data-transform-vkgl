package org.molgenis.core;

import org.apache.camel.main.Main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MySpringBootApplication extends Main {

  /**
   * A main method to start this application.
   */
  public static void main(String[] args) {
    SpringApplication.run(MySpringBootApplication.class, args);
  }

}

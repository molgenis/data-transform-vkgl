package org.molgenis.core;

import java.util.MissingResourceException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MySpringBootApplication {

  /**
   * A main method to start this application.
   */
  public static void main(String[] args) {
    try {
      SpringApplication.run(MySpringBootApplication.class, args);
    } catch (BeanCreationException ex) {
      throw new MissingResourceException(
          "Missing environment variable: [hgnc.genes]. Consult README.md for more information.",
          "Environment variable",
          "hgnc.genes");
    }
  }

}

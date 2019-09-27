package org.molgenis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MySpringBootApplication {

  /**
   * A main method to start this application.
   */
  public static void main(String[] args) {
//    String[] options = CommandlineOptions.generateOptions({"BLAAT"});
    SpringApplication.run(MySpringBootApplication.class, args);
  }

}

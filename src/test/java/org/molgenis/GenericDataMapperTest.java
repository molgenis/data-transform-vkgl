package org.molgenis;

import static org.junit.Assert.assertEquals;
import static org.molgenis.GenericDataMapper.getType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenericDataMapperTest {

  @Test
  void getTypeLumcTest() {
    Set<String> set = new HashSet<>(Arrays.asList("a", "b", "gDNA_normalized"));
    assertEquals("lumc", getType(set));
  }

  @Test
  void getTypeRadboudTest() {
    Set<String> set = new HashSet<>(Arrays.asList("a", "b", "empty1"));
    assertEquals("radboud", getType(set));
  }

  @Test
  void getTypeAlissaTest() {
    Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
    assertEquals("alissa", getType(set));
  }

}
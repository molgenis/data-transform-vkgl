package org.molgenis.mappers;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.molgenis.mappers.GenericDataMapper.getType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.validation.UnexpectedTypeException;
import org.junit.jupiter.api.Test;

class GenericDataMapperTest {

  @Test
  void getTypeLumcTest() {
    Set<String> set = new HashSet<>(Arrays
        .asList("refseq_build", "chromosome", "gDNA_normalized", "variant_effect", "geneid", "cDNA",
            "Protein"));
    assertEquals("lumc", getType(set));
  }

  @Test
  void getTypeRadboudTest() {
    Set<String> set = new HashSet<>(Arrays
        .asList("chromosome_orig", "start", "stop", "ref_orig", "alt_orig", "gene_orig",
            "transcript",
            "protein", "empty1", "exon", "empty2", "classification"));
    assertEquals("radboud", getType(set));
  }

  @Test
  void getTypeAlissaTest() {
    Set<String> set = new HashSet<>(Arrays
        //timestamp	id	chromosome	start	stop	ref	alt	gene	transcript	c_nomen	p_nomen	exon	variant_type	location	effect	classification	last_updated_by	last_updated_on
        .asList("timestamp", "id", "chromosome", "start", "stop", "ref", "alt", "gene",
            "transcript", "c_nomen", "p_nomen", "exon", "variant_type", "location", "effect",
            "classification", "last_updated_by", "last_updated_on"));
    assertEquals("alissa", getType(set));
  }

  @Test
  void getTypeErrorTest() {
    Set<String> set = new HashSet<>(Arrays.asList("my", "beautiful", "shoppinglist"));
    try {
      getType(set);
    } catch (UnexpectedTypeException caughtException) {
      assertThat(caughtException.getMessage(),
          is("Lab type not recognized, check headers with headers of alissa, radboud, and lumc"));
    }
  }

}
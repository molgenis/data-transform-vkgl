package org.molgenis.consensus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.consensus.model.Classification;
import org.molgenis.consensus.model.Consensus;
import org.molgenis.consensus.model.GeneVariant;
import org.molgenis.consensus.model.Lab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ConsensusGeneratorTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsensusGeneratorTest.class);

  private ConsensusGenerator consensusGenerator;
  @Mock
  private ClassificationParser classificationParser;
  @Mock
  private ConsensusMapper consensusMapper;
  @Mock
  private ConsensusWriter consensusWriter;

  @BeforeEach
  void setUp() {
    consensusGenerator = new ConsensusGenerator(classificationParser, consensusMapper,
        consensusWriter);
  }

  @Test
  void generateConsensus() throws IOException {
    Map<Lab, Path> labPaths = new EnumMap<>(Lab.class);
    labPaths.put(Lab.AMC, Files.createTempFile("transformed_amc", "tsv"));
    labPaths.put(Lab.ERASMUS_MC, Files.createTempFile("transformed_erasmuc_mc", "tsv"));

    GeneVariant geneVariant = GeneVariant.builder().build();
    GeneVariant amcGeneVariant = GeneVariant.builder().build();
    Classification amcClassification0 = Classification.builder().build();
    Classification amcClassification1 = Classification.builder().build();
    List<Classification> amcClassifications = List.of(amcClassification0, amcClassification1);

    GeneVariant erasmusMcGeneVariant = GeneVariant.builder().build();
    Classification erasmusMcClassification0 = Classification.builder().build();
    Classification erasmusMcClassification1 = Classification.builder().build();
    List<Classification> erasmusMcClassifications = List.of(erasmusMcClassification0, erasmusMcClassification1);

    when(classificationParser.parse(any())).thenReturn(amcClassifications).thenReturn(erasmusMcClassifications);

    Consensus consensus = Consensus.builder().build();
    doReturn(consensus).when(consensusMapper).map(geneVariant, Map.of(Lab.AMC, amcClassification0, Lab.ERASMUS_MC, erasmusMcClassification1));

    Consensus amcConsensus = Consensus.builder().build();
    doReturn(amcConsensus).when(consensusMapper).map(amcGeneVariant, Map.of(Lab.AMC, amcClassification1));

    Consensus erasmusMcConsensus = Consensus.builder().build();
    doReturn(erasmusMcConsensus).when(consensusMapper).map(erasmusMcGeneVariant, Map.of(Lab.ERASMUS_MC, erasmusMcClassification0));

    consensusGenerator.generateConsensus(labPaths, Files.createTempFile("transformed_consensus", "tsv"));
  }
}
package com.tmilar.labelsimplification;

import com.tmilar.labelsimplification.model.Extractor;
import com.tmilar.labelsimplification.service.LabelSimplificationService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LabelSimplificationServiceTest {

  private LabelSimplificationService labelSimplificationService;

  @Before
  public void setup() {
    List<Extractor> extractionRules = sampleExtractionRules();
    labelSimplificationService = new LabelSimplificationService();
    labelSimplificationService.load(extractionRules);
  }

  private List<Extractor> sampleExtractionRules() {
    String[][] data = {
        // keyName, extractedValue, matcher, parentKeyName, parentValue
        {"Juego", "Magic", "Mtg|Mag", null, null},
        {"Juego", "Pokemon", "Pok|Pkm", null, null},
        {"Coleccion", "Sun & Moon", "SM1|Sun & Moon", "Juego", "Pokemon"},
        {"Coleccion", "Sun & Moon: Guardians Rising", "SM2|Guardians Rising", "Juego", "Pokemon"},
        {"Coleccion", "Theros", "Theros", "Juego", "Magic"},
        {"TipoProducto", "Booster Box", "Booster Box|Booster Display Box", "Juego", "Pokemon"},
        {"TipoProducto", "Booster Box", "Booster Box|Booster Display Box", "Juego", "Magic"},
        {"Idioma", "Español", "SP]Spanish]Esp", null, null},
        {"Idioma", "Ingles", "", null, null}
    };

    List<Extractor> extractors = Arrays.stream(data)
        .map(e -> new Extractor(e[0], e[1], e[2], e[3], e[4])).collect(Collectors.toList());

    return extractors;
  }

  @Test
  public void simplifyLabel_shouldSimplifyLabel_forValidCase() {
    String label = "Pokemon SM1 booster Box ";
    String expectedLabel = "Pokemon Sun & Moon Booster Box Ingles";

    String simplifiedLabel = labelSimplificationService.simplifyLabel(label);

    Assert.assertEquals(expectedLabel, simplifiedLabel);
  }
}

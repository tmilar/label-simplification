package com.tmilar.labelsimplification;

import com.tmilar.labelsimplification.model.Extractor;
import com.tmilar.labelsimplification.model.SimplifiedLabel;
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
        {"Juego", "Magic", "Mtg|Mag", "0", null},
        {"Juego", "Pokemon", "Pokemon|Pkm", "0", null},
        {"Coleccion", "Sun & Moon", "SM1|Sun & Moon", "0", "Juego.Pokemon[0]"},
        {"Coleccion", "Sun & Moon: Guardians Rising", "SM2|Guardians Rising", "0", "Juego.Pokemon[0]"},
        {"Coleccion", "Theros", "Theros", "0", "Juego.Magic"},
        {"TipoProducto", "Booster Box", "Booster Box|Booster Display Box", "0", "Juego.Pokemon[0]"},
        {"TipoProducto", "Booster Box", "Booster Box|Booster Display Box", "0", "Juego.Magic[0]"},
        {"Idioma", "Español", "SP]Spanish]Esp", "0", null},
        {"Idioma", "Ingles", "", "0", null}
    };

    List<Extractor> extractors = Arrays.stream(data)
        .map(e -> new Extractor(e[0], e[1], e[2], e[4], Integer.valueOf(e[3]))).collect(Collectors.toList());

    return extractors;
  }

  @Test
  public void simplifyLabel_shouldSimplifyLabel_forValidCase() {
    String label = "Pokemon SM1 booster Box ";
    String expectedLabel = "Pokemon Sun & Moon Booster Box Ingles";

    SimplifiedLabel simplifiedLabel = labelSimplificationService.simplifyLabel(label);

    Assert.assertEquals(expectedLabel, simplifiedLabel.getSimplifiedLabel());
  }
}

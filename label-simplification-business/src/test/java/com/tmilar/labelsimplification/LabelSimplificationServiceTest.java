package com.tmilar.labelsimplification;

import com.tmilar.labelsimplification.service.LabelSimplificationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LabelSimplificationServiceTest {

  private LabelSimplificationService labelSimplificationService;

  @Before
  public void setup() {
    String[][] extractionRules = sampleExtractionRules();
    labelSimplificationService = new LabelSimplificationService();
    labelSimplificationService.load(extractionRules);
  }

  private static String[][] sampleExtractionRules() {
    return new String[][]{
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
  }

  @Test
  public void simplifyLabel_shouldSimplifyLabel_forValidCase() {
    String label = "Pokemon SM1 booster Box ";
    String expectedLabel = "Pokemon Sun & Moon Booster Box Ingles";

    String simplifiedLabel = labelSimplificationService.simplifyLabel(label);

    Assert.assertEquals(expectedLabel, simplifiedLabel);
  }
}

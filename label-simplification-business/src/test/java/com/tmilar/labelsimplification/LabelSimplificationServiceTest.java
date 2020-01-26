package com.tmilar.labelsimplification;

import com.tmilar.labelsimplification.model.Extractor;
import com.tmilar.labelsimplification.model.Label;
import com.tmilar.labelsimplification.model.SimplifiedLabel;
import com.tmilar.labelsimplification.service.LabelSimplificationService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    Map<String, List<String>> categoryStopWords = new HashMap<>();
    labelSimplificationService.load(extractionRules, categoryStopWords);
  }

  private List<Extractor> sampleExtractionRules() {
    String[][] data = {
        // keyName, extractedValue, matcher, parentKeyName, parentValue
        {"TCG", "Juego", "Magic", "Mtg|Mag", "0", null},
        {"TCG", "Juego", "Pokemon", "Pokemon|Pkm", "0", null},
        {"TCG", "Coleccion", "Sun & Moon", "SM1|Sun & Moon", "0", "Juego.Pokemon[0]"},
        {"TCG", "Coleccion", "Sun & Moon: Guardians Rising", "SM2|Guardians Rising", "0", "Juego.Pokemon[0]"},
        {"TCG", "Coleccion", "Theros", "Theros", "0", "Juego.Magic"},
        {"TCG", "TipoProducto", "Booster Box", "Booster Box|Booster Display Box", "0", "Juego.Pokemon[0]"},
        {"TCG", "TipoProducto", "Booster Box", "Booster Box|Booster Display Box", "0", "Juego.Magic[0]"},
        {"TCG", "Idioma", "Español", "SP]Spanish]Esp", "0", null},
        {"TCG", "Idioma", "Ingles", "", "0", null}
    };

    List<Extractor> extractors = Arrays.stream(data)
        .map(e -> new Extractor(e[1], e[2], e[3], e[5], Integer.valueOf(e[4]), e[0])).collect(Collectors.toList());

    return extractors;
  }

  @Test
  public void simplifyLabel_shouldSimplifyLabel_forValidCase() {
    Label label = new Label("Pokemon SM1 booster Box ");
    String expectedLabel = "Pokemon Sun & Moon Booster Box Ingles";

    SimplifiedLabel simplifiedLabel = labelSimplificationService.simplifyLabel(label);

    Assert.assertEquals(expectedLabel, simplifiedLabel.getSimplifiedLabel());
  }
}

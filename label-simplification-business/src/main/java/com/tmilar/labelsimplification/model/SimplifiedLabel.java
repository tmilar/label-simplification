package com.tmilar.labelsimplification.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public class SimplifiedLabel extends Label {

  private Map<String, List<Pair<Extractor, String>>> extractionsMap;
  /**
   * The label result after simplification step process.
   */
  private String simplifiedLabel;

  public SimplifiedLabel(Label labelEntity, String simplifiedLabel) {
    super(labelEntity.getLabel());
    this.simplifiedLabel = simplifiedLabel;
  }

  public SimplifiedLabel(Label label, String simplifiedString,
      Map<String, List<Pair<Extractor, String>>> extractionsMap) {
    super(label.getLabel());
    this.simplifiedLabel = simplifiedString;
    this.extractionsMap = extractionsMap;
  }

  public SimplifiedLabel(String label, String simplifiedString,
      Map<String, List<Pair<Extractor, String>>> extractionsMap) {
    this(new Label(label), simplifiedString, extractionsMap);
  }

  public Map<String, String> getExtractedValuesMap() {
    Map<String, String> extractedValues = new LinkedHashMap<>();
    if(extractionsMap == null) {
      return extractedValues;
    }
    extractionsMap.forEach((String key, List<Pair<Extractor, String>> extractions) -> {
      String extraction = extractions.stream()
          .map(Pair::getValue)
          .findFirst()
          .orElse(null);
      extractedValues.put(key, extraction);
    });

    return extractedValues;
  }

  public String getSimplifiedLabel() {
    return simplifiedLabel;
  }

  public void setSimplifiedLabel(String simplifiedLabel) {
    this.simplifiedLabel = simplifiedLabel;
  }

  @Override
  public String toString() {
    return String.format(
        "[SimplifiedLabel: label='%s', simplified='%s']", label, simplifiedLabel);
  }

  public Map<String, List<Pair<Extractor, String>>> getExtractionsMap() {
    return extractionsMap;
  }

  public void setExtractionsMap(
      Map<String, List<Pair<Extractor, String>>> extractionsMap) {
    this.extractionsMap = extractionsMap;
  }
}

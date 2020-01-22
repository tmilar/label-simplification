package com.tmilar.labelsimplification.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Extractor {

  private static final Logger logger = LogManager.getLogger(Extractor.class);

  // the key to be extracted. We extract only one instance of a key (ie. "Juego", "Coleccion").
  private final String keyName;

  // the value to extract when the match succeeds.
  private final String extractValue;

  // regex to use for check for a match.
  private final String matcher;
  private final Pattern compiledMatcher;

  // data refs to parent extractor
  private String parentKeyName;
  private String parentValue;

  public Extractor(String keyName, String extractValue, String matcher) {
    this.keyName = keyName;
    this.extractValue = extractValue;
    // escape empty matcher to end-of-string
    this.matcher = Objects.equals(matcher, "") ? "$" : matcher;

    // pre-compile pattern matcher.
    this.compiledMatcher = Pattern.compile(matcher, Pattern.CASE_INSENSITIVE);
  }

  public Extractor(String keyName, String extractValue, String matcher, String parentKeyName,
      String parentValue) {
    this(keyName, extractValue, matcher);
    this.parentKeyName = parentKeyName;
    this.parentValue = parentValue;
  }

  public String extract(String label) {
    if (matcher == null) {
      logger.warn("Matcher for key '{}' was null - can't apply to label '{}'", label);
    }

    if (Objects.equals(matcher, "")
        || Objects.equals(matcher, "*")
        || Objects.equals(matcher, ".*")) {
      // 'any' matcher -> return immediately
      logger.debug("Direct extraction key '{}' (regex: '{}') for label '{}'.",
          keyName, matcher, label);
      return extractValue;
    }

    if (checkRegexMatch(label)) {
      return extractValue;
    }

    return null;
  }

  private Boolean checkRegexMatch(String label) {
    Matcher labelMatcher = compiledMatcher.matcher(label);
    List<String> matches = new ArrayList<>();

    while (labelMatcher.find()) {
      matches.add(labelMatcher.group());
    }

    if (matches.size() == 0) {
      logger.debug("No extraction match key '{}' (regex: '{}') found for label '{}'.",
          keyName, matcher, label);
      return false;
    }

    if (matches.size() > 1) {
      logger.warn("Found more than 1 matches for key '{}' (regex: '{}'): '{}' , in label '{}'.",
          keyName, matcher, String.join("; ", matches), label);
    }

    return true;
  }

  public String getExtractValue() {
    return extractValue;
  }

  public String getMatcher() {
    return matcher;
  }

  public String getKeyName() {
    return keyName;
  }

  @Override
  public String toString() {
    return "Extractor{" +
        "keyName='" + keyName + '\'' +
        ", extractValue='" + extractValue + '\'' +
        ", matcher='" + matcher + '\'' +
        '}';
  }

  public String getParentKeyName() {
    return parentKeyName;
  }

  public void setParentKeyName(String parentKeyName) {
    this.parentKeyName = parentKeyName;
  }

  public String getParentValue() {
    return parentValue;
  }

  public void setParentValue(String parentValue) {
    this.parentValue = parentValue;
  }
}

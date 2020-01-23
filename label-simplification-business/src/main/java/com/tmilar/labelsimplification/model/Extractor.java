package com.tmilar.labelsimplification.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

  private Integer priority = 0;
  // regex to use for check for a match.
  private final String matcher;
  private final Pattern compiledMatcher;

  // data ref to parent extractor
  private String parentPath = "";

  // current path based on keyName, extractValue, and priority
  private String currentPath = "";

  private String category;

  public Extractor(String keyName, String extractValue, String matcher) {
    this.keyName = keyName;
    this.extractValue = extractValue;
    // escape empty matcher to end-of-string
    this.matcher = Objects.equals(matcher, "") ? "$" : matcher;

    // pre-compile pattern matcher.
    this.compiledMatcher = Pattern.compile(matcher, Pattern.CASE_INSENSITIVE);
  }

  public Extractor(String keyName, String extractValue, String matcher, String parentPath,
      Integer priority, String category) {
    this(keyName, extractValue, matcher);
    this.parentPath = parentPath;
    this.priority = priority;
    this.category = category;

    this.currentPath = buildCurrentPath();
  }

  public String extract(String label) {
    if (matcher == null) {
      logger.warn("Matcher for key '{}' was null - can't apply to label '{}'", label);
    }

    if (checkRegexMatch(label)) {
      return extractValue;
    }

    return null;
  }

  private Boolean checkRegexMatch(String label) {
    List<String> matches = findRegexMatches(label);

    if (matches.size() == 0) {
      return false;
    }

    if (matches.size() > 1) {
      logger.warn("Found more than 1 matches for key '{}' (regex: '{}'): '{}' , in label '{}'.",
          keyName, matcher, String.join("; ", matches), label);
    }

    return true;
  }

  public List<String> findRegexMatches(String label) {
    boolean hasEmptyMatchers = Arrays.stream(matcher.split("\\|", -1))
        .anyMatch(m -> Objects.equals(m, ""));
    if (hasEmptyMatchers) {
      // 'any' matcher -> return immediately
      logger.debug("Empty extraction of key '{}' (regex 'any match': '{}') for label '{}'.",
          keyName, matcher, label);

      return Collections.singletonList("");
    }

    Matcher labelMatcher = compiledMatcher.matcher(label);
    List<String> matches = new ArrayList<>();

    while (labelMatcher.find()) {
      int groups = labelMatcher.groupCount();
      if(groups == 0) {
        matches.add(labelMatcher.group());
      }
      if(groups > 1) {
        logger.debug("Match with more than 1 groups ({}) for label '{}', regex: '{}'", matches.size(), label, matcher);
        for (int i = 0; i < groups; i++) {
          String groupMatch = labelMatcher.group(i);
          if(groupMatch != null) {
            matches.add(groupMatch);
          }
        }
      }
    }
    if(matches.size() > 1) {
      logger.info("More than 1 matches ({}) for label '{}', regex: '{}'", matches.size(), label, matcher);
    }
    return matches;
  }

  private String buildCurrentPath() {
    if(keyName == null || extractValue == null) {
      return "";
    }
    String currentPath = String.format("%s.%s[%d]", keyName, extractValue, priority);
    String parent = parentPath;
    if (parent.length() > 0) {
      parent += ".";
    }
    return parent + currentPath;
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

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public String getParentPath() {
    return parentPath;
  }

  public void setParentPath(String parentPath) {
    this.parentPath = parentPath;
  }

  public String getCurrentPath() {
    return currentPath;
  }

  public String getCategory() {
    return category;
  }
}

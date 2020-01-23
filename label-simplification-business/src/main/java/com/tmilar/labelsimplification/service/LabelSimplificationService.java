package com.tmilar.labelsimplification.service;

import com.tmilar.labelsimplification.model.Extractor;
import com.tmilar.labelsimplification.model.Label;
import com.tmilar.labelsimplification.model.SimplifiedLabel;
import com.tmilar.labelsimplification.util.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LabelSimplificationService {

  private static final Logger logger = LogManager.getLogger(LabelSimplificationService.class);
  public static final String REMAINDER_KEY_NAME = "REMAINDER";

  private Map<String, Set<String>> categoryKeysSet;
  private Map<String, TreeNode<Extractor>> catExtractionsTreeRoot;

  public void load(List<Extractor> extractors) {
    Extractor rootExtractor = new Extractor(null, null, "");
    catExtractionsTreeRoot = new LinkedHashMap<>();

    categoryKeysSet = new LinkedHashMap<>();

    extractors.forEach(extractor -> {
      String keyName = extractor.getKeyName();
      String extractedValue = extractor.getExtractValue();
      String matcher = extractor.getMatcher();
      String parentPath = extractor.getParentPath();
      Integer priority = extractor.getPriority();
      String category = extractor.getCategory();

      if (!categoryKeysSet.containsKey(category)) {
        LinkedHashSet<String> keysSet = new LinkedHashSet<>();
        keysSet.add(REMAINDER_KEY_NAME);
        categoryKeysSet.put(category, keysSet);
        catExtractionsTreeRoot.put(category, new TreeNode<>(rootExtractor));
      }

      categoryKeysSet.get(category).add(keyName);
      TreeNode<Extractor> extractionsTreeRoot = catExtractionsTreeRoot.get(category);

      // find parent by parentKeyName & parentKeyValue.
      // if parent present -> find child node by keyName
      // if parent not present -> fail (must match some parent, at least the null root)

      boolean isRootKey = parentPath == null
          || Objects.equals(parentPath, "")
          || Objects.equals(parentPath, "null");

      String parentKey = isRootKey ? "" : parentPath;

      Optional<TreeNode<Extractor>> parentNodeOpt = extractionsTreeRoot.findTreeNodeBy(e ->
          Objects.equals(e.getCurrentPath(), parentKey));

      boolean isParentNodePresent = parentNodeOpt.isPresent();

      if (!isParentNodePresent) {
        logger.error(
            "Required Parent node [path: '{}'] not found, can't add child node [key: '{}', extractValue: '{}']",
            parentPath, keyName, extractedValue);
        return;
      }

      // parent IS present. Add as new child node to the parent.
      TreeNode<Extractor> parentNode = parentNodeOpt.get();

      Optional<TreeNode<Extractor>> childNodeOptional = parentNode.findTreeNodeBy(e ->
          Objects.equals(e.getCurrentPath(), extractor.getCurrentPath())
      );

      boolean isChildAlreadyPresent = childNodeOptional.isPresent();

      if (!isChildAlreadyPresent) {
        // add the current as child , first time.
        parentNode.addChild(extractor);
      } else {
        // get existing node, append the matcher regex.
        TreeNode<Extractor> extractorTreeNode = childNodeOptional.get();
        Extractor previous = extractorTreeNode.data;

        String combinedMatcher = previous.getMatcher() + "|" + matcher;

        Extractor combinedExtractor = new Extractor(
            keyName, extractedValue, combinedMatcher, parentPath, priority, category);
        extractorTreeNode.data = combinedExtractor;
      }
    });
  }

  /**
   * Visit tree nodes in-order, depth-first search.
   * Run a pre-defined visitor function, which also decides if traversal should proceed to children.
   *
   * @param visitor - node visitor fn. Return true if should traverse to children, false otherwise.
   */
  private void visitTree(TreeNode<Extractor> treeNode,
      Function<TreeNode<Extractor>, Boolean> visitor) {

    // visit {tree} node.
    // If node.key is in map, save the value, and continue to children.
    // If not, don't (just continue on the same level).

    Boolean traverse = visitor.apply(treeNode);

    if (!traverse) {
      return;
    }

    List<TreeNode<Extractor>> children = treeNode.children;

    children.forEach(node -> visitTree(node, visitor));
  }

  public SimplifiedLabel simplifyLabel(Label label) {
    Map<String, List<Pair<Extractor, String>>> extractionsMap = new HashMap<>();

    String labelStr = label.getLabel();
    String category = label.getCategory();

    if (!categoryKeysSet.containsKey(category)) {
      logger.debug("Label category '{}' not mapped (label: '{}'), returning empty label",
          category, labelStr);
      return new SimplifiedLabel(label, "");
    }

    Set<String> keysSet = categoryKeysSet.get(category);
    TreeNode<Extractor> extractionsTreeRoot = catExtractionsTreeRoot.get(category);

    Function<TreeNode<Extractor>, Boolean> treeNodeVisitor = node -> {
      if (node.parent == null) {
        // root node has no logic, just traverse to children.
        return true;
      }

      Extractor extractor = node.data;

      // initialize extractionsMap for keyName
      String keyName = extractor.getKeyName();

      if (!extractionsMap.containsKey(keyName)) {
        extractionsMap.put(keyName, new ArrayList<>());
      }

      // try extract value
      String extracted = extractor.extract(labelStr);
      if (extracted == null) {
        // should not traverse.
        return false;
      }

      List<Pair<Extractor, String>> keyExtractions = extractionsMap.get(keyName);
      keyExtractions.add(Pair.of(extractor, extracted));
      // matched -> next should traverse to children (if any).
      return true;
    };

    visitTree(extractionsTreeRoot, treeNodeVisitor);

    List<String> labelExtractions = new ArrayList<>();
    Map<String, List<String>> regexMatches = new LinkedHashMap<>();

    // for each key, retrieve it's extraction.
    keysSet.forEach(key -> {
      if (!extractionsMap.containsKey(key)) {
        if (!Objects.equals(key, REMAINDER_KEY_NAME)) {
          logger.error("No key '{}' present in extractionsMap for label '{}' "
                  + "(should not happen, check if all matchers were key-mapped properly).",
              key, labelStr);
        }
        return;
      }

      List<Pair<Extractor, String>> keyExtractions = extractionsMap.get(key);

      if (keyExtractions.isEmpty()) {
        logger.debug("No extractions for key '{}' matched in label '{}'", key, labelStr);
        return;
      }
      if (keyExtractions.size() > 1) {
        String keyExtractionsListStr = keyExtractions.stream()
            .map(e -> e.getValue() + " (" + e.getKey().getPriority() + ")")
            .collect(Collectors.joining(", "));
        logger.debug("More than 1 extractions ({}) for key '{}' matched in label '{}' -> '{}'",
            keyExtractions.size(), key, labelStr, keyExtractionsListStr);
      }

      Pair<Extractor, String> firstExtractionPair = Collections.max(
          keyExtractions,
          Comparator.comparing(k -> k.getKey().getPriority())
      );

      // grab the highest-priority matched extraction.
      labelExtractions.add(firstExtractionPair.getValue());
      // grab the regex matches, used to calculate remainder later.
      List<String> extractorRegexMatches = firstExtractionPair.getKey().findRegexMatches(labelStr);
      regexMatches.put(key, extractorRegexMatches);
    });

    // get remainder, then append to labelExtractions & extractionsMap
    String cleanRemainder = computeRemainder(labelStr, regexMatches);

    if (cleanRemainder.length() > 0) {
      labelExtractions.add(cleanRemainder);
      extractionsMap.put(
          REMAINDER_KEY_NAME,
          Collections.singletonList(Pair.of(null, cleanRemainder))
      );
    }

    String simplifiedString = String.join(" ", labelExtractions);

    SimplifiedLabel simplifiedLabel = new SimplifiedLabel(labelStr, simplifiedString,
        extractionsMap);
    return simplifiedLabel;
  }

  private String computeRemainder(String label, Map<String, List<String>> regexMatches) {
    String remainder = label; // initialize as full label, then remove the matches.
    for (List<String> matches : regexMatches.values()) {
      for (String match : matches) {
        if (match.length() == 0) {
          continue; // skip empty matches
        }
        remainder = remainder.replace(match, "#");
      }
    }
    return remainder.replaceAll("#", "").trim();
  }

  public Map<String, Set<String>> getCategoryMappings() {
    return categoryKeysSet;
  }
}

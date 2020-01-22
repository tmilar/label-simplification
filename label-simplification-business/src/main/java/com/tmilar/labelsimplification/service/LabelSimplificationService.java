package com.tmilar.labelsimplification.service;

import com.tmilar.labelsimplification.model.Extractor;
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

  private Set<String> keysSet;
  private TreeNode<Extractor> extractionsTreeRoot;

  public void load(List<Extractor> extractors) {
    Extractor rootExtractor = new Extractor(null, null, "");
    extractionsTreeRoot = new TreeNode<>(rootExtractor);

    Map<String, TreeNode<Extractor>> treeNodeMap = new HashMap<>();
    treeNodeMap.put(null, extractionsTreeRoot);

    keysSet = new LinkedHashSet<>();

    extractors.forEach(extractor -> {
      String keyName = extractor.getKeyName();
      String extractedValue = extractor.getExtractValue();
      String matcher = extractor.getMatcher();
      String parentPath = extractor.getParentPath();
      Integer priority = extractor.getPriority();

      keysSet.add(keyName);

      // find parent by parentKeyName & parentKeyValue.
      // if parent present -> find child node by keyName
      // if parent not present -> fail (must match some parent, at least the null root)

      boolean isRootKey = parentPath == null
          || Objects.equals(parentPath, "")
          || Objects.equals(parentPath, "null");

      String parentKey = isRootKey ? null : parentPath;

      boolean isParentNodePresent = treeNodeMap.containsKey(parentKey);

      if (!isParentNodePresent) {
        logger.error(
            "Required Parent node [path: '{}'] not found, can't add child node [key: '{}', extractValue: '{}']",
            parentPath, keyName, extractedValue);
        return;
      }

      // parent IS present. Add as new child node to the parent.
      TreeNode<Extractor> parentNode = treeNodeMap.get(parentKey);

      String currentExtractorKey = extractor.getCurrentPath();

      Optional<TreeNode<Extractor>> childNodeOptional = parentNode.findTreeNodeBy(e ->
              Objects.equals(e.getCurrentPath(), extractor.getCurrentPath())
      );

      boolean isChildAlreadyPresent = childNodeOptional.isPresent();

      if (!isChildAlreadyPresent) {
        // add the current as child , first time.
        TreeNode<Extractor> currentExtractorTreeNode = parentNode.addChild(extractor);
        treeNodeMap.put(currentExtractorKey, currentExtractorTreeNode);
      } else {
        // get existing node, append the matcher regex.
        TreeNode<Extractor> extractorTreeNode = childNodeOptional.get();
        Extractor previous = extractorTreeNode.data;

        String combinedMatcher = previous.getMatcher() + "|" + matcher;

        Extractor combinedExtractor = new Extractor(
            keyName, extractedValue, combinedMatcher, parentPath, priority);
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

  public SimplifiedLabel simplifyLabel(String label) {
    Map<String, List<Pair<Extractor, String>>> extractionsMap = new HashMap<>();

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
      String extracted = extractor.extract(label);
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
        logger.error("No key '{}' present in extractionsMap for label '{}' "
                + "(should not happen, check if all matchers were key-mapped properly).",
            key, label);
        return;
      }

      List<Pair<Extractor, String>> keyExtractions = extractionsMap.get(key);

      if (keyExtractions.isEmpty()) {
        logger.debug("No extractions for key '{}' matched in label '{}'", key, label);
        return;
      }
      if (keyExtractions.size() > 1) {
        String keyExtractionsListStr = keyExtractions.stream()
            .map(e -> e.getValue() + "(" + e.getKey().getPriority() + ")")
            .collect(Collectors.joining(", "));
        logger.debug("More than 1 extractions ({}) for key '{}' matched in label '{}' -> '{}'",
            keyExtractions.size(), key, label, keyExtractionsListStr);
      }
      Pair<Extractor, String> firstExtractionPair = Collections.max(
          keyExtractions,
          Comparator.comparing(k -> k.getKey().getPriority())
      );

      labelExtractions.add(firstExtractionPair.getValue()); // grab the first matched extraction.
      regexMatches.put(key, firstExtractionPair.getKey()
          .findRegexMatches(label)); // grab the regex matches, used to calculate remainder later.
    });

    // get remainder, then append to labelExtractions & extractionsMap
    String cleanRemainder = computeRemainder(label, regexMatches);

    if (cleanRemainder.length() > 0) {
      labelExtractions.add(cleanRemainder);
      extractionsMap.put("REMAINDER", Collections.singletonList(Pair.of(null, cleanRemainder)));
    }

    String simplifiedString = String.join(" ", labelExtractions);

    SimplifiedLabel simplifiedLabel = new SimplifiedLabel(label, simplifiedString, extractionsMap);
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

}

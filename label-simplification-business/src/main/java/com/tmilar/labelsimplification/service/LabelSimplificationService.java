package com.tmilar.labelsimplification.service;

import com.tmilar.labelsimplification.model.Extractor;
import com.tmilar.labelsimplification.model.SimplifiedLabel;
import com.tmilar.labelsimplification.util.TreeNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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

      String parentKeyName = extractor.getParentKeyName();
      String parentValue = extractor.getParentValue();

      keysSet.add(keyName);

      // find parent by parentKeyName & parentKeyValue.
      // if parent present -> find child node by keyName
      // if parent not present -> fail (must match some parent, at least the null root)

      boolean isRootKey = parentKeyName == null
          || Objects.equals(parentKeyName, "")
          || Objects.equals(parentKeyName, "null");

      String parentKey = isRootKey ? null : String.join(".", parentKeyName, parentValue);

      boolean isParentNodePresent = treeNodeMap.containsKey(parentKey);

      if (!isParentNodePresent) {
        logger.error(
            "Required Parent node [key: '{}', extractValue: '{}'] not found, can't add child node [key: '{}', extractValue: '{}']",
            parentKeyName, parentValue, keyName, extractedValue);
        return;
      }

      // parent IS present. Add as new child node to the parent.
      TreeNode<Extractor> parentNode = treeNodeMap.get(parentKey);

      String currentExtractorKey = String.join(".", keyName, extractedValue);

      Optional<TreeNode<Extractor>> childNodeOptional = parentNode.findTreeNodeBy(e ->
          Objects.equals(e.getKeyName(), keyName)
              && Objects.equals(e.getExtractValue(), extractedValue));

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

        Extractor combinedExtractor = new Extractor(extractor.getKeyName(),
            extractor.getExtractValue(), combinedMatcher, extractor.getParentKeyName(),
            extractor.getParentValue());
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
    logger.debug("Visiting: {}", treeNode);

    Boolean traverse = visitor.apply(treeNode);

    if (!traverse) {
      return;
    }

    List<TreeNode<Extractor>> children = treeNode.children;

    if (children.size() > 0) {
      logger.debug("Traversing to children: [{}]", children);
    } else {
      logger.debug("This is leaf, no further children to traverse to.");
    }

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

    logger.debug("Start extraction of label '{}'", label);
    visitTree(extractionsTreeRoot, treeNodeVisitor);

    List<String> labelExtractions = new ArrayList<>();

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
        logger.debug("More than 1 extractions for key '{}' matched in label '{}'", key, label);
      }
      Pair<Extractor, String> firstExtractionPair = keyExtractions.get(0);

      labelExtractions.add(firstExtractionPair.getValue()); // grab the first matched extraction.
    });

    String simplifiedString = String.join(" ", labelExtractions);

    SimplifiedLabel simplifiedLabel = new SimplifiedLabel(label, simplifiedString, extractionsMap);
    return simplifiedLabel;
  }

}

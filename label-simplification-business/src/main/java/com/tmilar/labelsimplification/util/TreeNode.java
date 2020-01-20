package com.tmilar.labelsimplification.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class TreeNode<T> {

  public T data;
  public TreeNode<T> parent;
  public List<TreeNode<T>> children;

  private List<TreeNode<T>> elementsIndex;

  public boolean isRoot() {
    return parent == null;
  }

  public boolean isLeaf() {
    return children.size() == 0;
  }

  public TreeNode(T data) {
    this.data = data;
    this.children = new LinkedList<>();
    this.elementsIndex = new LinkedList<>();
    this.elementsIndex.add(this);
  }

  public TreeNode<T> addChild(T child) {
    TreeNode<T> childNode = new TreeNode<T>(child);
    childNode.parent = this;
    this.children.add(childNode);
    this.registerChildForSearch(childNode);
    return childNode;
  }

  public int getLevel() {
    if (this.isRoot()) {
      return 0;
    } else {
      return parent.getLevel() + 1;
    }
  }

  private void registerChildForSearch(TreeNode<T> node) {
    elementsIndex.add(node);
    if (parent != null) {
      parent.registerChildForSearch(node);
    }
  }

  public TreeNode<T> findTreeNode(Comparable<T> cmp) {
    for (TreeNode<T> element : this.elementsIndex) {
      T elData = element.data;
      if (cmp.compareTo(elData) == 0) {
        return element;
      }
    }

    return null;
  }

  @Override
  public String toString() {
    return data != null ? data.toString() : "[data null]";
  }

  public Optional<TreeNode<T>> findTreeNodeBy(Predicate<T> comparator) {
    return this.elementsIndex.stream()
        .filter(node -> comparator.test(node.data))
        .findFirst();
  }
}
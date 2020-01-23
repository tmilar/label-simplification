package com.tmilar.labelsimplification.model;

public class Label {

  /**
   * Label string to process.
   */
  protected String label;
  protected String category;

  public Label(String label) {
    this.label = label;
  }

  public Label(String label, String category) {
    this.label = label;
    this.category = category;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  @Override
  public String toString() {
    return "Label{" +
        "label='" + label + '\'' +
        '}';
  }
}

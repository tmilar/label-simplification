package com.tmilar.labelsimplification.model;

public class Label {

  /**
   * Label string to process.
   */
  protected String label;

  public Label(String label) {
    this.label = label;
  }

  public Label(String label, String ruleCode) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

}

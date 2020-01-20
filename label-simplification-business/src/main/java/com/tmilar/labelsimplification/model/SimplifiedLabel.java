package com.tmilar.labelsimplification.model;

public class SimplifiedLabel extends Label {

  /**
   * The label result after simplification step process.
   */
  private String simplifiedLabel;

  public SimplifiedLabel(Label labelEntity, String simplfiedLabel) {
    super(labelEntity.getLabel());
    this.simplifiedLabel = simplfiedLabel;
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
}

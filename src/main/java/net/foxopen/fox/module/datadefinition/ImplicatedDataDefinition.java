package net.foxopen.fox.module.datadefinition;

import net.foxopen.fox.dom.DOM;

public class ImplicatedDataDefinition {
  private final String mDataDefinitionName;
  private final String mMatchPath;
  private final String mFoxDataKeyPath;

  public ImplicatedDataDefinition(DOM pImplicatedDataDefinitionElement) {
    mDataDefinitionName = pImplicatedDataDefinitionElement.value();
    mMatchPath = pImplicatedDataDefinitionElement.getAttrOrNull("match");
    mFoxDataKeyPath = pImplicatedDataDefinitionElement.getAttrOrNull("foxDataKey");
  }

  public String getDataDefinitionName() {
    return mDataDefinitionName;
  }

  public String getMatchPath() {
    return mMatchPath;
  }

  public String getFoxDataKeyPath() {
    return mFoxDataKeyPath;
  }
}

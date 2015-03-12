package net.foxopen.fox.module.fieldset.action;

import java.util.Map;


public class InternalActionContext {
  
  private final String mFieldSetLabel;
  private final String mInternalActionId;

  public InternalActionContext(String pFieldSetLabel, String pInternalActionId) {
    mFieldSetLabel = pFieldSetLabel;
    mInternalActionId = pInternalActionId;
  }
  
  public String generateActionRef() {
    return mFieldSetLabel + "/" + mInternalActionId;
  }
  
  public String generateActionRef(Map<String, String> pParams) {
    
    StringBuilder lActionRefBuilder = new StringBuilder(generateActionRef() + "?");
    for(Map.Entry<String, String> lParamEntry : pParams.entrySet()) {
      lActionRefBuilder.append(lParamEntry.getKey() + "=" + lParamEntry.getValue());
    }
    
    return lActionRefBuilder.toString();
  }
  
}

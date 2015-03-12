package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExModule;

/**
 * Definition of an action as defined in module or state markup.
 */
public class ActionDefinition
implements Validatable {

  private final String mActionName;
  private final XDoCommandList mXDoCommandList;
  private final NamespaceAttributeTable mNamespaceAttributes;
  private final AutoActionType mAutoActionType; //Can be null
  private final boolean mApplyFlag;

  public static ActionDefinition createActionDefinition(DOM pDefinitionElement, Mod pModule)
  throws ExModule, ExDoSyntax {
    return createActionDefinition(pDefinitionElement, pModule, "");
  }

  public static ActionDefinition createActionDefinition(DOM pDefinitionElement, Mod pModule, String pActionPurposePrefix)
  throws ExModule, ExDoSyntax {

    String lActionName = pDefinitionElement.getAttr("name");
    if (XFUtil.isNull(lActionName)) {
      throw new ExModule("Could not find action name: ", pDefinitionElement);
    }

    String lPurpose = lActionName;
    if(!XFUtil.isNull(pActionPurposePrefix)) {
      lPurpose  =  pActionPurposePrefix + "/" + lActionName;
    }

    DOM lDoElem;
    try {
      lDoElem = pDefinitionElement.get1E("fm:do");
    }
    catch (ExCardinality x) {
      throw new ExModule("Error retrieving do block from action: "+lPurpose, pDefinitionElement);
    }

    XDoCommandList lXDoCommandList = new XDoCommandList(pModule, lDoElem, lPurpose);

    // Extract and process auto apply command attribute "no-apply"
    boolean lApplyFlag = true;
    if (pDefinitionElement.hasAttr(Mod.NO_APPLY_ATTRIBUTE)) {
      lApplyFlag = !XFUtil.stringBoolean(pDefinitionElement.getAttr(Mod.NO_APPLY_ATTRIBUTE));
    }

    //Establish auto action type
    AutoActionType lAutoActionType = AutoActionType.getTypeFromActionName(lActionName);

    return new ActionDefinition(lActionName, lXDoCommandList, pDefinitionElement.getNamespaceAttributeTable(), lAutoActionType, lApplyFlag);
  }

  private ActionDefinition(String pActionName, XDoCommandList pXDoCommandList, NamespaceAttributeTable pNamespaceAttributes, AutoActionType pAutoActionType, boolean pApplyFlag) {
    mActionName = pActionName;
    mXDoCommandList = pXDoCommandList;
    mNamespaceAttributes = pNamespaceAttributes;
    mAutoActionType = pAutoActionType;
    mApplyFlag = pApplyFlag;
  }

  public String getActionName() {
    return mActionName;
  }

  public XDoCommandList getXDoCommandList() {
    return mXDoCommandList;
  }

  public boolean isApplyFlag() {
    return mApplyFlag;
  }

  public NamespaceAttributeTable getNamespaceAttributeTable() {
    return mNamespaceAttributes;
  }

  @Override
  public void validate(Mod pModule) {
    mXDoCommandList.validate(pModule);
  }

  public boolean isAutoAction() {
    return mAutoActionType != null;
  }

  public AutoActionType getAutoActionType() {
    return mAutoActionType;
  }
}

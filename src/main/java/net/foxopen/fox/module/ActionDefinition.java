package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Definition of an action as defined in module or state markup.
 */
public class ActionDefinition
implements Validatable {

  private final String mActionName;
  private final String mStateName;
  private final XDoCommandList mXDoCommandList;
  private final NamespaceAttributeTable mNamespaceAttributes;
  private final AutoActionType mAutoActionType; //Can be null
  private final boolean mApplyFlag;

  private final Collection<InvokePrecondition> mInvokePreconditions;

  public static ActionDefinition createActionDefinition(DOM pDefinitionElement, Mod pModule)
  throws ExModule, ExDoSyntax {
    return createActionDefinition(pDefinitionElement, pModule, "");
  }

  public static ActionDefinition createActionDefinition(DOM pDefinitionElement, Mod pModule, String pStateName)
  throws ExModule, ExDoSyntax {

    String lActionName = pDefinitionElement.getAttr("name");
    if (XFUtil.isNull(lActionName)) {
      throw new ExModule("Could not find action name: ", pDefinitionElement);
    }

    String lPurpose = lActionName;
    if(!XFUtil.isNull(pStateName)) {
      lPurpose  =  pStateName + "/" + lActionName;
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

    //Parse preconditions
    Collection<InvokePrecondition> lInvokePreconditions = new HashSet<>();
    for(DOM lRequiresDefinition : pDefinitionElement.getUL("fm:requires/*")) {
      String lNameAttr = lRequiresDefinition.getAttr("name");
      String lElemName = lRequiresDefinition.getLocalName();

      if(XFUtil.isNull(lNameAttr)) {
        throw new ExDoSyntax("fm:requires " + lElemName + " definition missing mandatory name attribute");
      }

      if("variable".equals(lElemName)) {
        lInvokePreconditions.add(new VariablePrecondition(lNameAttr));
      }
      else if("context".equals(lElemName)) {
        lInvokePreconditions.add(new ContextLabelPrecondition(lNameAttr));
      }
    }

    return new ActionDefinition(lActionName, pStateName, lXDoCommandList, pDefinitionElement.getNamespaceAttributeTable(), lAutoActionType,
                                lApplyFlag, Collections.unmodifiableCollection(lInvokePreconditions));
  }

  private ActionDefinition(String pActionName, String pStateName, XDoCommandList pXDoCommandList, NamespaceAttributeTable pNamespaceAttributes, AutoActionType pAutoActionType,
                           boolean pApplyFlag, Collection<InvokePrecondition> pInvokePreconditions) {
    mActionName = pActionName;
    mStateName = pStateName;
    mXDoCommandList = pXDoCommandList;
    mNamespaceAttributes = pNamespaceAttributes;
    mAutoActionType = pAutoActionType;
    mApplyFlag = pApplyFlag;
    mInvokePreconditions = pInvokePreconditions;
  }

  public String getActionName() {
    return mActionName;
  }

  /**
   * Creates an ActionIdentifer which can be used to identify and run this ActionDefintion.
   * @param pIncludeStateName If true, the state name will be included in the identifier. This should be true for actions
   *                          which are not in the current state.
   * @return New ActionIdentifier for this action.
   */
  public ActionIdentifier createActionIdentifier(boolean pIncludeStateName) {
    return new ActionIdentifier(pIncludeStateName ? mStateName : null, mActionName);
  }

  public XDoCommandList checkPreconditionsAndGetCommandList(ActionRequestContext pRequestContext) {
    //Validate that the request context is in the correct state to run this action
    mInvokePreconditions.forEach(e -> e.validate(pRequestContext));
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

  /**
   * A condition which must be satisfied before an action can be invoked.
   */
  private interface InvokePrecondition {
    void validate(ActionRequestContext pRequestContext);
  }

  /**
   * Precondition which requires that a local variable must be set.
   */
  private static class VariablePrecondition
  implements InvokePrecondition {
    private final String mVariableName;

    private VariablePrecondition(String pVariableName) {
      mVariableName = pVariableName;
    }

    @Override
    public void validate(ActionRequestContext pRequestContext) {
      if(!pRequestContext.getXPathVariableManager().isVariableSet(mVariableName, true)){
        throw new ExInternal("Action requires local variable " + mVariableName + " to be set");
      }
    }
  }

  /**
   * Precondition which requires that a context label must be set.
   */
  private static class ContextLabelPrecondition
  implements InvokePrecondition {

    private final String mLabelName;

    private ContextLabelPrecondition(String pLabelName) {
      mLabelName = pLabelName;
    }

    @Override
    public void validate(ActionRequestContext pRequestContext) {
      if(pRequestContext.getContextUElem().getUElemOrNull(mLabelName) == null){
        throw new ExInternal("Action requires context label " + mLabelName + " to be set");
      }
    }
  }
}

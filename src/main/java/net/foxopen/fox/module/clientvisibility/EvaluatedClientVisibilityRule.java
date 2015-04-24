package net.foxopen.fox.module.clientvisibility;


import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.RadioGroupValueFieldMgr;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * An instantiated {@link ClientVisibilityRule}. When constructed, the evaluated rule establishes as much information as
 * it can to determine its initial state. The primary purpose of this object is to generate a JSON object for use
 * by the client visibility JavaScript. This is done by calling {@link #getJSON(FieldSet)} and providing a FieldSet which
 * is ready to be sent. A FieldSet is required for resolving the FieldMgrs of trigger widgets -  amongst other things, the
 * trigger's FieldMgr is used for mapping internal mapset keys to their external values.<br/><br/>
 *
 * In this class, the "target" of the rule is the widget which is being controlled by it. A "trigger" is a widget which
 * is controlling the target. A rule will always have one target and zero or more triggers.<br/><br/>
 *
 * EvaluatedClientVisibilityRules should not be permanently cached or reused - they are transient and tied to a single
 * run of an HtmlGenerator.
 *
 */
public class EvaluatedClientVisibilityRule {

  /** External FOX id data attribute which this rule applies to. Mutually exclusive with mTargetEvalNodeInfo. */
  private final String mTargetExternalFoxId;

  /** ENI to do JIT external FOX ID retrieval. */
  private final EvaluatedNodeInfo mTargetEvalNodeInfo;

  /** The root level evaluated operation. This may contain nested operations. */
  private final EvaluatedOperation mEvaluatedOperation;

  /** Name of the rule, taken from the original ClientVisibilityRule. Used for debug error messages. */
  private final String mRuleName;

  /** If true, the CSS "visibility" property will be used to hide contents. If false then "display" is used. */
  private final boolean mToggleVisibility;

  /** If true, the contents of the target elements are hidden by CSS rules/Javascript.
   * If false, the target elements themselves are hidden. */
  private final boolean mHideContents;

  private static final String JSON_OPERATION_TARGET_XFID_NAME = "target_xfid";
  private static final String JSON_OPERATION_TYPE_NAME = "operation_type";
  private static final String JSON_OPERATION_TYPE_TEST = "test";
  private static final String JSON_OPERATION_LIST_NAME = "operation_list";
  private static final String JSON_INVERT_CONDITION_NAME = "invert_condition";
  private static final String JSON_OPERATION_TEST_TYPE_NAME = "test_type";
  private static final String JSON_OPERATION_TEST_TYPE_FIXED = "fixed";
  private static final String JSON_OPERATION_TEST_TYPE_WIDGET = "widget";
  private static final String JSON_OPERATION_FIXED_VALUE_NAME = "fixed_value";
  private static final String JSON_OPERATION_TRIGGER_VALUE_NAME = "trigger_value";
  private static final String JSON_OPERATION_TRIGGER_NAME_NAME = "trigger_name";
  private static final String JSON_TOGGLE_VISIBILITY_NAME = "toggle_visibility";
  private static final String JSON_HIDE_CONTENTS_NAME = "hide_contents";

  public static final String CSS_HIDE_VISIBILITY_CONTENTS_CLASS_NAME = "clv-hide-visibility-contents";
  public static final String CSS_HIDE_VISIBILITY_CLASS_NAME = "clv-hide-visibility";
  public static final String CSS_HIDE_DISPLAY_CONTENTS_CLASS_NAME = "clv-hide-display-contents";
  public static final String CSS_HIDE_DISPLAY_CLASS_NAME = "clv-hide-display";

  public static final String EXTERNAL_CLV_ID_PREFIX = "clv-";


  /**
   * Constructs a new EvaluatedClientVisibilityRule which only targets a CSS class (instead of a specific widget).
   * @param pRule ClientVisibilityRule to evaluate.
   * @param pExternalFoxId External foxid which will be this rule's target.
   * @param pRelativeDOM Relative DOM for XPath evaluation.
   * @param pContextUElem Context for XPath evaluation.
   */
  EvaluatedClientVisibilityRule(ClientVisibilityRule pRule, String pExternalFoxId, DOM pRelativeDOM, ContextUElem pContextUElem) {
    mTargetExternalFoxId = pExternalFoxId;
    mTargetEvalNodeInfo = null;

    mRuleName = pRule.getRuleName();
    try {
      mToggleVisibility = pContextUElem.extendedXPathBoolean(pRelativeDOM, pRule.mUseVisibilityXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate client visibility rule " + mRuleName + ": use-css-visibility XPath (" + pRule.mUseVisibilityXPath + ") failed", e);
    }

    //As we have a pre-determined extenral fox id target it is safe to assume we will always be hiding the container element.
    mHideContents = false;

    //Evaluate the operations
    mEvaluatedOperation = evaluateOperation(pRule.mOperation, null, pRelativeDOM, pContextUElem);
  }

  /**
   * Constructs a new EvaluatedClientVisibilityRule which targets a displayed field.
   * @param pRule ClientVisibilityRule to evaluate.
   * @param pTargetNodeInfo FieldMgr of the field which this rule is targeting.
   * @param pTargetDOM DOM (item) element of the field which this rule is targeting.
   * @param pRelativeDOM Relative DOM for XPath evaluation.
   * @param pContext Context for XPath evaluation.
   */
  EvaluatedClientVisibilityRule(ClientVisibilityRule pRule, EvaluatedNodeInfo pTargetNodeInfo, DOM pTargetDOM, DOM pRelativeDOM, ContextUElem pContext) {

    pContext.localise("evaluate-cvr");
    try {
      //Set "item" context to the targeted node - not for phantoms this will be null
      if(pTargetDOM != null){
        pContext.setUElem(ContextLabel.ITEM, pTargetDOM);
      }
      pContext.setUElem(ContextLabel.ITEMREC, pRelativeDOM);

      //Store a reference to the ENI which we can ask for the external FOXID just-in-time - the external FOXID is not known at this point
      mTargetExternalFoxId = null;
      mTargetEvalNodeInfo = pTargetNodeInfo;

      mRuleName = pRule.getRuleName();
      try {
        mToggleVisibility = pContext.extendedXPathBoolean(pRelativeDOM, pRule.mUseVisibilityXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate client visibility rule " + mRuleName + ": use-css-visibility XPath (" + pRule.mUseVisibilityXPath + ") failed", e);
      }
      //Currently, for fields we always hide the contents rather than the container.
      //I.e. in a form set out we cannot hide <td> elements, which are the containers, as this disrupts table structure.
      mHideContents = true;

      //Evaluate the operations
      mEvaluatedOperation = evaluateOperation(pRule.mOperation, pTargetNodeInfo, pRelativeDOM, pContext);
    }
    finally {
      pContext.delocalise("evaluate-cvr");
    }
  }

  /**
   * Recursively evaluates Operations by creating EvaluatedOperation.
   * @param pOperation Operation to evaluate.
   * @param pTargetNodeInfo NodeInfo of the target field. May be null.
   * @param pRelativeDOM Relative DOM for XPath evaluation.
   * @param pContext Context for XPath evaluation.
   * @return A new EvaluatedOperation.
   */
  private EvaluatedOperation evaluateOperation(ClientVisibilityRule.Operation pOperation, EvaluatedNodeInfo pTargetNodeInfo, DOM pRelativeDOM, ContextUElem pContext) {
    if (pOperation instanceof ClientVisibilityRule.BinaryOperation) {
      return new EvaluatedBinaryOperation((ClientVisibilityRule.BinaryOperation) pOperation, pTargetNodeInfo, pRelativeDOM, pContext);
    }
    else if (pOperation instanceof ClientVisibilityRule.WidgetTest) {
      return new EvaluatedWidgetTest((ClientVisibilityRule.WidgetTest) pOperation, pTargetNodeInfo, pRelativeDOM, pContext);
    }
    else if (pOperation instanceof ClientVisibilityRule.FixedXPathTest) {
      return new EvaluatedFixedXPathTest((ClientVisibilityRule.FixedXPathTest) pOperation, pRelativeDOM, pContext);
    }
    else {
      throw new ExInternal("Unrecognised operation type");
    }
  }

  /**
   * This must be called after all widgets are generated, so target NodeInfos can be used to get external IDs for FVM selections.
   * @param pFieldSet
   */
  public void completeEvaluation(FieldSet pFieldSet) {
    mEvaluatedOperation.completeEvaluation(pFieldSet);
  }

  /**
   * Generates the JSON for this rule. The JSON object will contain a nested array of properties objects for individual
   * operations, as well as some top-level information about the rule (i.e. toggle visibility flag, etc).
   * @param pFieldSet The FieldSet which this rule applies to. It should be fully populated before
   * this method is called.
   * @return The JSON representation of this evaluated rule.
   */
  public JSONObject getJSON(FieldSet pFieldSet) {

    JSONObject lJSONObject = new JSONObject();

    lJSONObject.put(JSON_OPERATION_TARGET_XFID_NAME, mTargetExternalFoxId == null ? mTargetEvalNodeInfo.getExternalFoxId() : mTargetExternalFoxId);

    lJSONObject.put(JSON_TOGGLE_VISIBILITY_NAME, mToggleVisibility);
    lJSONObject.put(JSON_HIDE_CONTENTS_NAME, mHideContents);

    JSONArray lJSONArray = new JSONArray();
    //Proces the nested operations.
    lJSONArray.add(mEvaluatedOperation.getJSON(pFieldSet));

    lJSONObject.put(JSON_OPERATION_LIST_NAME, lJSONArray);

    return lJSONObject;
  }

  /**
   * Gets the initial CSS class the target element should be given when the page is served out. If the element should
   * not be hidden when the page is loaded, this method returns empty String. These CSS classes are transient and will
   * only be effective when the page first loads - they are removed by the JavaScript as soon as it needs to control
   * element visibility.
   * @return A CSS class name, or empty String if none is required.
   */
  public String getInitialCSSClass(){
    if(!mEvaluatedOperation.getBooleanResult()){
      //If the field isn't visible at the start
      if(mHideContents){
        //There are seperate CSS rules for hiding contents as opposed to directly hiding the element
        return mToggleVisibility ? CSS_HIDE_VISIBILITY_CONTENTS_CLASS_NAME : CSS_HIDE_DISPLAY_CONTENTS_CLASS_NAME;
      }
      else {
        return mToggleVisibility ? CSS_HIDE_VISIBILITY_CLASS_NAME : CSS_HIDE_DISPLAY_CLASS_NAME;
      }
    }
    else {
      return "";
    }
  }

  /**
   * Determines if this rule should hide the contents of HTML elements resolved by its target's class selector, or
   * if it should hide the target elements directly.
   * @return True if hiding target contents, false if hiding the target.
   */
  public boolean isHideContents() {
    return mHideContents;
  }

  /**
   * Repesents a {@link ClientVisibilityRule.Operation} which has been evaluated and is ready to provide output.
   */
  interface EvaluatedOperation {

    public void completeEvaluation(FieldSet pFieldSet);

    /**
     * Generates the JSON which will be used by client side JavaScript to evaluate this rule on the page as the user
     * changes field values.
     * @param pFieldSet A fully populated FieldSet.
     * @return A new JSONOject.
     */
    public JSONObject getJSON(FieldSet pFieldSet);

    /**
     * Establishes the initial result of this Operation as it will be when the page is sent. This should be called after
     * getJSON.
     * @return True if the field should be visible when the page is sent, false if not.
     */
    public boolean getBooleanResult();
  }

  /**
   * Repesents an evaluated WidgetTest Operation. A WidgetTest represents a trigger for this rule. When a widget which
   * is subject to a WidgetTest is changed by the user on the screen, the whole rule is evaluated to determine the new
   * visibility for the target. Currently triggers can only be based on widgets which support FieldValueMappings (i.e. mapsets
   * or booleans). For most purposes a boolean datatype is treated like a built-in mapset (with the exception of tickbox widgets).
   */
  private class EvaluatedWidgetTest
  implements EvaluatedOperation {

    /** Condition type for this test operation. */
    final ClientVisibilityRule.ConditionType mConditionType;

    /** Value to test for. Could be null if this is not an equality test. */
    final String mTestValue;

    /** The XPath used to determine the trigger of this operation. Stored here so better errors can be reported to developers. */
    final String mTriggerXPath;

    /** DOM element of the "trigger" for this operation. */
    final DOM mTriggerDOM;

    /** Result of evaluating this rule. Will be null before it is evaluated. */
    Boolean mEvaluationResult = null;

    /**
     * Constucts a new EvaluatedWidgetTest based on an existing WidgetTest object and some contextual information.
     * @param pWidgetTest The WidgetTest to evaluate.
     * @param pTargetNodeInfo The NodeInfo of the element being targeted by this rule, or null if an element is not targeted.
     * @param pRelativeDOM Relative DOM for XPath evaluation.
     * @param pContext Context for XPath evaluation.
     */
    private EvaluatedWidgetTest(ClientVisibilityRule.WidgetTest pWidgetTest, EvaluatedNodeInfo pTargetNodeInfo, DOM pRelativeDOM, ContextUElem pContext) {

      mConditionType = pWidgetTest.mConditionType;

      //Determine the value to test (if required)
      if(mConditionType.isEqualityCheck()){
        String lTestValue = "";
        if(pWidgetTest.mUseValueFromSchema){
          //The "value" to test for is specified on the schema and not in the rule definition itself
          if(pTargetNodeInfo == null){
            throw new ExInternal("Cannot find the client-visibility-rule-value attribute without a NodeInfo (not supported on buffers)");
          }
          lTestValue = pTargetNodeInfo.getStringAttribute(NodeAttribute.CLIENT_VISIBILITY_RULE_VALUE);

          if(XFUtil.isNull(lTestValue)){
            throw new ExInternal("Client visibility rule " + mRuleName + ": client-visibility-rule-value should be specified on schema but none found");
          }
        }
        else {
          lTestValue = pWidgetTest.mValueXPath;
        }

        //Evaluate the value as an XPath
        try {
          mTestValue = pContext.extendedStringOrXPathString(pRelativeDOM, lTestValue);
        }
        catch (ExActionFailed e) {
          throw new ExInternal("Client visibility rule " + mRuleName + ": XPath evaluation failed to establish test value " + lTestValue, e);
        }

        //Validate that the XPath returned a non-empty result
        if("".equals(mTestValue)){
          throw new ExInternal("Client visibility rule " + mRuleName + ": Test value " + lTestValue + " evaluated to empty String");
        }

      }
      else {
        mTestValue = null;
      }

      //Determine the trigger's DOM element.
      String lTriggerXPath = "";
      if(pWidgetTest.mUseTargetFromSchema){
        //The "trigger" XPath of this rule is specified on the schema and not in the rule definition itself
        if(pTargetNodeInfo == null){
          throw new ExInternal("Cannot find the client-visibility-rule-target attribute without a NodeInfo (not supported on buffers)");
        }
        lTriggerXPath = pTargetNodeInfo.getStringAttribute(NodeAttribute.CLIENT_VISIBILITY_RULE_TARGET);

        if(XFUtil.isNull(lTriggerXPath)){
          throw new ExInternal("Client visibility rule " + mRuleName + " on " + pTargetNodeInfo.getIdentityInformation() + ": client-visibility-rule-target should be specified on schema but none found");
        }
      }
      else {
        lTriggerXPath = pWidgetTest.mTargetXPath;
      }

      mTriggerXPath = lTriggerXPath;

      try {
        mTriggerDOM = pContext.extendedXPath1E(pRelativeDOM, lTriggerXPath, false);
      }
      catch (ExActionFailed | ExCardinality e) {
        String lIdentityInfo = "";
        if(pTargetNodeInfo != null) {
          lIdentityInfo = " on " + pTargetNodeInfo.getIdentityInformation();
        }
        throw new ExInternal("Client visibility rule " + mRuleName + lIdentityInfo + ": Failed to resolve XPath " + lTriggerXPath + " to a single node", e);
      }
    }

    @Override
    public void completeEvaluation(FieldSet pFieldSet) {

      //Get the FieldMgr for the field referred to by this trigger
      FieldMgr lFieldMgr = pFieldSet.getFieldMgrForFoxIdOrNull(mTriggerDOM.getRef());
      if (lFieldMgr == null) {
        throw new ExInternal("Failed to resolve a FieldMgr for Client Visibility Rule " + mRuleName + ", XPath " + mTriggerXPath + "\n" +
            "Fields targeted in rules must be displayed on the screen.");
      }

      //If it's a radio group, get the wrapped FieldMgr
      if(lFieldMgr instanceof RadioGroupValueFieldMgr) {
        lFieldMgr = ((RadioGroupValueFieldMgr) lFieldMgr).getWrappedFieldMgr();
      }

      //Assert that we are targeting a supported widget type
      if(!(lFieldMgr instanceof OptionFieldMgr)) {
        throw new ExInternal("Client visibility rule " + mRuleName + ", XPath " + mTriggerXPath + ":\n" +
          "Only selector, radio or tickbox widgets can be targeted at this time");
      }

      //Now we have a FieldMgr for the trigger we can evaluate the result.
      mEvaluationResult = calculateBooleanResult((OptionFieldMgr) lFieldMgr);
    }

    /** {@inheritDoc} */
    public JSONObject getJSON(FieldSet pFieldSet) {

      //Already checked type is OK
      FieldMgr lUncastFieldMgr = pFieldSet.getFieldMgrForFoxIdOrNull(mTriggerDOM.getRef());
      OptionFieldMgr lFieldMgr;

      //If the trigger is a radio group, get the wrapped FieldMgr
      if(lUncastFieldMgr instanceof RadioGroupValueFieldMgr) {
        lFieldMgr = (OptionFieldMgr) ((RadioGroupValueFieldMgr) lUncastFieldMgr).getWrappedFieldMgr();
      }
      else {
        lFieldMgr = (OptionFieldMgr) lUncastFieldMgr;
      }

      JSONObject lResultJSON = new JSONObject();
      lResultJSON.put(JSON_OPERATION_TYPE_NAME, JSON_OPERATION_TYPE_TEST);

      //If the trigger is read only this will always be a fixed value, so we can skip generating code for a specific widget
      //and just send out a fixed true or false.
      if(lFieldMgr.getVisibility().asInt() <= NodeVisibility.VIEW.asInt()){
        getJSONForFixedValue(lResultJSON);
        return lResultJSON;
      }

      //Establish whether to invert the logic in the case of a "not" condition type
      boolean lInvert = mConditionType == ClientVisibilityRule.ConditionType.NOT_EQUALS || mConditionType == ClientVisibilityRule.ConditionType.NOT_NULL;
      String lTestValue = mTestValue;

      if(lFieldMgr.isStrictBoolean() && "false".equalsIgnoreCase(lTestValue)) {
        //Workaround: strict booleans are only ever true or null. Therefore a test for "false" must be converted to a check for "not true".
        lTestValue = "true";
        lInvert = !lInvert;
      }

      String lTestExternalValue;
      if(mConditionType.isEqualityCheck()) {
        lTestExternalValue = lFieldMgr.getExternalValueForOption(lTestValue);
      }
      else {
        //Null/not null
        lTestExternalValue = lFieldMgr.getExternalValueForNullSelection();
      }

      String lTestExternalName;
      if(lUncastFieldMgr instanceof RadioGroupValueFieldMgr) {
        lTestExternalName = ((RadioGroupValueFieldMgr) lUncastFieldMgr).getExternalFieldName();
      }
      else {
        lTestExternalName = lFieldMgr.getExternalFieldName();
      }

      lResultJSON.put(JSON_OPERATION_TRIGGER_VALUE_NAME, lTestExternalValue);
      lResultJSON.put(JSON_OPERATION_TRIGGER_NAME_NAME, lTestExternalName);
      lResultJSON.put(JSON_OPERATION_TEST_TYPE_NAME, JSON_OPERATION_TEST_TYPE_WIDGET);
      lResultJSON.put(JSON_INVERT_CONDITION_NAME, lInvert);

      return lResultJSON;
    }

    /**
     * Sets properties on the given JSONObject so this operation acts as a fixed value.
     * @param pJSONObject JSONObject being processed.
     */
    private void getJSONForFixedValue(JSONObject pJSONObject){
      pJSONObject.put(JSON_OPERATION_TEST_TYPE_NAME, JSON_OPERATION_TEST_TYPE_FIXED);
      pJSONObject.put(JSON_OPERATION_FIXED_VALUE_NAME, getBooleanResult());
    }

    /** {@inheritDoc} */
    public boolean getBooleanResult() {
      if(mEvaluationResult == null){
        throw new ExInternal("mEvaluationResult requires evaluation");
      }
      return mEvaluationResult;
    }

    /**
     * Runs through the logic of this widget test in the same way it would be evaluated on the client side. This determines
     * the initial state of this operation at page generation time.
     * @param pFieldMgr FieldMgr of the trigger widget.
     * @return True if the condition is true.
     */
    private boolean calculateBooleanResult(OptionFieldMgr pFieldMgr) {

      boolean lResult;
      if(mConditionType.isEqualityCheck()){
        //For equals/not equals checks, see if the desired string value is selected in the FVM
        lResult = pFieldMgr.isStringValueSelected(mTestValue);
      }
      else {
        //For null checks, see if something has been selected and invert
        lResult = !pFieldMgr.isRecognisedOptionSelected();
      }

      //Invert the result for a "not" type
      return (mConditionType == ClientVisibilityRule.ConditionType.NOT_EQUALS || mConditionType == ClientVisibilityRule.ConditionType.NOT_NULL) ? !lResult : lResult;
    }
  }

  /**
   * Repesents an evaluated FixedXPathTest Operation. On construction the XPath condition is evaluated and the generated
   * JSON simply contains the fixed boolean result of the operation.
   */
  private class EvaluatedFixedXPathTest
  implements EvaluatedOperation {

    final boolean mResult;

    /**
     * Constructs a new EvaluatedFixedXPathTest by evaluating an XPath condition.
     * @param pFixedXPathTest FixedXPathTest to evaluate.
     * @param pRelativeDOM Relative DOM for XPath evaluation.
     * @param pContext Context for XPath evaluation.
     */
    private EvaluatedFixedXPathTest(ClientVisibilityRule.FixedXPathTest pFixedXPathTest, DOM pRelativeDOM, ContextUElem pContext) {
      try {
        mResult = pContext.extendedXPathBoolean(pRelativeDOM, pFixedXPathTest.mTestXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate client visibility rule " + mRuleName + ": test XPath (" + pFixedXPathTest.mTestXPath + ") failed", e);
      }
    }

    @Override
    public void completeEvaluation(FieldSet pFieldSet) {
      //Nothing to do, result is already calculated
    }

    /** {@inheritDoc } */
    public JSONObject getJSON(FieldSet pFieldSet) {
      JSONObject lResultJSON = new JSONObject();
      lResultJSON.put(JSON_OPERATION_TYPE_NAME, JSON_OPERATION_TYPE_TEST);
      lResultJSON.put(JSON_OPERATION_TEST_TYPE_NAME, JSON_OPERATION_TEST_TYPE_FIXED);
      lResultJSON.put(JSON_OPERATION_FIXED_VALUE_NAME, getBooleanResult());
      return lResultJSON;
    }

    /** {@inheritDoc } */
    public boolean getBooleanResult() {
      return mResult;
    }
  }

  /**
   * Represents a list of nested EvaluatedOperations which will be evaluated together with and or or logic.
   */
  private class EvaluatedBinaryOperation
  implements EvaluatedOperation {

    final List<EvaluatedOperation> mEvaluatedOperationList = new ArrayList<EvaluatedOperation>();
    final ClientVisibilityRule.BinaryOperationType mOperationType; //i.e. "and" or "or"

    /**
     * Loops through the Operations nested within a BinaryOperation and recursively evaluates them.
     * @param pOperation The BinaryOperation to be evaluated
     * @param pTargetFieldMgr The FieldMgr of the element being targeted by this rule.
     * @param pRelativeDOM Relative DOM for XPath evaluation.
     * @param pContext Context for XPath evaluation.
     */
    private EvaluatedBinaryOperation(ClientVisibilityRule.BinaryOperation pOperation, EvaluatedNodeInfo pTargetNodeInfo, DOM pRelativeDOM, ContextUElem pContext) {
      mOperationType = pOperation.mOperationType;
      for (ClientVisibilityRule.Operation lOp : pOperation.mNestedOperationList) {
        mEvaluatedOperationList.add(evaluateOperation(lOp, pTargetNodeInfo, pRelativeDOM, pContext));
      }
    }

    @Override
    public void completeEvaluation(FieldSet pFieldSet) {
      //Pass down to nested operations
      for (EvaluatedOperation lOp : mEvaluatedOperationList) {
        lOp.completeEvaluation(pFieldSet);
      }
    }

    /** {@inheritDoc } */
    public JSONObject getJSON(FieldSet pFieldSet) {
      JSONObject lResultJSON = new JSONObject();
      lResultJSON.put(JSON_OPERATION_TYPE_NAME, mOperationType.toString().toLowerCase());

      JSONArray lArray = new JSONArray();
      for (EvaluatedOperation lOp : mEvaluatedOperationList) {
        lArray.add(lOp.getJSON(pFieldSet));
      }

      lResultJSON.put(JSON_OPERATION_LIST_NAME, lArray);

      return lResultJSON;
    }

    /** {@inheritDoc } */
    public boolean getBooleanResult() {
      //Default is true for "and" (will be set to false if one is false)
      //Default is false for "or" (will be set to true if one is true)
      boolean lResult = mOperationType == ClientVisibilityRule.BinaryOperationType.AND;
      for (EvaluatedOperation lOp : mEvaluatedOperationList) {
        boolean lNestedResult = lOp.getBooleanResult();
        if(!lNestedResult && mOperationType == ClientVisibilityRule.BinaryOperationType.AND){
          lResult = false;
          break;
        }
        else if(lNestedResult && mOperationType == ClientVisibilityRule.BinaryOperationType.OR){
          lResult = true;
          break;
        }
      }
      return lResult;
    }
  }

}

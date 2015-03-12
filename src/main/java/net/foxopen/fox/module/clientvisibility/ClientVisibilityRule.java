/*

Copyright (c) 2013, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.module.clientvisibility;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Representation of a client visibility rule as defined in module markup. Client visbility rules are composed of
 * Operations which are effectively different forms of boolean logic. ClientVisibilityRules are evaluated against contextual
 * information in order to create an {@link EvaluatedClientVisibilityRule} which can then be used to determine the target's
 * visibility.
 */
public class ClientVisibilityRule {

  private static final String USE_VALUE_FROM_SCHEMA_ATTR = "use-value-from-schema";
  private static final String VALUE_ATTR = "value";
  private static final String USE_TARGET_FROM_SCHEMA_ATTR = "use-target-from-schema";
  private static final String TARGET_ATTR = "target";

  /** Unique name of the rule within the module structure */
  private final String mRuleName;

  /** The 'root' or highest level Operation defined on this rule. */
  final Operation mOperation;

  /** XPath to be used to determine if the CSS "visibility" property should be used to control the display of the target. */
  final String mUseVisibilityXPath;

  /**
   * Get the name of this rule.
   * @return Rule name.
   */
  public String getRuleName() {
    return mRuleName;
  }

  /**
   * Enum for representing fm:and and fm:or Operations.
   */
  static enum BinaryOperationType{
    AND, OR;
  }

  /** Maps condition type Strings to enums. This must be defined outside the enum itself to satisfy the compiler */
  private static final Map<String, ConditionType> gConditionTypeStringToEnumMap = new HashMap<String, ConditionType>();

  /**
   * Enum for representing the allowable condition types of a ClientVisibilityRule.
   */
  public static enum ConditionType {
    EQUALS("equals", true),
    NOT_EQUALS("not-equals", true),
    NULL("null", false),
    NOT_NULL("not-null", false);

    private final boolean mIsEqualityCheck;

    /**
     * Matches an externally entered String (i.e. from the module markup) to a ConditionType enum.
     * @param pString String to look up.
     * @return The matched condition type.
     * @throws ExModule If the condition type string is not valid.
     */
    public static ConditionType fromExternalString(String pString)
    throws ExModule {
      ConditionType lType = gConditionTypeStringToEnumMap.get(pString);
      if(lType == null){
        throw new ExModule("'" + pString + "' is not a recognised condition type");
      }
      return lType;
    }

    private ConditionType(String pExternalString, boolean pEqualityCheck){
      gConditionTypeStringToEnumMap.put(pExternalString, this);
      mIsEqualityCheck = pEqualityCheck;
    }

    /**
     * Returns true if this condition type is a check for equality (equals or not equals).
     * @return True if this is an equality check.
     */
    public boolean isEqualityCheck(){
      return mIsEqualityCheck;
    }
  }

  /**
   * Constructs a new ClientVisibilityRule by parsing module markup.
   * @param pDOM The container element of the definition (i.e. "fm:client-visibility-rule").
   * @param pMod The module currently being parsed.
   * @throws ExModule
   */
  public ClientVisibilityRule(DOM pDOM, Mod pMod)
  throws ExModule {

    mRuleName = pDOM.getAttrOrNull("name");
    if(XFUtil.isNull(mRuleName)){
      throw new ExModule("Client visiblity rule must have a name.");
    }

    mUseVisibilityXPath = XFUtil.nvl(pDOM.getAttrOrNull("use-css-visibility"), "false()");

    try {
      mOperation = parseOperation(pDOM.get1E("fm:condition/*"), pMod);
    }
    catch (ExCardinality e) {
      throw new ExModule("Client visiblity rule must have exactly one element below its fm:condition element.", e);
    }
  }

  /**
   * Creates a new {@link EvaluatedClientVisibilityRule} for a field by evaluating this rule.
   * @param pTargetFieldMgr The FieldMgr of the field which is the target of the rule. //TODO PN fix
   * @param pTargetDOM The DOM element which is the target of the rule.
   * @param pRelativeDOM The target DOM's parent, to be used as the attach point for XPath evaluation (according to the evaluate context rule).
   * @param pContext Context currently in use by the HTML generator.
   * @return A new EvaluatedClientVisibilityRule.
   */
  public EvaluatedClientVisibilityRule evaluate(EvaluatedNodeInfo pTargetNodeInfo, DOM pTargetDOM, DOM pRelativeDOM, ContextUElem pContext){
    return new EvaluatedClientVisibilityRule(this, pTargetNodeInfo, pTargetDOM, pRelativeDOM, pContext);
  }

  /**
   * Creates a new {@link EvaluatedClientVisibilityRule} for a buffer (or other named page area which is not a widget) by evaluating this rule.
   * @param pExternalFoxId The external fox id which is the target of this rule.
   * @param pRelativeDOM DOM to be used as the attach point for XPath evaluation.
   * @param pContext Context currently in use by the HTML generator.
   * @return A new EvaluatedClientVisibilityRule.
   */
  public EvaluatedClientVisibilityRule evaluate(String pExternalFoxId, DOM pRelativeDOM, ContextUElem pContext){
    return new EvaluatedClientVisibilityRule(this, pExternalFoxId, pRelativeDOM, pContext);
  }

  /** An Operation represents the various types of boolean test within a ClientVisibilityRule. */
  interface Operation {}

  /**
   * Parses a DOM element which represents an Operation.
   * @param pDOM Container element of the Operation (i.e. "fm:widget", "fm:fixed-xpath", ...)
   * @param pMod The module currently being parsed.
   * @return A new representation of an Operation.
   * @throws ExModule If syntax is invalid.
   */
  private Operation parseOperation(DOM pDOM, Mod pMod)
  throws ExModule {

    String lElemName = pDOM.getName();
    if("fm:widget".equals(lElemName)){
      return new WidgetTest(pDOM);
    }
    else if("fm:fixed-xpath".equals(lElemName)){
      return new FixedXPathTest(pDOM);
    }
    else if("fm:nested-rule".equals(lElemName)){
      String lNestedName = pDOM.getAttrOrNull("name");
      if(XFUtil.isNull(lNestedName)){
        throw new ExModule("Client visiblity rule " + getRuleName() + " - nested rule must have a 'name' attribute");
      }
      try {
        //Get the definition for the nested rule and extract its root operation
        ClientVisibilityRule lNestedRule = pMod.getClientVisibilityRuleByName(lNestedName);
        return lNestedRule.mOperation;
      }
      catch(ExModule e){
        throw new ExModule("Client visiblity rule " + getRuleName() + " - failed to find nested rule named '" + lNestedName + "'. " +
          "Note that rules must be defined before they are referenced.");
      }
    }
    else if("fm:and".equals(lElemName) || "fm:or".equals(lElemName)){
      return new BinaryOperation(pDOM, pMod);
    }
    else {
      throw new ExModule("Client visiblity rule " + getRuleName() + " - " + lElemName + " not recognised as an Operation; must be one of fm:widget, fm:nested-rule, fm:fixed-xpath, fm:and or fm:or");
    }
  }

  /**
   * Represents a visibility test Operation based on the value or nullness of a Widget.
   */
  class WidgetTest
  implements Operation {
    /** Target XPath for the widget. Can be null if mUseTargetFromSchema is true. */
    final String mTargetXPath;

    /** Condition type used by this operation. */
    final ConditionType mConditionType;

    /** XPath to be run to determine the value to be used in the operation. Can be null if mUseValueFromSchema is true. */
    final String mValueXPath;

    /** If true, the value XPath will be read from the schema markup (i.e. the xs:element) instead of the rule markup. */
    final boolean mUseValueFromSchema;

    /** If true, the target XPath will be read from the schema markup (i.e. the xs:element) instead of the rule markup. */
    final boolean mUseTargetFromSchema;

    WidgetTest(DOM pDOM)
    throws ExModule {

      //Validate markup
      if(!(XFUtil.exists(pDOM.getAttrOrNull(TARGET_ATTR)) ^ XFUtil.exists(pDOM.getAttrOrNull(USE_TARGET_FROM_SCHEMA_ATTR)))){
        throw new ExModule("Client visiblity rule " + getRuleName() + " - 'target' and 'use-target-from-schema' attributes are mutually exclusive (one must be specified)");
      }

      if(pDOM.hasAttr(USE_TARGET_FROM_SCHEMA_ATTR) && !"yes".equals(pDOM.getAttr(USE_TARGET_FROM_SCHEMA_ATTR))){
        throw new ExModule("Client visiblity rule " + getRuleName() + " - 'use-target-from-schema' attribute must be 'yes'");
      }

      mTargetXPath = pDOM.getAttrOrNull(TARGET_ATTR);
      mUseTargetFromSchema = pDOM.hasAttr(USE_TARGET_FROM_SCHEMA_ATTR);

      String lConditionTypeString = pDOM.getAttrOrNull("condition-type");
      if(XFUtil.isNull(lConditionTypeString)){
        throw new ExModule("Client visiblity rule " + getRuleName() + " - widget condition must specify a condition type ('condition-type' attribute)");
      }

      mConditionType = ConditionType.fromExternalString(lConditionTypeString);


      if(mConditionType.isEqualityCheck()){
        //For equals and not equals operations a value to check must also be specified.

        if(pDOM.hasAttr(VALUE_ATTR) && pDOM.hasAttr(USE_VALUE_FROM_SCHEMA_ATTR)){
          throw new ExModule("Client visiblity rule " + getRuleName() + " - 'value' and 'use-value-from-schema' attributes are mutually exclusive");
        }

        if(pDOM.hasAttr(USE_VALUE_FROM_SCHEMA_ATTR) && !"yes".equals(pDOM.getAttr(USE_VALUE_FROM_SCHEMA_ATTR))){
          throw new ExModule("Client visiblity rule " + getRuleName() + " - 'use-value-from-schema' attribute must be 'yes'");
        }

        mValueXPath = pDOM.getAttrOrNull(VALUE_ATTR);
        mUseValueFromSchema = pDOM.hasAttr(USE_VALUE_FROM_SCHEMA_ATTR);

        if(XFUtil.isNull(mValueXPath) && !mUseValueFromSchema){
          throw new ExModule("Client visiblity rule " + getRuleName() + " - equals/not-equals widget condition must specify a value " +
                              "('" + VALUE_ATTR + "' or '" + USE_VALUE_FROM_SCHEMA_ATTR + "' attribute)");
        }
      }
      else {
        mValueXPath = null;
        mUseValueFromSchema = false;
      }
    }
  }

  /**
   * Represents a list of nested Operations which will be evaluated together with either "and" or "or" logic.
   */
  class BinaryOperation
  implements Operation {

    /** The binary operation type (and or or) */
    final BinaryOperationType mOperationType;

    /** List of nested Operations to be evaluated **/
    final List<Operation> mNestedOperationList;

    BinaryOperation(DOM pDOM, Mod pMod)
    throws ExModule {

      String lElemName = pDOM.getName();
      if("fm:and".equals(lElemName)){
        mOperationType = BinaryOperationType.AND;
      }
      else if("fm:or".equals(lElemName)){
        mOperationType = BinaryOperationType.OR;
      }
      else {
        throw new ExModule("Operation type not supported"); //This is here to satisfy the compiler (operation type field is final)
      }

      mNestedOperationList = new ArrayList<Operation>();
      DOMList lChildList = pDOM.getUL("*");

      //Assert we have at least 2 nested operations to perform.
      if(lChildList.getLength() < 2){
        throw new ExModule("Client visibility rule " + getRuleName() + " - " + lElemName + " operation must have at least 2 operands");
      }

      //Parse the child operations of this operation and add them to the nested list.
      DOM lChild;
      while((lChild = lChildList.popHead()) != null){
        mNestedOperationList.add(parseOperation(lChild, pMod));
      }
    }
  }

  /**
   * Represents an XPath test which will be evaluated at generation time and fixed for the duration of the page's life.
   */
  class FixedXPathTest
  implements Operation {

    /** Boolean XPath to run */
    final String mTestXPath;

    FixedXPathTest(DOM pDOM)
    throws ExModule {
      mTestXPath = pDOM.getAttr("test");
      if(XFUtil.isNull(mTestXPath)) {
        throw new ExModule("Client visibility rule " + getRuleName() + " - XPath test must have non-null 'test' attribute");
      }
    }
  }

}

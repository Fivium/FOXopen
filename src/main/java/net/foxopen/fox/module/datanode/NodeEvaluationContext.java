package net.foxopen.fox.module.datanode;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.StringUtil;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExSecurity;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.PresentationAttribute;
import net.foxopen.fox.module.evaluatedattributeresult.BooleanAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.DOMAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.DOMListAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.EvaluatedAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.FixedStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.PresentationStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.security.SecurityManager;
import net.foxopen.fox.xhtml.NameValuePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * The NodeEvaluationContext class holds information about which attributes are to be used for given data items based on
 * the schema and the NodeEvaluationContext of it's parent
 */
public class NodeEvaluationContext {
  private final List<String> mModeList;
  private final List<String> mViewList;
  private final EvaluatedParseTree mEvaluatedParseTree;
  private final ContextUElem mContextUElem;
  private final DOM mDataItem;
  private final DOM mEvaluateContextRuleItem;
  private final DOM mActionContextItem;
  private final NamespaceAttributeTable mNodeAttributes;
  private final List<String> mNamespacePrecedenceList;
  private final Table<NamespaceFunctionAttribute, NamespaceListType, NamespaceCheckResult> mPreCachedNamespaceFunctions = HashBasedTable.create();
  private final Map<NodeAttribute, EvaluatedAttributeResult> mPreCachedNamespaceFilteredAttributes = new EnumMap<>(NodeAttribute.class);

  private NodeEvaluationContext(List<String> pModeList, List<String> pViewList, EvaluatedParseTree pEvalParseTree, DOM pDataItem, DOM pEvaluateContextRuleItem, DOM pActionContextItem, Table<String, String, PresentationAttribute> pPresentationNodeAttributes, NamespaceAttributeTable pNodeAttributes, List<String> pParentNamespacePrecedenceList) {
    mModeList = Collections.unmodifiableList(pModeList);
    mViewList = Collections.unmodifiableList(pViewList);
    mEvaluatedParseTree = pEvalParseTree;
    mContextUElem = mEvaluatedParseTree.getContextUElem();
    mDataItem = pDataItem;
    mEvaluateContextRuleItem = pEvaluateContextRuleItem;
    mNodeAttributes = pNodeAttributes;

    // Generate an ordered list of namespaces with the most important first and least important last
    if (pParentNamespacePrecedenceList != null) {
      mNamespacePrecedenceList = Collections.unmodifiableList(mergeNamespacePrecedenceList(pPresentationNodeAttributes, pParentNamespacePrecedenceList));
    }
    else {
      mNamespacePrecedenceList = Collections.unmodifiableList(createNamespacePrecedenceList(pPresentationNodeAttributes));
    }

    // Pre-cache the attributes, evaluating where needed
    preCacheAttributes(pDataItem, pPresentationNodeAttributes);

    if (pActionContextItem != null) {
      mActionContextItem = pActionContextItem;
    }
    else {
      mActionContextItem = mEvaluateContextRuleItem;
    }
  }

  /**
   * Go through all the namespace filtered attributes and evaluate and cache any marked as evaluatable. This should be
   * done here and accessed later for when you're inside a loop and contexts will change and not be correct by the time
   * and attribute is actually used in set-out.
   *
   * @param pDataItem
   * @param pPresentationNodeAttributes
   */
  private void preCacheAttributes(DOM pDataItem, Table<String, String, PresentationAttribute> pPresentationNodeAttributes) {
    // Get cut down list of attributes filtered by which namespaces are on and by their precedence
    final Map<String, PresentationAttribute> lNamespaceFilteredAttributes = getNamespacePrecedenceFilteredAttributes(pPresentationNodeAttributes);

    // Pre-cache attributes (potential future optimisation to only pre-evaluate the local-context paths)
    PRECACHE_NAMEPSACE_ATTRIBUTES_LOOP:
    for (Map.Entry<String, PresentationAttribute> lEntry : lNamespaceFilteredAttributes.entrySet()) {
      NodeAttribute lNodeAttribute = NodeAttribute.fromString(lEntry.getKey());
      PresentationAttribute lPresentationAttribute = lEntry.getValue();
      if (lNodeAttribute == null) {
        // If there's no NodeAttribute for the attribute, we won't be able to access it later anyway, so skip
        continue PRECACHE_NAMEPSACE_ATTRIBUTES_LOOP;
      }

      // Eval for type and cache if it's evaluatable, else store fixed value
      if (lNodeAttribute.isEvaluatableXPath() && lPresentationAttribute.isEvaluatableAttribute()) {
        switch (lNodeAttribute.getResultType()) {
          case STRING:
            try {
              // Eval to string (XPathResult has a string object in it as well as an "escaping required" field)
              XPathResult lResult = getContextUElem().extendedConstantOrXPathResult(lPresentationAttribute.getEvalContextRuleDOM(), lPresentationAttribute.getValue());
              mPreCachedNamespaceFilteredAttributes.put(lNodeAttribute, new PresentationStringAttributeResult(lResult));
            }
            catch (ExActionFailed e) {
              throw new ExInternal("Failed to evaluate string attribute '" + lNodeAttribute.getResultType() + "' for attribute '" + lNodeAttribute.getExternalString() + "' on element '" + pDataItem.getName() + "'", e);
            }
            break;
          case BOOLEAN:
            try {
              // Eval to bool
              mPreCachedNamespaceFilteredAttributes.put(lNodeAttribute, new BooleanAttributeResult(getContextUElem().extendedXPathBoolean(lPresentationAttribute.getEvalContextRuleDOM(), lPresentationAttribute.getValue())));
            }
            catch (ExActionFailed e) {
              throw new ExInternal("Failed to evaluate boolean attribute '" + lNodeAttribute.getResultType() + "' for attribute '" + lNodeAttribute.getExternalString() + "' on element '" + pDataItem.getName() + "'", e);
            }
            break;
          case DOM:
            try {
              // Eval to DOM
              mPreCachedNamespaceFilteredAttributes.put(lNodeAttribute, new DOMAttributeResult(getContextUElem().extendedXPath1E(lPresentationAttribute.getEvalContextRuleDOM(), lPresentationAttribute.getValue())));
            }
            catch (ExActionFailed | ExCardinality e) {
              throw new ExInternal("Failed to evaluate DOM attribute '" + lNodeAttribute.getResultType() + "' for attribute '" + lNodeAttribute.getExternalString() + "' on element '" + pDataItem.getName() + "'", e);
            }
            break;
          case DOM_OPTIONAL:
            try {
              // Eval to DOM or possible null wrapper when XPath is valid but no data matched it
              mPreCachedNamespaceFilteredAttributes.put(lNodeAttribute, new DOMAttributeResult(getContextUElem().extendedXPath1E(lPresentationAttribute.getEvalContextRuleDOM(), lPresentationAttribute.getValue())));
            }
            catch (ExActionFailed | ExTooMany e) {
              throw new ExInternal("Failed to evaluate DOM attribute '" + lNodeAttribute.getResultType() + "' for attribute '" + lNodeAttribute.getExternalString() + "' on element '" + pDataItem.getName() + "'", e);
            }
            catch (ExTooFew e) {
              // If no nodes found, but it was defined, store a null DOM value
              mPreCachedNamespaceFilteredAttributes.put(lNodeAttribute, new DOMAttributeResult(null));
            }
            break;
          case DOM_LIST:
            try {
              // Eval to DOMList
              mPreCachedNamespaceFilteredAttributes.put(lNodeAttribute, new DOMListAttributeResult(getContextUElem().extendedXPathUL(lPresentationAttribute.getEvalContextRuleDOM(), lPresentationAttribute.getValue())));
            }
            catch (ExActionFailed e) {
              throw new ExInternal("Failed to evaluate DOM attribute '" + lNodeAttribute.getResultType() + "' for attribute '" + lNodeAttribute.getExternalString() + "' on element '" + pDataItem.getName() + "'", e);
            }
            break;
          default:
            throw new ExInternal("Attempting to pre-evaluate attributes but found unknown type '" + lNodeAttribute.getResultType() + "' for attribute '" + lNodeAttribute.getExternalString() + "' on element '" + pDataItem.getName() + "'");
        }
      }
      else {
        switch (lNodeAttribute.getResultType()) {
          case STRING:
            mPreCachedNamespaceFilteredAttributes.put(lNodeAttribute, new FixedStringAttributeResult(lPresentationAttribute.getValue()));
            break;
          case BOOLEAN:
            // Eval to bool
            mPreCachedNamespaceFilteredAttributes.put(lNodeAttribute, new BooleanAttributeResult(Boolean.valueOf(lPresentationAttribute.getValue())));
            break;
          case DOM:
          case DOM_OPTIONAL:
          case DOM_LIST:
            throw new ExInternal("Found attribute '" + lNodeAttribute.getExternalString() + "' on element '" + pDataItem.getName() + "' marked up as DOM type but marked as non-evaluatable in NodeAttribute class?");
            //break;
          default:
            throw new ExInternal("Attempting to pre-cache attributes but found unknown type '" + lNodeAttribute.getResultType() + "' for attribute '" + lNodeAttribute.getExternalString() + "' on element '" + pDataItem.getName() + "'");
        }
      }
    } // end PRECACHE_NAMEPSACE_ATTRIBUTES_LOOP

    // Pre-cache NamespaceCheckResult objects against all NamespaceFunctionAttributes so we don't evaluate them later
    for (NamespaceFunctionAttribute lNamespaceFunction : NamespaceFunctionAttribute.values()) {
      mPreCachedNamespaceFunctions.put(lNamespaceFunction, NamespaceListType.MODE, checkNamespaceListFunction(NamespaceListType.MODE, lNamespaceFunction));
      mPreCachedNamespaceFunctions.put(lNamespaceFunction, NamespaceListType.VIEW, checkNamespaceListFunction(NamespaceListType.VIEW, lNamespaceFunction));
    }
  }

  /**
   * Construct a NodeEvaluationContext for a given pDataItem given a list of Node Attributes and a parents Namespace
   * Precedence list to use and also a parent NodeEvaluationContext to get evaluated mode/view rule lists from
   *
   * @param pEvalParseTree
   * @param pEvaluatedPresentationNode
   * @param pDataItem
   * @param pEvaluateContextRuleItem
   * @param pActionContextItem
   * @param pNodeAttributes
   * @param pParentNamespacePrecedenceList
   * @param pParentNodeEvaluationContext    @return
   */
  public static NodeEvaluationContext createNodeInfoEvaluationContext(EvaluatedParseTree pEvalParseTree, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode,
                                                                      DOM pDataItem, DOM pEvaluateContextRuleItem, DOM pActionContextItem, NamespaceAttributeTable pNodeAttributes,
                                                                      List<String> pParentNamespacePrecedenceList, NodeEvaluationContext pParentNodeEvaluationContext) {
    return new NodeEvaluationContext( pParentNodeEvaluationContext.getModeList(),
                                      pParentNodeEvaluationContext.getViewList(),
                                      pEvalParseTree,
                                      pDataItem,
                                      pEvaluateContextRuleItem,
                                      pActionContextItem,
                                      pEvaluatedPresentationNode.getNamespaceAttributes(),
                                      pNodeAttributes,
                                      pParentNamespacePrecedenceList);
  }

  /**
   * Construct a NodeEvaluationContext for a given pDataItem given a list of Node Attributes and a parents Namespace
   * Precedence list to use.
   * WARNING: This should only be used at the top level. If you need a NodeEvaluationContext for a child element then
   * use the creator above which takes a parent NodeEvaluationContext
   *
   * @param pEvalParseTree
   * @param pEvaluatedPresentationNode
   * @param pDataItem
   * @param pEvaluateContextRuleItem
   * @param pActionContextItem
   * @param pNodeAttributes
   * @param pParentNamespacePrecedenceList
   * @return
   */
  public static NodeEvaluationContext createNodeInfoEvaluationContext(EvaluatedParseTree pEvalParseTree, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode,
                                                                      DOM pDataItem, DOM pEvaluateContextRuleItem, DOM pActionContextItem,
                                                                      NamespaceAttributeTable pNodeAttributes,
                                                                      List<String> pParentNamespacePrecedenceList) {
    List<NameValuePair> modeXPathList = new ArrayList<>(); // TODO - NP/Anyone - This legacy code is awful. Would be nice to re-factor, probably one for after we have unit tests to check it works though
    List<NameValuePair> viewXPathList = new ArrayList<>();
    List<String> modeList = new ArrayList<>();
    List<String> viewList = new ArrayList<>();
    SecurityManager.getInstance().getPotentialModesAndViewsFromEvaluatedPresentationNode(modeXPathList, viewXPathList, pEvaluatedPresentationNode);
    try {
       SecurityManager.getInstance().getApplicableModeViewLists(pEvalParseTree.getModeRulesOpsDescriptor(),
                                                                pEvalParseTree.getViewRulesOpsDescriptor(),
                                                                modeList,
                                                                viewList,
                                                                modeXPathList,
                                                                viewXPathList,
                                                                pEvalParseTree.getModule(),
                                                                pEvalParseTree.getContextUElem(),
                                                                pDataItem);
    }
    catch (ExSecurity ex) {
      throw ex.toUnexpected();
    }

    return new NodeEvaluationContext(modeList, viewList, pEvalParseTree, pDataItem, pEvaluateContextRuleItem, pActionContextItem, pEvaluatedPresentationNode.getNamespaceAttributes(), pNodeAttributes, pParentNamespacePrecedenceList);
  }

  /**
   * Construct a NodeEvaluationContext for a given pDataItem
   *
   * @param pEvalParseTree
   * @param pEvaluatedPresentationNode
   * @param pDataItem
   * @param pEvaluateContextRuleItem
   * @return
   */
  public static NodeEvaluationContext createPresentationNodeEvaluationContext(EvaluatedParseTree pEvalParseTree, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode,
                                                                              DOM pDataItem, DOM pEvaluateContextRuleItem) {

    List<NameValuePair> modeXPathList = new ArrayList<>(); // TODO - NP/Anyone - This legacy code is awful. Would be nice to re-factor, probably one for after we have unit tests to check it works though
    List<NameValuePair> viewXPathList = new ArrayList<>();
    List<String> modeList = new ArrayList<>();
    List<String> viewList = new ArrayList<>();
    SecurityManager.getInstance().getPotentialModesAndViewsFromEvaluatedPresentationNode(modeXPathList, viewXPathList, pEvaluatedPresentationNode);
    try {
       SecurityManager.getInstance().getApplicableModeViewLists(pEvalParseTree.getModeRulesOpsDescriptor(),
                                                                pEvalParseTree.getViewRulesOpsDescriptor(),
                                                                modeList,
                                                                viewList,
                                                                modeXPathList,
                                                                viewXPathList,
                                                                pEvalParseTree.getModule(),
                                                                pEvalParseTree.getContextUElem(),
                                                                pDataItem);
    }
    catch (ExSecurity ex) {
      throw ex.toUnexpected();
    }

    return new NodeEvaluationContext(modeList, viewList, pEvalParseTree, pDataItem, pEvaluateContextRuleItem, null, pEvaluatedPresentationNode.getNamespaceAttributes(), null, null);
  }

  /**
   * Go through lModeOrViewNamespaceList and for each namespace:
   *  If it's edit, move it to the top of pCurrentNamespacePrecedenceList
   *  If it's explicitly turned off, remove it from pCurrentNamespacePrecedenceList
   *
   * @param pCurrentNamespacePrecedenceList The current namespace precedence List (MODIFIED IN PLACE)
   * @param lModeOrViewNamespaceList List of namespaces to promote or cut down
   * @param pParentNamespaceList Optional NamespacePrecedenceList from the parent EvaluatedNode
   */
  private void namespaceEditPromotionAndExplicitCutdown(List<String> pCurrentNamespacePrecedenceList, List<String> lModeOrViewNamespaceList, List<String> pParentNamespaceList) {
    for (String lNamespace : lModeOrViewNamespaceList) {
      if (pParentNamespaceList == null || pParentNamespaceList.contains(lNamespace)) {
        if (checkNamespaceFunction(lNamespace, "edit")) {
          pCurrentNamespacePrecedenceList.remove(lNamespace);
          pCurrentNamespacePrecedenceList.add(0, lNamespace);
          continue;
        }

        // Remove from list if ro exists and is false or no ro and edit is false
        if (namespaceExplicitlyTurnedOff(lNamespace)) {
          pCurrentNamespacePrecedenceList.remove(lNamespace);
        }
      }
    }
  }

  /**
   * Go through the pCurrentNamespacePrecedenceList and move any occurrences of namespaces from pNamespacePreferenceList
   * to the front
   *
   * @param pCurrentNamespacePrecedenceList
   * @param pNamespacePreferenceList
   */
  private void namespacePreferencePromoter(List<String> pCurrentNamespacePrecedenceList, List<String> pNamespacePreferenceList) {
    for (String lPreferenceNamespace : Lists.reverse(pNamespacePreferenceList)) {
      if (pCurrentNamespacePrecedenceList.contains(lPreferenceNamespace)) {
        // Move to top if in the list and not already at the top
        pCurrentNamespacePrecedenceList.remove(lPreferenceNamespace);
        pCurrentNamespacePrecedenceList.add(0, lPreferenceNamespace);
      }
    }
  }

  /**
   * If the element has an ro attribute for pNamespace and it evaluates to false then that namespace is turned off.
   * If the element has not got an ro attribute but does have an edit attribute that evaluates to false then the namespace is also turned off
   *
   * @param pNamespace Namespace to test
   * @return true if a namespace is not turned off via ro/edit rules on the element
   */
  private boolean namespaceExplicitlyTurnedOff(String pNamespace) {
    if ((getNodeAttribute(pNamespace, "ro") != null && !checkNamespaceFunction(pNamespace, "ro"))
    || (getNodeAttribute(pNamespace, "ro") == null && getNodeAttribute(pNamespace, "edit") != null && !checkNamespaceFunction(pNamespace, "edit"))) {
      return true;
    }
    return false;
  }

  /**
   * Check a namespace to see if a function (edit/ro) is enabled
   *
   * @param pNamespace Namespace to check with
   * @param pFunction Function to test, edit/ro
   * @return false if no pNamespace:pFunction attr exists or it evaluates to false
   */
  private boolean checkNamespaceFunction(String pNamespace, String pFunction) {
    String lFunctionXPathCondition = getNodeAttribute(pNamespace, pFunction);
    if (lFunctionXPathCondition != null) {
      try {
        boolean lFunctionResult = getContextUElem().extendedXPathBoolean(mEvaluateContextRuleItem, lFunctionXPathCondition);
        if (lFunctionResult == true) {
          return lFunctionResult;
        }
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Bad XPath in attribute " + pNamespace + ":" + pFunction, e);
      }
    }
    return false;
  }


  /**
   * Interface that can return two booleans as a result for calls to checkNamespaceListFunction as functions may or may
   * not exist, and if they do exist could be true/false.
   */
  public interface NamespaceCheckResult {
    /**
     * Return true if the namespace was found when asked for
     */
    boolean attributeExists();

    /**
     * Get the value of the attribute if it existed, as a boolean, or return false if it didn't exist
     */
    boolean attributeValue();
  }

  /**
   * Check if a function is enabled on a namespace that's enabled via a mode/view rule
   *
   * @param pNamespaceListType list of namespaces to check against to check by
   * @param pNamespaceFunctionAttribute edit/ro attribute to check
   * @return True if pFunction is on a namespace enabled via mode and evaluates to true
   * @throws ExInternal Exception raised if pFunction attribute XPath is invalid
   */
  private NamespaceCheckResult checkNamespaceListFunction(NamespaceListType pNamespaceListType, NamespaceFunctionAttribute pNamespaceFunctionAttribute)
    throws ExInternal {
    String lFunctionXPathCondition;
    boolean lAttrExists = false;

    List<String> lNamespaceList;
    if (pNamespaceListType == NamespaceListType.MODE) {
      lNamespaceList = mModeList;
    }
    else if (pNamespaceListType == NamespaceListType.VIEW) {
      lNamespaceList = mViewList;
    }
    else {
      throw new ExInternal("Don't know which namespace list to look in when checking namespace function '" + pNamespaceFunctionAttribute.getExternalString() + "' against the '" + pNamespaceListType.toString() + "' list");
    }

    // Test with every namespace in the list - get the attribute value
    for (String lEnabledNamespace : lNamespaceList) {
      lFunctionXPathCondition = getNodeAttribute(lEnabledNamespace, pNamespaceFunctionAttribute.getExternalString());
      if (lFunctionXPathCondition != null) {
        lAttrExists = true;
        try {
          final boolean lFunctionResult = getContextUElem().extendedXPathBoolean(getEvaluateContextRuleItem(), lFunctionXPathCondition);
          if (lFunctionResult) {
            return new NamespaceCheckResult(){
              public boolean attributeValue() { return lFunctionResult; }
              public boolean attributeExists() { return true; }
            };
          }
        }
        catch (ExActionFailed e) {
          throw new ExInternal("Bad XPath in attribute " + lEnabledNamespace + ":" + pNamespaceFunctionAttribute.getExternalString(), e);
        }
      }
    }

    if (lAttrExists) {
      return new NamespaceCheckResult(){
        public boolean attributeValue() { return false; }
        public boolean attributeExists() { return true; }
      };
    }

    return new NamespaceCheckResult(){
      public boolean attributeValue() { return false; }
      public boolean attributeExists() { return false; }
    };
  }

  /**
   *
   * @param pNamespaceListType
   * @param pNamespaceFunctionAttribute
   * @return
   */
  public NamespaceCheckResult checkCachedNamespaceListFunction(NamespaceListType pNamespaceListType, NamespaceFunctionAttribute pNamespaceFunctionAttribute) {
    NamespaceCheckResult lCachedResult = mPreCachedNamespaceFunctions.get(pNamespaceFunctionAttribute, pNamespaceListType);
    if (lCachedResult != null) {
      return lCachedResult;
    }
    else {
      return new NamespaceCheckResult(){
        public boolean attributeValue() { return false; }
        public boolean attributeExists() { return false; }
      };
    }
  }

  /**
   * Check if any namespace on the original node attributes contains pNodeAttribute, regardless of its value
   *
   * @param pNodeAttribute Attribute to look for
   * @return True if the node attributes had an attribute for pNodeAttribute
   */
  public boolean hasNodeAttribute(NodeAttribute pNodeAttribute) {
    if (mNodeAttributes == null) {
      return false;
    }
    else {
      return mNodeAttributes.containsAttribute(pNodeAttribute);
    }
  }

  /**
   * Check if any namespace on the original node attributes contains pNodeAttribute, regardless of its value
   *
   * @param pNamespace Namespace to check for attribute in
   * @param pAttribute Attribute to look for
   * @return True if the node attributes had an attribute for pNodeAttribute
   */
  public boolean hasNodeAttribute(String pNamespace, String pAttribute) {
    if (mNodeAttributes == null) {
      return false;
    }
    else {
      return !XFUtil.isNull(mNodeAttributes.getAttribute(pNamespace, pAttribute));
    }
  }

  /**
   * Get an attribute from the node attributes (schema/action) for a given namespace
   *
   * @param pNamespace Namespace to get attribute from
   * @param pAttributeName Attribute name to get value from
   * @return Value of pNamespace:pAttributeName on the schema item
   */
  public String getNodeAttribute(String pNamespace, String pAttributeName) {
    if (mNodeAttributes == null) {
      return null;
    }
    else {
      return mNodeAttributes.getAttribute(pNamespace, pAttributeName);
    }
  }

  /**
   * Get a string list of namespaces from namespacePreference attributes, looking at the schema item, presentation node
   * and but not the the state/module/app/RM (as the namespace must be module-content to get re-written)
   *
   * @param pPresentationNodeAttributes Table of attributes from the underlying PresentationNode
   * @return namespace or list of namespaces in order if it was a CSV value
   */
  private List<String> getNamespacePreferenceList(Table<String, String, PresentationAttribute> pPresentationNodeAttributes) {
    if (mNodeAttributes != null) {
      String lNodePreference = mNodeAttributes.getAttribute(NodeInfo.FOX_NAMESPACE, "namespacePreference");
      if (lNodePreference != null) {
        return Lists.newArrayList(StringUtil.commaDelimitedListToIterableString(lNodePreference));
      }
    }

    PresentationAttribute lPresentationNodePreference = pPresentationNodeAttributes.get(NodeInfo.FOX_NAMESPACE, "namespacePreference");
    if (lPresentationNodePreference != null) {
      return Lists.newArrayList(StringUtil.commaDelimitedListToIterableString(lPresentationNodePreference.getValue()));
    }

    return Collections.emptyList();
  }

  /**
   * Based on the list of Mode/View namespaces (in precedence order, see class javadoc for info), generate a map of attributes -> values
   *
   * @param pPresentationNodeAttributes Table of attributes from the underlying PresentationNode
   * @return Map of attribute names -> Attribute objects (containing value and context to execute against)
   */
  private Map<String, PresentationAttribute> getNamespacePrecedenceFilteredAttributes(Table<String, String, PresentationAttribute> pPresentationNodeAttributes) {
    ListIterator<String> lReverseNamespaceIterator = mNamespacePrecedenceList.listIterator(mNamespacePrecedenceList.size());
    String lNamespace;
    Map<String, PresentationAttribute> lFilteredAttributeMap = new HashMap<>();
    Map<String, PresentationAttribute> lPresentationNodeAttrs;
    Map<String, String> lTempFoundAttrs;

    // State attributes (including rolled up module/resource master DIAs) go in first, un-namespaced
    if (mEvaluatedParseTree.getState().getStateAttributes() != null) {
      lFilteredAttributeMap.putAll(mEvaluatedParseTree.getStateAttributes());
    }

    // Loop through namespaces from least-import to most adding in their attrs, overriding any that pre-existed
    while (lReverseNamespaceIterator.hasPrevious()) {
      lNamespace = lReverseNamespaceIterator.previous();
      // Add in ones from the command first
      lPresentationNodeAttrs = pPresentationNodeAttributes.row(lNamespace);
      if (lPresentationNodeAttrs != null) {
        lFilteredAttributeMap.putAll(lPresentationNodeAttrs);
      }

      // Finally apply any from the schema item
      if(mNodeAttributes != null) {
        lTempFoundAttrs = mNodeAttributes.getAttributeMapForNamespace(lNamespace);
        if (lTempFoundAttrs != null) {
          lFilteredAttributeMap.putAll(PresentationAttribute.convertAttributeMap(lTempFoundAttrs, mEvaluateContextRuleItem, true));
        }
      }
    }

    return lFilteredAttributeMap;
  }

  /**
   * Merge list of namespaces to filter attributes with.
   * It should be ordered:
   * <ol>
   *  <li>namespacePreference's</li>
   *  <li>mode with edit</li>
   *  <li>mode with ro</li>
   *  <li>view with edit</li>
   *  <li>view with ro</li>
   *  <li>fox</li>
   * </ol>
   *
   * @param pPresentationNodeAttributes Table of attributes from the underlying PresentationNode
   * @return List of namespaces, most important ones at the front
   */
  private List<String> createNamespacePrecedenceList(Table<String, String, PresentationAttribute> pPresentationNodeAttributes) {
    List<String> lList = new ArrayList<>(mModeList.size() + mViewList.size() + 2);

    lList.add(NodeInfo.FOX_NAMESPACE);

    // Add view namespaces
    lList.addAll(0, mViewList);
    // Go through view namespaces and move any with edit="true" to the front
    namespaceEditPromotionAndExplicitCutdown(lList, mViewList, null);

    // Add mode namespaces
    lList.addAll(0, mModeList);
    // Go through mode namespaces and move any with edit="true" to the front
    namespaceEditPromotionAndExplicitCutdown(lList, mModeList, null);

    // Move namespaces in the preference list to the front, if not filtered out already, in reverse order, so the
    // first namespace in the namespacePreference attribute comes out at the front
    namespacePreferencePromoter(lList, getNamespacePreferenceList(pPresentationNodeAttributes));

    return lList;
  }

  /**
   * Merge list of namespaces to filter attributes with.
   * It should be ordered:
   * <ol>
   *  <li>namespacePreference's</li>
   *  <li>mode with edit</li>
   *  <li>mode with ro</li>
   *  <li>view with edit</li>
   *  <li>view with ro</li>
   *  <li>fox</li>
   * </ol>
   *
   * @param pPresentationNodeAttributes Table of attributes from the underlying PresentationNode
   * @param pParentNamespacePrecedenceList Parents namespace precedence list to base the new namespace list on
   * @return List of namespaces, most important ones at the front
   */
  private List<String> mergeNamespacePrecedenceList(Table<String, String, PresentationAttribute> pPresentationNodeAttributes, List<String> pParentNamespacePrecedenceList) {
    if (pParentNamespacePrecedenceList == null) {
      throw new ExInternal("Called mergeNamespacePrecedenceList but didn't give a valid Parent NamespacePrecedenceList");
    }

    // Create a new list, taking in the parents list
    List<String> lList = new ArrayList<>(pParentNamespacePrecedenceList);

    // Go through view namespaces and move any with edit="true" to the front
    namespaceEditPromotionAndExplicitCutdown(lList, mViewList, pParentNamespacePrecedenceList);

    // Go through mode namespaces and move any with edit="true" to the front
    namespaceEditPromotionAndExplicitCutdown(lList, mModeList, pParentNamespacePrecedenceList);

    // Move namespaces in the preference list to the front, if not filtered out already, in reverse order, so the
    // first namespace in the namespacePreference attribute comes out at the front
    namespacePreferencePromoter(lList, getNamespacePreferenceList(pPresentationNodeAttributes));

    return lList;
  }

  /**
   * Get the mode namespace list
   *
   * @return immutable list of namespaces with mode enabled
   */
  public List<String> getModeList() {
    return mModeList;
  }

  /**
   * Get the view namespace list
   *
   * @return immutable list of namespaces with mode enabled
   */
  public List<String> getViewList() {
    return mViewList;
  }

  public ContextUElem getContextUElem() {
    return mContextUElem;
  }

  public DOM getDataItem() {
    return mDataItem;
  }

  public DOM getEvaluateContextRuleItem() {
    return mEvaluateContextRuleItem;
  }

  public DOM getActionContextDOM() {
    return mActionContextItem;
  }

  public EvaluatedParseTree getEvaluatedParseTree() {
    return mEvaluatedParseTree;
  }

  public List<String> getNamespacePrecedenceList() {
    return mNamespacePrecedenceList;
  }

  public boolean isAttributeDefined(NodeAttribute pAttr) {
    return mPreCachedNamespaceFilteredAttributes.containsKey(pAttr);
  }

  /**
   * Perform a checked cast on EvaluatedAttributeResult objects
   *
   * @param pClass Class object to cast pEvaluatedAttributeResult to
   * @param pEvaluatedAttributeResult EvaluatedAttributeResult that needs casting to a implementation type of EvaluatedAttributeResult
   * @param <T> Implementation of EvaluatedAttributeResult
   * @return pEvaluatedAttributeResult cast to T type or null if given a null pEvaluatedAttributeResult
   */
  private static <T extends EvaluatedAttributeResult> T safeCast(Class<T> pClass, EvaluatedAttributeResult pEvaluatedAttributeResult ) {
    if (pEvaluatedAttributeResult == null) {
      return null;
    }
    else if(pClass.isAssignableFrom(pEvaluatedAttributeResult.getClass())) {
      return pClass.cast(pEvaluatedAttributeResult);
    }
    else {
      throw new ClassCastException("Cannot cast " + pEvaluatedAttributeResult.getClass().getName() + " type Evaluated Attribute Result to " + pClass.getName());
    }
  }

  /**
   * Get string value for an attribute, evaluated if the NodeAttribute defines it as evaluatable
   *
   * @param pAttr
   * @return null if no attribute defined
   */
  public StringAttributeResult getStringAttributeOrNull(NodeAttribute pAttr) {
    if (pAttr.getResultType() != NodeAttribute.ResultType.STRING) {
      throw new ExInternal("Asked for a string attribute result from '" + pAttr.getExternalString() + "' which is marked as type '" + pAttr.getResultType() + "'");
    }

    return safeCast(StringAttributeResult.class, mPreCachedNamespaceFilteredAttributes.get(pAttr));
  }

  /**
   * Return a list of the unescaped string attributes
   *
   * @param pNodeAttributes
   * @return A list containing the result of whatever attributes were asked for
   */
  public List<String> getStringAttributes(NodeAttribute... pNodeAttributes) {
    if (pNodeAttributes.length == 0) {
      throw new ExInternal("You have to specify at least one NodeAttribute to get");
    }

    List<String> lAttributeValueList = new ArrayList<>(pNodeAttributes.length);
    StringAttributeResult lAttributeValue;
    for (NodeAttribute lNodeAttribute : pNodeAttributes) {
      lAttributeValue = getStringAttributeOrNull(lNodeAttribute);
      if (lAttributeValue != null && !XFUtil.isNull(lAttributeValue.getString())) {
        lAttributeValueList.add(lAttributeValue.getString());
      }
    }

    return lAttributeValueList;
  }

  /**
   * Gets the boolean value for an attribute, evaluated if the NodeAttribute defines it as evaluatable or as Boolean.valueOf if not.
   * @param pAttr
   * @return Boolean value of attribute, or null if no attribute defined.
   */
  public BooleanAttributeResult getBooleanAttributeOrNull(NodeAttribute pAttr) {
    if (pAttr.getResultType() != NodeAttribute.ResultType.BOOLEAN) {
      throw new ExInternal("Asked for a boolean attribute result from '" + pAttr.getExternalString() + "' which is marked as type '" + pAttr.getResultType() + "'");
    }

    return safeCast(BooleanAttributeResult.class, mPreCachedNamespaceFilteredAttributes.get(pAttr));
  }

  /**
   * Gets the DOM value for an attribute, evaluated if the NodeAttribute defines it as evaluatable
   *
   * @param pAttr
   * @return DOMAttributeResult instance containing a DOM, or null if no attribute defined.
   */
  public DOMAttributeResult getDOMAttributeOrNull(NodeAttribute pAttr) {
    if (pAttr.getResultType() != NodeAttribute.ResultType.DOM && pAttr.getResultType() != NodeAttribute.ResultType.DOM_OPTIONAL) {
      throw new ExInternal("Asked for a DOM attribute result from '" + pAttr.getExternalString() + "' which is marked as type '" + pAttr.getResultType() + "'");
    }

    return safeCast(DOMAttributeResult.class, mPreCachedNamespaceFilteredAttributes.get(pAttr));
  }

  /**
   * Gets the DOMList value for an attribute, evaluated if the NodeAttribute defines it as evaluatable
   *
   * @param pAttr
   * @return DOMListAttributeResult instance containing a DOMList, or null if no attribute defined.
   */
  public DOMListAttributeResult getDOMListAttributeOrNull(NodeAttribute pAttr) {
    if (pAttr.getResultType() != NodeAttribute.ResultType.DOM_LIST) {
      throw new ExInternal("Asked for a DOMList attribute result from '" + pAttr.getExternalString() + "' which is marked as type '" + pAttr.getResultType() + "'");
    }

    return safeCast(DOMListAttributeResult.class, mPreCachedNamespaceFilteredAttributes.get(pAttr));
  }

  /**
   * Enum used when pre-caching namespace function attributes
   */
  public static enum NamespaceListType {
    MODE,
    VIEW
  }
}

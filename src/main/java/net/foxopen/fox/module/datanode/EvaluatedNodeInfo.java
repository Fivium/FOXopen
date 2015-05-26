package net.foxopen.fox.module.datanode;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.evaluatedattributeresult.BooleanAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.FixedStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.layout.CellItem;
import net.foxopen.fox.module.serialiser.layout.IndividualCellItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public abstract class EvaluatedNodeInfo extends EvaluatedNode {
  private final NodeInfo mNodeInfo;

  public EvaluatedNodeInfo(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility);

    if (pNodeInfo == null) {
      throw new ExInternal("Cannot construct a EvaluatedNodeInfo with a null NodeInfo");
    }

    mNodeInfo = pNodeInfo;
  }

  /**
   * Get the string enumeration as marked up in the schema for this ENI. If this ENI represents a multi-selector, the
   * enumeration is read from the child node info.
   * @return
   */
  public List<String> getSchemaEnumeration() {
    if(!isMultiSelect()) {
      return mNodeInfo.getSchemaEnumerationOrNull();
    }
    else {
      return getSelectorNodeInfo().getSchemaEnumerationOrNull();
    }
  }

  private DOM getSelectorModelDOMNode() {
    String lSelectorPath = getStringAttribute(NodeAttribute.SELECTOR);
    DOM lCurrentModelDOM = mNodeInfo.getModelDOMElem();
    try {
      return lCurrentModelDOM.get1E(lSelectorPath);
    }
    catch (ExCardinality e) {
      throw new ExInternal("Selector path " + lSelectorPath + " failed to locate target in schema for node " + this.getIdentityInformation(), e);
    }
  }

  /**
   * Gets the NodeInfo for the "selector" node of this ENI.
   * @return
   */
  public NodeInfo getSelectorNodeInfo() {
    return super.getEvaluatedParseTree().getModule().getNodeInfo(getSelectorModelDOMNode());
  }

  /** Determine cardinality relationship of relative node for selector widgets
   * (number of selections allowed)
   */
  @Override
  public int getSelectorMaxCardinality()
  throws ExInternal {
    // Get selector attribute
    String lSelectorXPath = super.getStringAttribute(NodeAttribute.SELECTOR);

    // When selector not specified return default cardinality 1 for implied xpath "."
    if(lSelectorXPath == null) {
      return 1;
    }

    // Located selector node
    DOM lSelectorNode;
    try {
      lSelectorNode = mNodeInfo.getModelDOMElem().get1E(lSelectorXPath);
    }
    catch(ExCardinality x) {
      throw new ExInternal("Bad map-set selector specified: "+lSelectorXPath, x);
    }

    // Determine cardinality of selector node
    String lCardinalityString = mNodeInfo.getParentModule().getNodeInfo(lSelectorNode).getAttribute("", "maxOccurs");
    try {
      return Integer.parseInt(lCardinalityString);
    }
    catch (NumberFormatException e) {
      throw new ExInternal("getSelectorMaxCardinality(): maxOccurs undefined or bad value: "+lCardinalityString);
    }
  }

  protected String getNodeInfoName() {
    return mNodeInfo.getName();
  }

  @Override
  public String getPromptInternal() {
    // Element Name
    String lPromptValue = getNodeInfoName();
    if (!XFUtil.exists(lPromptValue)) {
      return null;
    }
    else {
      return XFUtil.initCap(lPromptValue);
    }
  }

  @Override
  public StringAttributeResult getSummaryPrompt() {
    StringAttributeResult lSummaryPromptValue = super.getSummaryPromptInternal();
    if (lSummaryPromptValue != null) {
      if ("".equals(lSummaryPromptValue.getString())) {
        return null;
      }
      return lSummaryPromptValue;
    }
    else {
      // No general summaryPrompt, try the Element Name instead
      String lElementName = getNodeInfoName();
      if ("".equals(lElementName)) {
        return null;
      }
      else {
        return new FixedStringAttributeResult(XFUtil.initCap(lElementName));
      }
    }
  }

  /**
   * Gets the NodeInfo which identifies this node (may be different to the stored NodeInfo for phantom data nodes).
   *
   * @return NodeInfo object used to identify this DataNode
   */
  protected NodeInfo getIdentifyingNodeInfo() {
    return mNodeInfo;
  }

  @Override
  public String getIdentityInformation() {
    StringBuilder lReturnValue = new StringBuilder();
    lReturnValue.append(getClass().getSimpleName());
    lReturnValue.append("[");

    lReturnValue.append("Element: '");
    lReturnValue.append(getIdentifyingNodeInfo().getModelDOMElem().absolute());
    lReturnValue.append("'");

    String lPrompt = getPrompt().getString();
    if (lPrompt != null) {
      lReturnValue.append(", ");
      lReturnValue.append("Prompt: '");
      lReturnValue.append(lPrompt);
      lReturnValue.append("'");
    }

    String lActionName = getActionName();
    if (lActionName != null) {
      lReturnValue.append(", ");
      lReturnValue.append("Action: '");
      lReturnValue.append(lActionName);
      lReturnValue.append("'");
    }

    if (getEvaluatedPresentationNode() != null) {
      lReturnValue.append(", ");
      lReturnValue.append("PresentationNode: '");
      lReturnValue.append(getEvaluatedPresentationNode().toString());
      lReturnValue.append("'");
    }

    lReturnValue.append("]");
    return lReturnValue.toString();
  }

  @Override
  public boolean isPhantom() {
    return "phantom".equals(getNodeInfo().getAttribute("","type"));
  }


  /**
   * Test to see if an element is marked up as mandatory
   *
   * @return true if mand/mandatory evaluate to true
   */
  @Override
  public boolean isMandatory() {
    //TODO PN this needs fixing //think this is ok now
    if(isMultiSelect()) {
      if(getSelectorNodeInfo().getMinCardinality() >= 1) {
        return true;
      }
      else {
        return false;
      }
    }

    BooleanAttributeResult lMand = getBooleanAttributeOrNull(NodeAttribute.MAND);
    BooleanAttributeResult lMandatory = getBooleanAttributeOrNull(NodeAttribute.MANDATORY);
    //TODO PN move validation elsewhere
    if (lMand != null && lMandatory != null) {
      throw new ExInternal("mand and mandatory are both specified on the same element: " + getIdentityInformation());
    }
    else if (lMand != null) {
      return lMand.getBoolean();
    }
    else if (lMandatory != null) {
      return lMandatory.getBoolean();
    }
    else {
      return false;
    }
  }

  public List<EvaluatedNodeInfo> getChildren() {
    return Collections.emptyList();
  }

  public Map<NodeInfo, EvaluatedNodeInfo> getChildrenMap() {
    return Collections.emptyMap();
  }

  public void addChild(EvaluatedNodeInfo pEvaluatedNode) {
    throw new ExInternal("Called addChild on an EvaluatedNodeInfo type that doesn't contain children: " + getIdentityInformation());
  }

  public NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  @Override
  public String getName() {
    return getNodeInfoName();
  }

  @Override
  public String getActionName() {
    return getStringAttribute(NodeAttribute.ACTION);
  }

  public abstract String getExternalFoxId();

  @Override
  public Map<String, String> getCellInternalAttributes() {
    return Collections.singletonMap("xfid", getExternalFoxId());
  }

  /**
   * The auto-width of a NodeInfo field attempts to define itself from the maximum string size in an enumeration, the
   * maxLength schema restriction or from the length restriction on a field. If it's not an enumeration or no length
   * restrictions defined then default
   * @return
   */
  @Override
  protected String getAutoFieldWidth() {
    if (getNodeInfo().getSchemaEnumerationOrNull() != null && getNodeInfo().getSchemaEnumerationOrNull().size() > 0) {
      return getNodeInfo().getSchemaEnumerationMaxLength().toString();
    }
    else if (!XFUtil.isNull(getNodeInfo().getMaxDataLength()) && getNodeInfo().getMaxDataLength() != Integer.MAX_VALUE) {
      return getNodeInfo().getMaxDataLength().toString();
    }
    else if (!XFUtil.isNull(getNodeInfo().getLength())) {
      return getNodeInfo().getLength().toString();
    }
    else if (!XFUtil.isNull(getNodeInfo().getMinDataLength()) && getNodeInfo().getMinDataLength() > 0) {
      return getNodeInfo().getMinDataLength().toString();
    }

    return super.getAutoFieldWidth();
  }

  @Override
  public Integer getMaxDataLength() {
    return getNodeInfo().getMaxDataLength();
  }

  /**
   * Generate an IndividualCellItem that can represent this EvaluatedNodeInfo in a layout
   *
   * @param pColumnLimit Max amount of columns possible for this item
   * @param pSerialiser Serialiser to use when generating this item
   * @return IndividualCellItem for this item
   */
  public CellItem getCellItem(int pColumnLimit, OutputSerialiser pSerialiser) {
    return new IndividualCellItem(pColumnLimit, pSerialiser.getWidgetBuilder(getWidgetBuilderType()), this);
  }
}

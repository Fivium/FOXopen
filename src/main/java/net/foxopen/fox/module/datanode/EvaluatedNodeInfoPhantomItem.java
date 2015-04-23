package net.foxopen.fox.module.datanode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;


/**
 * A EvaluatedNodeInfo for phantom-data-xpath schema nodes. The primary NodeInfo of this object will be the NodeInfo of the
 * node targetted by the phantom-data-xpath attribute, if one could be found (otherwise the NodeInfo will be for the phantom
 * definition itself). A secondary reference to the phantom NodeInfo is always held so certain methods can be overloaded
 * to provide the correct properties to superclasses.
 */
public class EvaluatedNodeInfoPhantomItem
extends EvaluatedNodeInfoItem {

  private final NodeInfo mPhantomNodeInfo;

  /** Attributes to take from a resolved target NodeInfo when constructing the phantom data node. */
  private static final Set<NodeAttribute> gPhantomDataPreservedAttributes = EnumSet.of(
    NodeAttribute.WIDGET,
    NodeAttribute.MAPSET,
    NodeAttribute.MAPSET_ATTACH,
    NodeAttribute.SELECTOR,
    NodeAttribute.KEY_TRUE,
    NodeAttribute.KEY_FALSE,
    NodeAttribute.KEY_NULL,
    NodeAttribute.KEY_MISSING,
    NodeAttribute.FORMAT_DATE,
    NodeAttribute.INPUT_MASK
  );
  private static final Collection<String> gPhantomDataPreservedAttributeNames = new HashSet<>();

  static {
    for(NodeAttribute lAttr : gPhantomDataPreservedAttributes) {
      gPhantomDataPreservedAttributeNames.add(lAttr.getExternalString());
    }
  }

  /**
   * Constructs a new EvaluatedNodeInfoItem for a phantom-data-xpath element. If the XPath's target element exists and has
   * a corresponding NodeInfo, the target NodeInfo is passed to ENI parent constructors so schema information such as datatype
   * and enumerations are correctly established. Furthermore, the attributes listed in gPhantomDataPreservedAttributes are taken
   * from the target NodeInfo for use when constructing the new phantom EvaluatedNodeInfo. This means the developer can rely
   * on markup from the original NodeInfo without having to duplicate it on the phantom element definition.
   * @param pParent Parent node containing the phantom data XPath element.
   * @param pEvaluatedPresentationNode Current presentation node.
   * @param pNodeEvaluationContext Parent NodeEvaluationContext.
   * @param pNodeVisibility Node visibility of current node as determined by ro/edit rules.
   * @param pPhantomNodeInfo NodeInfo for the phantom data XPath element.
   * @return A
   */
  protected static EvaluatedNodeInfoItem createPhantomDataXPathENI(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext,
                                                                   NodeVisibility pNodeVisibility, NodeInfo pPhantomNodeInfo) {

    //Attempt to resolve the target node
    DOM lDataItem = pNodeEvaluationContext.getDOMAttributeOrNull(NodeAttribute.PHANTOM_DATA_XPATH).getDOM();
    if(lDataItem != null) {
      //Select the parent element if the developer has targeted a text node
      if(lDataItem.isText()) {
        Track.alert("PhantomDataXPathText", "phantom-data-xpath attribute on " + pPhantomNodeInfo.getAbsolutePath() +" resolved to a text node, bumping up to parent element", TrackFlag.BAD_MARKUP);
        lDataItem = lDataItem.getParentOrSelf();
      }

      //Target node exists; attempt to read its definition to augment into a new phantom EvaluatedNodeInfo

      DOM lEvaluateContextItem = lDataItem.getParentOrNull(); //Should be OK as phantom data currently only supports targeting items, so eval context is always parent

      //Check visibility isn't editable
      if(pNodeVisibility.asInt() >= NodeVisibility.EDIT.asInt()) {
        throw new ExInternal("phantom-data-xpath nodes cannot be editable " + pPhantomNodeInfo.getAbsolutePath());
      }

      NamespaceAttributeTable lMergedAttributeTable = pPhantomNodeInfo.getNamespaceAttributeTable();

      //Attempt to get a NodeInfo for the target node (may not exist if not marked up in schema)
      NodeInfo lTargetNodeInfo = pParent.getEvaluatedParseTree().getModule().getNodeInfo(lDataItem);
      if(lTargetNodeInfo != null) {
        //Filter the acceptable attributes from the target NodeInfo to use when constructing the new NodeInfo
        NamespaceAttributeTable lTargetNodeAttributes = lTargetNodeInfo.getNamespaceAttributeTable().createFilteredAttributeTable(gPhantomDataPreservedAttributeNames);

        lMergedAttributeTable = lMergedAttributeTable.createCopy();
        //Augment target NodeInfo attributes into the existing node attributes for the phantom NodeInfo (phantom attributes get precedence).
        lMergedAttributeTable.mergeTable(lTargetNodeAttributes);
      }

      //Create eval context for the phantom node info (optionally including merged attributes from the target)
      NodeEvaluationContext lNodeEvalCtxt = NodeEvaluationContext.createNodeInfoEvaluationContext(pParent.getEvaluatedParseTree(), pEvaluatedPresentationNode, lDataItem, lEvaluateContextItem, null,
                                                                                                  lMergedAttributeTable, pParent.getNamespacePrecedenceList(), pNodeEvaluationContext);

      return new EvaluatedNodeInfoPhantomItem(pParent, pEvaluatedPresentationNode, lNodeEvalCtxt, pNodeVisibility, pPhantomNodeInfo, lTargetNodeInfo);
    }
    else {
      //No target node exists so we can't work out anything that's not already specified on the original node info
      Track.alert("PhantomDataXPath", "Failed to resolve phantom-data-xpath target for node " + pPhantomNodeInfo.getAbsolutePath());
      return new EvaluatedNodeInfoPhantomItem(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pPhantomNodeInfo, null);
    }
  }

  private EvaluatedNodeInfoPhantomItem(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext,
                                       NodeVisibility pNodeVisibility, NodeInfo pPhantomNodeInfo, NodeInfo pOptionalTargetNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pOptionalTargetNodeInfo != null ? pOptionalTargetNodeInfo : pPhantomNodeInfo);

    mPhantomNodeInfo = pPhantomNodeInfo;
  }

  @Override
  protected String getNodeInfoName() {
    return getIdentifyingNodeInfo().getName();
  }

  @Override
  protected NodeInfo getIdentifyingNodeInfo() {
    //Hack: during construction we won't have a mPhantomNodeInfo yet but we might need one
    return mPhantomNodeInfo != null ? mPhantomNodeInfo : getNodeInfo();
  }

  @Override
  public String getActionName() {
    //Should never have an action name
    return "";
  }

  @Override
  public boolean isPhantomDataNode(){
    return true;
  }
}

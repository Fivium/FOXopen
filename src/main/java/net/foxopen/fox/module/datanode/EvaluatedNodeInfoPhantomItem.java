package net.foxopen.fox.module.datanode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
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

  private static final Set<NodeAttribute> gPhantomDataPreservedAttributes = EnumSet.of(
    NodeAttribute.WIDGET,
    NodeAttribute.MAPSET,
    NodeAttribute.MAPSET_ATTACH,
    NodeAttribute.SELECTOR,
    NodeAttribute.KEY_TRUE,
    NodeAttribute.KEY_FALSE,
    NodeAttribute.KEY_NULL,
    NodeAttribute.KEY_MISSING
  );
  private static final Collection<String> gPhantomDataPreservedAttributeNames = new HashSet<>();

  static {
    for(NodeAttribute lAttr : gPhantomDataPreservedAttributes) {
      gPhantomDataPreservedAttributeNames.add(lAttr.getExternalString());
    }
  }

  public static boolean isPhantomDataNode(EvaluatedNodeInfo pEvaluatedNodeInfo) {
    return pEvaluatedNodeInfo.isPhantom() && pEvaluatedNodeInfo.isAttributeDefined(NodeAttribute.PHANTOM_DATA_XPATH);
  }

  /**
   * Determines if the given EvaluatedNodeInfo is a phantom-data-xpath element, and if so attempts to construct a new EvaluatedNodeInfoItem
   * which reflects the targetted node if one can be found. The attributes listed in gPhantomDataPreservedAttributes are taken from the
   * target NodeInfo for use when constructing the new phantom EvaluatedNodeInfo, and the target NodeInfo is used to determine attributes such as
   * the datatype and schema enumeration for the new EvaluatedNodeInfo.
   * @param pEvaluatedNodeInfo Node to potentially convert to a phantom EvaluatedNodeInfo.
   * @param pEvaluatedPresentationNode Current presentation node.
   * @return pEvaluatedNodeInfo if no changes were made, or a new phantom EvaluatedNodeInfo.
   */
  protected static EvaluatedNodeInfoItem resolvePhantomDataXPath(EvaluatedNodeInfoItem pEvaluatedNodeInfo, GenericAttributesEvaluatedPresentationNode pEvaluatedPresentationNode) {

    if(isPhantomDataNode(pEvaluatedNodeInfo)) {
      //Attempt to resolve the target node
      DOM lDataItem = pEvaluatedNodeInfo.getDOMAttributeOrNull(NodeAttribute.PHANTOM_DATA_XPATH).getDOM();
      if(lDataItem != null) {
        //Select the parent element if the developer has targeted a text node
        if(lDataItem.isText()) {
          Track.alert("PhantomDataXPathText", "phantom-data-xpath attribute on " + pEvaluatedNodeInfo.getIdentityInformation() +
            " resolved to a text node, bumping up to parent element", TrackFlag.BAD_MARKUP);
          lDataItem = lDataItem.getParentOrSelf();
        }

        //Target node is available; attempt to read its definition to augment into a new phantom EvaluatedNodeInfo

        DOM lEvaluateContextItem = lDataItem.getParentOrNull(); //Should be OK as phantom data currently only supports targeting items

        NodeInfo lPhantomNodeInfo = pEvaluatedNodeInfo.getNodeInfo();

        //Check visibility isn't editable
        NodeVisibility lNodeVisibility = pEvaluatedNodeInfo.getVisibility();
        if(lNodeVisibility.asInt() >= NodeVisibility.EDIT.asInt()) {
          throw new ExInternal("phantom-data-xpath nodes cannot be editable " + pEvaluatedNodeInfo.getIdentityInformation());
        }

        NamespaceAttributeTable lMergedAttributeTable = pEvaluatedNodeInfo.getNodeInfo().getNamespaceAttributeTable();

        //Attempt to get a NodeInfo for the target node (may not exist if not marked up in schema)
        NodeInfo lTargetNodeInfo = pEvaluatedNodeInfo.getEvaluatedParseTree().getModule().getNodeInfo(lDataItem);
        if(lTargetNodeInfo != null) {
          //Filter the acceptable attributes from the target NodeInfo to use when constructing the new NodeInfo
          NamespaceAttributeTable lTargetNodeAttributes = lTargetNodeInfo.getNamespaceAttributeTable().createFilteredAttributeTable(gPhantomDataPreservedAttributeNames);

          lMergedAttributeTable = lMergedAttributeTable.createCopy();
          //Augment target NodeInfo attributes into the existing node attributes for the phantom NodeInfo (phantom attributes get precedence).
          lMergedAttributeTable.mergeTable(lTargetNodeAttributes);
        }

        NodeEvaluationContext lNodeEvalCtxt = NodeEvaluationContext.createNodeInfoEvaluationContext(pEvaluatedNodeInfo.getEvaluatedParseTree(), pEvaluatedPresentationNode, lDataItem, lEvaluateContextItem, null, lMergedAttributeTable, pEvaluatedNodeInfo.getParent().getNamespacePrecedenceList(), pEvaluatedNodeInfo.getNodeEvaluationContext());

        //Construct the new phantom EvaluatedNodeInfo with the filtered attribute table
        EvaluatedNodeInfoItem lPhantomEvaluatedNode = new EvaluatedNodeInfoPhantomItem(pEvaluatedNodeInfo.getParent(), pEvaluatedPresentationNode, lNodeEvalCtxt,
                                                                                       lNodeVisibility, lPhantomNodeInfo, lTargetNodeInfo);

        return lPhantomEvaluatedNode;
      }
      else {
        //No target node exists so we can't work out anything that's not already specified on the original node info
        Track.alert("PhantomDataXPath", "Failed to resolve phantom-data-xpath target for node " + pEvaluatedNodeInfo.getIdentityInformation());
        return pEvaluatedNodeInfo;
      }
    }
    else {
      return pEvaluatedNodeInfo;
    }
  }

  private EvaluatedNodeInfoPhantomItem(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext,
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
}

package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.PresentationAttribute;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;

import java.util.Map;


/**
 * Abstract class for all Evaluated Presentation Nodes. Evaluated Presentation Nodes are constructed to run XPaths and
 * cache the results and other objects to be looked over at serialisation time.
 */
public abstract class GenericAttributesEvaluatedPresentationNode <T extends GenericAttributesPresentationNode> extends EvaluatedPresentationNode<GenericAttributesPresentationNode> {
  private final Table<String, String, PresentationAttribute> mNamespaceAttributes = HashBasedTable.create();

  public GenericAttributesEvaluatedPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParentNode, T pOriginalNode, DOM pEvalContext) {
    super(pParentNode, pOriginalNode, pEvalContext);

    // Build up table of namespaces and attributes
    for (Map.Entry<String, String> mAttr : pOriginalNode.getAttributes().entrySet()) {
      int lNSIndex = mAttr.getKey().indexOf(":");
      String lNamespace = "";
      String lAttrName = mAttr.getKey();
      if (lNSIndex > 0) {
        lNamespace = lAttrName.substring(0, lNSIndex);
        lAttrName = lAttrName.substring(lNSIndex + 1);
      }

      mNamespaceAttributes.put(lNamespace, lAttrName, new PresentationAttribute(mAttr.getValue(), pEvalContext, true));
    }
  }

  /**
   * Get a reference the the Attribute Table, where rows = namespaces, columns = attribute names and values = values.
   * Note that this is not a copy, so if any modifications are going to occur outside, copy first!
   *
   * @return Reference to attribute table
   */
  public Table<String, String, PresentationAttribute> getNamespaceAttributes() {
    return mNamespaceAttributes;
  }
}

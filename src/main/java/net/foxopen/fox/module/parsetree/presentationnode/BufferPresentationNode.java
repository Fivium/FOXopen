package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores an fm:buffer
 */
public class BufferPresentationNode extends PresentationNode {
  private String mBufferName;
  private final String mRegionOrder;
  private final String mRegionTitle;
  private final boolean mPreserveComments;
  private final String mPageDefinitionName;

  public BufferPresentationNode(DOM pCurrentNode) {
    mBufferName = pCurrentNode.getAttr("name");
    mRegionTitle = pCurrentNode.getAttr("region-title");
    mRegionOrder = pCurrentNode.getAttr("region-order");
    mPreserveComments = "true".equals(pCurrentNode.getAttr("preserve-comments"));
    mPageDefinitionName = pCurrentNode.getAttr("page-definition-name");

    // Process children
    ParseTree.parseDOMChildren(this, pCurrentNode, mPreserveComments);
  }

  /**
   * When a set-page node is parsed by Mod it has no buffer name, so Mod sets the name later, hence this method
   *
   * @param pName
   */
  public void setName(String pName) {
    mBufferName = pName;
  }

  /**
   * Get the name of this buffer so that external code can map the name to the object and create a lookup map
   *
   * @return The buffer name
   */
  public String getName() {
    return mBufferName;
  }

  /**
   * Buffer regions are used by the skiplink code, the skiplinks can be ordered by the region-order attribute
   *
   * @return Numerical value denoting the order to show this buffer in the skiplinks
   */
  public String getRegionOrder() {
    return mRegionOrder;
  }

  /**
   * Buffer regions are used by the skiplink code, the skiplink title is set by the region-title attribute
   *
   * @return String to show as the link in the skiplinks list
   */
  public String getRegionTitle() {
    return mRegionTitle;
  }

  /**
   * Preserve comments when serialising this buffer
   * TODO - NP - This doesn't currently have any effect, perhaps something to add to a ParseTree context object and pass down?
   *
   * @return default: false
   */
  public boolean isPreserveComments() {
    return mPreserveComments;
  }

  /**
   * Get the name of the page definition used to determine what size a PDF page should be when this buffer is used as
   * the target of a generate-pdf command.
   * @return The page definition name
   */
  public String getPageDefinitionName() {
    return mPageDefinitionName;
  }

  public String toString() {
    return "Buffer ("+mBufferName+")";
  }

  @Override
  public EvaluatedBufferPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedBufferPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext, null);
  }
}

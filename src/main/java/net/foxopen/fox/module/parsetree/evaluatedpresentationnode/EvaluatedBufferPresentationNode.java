package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.clientvisibility.EvaluatedClientVisibilityRule;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.BufferPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.page.PageDefinition;

/**
 *
 */
public class EvaluatedBufferPresentationNode extends EvaluatedPresentationNode<BufferPresentationNode> {
  private final String mBufferName;
  private final String mSkipLinkID;
  private final String mRegionTitle;
  private final String mRegionOrder;
  private final PageDefinition mPageDefinition;

  private final EvaluatedClientVisibilityRule mEvalClientVisibilityRule;


  /**
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvaluatedParseTree
   * @param pEvalContext
   * @param pCVRName Client visibilty rule controlling this buffer's visibility - can be null.
   */
  public EvaluatedBufferPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, BufferPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext, String pCVRName) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    mBufferName = pOriginalPresentationNode.getName();

    // Used for skip link anchors
    mSkipLinkID = "b" + (pEvaluatedParseTree.getFieldSet().getNextFieldSequence());

    String lRegionTitle = pOriginalPresentationNode.getRegionTitle();
    String lRegionOrder = pOriginalPresentationNode.getRegionOrder();
    try {
      if (!XFUtil.isNull(lRegionTitle)) {
        lRegionTitle = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lRegionTitle);
      }

      if (!XFUtil.isNull(lRegionOrder)) {
        lRegionOrder = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lRegionOrder);
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Error while parsing XPaths on fm:hint-out attributes", e);
    }
    mRegionTitle = lRegionTitle;
    mRegionOrder = lRegionOrder;

    // Add the the Buffer Regious list on the Evaluated Parse Tree if a title was specified (for skip links)
    if (!XFUtil.isNull(mRegionTitle)) {
      pEvaluatedParseTree.addBufferRegion(this);
    }

    if(!XFUtil.isNull(pCVRName)) {
      mEvalClientVisibilityRule = pEvaluatedParseTree.evaluateClientVisibilityRule(pCVRName, mSkipLinkID, pEvalContext);
    }
    else {
      mEvalClientVisibilityRule = null;
    }

    String lPageDefinitionName = pOriginalPresentationNode.getPageDefinitionName();
    if (!XFUtil.isNull(lPageDefinitionName)) {
      try {
        String lEvaluatedPageDefinitionName = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lPageDefinitionName);
        mPageDefinition = pEvaluatedParseTree.getModule().getPageDefinitionByName(lEvaluatedPageDefinitionName);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Error while parsing page-definition-name attribute on buffer '" + mBufferName + "'", e);
      }
    }
    else {
      mPageDefinition = null;
    }
  }

  public String getBufferName() {
    return mBufferName;
  }

  /**
   * Get the unique ID to use for skip link anchors
   * @return unique ID to anchor with
   */
  public String getSkipLinkID() {
    return mSkipLinkID;
  }

  /**
   * Get the Region Order, which is used to define the order of buffer links in the skip link list
   * @return region-order from the fm:buffer or Integer.MAX_VALUE if not set
   */
  public Integer getRegionOrder() {
    Integer lRegionOrder = Integer.MAX_VALUE;

    if (!XFUtil.isNull(mRegionOrder)) {
      lRegionOrder = Integer.parseInt(mRegionOrder);
    }
    return lRegionOrder;
  }

  public String getRegionTitle() {
    return mRegionTitle;
  }

  public EvaluatedClientVisibilityRule getEvalClientVisibilityRule() {
    //TODO PN - this shouldn't be exposed
    return mEvalClientVisibilityRule;
  }

  public PageDefinition getPageDefinition() {
    return mPageDefinition;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.BUFFER;
  }
}

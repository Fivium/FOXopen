package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.HeaderFooterPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;

/**
 * Evaluate an fm:header or fm:footer for serialising later
 */
public abstract class EvaluatedHeaderFooterPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final String mStyles;
  private final String mClasses;

  /**
   * Evaluate a HeaderFooterPresentationNode
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvalContext
   */
  protected EvaluatedHeaderFooterPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                  HeaderFooterPresentationNode pOriginalPresentationNode,
                                                  EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    if (!XFUtil.isNull(pOriginalPresentationNode.getStyles())) {
      mStyles = evaluateNodeAttribute(pOriginalPresentationNode.getStyles(), pEvaluatedParseTree);
    }
    else {
      mStyles = "";
    }

    if (!XFUtil.isNull(pOriginalPresentationNode.getClasses())) {
      mClasses = evaluateNodeAttribute(pOriginalPresentationNode.getClasses(), pEvaluatedParseTree);
    }
    else {
      mClasses = "";
    }
  }

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }

  private String evaluateNodeAttribute(String pAttribute, EvaluatedParseTree pEvaluatedParseTree) {
    String lEvaluatedAttribute = "";

    try {
      lEvaluatedAttribute = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), pAttribute);
    }
    catch (ExActionFailed e) {
      throw e.toUnexpected();
    }

    return lEvaluatedAttribute;
  }
}

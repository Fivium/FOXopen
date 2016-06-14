package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.StringUtil;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedContainerPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedWidgetOutPresentationNode;

import java.util.ArrayList;
import java.util.List;


/**
 * Store fm:widget-out command
 */
public class WidgetOutPresentationNode extends GenericAttributesPresentationNode {
  private final String mMatch;
  private final String mShowPrompt;
  private final String mShowWidget;
  private final String mShowError;
  private final String mShowHint;
  private final String mShowDescription;

  public WidgetOutPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);

    mMatch = pCurrentNode.getAttr("match");
    mShowPrompt = XFUtil.nvl(pCurrentNode.getAttr("showPrompt"), "false()");
    mShowWidget = XFUtil.nvl(pCurrentNode.getAttr("showWidget"), "false()");
    mShowError = XFUtil.nvl(pCurrentNode.getAttr("showError"), "false()");
    mShowHint = XFUtil.nvl(pCurrentNode.getAttr("showHint"), "false()");
    mShowDescription = XFUtil.nvl(pCurrentNode.getAttr("showDescription"), "false()");

    if ("false()".equals(mShowPrompt) && "false()".equals(mShowWidget) && "false()".equals(mShowError) && "false()".equals(mShowHint) && "false()".equals(mShowDescription)) {
      throw new ExInternal("fm:widget-out found but no attributes to specify which facets to show: " + mMatch);
    }

    // This type of node has no children to process
  }

  public EvaluatedContainerPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    List<EvaluatedPresentationNode<? extends PresentationNode>> lEvaluatedNodes = EvaluatedWidgetOutPresentationNode.evaluate(pParent, this, pEvaluatedParseTree, pEvalContext);
    return new EvaluatedContainerPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext, lEvaluatedNodes);
//    return new EvaluatedWidgetOutPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }

  public String getMatch() {
    return mMatch;
  }

  public String getShowPromptXPath() {
    return mShowPrompt;
  }

  public String getShowWidgetXPath() {
    return mShowWidget;
  }

  public String getShowErrorXPath() {
    return mShowError;
  }

  public String getShowHintXPath() {
    return mShowHint;
  }

  public String getShowDescriptionXPath() {
    return mShowDescription;
  }

  @Override
  public String toString() {
    List<String> lShowingFacets = new ArrayList<>(4);
    if (!"false()".equals(mShowPrompt)) {
      lShowingFacets.add("PROMPT[" + mShowPrompt + "]");
    }
    if (!"false()".equals(mShowWidget)) {
      lShowingFacets.add("WIDGET[" + mShowWidget + "]");
    }
    if (!"false()".equals(mShowError)) {
      lShowingFacets.add("ERROR[" + mShowError + "]");
    }
    if (!"false()".equals(mShowHint)) {
      lShowingFacets.add("HINT[" + mShowHint + "]");
    }
    if (!"false()".equals(mShowDescription)) {
      lShowingFacets.add("DESCRIPTION[" + mShowDescription + "]");
    }
    return "WidgetOutNode ("+mMatch+": " + StringUtil.collectionToDelimitedString(lShowingFacets, ", ") + ")";
  }
}

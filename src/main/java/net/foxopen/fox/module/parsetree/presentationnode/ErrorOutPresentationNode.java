package net.foxopen.fox.module.parsetree.presentationnode;

import java.util.List;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.PagerSetup;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.PageControlsPosition;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedSetOutPresentationNode;


/**
 * Store fm:set-out command
 */
public class ErrorOutPresentationNode extends GenericAttributesPresentationNode implements SetOutInfoProvider {
  private static final String MATCH_PATH = ":{error}/error-list";

  public ErrorOutPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);

    // Stub in error mode attribute
    addAttribute("error:mode", ".");

    // This type of node has no children to process
  }

  @Override
  public String getMatch() {
    return MATCH_PATH;
  }

  @Override
  public EvaluatedPresentationNode<? extends PresentationNode> evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    //TODO PN - make this method return a list
    List<EvaluatedPresentationNode<? extends PresentationNode>> lEvaluateResult = EvaluatedSetOutPresentationNode.evaluate(pParent, this, pEvaluatedParseTree, pEvalContext);
    if(lEvaluateResult.size() > 0) {
      return lEvaluateResult.get(0);
    }
    else {
      return null;
    }
  }

  @Override
  public String getDatabasePaginationInvokeName() {
    return null;
  }

  @Override
  public PageControlsPosition getPageControlsPosition() {
    //Default is no paging for error-out
    return PageControlsPosition.NONE;
  }

  @Override
  public PagerSetup getDOMPagerSetup() {
    return null;
  }

  @Override
  public String toString() {
    return "ErrorOutNode";
  }
}

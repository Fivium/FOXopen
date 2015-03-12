package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.OutputHint;
import net.foxopen.fox.module.evaluatedattributeresult.PresentationStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.HintOutPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;


public class EvaluatedHintOutPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final OutputHint mHint;

  public EvaluatedHintOutPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, HintOutPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lHintTitle = pOriginalPresentationNode.getHintTitle();
    String lHintText = pOriginalPresentationNode.getHintText();
    String lHintUrl = pOriginalPresentationNode.getHintURL();

    StringAttributeResult lHintTitleResult = null;
    StringAttributeResult lHintTextResult = null;
    try {
      if (!XFUtil.isNull(lHintTitle)) {
        lHintTitleResult = new PresentationStringAttributeResult(pEvaluatedParseTree.getContextUElem().extendedConstantOrXPathResult(getEvalContext(), lHintTitle));
      }

      if (!XFUtil.isNull(lHintText)) {
        lHintTextResult = new PresentationStringAttributeResult(pEvaluatedParseTree.getContextUElem().extendedConstantOrXPathResult(getEvalContext(), lHintText));
      }

      if (!XFUtil.isNull(lHintUrl)) {
        lHintUrl = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(getEvalContext(), lHintUrl);
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Error while parsing XPaths on fm:hint-out attributes", e);
    }

    mHint = new OutputHint("hint"+pEvaluatedParseTree.getFieldSet().getNextFieldSequence(), lHintTitleResult, lHintTextResult, null, lHintUrl);
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.HINT_OUT;
  }

  public OutputHint getHint() {
    return mHint;
  }
}

package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores the name of a buffer to include from an fm:include block
 */
public class IncludePresentationNode extends PresentationNode {
  private final String mBufferName;
  private final String mAttach;
  private final String mClientVisibilityRule;

  public IncludePresentationNode(DOM pCurrentNode) {
    mBufferName = pCurrentNode.getAttr("name");
    mAttach = pCurrentNode.getAttr("attach");
    mClientVisibilityRule = pCurrentNode.getAttr("client-visibility-rule");

    // This type of node has no children to process
  }

  /**
   * Get a BufferPresentationNode based on the buffer name on the fm:include and return an evaluated version of that
   *
   * @inheritDoc
   */
  public EvaluatedBufferPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    try {
      String lBufferName;
      try {
        lBufferName = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(pEvalContext, mBufferName);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate the name attribute on an fm:include: @name = " + mBufferName, e);
      }

      if(XFUtil.isNull(lBufferName)) {
        throw new ExInternal("Buffer name attribute evaluated to null on an fm:include: @name = " + mBufferName);
      }

      BufferPresentationNode lBuffer = pEvaluatedParseTree.getBuffer(lBufferName);

      DOM lAttachContext = pEvalContext;
      if (XFUtil.exists(mAttach)) {
        try {
          lAttachContext = pEvaluatedParseTree.getContextUElem().extendedXPath1E(pEvalContext, mAttach, false);
        }
        catch (ExActionFailed | ExCardinality e) {
          throw new ExInternal("Failed to evaluate the attach attribute on an fm:include: @attach = " + mAttach, e);
        }
      }

      return new EvaluatedBufferPresentationNode(pParent, lBuffer, pEvaluatedParseTree, lAttachContext, mClientVisibilityRule);
    }
    catch (ExModule e) {
      throw new ExInternal("Failed to find buffer to include", e);
    }
  }

  public String toString() {
    return "Include ("+mBufferName+")";
  }
}

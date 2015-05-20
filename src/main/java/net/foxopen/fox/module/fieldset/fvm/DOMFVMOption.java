package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * FVMOption used to apply a complex DOM element to a target node. This is used by complex mapsets.
 */
public class DOMFVMOption
implements FVMOption {

  private final DOM mDOM;

  public DOMFVMOption(DOM pDOM) {
    mDOM = pDOM;
  }

  @Override
  public void applyToNode(ActionRequestContext pRequestContext, DOM pTargetNode) {
    mDOM.copyContentsTo(pTargetNode);
  }

  @Override
  public boolean isEqualTo(DOM pTargetNode) {
    return pTargetNode.contentEqualsOrSuperSetOf(mDOM);
  }
}

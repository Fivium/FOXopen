package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * FVMOption for applying a simple string value to a DOM. This is used by simple mapsets and other FVM types (booleans,
 * schema enums, etc).
 */
public class StringFVMOption
implements FVMOption {

  private final String mString;

  public StringFVMOption(String pString) {
    mString = pString;
  }

  @Override
  public void applyToNode(ActionRequestContext pRequestContext, DOM pTargetNode) {
    pTargetNode.setText(mString);
  }

  @Override
  public boolean isEqualTo(DOM pTargetNode) {
    return pTargetNode.value().equals(mString);
  }
}

package net.foxopen.fox.thread;

import net.foxopen.fox.command.XDoResult;

/**
 * Evaluated data instructing an OutputGenerator to focus on a given tab key. See SwitchTabAction.
 */
public class TabFocusResult
implements XDoResult, FocusResult {

  private final String mTabKeyRef;

  public TabFocusResult(String pNodeRef) {
    mTabKeyRef = pNodeRef;
  }

  public String getTabKeyRef() {
    return mTabKeyRef;
  }

  @Override
  public FocusType getFocusType() {
    return FocusType.TAB;
  }
}
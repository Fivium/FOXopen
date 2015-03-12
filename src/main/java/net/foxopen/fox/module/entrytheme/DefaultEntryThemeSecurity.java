package net.foxopen.fox.module.entrytheme;

import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * Default entry theme security checking which takes no action and allows all users through.
 */
class DefaultEntryThemeSecurity
implements EntryThemeSecurity {

  private static final EntryThemeSecurity INSTANCE = new DefaultEntryThemeSecurity();

  static final EntryThemeSecurity instance() {
    return INSTANCE;
  }

  private DefaultEntryThemeSecurity() { }

  @Override
  public XDoControlFlow evaluate(ActionRequestContext pRequestContext) {
    return XDoControlFlowContinue.instance();
  }

}

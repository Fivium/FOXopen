package net.foxopen.fox.module.fieldset.action;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoIsolatedRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.serialiser.components.html.TabGroupComponentBuilder;
import net.foxopen.fox.module.tabs.TabGroup;
import net.foxopen.fox.module.tabs.TabGroupProvider;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.TabFocusResult;

import java.util.Map;

public class SwitchTabAction
implements InternalAction {

  private final String mTabGroupKey;
  private final String mTabKey;
  private final String mTabDOMContextRef;
  private final String mPreTabAction;
  private final String mPostTabAction;


  public SwitchTabAction(String pTabGroupKey, String pTabKey, String pTabDOMContextRef, String pPreTabAction, String pPostTabAction) {
    mTabGroupKey = pTabGroupKey;
    mTabKey = pTabKey;
    mTabDOMContextRef = pTabDOMContextRef;
    mPreTabAction = pPreTabAction;
    mPostTabAction = pPostTabAction;
  }

  @Override
  public void run(ActionRequestContext pRequestContext, Map<String, String> pParams)
  throws ExUserRequest {

    XDoIsolatedRunner lCommandRunner = pRequestContext.createIsolatedCommandRunner(true);
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    lContextUElem.localise("SwitchTab");
    try {
      if(!XFUtil.isNull(mTabDOMContextRef)) {
        lContextUElem.setUElem(ContextLabel.ACTION, lContextUElem.getElemByRef(mTabDOMContextRef));
      }

      // Add a tab focus result so the HTML serialiser can focus the tab  after churn
      pRequestContext.addXDoResult(new TabFocusResult(TabGroupComponentBuilder.getExternalTabKeyID(mTabGroupKey, mTabKey)));

      XDoControlFlow lPreTabResult = XDoControlFlowContinue.instance();
      if(!XFUtil.isNull(mPreTabAction)) {
        lPreTabResult = lCommandRunner.runCommands(pRequestContext, pRequestContext.resolveActionName(mPreTabAction));
      }

      if(lPreTabResult.canContinue()) {
        TabGroup lTabGroup = pRequestContext.getModuleFacetProvider(TabGroupProvider.class).getTabGroupByKey(mTabGroupKey);
        lTabGroup.selectTab(pRequestContext.getPersistenceContext(), mTabKey);
      }

      if(!XFUtil.isNull(mPostTabAction)) {
        lCommandRunner.runCommands(pRequestContext, pRequestContext.resolveActionName(mPostTabAction));
      }
    }
    finally {
      lContextUElem.delocalise("SwitchTab");
    }

  }
}

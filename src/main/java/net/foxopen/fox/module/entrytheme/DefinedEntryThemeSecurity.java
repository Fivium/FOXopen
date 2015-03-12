package net.foxopen.fox.module.entrytheme;

import com.google.common.base.Splitter;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoIsolatedRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowCST;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.transform.CallStackTransformation;
import net.foxopen.fox.track.Track;

import java.util.Collection;

/**
 * Explcitly declared entry theme security rules. These may contain a set of "before validate" commands which should be
 * used to set up a security scope (for instance). The developer should also declare either a CSV list of required privileges,
 * of which the user must have at least one, or an XPath to run to determine the user's validity (see subclasses).
 */
abstract class DefinedEntryThemeSecurity
implements EntryThemeSecurity {

  /** Commands to run before doing the security check */
  private final XDoCommandList mBeforeValidateCommands;

  static DefinedEntryThemeSecurity create(DOM pDefinitionDOM, Mod pModule)
  throws ExDoSyntax, ExModule {

    //Optional before-validation command block
    DOM lBeforeValidation = pDefinitionDOM.get1EOrNull("fm:before-validation");
    XDoCommandList lBeforeValidateCommands;
    if(lBeforeValidation != null) {
      lBeforeValidateCommands = new XDoCommandList(pModule, lBeforeValidation);
    }
    else {
      lBeforeValidateCommands = XDoCommandList.emptyCommandList();
    }

    //The test to run - either a priv list or an XPath (never both)
    String lAllowedPrivsCSV = pDefinitionDOM.get1SNoEx("fm:privilege-list");
    DOM lPrivTestDOM = pDefinitionDOM.get1EOrNull("fm:privilege-test");
    String lPrivTestXPath = lPrivTestDOM != null ? lPrivTestDOM.getAttr("xpath") : "";

    if(XFUtil.isNull(lAllowedPrivsCSV) ^ XFUtil.isNull(lPrivTestXPath)) {

      if(!XFUtil.isNull(lAllowedPrivsCSV)) {
        Collection<String> lAllowedPrivileges = Splitter.on(",").trimResults().splitToList(lAllowedPrivsCSV);

        //Check the developer has defined at least 1 priv
        if(lAllowedPrivileges.size() == 0) {
          throw new ExModule("privilege-list must contain at least one privilege");
        }

        return new PrivilegeCheckEntryThemeSecurity(lBeforeValidateCommands, lAllowedPrivileges);
      }
      else {
        return new XPathEntryThemeSecurity(lBeforeValidateCommands, lPrivTestXPath);
      }
    }
    else {
      throw new ExModule("privilege-list and privilege-test are mutually exclusive and exactly one must be specified");
    }
  }

  protected DefinedEntryThemeSecurity(XDoCommandList pBeforeValidateCommands) {
    mBeforeValidateCommands = pBeforeValidateCommands;
  }

  /**
   * Implementors should define security check logic in this method.
   * @param pRequestContext
   * @return True if the entry attempt is valid, false if it should be rejected.
   */
  protected abstract boolean runCheck(ActionRequestContext pRequestContext);

  @Override
  public XDoControlFlow evaluate(ActionRequestContext pRequestContext) {

    Track.pushInfo("EntryThemeSecurityValidation");
    try {
      XDoIsolatedRunner lCommandRunner = pRequestContext.createIsolatedCommandRunner(true);
      lCommandRunner.runCommands(pRequestContext, mBeforeValidateCommands);

      boolean lValid = runCheck(pRequestContext);

      if(!lValid) {
        //Check failed - construct a CST to boot the user out to the "failed security check" module
        EntryTheme lAccessDeniedTheme = pRequestContext.getModuleApp().getSecurityCheckEntryTheme();

        Track.info("ValidationFailed", "Redirect user to theme " + lAccessDeniedTheme.getName() + " in " + lAccessDeniedTheme.getModule().getName());

        ModuleCall.Builder lBuilder = new ModuleCall.Builder(lAccessDeniedTheme);

        CallStackTransformation lCallStackTransformation = CallStackTransformation.createCallStackTransformation(CallStackTransformation.Type.MODAL_REPLACE_THIS_CANCEL_CALLBACKS, lBuilder);

        return new XDoControlFlowCST(lCallStackTransformation);
      }
      else {
        Track.info("ValidationPassed");
        return XDoControlFlowContinue.instance();
      }
    }
    finally {
      Track.pop("EntryThemeSecurityValidation");
    }
  }
}

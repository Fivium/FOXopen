package net.foxopen.fox.command.builtin;


import com.google.common.base.Splitter;
import net.foxopen.fox.App;
import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowCST;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExRoot;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.callback.ActionCallback;
import net.foxopen.fox.thread.stack.callback.ReturnTargetsCallback;
import net.foxopen.fox.thread.stack.transform.CallStackTransformation;
import net.foxopen.fox.thread.stack.transform.ModelessWindowOptions;

import java.util.Collection;
import java.util.Collections;


/**
 * Implementation of a FOX inter-module call command.
 */
public class ModuleCallCommand
extends BuiltInCommand
{
  /** A running sequence for window handles. */
  private static int windowHandleSequence = 1;

  private final CallStackTransformation.Type mCallStackTransformationType;

  /**
  * Constructs a Module Call command from the XML element specified.
  *
  * @param module the module where the command is used
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private ModuleCallCommand(DOM pCommandElement)
  throws ExDoSyntax {
    super(pCommandElement);

    String lOperationType = pCommandElement.getAttrOrNull("type");
    mCallStackTransformationType = CallStackTransformation.Type.forName(lOperationType);

    // Validate operation
    if (mCallStackTransformationType == null) {
      throw new ExDoSyntax("fm:call-module: Call module type not known: "+lOperationType);
    }
  }

  /**
  * Determines the application of the module call. This is obtained
  * through the <i>app</i> parameter or the current application, if
  * not specified.
  *
  * @return the application to call
  */
  private App getApplicationToCall(ActionRequestContext pRequestContext) {
    try {
      ContextUElem lContextUElem = pRequestContext.getContextUElem();
      String lAppName = lContextUElem.extendedStringOrXPathString(lContextUElem.getUElem(ContextLabel.ATTACH), getAttribute("app", pRequestContext.getCurrentModule().getApp().getMnemonicName()));
      return FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(lAppName);
    }
    catch (ExServiceUnavailable | ExActionFailed | ExApp ex) {
      throw new ExInternal("call-module action failed due to app unavailable: ", ex);
    }
  }

  /**
   * Note: default module is current module if not specified.
   * @param pRequestContext
   * @param pApp
   * @return
   */
  private Mod getModuleToCall(ActionRequestContext pRequestContext, App pApp) {
    ContextUElem context = pRequestContext.getContextUElem();
    try {
      String lModName = context.extendedStringOrXPathString(context.getUElem(ContextLabel.ATTACH),getAttribute("module", pRequestContext.getCurrentModule().getName()));

      return pApp.getMod(lModName);
    }
    catch (ExModule | ExServiceUnavailable  ex ) {
      throw new ExInternal("call-module action failed due to application module failure: ", ex);
    }
    catch (ExRoot ex) {
      throw new ExInternal("Unexpected error during call-module action processing: ", ex);
    }
  }

  /**
  * Determines the entry theme of the module to call.
  *
  * @param pModule the module that defines the theme
  * @return the theme to call
  */
  private EntryTheme getEntryThemeToCall(Mod pModule, ContextUElem context)
  throws ExActionFailed {
    String lThemeName = context.extendedStringOrXPathString(context.getUElem(ContextLabel.ATTACH),getAttribute("theme")) ;

    try {
      if (lThemeName == null)
        throw new ExActionFailed("MODULE", "No theme name specified for call-module action!");

      return pModule.getEntryTheme(lThemeName);
    }
    catch (ExUserRequest ex) {
      throw new ExActionFailed("MODULE", "call-module action failed - unknown theme, \""+lThemeName+"\", specified: ", ex);
    }
  }

  /**
  * Generates a unique, ascending number for the purposes of generating a unique name
  * for a popup associated with a module call.
  * @return A unique number.
  */
  private static int getUniqueWindowId() {
    return ++windowHandleSequence;
  }

  private static String getUniqueWindowName(String pAppMnem, String pModName, String pThemeName) {
   return (pAppMnem +"_" + pModName + "_" + pThemeName + "_" + getUniqueWindowId()).replace('-','_'); // null allowed. replace - with _ to avoid js bug in IE
  }

  public boolean isCallTransition() {
    return true;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    App lApp = getApplicationToCall(pRequestContext);
    Mod lMod = getModuleToCall(pRequestContext, lApp);
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    EntryTheme lTheme;
    try {
      lTheme = getEntryThemeToCall(lMod, lContextUElem);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Cannot get theme", e);
    }

    ModuleCall.Builder lBuilder = new ModuleCall.Builder(lTheme);

    DOM lParamsContainerDOM = DOM.createDocument("params");

    //Evaluate params expression
    if (isAttributeSupplied("params")) {
      String paramsXPath = getAttribute("params");
      try {
        lContextUElem.extendedXPathUL(paramsXPath, ContextUElem.ATTACH).copyContentsTo(lParamsContainerDOM);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate params XPath", e);
      }
    }

    //Parse literalParams attribute
    if (isAttributeSupplied("literalParams") ) {
      for(String lParamNameValue : Splitter.on(",").trimResults().split(getAttribute("literalParams"))) {
        String[] lNameValue = lParamNameValue.split("=");
        if (lNameValue.length == 2) {
          String lParamName = lNameValue[0];
          String lParamValue = lNameValue[1];
          lParamsContainerDOM.addElem(lParamName, lParamValue);
        }
      }
    }

    lBuilder.setParamsDOM(lParamsContainerDOM);

    if(!XFUtil.isNull(getAttribute("callback-action"))){
      lBuilder.addCallbackHandler(new ActionCallback(getAttribute("callback-action")));
    }

    if(!XFUtil.isNull(getAttribute("returnTargets"))){
      lBuilder.addCallbackHandler(new ReturnTargetsCallback(getAttribute("returnTargets")));
    }

    if(mCallStackTransformationType.isModeless()){
      String lWindowName = getAttribute("windowName", getUniqueWindowName(lApp.getMnemonicName(), lMod.getName(), lTheme.getName()));
      String lWindowProps = getAttribute("windowProperties");
      lBuilder.setModelessWindowOptions(new ModelessWindowOptions(lWindowName, lWindowProps));
    }

    CallStackTransformation lCST = CallStackTransformation.createCallStackTransformation(mCallStackTransformationType, lBuilder);

    //Tell consumers that this action caused a callstack transformation
    return new XDoControlFlowCST(lCST);
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ModuleCallCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("call-module");
    }
  }
}

package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.auth.AuthUtil;
import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.auth.loginbehaviours.ResumeLoginBehaviour;
import net.foxopen.fox.auth.loginbehaviours.StandardResumeLoginBehaviour;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.*;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

public class ResumeLoginCommand extends BuiltInCommand{

  /** An XPATH expression for the hash code of the user log in attempt. */
  private final String mHashCodeExpr;

  /** An XPATH expression for the status code to be assigned to */
  private final String mStatusCodeExpr;

  /** An XPATH expression for the status message to be assigned to */
  private final String mStatusMsgExpr;

  /**
   * Constructs and initialises a command from the XML element
   * specified.
   *
   * @param pModule Module the command is in
   * @param pCommandElement
   */
  public ResumeLoginCommand(Mod pModule, DOM pCommandElement) {
    super(pCommandElement);
    mHashCodeExpr = pCommandElement.getAttrOrNull("hash-code");
    mStatusCodeExpr = pCommandElement.getAttrOrNull("status-code");
    mStatusMsgExpr = pCommandElement.getAttrOrNull("status-message");

    if ( mHashCodeExpr == null ) {
      throw new ExInternal( "The "+getName()+" command has been used incorrectly in application \""+
      pModule.getApp().getAppMnem()+"\" and module \""+
      pModule.getName()+": the command is missing the " + mHashCodeExpr + " attribute that specifies an XPath expression for the hash code to confirm the login attempt.");
    }

    if ((mStatusCodeExpr == null ^ mStatusMsgExpr == null)) {
      throw new ExInternal(  "The "+getName()+" command has been used incorrectly in application \""+
      pModule.getApp().getAppMnem()+"\" and module \""+
      pModule.getName()+": the command is missing the " + mStatusMsgExpr  + " attribute or the  " + mStatusCodeExpr + " attribute. Both " + mStatusMsgExpr + " and " + mStatusCodeExpr + " must be declared.");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    try {
      // Set up status DOM elements
      DOM lStatusCodeDOM = null;
      DOM lStatusMsgDOM = null;
      if (mStatusCodeExpr != null && mStatusMsgExpr != null) {
        try {
          lStatusCodeDOM = lContextUElem.getCreateXPath1E(mStatusCodeExpr, ContextUElem.ATTACH);
          lStatusMsgDOM = lContextUElem.getCreateXPath1E(mStatusMsgExpr, ContextUElem.ATTACH);
        }
        catch (ExCardinality e) {
          throw new ExInternal("Failed to evaluate XPath when running ResumeLoginCommand", e);
        }
      }

      String lHashCodeExprResult = lContextUElem.extendedXPathString(lContextUElem.attachDOM(), mHashCodeExpr);

      String lClientInfo = AuthUtil.getClientInfoNVP(pRequestContext.getFoxRequest());

      ResumeLoginBehaviour lResumeLoginBehaviour = new StandardResumeLoginBehaviour(lHashCodeExprResult, lClientInfo);

      AuthenticationResult lAuthenticationResult = pRequestContext.getAuthenticationContext().resumeLogin(pRequestContext, lResumeLoginBehaviour);

      String lStatusCode = lAuthenticationResult.getCode().toString();
      String lStatusMsg = lAuthenticationResult.getMessage();

      if (lStatusCodeDOM != null && lStatusMsgDOM != null) {
        lStatusCodeDOM.setText(lStatusCode);
        lStatusMsgDOM.setText(lStatusMsg);
      }
      else if (lAuthenticationResult.getCode() != AuthenticationResult.Code.VALID) {
        // otherwise, throw if not VALID
        Throwable lExMod = new ExModule("Exception raised because fm:resume-login command did not specify attributes " +
        "status-code or status-message; if present fm:user-login will write out status code and message to those " +
        "XPath locations instead of raising an exception");
        throw new ExActionFailed(lStatusCode, lStatusMsg, lExMod);
      }

    }
    catch (ExActionFailed e) {
      throw new ExInternal("Login failed when running ResumeLoginCommand", e);
    }

    return XDoControlFlowContinue.instance();
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ResumeLoginCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("resume-login");
    }
  }
}

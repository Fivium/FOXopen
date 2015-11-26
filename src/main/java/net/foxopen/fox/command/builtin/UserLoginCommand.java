package net.foxopen.fox.command.builtin;


import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthUtil;
import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.auth.loginbehaviours.LDAPLoginBehaviour;
import net.foxopen.fox.auth.loginbehaviours.LoginBehaviour;
import net.foxopen.fox.auth.loginbehaviours.StandardLoginBehaviour;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.UserLoginResult;

import java.util.Collection;
import java.util.Collections;


public class UserLoginCommand
extends BuiltInCommand {
  /**
   * Attribute containing XPath to the element containing the user login id.
   */
  public static final String LOGIN_ID_ATTRIBUTE = "wua-login-id";

  /**
   * Attribute containing XPath to the element containing the password.
   */
  public static final String PASSWORD_ATTRIBUTE = "password";

  /**
   * Attribute containing XPath to the element where the status code should be written (element
   * need not exist, will be created if necessary).
   */
  public static final String STATUS_CODE_ATTRIBUTE = "status-code";

  /**
   * Attribute containing XPath to the element where the status mesage should be written (element
   * need not exist, will be created if necessary).
   */
  public static final String STATUS_MESSAGE_ATTRIBUTE = "status-message";

  /**
   * Attribute containing the authentication method to use (i.e to enable LDAP auth).
   */
  public static final String AUTH_METHOD_ATTRIBUTE = "authentication-method";

  /**
   * Attribute containing the authentication domain to use with the specified method.
   */
  public static final String AUTH_DOMAIN_ATTRIBUTE = "authentication-domain";

  /** An XPATH expression for the Web User Account ID of the user to log in. */
  private final String mWuaLoginIdExpr;

  /** An XPATH expression for the password of the account to log in. */
  private final String mPasswordExpr;

  /** An XPATH expression for the status code to be assigned to */
  private final String mStatusCodeExpr;

  /** An XPATH expression for the status message to be assigned to */
  private final String mStatusMsgExpr;

  /** Authentication method */
  private final String mAuthMethodExpr;
  private final String mAuthDomainExpr;

  /**
   * Constructs the command from the XML element specified.
   *
   * @param pModule Module the command is in
   * @param pCommandElement the element from which the command will be constructed.
   */
  private UserLoginCommand(Mod pModule, DOM pCommandElement) {
    super(pCommandElement);
    mWuaLoginIdExpr = pCommandElement.getAttrOrNull(LOGIN_ID_ATTRIBUTE);
    mPasswordExpr = pCommandElement.getAttrOrNull(PASSWORD_ATTRIBUTE);
    mStatusCodeExpr = pCommandElement.getAttrOrNull(STATUS_CODE_ATTRIBUTE);
    mStatusMsgExpr = pCommandElement.getAttrOrNull(STATUS_MESSAGE_ATTRIBUTE);

    mAuthMethodExpr = pCommandElement.getAttrOrNull(AUTH_METHOD_ATTRIBUTE);
    mAuthDomainExpr = pCommandElement.getAttrOrNull(AUTH_DOMAIN_ATTRIBUTE);

    if (mWuaLoginIdExpr == null) {
      throw new ExInternal("The "+getName()+" command has been used incorrectly in application \""+
                           pModule.getApp().getAppMnem()+"\" and module \""+
                           pModule.getName()+": the command is missing the " + LOGIN_ID_ATTRIBUTE + " attribute that specifies an XPath expression for the Web User Account (WUA) Login Id of the user to login.");
    }

    if (mPasswordExpr == null) {
      throw new ExInternal("The "+getName()+" command has been used incorrectly in application \""+
                           pModule.getApp().getAppMnem()+"\" and module \""+
                           pModule.getName()+": the command is missing the " + PASSWORD_ATTRIBUTE + " attribute specifying an XPath expression for the password of the Web User Account (WUA) of the user to login.");
    }

    if (mStatusCodeExpr != null && mStatusMsgExpr == null) {
      throw new ExInternal("The "+getName()+" command has been used incorrectly in application \""+
                           pModule.getApp().getAppMnem()+"\" and module \""+
                           pModule.getName()+": the command is missing the " + STATUS_MESSAGE_ATTRIBUTE  + " attribute, but " + STATUS_CODE_ATTRIBUTE + " is specified. You must declare " + STATUS_MESSAGE_ATTRIBUTE + " or remove " + STATUS_CODE_ATTRIBUTE);
    }

    if (mStatusMsgExpr != null && mStatusCodeExpr == null) {
      throw new ExInternal("The "+getName()+" command has been used incorrectly in application \""+
                           pModule.getApp().getAppMnem()+"\" and module \""+
                           pModule.getName()+": the command is missing the " + STATUS_CODE_ATTRIBUTE + " attribute, but " + STATUS_MESSAGE_ATTRIBUTE + " is specified. You must declare " + STATUS_CODE_ATTRIBUTE + " or remove " + STATUS_MESSAGE_ATTRIBUTE);
    }
  }

  public boolean isCallTransition() {
    return false;
  }

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
          throw new ExInternal("Failed to evaluate XPath when running UserLoginCommand", e);
        }
      }

      // Evaluate auth method/domain
      String lAuthMethod = null;
      String lAuthDomain = null;
      if (!XFUtil.isNull(mAuthMethodExpr)) {
        lAuthMethod = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mAuthMethodExpr);
      }
      if (!XFUtil.isNull(mAuthDomainExpr)) {
        lAuthDomain = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mAuthDomainExpr);
      }

      String lUsername = lContextUElem.extendedXPathResult(lContextUElem.attachDOM(), mWuaLoginIdExpr).asString();
      String lPassword = lContextUElem.extendedXPathResult(lContextUElem.attachDOM(), mPasswordExpr).asString();

      String lClientInfo = AuthUtil.getClientInfoNVP(pRequestContext.getFoxRequest());

      LoginBehaviour lLoginBehaviour;
      if (LDAPLoginBehaviour.getLoginBehaviourName().equals(lAuthMethod)) {
        lLoginBehaviour = new LDAPLoginBehaviour(lUsername, lPassword, lClientInfo, lAuthDomain);
      }
      else {
        lLoginBehaviour = new StandardLoginBehaviour(lUsername, lPassword, lClientInfo);
      }

      AuthenticationResult lAuthenticationResult = pRequestContext.getAuthenticationContext().login(pRequestContext, lLoginBehaviour);

      String lStatusCode = lAuthenticationResult.getCode().toString();
      String lStatusMsg = lAuthenticationResult.getMessage();

      pRequestContext.addXDoResult(new UserLoginResult(lAuthenticationResult));

      if (lStatusCodeDOM != null && lStatusMsgDOM != null) {
        lStatusCodeDOM.setText(lStatusCode);
        lStatusMsgDOM.setText(lStatusMsg);
      }
      else if (lAuthenticationResult.getCode() != AuthenticationResult.Code.VALID) {
        // otherwise, throw if not VALID
        Throwable lExMod = new ExModule("Exception raised because fm:user-login command did not specify attributes " +
          "status-code or status-message; if present fm:user-login will write out status code and message to those " +
          "XPath locations instead of raising an exception");
        throw new ExActionFailed(lStatusCode, lStatusMsg, lExMod);
      }

    }
    catch (ExActionFailed e) {
      throw new ExInternal("Login failed when running UserLoginCommand", e);
    }

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new UserLoginCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("user-login");
    }
  }
}

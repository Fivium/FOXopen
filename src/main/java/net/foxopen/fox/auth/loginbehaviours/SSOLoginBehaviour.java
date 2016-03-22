package net.foxopen.fox.auth.loginbehaviours;

import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;

/**
 * SSO style login that doesn't require a password and instead relies on trusted information being passed in here to the
 * Username and AuthInfo parameters. Trust should be established outside this class.
 */
public class SSOLoginBehaviour implements LoginBehaviour {
  private static final String SESSION_CREATE_SSO_FILENAME = "SessionCreateSSO.sql";
  private final String mUsername;
  private final String mClientInfo;
  private final String mAuthDomain;
  private final String mAuthScheme;
  private final DOM mAuthInfoDOM;

  public SSOLoginBehaviour(String pUsername, String pClientInfo, String pAuthDomain, String pAuthScheme, DOM pAuthInfoDOM) {
    mUsername = pUsername;
    mClientInfo = pClientInfo;
    mAuthDomain = pAuthDomain;
    mAuthScheme = pAuthScheme;
    mAuthInfoDOM = pAuthInfoDOM;
  }

  @Override
  public AuthenticationResult login(RequestContext pRequestContext) {
    // Call SSO login on database
    Track.pushInfo("authenticateSSO");
    try {
      Track.info("login-id", mUsername);
      Track.info("auth-scheme", mAuthScheme);
      Track.info("auth-domain", mAuthDomain);
      Track.info("auth-info", mAuthInfoDOM.outputNodeToString(true));

      UConBindMap lBindMap = new UConBindMap()
        .defineBind(":session_id_out", UCon.bindOutString())
        .defineBind(":client_info", mClientInfo)
        .defineBind(":app_display_name", "FOX-SYSTEM")
        .defineBind(":login_id", mUsername)
        .defineBind(":pre_auth_xml", mAuthInfoDOM)
        .defineBind(":auth_scheme", mAuthScheme)
        .defineBind(":auth_domain", mAuthDomain)
        .defineBind(":status_code_out", UCon.bindOutString())
        .defineBind(":status_message_out", UCon.bindOutString());

      UCon lUCon = pRequestContext.getContextUCon().getUCon("User Login SSO");
      UConStatementResult lAPIResult;
      try {
        lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(SESSION_CREATE_SSO_FILENAME, getClass()), lBindMap);
      }
      catch (ExDB e) {
        throw new ExInternal("Failed to authenticate user " + mUsername, e);
      }
      finally {
        pRequestContext.getContextUCon().returnUCon(lUCon, "User Login SSO");
      }

      AuthenticationResult.Code lCode = AuthenticationResult.Code.fromString(lAPIResult.getString(":status_code_out"));
      return new AuthenticationResult(lCode, lAPIResult.getString(":status_message_out"), lAPIResult.getString(":session_id_out"));
    }
    catch (Throwable th) {
      throw new ExInternal("Failed to authenticate SSO user: " + mUsername, th);
    }
    finally {
      Track.pop("authenticateSSO");
    }
  }
}

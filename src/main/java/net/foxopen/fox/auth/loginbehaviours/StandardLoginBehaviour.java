package net.foxopen.fox.auth.loginbehaviours;

import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.RequestContext;

public class StandardLoginBehaviour implements LoginBehaviour {
  private static final String SESSION_CREATE_FILENAME = "SessionCreate.sql";

  private final String mUsername;
  private final String mPassword;
  private final String mClientInfo;

  public StandardLoginBehaviour(String pUsername, String pPassword, String pClientInfo) {
    mUsername = pUsername;
    mPassword = pPassword;
    mClientInfo = pClientInfo;
  }

  @Override
  public AuthenticationResult login(RequestContext pRequestContext) {
    UConBindMap lBindMap = new UConBindMap()
      .defineBind(":session_id_out", UCon.bindOutString())
      .defineBind(":client_info", mClientInfo)
      .defineBind(":app_display_name", "FOX-SYSTEM")
      .defineBind(":login_id", mUsername)
      .defineBind(":password", mPassword)
      .defineBind(":service_list", FoxGlobals.getInstance().getFoxBootConfig().getFoxServiceList())
      .defineBind(":status_code_out", UCon.bindOutString())
      .defineBind(":status_message_out", UCon.bindOutString());

    UCon lUCon = pRequestContext.getContextUCon().getUCon("User Login");
    UConStatementResult lAPIResult;
    try {
      lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(SESSION_CREATE_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to authenticate user " + mUsername, e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "User Login");
    }

    AuthenticationResult.Code lCode = AuthenticationResult.Code.fromString(lAPIResult.getString(":status_code_out"));
    AuthenticationResult lAuthenticationResult = new AuthenticationResult(lCode, lAPIResult.getString(":status_message_out"), lAPIResult.getString(":session_id_out"));
    return lAuthenticationResult;
  }
}

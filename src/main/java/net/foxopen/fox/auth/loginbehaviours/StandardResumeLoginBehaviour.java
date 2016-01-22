package net.foxopen.fox.auth.loginbehaviours;

import net.foxopen.fox.auth.AuthenticationResult;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.RequestContext;

public class StandardResumeLoginBehaviour
implements ResumeLoginBehaviour {

  private static final String SESSION_CREATE_FILENAME = "SessionCreateAfterResume.sql";

  private final String mValidationHashCode;
  private final String mClientInfo;

  public StandardResumeLoginBehaviour(String pValidationHashCode, String pClientInfo) {
    mValidationHashCode = pValidationHashCode;
    mClientInfo = pClientInfo;
  }

  public AuthenticationResult resumeLogin(RequestContext pRequestContext) {
    UConBindMap lBindMap = new UConBindMap()
      .defineBind(":session_id_out", UCon.bindOutString())
      .defineBind(":client_info", mClientInfo)
      .defineBind(":app_display_name", "FOX-SYSTEM")
      .defineBind(":hash_code", mValidationHashCode)
      .defineBind(":status_code_out", UCon.bindOutString())
      .defineBind(":status_message_out", UCon.bindOutString());

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Resume Login");
    UConStatementResult lAPIResult;
    try {
      lAPIResult = lUCon.executeAPI(SQLManager.instance().getStatement(SESSION_CREATE_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Expected verification hash code not found", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Resume Login");
    }

    AuthenticationResult.Code lCode = AuthenticationResult.Code.fromString(lAPIResult.getString(":status_code_out"));
    AuthenticationResult lAuthenticationResult = new AuthenticationResult(lCode, lAPIResult.getString(":status_message_out"), lAPIResult.getString(":session_id_out"));
    return lAuthenticationResult;
  }
}

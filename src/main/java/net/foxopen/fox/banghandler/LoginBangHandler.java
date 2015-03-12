package net.foxopen.fox.banghandler;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.configuration.FoxBootConfig;
import net.foxopen.fox.entrypoint.FoxGlobals;

import java.util.Collection;
import java.util.Collections;

public class LoginBangHandler
implements BangHandler {

  private static final LoginBangHandler INSTANCE = new LoginBangHandler();
  public static LoginBangHandler instance() {
    return INSTANCE;
  }

  private LoginBangHandler() {}

  @Override
  public String getAlias() {
    return "LOGIN";
  }

  @Override
  public Collection<String> getParamList() {
    return Collections.emptySet();
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_SUPPORT;
  }

  @Override
  public boolean isDevAccessAllowed() {
    return false;
  }

  @Override
  public FoxResponse respond(FoxRequest pFoxRequest) {
    InternalAuthLevel lSessionAuthLevel = InternalAuthentication.instance().getSessionAuthLevel(pFoxRequest);
    FoxBootConfig lBC = FoxGlobals.getInstance().getFoxBootConfig();
    return BangHandlerServlet.basicHtmlResponse("Logged in as " + (lSessionAuthLevel == InternalAuthLevel.INTERNAL_ADMIN ? lBC.getAdminUsername() : lBC.getSupportUsername()));
  }
}

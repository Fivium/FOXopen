package net.foxopen.fox.entrypoint.auth;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.StandardAuthenticationContext;
import net.foxopen.fox.ex.ExSessionTimeout;
import net.foxopen.fox.thread.RequestContext;


public class AuthTypeFoxCommand
implements AuthType {

  private static AuthType INSTANCE = new AuthTypeFoxCommand();

  public static AuthType getInstance() {
    return INSTANCE;
  }

  private AuthTypeFoxCommand() {}

  @Override
  public AuthenticationContext processBeforeEntry(RequestContext pRequestContext) throws ExSessionTimeout {
    // Do nothing here, the user details are provided by a form from set-out and parsed/authenticated via fm:user-login

    // Automatically log in a user if a valid WUS is associated with the current FOX session cookie
    // This allows a user to login in tab 1, then open a new authenticated module in tab 2
    String lLatestWusId = pRequestContext.getFoxSession().getAuthContextSessionId(pRequestContext);
    if (!XFUtil.isNull(lLatestWusId)) {
      return new StandardAuthenticationContext(lLatestWusId);
    }
    else {
      return new StandardAuthenticationContext(pRequestContext);
    }
  }
}

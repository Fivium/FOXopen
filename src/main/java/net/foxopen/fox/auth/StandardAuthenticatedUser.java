package net.foxopen.fox.auth;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;

import java.util.Set;


public class StandardAuthenticatedUser
implements AuthenticatedUser {

  private final String mUsername;
  private final String mWuaId;
  private final String mEmailAddress;
  private final Set<String> mPrivilegeList;

  public StandardAuthenticatedUser(DOM pUserDOM, Set<String> pPrivilegeList) {

    try {
      mUsername = pUserDOM.get1S("FULL_NAME");
      mWuaId = pUserDOM.get1S("WUA_ID");
      mEmailAddress = pUserDOM.get1S("PRIMARY_EMAIL_ADDRESS");
    }
    catch (ExCardinality e) {
      throw new ExInternal("User DOM in unexpected format", e);
    }

    mPrivilegeList = pPrivilegeList;
  }

  @Override
  public String getUsername() {
    return mUsername;
  }

  @Override
  public String getAccountID() {
    return mWuaId;
  }

  @Override
  public String getEmailAddress() {
    return mEmailAddress;
  }

  @Override
  public Set<String> getPrivileges() {
    return mPrivilegeList;
  }

  @Override
  public boolean hasPrivilege(String pPrivilege) {
    return mPrivilegeList.contains(pPrivilege);
  }
}

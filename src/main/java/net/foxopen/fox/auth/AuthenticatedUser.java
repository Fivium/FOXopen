package net.foxopen.fox.auth;

import java.util.Set;

/**
 * Representation of an abstract user account within the FOX authentication system.
 */
public interface AuthenticatedUser {

  /**
   * Gets the username for this user. This may match the account ID or email address.
   * @return
   */
  public String getUsername();

  /**
   * Gets the unique internal identifier for this user.  This will typically be the WUA ID.
   * @return
   */
  public String getAccountID();

  /**
   * Gets the email address for this user, if one is available.
   * @return
   */
  public String getEmailAddress();

  /**
   * Gets a set of the names of this user's privileges.
   * @return
   */
  public Set<String> getPrivileges();

  public boolean hasPrivilege(String pPrivilege);

}

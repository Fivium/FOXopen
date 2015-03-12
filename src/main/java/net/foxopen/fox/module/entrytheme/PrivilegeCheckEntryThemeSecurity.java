package net.foxopen.fox.module.entrytheme;

import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

import java.util.Collection;

class PrivilegeCheckEntryThemeSecurity
extends DefinedEntryThemeSecurity {

  private final Collection<String> mAllowedPrivileges;

  PrivilegeCheckEntryThemeSecurity(XDoCommandList pBeforeValidateCommands, Collection<String> pAllowedPrivileges) {
    super(pBeforeValidateCommands);
    mAllowedPrivileges = pAllowedPrivileges;
  }

  @Override
  protected boolean runCheck(ActionRequestContext pRequestContext) {
    boolean lPrivMatched = false;
    PRIV_LOOP:
    for(String lPriv : mAllowedPrivileges) {
      //Search for at least 1 matching priv
      if(pRequestContext.getAuthenticationContext().getAuthenticatedUser().hasPrivilege(lPriv)) {
        lPrivMatched = true;
        Track.info("PrivilegeCSVCheck", "Matched privilege " + lPriv);
        break PRIV_LOOP;
      }
    }

    if(!lPrivMatched) {
      Track.info("PrivilegeCSVCheck", "Failed to match any privilege in CSV list");
    }

    return lPrivMatched;
  }

}

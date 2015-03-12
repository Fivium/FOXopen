package net.foxopen.fox.banghandler;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;

import java.util.Collection;

public interface BangHandler {

  String getAlias();

  Collection<String> getParamList();

  InternalAuthLevel getRequiredAuthLevel();

  boolean isDevAccessAllowed();

  FoxResponse respond(FoxRequest pFoxRequest);

}

package net.foxopen.fox.entrypoint;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.banghandler.BangHandlerServlet;
import net.foxopen.fox.banghandler.InternalAuthLevel;

import java.util.Collection;
import java.util.Collections;

public class VersionBangHandler
implements BangHandler {

  private static final VersionBangHandler INSTANCE = new VersionBangHandler();
  public static VersionBangHandler instance() {
    return INSTANCE;
  }

  private VersionBangHandler() { }

  @Override
  public String getAlias() {
    return "VERSION";
  }

  @Override
  public Collection<String> getParamList() {
    return Collections.emptySet();
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_ADMIN;
  }

  @Override
  public boolean isDevAccessAllowed() {
    return true;
  }

  @Override
  public FoxResponse respond(FoxRequest pFoxRequest) {
    EngineVersionInfo lEngineVersionInfo = FoxGlobals.getInstance().getEngineVersionInfo();
    String lHTML = "<strong>Version number:</strong> " + lEngineVersionInfo.getVersionNumber() + "<br/>";
    lHTML += "<strong>Build tag:</strong> " + lEngineVersionInfo.getBuildTag() + "<br/>";
    lHTML += "<strong>Build time:</strong> " + lEngineVersionInfo.getBuildTime() + "<br/>";

    return BangHandlerServlet.basicHtmlResponse(lHTML);
  }
}

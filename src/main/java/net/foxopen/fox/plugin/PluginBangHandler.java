package net.foxopen.fox.plugin;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.banghandler.BangHandlerServlet;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * PN note: this is a temporary class, plugin maintenance should be moved to WS status interface
 */
public class PluginBangHandler
implements BangHandler {

  private static final PluginBangHandler INSTANCE = new PluginBangHandler();
  public static final String RELOAD_PARAM_NAME = "reload";
  public static final String UNLOAD_PARAM_NAME = "unload";

  public static PluginBangHandler instance() {
    return INSTANCE;
  }

  private PluginBangHandler() { }

  @Override
  public String getAlias() {
    return "PLUGIN";
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
    return false;
  }

  @Override
  public FoxResponse respond(FoxRequest pFoxRequest) {

    String lReloadName = pFoxRequest.getHttpRequest().getParameter(RELOAD_PARAM_NAME);
    String lUnloadName = pFoxRequest.getHttpRequest().getParameter(UNLOAD_PARAM_NAME);

    if(lReloadName != null) {
      PluginManager.instance().reloadPlugin(lReloadName);
      return BangHandlerServlet.basicHtmlResponse("Reload " + StringEscapeUtils.escapeHtml4(lReloadName) + " OK");
    }
    else if(lUnloadName != null) {
      PluginManager.instance().unloadPlugin(lUnloadName);
      return BangHandlerServlet.basicHtmlResponse("Unload " + StringEscapeUtils.escapeHtml4(lUnloadName) + " OK");
    }
    else {
      PluginManager.instance().scanAndLoadPlugins();
      return BangHandlerServlet.basicHtmlResponse("Scan OK");
    }
  }
}

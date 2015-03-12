package net.foxopen.fox.banghandler;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;

/**
 * PN note: this is a temporary class, cache maintenance should be moved to WS status interface
 */
public class FlushCacheBangHandler
implements BangHandler {

  public static final String ID_PARAM_NAME = "id";

  private static final FlushCacheBangHandler INSTANCE = new FlushCacheBangHandler();
  public static FlushCacheBangHandler instance() {
    return INSTANCE;
  }

  private FlushCacheBangHandler() {}

  @Override
  public String getAlias() {
    return "FLUSHCACHE";
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
    CacheManager.flushCache(pFoxRequest.getParameter(ID_PARAM_NAME));
    return BangHandlerServlet.basicHtmlResponse("Flush OK");
  }
}

package net.foxopen.fox.module.mapset;

import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.mapset.MapSetDefinitionFactory.DefinitionBuilder;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;

/**
 * A MapSet which is constructed by arbitrary commands in an fm:do block.
 */
public class DefaultDefinition
extends MapSetDefinition {

  static class Builder
  extends DefinitionBuilder {

    @Override
    protected CacheKey getAutoCacheKeyOrNull() {
      //A default definition can never generate a cache key
      return null;
    }

    protected @Override
    Long getDefaultTimeoutMinsOrNull() {
      //The developer should be foreced to specify the refresh timeout for a default mapset
      return null;
    }

    @Override
    protected MapSetDefinition buildInternal(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule) throws ExModule {
      return new DefaultDefinition(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule);
    }
  }

  private DefaultDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule) {
    super(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule);
  }

  @Override
  protected DOM createMapSetDOM(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {
    return createDefaultContainerDOM();
  }

}

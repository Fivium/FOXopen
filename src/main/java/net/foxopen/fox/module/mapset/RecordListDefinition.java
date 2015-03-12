package net.foxopen.fox.module.mapset;

import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.mapset.MapSetDefinitionFactory.DefinitionBuilder;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;

public class RecordListDefinition
extends MapSetDefinition {

  static class Builder
  extends DefinitionBuilder {

    private final DOMList mRecList;

    Builder(DOM pDefinitionElement)
    throws ExModule {
      //Read the rec list straight off the definition element
      mRecList = pDefinitionElement.getUL("rec");
      if(mRecList.size() == 0) {
        throw new ExModule("MapSet definition missing 'rec' elements");
      }
    }

    @Override
    protected CacheKey getAutoCacheKeyOrNull() {
      return CacheKey.createStaticCacheKey();
    }

    @Override
    protected Long getDefaultTimeoutMinsOrNull() {
      return MapSetDefinitionFactory.NEVER_REFRESH_TIMEOUT_MINS;
    }

    @Override
    protected MapSetDefinition buildInternal(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule) throws ExModule {
      return new RecordListDefinition(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule, mRecList);
    }
  }

  private final DOMList mRecList;

  private RecordListDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule, DOMList pRecList)
  throws ExModule {
    super(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule);
    mRecList = pRecList;
  }

  @Override
  protected DOM createMapSetDOM(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {

    DOM lMapSetContainerDOM = createDefaultContainerDOM();
    DOM lMapSetDOM = lMapSetContainerDOM.addElem(MapSet.MAPSET_ELEMENT_NAME);

    //Copy recs from the rec list into the mapset DOM
    mRecList.copyContentsTo(lMapSetDOM);

    return lMapSetContainerDOM;
  }
}

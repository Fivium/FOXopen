package net.foxopen.fox.module.datadefinition;

import com.google.common.base.Joiner;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.dbinterface.InterfaceStatement;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datadefinition.datatransformer.TransformerEnum;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;
import net.foxopen.fox.track.Track;

import java.util.HashMap;
import java.util.Map;

public class DataDefinition {
  // Name of the DataDefinition
  private final String mName;
  // Name of the DataDefinition including the module name it came from for better cache entropy
  private final String mFullName;

  private final InterfaceStatement mQueryStatement;

  private final boolean mIsAJAX;
  private final CacheKey mCacheKey;
  private final Long mRefreshTimeoutMins;

  private final TransformerEnum mDataTransformer;

  private final DOM mColumnMapping;


  private DataDefinition (Mod pModule, DOM pDefinitionElement, DOM pDataQueryElement, boolean pIsAJAX, TransformerEnum pDataTransformer, DOM pColumnMapping, CacheKey pCacheKey, Long pRefreshTimeoutMins)
    throws ExModule {
    String lDBInterfaceName = pDataQueryElement.getAttr("interface");
    String lQueryName = pDataQueryElement.getAttr("query");

    if(XFUtil.isNull(lDBInterfaceName)) {
      throw new ExModule("Bad definition - missing mandatory 'interface' attribute from data-query element");
    }
    else if(XFUtil.isNull(lQueryName)) {
      throw new ExModule("Bad definition - missing mandatory 'query' attribute from data-query element");
    }

    mQueryStatement = pModule.getDatabaseInterface(lDBInterfaceName).getInterfaceQuery(lQueryName);

    mName = pDefinitionElement.getAttr("name");
    mFullName = pModule.getName() + "/" + mName;

    mIsAJAX = pIsAJAX;
    mCacheKey = pCacheKey;

    mRefreshTimeoutMins = pRefreshTimeoutMins;

    mDataTransformer = pDataTransformer;
    mColumnMapping = pColumnMapping;
  }

  public EvaluatedDataDefinition getOrCreateEvaluatedDataDefinition(ActionRequestContext pRequestContext, ImplicatedDataDefinition pImplicatedDataDefinition) {
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    Track.pushInfo("getOrCreateEvaluatedDataDefinition");
    try {
      String lCacheKeyPrefix = pRequestContext.getRequestAppMnem() + "/" + mFullName;
      String lEvaluatedCacheKey = mCacheKey.evaluate(lCacheKeyPrefix, lContextUElem, pRequestContext.getCurrentCallId());
      Track.debug("DataDefinitionCacheKey", lEvaluatedCacheKey);

      FoxCache<String, EvaluatedDataDefinition> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.DATA_DEFINITIONS);
      EvaluatedDataDefinition lCachedEvaluatedDataDefinition = lFoxCache.get(lEvaluatedCacheKey);

      boolean lCreateNewEvaluatedDataDefinition;
      if (getRefreshTimeoutMins() <= 0 || lCachedEvaluatedDataDefinition == null) {
        // If refresh timeout is specified as 0 or an evaluated copy wasn't found in the cache a new one needs creating
        lCreateNewEvaluatedDataDefinition = true;
      }
      else {
        // Otherwise, only create if the specified number of minutes has elapsed since the cache-hit evaluated data definition was created
        lCreateNewEvaluatedDataDefinition = System.currentTimeMillis() - lCachedEvaluatedDataDefinition.getCreatedTimeMS() > getRefreshTimeoutMins() * 60 * 1000;
      }

      if(lCreateNewEvaluatedDataDefinition) {
        // If no evaluated data definition is in cache, or a refresh is required recreate the evaluated data definition
        Track.debug("DataDefinitionCreate", lCachedEvaluatedDataDefinition == null ? "DataDefinition not in cache" : "DataDefinition refresh required");

        EvaluatedDataDefinition lNewEvaluatedDataDefinition = new EvaluatedDataDefinition(pRequestContext, this, pImplicatedDataDefinition);

        if (!mCacheKey.containsUniqueBind()) {
          // Only cache the Evaluated Data Definition if the cache key isn't unique
          lFoxCache.put(lEvaluatedCacheKey, lNewEvaluatedDataDefinition);
        }

        return lNewEvaluatedDataDefinition;
      }
      else {
        Track.debug("DataDefinitionCacheHit");
        return lCachedEvaluatedDataDefinition;
      }
    }
    finally {
      Track.pop("getOrCreateEvaluatedDataDefinition");
    }
  }

  public static DataDefinition createDefinitionFromDOM(DOM pDefinitionDOM, Mod pModule)
    throws ExModule, ExDoSyntax {

    //Get the name
    String lDataDefinitionName = pDefinitionDOM.getAttr("name");
    if(XFUtil.isNull(lDataDefinitionName)) {
      throw new ExModule("fm:data-definition element must have a name attribute", pDefinitionDOM);
    }

    Track.pushDebug("CreateDataDefinition", "DataDefinition name " + lDataDefinitionName);
    try {
      //Load all tags into a map
      Map<String, DOM> lDefinitionTagNameMap = new HashMap<>();
      for(DOM lChildDOM : pDefinitionDOM.getChildElements()) {
        lDefinitionTagNameMap.put(lChildDOM.getLocalName(), lChildDOM);
      }

      //Reduce the tag map to ensure the definition is valid

      // Get the data query definition
      DOM lDataQueryBlock = lDefinitionTagNameMap.remove("data-query");
      if(lDataQueryBlock == null) {
        throw new ExInternal("Missing data-query from data-definition");
      }

      // Get the data query definition
      boolean lIsAJAX = false;
      DOM lAJAXDataFlag = lDefinitionTagNameMap.remove("ajax-data");
      if(lAJAXDataFlag != null) {
        lIsAJAX = Boolean.valueOf(lAJAXDataFlag.value());
      }

      // Get the data query definition
      TransformerEnum lDataTransformer;
      DOM lTransformerBlock = lDefinitionTagNameMap.remove("transformer");
      if(lTransformerBlock == null) {
        throw new ExInternal("Missing transformer from data-definition");
      }
      else {
        String lTransformerString = lTransformerBlock.value();
        if (XFUtil.isNull(lTransformerString)) {
          throw new ExInternal("Transformer from data-definition not defined or not 'object', 'array' or 'verbatim': " + lTransformerString);
        }
        if ("object".equalsIgnoreCase(lTransformerString) ) {
          lDataTransformer = TransformerEnum.OBJECT;
        }
        else if ("array".equalsIgnoreCase(lTransformerString) ) {
          lDataTransformer = TransformerEnum.ARRAY;
        }
        else if ("verbatim".equalsIgnoreCase(lTransformerString) ) {
          lDataTransformer = TransformerEnum.VERBATIM;
        }
        else {
          throw new ExInternal("Transformer from data-definition not 'object', 'array' or 'verbatim': " + lTransformerString);
        }
      }

      // Get the data query definition
      DOM lColumnMappingBlock = lDefinitionTagNameMap.remove("column-mapping");
      if(lColumnMappingBlock == null && lDataTransformer != TransformerEnum.VERBATIM) {
        throw new ExInternal("Missing column-mapping from data-definition");
      }

      // Get the cache key definition
      CacheKey lCacheKey;
      DOM lCacheKeyBlock = lDefinitionTagNameMap.remove("cache-key");
      if(lCacheKeyBlock != null) {
        lCacheKey = CacheKey.createFromDefinitionDOM(lCacheKeyBlock, false);
      }
      else {
        lCacheKey = null;
      }

      // Get and parse refresh-timeout-mins if defined
      Long lRefreshTimeoutMins;
      try {
        lRefreshTimeoutMins = getRefreshTimeoutMinsOrNull(lDefinitionTagNameMap);
      }
      catch (ExModule e) {
        throw new ExModule("Bad refresh timeout definition for data-definition", e);
      }

      // Error if we have any other unexpected elements left over
      if(lDefinitionTagNameMap.size() > 0) {
        throw new ExModule("Invalid data definition for " + lDataDefinitionName + " - read all the properties from the definition that are known but properties remain: " + Joiner.on(", ").join(lDefinitionTagNameMap.keySet()));
      }

      return new DataDefinition(pModule, pDefinitionDOM, lDataQueryBlock, lIsAJAX, lDataTransformer, lColumnMappingBlock, lCacheKey, lRefreshTimeoutMins);
    }
    finally {
      Track.pop("CreateDataDefinition");
    }
  }

  private static Long getRefreshTimeoutMinsOrNull(Map<String, DOM> pDefinitionTagNameMap)
      throws ExModule {
    DOM lTimeoutMinsElem = pDefinitionTagNameMap.remove("refresh-timeout-mins");
    DOM lRefreshElem = pDefinitionTagNameMap.remove("refresh");

    if(lTimeoutMinsElem != null && lRefreshElem != null) {
      throw new ExModule("fm:refresh and fm:refresh-timeout-mins are mutually exclusive");
    }

    if(lTimeoutMinsElem != null) {
      Long lRefreshTimeoutMins;
      try {
        lRefreshTimeoutMins = Long.valueOf(lTimeoutMinsElem.value());
      }
      catch (NumberFormatException e) {
        throw new ExModule("Invalid number specified for refresh-timeout-mins", e);
      }
      if(lRefreshTimeoutMins < 0) {
        throw new ExModule("Illegal negative number specified for refresh-timeout-mins");
      }

      return lRefreshTimeoutMins;
    }
    else if(lRefreshElem != null) {
      String lRefreshElemValue = lRefreshElem.value();
      if("always".equals(lRefreshElemValue)) {
        return 0L;
      }
      else if("never".equals(lRefreshElemValue)) {
        return 999999999L;
      }
      else {
        throw new ExModule("Unrecognised value " + lRefreshElemValue + " for fm:refresh element");
      }
    }
    else {
      // Neither specified - return a default timeout of never
      return 999999999L;
    }
  }

  public String getName() {
    return mName;
  }

  public InterfaceStatement getQueryStatement() {
    return mQueryStatement;
  }

  public boolean isAJAX() {
    return mIsAJAX;
  }

  public CacheKey getCacheKey() {
    return mCacheKey;
  }

  public Long getRefreshTimeoutMins() {
    return mRefreshTimeoutMins;
  }

  public TransformerEnum getDataTransformer() {
    return mDataTransformer;
  }

  public DOM getColumnMapping() {
    return mColumnMapping;
  }
}

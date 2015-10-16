package net.foxopen.fox.module.mapset;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dom.PathOrDOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;

/**
 * Mapset name and bind information to be cached specially for AJAX Search selectors. The cache decreases the time required
 * to run an AJAX search query by avoiding having to ramp a thread.
 */
public class AJAXSearchQueryCachedBinds {

  private final String mAppMnem;
  private final String mModule;
  private final String mMapSetName;
  private final BindObjectProvider mEvaluatedBinds;

  /**
   * Evaluates the bind variables for the given item's AJAX mapset search query, and caches them for later retrieval by the
   * mapset webservice. This improves the performance of the web service by avoiding having to ramp a thread to run the search
   * query.<br/><br/>
   *
   * The MapSet defined on the given ENI must be a JITMapSet - otherwise this method throws an exception.
   *
   * @param pRequestContext Current RequestContext.
   * @param pEvaluateNodeInfo ENI for the JITMapSet node.
   */
  public static void cacheSearchQueryBinds(ActionRequestContext pRequestContext, EvaluatedNodeInfoItem pEvaluateNodeInfo) {
    if(pEvaluateNodeInfo.getMapSet() != null && pEvaluateNodeInfo.getMapSet() instanceof JITMapSet) {

      Track.pushInfo("CacheSearchQueryBinds", pEvaluateNodeInfo.getMapSet().getMapSetName());
      try {
        AJAXQueryDefinition lMapSetDefinition = ((JITMapSet) pEvaluateNodeInfo.getMapSet()).getMapSetDefinition();
        InterfaceQuery lInterfaceStatement = lMapSetDefinition.getSearchQueryStatement(pRequestContext);

        ContextUElem lContextUElem = pRequestContext.getContextUElem().localise("AJAXSearchQueryCache");
        BindObjectProvider lEvaluatedBinds;
        try {
          //Set item[rec] contexts in case they're used in the query definition
          //TODO - AJMS - mapset attach
          lMapSetDefinition.setupContextUElem(lContextUElem, pEvaluateNodeInfo.getDataItem(), new PathOrDOM(""));
          lEvaluatedBinds = lInterfaceStatement.preEvaluateBinds(pRequestContext, pEvaluateNodeInfo.getDataItem());
        }
        finally {
          lContextUElem.delocalise("AJAXSearchQueryCache");
        }

        String lAppMnem = pRequestContext.getModuleApp().getAppMnem();
        String lModule = pRequestContext.getCurrentModule().getName();

        lInterfaceStatement.getStatementName();

        String lCacheKey = cacheKey(pRequestContext.getThreadInfoProvider().getThreadId(), pEvaluateNodeInfo.getFieldMgr().getExternalFieldName());

        FoxCache<Object, Object> lCache = CacheManager.getCache(BuiltInCacheDefinition.AJAX_MAPSET_BINDS);
        lCache.put(lCacheKey, new AJAXSearchQueryCachedBinds(lAppMnem, lModule, lMapSetDefinition.getLocalName(), lEvaluatedBinds));
      }
      finally {
        Track.pop("CacheSearchQueryBinds");
      }
    }
    else {
      throw new ExInternal("Cannot cache binds for mapset " + pEvaluateNodeInfo.getMapSet());
    }
  }

  /**
   * Gets the cached object corresponding to the given thread/field ID tuple, or null if nothing is cached.
   * @param pThreadId User's current thread ID.
   * @param pFieldId External field ID for the mapset element.
   * @return AJAX cached bind object, or null if nothing is cached.
   */
  public static AJAXSearchQueryCachedBinds getCachedBindsOrNull(String pThreadId, String pFieldId) {
    return CacheManager.<String, AJAXSearchQueryCachedBinds>getCache(BuiltInCacheDefinition.AJAX_MAPSET_BINDS).get(cacheKey(pThreadId, pFieldId));
  }

  private static String cacheKey(String pThreadId, String pFieldId){
    return pThreadId + "/" + pFieldId;
  }

  private AJAXSearchQueryCachedBinds(String pAppMnem, String pModule, String pMapSetName, BindObjectProvider pEvaluatedBinds) {
    mAppMnem = pAppMnem;
    mModule = pModule;
    mMapSetName = pMapSetName;
    mEvaluatedBinds = pEvaluatedBinds;
  }

  @Override
  public String toString() {
    return mMapSetName + ": " + mEvaluatedBinds;
  }

  /**
   * Gets the mapset definition corresponding to the field of this cache object.
   * @return Mapset definition.
   */
  public AJAXQueryDefinition getMapSetDefinition() {
    Mod lMod = Mod.getModuleForAppMnemAndModuleName(mAppMnem, mModule);
    return (AJAXQueryDefinition) lMod.getMapSetDefinitionByName(mMapSetName);
  }

  /**
   * Runs the AJAX search query for this cached mapset using the pre-cached binds, delivering the results into the given QueryResultDeliverer.
   *
   * @param pRequestContext For UCon retrieval.
   * @param pSearchTermBindProvider BindProvider for providing the search term bind. This will be used to decorate the
   *                                pre-cached BindObjectProvider already stored on this object.
   * @param pQueryResultDeliverer Destination for search query result.
   */
  public void runSearchQuery(RequestContext pRequestContext, DecoratingBindObjectProvider pSearchTermBindProvider, QueryResultDeliverer pQueryResultDeliverer) {

    BindObjectProvider lBindObjectProvider = pSearchTermBindProvider.decorate(mEvaluatedBinds);

    Mod lMod = Mod.getModuleForAppMnemAndModuleName(mAppMnem, mModule);

    //Manually find the query to run (we can't use the standard lookup because we don't have an ActionRequestContext)
    AJAXQueryDefinition lMapSetDefinition = getMapSetDefinition();
    InterfaceQuery lInterfaceQuery = lMod.getDatabaseInterface(lMapSetDefinition.getDBInterfaceName()).getInterfaceQuery(lMapSetDefinition.getSearchQueryStatementName());
    ExecutableQuery lExecutableQuery = lInterfaceQuery.getParsedStatement().createExecutableQuery(lBindObjectProvider);

    //Run the search query into the given deliverer
    UCon lUCon = pRequestContext.getContextUCon().getUCon("AJAXSearch");
    try {
      lExecutableQuery.executeAndDeliver(lUCon, pQueryResultDeliverer);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to execute search query for mapset: " + mMapSetName, e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "AJAXSearch");
    }
  }
}

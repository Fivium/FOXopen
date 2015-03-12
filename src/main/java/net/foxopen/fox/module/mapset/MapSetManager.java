package net.foxopen.fox.module.mapset;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.PathOrDOM;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;


/**
 * Maintains a "local" cache for MapSets which cannot be cached globally, and provides the functionality to track which
 * MapSets have been processed in the current request. This means MapSets can be cached at the appropriate level and do
 * not need to be refreshed more frequently than necessary.
 */
public class MapSetManager {

  /** All cache keys which have been processed in the current churn (cleared after every churn). */
  private final Set<String> mProcessedCacheKeys = new HashSet<>();
  /** Cached MapSets which are scoped to a module call. */
  private final Map<String, MapSet> mModuleCallScopeMapSetCache = new HashMap<>();

  private final MapSetInstanceTracker mMapSetInstanceTracker = new MapSetInstanceTracker();

  /** MapSets which should be refreshed every churn - this cache is used to prevent duplicate evaluations within a churn. */
  private final Map<String, MapSet> mRefreshAlwaysMapSetCache = new HashMap<>();

  public MapSetManager() {}

  /**
   * Resolves a MapSet, using this manager for local cache lookup. See {@link MapSetDefinition#getOrCreateMapSet} for implementation details.
   * @param pRequestContext
   * @param pMapSetName
   * @param pItemDOM
   * @param pMapSetAttach
   * @param pUniqueConstant
   * @return
   */
  public MapSet getMapSet(ActionRequestContext pRequestContext, String pMapSetName, DOM pItemDOM, PathOrDOM pMapSetAttach, String pUniqueConstant) {
    MapSetDefinition lMapSetDefinition = pRequestContext.getCurrentModule().getMapSetDefinitionByName(pMapSetName);
    MapSet lMapSet = lMapSetDefinition.getOrCreateMapSet(pRequestContext, pItemDOM, pMapSetAttach, pUniqueConstant, this);
    return lMapSet;
  }

  /**
   * Clears the processed MapSet keys and "refresh always" MapSets from this manager so it is in the correct state for
   * the next page request.
   */
  public void handleRequestCompletion() {
    mProcessedCacheKeys.clear();
    mRefreshAlwaysMapSetCache.clear();
  }

  /**
   * Lookup a MapSet with the given cache key in this manager's local cache.
   * @param pCacheKey Cache key to look up.
   * @return Locally cached MapSet or null.
   */
  public MapSet getLocalCachedMapSet(String pCacheKey) {
    //Check the module call scope cache first
    MapSet lMapSet = mModuleCallScopeMapSetCache.get(pCacheKey);
    if(lMapSet != null) {
      Track.debug("ModuleCallScopeCacheHit");
      return lMapSet;
    }
    else {
      return mRefreshAlwaysMapSetCache.get(pCacheKey);
    }
  }

  /**
   * Caches the given MapSet in this manager's local cache.
   * @param pMapSet
   */
  public void addLocalCachedMapSet(MapSet pMapSet) {
    if(pMapSet.getMapSetDefinition().isRefreshAlways()) {
      //If the mapset will be refreshed every churn, store it in a map which will expire at the end of the churn
      mRefreshAlwaysMapSetCache.put(pMapSet.getEvaluatedCacheKey(), pMapSet);
    }
    else {
      mModuleCallScopeMapSetCache.put(pMapSet.getEvaluatedCacheKey(), pMapSet);

      //Record the mapset in a local instance tracker
      mMapSetInstanceTracker.addMapSet(pMapSet);
    }
  }

  /**
   * Tests if a MapSet with the given cache key has been marked as processed in the scope of the current request.
   * @param pCacheKey
   * @return
   */
  public boolean isProcessed(String pCacheKey) {
    return mProcessedCacheKeys.contains(pCacheKey);
  }

  /**
   * Marks the given MapSet as processed within the scope of the current request.
   * @param pCacheKey
   */
  public void markMapSetProcessed(String pCacheKey) {
    mProcessedCacheKeys.add(pCacheKey);
  }

  public void refreshMapSets(ActionRequestContext pRequestContext, String pMapSetName) {

    //Resolve the definition based on name
    MapSetDefinition lDefinition = pRequestContext.getCurrentModule().getMapSetDefinitionByName(pMapSetName);

    //If this definition is in local cache refresh the local cache, otherwise refresh the global cache
    if(lDefinition.isCachedLocally()) {
      refreshMapSets(lDefinition);
    }
    else {
      MapSet.refreshMapSets(lDefinition);
    }
  }

  private void refreshMapSets(MapSetDefinition pDefinition) {
    mMapSetInstanceTracker.refreshMapSets(pDefinition, mModuleCallScopeMapSetCache);
  }

  public String debugOutput() {
    StringBuilder lStringBuilder = new StringBuilder(mModuleCallScopeMapSetCache.values().size() + " mapsets (cache keys below)<br>");
    for(String lMsCacheKey : mModuleCallScopeMapSetCache.keySet()) {
      lStringBuilder.append(lMsCacheKey + "<br>");
    }
    return lStringBuilder.toString();
  }
}

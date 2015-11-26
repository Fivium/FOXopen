package net.foxopen.fox.module.mapset;

import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.fieldset.fvm.FVMOption;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.List;


public interface MapSet {
  //Constants for mapsets in DOM form
  String MAPSET_LIST_ELEMENT_NAME = "map-set-list";
  String MAPSET_ELEMENT_NAME = "map-set";
  String REC_ELEMENT_NAME = "rec";
  String KEY_ELEMENT_NAME = "key";
  String DATA_ELEMENT_NAME = "data";

  //TODO keep an eye on this, may become bloated over engine lifecycle
  MapSetInstanceTracker gMapSetInstanceTracker = new MapSetInstanceTracker();

  static MapSet getFromCache(String pCacheKey) {
    FoxCache<String, MapSet> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.MAPSETS);
    return lFoxCache.get(pCacheKey);
  }

  static void addToCache(MapSet pMapSet) {
    FoxCache<String, MapSet> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.MAPSETS);
    lFoxCache.put(pMapSet.getEvaluatedCacheKey(), pMapSet);
    //Record the mapset in the global instance tracker
    gMapSetInstanceTracker.addMapSet(pMapSet);
  }

  static void refreshMapSets(MapSetDefinition pDefinition) {
    FoxCache<String, MapSet> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.MAPSETS);
    gMapSetInstanceTracker.refreshMapSets(pDefinition, lFoxCache);
  }

  /**
   * Gets the name of this mapset as defined in module markup. This will be relative to the module the MapSet is defined in.
   * @return This mapset's name.
   */
  String getMapSetName();

  /**
   * Tests if this mapset is dynamic, i.e. if it may change between page churns.
   * @return True if dynamic.
   */
  boolean isDynamic();

  /**
   * Tests if this MapSet requires refreshing, based on its definition (i.e. if a refresh is always/never required)
   * or the time elapsed since it was last refreshed.
   * @return True if a refresh is required.
   */
  boolean isRefreshRequired();

  /**
   * Just-in-time generates a DOM representation of this mapset, in the form: /map-set-list/map-set/rec/{key | data}
   * @return This MapSet as a DOM.
   */
  DOM getMapSetAsDOM();

  /**
   * Tests if this MapSet contains the given DOM value.
   * @param pRequestContext Request context to potentially use when checking for the value
   * @param pDataDOM DOM value to check for.
   * @return True if the value is contained within this MapSet according to the MapSet's rules.
   */
  boolean containsData(ActionRequestContext pRequestContext, DOM pDataDOM);

  /**
   * Resolves the given data DOM to its corresponding key value. If the data DOM does not exist as a "data" item in this
   * MapSet, empty string is returned.
   *
   * @param pRequestContext
   * @param pDataDOM DOM to resolve.
   * @return MapSet key or empty string.
   */
  String getKey(ActionRequestContext pRequestContext, DOM pDataDOM);

  /**
   * Attempts to resolve the given data string value to its corresponding key. This will only be possible for SimpleMapSets -
   * ComplexMapSets will return null. Empty string should be returned by a SimpleMapSet if no key could be found.
   *
   * @param pRequestContext
   * @param pMapSetItem
   *@param pDataString Data string to resolve.  @return The corresponding key, empty string if no key exists, or null if a lookup was not possible.
   */
  String getKeyForDataString(ActionRequestContext pRequestContext, DOM pMapSetItem, String pDataString);

  MapSetDefinition getMapSetDefinition();

  String getEvaluatedCacheKey();

  // Not to be implemented in JIT MapSet
  int indexOf(DOM pItemDOM);
  List<FVMOption> getFVMOptionList();
  List<MapSetEntry> getEntryList();
}

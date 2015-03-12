package net.foxopen.fox.module.mapset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.foxopen.fox.track.Track;


/**
 * Maintains a map of MapSet definition keys to instantiated MapSets. This facilitates refresh mapset functionality by
 * providing a quick way of resolving any MapSet instance which corresponds to a definition's key.
 */
class MapSetInstanceTracker {

  /** Map of definition keys to sets of implicated MapSets. These should be weak sets to prevent retention by this object. */
  private final Map<String, Set<MapSet>> mDefinitionKeysToMapSets = new HashMap<>();

  MapSetInstanceTracker() {}

  void addMapSet(MapSet pMapSet) {
    String lDefinitionKey = pMapSet.getMapSetDefinition().getDefinitionKey();
    Set<MapSet> lDefinitionKeyMapSets = mDefinitionKeysToMapSets.get(lDefinitionKey);

    //Store a weak reference to this MapSet against the definition key
    if(lDefinitionKeyMapSets == null) {
      //Bootstrap a new weak HashSet (backed by a WeakHashMap) for storing MapSets for this definition
      lDefinitionKeyMapSets = Collections.newSetFromMap(new WeakHashMap<MapSet, Boolean>());
      mDefinitionKeysToMapSets.put(lDefinitionKey, lDefinitionKeyMapSets);
    }

    lDefinitionKeyMapSets.add(pMapSet);
  }

  public void refreshMapSets(MapSetDefinition pDefinition, Map<String, MapSet> pCache) {
    Set<MapSet> lImplicatedMapSets = mDefinitionKeysToMapSets.get(pDefinition.getDefinitionKey());

    int lRefreshCount = 0;
    Track.pushDebug("RefreshMapSets", pDefinition.getFullName());
    try {
      if(lImplicatedMapSets != null) {
        for(MapSet lMapSet : lImplicatedMapSets) {
          pCache.remove(lMapSet.getEvaluatedCacheKey());
          lRefreshCount++;
        }
      }
      Track.debug("RefreshedCount", Integer.toString(lRefreshCount));
    }
    finally {
      Track.pop("RefreshMapSets");
    }
  }
}

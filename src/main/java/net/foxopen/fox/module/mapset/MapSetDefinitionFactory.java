package net.foxopen.fox.module.mapset;


import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.storage.CacheKey;
import net.foxopen.fox.thread.storage.StorageLocation;
import net.foxopen.fox.track.Track;

import java.util.HashMap;
import java.util.Map;


public final class MapSetDefinitionFactory {
  private MapSetDefinitionFactory() {}

  static final long NEVER_REFRESH_TIMEOUT_MINS = 999999999L;

  /** Elements which can appear in a mapset definition. */
  static enum DefinitionTag {
    STORAGE_LOCATION,
    DO,
    TEMPLATE,
    RECORD_LIST,
    RECORD_QUERY,
    DOM_QUERY,
    DATABASE_DEFINITION,
    CACHE_KEY,
    REFRESH,
    REFRESH_TIMEOUT_MINS,
    REFRESH_IN_BACKGROUND;

    @Override
    public String toString() {
      return name().toLowerCase().replaceAll("_", "-");
    }
  }

  /**
   * Definitions should be constructed via a builder. This abstract class encapsulates the logic for when an auto cache key
   * and/or default timeout should be established. It also ensures all definitions are forced to define the logic for these
   * two processes.
   */
  static abstract class DefinitionBuilder {

    /**
     * Gets the auto cache key for this definition. This will only be requested if it is not explicitly defined. If the
     * definition is unable to generate a cache key, this should either throw an exception containing extra details,
     * or return null (a default exception to be thrown in this case).
     * @return Auto cache key or null
     */
    protected abstract CacheKey getAutoCacheKeyOrNull();

    /**
     * Gets the default timeout for this definition. This will only be requested if it is not explicitly defined. If it
     * is not possible for a definition type to derive a default, this should either throw an exception containing extra details,
     * or return null (a default exception to be thrown in this case).
     * @return Timeout mins or null.
     */
    protected abstract Long getDefaultTimeoutMinsOrNull();

    protected final MapSetDefinition build(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, Long pRefreshTimeoutMins, Mod pModule)
    throws ExModule {

      CacheKey lCacheKey;
      if (pCacheKey != null) {
        lCacheKey = pCacheKey;
      }
      else if (pXDo == null) {
        //Only generate an auto cache key if an fm:do is not specified
        lCacheKey = getAutoCacheKeyOrNull();
      }
      else {
        lCacheKey = null;
      }

      if (lCacheKey == null) {
        throw new ExModule("Cache key not defined and could not be automatically generated for mapset " + pLocalName);
      }

      Long lRefreshTimeoutMins;
      if (pRefreshTimeoutMins == null && pXDo == null) {
        lRefreshTimeoutMins = getDefaultTimeoutMinsOrNull();
      }
      else {
        lRefreshTimeoutMins = pRefreshTimeoutMins;
      }

      if (lRefreshTimeoutMins == null) {
        throw new ExModule("Refresh timeout not specified and no default available for mapset " + pLocalName);
      }

      return buildInternal(pLocalName, lCacheKey, pXDo, lRefreshTimeoutMins, pModule);
    }

    /**
     * Implementors should construct the definition object here.
     */
    protected abstract MapSetDefinition buildInternal(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule)
    throws ExModule;
  }

  /**
   * Creates a new MapSetDefinition from the markup specified in the given DOM.
   * @param pDefinitionDOM DOM containing MapSet markup.
   * @param pModule Module containing the MapSet definition.
   * @return New MapSetDefinition.
   * @throws ExModule If syntax is invalid.
   * @throws ExDoSyntax If syntax is invalid.
   */
  public static MapSetDefinition createDefinitionFromDOM(DOM pDefinitionDOM, Mod pModule)
  throws ExModule, ExDoSyntax {

    //Get the name
    String lMapSetName = pDefinitionDOM.getAttr("name");
    if(XFUtil.isNull(lMapSetName)) {
      throw new ExModule("fm:map-set element must have a name attribute", pDefinitionDOM);
    }

    Track.pushDebug("CreateMapSetDefinition", "MapSet name " + lMapSetName);
    try {
      //Load all tags into a map
      Map<String, DOM> lDefinitionTagNameMap = new HashMap<>();
      for(DOM lChildDOM : pDefinitionDOM.getChildElements()) {
        lDefinitionTagNameMap.put(lChildDOM.getLocalName(), lChildDOM);
      }

      //Reduce the tag map to ensure the definition is valid

      //Refresh in background was never used, remove and ignore it
      lDefinitionTagNameMap.remove(DefinitionTag.REFRESH_IN_BACKGROUND.toString());

      //Get and parse the fm:do block if defined
      XDoCommandList lXDo;
      DOM lDoBlock = lDefinitionTagNameMap.remove(DefinitionTag.DO.toString());
      if(lDoBlock != null) {
        lXDo = new XDoCommandList(pModule, lDoBlock);
      }
      else {
        lXDo = null;
      }

      //Get and parse refresh-timeout-mins if defined
      Long lRefreshTimeoutMins;
      try {
        lRefreshTimeoutMins = getRefreshTimeoutMinsOrNull(lDefinitionTagNameMap);
      }
      catch (ExModule e) {
        throw new ExModule("Bad refresh timeout definition for mapset " + lMapSetName, e);
      }

      //Read storage location definition if defined (legacy)
      boolean lStorageLocationHasQuery;
      StorageLocation lStorageLocation;
      if (lDefinitionTagNameMap.containsKey(DefinitionTag.STORAGE_LOCATION.toString())) {
        String lSLName = lDefinitionTagNameMap.get(DefinitionTag.STORAGE_LOCATION.toString()).value();
        lStorageLocation = pModule.getDataStorageLocation(lSLName);
        lStorageLocationHasQuery = lStorageLocation.hasQueryStatement();
      }
      else {
        lStorageLocationHasQuery = false;
        lStorageLocation = null;
      }

      //Get the cache key definition
      CacheKey lCacheKey;
      DOM lCacheKeyBlock = lDefinitionTagNameMap.remove(DefinitionTag.CACHE_KEY.toString());
      if(lCacheKeyBlock != null) {
        lCacheKey = CacheKey.createFromDefinitionDOM(lCacheKeyBlock, false);
      }
      else if (lStorageLocation != null) {
        lCacheKey = lStorageLocation.getCacheKey();
      }
      else {
        lCacheKey = null;
      }

      //We should have one element left which defines the type of mapset definition we're after; error if not
      if(lDefinitionTagNameMap.size() > 1) {
        throw new ExModule("Invalid mapset definition for " + lMapSetName + " - expected at most one element to define mapset type but found " + Joiner.on(", ").join(lDefinitionTagNameMap.keySet()));
      }
      else if(lDefinitionTagNameMap.size() == 0 && lXDo == null) {
        //If nothing's left in the map, check that at least an fm:do was defined
        throw new ExModule("Invalid mapset definition for " + lMapSetName + " - no way to determine mapset type");
      }

      //Determine builder type and build the definition
      DefinitionBuilder lDefinitionBuilder;
      try {
        if(lDefinitionTagNameMap.size() == 0) {
          //No type definition DOM - it's a do block only mapset
          lDefinitionBuilder = new DefaultDefinition.Builder();
        }
        else {
          //Get the last tag from the map (should be the only one left)
          String lDefintionTypeTag = lDefinitionTagNameMap.keySet().iterator().next();
          DOM lDefinitionTypeDOM = lDefinitionTagNameMap.get(lDefintionTypeTag);

          if(DefinitionTag.STORAGE_LOCATION.toString().equals(lDefintionTypeTag)) {
            if(lStorageLocationHasQuery) {
              //Legacy: storage location with query
              lDefinitionBuilder = new SLQueryDefinition.Builder(lStorageLocation);
            }
            else {
              //Legacy: storage location with no query and a do block - effectively do block only
              lDefinitionBuilder = new DefaultDefinition.Builder();
            }
          }
          else if(DefinitionTag.DOM_QUERY.toString().equals(lDefintionTypeTag) || DefinitionTag.RECORD_QUERY.toString().equals(lDefintionTypeTag)) {
            lDefinitionBuilder = new InterfaceQueryDefinition.Builder(pModule, lDefinitionTypeDOM);
          }
          else if(DefinitionTag.DATABASE_DEFINITION.toString().equals(lDefintionTypeTag)) {
            lDefinitionBuilder = new DatabaseDefinition.Builder(pModule, lDefinitionTypeDOM);
          }
          else if(DefinitionTag.RECORD_LIST.toString().equals(lDefintionTypeTag)) {
            lDefinitionBuilder = new RecordListDefinition.Builder(lDefinitionTypeDOM);
          }
          else if (DefinitionTag.TEMPLATE.toString().equals(lDefintionTypeTag)) {
            lDefinitionBuilder = new TemplateDefinition.Builder(pModule, lDefinitionTypeDOM);
          }
          else {
            throw new ExModule("Invalid mapset definition for " + lMapSetName + " - mapset type not recognised: " + lDefintionTypeTag);
          }
        }

        return lDefinitionBuilder.build(lMapSetName, lCacheKey, lXDo, lRefreshTimeoutMins, pModule);
      }
      catch (ExModule e) {
        //Catch any ExModules to append the mapset name
        throw new ExModule("Could not construct definition for MapSet " + lMapSetName, e);
      }
    }
    finally {
      Track.pop("CreateMapSetDefinition");
    }
  }

  private static Long getRefreshTimeoutMinsOrNull(Map<String, DOM> pDefinitionTagNameMap)
  throws ExModule {
    DOM lTimeoutMinsElem = pDefinitionTagNameMap.remove(DefinitionTag.REFRESH_TIMEOUT_MINS.toString());
    DOM lRefreshElem = pDefinitionTagNameMap.remove(DefinitionTag.REFRESH.toString());

    if(lTimeoutMinsElem != null && lRefreshElem != null) {
      throw new ExModule("fm:refresh and fm:refresh-timeout-mins are mutually exclusive");
    }

    if(lTimeoutMinsElem != null) {
      long lRefreshTimeoutMins;
      try {
        lRefreshTimeoutMins = Long.parseLong(lTimeoutMinsElem.value());
      }
      catch (NumberFormatException e) {
        throw new ExModule("Invalid number specified for " + DefinitionTag.REFRESH_TIMEOUT_MINS, e);
      }
      if(lRefreshTimeoutMins < 0) {
        throw new ExModule("Illegal negative number specified for " + DefinitionTag.REFRESH_TIMEOUT_MINS);
      }

      return lRefreshTimeoutMins;
    }
    else if(lRefreshElem != null) {
      String lRefreshElemValue = lRefreshElem.value();
      if("always".equals(lRefreshElemValue)) {
        return 0L;
      }
      else if("never".equals(lRefreshElemValue)) {
        return NEVER_REFRESH_TIMEOUT_MINS;
      }
      else {
        throw new ExModule("Unrecognised value " + lRefreshElemValue + " for fm:refresh element");
      }
    }
    else {
      //Neither specified - return null (defer to the builder's default)
      return null;
    }
  }
}

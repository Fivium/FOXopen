package net.foxopen.fox.cache;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.FlushCacheBangHandler;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.StatusAction;
import net.foxopen.fox.enginestatus.StatusCategory;
import net.foxopen.fox.enginestatus.StatusCollection;
import net.foxopen.fox.enginestatus.StatusDetail;
import net.foxopen.fox.enginestatus.StatusItem;
import net.foxopen.fox.enginestatus.StatusMessage;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CacheManager {

  private static String GET_CACHE_PROP_FILENAME = "GetCacheProperties.sql";
  private static String CACHE_NAME_COL = "CACHE_NAME";
  private static final Map<String, FoxCache<?, ?>> gCacheIdToFoxCache = new TreeMap<>();
  private static final Map<String, FoxCacheBuilder> gCacheIdToFoxCacheBuilder = new HashMap<>();

  private static final Map<String, CacheBuilderFactory> gCacheNameToBuilderFactory = new HashMap<>();
  static {
    populateDefaultBuilderFactories();
  }

  static {
    EngineStatus.instance().registerStatusProvider(new StatusProvider() {
      @Override
      public void refreshStatus(StatusCategory pCategory) {
        cacheStatsTable(pCategory);
      }

      @Override
      public String getCategoryTitle() {
        return "Caches";
      }

      @Override
      public String getCategoryMnemonic() {
        return "cache";
      }

      @Override
      public boolean isCategoryExpandedByDefault() {
        return false;
      }
    });
  }

  public static void reloadFoxCaches() throws ExFoxConfiguration {
    gCacheIdToFoxCache.clear();
    gCacheIdToFoxCacheBuilder.clear();
    gCacheNameToBuilderFactory.clear();
    populateDefaultBuilderFactories();
  }

  private static void populateDefaultBuilderFactories() {
    for(BuiltInCacheDefinition lBuiltInCacheDefinition : BuiltInCacheDefinition.values()) {
      try {
        gCacheNameToBuilderFactory.put(lBuiltInCacheDefinition.getCacheName(), lBuiltInCacheDefinition.getCacheBuilderFactory());
      }
      catch (ExFoxConfiguration e) {
        throw new ExInternal("Error creating default cache definition for " + lBuiltInCacheDefinition, e);
      }
    }
  }

  //TODO PN this needs re-implementing
  public static void loadFoxCachesFromDatabase() throws ExFoxConfiguration {
    // Only called once a database connection is acquired. Should not need a deep copy.
    final Map<String, CacheBuilderFactory> lCacheNameToBuilder = new HashMap<>(gCacheNameToBuilderFactory);
    final Map<String, FoxCache<?, ?>> lCacheNameToFoxCache = new HashMap<>(gCacheIdToFoxCache);
    createCachesFromDatabase(lCacheNameToBuilder, lCacheNameToFoxCache);

    gCacheNameToBuilderFactory.putAll(lCacheNameToBuilder);
    gCacheIdToFoxCache.putAll(lCacheNameToFoxCache);
    gCacheIdToFoxCacheBuilder.clear();
  }

  private static String memberCacheId(String pCacheName, String pMemberIdentifier) {
    return pCacheName + "_" + pMemberIdentifier;
  }

  private static void createCachesFromDatabase(Map<String, CacheBuilderFactory> pBuilderCacheMap, Map<String, FoxCache<?, ?>> pFoxCacheMap) throws ExFoxConfiguration {
  // If there is a database connection and the engine has been configured.
    if (FoxGlobals.getInstance().getFoxBootConfig() != null) {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":p_environment_key", FoxGlobals.getInstance().getFoxBootConfig().getFoxEnvironmentKey());
      lBindMap.defineBind(":p_engine_locator", FoxGlobals.getInstance().getEngineLocator());

      ContextUCon lContextUCon = ContextUCon.createContextUCon(FoxGlobals.getInstance().getEngineConnectionPoolName(), "Get Cache ContextUCon");
      try {
        lContextUCon.pushConnection("Construct Cache Push");
        UCon lGetCacheUCon = null;
        try {
          lGetCacheUCon = lContextUCon.getUCon("Construct Cache Conn");
          ParsedStatement lParsedStatement = SQLManager.instance().getStatement(GET_CACHE_PROP_FILENAME, CacheManager.class);

          List<UConStatementResult> lCacheStatementResultList = lGetCacheUCon.queryMultipleRows(lParsedStatement, lBindMap);
          for (UConStatementResult lCacheStatementResult : lCacheStatementResultList) {
            String lCacheName = lCacheStatementResult.getString(CACHE_NAME_COL);
            try {
              // construct fox cache and put them in the maps
              final FoxCacheBuilder lFoxCacheBuilder = FoxCacheBuilder.createFoxCacheBuilder(lCacheStatementResult);
              pBuilderCacheMap.put(lCacheName, new CacheBuilderFactory() {
                @Override
                public FoxCacheBuilder createCacheBuilder()
                throws ExFoxConfiguration {
                  //TODO PN this won't work properly, needs to store the row data and create the object every time
                  return lFoxCacheBuilder;
                }
              });
              //pFoxCacheMap.put(lCacheName, lFoxCacheBuilder.buildFoxCache());
            }
            catch (ExFoxConfiguration e) {
              throw new ExFoxConfiguration("Failed to load a cache '" + lCacheName + "' with Fox environment '" + FoxGlobals.getInstance().getFoxBootConfig().getFoxEnvironmentKey()
                                           + "'  and engine locator '" + FoxGlobals.getInstance().getEngineLocator() + "'", e);
            }
          }

        }
        catch (ExDB e) {
          throw new ExFoxConfiguration("Failed to load a cache with Fox environment '" + FoxGlobals.getInstance().getFoxBootConfig().getFoxEnvironmentKey() + "'  and engine locator '" + FoxGlobals.getInstance().getEngineLocator() + "'", e);
        }
        finally {
          lContextUCon.returnUCon(lGetCacheUCon, "Construct Cache Conn");
        }
      }
      finally {
        if (lContextUCon != null) {
          lContextUCon.popConnection("Construct Cache Push");
        }
      }
    }
  }

  private static <T, U> FoxCache<T, U> getOrCreateFoxCache(String pCacheId, String pCacheName, String pMemberIdentifier) {
    FoxCache<T, U> lFoxCache = (FoxCache<T, U>) gCacheIdToFoxCache.get(pCacheId);
    if (lFoxCache != null) {
      return lFoxCache;
    }
    else {
      try {
        CacheBuilderFactory lCacheBuilderFactory = gCacheNameToBuilderFactory.get(pCacheName);
        if(lCacheBuilderFactory == null) {
          throw new ExInternal("Failed to find a cache builder for cache name " + pCacheName);
        }

        FoxCacheBuilder lFoxCacheBuilder = lCacheBuilderFactory.createCacheBuilder();
        if(!XFUtil.isNull(pMemberIdentifier)) {
          lFoxCacheBuilder.setPurpose(lFoxCacheBuilder.getPurpose() + " (" + pMemberIdentifier + ")");
        }

        lFoxCache = (FoxCache<T, U>) lFoxCacheBuilder.buildFoxCache();
        gCacheIdToFoxCache.put(pCacheId, lFoxCache);
        gCacheIdToFoxCacheBuilder.put(pCacheId, lFoxCacheBuilder);
        return lFoxCache;
      }
      catch (ExFoxConfiguration e) {
        throw new ExInternal("Failed to create FOX built in cache", e);
      }
    }
  }

  public static <T, U> FoxCache<T, U> getCache(BuiltInCacheDefinition pBuiltInCacheDefinition) {
    return getOrCreateFoxCache(pBuiltInCacheDefinition.getCacheName(), pBuiltInCacheDefinition.getCacheName(), null);
  }

  public static <T, U> FoxCache<T, U> getMemberCache(BuiltInCacheDefinition pBuiltInCacheDefinition, String pMemberIdentifier) {
    String lCacheId = memberCacheId(pBuiltInCacheDefinition.getCacheName(), pMemberIdentifier);
    return getOrCreateFoxCache(lCacheId, pBuiltInCacheDefinition.getCacheName(), pMemberIdentifier);
  }

  /**
   * Flushes (empties) the cache with the given ID.
   * @param pCacheId ID of cache to flush.
   */
  public static void flushCache(String pCacheId){
    FoxCache lCache = gCacheIdToFoxCache.get(pCacheId);
    if(lCache != null) {
      lCache.flush();
    }
    else {
      throw new ExInternal("Cache with id " + pCacheId + " not found");
    }
  }

  /**
   * Flushes a member-based cache if it exists. If the cache does not exist no action is taken.
   * @param pBuiltInCacheDefinition Cache type to be flushed.
   * @param pMemberIdentifier Cache member identifier.
   */
  public static void flushMemberCache(BuiltInCacheDefinition pBuiltInCacheDefinition, String pMemberIdentifier) {
    String lCacheId = memberCacheId(pBuiltInCacheDefinition.getCacheName(), pMemberIdentifier);
    FoxCache lCache = gCacheIdToFoxCache.get(lCacheId);
    if(lCache != null) {
      lCache.flush();
    }
  }

  private static void cacheStatsTable(StatusCategory pStatusCategory){

    StatusTable lTable = pStatusCategory.addTable("Cache Summary", "Purpose", "Implementation Class", "Current Size", "Capacity", "Hit Rate", "Hit Count", "Miss Count", "Actions");
    lTable.setRowProvider(new StatusTable.RowProvider() {
      @Override
      public void generateRows(StatusTable.RowDestination pRowDestination) {
        for (final Map.Entry<String, FoxCache<?, ?>> lCacheEntry : gCacheIdToFoxCache.entrySet()) {
          final FoxCache<?, ?> lCache = lCacheEntry.getValue();
          Map<FoxCache.Statistic, Number> lStats = lCache.getStatistics();

          StatusCollection lActions = new StatusCollection("actions");

          lActions.addItem(new StatusDetail("View contents", new StatusDetail.Provider() {
            @Override
            public StatusItem getDetailMessage() {
              StatusCollection lContents = new StatusCollection("cacheContents");
              for (Object o : lCache.entrySet()) {
                Map.Entry lEntry = (Map.Entry) o;
                lContents.addItem(new StatusMessage(lEntry.getKey().toString(), lEntry.getValue().toString()));
              }
              return lContents;
            }
          }));

          final FoxCacheBuilder lFoxCacheBuilder = gCacheIdToFoxCacheBuilder.get(lCacheEntry.getKey());
          lActions.addItem(new StatusDetail("View config", new StatusDetail.Provider() {
            @Override
            public StatusItem getDetailMessage() {
              StatusCollection lConfig = new StatusCollection("cacheConfig");
              for(Map.Entry<String, String> lEntry : lFoxCacheBuilder.getPropertyMap().entrySet()) {
                lConfig.addItem(new StatusMessage(lEntry.getKey(), lEntry.getValue()));
              }
              return lConfig;
            }
          }));
          lActions.addItem(new StatusAction("Flush", FlushCacheBangHandler.instance(), Collections.singletonMap(FlushCacheBangHandler.ID_PARAM_NAME, lCacheEntry.getKey())));

          pRowDestination.addRow(lCacheEntry.getKey())
            .setColumn(lCache.getPurpose())
            .setColumn(lCache.getClass().getSimpleName())
            .setColumn(lStats.get(FoxCache.Statistic.CURRENT_SIZE).toString())
            .setColumn(lStats.get(FoxCache.Statistic.CAPACITY).toString())
            .setColumn(lStats.get(FoxCache.Statistic.HIT_RATE).toString())
            .setColumn(lStats.get(FoxCache.Statistic.HIT_COUNT).toString())
            .setColumn(lStats.get(FoxCache.Statistic.MISS_COUNT).toString())
            .setColumn(lActions);
        }
      }
    });
  }
}

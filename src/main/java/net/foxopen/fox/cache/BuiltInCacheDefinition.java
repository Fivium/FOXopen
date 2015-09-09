package net.foxopen.fox.cache;

import net.foxopen.fox.ex.ExFoxConfiguration;

import java.util.concurrent.TimeUnit;


public enum BuiltInCacheDefinition {
  XML_WORKDOCS("XML_WORKDOC", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
      lCacheBuilder.setPurpose("XML WorkDocs");
      lCacheBuilder.setMaxCapacity(500);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  UPLOAD_INFOS("UPLOAD_INFOS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
      lCacheBuilder.setPurpose("Upload Info objects");
      lCacheBuilder.setMaxCapacity(500);
      lCacheBuilder.setTimeToLiveMs(60 * 60 * 1000);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  STATEFUL_XTHREADS("STATEFUL_XTHREADS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
      lCacheBuilder.setPurpose("XThreads");
      lCacheBuilder.setMaxCapacity(300);
      lCacheBuilder.setConcurrencyLevel(5);
      //Clean up threads after 2 days because that's when they're deleted by the PL/SQL job (by default)
      //TODO link this timeout to the PL/SQL job timeout
      lCacheBuilder.setTimeToLiveMs((int) TimeUnit.DAYS.toMillis(2));
      return lCacheBuilder;
    }
  }),
  FOX_XPATH_EVALUATORS("FOX_XPATH_EVALUATORS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
      lCacheBuilder.setPurpose("FOX XPaths");
      lCacheBuilder.setMaxCapacity(10000);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  MAPSETS("MAPSETS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
      lCacheBuilder.setPurpose("Mapsets (Global)");
      lCacheBuilder.setMaxCapacity(5000);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  AJAX_MAPSET_BINDS("AJAX_MAPSET_BINDS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
      lCacheBuilder.setPurpose("AJAX Mapset binds");
      lCacheBuilder.setMaxCapacity(100);
      lCacheBuilder.setConcurrencyLevel(5);
      lCacheBuilder.setTimeToLiveMs((int) TimeUnit.MINUTES.toMillis(10));
      return lCacheBuilder;
    }
  }),
  DATA_DEFINITIONS("DATA_DEFINITIONS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
      lCacheBuilder.setPurpose("Data Definitions");
      lCacheBuilder.setMaxCapacity(100);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  DEV_TOOLBAR_FLAGS("DEV_TOOLBAR_FLAGS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
      lCacheBuilder.setPurpose("Dev Toolbar Flags");
      lCacheBuilder.setMaxCapacity(300);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  DATABASE_SHARED_DOMS("DATABASE_SHARED_DOMS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
      lCacheBuilder.setPurpose("Database Shared DOMs");
      //4 hours
      lCacheBuilder.setMaxCapacity(500);
      lCacheBuilder.setTimeToLiveMs(4 * 60 * 60 * 1000);
      lCacheBuilder.setInitialCapacity(16);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  INTERNAL_SQL_STATEMENTS("INTERNAL_SQL_STATEMENTS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxPermanentCacheBuilder lCacheBuilder = new FoxPermanentCacheBuilder();
      lCacheBuilder.setPurpose("Internal SQL Statements");
      lCacheBuilder.setInitialCapacity(16);
      lCacheBuilder.setConcurrencyLevel(2);
      return lCacheBuilder;
    }
  }),
  RECENT_TRACKS("RECENT_TRACKS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
      lCacheBuilder.setPurpose("Recent Tracks");
      lCacheBuilder.setMaxCapacity(50);
      lCacheBuilder.setTimeToLiveMs(30 * 60 * 1000);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  APP_COMPONENTS("APP_COMPONENTS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
      lCacheBuilder.setPurpose("App Components");
      lCacheBuilder.setMaxCapacity(500);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  RECENT_TRACK_IDS_FOR_SESSION_ID("SESSION_ID_TO_TRACK_ID", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
      lCacheBuilder.setPurpose("Recent Session Track IDs");
      //1 hour
      lCacheBuilder.setTimeToLiveMs(60 * 60 * 1000);
      lCacheBuilder.setMaxCapacity(100);
      lCacheBuilder.setInitialCapacity(16);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  SESSION_COOKIES("SESSION_COOKIES", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
      lCacheBuilder.setPurpose("Session Cookies");
      lCacheBuilder.setMaxCapacity(100);
      lCacheBuilder.setTimeToLiveMs(60 * 60 * 1000);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  HOT_TRACKS("HOT_TRACKS", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
      lCacheBuilder.setPurpose("Hot Tracks");
      lCacheBuilder.setMaxCapacity(100);
      //15 mins only
      lCacheBuilder.setTimeToLiveMs(15 * 60 * 1000);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  }),
  THREAD_TEMP_RESOURCES("THREAD_TEMP_RESOURCES", new CacheBuilderFactory() {
    public FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration {
      FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
      lCacheBuilder.setPurpose("Thread Temp Resources");
      lCacheBuilder.setMaxCapacity(200);
      //15 mins only
      lCacheBuilder.setTimeToLiveMs(15 * 60 * 1000);
      lCacheBuilder.setConcurrencyLevel(5);
      return lCacheBuilder;
    }
  });

  private final String mCacheName;
  private final CacheBuilderFactory mCacheBuilderFactory;

  private BuiltInCacheDefinition(String pCacheName, CacheBuilderFactory pCacheBuilderFactory) {
    mCacheName = pCacheName;
    mCacheBuilderFactory = pCacheBuilderFactory;
  }

  public String getCacheName() {
    return mCacheName;
  }

  public CacheBuilderFactory getCacheBuilderFactory() throws ExFoxConfiguration {
    return mCacheBuilderFactory;
  }
}

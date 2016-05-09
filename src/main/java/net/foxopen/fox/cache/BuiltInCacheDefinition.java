package net.foxopen.fox.cache;

import net.foxopen.fox.ex.ExFoxConfiguration;

import java.util.concurrent.TimeUnit;


public enum BuiltInCacheDefinition {
  XML_WORKDOCS("XML_WORKDOC", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("XML WorkDocs");
    lCacheBuilder.setMaxCapacity(500);
    lCacheBuilder.setConcurrencyLevel(5);
    //TTL matches XThread cache as most data will be common to both
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.DAYS.toMillis(2));
    return lCacheBuilder;
  }),
  UPLOAD_INFOS("UPLOAD_INFOS", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("Upload Info objects");
    lCacheBuilder.setMaxCapacity(500);
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.HOURS.toMillis(1));
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  STATEFUL_XTHREADS("STATEFUL_XTHREADS", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("XThreads");
    lCacheBuilder.setMaxCapacity(300);
    lCacheBuilder.setConcurrencyLevel(5);
    //Clean up threads after 2 days because that's when they're deleted by the PL/SQL job (by default)
    //TODO link this timeout to the PL/SQL job timeout
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.DAYS.toMillis(2));
    return lCacheBuilder;
  }),
  FOX_XPATH_EVALUATORS("FOX_XPATH_EVALUATORS", () -> {
    FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
    lCacheBuilder.setPurpose("FOX XPaths");
    lCacheBuilder.setMaxCapacity(10000);
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  MAPSETS("MAPSETS", () -> {
    FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
    lCacheBuilder.setPurpose("Mapsets (Global)");
    lCacheBuilder.setMaxCapacity(5000);
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  AJAX_MAPSET_BINDS("AJAX_MAPSET_BINDS", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("AJAX Mapset binds");
    lCacheBuilder.setMaxCapacity(100);
    lCacheBuilder.setConcurrencyLevel(5);
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.MINUTES.toMillis(10));
    return lCacheBuilder;
  }),
  DATA_DEFINITIONS("DATA_DEFINITIONS", () -> {
    FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
    lCacheBuilder.setPurpose("Data Definitions");
    lCacheBuilder.setMaxCapacity(100);
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  DEV_TOOLBAR_FLAGS("DEV_TOOLBAR_FLAGS", () -> {
    FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
    lCacheBuilder.setPurpose("Dev Toolbar Flags");
    lCacheBuilder.setMaxCapacity(300);
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  DATABASE_SHARED_DOMS("DATABASE_SHARED_DOMS", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("Database Shared DOMs");
    lCacheBuilder.setMaxCapacity(500);
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.HOURS.toMillis(4));
    lCacheBuilder.setInitialCapacity(16);
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  INTERNAL_SQL_STATEMENTS("INTERNAL_SQL_STATEMENTS", () -> {
    FoxPermanentCacheBuilder lCacheBuilder = new FoxPermanentCacheBuilder();
    lCacheBuilder.setPurpose("Internal SQL Statements");
    lCacheBuilder.setInitialCapacity(16);
    lCacheBuilder.setConcurrencyLevel(2);
    return lCacheBuilder;
  }),
  RECENT_TRACKS("RECENT_TRACKS", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("Recent Tracks");
    lCacheBuilder.setMaxCapacity(50);
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.MINUTES.toMillis(30));
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  APP_COMPONENTS("APP_COMPONENTS", () -> {
    FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
    lCacheBuilder.setPurpose("App Components");
    lCacheBuilder.setMaxCapacity(500);
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  RECENT_TRACK_IDS_FOR_SESSION_ID("SESSION_ID_TO_TRACK_ID", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("Recent Session Track IDs");
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.HOURS.toMillis(1));
    lCacheBuilder.setMaxCapacity(100);
    lCacheBuilder.setInitialCapacity(16);
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  SESSION_COOKIES("SESSION_COOKIES", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("Session Cookies");
    lCacheBuilder.setMaxCapacity(100);
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.HOURS.toMillis(1));
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  HOT_TRACKS("HOT_TRACKS", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("Hot Tracks");
    lCacheBuilder.setMaxCapacity(100);
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.HOURS.toMillis(12));
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  THREAD_TEMP_RESOURCES("THREAD_TEMP_RESOURCES", () -> {
    FoxTTLCacheBuilder lCacheBuilder = new FoxTTLCacheBuilder();
    lCacheBuilder.setPurpose("Thread Temp Resources");
    lCacheBuilder.setMaxCapacity(200);
    lCacheBuilder.setTimeToLiveMs((int) TimeUnit.MINUTES.toMillis(15));
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  }),
  SAML_SIGNING_CERTS("SAML_SIGNING_CERTS", () -> {
    FoxLRUCacheBuilder lCacheBuilder = new FoxLRUCacheBuilder();
    lCacheBuilder.setPurpose("SAML Signing Digital Certificates");
    lCacheBuilder.setMaxCapacity(10);
    lCacheBuilder.setConcurrencyLevel(5);
    return lCacheBuilder;
  });

  private final String mCacheName;
  private final CacheBuilderFactory mCacheBuilderFactory;

  BuiltInCacheDefinition(String pCacheName, CacheBuilderFactory pCacheBuilderFactory) {
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

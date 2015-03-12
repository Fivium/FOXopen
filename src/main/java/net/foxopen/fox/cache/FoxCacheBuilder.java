package net.foxopen.fox.cache;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;

import java.util.Map;
import java.util.TreeMap;

public abstract class FoxCacheBuilder {
  private static final String CACHE_NAME_COL = "CACHE_NAME";
  private static final String CACHE_TYPE_COL = "CACHE_TYPE";
  private static final String PURPOSE_COL = "PURPOSE_COL";
  private static final String INITIAL_CAPACITY_COL = "INITIAL_CAPACITY";
  private static final String CONCURRENCY_LEVEL_COL = "CONCURRENCY_LEVEL";

  protected String mPurpose;
  private Integer mInitialCapacity;
  private Integer mConcurrencyLevel;

  public FoxCacheBuilder() {
  }

  public FoxCacheBuilder(UConStatementResult pUConStatementResult) throws ExFoxConfiguration {
    String lCacheName = pUConStatementResult.getString(CACHE_NAME_COL);
    if (XFUtil.isNull(lCacheName)) {
      throw new ExFoxConfiguration("A cache name was acquired from the database that was null.");
    }
    // validate this cache exists
    BuiltInCacheDefinition.valueOf(lCacheName);
    mPurpose = lCacheName;
    mInitialCapacity = pUConStatementResult.getInteger(INITIAL_CAPACITY_COL);
    mConcurrencyLevel = pUConStatementResult.getInteger(CONCURRENCY_LEVEL_COL);
  }

  public static FoxCacheBuilder createFoxCacheBuilder(UConStatementResult pUConStatementResult) throws ExFoxConfiguration {
    String lCacheTypeString = pUConStatementResult.getString(CACHE_TYPE_COL);
    if (XFUtil.isNull(lCacheTypeString)) {
      throw new ExFoxConfiguration(lCacheTypeString);
    }

    CacheType lCacheType = CacheType.valueOf(lCacheTypeString);

    //TODO PN -  needs reimplementing
    FoxCacheBuilder lFoxCacheBuilder = null;
    switch (lCacheType) {
//      case FOX_LRU_CACHE:
//        lFoxCacheBuilder = new FoxLRUCacheBuilder(pUConStatementResult);
//        break;
//      case FOX_TTL_CACHE:
//        lFoxCacheBuilder = new FoxTTLCacheBuilder(pUConStatementResult);
//        break;
//      case FOX_PERMANENT_CACHE:
//        lFoxCacheBuilder = new FoxPermanentCacheBuilder(pUConStatementResult);
//        break;
//      case FOX_WEAK_VALUE_CACHE:
//        lFoxCacheBuilder = new FoxWeakValueCacheBuilder(pUConStatementResult);
//        break;
      default:
        throw new ExInternal("Unknown cache type " + lCacheType);
    }

//    return lFoxCacheBuilder;
  }

  protected void validate()
  throws ExFoxConfiguration {
    checkNotNull(mPurpose, "A purpose provided in a fox cache builder was null.");
    checkNotNull(mInitialCapacity, "A initial capacity provided in a fox cache builder was null.");
    checkNotNull(mConcurrencyLevel, "A concurrency level provided in a fox cache builder was null.");
  }

  FoxCache<?, ?> buildFoxCache() throws ExFoxConfiguration {
    loadDefaults();
    validate();
    return buildFoxCacheInternal();
  }

  protected abstract FoxCache<?, ?> buildFoxCacheInternal() throws ExFoxConfiguration;

  protected abstract void loadDefaults() throws ExFoxConfiguration;

  protected static void checkNotNull(Object pObject, String pErrorMessage) throws ExFoxConfiguration {
    if (XFUtil.isNull(pObject)) {
      throw new ExFoxConfiguration(pErrorMessage);
    }
  }

  public FoxCacheBuilder setPurpose(String pPurpose) {
    this.mPurpose = pPurpose;
    return this;
  }

  public String getPurpose() {
    return mPurpose;
  }

  public FoxCacheBuilder setInitialCapacity(Integer pInitialCapacity) {
    this.mInitialCapacity = pInitialCapacity;
    return this;
  }

  public Integer getInitialCapacity() {
    return mInitialCapacity;
  }

  public FoxCacheBuilder setConcurrencyLevel(Integer pConcurrencyLevel) {
    this.mConcurrencyLevel = pConcurrencyLevel;
    return this;
  }

  public Integer getConcurrencyLevel() {
    return mConcurrencyLevel;
  }

  /**
   * Gets a human-readable set of the values in this cache builder.
   * @return
   */
  public Map<String, String> getPropertyMap() {
    Map<String, String> lProps = new TreeMap<>();
    lProps.put("Purpose", mPurpose);
    lProps.put("Initial capacity", Integer.toString(mInitialCapacity));
    lProps.put("Concurrency level", Integer.toString(mConcurrencyLevel));
    return lProps;
  }
}

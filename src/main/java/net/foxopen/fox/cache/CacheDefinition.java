package net.foxopen.fox.cache;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.ex.ExFoxConfiguration;


public class CacheDefinition {
  private static final String PURPOSE_COL = "PURPOSE";
  private static final String CACHE_TYPE_COL = "CACHE_TYPE";
  private static final String ENVIRONMENT_KEY_COL = "ENVIRONMENT_KEY";
  private static final String ENGINE_LOCATOR_COL = "ENGINE_LOCATOR";
  private static final String INITIAL_CAPACITY_COL = "INITIAL_CAPACITY";
  private static final String MAX_CAPACITY_COL = "MAX_CAPACITY";
  private static final String REQUIRES_CONCURRENCY_COL = "REQUIRES_CONCURRENCY";
  private static final String USE_WEAK_VALUES_COL = "USE_WEAK_VALUES";
  private static final String CONCURRENCY_LEVEL_COL = "CONCURRENCY_LEVEL";
  private static final String TIME_TO_LIVE_MS_COL = "TIME_TO_LIVE_MS";

  private final String mPurpose;
  private final String mCacheType;
  private final String mEnvironmentKey;
  private final String mEngineLocator;
  private final Integer mInitialCapacity;
  private final Integer mMaxCapacity;
  private final Boolean mRequiresConcurrency;
  private final Boolean mUseWeakValues;
  private final Integer mConcurrencyLevel;
  private final Integer mTimeToLiveMs;

  public static CacheDefinition createCacheTypeDefinition(UConStatementResult pUConStatementResult) throws ExFoxConfiguration {
    return new CacheDefinition(pUConStatementResult);
  }

  private CacheDefinition(UConStatementResult pUConStatementResult) throws ExFoxConfiguration {
    mPurpose = pUConStatementResult.getString(PURPOSE_COL);

    if (XFUtil.isNull(mPurpose)) {
      throw new ExFoxConfiguration("An error occured trying to create a fox cache config object");
    }

    mCacheType = pUConStatementResult.getString(CACHE_TYPE_COL);
    mEnvironmentKey = pUConStatementResult.getString(ENVIRONMENT_KEY_COL);
    mEngineLocator = pUConStatementResult.getString(ENGINE_LOCATOR_COL);
    mInitialCapacity = pUConStatementResult.getInteger(INITIAL_CAPACITY_COL);
    mMaxCapacity = pUConStatementResult.getInteger(MAX_CAPACITY_COL);

    String lRequiresConcurrency = pUConStatementResult.getString(REQUIRES_CONCURRENCY_COL);
    if (!XFUtil.isNull(lRequiresConcurrency)) {
      if ("true".equals(lRequiresConcurrency.toLowerCase())) {
        mRequiresConcurrency = true;
      }
      else {
        mRequiresConcurrency = false;
      }
    }
    else {
      mRequiresConcurrency = null;
    }

    String lUseWeakValues = pUConStatementResult.getString(USE_WEAK_VALUES_COL);
    if (!XFUtil.isNull(lUseWeakValues)) {
      if ("true".equals(lUseWeakValues.toLowerCase())) {
        mUseWeakValues = true;
      }
      else {
        mUseWeakValues = false;
      }
    }
    else {
      mUseWeakValues = null;
    }

    mConcurrencyLevel = pUConStatementResult.getInteger(CONCURRENCY_LEVEL_COL);
    mTimeToLiveMs = pUConStatementResult.getInteger(TIME_TO_LIVE_MS_COL);
  }

  public String getPurpose() {
    return mPurpose;
  }

  public String getCacheType() {
    return mCacheType;
  }

  public String getEnvironmentKey() {
    return mEnvironmentKey;
  }

  public String getEngineLocator() {
    return mEngineLocator;
  }

  public Integer getInitialCapacity() {
    return mInitialCapacity;
  }

  public Integer getMaxCapacity() {
    return mMaxCapacity;
  }

  public Boolean getRequiresConcurrency() {
    return mRequiresConcurrency;
  }

  public Boolean getUseWeakValues() {
    return mUseWeakValues;
  }

  public Integer getConcurrencyLevel() {
    return mConcurrencyLevel;
  }

  public Integer getTimeToLiveMs() {
    return mTimeToLiveMs;
  }
}

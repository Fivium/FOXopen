package net.foxopen.fox.cache;

import net.foxopen.fox.ex.ExFoxConfiguration;

import java.util.Map;


public class FoxTTLCacheBuilder
extends FoxLRUCacheBuilder {

  private Integer mTimeToLiveMs;

  public FoxTTLCacheBuilder() throws ExFoxConfiguration {
    super();
  }

  public void setTimeToLiveMs(Integer pTimeToLiveMs) {
    this.mTimeToLiveMs = pTimeToLiveMs;
  }

  public Integer getTimeToLiveMs() {
    return mTimeToLiveMs;
  }

  @Override
  protected FoxCache buildFoxCacheInternal()
  throws ExFoxConfiguration {
    return new FoxTTLCache(mPurpose, getInitialCapacity(), getMaxCapacity(), mTimeToLiveMs, isUseWeakValues(), getConcurrencyLevel());
  }

  @Override
  protected void validate()
  throws ExFoxConfiguration {
    super.validate();
    checkNotNull(mTimeToLiveMs, "A time to live provided to a TTL fox cache builder was null.");
  }

  @Override
  public Map<String, String> getPropertyMap() {
    Map<String, String> lProps = super.getPropertyMap();
    lProps.put("Time to live MS", Integer.toString(mTimeToLiveMs));
    return lProps;
  }
}

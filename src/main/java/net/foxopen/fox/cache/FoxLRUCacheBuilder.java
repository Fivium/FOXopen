package net.foxopen.fox.cache;

import net.foxopen.fox.ex.ExFoxConfiguration;

import java.util.Map;


public class FoxLRUCacheBuilder
extends FoxCacheBuilder {

  private Integer mMaxCapacity;
  private Boolean mUseWeakValues;

  public FoxLRUCacheBuilder() {
    super();
  }

  public Integer getMaxCapacity() {
    return mMaxCapacity;
  }

  public Boolean isUseWeakValues() {
    return mUseWeakValues;
  }

  public void setMaxCapacity(Integer pMaxCapacity) {
    this.mMaxCapacity = pMaxCapacity;
  }

  public void setUseWeakValues(Boolean pUseWeakValues) {
    this.mUseWeakValues = pUseWeakValues;
  }

  @Override
  protected void validate()
  throws ExFoxConfiguration {
    super.validate();

    checkNotNull(mUseWeakValues, "A use weak values provided to a TTL fox cache builder was null");
    checkNotNull(mMaxCapacity, "A max capacity provided to a TTL fox cache builder was null");

    if (getInitialCapacity() > mMaxCapacity) {
      throw new ExFoxConfiguration("Tried to create a cache with a initial capacity bigger than the max capacity, cache purpose " + mPurpose);
    }
  }

  @Override
  protected FoxCache buildFoxCacheInternal() throws ExFoxConfiguration {
    return new FoxLRUCache<>(mPurpose, getInitialCapacity(), mMaxCapacity, mUseWeakValues, getConcurrencyLevel());
  }

  @Override
  public void loadDefaults() throws ExFoxConfiguration {

    if (mUseWeakValues == null) {
      mUseWeakValues = false;
    }

    if (getInitialCapacity() == null) {
      setInitialCapacity(mMaxCapacity / 4);
    }
  }

  @Override
  public Map<String, String> getPropertyMap() {
    Map<String, String> lProps = super.getPropertyMap();
    lProps.put("Max capacity", Integer.toString(mMaxCapacity));
    lProps.put("Weak values", Boolean.toString(mUseWeakValues));
    return lProps;
  }
}

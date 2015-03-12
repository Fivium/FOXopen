package net.foxopen.fox.cache;

import net.foxopen.fox.ex.ExFoxConfiguration;

public class FoxWeakValueCacheBuilder
extends FoxCacheBuilder {

  public FoxWeakValueCacheBuilder() {
  }

  @Override
  protected FoxCache<?, ?> buildFoxCacheInternal() throws ExFoxConfiguration {
    return new FoxWeakValueCache(mPurpose, getInitialCapacity(), getConcurrencyLevel());
  }

  @Override
  public void loadDefaults() throws ExFoxConfiguration {
    // No defaults
  }
}

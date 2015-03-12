package net.foxopen.fox.cache;

import net.foxopen.fox.ex.ExFoxConfiguration;

public class FoxPermanentCacheBuilder
extends FoxCacheBuilder {

  public FoxPermanentCacheBuilder() {
  }

  @Override
  protected FoxCache buildFoxCacheInternal() throws ExFoxConfiguration {
    return new FoxPermanentCache(mPurpose, getInitialCapacity(), getConcurrencyLevel());
  }

  @Override
  public void loadDefaults() throws ExFoxConfiguration {
    // No defaults
  }
}

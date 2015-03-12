package net.foxopen.fox.cache;


public enum CacheType {
  FOX_LRU_CACHE("FOX_LRU_CACHE"),
  FOX_TTL_CACHE("FOX_TTL_CACHE"),
  FOX_PERMANENT_CACHE("FOX_PERMANENT_CACHE"),
  FOX_WEAK_VALUE_CACHE("FOX_WEAK_VALUE_CACHE");

  private final String mCacheTypeName;

  private CacheType(String pCacheTypeName) {
    mCacheTypeName = pCacheTypeName;
  }

  public String getCacheTypeName() {
    return mCacheTypeName;
  }
}

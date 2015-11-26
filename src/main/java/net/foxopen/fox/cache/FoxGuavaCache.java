package net.foxopen.fox.cache;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.collect.HashMultiset;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The <tt>FoxGuavaCache</tt> provides a bridge between a Google Guava Cache object and the FoxCache interface.
 * Guava Caches are all of the same type but have different configuration options. Implementors of this abstract class
 * are effectively implementing different forms of Guava configuration.<br/><br/>
 *
 * All Guava caches are thread-safe. However on construction a concurrency level is required - this is the estimate of
 * the amount of threads which will be attempting simultaneous write access and is used to decide how the data is
 * partitioned. Some care must be taken here to ensure optimal performance.
 *
 * @param <K> Key.
 * @param <V> Value.
 * @see <a href="https://code.google.com/p/guava-libraries/wiki/CachesExplained">Guava Documentation</a>
 */
public abstract class FoxGuavaCache<K, V>
extends FoxCache<K, V> {

  protected Cache<K, V> mCache;
  protected int mCapacity = 0;

  /**
   * Shared functionality for constructing a Guava CacheBuilder in a uniform way.
   * @param pInitialCapacity
   * @param pMaxCapacity
   * @param pUseWeakValues
   * @param pConcurrencyLevel
   * @return Cache builder ready to create a new Guava cache.
   */
  protected static CacheBuilder getBuilder(int pInitialCapacity, int pMaxCapacity, boolean pUseWeakValues, int pConcurrencyLevel){
    CacheBuilder lBuilder = CacheBuilder.newBuilder();

    if(pMaxCapacity != -1){
      lBuilder.initialCapacity(pInitialCapacity);
      lBuilder.maximumSize(pMaxCapacity);
    }

    lBuilder.recordStats();
    lBuilder.concurrencyLevel(pConcurrencyLevel);

    if(pUseWeakValues){
      lBuilder.weakValues();
    }

    return lBuilder;
  }

  FoxGuavaCache(String pPurpose){
    super(pPurpose);
  }

  public V get(Object pKey) {
    return mCache.getIfPresent(pKey);
  }

  public V put(K pKey, V pValue) {
    mCache.put(pKey, pValue);
    return pValue;
  }

  public void putAll(Map<? extends K, ? extends V> pMap) {
    mCache.putAll(pMap);
  }

  public int size() {
    return (int) mCache.size();
  }

  public void clear() {
    mCache.invalidateAll();
    mCache.cleanUp();
  }

  public Collection<V> values() {
    return HashMultiset.create(mCache.asMap().values());
  }

  public Set<K> keySet() {
    return new HashSet<K>(mCache.asMap().keySet());
  }

  public void cleanUp() {
    mCache.cleanUp();
  }

  public void flush() {
    mCache.invalidateAll();
  }

  public V remove(Object pKey) {
    V lObject = get(pKey);
    mCache.invalidate(pKey);
    return lObject;
  }

  public boolean isEmpty() {
    return mCache.asMap().isEmpty();
  }

  public boolean containsKey(Object pKey) {
    return mCache.asMap().containsKey(pKey);
  }

  public boolean containsValue(Object pValue) {
    return mCache.asMap().containsValue(pValue);
  }

  public Set<Map.Entry<K, V>> entrySet() {
    return new HashSet<>(mCache.asMap().entrySet());
  }

  public Map<FoxCache.Statistic, Number> getStatistics() {
    HashMap<FoxCache.Statistic, Number> lResult = new HashMap<FoxCache.Statistic, Number>();
    CacheStats lStats = mCache.stats();

    lResult.put(FoxCache.Statistic.CURRENT_SIZE, mCache.size());
    lResult.put(FoxCache.Statistic.CAPACITY, mCapacity);
    lResult.put(FoxCache.Statistic.HIT_RATE, lStats.hitRate());
    lResult.put(FoxCache.Statistic.HIT_COUNT, lStats.hitCount());
    lResult.put(FoxCache.Statistic.MISS_COUNT, lStats.missCount());

    return lResult;
  }
}

package net.foxopen.fox.cache;


import net.foxopen.fox.ex.ExCache;

/**
 * Implementation of a Least Recently Used cache. LRU caches evict entries which were used least recently when
 * the cache's capacity is reached. <br/><br/>
 *
 * This implementation currently uses Google Guava, and as such is subject to the behaviours detailed in the Guava
 * documentation.
 * @param <K> Key.
 * @param <V> Value.
 * @see <a href="https://code.google.com/p/guava-libraries/wiki/CachesExplained">Guava Documentation</a>
 */
public class FoxLRUCache<K, V>
extends FoxGuavaCache<K, V> {

  protected FoxLRUCache(String pPurpose){
    super(pPurpose);
  }

  /**
   * Construct a new Least Recently Used Cache.
   * @param pPurpose A descriptive name for the cache, used for statistics reporting.
   * @param pInitialCapacity The initial size of the cache.
   * @param pMaxCapacity The maximum capacity of the cache.
   * @param pUseWeakValues If true, the cache values are stored as Weak References. This prevents the cache from
   * holding otherwise unreferenced objects in memory. However it also means objects may arbitrarily disappear from
   * the cache if they are garbage collected.
   * @param pConcurrencyLevel The number of threads expected to concurrently write to the cache. Set to 1 if no
   * concurrency is required.
   */
  FoxLRUCache(String pPurpose, int pInitialCapacity, int pMaxCapacity, boolean pUseWeakValues, int pConcurrencyLevel){
    super(pPurpose);
    if(pMaxCapacity <= 0){
      throw new ExCache("pMaxCapacity must be greater than 0 for an LRU cache.");
    }
    mCapacity = pMaxCapacity;
    mCache = getBuilder(pInitialCapacity, pMaxCapacity, pUseWeakValues, pConcurrencyLevel).build();
  }
}

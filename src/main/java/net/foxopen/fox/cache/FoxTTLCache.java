package net.foxopen.fox.cache;


import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of a Time To Live cache. This TTL cache evicts entries which were last added or modified after the
 * Time To Live has expired. If a maximum capacity is specified and the capcity is reached before any entries expire,
 * a Least Recently Used eviction algorithm is used to make space in the cache.<br/><br/>
 *
 * This implementation currently uses Google Guava, and as such is subject to the behaviours detailed in the Guava
 * documentation. Of particular note is the fact that entries are not automatically removed from the cache as soon as they
 * expire, and will only be removed during get/put operations, or when {@link #cleanUp} is performed.
 * @param <K> Key.
 * @param <V> Value.
 * @see <a href="https://code.google.com/p/guava-libraries/wiki/CachesExplained">Guava Documentation</a>
 */
public class FoxTTLCache<K, V>
extends FoxLRUCache<K, V> {

  /**
   * Creates a new TTL cache.
   * @param pPurpose A descriptive name for the cache, used for statistics reporting.
   * @param pInitialCapacity The initial size of the cache.
   * @param pMaxCapacity The maximum capacity of the cache. For a TTL cache with unlimited capacity, use -1.
   * @param pTimeToLiveMS The time to live of cache items in milliseconds.
   * @param pUseWeakValues If true, the cache values are stored as Weak References. This prevents the cache from
   * holding otherwise unreferenced objects in memory. However it also means objects may arbitrarily disappear from
   * the cache if they are garbage collected.
   * @param pConcurrencyLevel The number of threads expected to concurrently write to the cache. Set to 1 if no
   * concurrency is required.
   */
  public FoxTTLCache(String pPurpose, int pInitialCapacity, int pMaxCapacity, int pTimeToLiveMS, boolean pUseWeakValues, int pConcurrencyLevel){
    super(pPurpose);
    CacheBuilder lBuilder = getBuilder(pInitialCapacity, pMaxCapacity, pUseWeakValues, pConcurrencyLevel);

    lBuilder.expireAfterWrite(pTimeToLiveMS, TimeUnit.MILLISECONDS);

    mCache = lBuilder.build();
  }
}

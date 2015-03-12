package net.foxopen.fox.cache;


/**
 * A FoxCache with unlimited capacity but weak values. Objects will be evicted from this cache when they are garbage collected.
 * Objects held in this cache should be the subject of at least one strong reference for the duration of the time for which
 * they are in use.
 *
 * @param <K> Cache key type.
 * @param <V> Cache value type.
 */
public class FoxWeakValueCache<K, V>
extends FoxGuavaCache<K, V> {

  FoxWeakValueCache(String pPurpose, int pInitialCapacity, int pConcurrencyLevel){
    super(pPurpose);
    mCache = getBuilder(pInitialCapacity, -1, true, pConcurrencyLevel).build();
  }
}

package net.foxopen.fox.cache;


/**
 * A cache which stores values permanently and has unlimited capacity.
 * @param <K>
 * @param <V>
 */
public class FoxPermanentCache<K, V>
extends FoxGuavaCache<K, V> {

  FoxPermanentCache(String pPurpose, int pInitialCapacity, int pConcurrencyLevel){
    super(pPurpose);
    mCache = getBuilder(pInitialCapacity, -1, false, pConcurrencyLevel).build();
  }
}

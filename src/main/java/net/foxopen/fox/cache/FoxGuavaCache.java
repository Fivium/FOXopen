/*

Copyright (c) 2012, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE - 
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, 
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
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
    return new HashSet<Map.Entry<K, V>>(mCache.asMap().entrySet());
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

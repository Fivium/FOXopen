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

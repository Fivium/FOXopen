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

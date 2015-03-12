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

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExCache;

import java.util.Map;


/**
 * A FoxCache is a representation of a cache within the FOX engine, and should be the standard mechanism used for all
 * caching unless there is a good reason not to use it. In Fox, a cache is effectively a Map and as such implements the
 * {@link Map} interface. <br/><br/>
 *
 * When constructed, a FoxCache registers itself for engine reporting purposes, making it simple for the engine to
 * show statistics about all its known caches.<br/><br/>
 *
 * A FoxCache implementation makes the following guarantees: <br/>
 * <ol>
 * <li>All methods are thread-safe. Therefore it is not necessary to wrap cache access in <code>synchronized</code> blocks
 * (though this may be how thread safety is achieved internally).</li>
 * <li>For all methods which return views of the cache as Collections ({@link #keySet}, {@link #values}, {@link #entrySet}),
 * the Collection returned is non-live and modifying it will not modify the underlying cache.</li>
 * <li>Null keys and values will not be stored. Attempting to insert nulls into the cache will cause an exception.</li>
 * </ol>
 *
 * @param <K> Key to cache against.
 * @param <V> Value object to cache.
 */
public abstract class FoxCache<K, V>
implements Map<K, V>, Comparable {

  /** Readable text description of the cache's purpose. */
  protected final String mPurpose;

  /**
   * Cache statistics which implementors are expected to be able to report on.
   */
  public enum Statistic {
    CURRENT_SIZE,
    CAPACITY,
    HIT_RATE,
    HIT_COUNT,
    MISS_COUNT
  }

  /**
   * Privatised default constructor to force use of the correct constructor by subclasses.
   */
  private FoxCache(){
    mPurpose = null;
  }

  /**
   * Constructs a new FoxCache with the given purpose and registers it globally.
   * @param pPurpose Description of the cache's purpose.
   */
  protected FoxCache(String pPurpose){
    if(XFUtil.isNull(pPurpose)){
      throw new ExCache("A purpose must be provided when constructing a FoxCache");
    }
    mPurpose = pPurpose;
  }

  /**
   * Gets the purpose of the cache as specified at construction time.
   * @return Cache purpose.
   */
  public String getPurpose(){
    return mPurpose;
  }

  /**
   * Gets a current set of statistics for this cache.
   * @return Cache statistics.
   */
  public abstract Map<Statistic, Number> getStatistics();

  /**
   * Performs any housekeeping the cache might require, i.e. removal of stale items, etc. Implementations may choose
   * to do nothing.
   */
  public abstract void cleanUp();

  /**
   * Empties the cache.
   */
  public abstract void flush();

  /**
   * Empties the cache and removes it from the global register. Invoke this method when the cache is no longer needed.
   */
  public void decommission(){
    clear();
  }

  //Allows proper ordering in debug page
  @Override
  public int compareTo(Object o) {
    if(!(o instanceof FoxCache)) {
      return -1;
    }
    else {
      return mPurpose.compareTo(((FoxCache) o).mPurpose);
    }
  }
}

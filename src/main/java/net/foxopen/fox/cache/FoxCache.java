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

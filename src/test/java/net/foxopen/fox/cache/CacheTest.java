package net.foxopen.fox.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.foxopen.fox.ex.ExCache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


public class CacheTest {

  private void initCache(FoxCache pCache, int pNumEntries){
    for(int i=1; i <= pNumEntries; i++){
      pCache.put(i, Integer.toString(i));
    }
  }

  @Test
  public void testFoxLRUCache() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (5 / 4), 5, false, 1);
    initCache(lCache, 5);

    assertTrue("1 is still in cache", lCache.get(1) != null);
    assertTrue("5 is still in cache", lCache.get(5) != null);

    lCache.get(2);
    lCache.get(3);
    lCache.get(4);
    lCache.get(5);

    lCache.put(6,"6");

    assertTrue("1 evicted from cache", lCache.get(1) == null);
    assertTrue("6 now in cache", lCache.get(6) != null);

    //Give 2 top precedence
    lCache.get(2);
    //This should evict 3 (the least recently used value)
    lCache.put(7,"7");

    assertTrue("3 evicted from cache", lCache.get(3) == null);
    assertEquals("Cache still has 5 items", 5, lCache.size());
    assertTrue("2 is still in cache", lCache.get(2) != null);
    assertTrue("7 now in cache", lCache.get(7) != null);
  }

  @Test
  public void testFoxLRUCache_FillsToMax() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (5000 / 4), 5000, false, 1);
    initCache(lCache, 10000);
    assertEquals("Cache is full", 5000, lCache.size());
    initCache(lCache, 5000);
    assertEquals("Cache remains full after re-add", 5000, lCache.size());
  }

  @Test
  public void testFoxLRUCache_Clear() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (5 / 4), 5, false, 1);
    initCache(lCache, 5);
    lCache.clear();
    assertEquals("Cache is emptied", 0, lCache.size());
  }

  @Test(expected = ExCache.class)
  public void testFoxLRUCache_MustHaveDefinedCapacity() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (-1 / 4), -1, false, 1);
  }

  @Test(expected = NullPointerException.class)
  public void testFoxLRUCache_RejectsNullKeys() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (5 / 4), 5, false, 1);
    lCache.put(null, "");
  }

  @Test(expected = NullPointerException.class)
  public void testFoxLRUCache_RejectsNullValues() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (5 / 4), 5, false, 1);
    lCache.put(1, null);
  }

  @Test
  public void testFoxLRUCache_KeySetIndependence() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (5 / 4), 5, false, 1);
    initCache(lCache, 5);
    Set<Integer> lSet = lCache.keySet();
    lSet.remove(1);
    assertTrue("Removal from key set does NOT affect cache", lCache.get(1) != null);
  }

  @Test
  public void testFoxLRUCache_ValueCollectionIndependence() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (5 / 4), 5, false, 1);
    initCache(lCache, 5);
    Collection<String> lCollection = lCache.values();
    lCollection.remove(lCollection.iterator().next());
    assertEquals("Removal from value collection does NOT affect cache", 5, lCache.size());
  }

  @Test
  public void testFoxLRUCache_EntrySetIndependence() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (5 / 4), 5, false, 1);
    initCache(lCache, 5);
    Set<Map.Entry<Integer, String>> lEntrySet = lCache.entrySet();
    lEntrySet.remove(lEntrySet.iterator().next());
    assertEquals("Removal from entry set does NOT affect cache", 5, lCache.size());
  }


  @Test
  public void testFoxTTLCache_UnlimitedCapacity() {
    FoxTTLCache<Integer, String> lCache = new FoxTTLCache<>("Testing Purpose", (-1 / 4), -1, 10000, false, 1);
    initCache(lCache, 5000);
    assertEquals("Cache grows infinitely", 5000, lCache.size());
  }

  @Test
  public void testFoxTTLCache_EntriesExpire() throws InterruptedException {
    FoxTTLCache<Integer, String> lCache = new FoxTTLCache<>("Testing Purpose", (-1 / 4), -1, 5, false, 1);;
    initCache(lCache, 5);
    assertEquals("All entries in cache", 5, lCache.size());

    //bit dodgy
    Thread.sleep(500);

    assertTrue("All entries expired", lCache.get(1) == null);
    assertTrue("All entries expired", lCache.get(3) == null);
    assertTrue("All entries expired", lCache.get(5) == null);
    //Querying the cache for expired entries should cause it to clean up
    assertEquals("All entries expired", 0, lCache.size());
  }


  @Test
  public void testFoxTTLCache_LRUEvictionUsedWhenCapacityReached() {
    FoxTTLCache<Integer, String> lCache = new FoxTTLCache<>("Testing Purpose", (5 / 4), 5, 100000, false, 1);;
    initCache(lCache, 5);
    assertTrue("1 is still in cache", lCache.get(1) != null);
    assertTrue("5 is still in cache", lCache.get(5) != null);

    lCache.get(2);
    lCache.get(3);
    lCache.get(4);
    lCache.get(5);

    lCache.put(6,"6");

    assertTrue("1 evicted from cache", lCache.get(1) == null);
    assertTrue("6 now in cache", lCache.get(6) != null);
  }

  @Test
  public void testFoxLRUCache_ClearAndCleanUp() {
    FoxLRUCache<Integer, String> lCache = new FoxLRUCache<>("Testing Purpose", (5 / 4), 5, false, 1);
    initCache(lCache, 5);
    lCache.clear();
    lCache.cleanUp();
    assertEquals("Cache is emptied", 0, lCache.size());
  }


}

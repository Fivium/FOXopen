package net.foxopen.fox.xhtml;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test that this simple object behaves as expected.
 */
public class QueryStringSeparatorProviderTest {

  /**
   * Validate the basic desired outcome of the object.
   */
  @Test
  public void testBasicBehaviour() {
    QueryStringSeparatorProvider lSeparatorProvider = new QueryStringSeparatorProvider();
    Assert.assertEquals("First separator provided should be a question mark", "?", lSeparatorProvider.getSeparator());
    Assert.assertEquals("Second separator provided should be an ampersand", "&", lSeparatorProvider.getSeparator());
    Assert.assertEquals("Third separator provided should be an ampersand", "&", lSeparatorProvider.getSeparator());
  }

  /**
   * Test that newly constructed objects don't interfere with the first.
   */
  @Test
  public void testMultiInstanceBehaviour1() {
    QueryStringSeparatorProvider lSeparatorProvider = new QueryStringSeparatorProvider();
    Assert.assertEquals("First separator provided should be a question mark", "?", lSeparatorProvider.getSeparator());
    new QueryStringSeparatorProvider();
    Assert.assertEquals("Second separator provided should be an ampersand", "&", lSeparatorProvider.getSeparator());
    new QueryStringSeparatorProvider();
    Assert.assertEquals("Third separator provided should be an ampersand", "&", lSeparatorProvider.getSeparator());
  }

  /**
   * Test that newly constructed objects don't interfere with the first.
   */
  @Test
  public void testMultiInstanceBehaviour2() {
    QueryStringSeparatorProvider lSeparatorProvider1 = new QueryStringSeparatorProvider();
    QueryStringSeparatorProvider lSeparatorProvider2 = new QueryStringSeparatorProvider();
    Assert.assertEquals("First separator provided should be a question mark", "?", lSeparatorProvider1.getSeparator());
    Assert.assertEquals("First separator provided should be a question mark", "?", lSeparatorProvider2.getSeparator());
    Assert.assertEquals("Second separator provided should be an ampersand", "&", lSeparatorProvider1.getSeparator());
    Assert.assertEquals("Second separator provided should be an ampersand", "&", lSeparatorProvider2.getSeparator());
    Assert.assertEquals("Third separator provided should be an ampersand", "&", lSeparatorProvider1.getSeparator());
    Assert.assertEquals("Third separator provided should be an ampersand", "&", lSeparatorProvider2.getSeparator());
  }

}

package net.foxopen.fox.plugin.api.util;

import net.foxopen.fox.StringUtil;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Collection;

public class FxpStringUtil {

  /**
   * Convenience method to return a Collection as a delimited (e.g. CSV)
   * String. Useful for toString() implementations
   *
   * @param pCollection Collection to display
   * @param pDelimiter delimiter to use (probably a ,)
   */
  public static String collectionToDelimitedString(Collection pCollection, String pDelimiter) {
   return StringUtil.collectionToDelimitedString(pCollection, pDelimiter);
  }

  /**
   * <p>Capitalizes all the whitespace separated words in a String.
   * Only the first letter of each word is changed.
   *
   * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
   * A <code>null</code> input String returns <code>null</code>.
   * Capitalization uses the Unicode title case, normally equivalent to
   * upper case.</p>
   *
   * <pre>
   * WordUtils.capitalize(null)        = null
   * WordUtils.capitalize("")          = ""
   * WordUtils.capitalize("i am FINE") = "I Am FINE"
   * </pre>
   *
   * @param str  the String to capitalize, may be null
   * @return capitalized String, <code>null</code> if null String input
   */
  public static String initCap(String pString) {
    return WordUtils.capitalize(pString);
  }
}

package net.foxopen.fox;


import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import net.foxopen.fox.ex.ExInternal;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;


/**
 * String utility class
 */
public class StringUtil {
  /**
   * Checks to see if a string contains alpha numeric characters
   *
   * @param pString the string to be tested
   * @return true if the string contained any alphanumeric characters
   */
  public static boolean containsAlphanumeric(String pString) {
    boolean lContainsAlphaNumerics = false;
    for( int i = 0; i < pString.length() && !lContainsAlphaNumerics; i++ ) {
      lContainsAlphaNumerics = Character.isLetterOrDigit(pString.charAt(i));
    }

    return lContainsAlphaNumerics;
  }

  /**
   * Checks to see if a string contains control characters
   *
   * @param pString string to be tested
   * @return true if the string contained any control characters
   */
  public static boolean containsControlCharacters(String pString) {
    boolean lContainsControlChars = false;
    for( int i = 0; i < pString.length() && !lContainsControlChars; i++ ) {
      lContainsControlChars = Character.isISOControl(pString.charAt(i));
    }

    return lContainsControlChars;
  }

  /**
   * Checks to see if a string contains control characters
   *
   * @param pString string to be tested
   * @return true if the string contained any control characters
   */
  public static boolean containsWhiteSpaceCharacters(String pString) {
    boolean lContainsWhiteSpace = false;
    for( int i = 0; i < pString.length() && !lContainsWhiteSpace; i++ ) {
      lContainsWhiteSpace = Character.isWhitespace(pString.charAt(i));
    }
    return lContainsWhiteSpace;
  }

  /**
   * Removes all of the whitespace from a string
   *
   * @param pString string to be converted
   * @return a new string leaving the passed in string in tact
   */
  public static String removeWhitespace(String pString) {
    StringBuilder lReturnBuffer = new StringBuilder();
    for(int i = 0; i < pString.length(); i++ ) {
      if(!Character.isWhitespace(pString.charAt(i))) {
        lReturnBuffer.append(pString.charAt(i));
      }
    }

    return lReturnBuffer.toString();
  }

  /**
   * Similar to the Perl chomp command. Removes all leading and trailing
   * whitespace.
   *
   * @param pStringBuilder The StringBuffer object to remove leading and trailing whitespace
   * characters from.
   * @return StringBuilder the chomped string.
   */
  public static StringBuilder trim(StringBuilder pStringBuilder){
    return leftTrim(rightTrim(pStringBuilder));
  }

  /**
   * Similar to the Perl chomp command. Removes all leading and trailing
   * whitespace.
   *
   * @param pString The String object to remove leading and trailing whitespace
   * characters from.
   * @return String the chomped string.
   */
  public static String trim(String pString) {
    return trim(new StringBuilder(pString)).toString();
  }

  /**
   * Similar to the Perl chomp command. Removes all leading
   * whitespace.
   *
   * @param pStringBuilder The StringBuffer object to remove leading and trailing whitespace
   * characters from.
   * @return StringBuffer the chomped string.
   */
  public static StringBuilder leftTrim(StringBuilder pStringBuilder){
    while(true){
      if(pStringBuilder.length() != 0 && Character.isWhitespace(pStringBuilder.charAt(0))) {
        pStringBuilder.deleteCharAt(0);
      }
      else {
        break;
      }
    }

    return pStringBuilder;
  }

  /**
   * Similar to the Perl chomp command. Removes all leading
   * whitespace.
   *
   * @param pString The String object to remove leading and trailing whitespace
   * characters from.
   * @return String the chomped string.
   */
  public static String leftTrim(String pString) {
    return leftTrim(new StringBuilder(pString)).toString();
  }

  /**
   * Similar to the Oracle lpad command. Left pads a string.
   *
   * @param pStringBuilder The StringBuffer object to pad.
   * @param pLength The length to pad to.
   * @param pChar The character to pad with.
   * @return StringBuilder the chomped string.
   */
  public static StringBuilder leftPad(StringBuilder pStringBuilder, int pLength, char pChar){
    StringBuilder lTempBuffer = new StringBuilder(pLength);
    while((lTempBuffer.length()+pStringBuilder.length()) < pLength) {
      lTempBuffer.append(pChar);
    }
    lTempBuffer.append(pStringBuilder);

    return lTempBuffer;
  }

  /**
   * Similar to the Oracle lpad command. Left pads a string.
   *
   * @param pStr The String object to pad.
   * @param pLength The length to pad to.
   * @param pChar The character to pad with.
   * @return String the padded string.
   */
  public static String leftPad(String pStr, int pLength, char pChar) {
    return leftPad(new StringBuilder(pStr), pLength, pChar).toString();
  }

  /**
   * Similar to the Oracle lpad command. Left pads a string.
   *
   * @param pStr The String object to pad.
   * @param pLength The length to pad to.
   * @return String the padded string.
   */
  public static String leftPad(String pStr, int pLength) {
    return leftPad(new StringBuilder(pStr), pLength, ' ').toString();
  }

  /**
   * Similar to the Perl chomp command. Removes all trailing
   * whitespace.
   *
   * @param pStringBuilder The StringBuffer object to remove leading and trailing whitespace
   * characters from.
   * @return StringBuilder the chomped string.
   */
  public static StringBuilder rightTrim(StringBuilder pStringBuilder){
    int endPos = pStringBuilder.length() - 1;
    while(endPos != -1 && Character.isWhitespace(pStringBuilder.charAt(endPos))) {
      pStringBuilder.deleteCharAt(endPos);
    }

    return pStringBuilder;
  }

  /**
   * Similar to the Perl chomp command. Removes all trailing
   * whitespace.
   *
   * @param pString The String object to remove leading and trailing whitespace
   * characters from.
   * @return String the chomped string.
   */
  public static String rightTrim(String pString) {
    return rightTrim(new StringBuilder(pString)).toString();
  }

  /**
   * Similar to the Oracle rpad command. Right pads a string.
   *
   * @param pStringBuilder The StringBuffer object to pad.
   * @param pLength The length to pad to.
   * @param pChar The character to pad with.
   * @return StringBuffer the chomped string.
   */
  public static StringBuilder rightPad(StringBuilder pStringBuilder, int pLength, char pChar){
    while(pStringBuilder.length() < pLength){
      pStringBuilder.append(pChar);
    }

    return pStringBuilder;
  }

  /**
   * Similar to the Oracle rpad command. Right pads a string.
   *
   * @param pString The String object to pad.
   * @param pLength The length to pad to.
   * @param pChar The character to pad with.
   * @return String the padded string.
   */
  public static String rightPad(String pString, int pLength, char pChar) {
    return rightPad(new StringBuilder(pString), pLength, pChar).toString();
  }

  /**
   * Similar to the Oracle rpad command. Right pads a string.
   *
   * @param pString The String object to pad.
   * @param pLength The length to pad to.
   * @return String the padded string.
   */
  public static String rightPad(String pString, int pLength) {
    return rightPad(new StringBuilder(pString), pLength, ' ').toString();
  }

  /**
   * Replaces all occurrences of a substring within a string with another string.
   *
   * @param inString String to examine
   * @param oldPattern String to replace
   * @param newPattern String to insert
   * @return a String with the replacements
   * @deprecated
   */
  public static String replace(String inString, String oldPattern, String newPattern) {
    // Pick up error conditions
    if (inString == null) {
      return null;
    }
    if (oldPattern == null || newPattern == null) {
      return inString;
    }

    StringBuffer sbuf = new StringBuffer();   // Output StringBuffer we'll build up
    int pos = 0;                              // Our position in the old string
    int index = inString.indexOf(oldPattern); // The index of an occurrence we've found, or -1
    int patLen = oldPattern.length();
    while (index >= 0) {
      sbuf.append(inString.substring(pos, index));
      sbuf.append(newPattern);
      pos = index + patLen;
      index = inString.indexOf(oldPattern, pos);
    }
    sbuf.append(inString.substring(pos));     // Remember to append any characters to the right of a match
    return sbuf.toString();
  }

  /**
   * Lazily convert a delimited list into an iterable string using guava
   *
   * @param pString String
   * @param pDelimiter delimiter. This will not be returned
   * @return an Iterable String
   */
  public static Iterable<String> delimitedListToIterableString(String pString, String pDelimiter) {
    return Splitter.on(pDelimiter)
            .trimResults()
            .omitEmptyStrings()
            .split(pString);
  }

  /**
   * Lazily convert a CSV list into an iterable string using guava
   *
   * @param pString CSV list
   * @return an Iterable String
   */
  public static Iterable<String> commaDelimitedListToIterableString(String pString) {
    return delimitedListToIterableString(pString, ",");
  }

  /**
   * Convenience method to convert a CSV string list to a set. Note that
   * this will suppress duplicates.
   *
   * @param pString CSV String
   * @return a Set of String entries in the list
   */
  public static Set<String> commaDelimitedListToSet(String pString) {
    Set<String> lResultSet = new TreeSet<>();
    Iterable<String> lTokens = commaDelimitedListToIterableString(pString);
    for (String lToken : lTokens) {
      lResultSet.add(lToken);
    }
    return lResultSet;
  }

  /**
   * Convenience method to return a String array as a delimited (e.g. CSV)
   * String. Useful for toString() implementations
   *
   * @param pArray array to display. Elements may be of any type (toString() will be
   * called on each element).
   * @param pDelimiter delimiter to use (probably a ,)
   */
  public static String arrayToDelimitedString(Object[] pArray, String pDelimiter) {
    if (pArray == null) {
      return "null";
    }
    else {
      return Joiner.on(pDelimiter).join(pArray);
    }
  }

  /**
   * Convenience method to return a String array as a delimited (e.g. CSV)
   * String. Where the elements of the input array are null or if String elements are empty,
   * the element is not included in the string returned.
   *
   * @param pArray array to display. Elements may be of any type (toString() will be
   * called on each element).
   * @param pDelimiter delimiter to use (probably a ,)
   * @param pIncludeNullOrEmptyElements
   * @return String
   */
  public static String arrayToDelimitedString(Object[] pArray, String pDelimiter, boolean pIncludeNullOrEmptyElements) {
    if (pIncludeNullOrEmptyElements) {
      return arrayToDelimitedString(pArray, pDelimiter);
    }
    if (pArray == null) {
      return "null";
    }
    else {
      return Joiner.on(pDelimiter).skipNulls().join(pArray);
    }
  }

  /**
   * Convenience method to return a Collection as a delimited (e.g. CSV)
   * String. Useful for toString() implementations
   *
   * @param pCollection Collection to display
   * @param pDelimiter delimiter to use (probably a ,)
   */
  public static String collectionToDelimitedString(Collection pCollection, String pDelimiter) {
    if (pCollection == null) {
      return "null";
    }
    else {
      return Joiner.on(pDelimiter).join(pCollection);
    }
  }

  /**
   * Convenience method to return a List as a delimited (e.g. CSV)
   * String. Useful for toString() implementations
   *
   * @param pList List to display
   * @param pDelimiter delimiter to use (probably a ,)
   */
  public static String ListToDelimitedString(List pList, String pDelimiter) {
    if (pList == null) {
      return "null";
    }
    else {
      return Joiner.on(pDelimiter).join(pList);
    }
  }

  /**
   * Convenience method to return a Map as a delimited (e.g. CSV)
   * String. Useful for toString() implementations
   *
   * @param pMap Map to display
   * @param pDelimiter delimiter to use (probably a ,)
   */
  public static String mapToDelimitedString(Map pMap, String pDelimiter) {
    if (pMap == null) {
      return "null";
    }
    else {
      StringBuffer sb = new StringBuffer();
      int i = 0;
      Iterator itr = pMap.entrySet().iterator();
      while (itr.hasNext()) {
        if (i++ > 0) {
          sb.append(pDelimiter);
        }
        Map.Entry e = (Map.Entry)itr.next();
        sb.append(e.getKey()).append("='").append(e.getValue()).append("'");
      }
      return sb.toString();
    }
  }

  /**
   * Returns a Collection of strings of given input string.
   *
   * @param values
   * @param delimiter
   * @return Collection
   */
  public static Collection listOfValuesToCollection(String values, String delimiter) {
    StringTokenizer st = new StringTokenizer(values, delimiter, false);
    Collection tokens = new LinkedList();
    int tokenNum = st.countTokens();
    for (int i = 0; i < tokenNum; i++) {
      tokens.add(st.nextToken());
    }
    return tokens;
  }

  /**
   * Translate pSearch characters in pSource to characters in pReplacements
   *
   * @param pSource
   * @param pSearch
   * @param pReplacements
   * @return
   */
  public static String translate(String pSource, String pSearch, String pReplacements){
    if(pSource==null || pSearch==null || pReplacements==null) {
      throw new ExInternal("Null param passed to translate");
    }

    StringBuilder lBuffer = new StringBuilder(pSource);
    int lPointer;

    //Find all searched characters and replace with appropriate replacement character/null
    char lSourceChar;
    for (int i = 0; i < lBuffer.length(); ++i) {
      lSourceChar = lBuffer.charAt(i);
      if ((lPointer = pSearch.indexOf(lSourceChar)) != -1) {
        if (lPointer < pReplacements.length()) {
          lBuffer.setCharAt(i, pReplacements.charAt(lPointer));
        }
        else {
          lBuffer.deleteCharAt(i);
          i--;
        }
      }
    }

    return lBuffer.toString();
  }

  /**
   * Turn an initcapped string into a hyphenated string.<br/>
   * e.g.
   * <pre>
   *   helloWorld => hello-world
   *   HelloWorld => hello-world
   *   hello-World => hello-world
   *   fox:helloWorld => fox:hello-world
   *   ns1Fox:helloWorld => ns1Fox:hello-world (if pIgnoreNameSpace == true)
   *   ns1Fox:helloWorld => ns1-fox:hello-world (if pIgnoreNameSpace == false)
   * </pre>
   *
   * @param pValue InitCapped string to hyphenate
   * @param pIgnoreNamespace If true it will skip hyphenating any namespaces
   * @return Hyphenated version of pValue
   */
  public static String hyphenateInitCappedString(String pValue, boolean pIgnoreNamespace) {
    StringBuilder lOutputValue = new StringBuilder(pValue.length());
    char[] lChars = pValue.toCharArray();
    char lPrevious = 0;
    int lOffset = 0;

    if (pIgnoreNamespace) {
      // If ignoring namespaces, copy the un-altered namespace chars to the string buffer and set the NamepsaceOffset
      lOffset = pValue.indexOf(":") + 1;
      lOutputValue.append(lChars, 0, lOffset);
    }

    // Loop through the rest of the characters
    for (; lOffset < lChars.length; lOffset++) {
      if (Character.isUpperCase(lChars[lOffset])) {
        // If the character is upper cased it's a camelCase separation point
        if (Character.isLetterOrDigit(lPrevious)) {
          // If the previous character was a letter, we should add a new hyphen break
          lOutputValue.append('-');
        }
        // Then add the lower cased letter
        lOutputValue.append(Character.toLowerCase(lChars[lOffset]));
      }
      else {
        lOutputValue.append(lChars[lOffset]);
      }
      lPrevious = lChars[lOffset];
    }

    return lOutputValue.toString();
  }
}

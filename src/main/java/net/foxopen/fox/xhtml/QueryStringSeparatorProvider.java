package net.foxopen.fox.xhtml;

/**
 * Simple utility class that provides a query string separator, first time "?", and "&" thereafter.
 * You should create a new instance required for each URL generated.
 */
public class QueryStringSeparatorProvider {

  private static final String AMP = "&";
  private static final String QMARK = "?";

  // Tracker boolean
  private boolean mQuestionMarkUsed = false;

  /**
   * Provides a query string separator, first time "?", and "&" thereafter.
   * @return separator as a String.
   */
  public String getSeparator() {
    if (mQuestionMarkUsed) {
      return AMP;
    }
    else {
      mQuestionMarkUsed = true;
      return QMARK;
    }
  }
}

package net.foxopen.fox.database.storage.dom;

import net.foxopen.fox.ex.ExDB;

/**
 * Represents the result of an attempt to select a row for an XMLWorkDoc.
 */
class XMLWorkDocSelectResult {

  private final boolean mRowExists;
  /* The exception encountered when attempting the select - can be null */
  private final ExDB mSelectException;

  /**
   * Creates an XMLWorkDocSelectResult representing an existing row.
   * @return New XMLWorkDocSelectResult
   */
  static XMLWorkDocSelectResult rowExistsResult() {
    return new XMLWorkDocSelectResult(true, null);
  }
  /**
   * Creates an XMLWorkDocSelectResult representing no rows found
   * @param pSelectException  TooFewRows exception
   * @return New XMLWorkDocSelectResult
   */
  static XMLWorkDocSelectResult tooFewRowsResult(ExDB pSelectException) {
    return new XMLWorkDocSelectResult(false, pSelectException);
  }

  private XMLWorkDocSelectResult(boolean pRowExists, ExDB pSelectException) {
    mRowExists = pRowExists;
    mSelectException = pSelectException;
  }

  /**
   * Tests if a row was found.
   * @return True if a row was found.
   */
  public boolean rowExists() {
    return mRowExists;
  }

  /**
   * Gets the exception encountered during the select attempt, or null if rowExists is true.
   * @return ExDBTooFew exception or null.
   */
  public ExDB getSelectException() {
    return mSelectException;
  }
}

package net.foxopen.fox.thread.assertion;

import net.foxopen.fox.command.XDoResult;

/**
 * Result of an assertion command. Subclasses specialise for the different types of command.
 */
public class AssertionResult
implements XDoResult {
  protected final String mTestXPath;
  protected final String mMessage;
  protected final boolean mPassed;

  public AssertionResult(String pTestXPath, String pMessage, boolean pPassed) {
    mTestXPath = pTestXPath;
    mMessage = pMessage;
    mPassed = pPassed;
  }

  /**
   * @return True if the assertion passed, false if it failed.
   */
  public boolean assertionPassed() {
    return mPassed;
  }

  /**
   * @return The XPath used to evaluate the assertion. May be empty string if the assertion command did not run an XPath.
   */
  public String getTestXPath() {
    return mTestXPath;
  }

  /**
   * @return The type of assertion command used to create this result.
   */
  public String getAssertionType() {
    return "assert";
  }

  /**
   * @return The message from the assertion statement, specified by the module developer.
   */
  public String getFullMessage() {
    return mMessage;
  }
}

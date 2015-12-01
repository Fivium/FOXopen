package net.foxopen.fox.thread.assertion;

/**
 * Result of an fm:assert-fails command.
 */
public class AssertionFailsResult
extends AssertionResult {

  private final String mErrorMessage;

  public AssertionFailsResult(String pMessage, String pErrorMessage, boolean pPassed) {
    super("", pMessage, pPassed);
    mErrorMessage = pErrorMessage;
  }

  @Override
  public String getAssertionType() {
    return "assert-fails";
  }

  @Override
  public String getFullMessage() {
    return mMessage + " - " + (mPassed ? "failed with error: '" +  mErrorMessage + "'": "expected error but no error thrown");
  }
}

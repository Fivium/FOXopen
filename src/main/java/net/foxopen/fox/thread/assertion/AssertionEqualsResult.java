package net.foxopen.fox.thread.assertion;

/**
 * Result of an fm:assert-equals command.
 */
public class AssertionEqualsResult
extends AssertionResult {
  private final String mExpectedResult;
  private final String mActualResult;

  public AssertionEqualsResult(String pTestXPath, String pMessage, String pExpectedResult, String pActualResult, boolean pPassed) {
    super(pTestXPath, pMessage, pPassed);
    mExpectedResult = pExpectedResult;
    mActualResult = pActualResult;
  }

  @Override
  public String getAssertionType() {
    return "assert-equals";
  }

  @Override
  public String getFullMessage() {
    return mMessage + " - expected '" + mExpectedResult + "', got '" + mActualResult + "'";
  }
}

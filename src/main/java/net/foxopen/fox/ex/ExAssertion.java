package net.foxopen.fox.ex;

import net.foxopen.fox.thread.assertion.AssertionResult;


/**
 * Exceptions thrown by assertion commands.
 */
public class ExAssertion
extends ExRuntimeRoot {

  static String TYPE = "fm:assert assertion failure";

  private ExAssertion(String msg)  {
    super(msg, TYPE, null, null);
  }

  public static ExAssertion createFromAssertionResult(AssertionResult pAssertionResult) {
    return new ExAssertion("Assertion failed: " + pAssertionResult.getFullMessage());
  }
}

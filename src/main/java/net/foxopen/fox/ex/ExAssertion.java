package net.foxopen.fox.ex;

import java.util.List;

import net.foxopen.fox.command.builtin.AssertCommand.AssertionResult;


public class ExAssertion 
extends ExRuntimeRoot {
  
  static String TYPE = "fm:assert assertion failure";
  
  private ExAssertion(String msg)  {
    super(msg, TYPE, null, null);
  }  
  
  public static ExAssertion createFromAssertionResultList(List<AssertionResult> pAssertionResults) {
    StringBuilder lMessage = new StringBuilder();
    
    int lFailedCount = 0;
    for(AssertionResult lAssertionResult : pAssertionResults) {
      if(!lAssertionResult.assertionPassed()) {
        lMessage.append(lAssertionResult.getFullMessage() + "\n");
        lFailedCount++;
      }
    }
    
    String lMsgString = lFailedCount + " assertions failed:\n\n" + lMessage.toString();    
    return new ExAssertion(lMsgString);
  }
  
}
 
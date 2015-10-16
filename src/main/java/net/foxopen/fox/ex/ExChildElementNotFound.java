package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.page.ChildElementDefinition;

public class ExChildElementNotFound extends ExRuntimeRoot {
  private static final String TYPE = "Child element not found";
  private final ChildElementDefinition mChildElementDefinition;

  public ExChildElementNotFound(ChildElementDefinition pChildElementDefinition) {
    super(null, TYPE, null, null);
    mChildElementDefinition = pChildElementDefinition;
  }

  public ExChildElementNotFound(ChildElementDefinition pChildElementDefinition, String pMessage) {
    super(pMessage, TYPE, null, null);
    mChildElementDefinition = pChildElementDefinition;
  }

  public ExChildElementNotFound(ChildElementDefinition pChildElementDefinition, String pMessage, Throwable pNestedException) {
    super(pMessage, TYPE, null, pNestedException);
    mChildElementDefinition = pChildElementDefinition;
  }

  public ExChildElementNotFound(ChildElementDefinition pChildElementDefinition, String pMessage, DOM pDOM, Throwable pNestedException) {
    super(pMessage, TYPE, pDOM, pNestedException);
    mChildElementDefinition = pChildElementDefinition;
  }

  public ChildElementDefinition getChildElementDefinition() {
    return mChildElementDefinition;
  }
}

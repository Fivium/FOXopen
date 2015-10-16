package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.page.AttributeDefinition;

public class ExAttributeNotFound extends ExRuntimeRoot {
  private static final String TYPE = "Attribute not found";
  private final AttributeDefinition mAttributeDefinition;

  public ExAttributeNotFound(AttributeDefinition pAttributeDefinition) {
    super(null, TYPE, null, null);
    mAttributeDefinition = pAttributeDefinition;
  }

  public ExAttributeNotFound(AttributeDefinition pAttributeDefinition, String pMessage) {
    super(pMessage, TYPE, null, null);
    mAttributeDefinition = pAttributeDefinition;
  }

  public ExAttributeNotFound(AttributeDefinition pAttributeDefinition, String pMessage, Throwable pNestedException) {
    super(pMessage, TYPE, null, pNestedException);
    mAttributeDefinition = pAttributeDefinition;
  }

  public ExAttributeNotFound(AttributeDefinition pAttributeDefinition, String pMessage, DOM pDOM, Throwable pNestedException) {
    super(pMessage, TYPE, pDOM, pNestedException);
    mAttributeDefinition = pAttributeDefinition;
  }

  public AttributeDefinition getAttributeDefinition() {
    return mAttributeDefinition;
  }
}

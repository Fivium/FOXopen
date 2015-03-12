package net.foxopen.fox.plugin.api.ex;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.RuntimeExceptionWrapperException;

public class ExInternalPlugin 
extends RuntimeExceptionWrapperException {
  
  public ExInternalPlugin(String s, Throwable pEx) {
    super(s, pEx);
  }

  public ExInternalPlugin(String s) {
    super(s);
  }

  public ExInternalPlugin() {
    super();
  }
  
  public ExInternal toUnexpected(String pMsg) {
    return new ExInternal("Unexpected Error: "+pMsg, this);
  }
}

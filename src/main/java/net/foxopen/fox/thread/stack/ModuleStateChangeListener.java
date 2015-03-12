package net.foxopen.fox.thread.stack;

import net.foxopen.fox.thread.RequestContext;


//TODO name - doesn't necessarily refer to a module state but more generally request state
public interface ModuleStateChangeListener {
  
  public enum EventType {
    MODULE, STATE, SECURITY_SCOPE;
  }
  
  public void handleStateChange(RequestContext pRequestContext, EventType pEventType, ModuleCallStack pCallStack);
  
}

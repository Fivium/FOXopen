package net.foxopen.fox.thread;

/**
 * Listeners which respond to XThread events (e.g. start/stop request processing).
 */
public interface ThreadEventListener {

  void handleThreadEvent(ActionRequestContext pRequestContext, ThreadEventType pEventType);

}

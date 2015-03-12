package net.foxopen.fox.thread;

/**
 * Events that an XThread can fire when processing a request.
 */
public enum ThreadEventType {
  /** Called at the start of request processing, before any actions have been run or any FieldSet applied. */
  START_REQUEST_PROCESSING,
  /** Called at the end of request processing after any actions or HTML generation has taken place. */
  FINISH_REQUEST_PROCESSING;
}

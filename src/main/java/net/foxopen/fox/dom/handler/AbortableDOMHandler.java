package net.foxopen.fox.dom.handler;


/**
 * DOMHandlers which need to be aborted if an error is encountered during a churn. This could involve rolling back a transaction,
 * resetting internal flags, etc. If abort is invoked on a DOMHandler, <tt>close</tt> will not be invoked.
 */
public interface AbortableDOMHandler
extends DOMHandler {

  /**
   * Aborts the DOMHandler, providing a hook for any cleanup code to run and reset the object to a usable state.
   */
  public void abort();

}

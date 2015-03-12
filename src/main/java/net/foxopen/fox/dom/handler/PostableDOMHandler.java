package net.foxopen.fox.dom.handler;

import net.foxopen.fox.thread.ActionRequestContext;

/**
 * DOMHandlers which can be posted (i.e. written to the database) using the fm:post-dom command.
 */
public interface PostableDOMHandler
extends DOMHandler {

  /**
   * Posts the underlying DOM to the database in response to the fm:post-dom command.
   * @param pRequestContext
   */
  public void postDOM(ActionRequestContext pRequestContext);

}

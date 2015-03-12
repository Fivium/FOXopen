package net.foxopen.fox.dom.handler;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * A DOMHandler manages the retrieval and serialisation of a DOM across a transaction processing cycle. Once registered,
 * a DOMHandler will be opened at the start of action processing and closed at the end of action processing. Output generation
 * will occur when the DOMs are closed and conceptually read-only. Consumers may keep references to the DOM returned by a
 * Handler, but they must honour the result of {@link #isTransient()}.<br><br>
 *
 * A reference to a DOMHandler should only be held by one user thread, so they are thread safe.
 */
public interface DOMHandler {

  public static final int LOAD_PRECEDENCE_HIGH = 3;
  public static final int LOAD_PRECEDENCE_MEDIUM = 2;
  public static final int LOAD_PRECEDENCE_LOW = 1;

  /**
   * Opens this DOM handler, preparing for it to be potentially read from and written to if the underlying DOM allows
   * such access. This method is invoked on the handler before action processing starts. The handler is not required to
   * retrieve the DOM until {@link #getDOM} is invoked. This means handlers can implement JIT retrieval if they wish.
   * @param pRequestContext
   */
  public void open(ActionRequestContext pRequestContext);

  /**
   * Retrieves the DOM from this DOM handler. The DOM may have read or write permissions set by the handler. Consumers
   * may hold references to the DOM outside of the handler, but they must check {@link #isTransient} if they wish to
   * retain references across churns.
   * @return Latest version of the DOM this handler represents.
   */
  public DOM getDOM();

  /**
   * Closes this DOM handler at the end of action processing. The underlying DOM may still need to be accessed after its
   * handler is closed, so handlers should allow this as much as possible (i.e. do not set the DOM to no access). However
   * the DOM will not be modified after this point, so handlers should use this method to implement any serialisation logic.
   * @param pRequestContext
   */
  public void close(ActionRequestContext pRequestContext);

  /**
   * Returns true if the DOM retrieved from this Handler will expire (i.e. become stale) after close. If this returns true,
   * no references to the underlying DOM will be retained across transaction processing cycles, and {@link #getDOM} will
   * be invoked whenever the DOM is required. This is espescially important for DOMs which may expire or have shared access
   * modes.
   * @return True if references to the DOM returned by this handler should not be retained across churns.
   */
  public boolean isTransient();

  /**
   * Gets the :{context} label which should be used to reference this DOM.
   * @return
   */
  public String getContextLabel();

  /**
   * Determines the load order of this handler, to be used when multiple unloaded handlers are being searched for a node.
   * Handlers with a higher precedence will be searched first. This method should return one of the integer constants from the
   * DOMHandler interface.
   * @return A positive integer specifying load priority.
   */
  public int getLoadPrecedence();

}

package net.foxopen.fox.module.fieldset.clientaction;

import net.foxopen.fox.thread.ActionRequestContext;
import org.json.simple.JSONObject;

/**
 * An action which is enqueued on the client side and applied before the main action is run when the page is posted. In
 * future releases, ClientActions may be run asynchronously without having to post the whole form.
 */
public interface ClientAction {

  /**
   * Gets the key which uniquely idenitifies this client action. Actions submitted from the client side should use this
   * key.
   * @return
   */
  public String getActionKey();

  /**
   * Applies the result of this ClientAction to the current request (i.e. DOM manipulation, etc).
   * @param pRequestContext Current RequestContext.
   * @param pParams JSON parameters sent from the client side. May be an empty object.
   */
  public void applyAction(ActionRequestContext pRequestContext, JSONObject pParams);

}

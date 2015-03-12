package net.foxopen.fox.module.fieldset;

import java.util.List;

import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * Overarching interface for all forms of FieldInfo, stored in a FieldSet. Implementors should be able to take an
 * externally posted value and apply it to a target (either a DOM or a thread object) in some way.
 */
public interface PostedValueProcessor {

  /**
   * Applies the posted values in the given array to the thread (including possibly its current DOMs) in some way.
   * @param pPostedValues Value(s) to apply.
   * @return A list of change actions to execute, or an empty list if no change occurred or no change actions are required.
   */
  public List<ChangeActionContext> applyPostedValues(ActionRequestContext pRequestContext, String[] pPostedValues);

  /**
   * Gets the external name of this FieldInfo's HTML field.
   * @return
   */
  public String getExternalName();

}

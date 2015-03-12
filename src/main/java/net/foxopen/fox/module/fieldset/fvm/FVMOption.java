package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.dom.DOM;

/**
 * Representation of an option which can be selected in a FieldValueMapping, providing a serialisable strategy for applying
 * a posted form value to a DOM. Sets of FVMOptions are stored on a FieldSet within an FVM and are used to translate field
 * values sent from the form into their underlying mapset "data" (or equivelant for booleans/enums etc).
 *
 * The implementations of this interface are used to differentiate between DOM based options (complex content) and string
 * based options (simple content).
 */
public interface FVMOption {

  /**
   * Applies the value represented by this FVMOption to the given node. Only the value is applied; any other logic (i.e.
   * clearing existing content) should be performed externally.
   * @param pTargetNode Node to apply change to.
   */
  public void applyToNode(DOM pTargetNode);

  /**
   * Tests if the value represented by this FVMOption is equal to the value of the given target node.
   * @param pTargetNode Node to check.
   * @return True if this FVMOption is equal to the target node.
   */
  public boolean isEqualTo(DOM pTargetNode);

}

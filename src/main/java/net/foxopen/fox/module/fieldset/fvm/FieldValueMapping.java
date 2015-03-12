package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.fieldset.fieldmgr.DataFieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.List;
import java.util.Set;

/**
 * A FieldValueMapping (FVM) represents a set of fixed options for a FieldMgr, as defined in a schema by an application developer.
 * The three main subtypes are mapsets, schema enumerations and boolean values. An {@link OptionFieldMgr}s is constructed
 * with a FVM, which is used to provide the field options to a WidgetBuilder from {@link #getSelectOptions}. <br><br>
 *
 * The FVM is also referenced by the {@link net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo} created by the FieldMgr.
 * It is used here to apply posted form values back to the DOM. As such, an FVM must be serialisable and implementations
 * must not implicate a complex object graph. <br><br>
 *
 * It is the FieldInfo's responsibility to perform the translation of a posted value into an {@link FVMOption}, which is
 * in turn used to transform the DOM based on what the user selected. This cannot be done by the FVM as the FieldInfo may
 * have introduced extra options which the FVM does not know about. Typically this will be to introduce a "null" value which
 * the user can select or to augment unrecognised values into the FVM's option list.<br><br>
 *
 * The order of an FVM's FVMOption List is significant. The indices of the list are immutable once an FVM is constructed
 * and should be used to uniquely identify FVMOptions within the FVM. A numeric key is useful because it is generic across
 * all FVM types and does not leak internal information about the underlying DOM values etc to the output page. Consumers
 * should always use these indices when referring to FVM values.
 */
public abstract class FieldValueMapping {

  /** External value to use for FieldOptions which represent null. */
  public static final String NULL_VALUE = "N";

  /** Prefix for external values for FieldOptions representing unrecognised values. */
  public static final String UNRECOGNISED_PREFIX = "U_";

  /**
   * Gets the known FVMOptions enumerated by this FieldValueMapping. These will be created based on the underlying FVM
   * type, i.e. from a mapset, schema enum or boolean value markup. The index of an FVMOption in this list corresponds
   * to the result of {@link #getIndexForItem}. E.g. for a boolean FVM, the result of calling getIndexForItem() for an
   * element containing the text value "false" is 1. Therefore the FVMOption at index 1 of this list will be the FVMOption for "false".
   * @param pRequestContext Current RequestContext.
   * @param pTargetDOM Used by some FVM implentations if they are contextual to a DOM node (i.e. mapsets).
   * @return List of FVMOptions known to this FVM.
   */
  public abstract List<FVMOption> getFVMOptionList(ActionRequestContext pRequestContext, DOM pTargetDOM);

  /**
   * Looks up the given data item within this FVM and returns the corresponding index within its FVMOption List. If not
   * matching entry can be found, the special value of -1 is returned. This method should be used instead of external loops
   * around the FVMOption List, because some FVM classes may have optimised methods for looking up an item's index.
   * @param pFieldMgr The FieldMgr which owns this FVM, used if the FVM needs to look up enum information for the target node.
   * @param pItemDOM DOM containing the value to be looked up.
   * @return 0-based index of the FVMOption corresponding to the given item, or -1 if no appropriate option exists.
   */
  public abstract int getIndexForItem(DataFieldMgr pFieldMgr, DOM pItemDOM);

  /**
   * Allows a consumer to create additional FieldSelectOptions for this FieldValueMapping. This can be used to augment
   * the FVM's list of options with options from other sources, e.g. unrecognised options which the user has entered manually.
   * The default implementation returns a BasicSelectOption; subclasses may overload this to provide different FieldSelectOption
   * subtypes as they require.
   * @param pDisplayKey User facing display key for the new option.
   * @param pSelected True if the option is selected.
   * @param pIsNullEntry True if the option represents a "key-null" entry.
   * @param pExternalValue Value which this option will be represented as in a posted form.
   * @return A new FieldSelectOption, based on the FVM subclass.
   */
  public FieldSelectOption createFieldSelectOption(String pDisplayKey, boolean pSelected, boolean pIsNullEntry, String pExternalValue) {
    return new BasicSelectOption(pDisplayKey, pSelected, pIsNullEntry, pExternalValue, false);
  }

  /**
   * Gets a list of FieldSelectOptions which represent the FVMOptions this FVM contains. Consumers may need to augment this
   * list if the widget is allowed to contain additional "unrecognised" options, or a null option, etc.
   * @param pFieldMgr The FieldMgr which owns this FVM, used if the FVM needs to look up enum information for the target node.
   * @param pSelectedIndexes Indexes of the FVMOptions which are currently selected. This is used when constructing the
   *                         FieldSelectOptions to determine their initial state.
   * @return
   */
  public abstract List<FieldSelectOption> getSelectOptions(OptionFieldMgr pFieldMgr, Set<Integer> pSelectedIndexes);

}

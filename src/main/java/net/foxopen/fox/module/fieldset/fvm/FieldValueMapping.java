package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.fieldset.fieldmgr.DataFieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.List;
import java.util.Map;
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
 * An FVM should maintain a mapping of "refs" to FVMOptions. An "ref" is a reference which maps an external field value
 * (in the submitted form) to an FVMOption. The FVM should ensure that a ref mapping will be preserved between form churns.
 * For example, if a ref of "3" maps to a DOM value of "Option 3" when the form is generated, this mapping MUST also apply
 * to the inbound value of "3". Otherwise, users may select one value on the form only to have a different value applied to the DOM.
 * Refs should attempt to obfuscate their mapped value to help improve system security and minimise the risk of internal data
 * leakage. For this reason, it is recommended that a numeric ref is used unless there is a good reason not to do this.
 */
public abstract class FieldValueMapping {

  /** External value to use for FieldOptions which represent null. */
  public static final String NULL_VALUE = "__FX_N";

  /** Prefix for external values for FieldOptions representing unrecognised values. */
  public static final String UNRECOGNISED_PREFIX = "__FX_U_";

  /** Prefix for external values for FieldOptions representing free text values. */
  public static final String FREE_TEXT_PREFIX = "__FX_FT_";

  /**
   * Gets the FVMOption for the given ref of this FieldValueMapping. These will be created based on the underlying FVM
   * type, i.e. from a mapset, schema enum or boolean value markup. The ref of an FVMOption corresponds to the result of
   * {@link #getFVMOptionRefForItem}. E.g. for a boolean FVM, the result of calling getFVMOptionRefForItem() for an element
   * containing the text value "false" is "1". Therefore the FVMOption for ref "1" will be the FVMOption for "false".<br><br>
   *
   * <b>Important:</b> this method assumes the ref is valid for this FVM. Passing an invalid ref to this method will throw an exception.
   *
   * @param pRequestContext Current RequestContext.
   * @param pTargetDOM Used by some FVM implementations if they are contextual to a DOM node (i.e. mapsets).
   * @param pRef Ref of FVMOption to retrieve.
   * @return List of FVMOptions known to this FVM.
   */
  public abstract FVMOption getFVMOptionForRef(ActionRequestContext pRequestContext, DOM pTargetDOM, String pRef);

  /**
   * Looks up the given data item within this FVM and returns its corresponding FVMOption ref. If no matching entry can be
   * found, this method returns null (not empty string, which should never be returned).
   *
   * @param pFieldMgr The FieldMgr which owns this FVM, used if the FVM needs to look up enum information for the target node.
   * @param pItemDOM DOM containing the value to be looked up.
   * @return Ref of the FVMOption corresponding to the given item for this FVM, or null if no appropriate option exists.
   */
  public abstract String getFVMOptionRefForItem(DataFieldMgr pFieldMgr, DOM pItemDOM);

  /**
   * Allows a consumer to create additional FieldSelectOptions for this FieldValueMapping. This can be used to augment
   * the FVM's list of options with options from other sources, e.g. unrecognised options which the user has entered manually.
   * The default implementation returns a BasicSelectOption; subclasses may overload this to provide different FieldSelectOption
   * subtypes as they require.
   * @param pDisplayKey User facing display key for the new option.
   * @param pSelected True if the option is selected.
   * @param pNullOptionType Type of null option (i.e. key-missing/key-null) this represents, if any. NOT_NULL indicates that this is a "real" option.
   * @param pExternalValue Value which this option will be represented as in a posted form.
   * @return A new FieldSelectOption, based on the FVM subclass.
   */
  public FieldSelectOption createFieldSelectOption(String pDisplayKey, boolean pSelected, NullOptionType pNullOptionType, String pExternalValue) {
    return new BasicSelectOption(pDisplayKey, pSelected, pNullOptionType, pExternalValue, false);
  }

  /**
   * Allows a consumer to create additional FieldSelectOptions for this FieldValueMapping. This can be used to augment
   * the FVM's list of options with options from other sources, e.g. unrecognised options which the user has entered manually.
   * The default implementation returns a BasicSelectOption; subclasses may overload this to provide different FieldSelectOption
   * subtypes as they require.
   * @param pDisplayKey User facing display key for the new option.
   * @param pSelected True if the option is selected.
   * @param pNullOptionType Type of null option (i.e. key-missing/key-null) this represents, if any. NOT_NULL indicates that this is a "real" option.
   * @param pExternalValue Value which this option will be represented as in a posted form.
   * @param pAdditionalProperties Map of additional properties to add to the BasicSelectOption
   * @return A new FieldSelectOption, based on the FVM subclass.
   */
  public FieldSelectOption createFieldSelectOption(String pDisplayKey, boolean pSelected, NullOptionType pNullOptionType, String pExternalValue, Map<String, String> pAdditionalProperties) {
    return new BasicSelectOption(pDisplayKey, pSelected, pNullOptionType, pExternalValue, false, pAdditionalProperties);
  }

  /**
   * Gets a list of FieldSelectOptions which represent the FVMOptions this FVM contains. Consumers may need to augment this
   * list if the widget is allowed to contain additional "unrecognised" options, or a null option, etc.
   * @param pFieldMgr The FieldMgr which owns this FVM, used if the FVM needs to look up enum information for the target node.
   * @param pSelectedRefs Refs of the FVMOptions which are currently selected. This is used when constructing the FieldSelectOptions
   *                      to determine their initial state.
   * @return List of FieldSelectOptions, instantiated based on the given selected refs.
   */
  public abstract List<FieldSelectOption> getSelectOptions(OptionFieldMgr pFieldMgr, Set<String> pSelectedRefs);

}

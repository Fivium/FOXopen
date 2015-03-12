package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.fieldset.fieldmgr.DataFieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * FVM for a boolean value. These are declared in a schema with a type of <tt>xs:boolean</tt> and the module developer is able to
 * customise the display values using <tt>key-true</tt> and <tt>key-false</tt> attributes. There is only a requirement for 2 singleton
 * instantiations of this class as boolean FVMs can never be stateful. The display information is contextual to the target
 * node, so this is generated just in time when required. <br><br>
 *
 * The "strict" variant of a BooleanFVM does not allow the boolean to be set to null. It achieves this by only presenting
 * "true" as an option - its owning FieldInfo should handle implicitly converting the absence of a true value to "false".
 * This functionality is used by the tickbox widget.
 */
public class BooleanFVM
extends FieldValueMapping {

  private final boolean mStrictFlag;

  private static final BooleanFVM STRICT_INSTANCE = new BooleanFVM(true);
  private static final BooleanFVM NON_STRICT_INSTANCE = new BooleanFVM(false);

  public static final String TRUE_VALUE = "0";
  public static final String FALSE_VALUE = "1";

  private static final int TRUE_INDEX = 0;
  private static final int FALSE_INDEX = 1;

  public static final String TRUE_STRING = "true";
  public static final String FALSE_STRING = "false";

  private static final List<FVMOption> BOOLEAN_FVM_OPTION_LIST = Collections.unmodifiableList(Arrays.<FVMOption>asList(new StringFVMOption(TRUE_STRING), new StringFVMOption(FALSE_STRING)));

  public static BooleanFVM getInstance(boolean pStrictBoolean) {
    return pStrictBoolean ? STRICT_INSTANCE : NON_STRICT_INSTANCE;
  }

  private BooleanFVM(boolean pStrictFlag) {
    mStrictFlag = pStrictFlag;
  }

  @Override
  public List<FieldSelectOption> getSelectOptions(OptionFieldMgr pFieldMgr, Set<Integer> pSelectedIndexes) {

    if(pSelectedIndexes.size() > 1) {
      throw new ExInternal("Cannot have more than one selected index for a boolean FVM, got " + pSelectedIndexes.size());
    }

    int lSelectedIndex = pSelectedIndexes.iterator().next();

    EvaluatedNodeInfo lEvaluatedNodeInfo = pFieldMgr.getEvaluatedNodeInfoItem();

    //Create an option list based on the schema markup for key-true and key-false attrs
    List<FieldSelectOption> lOptionList = new ArrayList<>();
    lOptionList.add(new BasicSelectOption(lEvaluatedNodeInfo.getStringAttribute(NodeAttribute.KEY_TRUE, "Yes"), lSelectedIndex == TRUE_INDEX, pFieldMgr.getExternalValueForOption(TRUE_INDEX)));
    if(!mStrictFlag) {
      lOptionList.add(new BasicSelectOption(lEvaluatedNodeInfo.getStringAttribute(NodeAttribute.KEY_FALSE, "No"), lSelectedIndex == FALSE_INDEX, pFieldMgr.getExternalValueForOption(FALSE_INDEX)));
    }

    return lOptionList;
  }

  @Override
  public List<FVMOption> getFVMOptionList(ActionRequestContext pRequestContext, DOM pItemDOM) {
    return BOOLEAN_FVM_OPTION_LIST;
  }

  @Override
  public int getIndexForItem(DataFieldMgr pFieldMgr, DOM pItemDOM) {
    String lStringValue = pItemDOM.value().trim();
    switch (lStringValue) {
      case TRUE_STRING:
        return TRUE_INDEX;
      case FALSE_STRING:
        return FALSE_INDEX;
      default:
        return -1;
    }
  }
}

package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.fieldset.fieldmgr.DataFieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FVM for a string enumeration declared in a schema within an <tt>xs:enumeration</tt>. This FVM implementation provides
 * a straightforward mapping of an enum option to a text value within the DOM. As a schema enumeration is considered
 * immutable after it is defined in a module, only the path to the corresponding NodeInfo needs to be serialised into the
 * FieldSet. This can then be used to resolve the schema enumeration for the application of posted values.<br><br>
 *
 * Note that if a module developer modifies a schema enumeration within a module and then posts a page containing a widget
 * based on the enum, they may observe unexpected behaviour as the option refs will have changed.<br><br>
 *
 * Note: FVMOption refs for this class are integer values corresponding to the 0-based index of the option within the schema.
 */
public class SchemaEnumFVM
extends FieldValueMapping {

  /** Absolute path to the schema node which defines the enumeration for this FVM. */
  private final String mNodeInfoPath;

  public static SchemaEnumFVM createSchemaEnumFVM(EvaluatedNodeInfo pEvaluatedNodeInfo) {
    String lNodeInfoPath;
    //For multi-select nodes, get the enum definition from the "selector" target node info
    if(pEvaluatedNodeInfo.isMultiSelect()) {
      lNodeInfoPath = pEvaluatedNodeInfo.getSelectorNodeInfo().getAbsolutePath();
    }
    else {
      lNodeInfoPath = ".";
    }
    return new SchemaEnumFVM(lNodeInfoPath);
  }

  public SchemaEnumFVM(String pNodeInfoPath) {
    mNodeInfoPath = pNodeInfoPath;
  }

  @Override
  public List<FieldSelectOption> getSelectOptions(OptionFieldMgr pFieldMgr, Set<String> pSelectedRefs) {

    List<String> lSchemaEnumeration = pFieldMgr.getEvaluatedNodeInfoItem().getSchemaEnumeration();
    List<FieldSelectOption> lOptionList = new ArrayList<>(lSchemaEnumeration.size());

    int i = 0;
    for(String lEnumOption : lSchemaEnumeration) {
      String lIdxString = Integer.toString(i);
      lOptionList.add(new BasicSelectOption(lEnumOption, pSelectedRefs.contains(lIdxString), pFieldMgr.getExternalValueForOptionRef(lIdxString)));
      i++;
    }

    return lOptionList;
  }

  @Override
  public String getFVMOptionRefForItem(DataFieldMgr pFieldMgr, DOM pItemDOM) {

    String lCurrentValue = pItemDOM.value().trim();

    int i = 0;
    for(String lEnumOption : pFieldMgr.getEvaluatedNodeInfoItem().getSchemaEnumeration()) {
      if(lEnumOption.equals(lCurrentValue)) {
        return Integer.toString(i);
      }
      i++;
    }
    return null;
  }

  public FVMOption getFVMOptionForRef(ActionRequestContext pRequestContext, DOM pTargetDOM, String pRef) {

    NodeInfo lNodeInfo;
    if(".".equals(mNodeInfoPath)) {
      lNodeInfo = pRequestContext.getCurrentModule().getNodeInfo(pTargetDOM);
    }
    else {
      lNodeInfo = pRequestContext.getCurrentModule().getNodeInfo(mNodeInfoPath);
    }

    int lIntIndex = Integer.valueOf(pRef);

    //TODO PN - module parser should return a list of StringFVMOptions; this code should just use the list index
    int i=0;
    for(String lEnumValue : lNodeInfo.getSchemaEnumerationOrNull()) {
      if(i++ == lIntIndex) {
        return new StringFVMOption(lEnumValue);
      }
    }

    //Something went wrong - the requested index was not found
    throw new ExInternal("Failed to find a schema enum value for index " + lIntIndex + " on node " + mNodeInfoPath);
  }
}

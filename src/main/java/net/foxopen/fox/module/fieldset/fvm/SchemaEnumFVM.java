package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.dom.DOM;
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
 * based on the enum, they may observe unexpected behaviour as the option indexes will have changed.
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
  public List<FieldSelectOption> getSelectOptions(OptionFieldMgr pFieldMgr, Set<Integer> pSelectedIndexes) {

    List<String> lSchemaEnumeration = pFieldMgr.getEvaluatedNodeInfoItem().getSchemaEnumeration();
    List<FieldSelectOption> lOptionList = new ArrayList<>(lSchemaEnumeration.size());

    int i = 0;
    for(String lEnumOption : lSchemaEnumeration) {
      lOptionList.add(new BasicSelectOption(lEnumOption, pSelectedIndexes.contains(i), pFieldMgr.getExternalValueForOption(i)));
      i++;
    }

    return lOptionList;
  }

  @Override
  public int getIndexForItem(DataFieldMgr pFieldMgr, DOM pItemDOM) {

    String lCurrentValue = pItemDOM.value().trim();

    int i = 0;
    for(String lEnumOption : pFieldMgr.getEvaluatedNodeInfoItem().getSchemaEnumeration()) {
      if(lEnumOption.equals(lCurrentValue)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  public List<FVMOption> getFVMOptionList(ActionRequestContext pRequestContext, DOM pItemDOM) {

    NodeInfo lNodeInfo;
    if(".".equals(mNodeInfoPath)) {
      lNodeInfo = pRequestContext.getCurrentModule().getNodeInfo(pItemDOM);
    }
    else {
      lNodeInfo = pRequestContext.getCurrentModule().getNodeInfo(mNodeInfoPath);
    }

    //TODO PN Temp conversion - should be done by module parser
    List<FVMOption> lResult = new ArrayList<>();
    for(String lEnumValue : lNodeInfo.getSchemaEnumerationOrNull()) {
      lResult.add(new StringFVMOption(lEnumValue));
    }

    return lResult;
  }
}

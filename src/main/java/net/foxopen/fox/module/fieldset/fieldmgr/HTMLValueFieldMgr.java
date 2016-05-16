package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.HTMLValueFieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;

import java.util.List;


public class HTMLValueFieldMgr
extends DataFieldMgr {

  public HTMLValueFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, String pFieldId) {
    super(pEvaluatedNodeInfo, pFieldSet, pFieldId);
  }

  @Override
  public String getExternalFieldName() {
    return getFieldId();
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {
    return new HTMLValueFieldInfo(getExternalFieldName(), getValueDOM().getRef(), getSingleTextValue(), getEvaluatedNodeInfoItem().getChangeActionName(),
                                  getEvaluatedNodeInfoItem().getStringAttribute(NodeAttribute.HTML_WIDGET_CONFIG));
  }

  /**
   * Gets the XHTML DOM value for this HTML field. The XML in the DOM has fox-error elements and foxid attributes removed before
   * being sent to the user.
   * @return
   */
  @Override
  public DOM getSingleXMLValue() {

    DOM lModifiedDOM = getValueDOM().createDocument();

    // Translate and preprocess elements
    try {
      lModifiedDOM.xpathUL("//fox-error", null).removeFromDOMTree();

      // Remove foxids
      lModifiedDOM.removeRefsRecursive();
    }
    catch (ExBadPath e) {
      throw new ExInternal("Failed to prepare HTML value for sending", e);
    }

    return lModifiedDOM;
  }

  @Override
  public String getSingleTextValue() {
    return getSingleXMLValue().outputHTM5LNodeContentsToString(false);
  }

  @Override
  public List<FieldSelectOption> getSelectOptions() {
    return null;
  }

}

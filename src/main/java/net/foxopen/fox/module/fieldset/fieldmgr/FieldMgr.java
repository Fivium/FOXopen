package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeAction;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoFileItem;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.FieldSelectConfig;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.List;

/**
 * A FieldMgr is a high level manager for a "field" which is being set out on a screen. A "field" for these purposes is
 * any item which is being displayed by a WidgetBuilder, typically a data element or an action link/button. The FieldMgr provides
 * a bridge between a field's EvaluatedNode, WidgetBuilder and owning FieldSet. The FieldMgr is aware of a field's visibility,
 * text content/field value mapping (including any format mask transformations which need to be applied) and its external
 * identifier (i.e. HTML ID).<br><br>
 *
 * The FieldMgr is the final determinant of a field's NodeVisibility. This is because a FieldSet may decide to knock back
 * a field's visibility if the underlying data item has been set out already. A FieldMgr does not immediately mean a field
 * can be edited; that requires a FieldInfo object to be created and stored on the FieldSet (see {@link #createFieldInfoOrNull}.)<br><br>
 *
 * WidgetBuilders must invoke {@link #prepareForSetOut} before setting out the field controlled by a FieldMgr. This is so
 * the FieldSet can be notified that the field will definitely be visible on the output page in some way. The FieldMgr must
 * provide a FieldInfo object at this point so the FieldSet knows how to handle any returned values when the output page
 * is posted (for instance, the removal of format masks - see {@link FieldInfo}.
 */
public abstract class FieldMgr {
  private final String mFieldId;
  private final FieldSet mOwningFieldSet;
  private NodeVisibility mVisibility;

  public static ActionFieldMgr createFieldMgr(EvaluatedNodeAction pEvaluatedNodeAction, FieldSet pFieldSet, String pNewFieldId) {
    return new ActionFieldMgr(pEvaluatedNodeAction, pFieldSet, pNewFieldId);
  }

  public static FieldMgr createFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfoItem, FieldSet pFieldSet, String pNewFieldId) {

    WidgetBuilderType lWidgetBuilderType = pEvaluatedNodeInfoItem.getWidgetBuilderType();
    FieldSelectConfig lFieldSelectConfig = lWidgetBuilderType.getFieldSelectConfig();

    if(lFieldSelectConfig != null) {
      return OptionFieldMgr.createOptionFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, lFieldSelectConfig, pNewFieldId);
    }
    else if(lWidgetBuilderType.isTextValue()) {
      //HTML value
      HTML: {
        String lMixed = pEvaluatedNodeInfoItem.getNodeInfo().getAttribute("", "mixed");
        String lDataType = pEvaluatedNodeInfoItem.getNodeInfo().getDataType();

        if("xs:anyType".equals(lDataType) || "true".equals(lMixed) || lWidgetBuilderType == WidgetBuilderType.HTML) {
          return new HTMLValueFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pNewFieldId);
        }
      }

      //Pure text value
      switch(lWidgetBuilderType){
        case PASSWORD:
          return new PasswordFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pNewFieldId);
        default:
          return new TextValueFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pNewFieldId);
      }
    }
    else if(lWidgetBuilderType.isAction()) {
      //Buttons, links etc - widgets which don't have a text value
      return new ActionFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pNewFieldId);
    }
    else {
      switch(lWidgetBuilderType){
        case FILE:
          return new UploadFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pNewFieldId);
        case IMAGE:
          //Don't attempt to create an image widget FieldMgr if the ENI is of the wrong type (currently because of missing FSL attr)
          //TODO - need a better way to map FieldMgrs to ENI types
          if(!(pEvaluatedNodeInfoItem instanceof EvaluatedNodeInfoFileItem)) {
            throw new ExInternal("Image widgets must have a file-storage-location specified");
          }
          return new ImageFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pNewFieldId);
        case PHANTOM_BUFFER:
        case PHANTOM_MENU:
        case CARTOGRAPHIC: // TODO - NP - carto - This should be temporary, subject to change when we add map editing?
          return new PhantomFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pNewFieldId);
        default:
          throw new ExInternal("Don't know how to create a FieldMgr for a " + lWidgetBuilderType + " widget");
      }
    }
  }

  protected FieldMgr(EvaluatedNode pEvaluatedNode, FieldSet pFieldSet, String pFieldId) {
    mFieldId = pFieldId;
    mOwningFieldSet = pFieldSet;
    mVisibility = pEvaluatedNode.getVisibility();
  }

  /**
   * Tests if this FieldMgr will be used to directly invoke an action (i.e. an onclick or onchange action).
   * @return
   */
  public abstract boolean isRunnable();

  public NodeVisibility getVisibility() {
    return mVisibility;
  }

  protected void setVisibility(NodeVisibility pNewVisibility) {
    mVisibility = pNewVisibility;
  }

  protected FieldSet getOwningFieldSet() {
    return mOwningFieldSet;
  }

  protected String getFieldId() {
    return mFieldId;
  }

  /**
   * This should be called before this Field is set out by a widget. This allows the Field to perform any actions relevant
   * to being set out, i.e. registering itself in a FieldSet if it is editable/runnable, etc.
   */
  public abstract void prepareForSetOut();

  /**
   * Gets the string value which should be used to refer to this FieldMgr externally, i.e. as the name attribute of an HTML
   * input. This name will be used to identify the corresponding FieldInfo when the returned FieldSet is applied.
   * @return
   */
  public abstract String getExternalFieldName();

  /**
   * Gets a list of FieldSelectOptions if this FieldMgr represents a field which can have multiple options selected, or
   * null if it does not.
   * @return
   */
  public abstract List<FieldSelectOption> getSelectOptions();

  /**
   * Gets the single text value which represents this field. If the field is a list of options, the single text value
   * will be a concatentation of all the selected string values.
   * @return
   */
  public abstract String getSingleTextValue();

  /**
   * Gets the XML value to be sent for this field, as a DOM. Note this is NOT the underlying DOM value of the field (see
   * {@link DataFieldMgr#getValueDOM} for this). The XML value is only applicable for fields which require XML to be sent
   * to the user, i.e. the HTML widget. For other fields the default implementation is to return the single text value wrapped
   * in a text node.
   * @return
   */
  public DOM getSingleXMLValue() {
    return DOM.createUnconnectedText(getSingleTextValue());
  }

  /**
   * Creates a FieldInfo for this FieldMgr for retention in a FieldSet. The FieldMgr will construct the correct type of
   * FieldInfo based on its subclass. Some subclasses may return null (e.g. RadioGroupValues) because they deal with creating
   * their FieldInfos autonomously or because they are never editable on screen.
   * @return A new FieldInfo for serialisation on a FieldSet, or null if a FieldInfo cannot be created for this FieldMgr.
   */
  protected abstract FieldInfo createFieldInfoOrNull();
}

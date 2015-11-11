package net.foxopen.fox.module.serialiser.widgets;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;

import java.util.stream.Stream;

/**
 * Shared methods which may be used by all option widget builders.
 */
public class OptionWidgetUtils {
  private static final int DEFAULT_ITEMS_PER_ROW = 4;

  private OptionWidgetUtils() {
  }

  /**
   * Return the number of items per row based on the items per row node attribute for a multi option widget.
   * If the attribute is not specified, the default number of items per row is returned.
   *
   * @param pEvalNode The multi option select node
   * @return the number of items per row
   */
  public static int getItemsPerRow(EvaluatedNode pEvalNode) {
    String lItemsPerRowAttribute = pEvalNode.getStringAttribute(NodeAttribute.ITEMS_PER_ROW);
    return !XFUtil.isNull(lItemsPerRowAttribute) ? Integer.parseInt(lItemsPerRowAttribute) : DEFAULT_ITEMS_PER_ROW;
  }

  /**
   * Filters an option list for a search selector widget (RO or editable). The FieldSelectConfig for a search selector
   * requires the inclusion of a key-null entry, but this should only be displayed for single select widgets with the
   * <tt>searchMandatorySelection</tt> attribute set to true (unlike a selector widget, the search selector can signify
   * nullness simply by appearing empty, rather than requiring a special value). For a multi select widget, allowing
   * key-null to be displayed or selected does not make sense, as the user can clear all the selected tags.
   *
   * @param pEvalNode Node which options are being displayed for.
   * @return Filtered stream of FieldSelectOptions, ready for additional intermediate or terminal operations.
   */
  public static Stream<FieldSelectOption> filteredSearchSelectorOptions(EvaluatedNodeInfoItem pEvalNode) {
    //Filter key-null/key-missing entries, unless mandatory selection is enabled and the field is single select and not mandatory (i.e. a key-null needs to be displayed)
    return pEvalNode.getFieldMgr().getSelectOptions()
      .stream()
      .filter(e -> (!e.isNullEntry() && !e.isMissingEntry())
        || (e.isNullEntry() && pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_MANDATORY_SELECTION, false) && pEvalNode.getSelectorMaxCardinality() <= 1));
  }

  /**
   * Filters an option list for a read only selector widget. Only selected options are preserved, and key-missing options
   * are never shown. Search selectors have additional filtering applied to remove key-null entries if the widget configuration
   * does not allow them.
   *
   * @param pEvalNode Node which options are being displayed for.
   * @return Filtered stream of FieldSelectOptions, ready for additional intermediate or terminal operations.
   */
  public static Stream<FieldSelectOption> filteredReadOnlySelectorOptions(EvaluatedNodeInfoItem pEvalNode) {

    Stream<FieldSelectOption> lStream;
    if(pEvalNode.getWidgetBuilderType() ==  WidgetBuilderType.SEARCH_SELECTOR) {
      lStream = filteredSearchSelectorOptions(pEvalNode);
    }
    else {
      lStream = pEvalNode.getFieldMgr().getSelectOptions().stream();
    }

    return lStream.filter(e -> e.isSelected() && !e.isMissingEntry());
  }
}

package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.serialiser.FOXGridUtils;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.layout.GridLayoutManager;
import net.foxopen.fox.module.serialiser.layout.items.LayoutFieldValueMappingItemColumn;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItem;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemEnum;
import net.foxopen.fox.module.serialiser.layout.methods.FieldValueMappingLayout;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.Map;


/**
 * Contains the layout code for radio buttons and tickboxes as they're pretty much the same thing
 */
public class MultiOptionSelectWidget extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfo> {
  private static final String MUSTACHE_TEMPLATE = "html/MultiOptionSelectWidget.mustache";

  private final WidgetBuilderType mWidgetBuilderType;

  public static final WidgetBuilder getInstance() {
    throw new ExInternal("Cannot create a MultiOptionSelectWidget directly, must be a radio or tickbox widget");
  }

  protected MultiOptionSelectWidget(WidgetBuilderType pWidgetBuilderType) {
    mWidgetBuilderType = pWidgetBuilderType;
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    String lItemsPerRowEval = pEvalNode.getStringAttribute(NodeAttribute.ITEMS_PER_ROW, "4");
    int lItemsPerRow = Integer.parseInt(lItemsPerRowEval);

    GridLayoutManager lMultiOptionsLayout = new GridLayoutManager(lItemsPerRow, pSerialiser, pEvalNode, FieldValueMappingLayout.getInstance());

    //TODO PN move this sort of logic to widget builder type?
    String lHtmlInputType;
    switch(mWidgetBuilderType) {
      case TICKBOX:
        lHtmlInputType = "checkbox";
        break;
      case RADIO:
        lHtmlInputType = "radio";
        break;
      default:
        throw new ExInternal("Don't know what HTML input type to use for " + mWidgetBuilderType);
    }

    if (lMultiOptionsLayout.getFilledColumnCount() == 1) {
      ITEM_LOOP: for (LayoutItem lItem : lMultiOptionsLayout.getLayoutItems()) {
        if (lItem.getItemType() == LayoutItemEnum.COLUMN) {
          LayoutFieldValueMappingItemColumn lColumnItem = (LayoutFieldValueMappingItemColumn) lItem;
          FieldSelectOption lSelectOption = lColumnItem.getFieldSelectOption();
          if (!lColumnItem.isFiller()) {
            Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialiser, pEvalNode);
            lTemplateVars.put("InputType", lHtmlInputType);
            lTemplateVars.put("FieldID", lSelectOption.getExternalFieldValue());
            lTemplateVars.put("Checked", lSelectOption.isSelected());
            lTemplateVars.put("Disabled", pEvalNode.getFieldMgr().getVisibility() == NodeVisibility.VIEW || lSelectOption.isDisabled());
            lTemplateVars.put("PromptText", lSelectOption.getDisplayKey());
            lTemplateVars.put("KeyClass", Joiner.on(" ").skipNulls().join("radio-label", pEvalNode.getStringAttribute(NodeAttribute.KEY_CLASS)));
            lTemplateVars.put("KeyStyle", pEvalNode.getStringAttribute(NodeAttribute.KEY_STYLE));

            MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
            break ITEM_LOOP; // Break out after finding and serialising the first (and only) non-filler column
          }
        }
      }
    }
    else if (lMultiOptionsLayout.getLayoutItems().size() > 0) {
      pSerialiser.append("<div class=\"container\">");

      for (LayoutItem lItem : lMultiOptionsLayout.getLayoutItems()) {
        if (lItem.getItemType() == LayoutItemEnum.ROW_START) {
          pSerialiser.append("<div class=\"row\">");
        }
        else if (lItem.getItemType() == LayoutItemEnum.ROW_END) {
          pSerialiser.append("</div>"); // End the row div
        }
        else if (lItem.getItemType() == LayoutItemEnum.COLUMN) {
          LayoutFieldValueMappingItemColumn lColumnItem = (LayoutFieldValueMappingItemColumn)lItem;
          FieldSelectOption lSelectOption = lColumnItem.getFieldSelectOption();

          pSerialiser.append("<div class=\"");
          pSerialiser.append(FOXGridUtils.calculateColumnClassName(lColumnItem.getColSpan(), lItemsPerRow));
          pSerialiser.append(" columns\">");

          if (!lColumnItem.isFiller()) {
            Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialiser, pEvalNode);
            lTemplateVars.put("InputType", lHtmlInputType);
            lTemplateVars.put("FieldID", lSelectOption.getExternalFieldValue());
            lTemplateVars.put("Checked", lSelectOption.isSelected());
            lTemplateVars.put("Disabled", pEvalNode.getFieldMgr().getVisibility() == NodeVisibility.VIEW || lSelectOption.isDisabled());
            lTemplateVars.put("PromptText", lSelectOption.getDisplayKey());
            lTemplateVars.put("KeyClass", Joiner.on(" ").skipNulls().join("radio-label", pEvalNode.getStringAttribute(NodeAttribute.KEY_CLASS)));
            lTemplateVars.put("KeyStyle", pEvalNode.getStringAttribute(NodeAttribute.KEY_STYLE));

            MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
          }
          else {
            pSerialiser.append("<span style=\"display:none;\">Filler column...</span>");
          }

          pSerialiser.append("</div>"); // End the column div
        }
      }
      pSerialiser.append("</div>"); // End container div
    }
  }
}

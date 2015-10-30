package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.LayoutDirection;
import net.foxopen.fox.module.OutputError;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.FOXGridUtils;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.layout.GridLayoutManager;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItem;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemEnum;
import net.foxopen.fox.module.serialiser.layout.items.LayoutWidgetItemColumn;
import net.foxopen.fox.module.serialiser.widgets.FormWidgetUtils;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO - This contains a lot of shared code with the form widget builder aside from a couple of class/style attribute differences.
 * Should make a generic helper class for these so that changes to one affect the other
 */
public class CellmateWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfo> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfo> INSTANCE = new CellmateWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfo> getInstance() {
    return INSTANCE;
  }

  private CellmateWidgetBuilder () {
  }

  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    if (hasPrompt(pEvalNode)) {
      Map<String, Object> lTemplateVars = new HashMap<>();
      lTemplateVars.put("FieldName", pEvalNode.getFieldMgr().getExternalFieldName());
      lTemplateVars.put("PromptText", pEvalNode.getPrompt().getString());
      lTemplateVars.put("Class", "prompt " + pEvalNode.getStringAttribute(NodeAttribute.PROMPT_LAYOUT, "west"));

      // TODO - NP - Need to think how/if mandatoryness is displayed for cellmates

      MustacheFragmentBuilder.applyMapToTemplate("html/Prompt.mustache", lTemplateVars, pSerialiser.getWriter());
    }
  }

  @Override
  public void buildWidget(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    buildWidgetInternal(pSerialisationContext, pSerialiser, pEvalNode);
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    final int lColumnLimit = FormWidgetUtils.getFormColumns(pEvalNode);

    GridLayoutManager lFormLayout = new GridLayoutManager(lColumnLimit, pSerialiser, pEvalNode);

    List<EvaluatedNodeInfo> lChildren = pEvalNode.getChildren();
    if (lChildren.size() > 0) {
      pSerialiser.append("<div class=\"container cellmates");

      List<String> lClasses = pEvalNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.FORM_CLASS, NodeAttribute.CELLMATE_CLASS);
      List<String> lStyles = pEvalNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.FORM_STYLE, NodeAttribute.CELLMATE_STYLE);
      if (lClasses.size() > 0) {
        pSerialiser.append(" ");
        pSerialiser.append(Joiner.on(" ").join(lClasses));
      }
      pSerialiser.append("\"");

      if (lStyles.size() > 0) {
        pSerialiser.append(" style=\"");
        pSerialiser.append(Joiner.on(" ").join(lStyles));
        pSerialiser.append("\"");
      }
      pSerialiser.append(">");

      for (LayoutItem lItem : lFormLayout.getLayoutItems()) {
        if (lItem.getItemType() == LayoutItemEnum.ROW_START) {
          pSerialiser.append("<div class=\"row\">");
        }
        else if (lItem.getItemType() == LayoutItemEnum.ROW_END) {
          pSerialiser.append("</div>");
        }
        else if (lItem.getItemType() == LayoutItemEnum.COLUMN) {
          LayoutWidgetItemColumn lColumnItem = (LayoutWidgetItemColumn)lItem;
          pSerialiser.append("<div class=\"");
          pSerialiser.append(FOXGridUtils.calculateColumnClassName(lColumnItem.getColSpan(), lColumnLimit));
          pSerialiser.append(" columns");

          List<String> lCellClasses = new ArrayList<>();
          List<String> lCellStyles = new ArrayList<>();

          if(!lColumnItem.isFiller()) {
            //For prompts and widgets (i.e. non-filler cells), add any extra classes which might be needed (i.e. client visibility initial state)
            lCellClasses.addAll(lColumnItem.getItemNode().getCellInternalClasses());
          }

          if (lColumnItem.isPrompt()) {
            // If it's a cell for a prompt
            lCellClasses.addAll(lColumnItem.getItemNode().getStringAttributes(NodeAttribute.PROMPT_CLASS));
            lCellStyles.addAll(lColumnItem.getItemNode().getStringAttributes(NodeAttribute.PROMPT_STYLE));
          }
          else if (!lColumnItem.isFiller()) {
            // If it's a cell for a field
            if (lColumnItem.getItemNode().hasHint() && lColumnItem.getItemNode().getWidgetBuilderType() != WidgetBuilderType.BUTTON) { // TODO - NP - Hack to stop hints on buttons
              // Add the input group class when not a prompt column and has hints
              lCellClasses.add("input-group");
            }
            if (lColumnItem.getItemNode().hasError()) {
              lCellClasses.add("input-error");
            }

            // Add styles just for the field cell
            lCellClasses.addAll(lColumnItem.getItemNode().getStringAttributes(NodeAttribute.FORM_TABLE_CLASS, NodeAttribute.FORM_CELL_CLASS, NodeAttribute.FIELD_CELL_CLASS, NodeAttribute.CELL_CLASS));
            lCellStyles.addAll(lColumnItem.getItemNode().getStringAttributes(NodeAttribute.FORM_TABLE_STYLE, NodeAttribute.FORM_CELL_STYLE, NodeAttribute.FIELD_CELL_STYLE, NodeAttribute.CELL_STYLE));
          }

          if (lCellClasses.size() > 0) {
            pSerialiser.append(" " + Joiner.on(" ").join(lCellClasses));
          }
          pSerialiser.append("\""); //End class attr

          if (lCellStyles.size() > 0) {
            pSerialiser.append(" style=\"" + Joiner.on(" ").join(lCellStyles) + "\"");
          }

          if(!lColumnItem.isFiller()) {
            HTMLSerialiser.serialiseDataAttributes(pSerialiser, lColumnItem.getItemNode());
          }

          pSerialiser.append(">");

          // Output debug information if turned on
          if (pSerialisationContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.HTML_GEN_DEBUG)) {
            StringBuilder lItemDebugInfo = new StringBuilder();
            if (!lColumnItem.isFiller()) {
              lItemDebugInfo.append("<p><strong>Namespaces:</strong><ol><li>");
              lItemDebugInfo.append(Joiner.on("</li><li>").join(lColumnItem.getItemNode().getNamespacePrecedenceList()));
              lItemDebugInfo.append("</li></ol></p>");
              lItemDebugInfo.append("<p>");
              lItemDebugInfo.append(StringEscapeUtils.escapeHtml4(lColumnItem.getItemNode().getIdentityInformation()));
              lItemDebugInfo.append("</p>");
            }
            lItemDebugInfo.append("<p><strong>Column Type:</strong> ");
            if (lColumnItem.isFiller()) {
              lItemDebugInfo.append("Filler");
            }
            else if (lColumnItem.isPrompt()) {
              lItemDebugInfo.append("Prompt");
            }
            else {
              lItemDebugInfo.append("Field");
            }
            lItemDebugInfo.append("</p>");
            lItemDebugInfo.append("<p><strong>Width:</strong> ");
            lItemDebugInfo.append(lColumnItem.getColSpan());
            lItemDebugInfo.append(" of ");
            lItemDebugInfo.append(lColumnLimit);
            lItemDebugInfo.append(" logical columns</p>");
            pSerialiser.addDebugInformation(lItemDebugInfo.toString());
          }

          if (!lColumnItem.isFiller()) {
            if (lColumnItem.isPrompt()) {
              lColumnItem.getWidgetBuilder().buildPrompt(pSerialisationContext, pSerialiser, lColumnItem.getItemNode());
            }
            else {
              if (lColumnItem.getItemNode().hasHint() && lColumnItem.getItemNode().getWidgetBuilderType() != WidgetBuilderType.BUTTON) {
                pSerialiser.append("<div class=\"eleven-column-input\">");
              }

              if(lColumnItem.getItemNode().hasDescription() && LayoutDirection.NORTH == lColumnItem.getItemNode().getDescriptionLayout()) {
                pSerialiser.addDescription(pSerialisationContext, lColumnItem.getItemNode());
              }

              lColumnItem.getWidgetBuilder().buildWidget(pSerialisationContext, pSerialiser, lColumnItem.getItemNode());

              if(lColumnItem.getItemNode().hasDescription() && LayoutDirection.SOUTH == lColumnItem.getItemNode().getDescriptionLayout()) {
                pSerialiser.addDescription(pSerialisationContext, lColumnItem.getItemNode());
              }

              if (lColumnItem.getItemNode().hasError()) {
                OutputError lError = lColumnItem.getItemNode().getError();
                pSerialiser.append("<div class=\"error-message\">");
                pSerialiser.append(lError.getContent());
                if (XFUtil.exists(lError.getErrorURL()) && XFUtil.exists(lError.getErrorURLPrompt())) {
                  pSerialiser.append("<a href=\"");
                  pSerialiser.append(StringEscapeUtils.escapeHtml4(lError.getErrorURL()));
                  pSerialiser.append("\" class=\"error-url\">");
                  pSerialiser.append(StringEscapeUtils.escapeHtml4(lError.getErrorURLPrompt()));
                  pSerialiser.append("</a>");
                }
                pSerialiser.append("</div>");
              }

              if (lColumnItem.getItemNode().hasHint() && lColumnItem.getItemNode().getWidgetBuilderType() != WidgetBuilderType.BUTTON) {
                pSerialiser.append("</div>");
                pSerialiser.append("<div class=\"input-group-addon hint-addon\">");
                pSerialiser.addHint(pSerialisationContext, lColumnItem.getItemNode().getHint());
                pSerialiser.append("</div>");
              }
            }
          }

          pSerialiser.append("</div>"); // Close the column div
        }
      }

      pSerialiser.append("</div>"); // Close the container div
    }
  }
}

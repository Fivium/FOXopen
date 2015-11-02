package net.foxopen.fox.module.serialiser.components.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.LayoutDirection;
import net.foxopen.fox.module.OutputError;
import net.foxopen.fox.module.OutputHistory;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.layout.items.LayoutWidgetItemColumn;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Helper class for widget-out and set-out which can both set out an individual widget
 */
public class SingleWidgetBuildHelper {
  /**
   * Build a widget with all its facets
   *
   * @param pEvalNode ENI for the widget to display
   * @param pSerialiser Output serialiser to write to
   */
  public static void buildWidget(SerialisationContext pSerialisationContext, EvaluatedNode pEvalNode, HTMLSerialiser pSerialiser) {
    buildWidget(pSerialisationContext, pEvalNode, pSerialiser, true, true, true, true, true);
  }

  /**
   * Build a widget, specifying which parts to display
   *
   * @param pEvalNode ENI for the widget to display
   * @param pSerialiser Output serialiser to write to
   * @param pShowPrompt
   * @param pShowWidget
   * @param pShowError
   * @param pShowHint
   */
  public static void buildWidget(SerialisationContext pSerialisationContext, EvaluatedNode pEvalNode, HTMLSerialiser pSerialiser,
                                 boolean pShowPrompt, boolean pShowWidget, boolean pShowError, boolean pShowHint, boolean pShowDescription) {
    if (pEvalNode != null) {
      WidgetBuilderType lWidgetBuilder = pEvalNode.getWidgetBuilderType();

      if (pShowPrompt) {
        pSerialiser.getWidgetBuilder(lWidgetBuilder).buildPrompt(pSerialisationContext, pSerialiser, pEvalNode);
      }

      if (pShowWidget || pShowError || pShowHint || pShowDescription) {
        if (pShowWidget && (pShowHint || pShowError)) {
          pSerialiser.append("<div class=\"individual-input");
          if (pShowHint && pEvalNode.hasHint() && pEvalNode.getWidgetBuilderType() != WidgetBuilderType.BUTTON) {
            // Add the input group class when item has hints and not a button which has hints on itself rather than appended
            pSerialiser.append(" input-group");
          }
          if (pShowError && pEvalNode.hasError()) {
            pSerialiser.append(" input-error");
          }
          pSerialiser.append("\">");
        }

        if (pShowWidget && pShowHint && pEvalNode.hasHint() && pEvalNode.getWidgetBuilderType() != WidgetBuilderType.BUTTON) {
          pSerialiser.append("<div class=\"eleven-column-input\">");
        }


        if(pShowDescription && pEvalNode.hasDescription() && LayoutDirection.NORTH == pEvalNode.getDescriptionLayout()) {
          pSerialiser.addDescription(pSerialisationContext, pEvalNode);
        }

        if (pShowWidget) {
          pSerialiser.getWidgetBuilder(lWidgetBuilder).buildWidget(pSerialisationContext, pSerialiser, pEvalNode);
        }

        if(pShowDescription && pEvalNode.hasDescription() && LayoutDirection.SOUTH == pEvalNode.getDescriptionLayout()) {
          pSerialiser.addDescription(pSerialisationContext, pEvalNode);
        }

        if (pShowError) {
          insertErrorMessage(pSerialiser, pEvalNode);
        }

        if (pShowHint && pEvalNode.hasHint() && pEvalNode.getWidgetBuilderType() != WidgetBuilderType.BUTTON) {
          if (pShowWidget) {
            pSerialiser.append("</div>");
            pSerialiser.append("<div class=\"input-group-addon hint-addon\">");
          }
          pSerialiser.addHint(pSerialisationContext, pEvalNode.getHint());
          if (pShowWidget) {
            pSerialiser.append("</div>");
          }
        }

        if (pShowWidget && (pShowHint || pShowError)) {
          pSerialiser.append("</div>");
        }
      }
    }
  }

  /**
   * Get a list of CSS classes based on features of the Evaluated Node (e.g. errors, history, widget types)
   *
   * @param pEvaluatedNode
   * @return
   */
  public static List<String> getClassesForNodeFeatures(EvaluatedNode pEvaluatedNode) {
    List<String> lClassList = new ArrayList<>();

    if (pEvaluatedNode.hasHint() && pEvaluatedNode.getWidgetBuilderType() != WidgetBuilderType.BUTTON) { // TODO - NP - Hack to stop hints on buttons
      // Add the input group class when not a prompt column and has hints
      lClassList.add("input-group");
    }
    if (pEvaluatedNode.getWidgetBuilderType() == WidgetBuilderType.RADIO || pEvaluatedNode.getWidgetBuilderType() == WidgetBuilderType.TICKBOX) {
      lClassList.add("radio-or-tickbox-group");
    }
    if (pEvaluatedNode.hasError()) {
      lClassList.add("input-error");
    }
    if (pEvaluatedNode.hasHistory()) {
      lClassList.add("history");
      lClassList.add("history-" + StringEscapeUtils.escapeEcmaScript(pEvaluatedNode.getHistory().getOperation()));
    }

    return lClassList;
  }

  /**
   * Insert an error message block using the HTMLSerialiser for a given EvaluatedNode if it has an error to show
   *
   * @param pSerialiser
   * @param pEvaluatedNode
   */
  public static void insertErrorMessage(HTMLSerialiser pSerialiser, EvaluatedNode pEvaluatedNode) {
    if (pEvaluatedNode.hasError()) {
      OutputError lError = pEvaluatedNode.getError();
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
  }

  /**
   * Insert a history message block using the HTMLSerialiser for a given EvaluatedNode if it has a history to show
   *
   * @param pSerialiser
   * @param pEvaluatedNode
   */
  public static void insertHistoryMessage(HTMLSerialiser pSerialiser, EvaluatedNode pEvaluatedNode) {
    if (pEvaluatedNode.hasHistory()) {
      OutputHistory lHistory = pEvaluatedNode.getHistory();
      pSerialiser.append("<div class=\"history-message\"><p>Data changed:</p><p>");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(XFUtil.initCap(lHistory.getOperation())));
      pSerialiser.append("</p><p>");
      if (XFUtil.isNull(lHistory.getValue())) {
        // TODO - NP - This showed in FOX4 but commented out as nobody wanted to see this now
//        pSerialiser.append("(Unable to show ");
//        pSerialiser.append(StringEscapeUtils.escapeHtml4(lHistory.getLabel()));
//        pSerialiser.append(")");
      }
      else {
        pSerialiser.append("(");
        pSerialiser.append(StringEscapeUtils.escapeHtml4(XFUtil.initCap(lHistory.getLabel())));
        pSerialiser.append(": ");
        pSerialiser.append(StringEscapeUtils.escapeHtml4(XFUtil.initCap(lHistory.getValue())));
        pSerialiser.append(")");
      }
      pSerialiser.append("</p></div>");
    }
  }

  /**
   * Output as much debug information as possible for a LayoutWidgetItemColumn
   *
   * @param pSerialiser Serialiser to output debug with
   * @param pColumnItem The item to get information from
   * @param pColumnLimit Limit for reference in debug information
   */
  public static void outputDebugInformation(HTMLSerialiser pSerialiser, LayoutWidgetItemColumn pColumnItem, int pColumnLimit) {
    StringBuilder lItemDebugInfo = new StringBuilder();
    if (!pColumnItem.isFiller()) {
      lItemDebugInfo.append("<p><strong>Namespaces:</strong><ol><li>");
      lItemDebugInfo.append(Joiner.on("</li><li>").join(pColumnItem.getItemNode().getNamespacePrecedenceList()));
      lItemDebugInfo.append("</li></ol></p>");
      lItemDebugInfo.append("<p>");
      lItemDebugInfo.append(StringEscapeUtils.escapeHtml4(pColumnItem.getItemNode().getIdentityInformation()));
      lItemDebugInfo.append("</p>");

      lItemDebugInfo.append("<p><strong>DisplayOrder:</strong> ");
      lItemDebugInfo.append(pColumnItem.getItemNode().getDisplayOrder());
      lItemDebugInfo.append("</p>");
      if (!XFUtil.isNull(pColumnItem.getItemNode().getDisplayBeforeAttribute())) {
        lItemDebugInfo.append("<p><strong>DisplayBefore:</strong> ");
        lItemDebugInfo.append(pColumnItem.getItemNode().getDisplayBeforeAttribute());
        lItemDebugInfo.append("</p>");
      }
      if (!XFUtil.isNull(pColumnItem.getItemNode().getDisplayAfterAttribute())) {
        lItemDebugInfo.append("<p><strong>DisplayAfter:</strong> ");
        lItemDebugInfo.append(pColumnItem.getItemNode().getDisplayAfterAttribute());
        lItemDebugInfo.append("</p>");
      }
    }
    lItemDebugInfo.append("<p><strong>Column Type:</strong> ");
    if (pColumnItem.isFiller()) {
      lItemDebugInfo.append("Filler");
    }
    else if (pColumnItem.isPrompt()) {
      lItemDebugInfo.append("Prompt");
    }
    else {
      lItemDebugInfo.append("Field");
    }
    lItemDebugInfo.append("</p>");
    lItemDebugInfo.append("<p><strong>Width:</strong> ");
    lItemDebugInfo.append(pColumnItem.getColSpan());
    lItemDebugInfo.append(" of ");
    lItemDebugInfo.append(pColumnLimit);
    lItemDebugInfo.append(" logical columns</p>");
    pSerialiser.addDebugInformation(lItemDebugInfo.toString());
  }

  /**
   * Output as much debug information as possible for a EvaluatedNode
   *
   * @param pSerialiser Serialiser to output debug with
   * @param pEvaluatedNode The item to get information from
   */
  public static void outputDebugInformation(HTMLSerialiser pSerialiser, EvaluatedNode pEvaluatedNode) {
    StringBuilder lItemDebugInfo = new StringBuilder();
    lItemDebugInfo.append("<p>Namespaces:<ol><li>");
    lItemDebugInfo.append(Joiner.on("</li><li>").join(pEvaluatedNode.getNamespacePrecedenceList()));
    lItemDebugInfo.append("</li></ol></p>");
    lItemDebugInfo.append("<p>");
    lItemDebugInfo.append(StringEscapeUtils.escapeHtml4(pEvaluatedNode.getIdentityInformation()));
    lItemDebugInfo.append("</p>");
    pSerialiser.addDebugInformation(lItemDebugInfo.toString());
  }
}

package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.LayoutDirection;
import net.foxopen.fox.module.OutputError;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import org.apache.commons.lang3.StringEscapeUtils;


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

        if (pShowError && pEvalNode.hasError()) {
          OutputError lError = pEvalNode.getError();
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
}

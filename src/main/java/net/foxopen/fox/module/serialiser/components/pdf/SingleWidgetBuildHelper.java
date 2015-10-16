package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;


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
  public static void buildWidget(SerialisationContext pSerialisationContext, EvaluatedNode pEvalNode, PDFSerialiser pSerialiser) {
    buildWidget(pSerialisationContext, pEvalNode, pSerialiser, true, true, true, true, true);
  }

  /**
   * Build a widget, specifying which parts to display
   *
   * @param pEvalNode ENI for the widget to display
   * @param pSerialiser Output serialiser to write to
   * @param pShowPrompt Whether the widget prompt should be displayed
   * @param pShowWidget Whether the widget itself should be displayed
   * @param pShowError Whether any errors should be displayed
   * @param pShowHint Whether the hint should be displayed
   * @param pShowDescription Whether the description should be displayed
   */
  public static void buildWidget(SerialisationContext pSerialisationContext, EvaluatedNode pEvalNode, PDFSerialiser pSerialiser,
                                 boolean pShowPrompt, boolean pShowWidget, boolean pShowError, boolean pShowHint, boolean pShowDescription) {
    if (pEvalNode == null) {
      return;
    }

    WidgetBuilderType lWidgetBuilder = pEvalNode.getWidgetBuilderType();
    if (pShowPrompt) {
      pSerialiser.getWidgetBuilder(lWidgetBuilder).buildPrompt(pSerialisationContext, pSerialiser, pEvalNode);
    }
    if (pShowWidget) {
      pSerialiser.getWidgetBuilder(lWidgetBuilder).buildWidget(pSerialisationContext, pSerialiser, pEvalNode);
    }

    // pShowError, pShowHint, pShowDescription are currently unused as PDF does not output any of those
  }
}

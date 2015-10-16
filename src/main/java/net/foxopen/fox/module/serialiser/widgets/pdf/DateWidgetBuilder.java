package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.Collections;

/**
 * Serialises a date widget
 */
public class DateWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNode> {
  private static final String DATE_FIELD_CLASS = "dateField";
  private static final String DATE_TIME_FIELD_CLASS = "dateTimeField";
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNode> INSTANCE = new DateWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private DateWidgetBuilder() {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (isVisible(pEvalNode)) {
      if (pEvalNode.isPlusWidget() || pEvalNode.getFieldMgr().getVisibility() == NodeVisibility.EDIT) {
        String lFieldClass = pEvalNode.getWidgetBuilderType() == WidgetBuilderType.DATE_TIME ? DATE_TIME_FIELD_CLASS : DATE_FIELD_CLASS;
        InputField<EvaluatedNode> lInputField = new InputField<>(this::addDateContent, isTightField(pEvalNode), Collections.singletonList(lFieldClass));
        lInputField.serialise(pSerialisationContext, pSerialiser, pEvalNode);
      }
      else {
        addDateContent(pSerialisationContext, pSerialiser, pEvalNode);
      }
    }
  }

  /**
   * Adds the date content text
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pEvalNode
   */
  private void addDateContent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    pSerialiser.getWidgetBuilder(WidgetBuilderType.TEXT).buildWidget(pSerialisationContext, pSerialiser, pEvalNode);
  }
}

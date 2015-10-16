package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.Collections;

public class InputWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNode> {
  private static final String TEXT_FIELD_CLASS = "textField";
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNode> INSTANCE = new InputWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private InputWidgetBuilder() {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (isVisible(pEvalNode)) {
      if (pEvalNode.isPlusWidget() || pEvalNode.getFieldMgr().getVisibility() == NodeVisibility.EDIT) {
        InputField<EvaluatedNode> lInputField = new InputField<>(this::addInputContent, isTightField(pEvalNode), Collections.singletonList(TEXT_FIELD_CLASS));
        lInputField.serialise(pSerialisationContext, pSerialiser, pEvalNode);
      }
      else {
        addInputContent(pSerialisationContext, pSerialiser, pEvalNode);
      }
    }
  }

  /**
   * Adds the input content text
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pEvalNode
   */
  private void addInputContent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    pSerialiser.getWidgetBuilder(WidgetBuilderType.TEXT).buildWidget(pSerialisationContext, pSerialiser, pEvalNode);
  }
}

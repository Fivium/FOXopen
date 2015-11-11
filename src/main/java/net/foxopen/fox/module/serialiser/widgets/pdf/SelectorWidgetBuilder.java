package net.foxopen.fox.module.serialiser.widgets.pdf;


import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.OptionWidgetUtils;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class SelectorWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNodeInfoItem> {
  private static final String SELECTOR_FIELD_CLASS = "selectorField";
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfoItem> INSTANCE = new SelectorWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfoItem> getInstance() {
    return INSTANCE;
  }

  private SelectorWidgetBuilder() {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfoItem pEvalNode) {
    if (isVisible(pEvalNode)) {
      if (pEvalNode.isPlusWidget() || pEvalNode.getFieldMgr().getVisibility() == NodeVisibility.EDIT) {
        InputField<EvaluatedNodeInfoItem> lInputField = new InputField<>(this::addSelectorContent, isTightField(pEvalNode), Collections.singletonList(SELECTOR_FIELD_CLASS));
        lInputField.serialise(pSerialisationContext, pSerialiser, pEvalNode);
      }
      else {
        addSelectorContent(pSerialisationContext, pSerialiser, pEvalNode);
      }
    }
  }

  /**
   * Adds the selector content text
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pEvalNode
   */
  private void addSelectorContent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfoItem pEvalNode) {
    getNonNullSelectedOptions(pEvalNode).forEach(pSerialiser::addParagraphText);
  }

  private List<String> getNonNullSelectedOptions(EvaluatedNodeInfoItem pEvalNode) {
    return OptionWidgetUtils.filteredReadOnlySelectorOptions(pEvalNode)
      .map(FieldSelectOption::getDisplayKey)
      .collect(Collectors.toList());
  }
}

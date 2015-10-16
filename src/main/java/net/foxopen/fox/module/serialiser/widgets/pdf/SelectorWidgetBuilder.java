package net.foxopen.fox.module.serialiser.widgets.pdf;


import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class SelectorWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNode> {
  private static final String SELECTOR_FIELD_CLASS = "selectorField";
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNode> INSTANCE = new SelectorWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private SelectorWidgetBuilder() {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (isVisible(pEvalNode)) {
      if (pEvalNode.isPlusWidget() || pEvalNode.getFieldMgr().getVisibility() == NodeVisibility.EDIT) {
        InputField<EvaluatedNode> lInputField = new InputField<>(this::addSelectorContent, isTightField(pEvalNode), Collections.singletonList(SELECTOR_FIELD_CLASS));
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
  private void addSelectorContent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    getNonNullSelectedOptions(pEvalNode.getFieldMgr()).forEach(pSerialiser::addParagraphText);
  }

  private List<String> getNonNullSelectedOptions(FieldMgr pFieldMgr) {
    return pFieldMgr.getSelectOptions()
                    .stream()
                    .filter(pOption -> pOption.isSelected() && !pOption.isNullEntry())
                    .map(FieldSelectOption::getDisplayKey)
                    .collect(Collectors.toList());
  }
}

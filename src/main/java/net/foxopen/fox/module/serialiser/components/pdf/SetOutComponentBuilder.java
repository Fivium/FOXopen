package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedSetOutPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.Optional;


public class SetOutComponentBuilder
extends ComponentBuilder<PDFSerialiser, EvaluatedSetOutPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedSetOutPresentationNode> INSTANCE = new SetOutComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedSetOutPresentationNode> getInstance() {
    return INSTANCE;
  }

  private SetOutComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedSetOutPresentationNode pEvalNode) {
    Optional.ofNullable(pEvalNode.getChildEvaluatedNodeOrNull()).ifPresent(pSetOutNode -> {
      // The set-out node is present and therefore is visible and should be output
      WidgetBuilderType lWidgetBuilderType = pSetOutNode.getWidgetBuilderType();

      if (lWidgetBuilderType.isInternalOnly()) {
        // An internal node is one that cannot be specified via the widget attribute in the module (e.g. form, list)
        pSerialiser.getWidgetBuilder(lWidgetBuilderType).buildWidget(pSerialisationContext, pSerialiser, pSetOutNode);
      }
      else {
        // When the node is not internal only, it means a single widget was specified to be set out (e.g. an input etc.
        // was directly targeted). The single widget build helper will add the prompt etc. as well as the widget itself.
        SingleWidgetBuildHelper.buildWidget(pSerialisationContext, pSetOutNode, pSerialiser);
      }
    });
  }
}

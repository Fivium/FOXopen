package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.datanode.EvaluatedNodeAction;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedActionOutPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;


public class ActionOutComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedActionOutPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedActionOutPresentationNode> INSTANCE = new ActionOutComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedActionOutPresentationNode> getInstance() {
    return INSTANCE;
  }

  private ActionOutComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedActionOutPresentationNode pEvalNode) {
    EvaluatedNodeAction lEvaluatedNodeAction = pEvalNode.getEvaluatedNodeAction();

    // Output debug information if turned on
    if (lEvaluatedNodeAction != null && pSerialisationContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.HTML_GEN_DEBUG)) {
      SingleWidgetBuildHelper.outputDebugInformation(pSerialiser, lEvaluatedNodeAction);
    }

    if (lEvaluatedNodeAction != null && lEvaluatedNodeAction.getVisibility() != NodeVisibility.DENIED) {
      pSerialiser.getWidgetBuilder(lEvaluatedNodeAction.getWidgetBuilderType()).buildWidget(pSerialisationContext, pSerialiser, lEvaluatedNodeAction);
    }
  }
}

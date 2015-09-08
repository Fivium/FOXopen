package net.foxopen.fox.module.serialiser.components.html;

import com.google.common.base.Joiner;
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
      StringBuilder lItemDebugInfo = new StringBuilder();
      lItemDebugInfo.append("<p><strong>Namespaces:</strong><ol><li>");
      lItemDebugInfo.append(Joiner.on("</li><li>").join(lEvaluatedNodeAction.getNamespacePrecedenceList()));
      lItemDebugInfo.append("</li></ol></p>");
      lItemDebugInfo.append("<p>");
      lItemDebugInfo.append(lEvaluatedNodeAction.getIdentityInformation());
      lItemDebugInfo.append("</p>");
      pSerialiser.addDebugInformation(lItemDebugInfo.toString());
    }

    if (lEvaluatedNodeAction != null && lEvaluatedNodeAction.getVisibility() != NodeVisibility.DENIED) {
      pSerialiser.getWidgetBuilder(lEvaluatedNodeAction.getWidgetBuilderType()).buildWidget(pSerialisationContext, pSerialiser, lEvaluatedNodeAction);
    }
  }
}

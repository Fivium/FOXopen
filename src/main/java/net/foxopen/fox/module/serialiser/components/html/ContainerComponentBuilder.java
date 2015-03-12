package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedContainerPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import org.apache.commons.lang3.StringEscapeUtils;


public class ContainerComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedContainerPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedContainerPresentationNode> INSTANCE = new ContainerComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedContainerPresentationNode> getInstance() {
    return INSTANCE;
  }

  private ContainerComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedContainerPresentationNode pEvalContainerNode) {

    if (pSerialiser.isInBody() && pSerialisationContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.HTML_GEN_DEBUG)) {
      pSerialiser.addDebugInformation("<strong>Container Start:</strong> " + StringEscapeUtils.escapeHtml4(pEvalContainerNode.toString()));
    }

    processChildren(pSerialisationContext, pSerialiser, pEvalContainerNode);

    if (pSerialiser.isInBody() && pSerialisationContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.HTML_GEN_DEBUG)) {
      pSerialiser.addDebugInformation("<strong>Container End:</strong> " + StringEscapeUtils.escapeHtml4(pEvalContainerNode.toString()));
    }
  }
}

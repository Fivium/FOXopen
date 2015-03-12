package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;


public class BufferComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedBufferPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedBufferPresentationNode> INSTANCE = new BufferComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedBufferPresentationNode> getInstance() {
    return INSTANCE;
  }

  private BufferComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedBufferPresentationNode pEvalBufferNode) {

    if(pEvalBufferNode.getEvalClientVisibilityRule() != null) {
      //Wrapping div for contents hiding
      //TODO PN make this better (shouldn't really wrap arbitrary stuff in a div, might break the page)
      pSerialiser.append("<div data-xfid=\"" + pEvalBufferNode.getSkipLinkID() + "\" class=\"" + pEvalBufferNode.getEvalClientVisibilityRule().getInitialCSSClass() + "\">");
    }

    if (!XFUtil.isNull(pEvalBufferNode.getRegionTitle())) {
      pSerialiser.append("<a id=\"");
      pSerialiser.append(pEvalBufferNode.getSkipLinkID());
      pSerialiser.append("\" class=\"skipTarget\"></a>");
    }

    processChildren(pSerialisationContext, pSerialiser, pEvalBufferNode);

    if(pEvalBufferNode.getEvalClientVisibilityRule() != null) {
      //Close the wrapping div
      pSerialiser.append("</div>");
    }
  }
}

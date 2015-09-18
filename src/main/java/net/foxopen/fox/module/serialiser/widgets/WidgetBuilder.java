package net.foxopen.fox.module.serialiser.widgets;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.SerialisationContext;


public abstract class WidgetBuilder<OS extends OutputSerialiser, EN extends EvaluatedNode> {
  /**
   * Will this widget try and set out a prompt?
   *
   * @param pEvalNode
   * @return Boolean value for if the widget will try and set out a prompt
   */
  public boolean hasPrompt(EN pEvalNode){
    return pEvalNode.hasPrompt();
  }

  /**
   * Default prompt set out. If the current widget will have a prompt it uses mustache and deald with the mand/opt display
   *
   * @param pSerialiser
   * @param pEvalNode
   */
  public abstract void buildPrompt(SerialisationContext pSerialisationContext, OS pSerialiser, EN pEvalNode);

  /**
   * Actually set out a widget to pSerialiser based off information on pEvalNodeInfo
   * TODO new class; WidgetBuilderItem - that deals with the FieldMgr (don't have to cater for complex widget builders)
   * all item WidgetBuilders should then extend that.
   * @param pSerialiser
   * @param pEvalNode
   */
  public void buildWidget(SerialisationContext pSerialisationContext, OS pSerialiser, EN pEvalNode) {
    pEvalNode.getFieldMgr().prepareForSetOut();
    buildWidgetInternal(pSerialisationContext, pSerialiser, pEvalNode);
  }

  protected abstract void buildWidgetInternal(SerialisationContext pSerialisationContext, OS pSerialiser, EN pEvalNode);

}

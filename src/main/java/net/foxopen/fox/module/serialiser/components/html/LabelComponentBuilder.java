package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedLabelPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;

import java.util.HashMap;
import java.util.Map;


public class LabelComponentBuilder
extends ComponentBuilder<HTMLSerialiser, EvaluatedLabelPresentationNode> {

  private static final ComponentBuilder<HTMLSerialiser, EvaluatedLabelPresentationNode> INSTANCE = new LabelComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedLabelPresentationNode> getInstance() {
    return INSTANCE;
  }

  private LabelComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedLabelPresentationNode pEvalNode) {

    Map<String, Object> lTemplateVars = new HashMap<>();
    if (pEvalNode.getForTargetElementOrNull() != null) {
      String pExternalTargetID = pSerialisationContext.getFieldSet().getExternalFoxId(pEvalNode.getForTargetElementOrNull());
      if (!XFUtil.isNull(pExternalTargetID)) {
        lTemplateVars.put("For", pExternalTargetID);
      }
    }
    lTemplateVars.put("Open", true);

    MustacheFragmentBuilder.applyMapToTemplate("html/LabelComponent.mustache", lTemplateVars, pSerialiser.getWriter());

    processChildren(pSerialisationContext, pSerialiser, pEvalNode);

    lTemplateVars.remove("Open");
    lTemplateVars.put("Close", true);

    MustacheFragmentBuilder.applyMapToTemplate("html/LabelComponent.mustache", lTemplateVars, pSerialiser.getWriter());
  }
}

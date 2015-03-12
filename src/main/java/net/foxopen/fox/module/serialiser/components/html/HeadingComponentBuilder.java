package net.foxopen.fox.module.serialiser.components.html;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHeadingPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;


public class HeadingComponentBuilder
extends ComponentBuilder<HTMLSerialiser, EvaluatedHeadingPresentationNode> {

  private static final ComponentBuilder<HTMLSerialiser, EvaluatedHeadingPresentationNode> INSTANCE = new HeadingComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedHeadingPresentationNode> getInstance() {
    return INSTANCE;
  }

  private HeadingComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedHeadingPresentationNode pEvalNode) {

    Map<String, Object> lTemplateVars = new HashMap<>();
    lTemplateVars.put("Level", pEvalNode.getLevel());
    lTemplateVars.put("Class", pEvalNode.getClasses());
    lTemplateVars.put("Style", pEvalNode.getStyles());
    lTemplateVars.put("Open", true);

    MustacheFragmentBuilder.applyMapToTemplate("html/HeadingComponent.mustache", lTemplateVars, pSerialiser.getWriter());

    processChildren(pSerialisationContext, pSerialiser, pEvalNode);

    lTemplateVars.remove("Open");
    lTemplateVars.put("Close", true);

    MustacheFragmentBuilder.applyMapToTemplate("html/HeadingComponent.mustache", lTemplateVars, pSerialiser.getWriter());
  }
}

package net.foxopen.fox.module.serialiser.components.html;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedGridPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;


public class GridComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new GridComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private GridComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    EvaluatedGridPresentationNode lEvalNode = (EvaluatedGridPresentationNode)pEvalNode;

    Map<String, Object> lTemplateVars = new HashMap<>();
    lTemplateVars.put("Class", "container" + (!XFUtil.isNull(lEvalNode.getClasses()) ? " " + lEvalNode.getClasses() : ""));
    lTemplateVars.put("Style", lEvalNode.getStyles());
    lTemplateVars.put("Open", true);

    MustacheFragmentBuilder.applyMapToTemplate("html/OpenCloseDivComponent.mustache", lTemplateVars, pSerialiser.getWriter());

    processChildren(pSerialisationContext, pSerialiser, pEvalNode);

    lTemplateVars.remove("Open");
    lTemplateVars.put("Close", true);

    MustacheFragmentBuilder.applyMapToTemplate("html/OpenCloseDivComponent.mustache", lTemplateVars, pSerialiser.getWriter());
  }
}

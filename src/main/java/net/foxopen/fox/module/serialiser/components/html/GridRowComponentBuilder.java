package net.foxopen.fox.module.serialiser.components.html;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedGridRowPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;


public class GridRowComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new GridRowComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private GridRowComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    EvaluatedGridRowPresentationNode lEvalNode = (EvaluatedGridRowPresentationNode)pEvalNode;

    Map<String, Object> lTemplateVars = new HashMap<>();
    lTemplateVars.put("Class", "row" + (!XFUtil.isNull(lEvalNode.getClasses()) ? " " + lEvalNode.getClasses() : ""));
    lTemplateVars.put("Style", lEvalNode.getStyles());
    lTemplateVars.put("Open", true);

    MustacheFragmentBuilder.applyMapToTemplate("html/OpenCloseDivComponent.mustache", lTemplateVars, pSerialiser.getWriter());

    processChildren(pSerialisationContext, pSerialiser, pEvalNode);

    lTemplateVars.remove("Open");
    lTemplateVars.put("Close", true);

    MustacheFragmentBuilder.applyMapToTemplate("html/OpenCloseDivComponent.mustache", lTemplateVars, pSerialiser.getWriter());
  }
}

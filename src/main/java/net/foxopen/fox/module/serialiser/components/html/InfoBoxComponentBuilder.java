package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.NotificationDisplayType;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedInfoBoxPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.TempSerialiser;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;

import java.util.HashMap;
import java.util.Map;


public class InfoBoxComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new InfoBoxComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  /**
   * @param pDisplayType Display type to get suffix for.
   * @return Gets the suffix to be appended to the "info-box-" class name prefix for the given display type.
   */
  public static String getInfoBoxClassNameSuffix(NotificationDisplayType pDisplayType) {
    return pDisplayType.toString().toLowerCase();
  }

  private InfoBoxComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    EvaluatedInfoBoxPresentationNode lEvalNode = (EvaluatedInfoBoxPresentationNode)pEvalNode;

    Map<String, Object> lTemplateVars = new HashMap<>();
    lTemplateVars.put("Class", "info-box info-box-" + getInfoBoxClassNameSuffix(lEvalNode.getInfoBoxType()) + (!XFUtil.isNull(lEvalNode.getClasses()) ? " " + lEvalNode.getClasses() : ""));
    lTemplateVars.put("Style", lEvalNode.getStyles());
    lTemplateVars.put("Open", true);

    if (lEvalNode.getEvaluatedTitleContainer() != null) {
      TempSerialiser lTempSerialiser = pSerialiser.getTempSerialiser();
      processChildren(pSerialisationContext, lTempSerialiser, lEvalNode.getEvaluatedTitleContainer());
      lTemplateVars.put("Title", lTempSerialiser.getOutput());
      lTemplateVars.put("TitleLevel", lEvalNode.getTitleLevel());
    }

    MustacheFragmentBuilder.applyMapToTemplate("html/InfoBoxComponent.mustache", lTemplateVars, pSerialiser.getWriter());

    processChildren(pSerialisationContext, pSerialiser, lEvalNode.getEvaluatedContentContainer());

    lTemplateVars.remove("Open");
    lTemplateVars.put("Close", true);
    MustacheFragmentBuilder.applyMapToTemplate("html/InfoBoxComponent.mustache", lTemplateVars, pSerialiser.getWriter());
  }
}

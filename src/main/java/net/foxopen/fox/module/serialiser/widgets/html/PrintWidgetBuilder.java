package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.Map;

/**
 * Generate a html button with some javascript to print the page
 */
public class PrintWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new PrintWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/ButtonWidget.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private PrintWidgetBuilder () {
  }

  @Override
  public boolean hasPrompt(EvaluatedNode pEvalNode) {
    return false;
  }

  @Override
  public void buildPrompt(HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (!pEvalNode.isRunnable() && !pEvalNode.isPlusWidget()) {
      return;
    }

    Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialiser, pEvalNode);
    lTemplateVars.put("AltText", lTemplateVars.get("PromptText"));
    lTemplateVars.put("Value", lTemplateVars.get("PromptText"));
    lTemplateVars.put("InputType", "button");

    if (pEvalNode.isRunnable()) {
      lTemplateVars.put("ActionJS", "window.print();");
    }
    else {
      lTemplateVars.put("Disabled", true);
    }

    String lImageURL = pEvalNode.getStringAttribute(NodeAttribute.IMAGE_URL);

    if (lImageURL != null) {
      lTemplateVars.put("ButtonImageURL", pSerialisationContext.getImageURI(lImageURL));
    }
    else {
      lTemplateVars.put("Class", Joiner.on(" ").skipNulls().join(lTemplateVars.get("Class"), "button"));
    }

    MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    pSerialiser.addHint(pEvalNode.getHint(), pEvalNode.getFieldMgr().getExternalFieldName(), false);
  }
}

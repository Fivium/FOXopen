package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.Map;


public class ButtonWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new ButtonWidgetBuilder("button");
  private static final String MUSTACHE_TEMPLATE = "html/ButtonWidget.mustache";

  private final String mInputType;

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  public ButtonWidgetBuilder (String pInputType) {
    mInputType = pInputType;
  }

  @Override
  public boolean hasPrompt(EvaluatedNode pEvalNode) {
    return false;
  }

  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (!pEvalNode.isRunnable() && !pEvalNode.isPlusWidget()) {
      return;
    }

    Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);
    lTemplateVars.put("InputType", mInputType);
    lTemplateVars.put("Value", pEvalNode.getPrompt().getString());
    lTemplateVars.put("AltText", pEvalNode.getPrompt().getString());

    if (!pEvalNode.isRunnable()) {
      lTemplateVars.put("Disabled", true);
    }

    String lImageURL = pEvalNode.getStringAttribute(NodeAttribute.IMAGE_URL);
    if (lImageURL != null) {
      lTemplateVars.put("ButtonImageURL", pSerialisationContext.getImageURI(lImageURL));
      lTemplateVars.put("Class", "image-button");
    }
    else {
      String lNoPromptActionClass = null;
      if(XFUtil.isNull(pEvalNode.getPrompt().getString())) {
        lNoPromptActionClass = "no-prompt-action";
      }
      lTemplateVars.put("Class",  Joiner.on(" ").skipNulls().join(lTemplateVars.get("Class"), "button", lNoPromptActionClass));
    }

    MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    pSerialiser.addHint(pSerialisationContext, pEvalNode.getHint(), pEvalNode.getFieldMgr().getExternalFieldName(), false);
  }
}

package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.Map;


public class LinkWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new LinkWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/LinkWidget.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private LinkWidgetBuilder () {
  }

  @Override
  public boolean hasPrompt(EvaluatedNode pEvalNodeInfo) {
    return false;
  }

  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNodeInfo) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (!pEvalNode.isRunnable() && !pEvalNode.isPlusWidget()) {
      return;
    }

    Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);

    // Links without prompts can be hard for developers to spot, so hard error as they don't have any purpose and could lead to actions being set out by mistake
    if (XFUtil.isNull(lTemplateVars.get("PromptText"))) {
      throw new ExInternal("Link widget found with no prompt, this is invisible on the page but the action is still there and likely not intended: " + pEvalNode.getIdentityInformation());
    }

    if (!pEvalNode.isRunnable()) {
      lTemplateVars.put("ReadOnly", true);
      lTemplateVars.put("Class", Joiner.on(" ").skipNulls().join(lTemplateVars.get("Class"), "disabledLink"));
    }

    MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());

    pSerialiser.addHint(pSerialisationContext, pEvalNode.getHint(), pEvalNode.getFieldMgr().getExternalFieldName(), false);
  }
}

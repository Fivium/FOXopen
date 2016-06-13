package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.OutputHint;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.html.MailtoWidgetHelper;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.Map;


public class MailToWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new MailToWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private MailToWidgetBuilder () {
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
    String lEmailAddress = pEvalNode.getStringAttribute(NodeAttribute.EMAIL);
    if (XFUtil.isNull(lEmailAddress)) {
      throw new ExInternal("Cannot have a mailto widget without an email attribute: " + pEvalNode);
    }

    String lEmailAddressCC = pEvalNode.getStringAttribute(NodeAttribute.CC);
    String lSubject = pEvalNode.getStringAttribute(NodeAttribute.SUBJECT);
    String lImageURL = pEvalNode.getStringAttribute(NodeAttribute.IMAGE_URL);
    OutputHint lHint = pEvalNode.getHint();

    Map<String, Object> lGenericTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);
    MailtoWidgetHelper.buildWidget(pSerialisationContext, pSerialiser,
      lGenericTemplateVars,
      lEmailAddress, lEmailAddressCC, lSubject, (String)lGenericTemplateVars.get("PromptText"), lImageURL,
      lHint);
  }
}

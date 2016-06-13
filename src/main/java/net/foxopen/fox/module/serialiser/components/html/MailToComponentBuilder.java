package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.OutputHint;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedMailToPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;

import java.util.HashMap;


public class MailToComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new MailToComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private MailToComponentBuilder () {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    EvaluatedMailToPresentationNode lEvalNode = (EvaluatedMailToPresentationNode)pEvalNode;

    String lEmailAddress = lEvalNode.getEmailAddress();

    String lEmailAddressCC = lEvalNode.getCarbonCopyCSVList();
    String lSubject = lEvalNode.getSubject();
    String lPrompt = lEvalNode.getPrompt();
    String lImageURL = lEvalNode.getImageURL();
    OutputHint lHint = lEvalNode.getHint();

    HashMap<String, Object> lTemplateVars = new HashMap<>();
    if (lHint != null) {
      lTemplateVars.put("HintContentID", lHint.getHintContentID());
    }

    MailtoWidgetHelper.buildWidget(pSerialisationContext, pSerialiser,
      lTemplateVars,
      lEmailAddress, lEmailAddressCC, lSubject, lPrompt, lImageURL,
      lHint);
  }
}

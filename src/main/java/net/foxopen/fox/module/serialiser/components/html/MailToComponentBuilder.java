package net.foxopen.fox.module.serialiser.components.html;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedMailToPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.xhtml.QueryStringSeparatorProvider;


public class MailToComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new MailToComponentBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/MailToWidget.mustache";

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private MailToComponentBuilder () {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    EvaluatedMailToPresentationNode lEvalNode = (EvaluatedMailToPresentationNode)pEvalNode;
    try {
      String lEmailAddress = lEvalNode.getEmailAddress();
      if (lEmailAddress.startsWith("mailto:")) {
        lEmailAddress = lEmailAddress.substring(7, lEmailAddress.length());
      }

      String lPrompt = lEvalNode.getPrompt();
      if (XFUtil.isNull(lPrompt)) {
        lPrompt = lEmailAddress;
      }

      String lEmailAddressCC = lEvalNode.getCarbonCopyCSVList();

      String lSubject = lEvalNode.getSubject();

      QueryStringSeparatorProvider lQSProvider = new QueryStringSeparatorProvider();
      // Always add the "to" address, everything else is optional
      StringBuilder lMailToLink = new StringBuilder("mailto:");
      lMailToLink.append(lEmailAddress);

      // CC List, semicolon separated
      if (XFUtil.exists(lEmailAddressCC)) {
        lMailToLink.append(lQSProvider.getSeparator());
        lMailToLink.append("cc=");
        lMailToLink.append(URLEncoder.encode(lEmailAddressCC, "UTF-8"));
      }

      // Subject
      if (XFUtil.exists(lSubject)) {
        lMailToLink.append(lQSProvider.getSeparator());
        lMailToLink.append("subject=");
        lMailToLink.append(URLEncoder.encode(lSubject, "UTF-8"));
      }

      Map<String, Object> lTemplateVars = new HashMap<>();
      lTemplateVars.put("MailToLink", lMailToLink);
      lTemplateVars.put("Prompt", lPrompt);


      String lImageURL = lEvalNode.getImageURL();
      if (!XFUtil.isNull(lImageURL)) {
        lTemplateVars.put("ImageURL", pSerialisationContext.getImageURI(lImageURL));
      }

      String lHint = lEvalNode.getHint();
      if (!XFUtil.isNull(lHint)) {
        lTemplateVars.put("HintTitle", lPrompt);
        lTemplateVars.put("HintContent", lHint);
        lTemplateVars.put("Class", "hint");
      }

      MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    }
    catch (UnsupportedEncodingException e) {
      throw new ExInternal("Failed to URL-encode mailto fields", e);
    }
  }
}

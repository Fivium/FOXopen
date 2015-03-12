package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.OutputHint;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.xhtml.QueryStringSeparatorProvider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;


public class MailToWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new MailToWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/MailToWidget.mustache";

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
  public void buildPrompt(HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    try {
      String lEmailAddress = pEvalNode.getStringAttribute(NodeAttribute.EMAIL);
      if (XFUtil.isNull(lEmailAddress)) {
        throw new ExInternal("Cannot have a mailto widget without an email attribute: " + pEvalNode);
      }
      else {
        if (lEmailAddress.startsWith("mailto:")) {
          lEmailAddress = lEmailAddress.substring(7, lEmailAddress.length());
        }
      }

      String lEmailAddressCC = pEvalNode.getStringAttribute(NodeAttribute.CC);

      String lSubject = pEvalNode.getStringAttribute(NodeAttribute.SUBJECT);

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

      Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialiser, pEvalNode);
      lTemplateVars.put("MailToLink", lMailToLink);
      lTemplateVars.put("PromptText", XFUtil.nvl(lTemplateVars.get("PromptText"), lEmailAddress));

      String lImageURL = pEvalNode.getStringAttribute(NodeAttribute.IMAGE_URL);
      if (!XFUtil.isNull(lImageURL)) {
        lTemplateVars.put("ImageURL", pSerialisationContext.getImageURI(lImageURL));
      }

      OutputHint lHint = pEvalNode.getHint();
      if (lHint != null) {
        lTemplateVars.put("HintTitle", pSerialiser.getSafeStringAttribute(lHint.getTitle()));
        lTemplateVars.put("HintContent", pSerialiser.getSafeStringAttribute(lHint.getContent()));
        lTemplateVars.put("Class", Joiner.on(" ").skipNulls().join(lTemplateVars.get("Class"), "hint"));
      }

      MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    }
    catch (UnsupportedEncodingException e) {
      throw new ExInternal("Failed to URL-encode mailto fields", e);
    }
  }
}

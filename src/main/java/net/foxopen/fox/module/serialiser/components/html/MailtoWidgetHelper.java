package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.OutputHint;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.xhtml.QueryStringSeparatorProvider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class MailtoWidgetHelper {
  private static final String MUSTACHE_TEMPLATE = "html/MailToWidget.mustache";

  /**
   * Construct a mailto for a given MenuOutActionProvider and serialiser
   *
   * @param pSerialisationContext
   * @param pSerialiser
   */
  public static void buildWidget(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser,
                                 Map<String, Object> pTemplateVars,
                                 String pEmailAddress, String pEmailAddressCC, String pSubject, String pPrompt, String pImageURL,
                                 OutputHint pHint) {
    QueryStringSeparatorProvider lQSProvider = new QueryStringSeparatorProvider();
    // Always add the "to" address, everything else is optional
    StringBuilder lMailToLink = new StringBuilder("mailto:");
    lMailToLink.append(encodeTextRFC6068(pEmailAddress));

    // CC List, semicolon separated
    if (XFUtil.exists(pEmailAddressCC)) {
      lMailToLink.append(lQSProvider.getSeparator());
      lMailToLink.append("cc=");
      lMailToLink.append(encodeTextRFC6068(pEmailAddressCC));
    }

    // Subject
    if (XFUtil.exists(pSubject)) {
      lMailToLink.append(lQSProvider.getSeparator());
      lMailToLink.append("subject=");
      lMailToLink.append(encodeTextRFC6068(pSubject));
    }

    pTemplateVars.put("MailToLink", lMailToLink);
    pTemplateVars.put("Prompt", XFUtil.nvl(pPrompt, pEmailAddress));

    if (!XFUtil.isNull(pImageURL)) {
      pTemplateVars.put("ImageURL", pSerialisationContext.getImageURI(pImageURL));
    }

    String lFieldName = null;
    if (pHint != null) {
      // TODO - This is a fairly hacky workaround for component mailtos with hints that need to show inline
      // Need to have a longer think about hints on non-ENIs at some point, but mailtos aren't used all that often so some messy code behind them isn't the worst thing
      if (XFUtil.isNull(pTemplateVars.get("FieldName"))) {
        lFieldName = pHint.getHintID().substring("hint".length());
        pTemplateVars.put("FieldName", lFieldName);
      }
      else {
        lFieldName = (String)pTemplateVars.get("FieldName");
      }
    }

    MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, pTemplateVars, pSerialiser.getWriter());
    pSerialiser.addHint(pSerialisationContext, pHint, lFieldName, false);
  }

  /**
   * Encode text so that it conforms to the RFC6068 used for mailto attributes, See https://tools.ietf.org/html/rfc6068#page-8
   * for more information
   *
   * @param pText Text to encode
   * @return Encoded string
   */
  private static String encodeTextRFC6068(String pText) {
    try {
      return URLEncoder.encode(pText, "UTF-8").replace("+", "%20");
    }
    catch (UnsupportedEncodingException ex) {
      throw new ExInternal("Failed to URL-encode mailto fields", ex);
    }
  }
}

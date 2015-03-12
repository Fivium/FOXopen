package net.foxopen.fox.module.fieldset.transformer;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;

public class PasswordTransformer
extends DefaultTransformer {

  private static PasswordTransformer INSTANCE = new PasswordTransformer();

  public static PasswordTransformer instance() {
    return INSTANCE;
  }

  private PasswordTransformer() {}

  public String applyOutboundTransform(DOM pSourceElement) {
    return XFUtil.obfuscateValue(super.applyOutboundTransform(pSourceElement));
  }

  public String applyInboundTransform(String pPostedValue) {
    return super.cleanXMLCharacters(pPostedValue);
  }
}

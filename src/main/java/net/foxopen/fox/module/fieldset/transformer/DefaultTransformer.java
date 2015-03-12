package net.foxopen.fox.module.fieldset.transformer;

import com.google.common.base.CharMatcher;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.xhtml.InputMask;

/**
 * Standard implementation of a FieldTransformer which is capable of adding and removing an optional input mask.
 */
public abstract class DefaultTransformer
implements FieldTransformer {

  /**
   * Disallow anything < 0x0020 (space) apart from horizontal tab (0x0009), newline (0x000A), or carriage return (0x00D)
   * See XML spec: http://www.w3.org/TR/xml/#charsets
   * Note: Guava does not deal with supplementary (4 byte) characters but all their bytes are above '0xD800' which is outside the ranges specified here
   */
  private static final CharMatcher INVALID_XML_CHARS =
    CharMatcher.inRange('\0', '\u0008').or(CharMatcher.inRange('\u000b', '\u000c').or(CharMatcher.inRange('\u000e', '\u001f')));

  /** Can be null */
  private final String mInputMaskName;

  DefaultTransformer() {
    mInputMaskName = null;
  }

  DefaultTransformer(String pInputMaskName) {
    mInputMaskName = pInputMaskName;
  }

  protected final String getDOMValue(DOM pSourceElement) {
    return pSourceElement.value();
  }

  protected final String applyInputMask(String pValue) {
    // TODO - PN - InputMask isn't great, think about this (only used for comma mask?)
    InputMask lInputMask = InputMask.getInputMaskByName(mInputMaskName);
    if(lInputMask != null) {
      return lInputMask.applyMask(pValue);
    }
    else {
      return pValue;
    }
  }

  protected final String removeInputMask(String pValue) {
    // TODO - PN - InputMask isn't great, think about this (only used for comma mask?)
    InputMask lInputMask = InputMask.getInputMaskByName(mInputMaskName);
    if(lInputMask != null) {
      return lInputMask.removeMask(pValue);
    }
    else {
      return pValue;
    }
  }

  @Override
  public String applyOutboundTransform(DOM pSourceElement) {
    return applyInputMask(getDOMValue(pSourceElement));
  }

  /**
   * Tests if this transformer should trim the inbound posted string. Default is true. Subclasses may overload this method if they
   * do not want to trim the posted string.
   * @return
   */
  protected boolean trimPostedValue() {
    return true;
  }

  protected final String cleanXMLCharacters(String pPostedValue) {
    return INVALID_XML_CHARS.removeFrom(pPostedValue);
  }

  protected final String processPostedValue(String pPostedValue) {
    //Trim the value if the transformer wants to (default is to trim)
    if(trimPostedValue()) {
      pPostedValue = pPostedValue.trim();
    }

    //Clean invalid, low-range ASCII characters before they go into a DOM
    pPostedValue = cleanXMLCharacters(pPostedValue);

    //Legacy behaviour was to replace windows newlines to unix newlines
    return pPostedValue.replaceAll("\r\n", "\n");
  }

  @Override
  public String applyInboundTransform(String pPostedValue) {
    return removeInputMask(processPostedValue(pPostedValue));
  }
}

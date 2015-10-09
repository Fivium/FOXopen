package net.foxopen.fox.module.facet;

import java.util.HashMap;
import java.util.Map;

/**
 * Property object representing options which can be set for a modal popover.
 */
public class ModalPopoverOptions {

  public enum PopoverSize {
    LARGE, REGULAR, SMALL
  }

  private final String mTitle;
  private final PopoverSize mSize;
  private final String mCSSClass;

  public ModalPopoverOptions(String pTitle, PopoverSize pSize, String pCSSClass) {
    mTitle = pTitle;
    mSize = pSize;
    mCSSClass = pCSSClass;
  }

  /**
   * @return Gets a property map representing these popover options, for application into the modal popover Mustache template.
   */
  public Map<String, Object> asMustacheTemplatePropertyMap() {
    Map<String, Object> lProperties = new HashMap<>();
    //Note: map keys correspond to mustache tag names
    lProperties.put("title", mTitle);
    lProperties.put("size", mSize.toString().toLowerCase());
    lProperties.put("cssClass", mCSSClass);

    return lProperties;
  }
}

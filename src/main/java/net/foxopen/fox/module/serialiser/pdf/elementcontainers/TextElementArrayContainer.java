package net.foxopen.fox.module.serialiser.pdf.elementcontainers;

import com.itextpdf.text.Element;
import com.itextpdf.text.TextElementArray;

/**
 * Container to add elements to a {@link com.itextpdf.text.TextElementArray}
 */
public class TextElementArrayContainer implements ElementContainer {
  private final TextElementArray mTextElementArray;

  /**
   * Create a container to add elements to a text element array
   * @param pTextElementArray The text element array elements should be added to
   */
  public TextElementArrayContainer(TextElementArray pTextElementArray) {
    mTextElementArray = pTextElementArray;
  }

  @Override
  public boolean isSuppressNewPageTemplates() {
    return false;
  }

  /**
   * Add an element to the text element array
   * @param pChildElement The element to be added
   */
  @Override
  public void addChildElement(Element pChildElement) {
    mTextElementArray.add(pChildElement);
  }
}

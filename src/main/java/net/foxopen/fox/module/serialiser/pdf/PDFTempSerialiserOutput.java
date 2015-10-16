package net.foxopen.fox.module.serialiser.pdf;

import java.util.LinkedList;
import java.util.List;

/**
 * The output of a {@link PDFTempSerialiser}
 */
public class PDFTempSerialiserOutput {
  private final List<String> mSerialisedHTMLTags = new LinkedList<>();

  /**
   * Adds the name of a HTML tag to a list of serialised HTML tags. During temporary serialisation, any HTML tags
   * encountered should be recorded using this method.
   * @param pTag The name of the HTML tag
   */
  public void addSerialisedHTMLTag(String pTag) {
    mSerialisedHTMLTags.add(pTag);
  }

  /**
   * Returns a list of HTML tag names encountered during temporary serialisation
   * @return A list of HTML tag names encountered during temporary serialisation
   */
  public List<String> getSerialisedHTMLTags() {
    return mSerialisedHTMLTags;
  }
}

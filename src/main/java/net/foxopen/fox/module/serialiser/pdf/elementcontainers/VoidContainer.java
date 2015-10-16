package net.foxopen.fox.module.serialiser.pdf.elementcontainers;

import com.itextpdf.text.Element;

/**
 * A container that discards any elements added to it. This is used for when elements should not actually be added to
 * any concrete, e.g. the temporary PDF serialiser does not need to do anything with the top level elements.
 */
public class VoidContainer implements ElementContainer {
  /**
   * Add an element. Any elements added are discarded.
   * @param pChildElement The element to be ignored.
   */
  @Override
  public void addChildElement(Element pChildElement) {
  }
}

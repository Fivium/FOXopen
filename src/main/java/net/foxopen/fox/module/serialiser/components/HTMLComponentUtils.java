package net.foxopen.fox.module.serialiser.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contains utility functions for HTML component builders
 */
public class HTMLComponentUtils {
  /**
   * List of Void Elements, html tags to force leaving open, no self close and no closing tag needed. These should throw
   * an error if they have children also.
   *
   * @see <a href="http://www.w3.org/TR/html-markup/syntax.html#void-element">http://www.w3.org/TR/html-markup/syntax.html#void-element</a>
   */
  private static final List<String> VOID_ELEMENTS = new ArrayList<>(Arrays.asList("br", "hr", "img", "input", "link", "meta", "area", "base", "col", "command", "embed", "keygen",
                                                                                  "param", "source", "track", "wbr"));

  public static boolean isVoidElement(String lTag) {
    return VOID_ELEMENTS.contains(lTag);
  }
}

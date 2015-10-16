package net.foxopen.fox.module.serialiser.pdf.elementattributes;

import net.foxopen.fox.ex.ExInternal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

/**
 * Manages the stack of element attributes
 */
public class ElementAttributeManager {
  private final Deque<ElementAttributes> mAttributes = new ArrayDeque<>();

  /**
   * Push element attributes onto the element attribute stack
   * @param pAttributes The element attributes
   */
  public void pushAttributes(ElementAttributes pAttributes) {
    mAttributes.push(pAttributes);
  }

  /**
   * Pop element attributes from the element attribute stack
   * @throws ExInternal If the stack is empty
   */
  public void popAttributes() throws ExInternal {
    try {
      ElementAttributes lAttributes = mAttributes.pop();
    }
    catch (NoSuchElementException e) {
      throw new ExInternal("Cannot end attributes as no attributes exist", e);
    }
  }

  /**
   * Return the element attributes at the top of the stack
   * @return The element attributes at the top of the stack
   * @throws ExInternal If the stack is empty
   */
  public ElementAttributes getCurrentAttributes() throws ExInternal {
    try {
      return mAttributes.element();
    }
    catch (NoSuchElementException e) {
      throw new ExInternal("Cannot get current attributes as no attributes exist", e);
    }
  }

  /**
   * Return whether or not element attributes exist on the stack
   * @return True when element attributes exist, false when the stack is empty
   */
  public boolean hasAttributes() {
    return !mAttributes.isEmpty();
  }
}

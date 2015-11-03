package net.foxopen.fox.module.serialiser.pdf.elementcontainers;

import net.foxopen.fox.ex.ExInternal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

/**
 * Manages the stack of element containers
 */
public class ElementContainerManager {
  private final Deque<ElementContainer> mContainers = new ArrayDeque<ElementContainer>();

  /**
   * Start an element container
   * @param pContainer The element container
   */
  public void startContainer(ElementContainer pContainer) {
    mContainers.push(pContainer);
  }

  /**
   * End the current element container
   * @throws ExInternal If no container exists
   */
  public void endContainer() throws ExInternal {
    try {
      ElementContainer lContainer = mContainers.pop();
      lContainer.onEndContainer();
    }
    catch (NoSuchElementException e) {
      throw new ExInternal("Cannot end container as no container exists", e);
    }
  }

  /**
   * Return whether a container exists
   * @return True when a container exists, false when the stack is empty
   */
  public boolean hasContainer() {
    return !mContainers.isEmpty();
  }

  /**
   * Return the current container
   * @return The current container
   * @throws ExInternal If no container exists
   */
  public ElementContainer getCurrentContainer() throws ExInternal {
    try {
      return mContainers.element();
    }
    catch (NoSuchElementException e) {
      throw new ExInternal("Cannot get current container as no container exists", e);
    }
  }

  /**
   * Returns true if any containers are currently suppressing new page templates. This method must be used instead of
   * checking only the current container, because the current container may not be suppressing new page templates but an
   * ancestor may be.
   * @return True if any containers are currently suppressing new page templates
   */
  public boolean isSuppressNewPageTemplates() {
    return mContainers.stream().anyMatch(ElementContainer::isSuppressNewPageTemplates);
  }
}

package net.foxopen.fox.module.facet;

import net.foxopen.fox.thread.persistence.PersistenceContext;

/**
 * ModuleFacet representing a modal popover created from an fm:show-popover command. A module call will have at most 1
 * modal popover. The existence of the popover, the options with which it was constructed and its current scroll position
 * all form part of the stateful information which needs to be maintained by the facet.
 */
public interface ModalPopover
extends ModuleFacet {

  /**
   * @return Gets the name of the buffer which forms the content part of this modal popover.
   */
  String getBufferName();

  /**
   * @return Gets the FOXID of the attach point for the modal popover's buffer.
   */
  String getBufferAttachFoxId();

  /**
   * Sets the scroll position of this modal popover to be serialised by this facet. This will typically be sent from
   * the HTML form.
   * @param pPersistenceContext Current PersistenceContext, for marking the facet as requiring an update.
   * @param pScrollPosition New scroll positition of the popover.
   */
  void setScrollPosition(PersistenceContext pPersistenceContext, int pScrollPosition);

  /**
   * @return Gets the current scroll position of this modal popover.
   */
  int getScrollPosition();

  /**
   * @return Gets the immutable options which this modal was created with.
   */
  ModalPopoverOptions getModalPopoverOptions();

}

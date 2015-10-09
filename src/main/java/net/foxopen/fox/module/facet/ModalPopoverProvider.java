package net.foxopen.fox.module.facet;

/**
 * ModuleFacetProvider which controls the current module call's {@link ModalPopover}. A module call can only have 1 popover
 * active at any given time, so this provider only needs to preserve the state of at most 1 popover.
 */
public interface ModalPopoverProvider
extends ModuleFacetProvider {

  /**
   * Creates a ModalPopover which will be immediately displayed on the screen.
   * @param pBufferName Name of the buffer containing the popover's content.
   * @param pBufferAttachFoxId FOX ID of the attach point for evaluating the popover's buffer.
   * @param pModalOptions Display options for the new modal popover.
   * @return The new ModalPopover.
   */
  ModalPopover showPopover(String pBufferName, String pBufferAttachFoxId, ModalPopoverOptions pModalOptions);

  /**
   * @return Gets the current modal popover for the FacetProvider's module call, or null if no popover is displayed.
   */
  ModalPopover getCurrentPopoverOrNull();

  /**
   * Closes the current modal popover, if one is open. Otherwise, no action is taken.
   */
  void closeCurrentPopover();

}

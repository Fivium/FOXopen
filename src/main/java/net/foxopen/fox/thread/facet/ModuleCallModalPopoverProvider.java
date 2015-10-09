package net.foxopen.fox.thread.facet;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.facet.ModalPopover;
import net.foxopen.fox.module.facet.ModalPopoverOptions;
import net.foxopen.fox.module.facet.ModalPopoverProvider;
import net.foxopen.fox.module.facet.ModuleFacetType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.track.Track;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Standard implementation of a ModalPopoverProvider.
 */
public class ModuleCallModalPopoverProvider
extends ModuleCallFacetProvider<ModalPopover>
implements ModalPopoverProvider {

  private static final Iterator<String> ID_ITERATOR = XFUtil.getUniqueIterator();

  public static ModuleCallFacetProvider.Builder getBuilder() {
    return new Builder();
  }

  private ModuleCallModalPopoverProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall, Map<String, ModalPopover> pFacetKeyToFacetMap) {
    super(pPersistenceContext, pModuleCall, pFacetKeyToFacetMap);
  }

  @Override
  public ModalPopover showPopover(String pBufferName, String pBufferAttachFoxId, ModalPopoverOptions pModalOptions) {
    ModalPopover lCurrentPopover = getCurrentPopoverOrNull();
    ModalPopover lNewPopover = new ModuleCallModalPopover(ID_ITERATOR.next(), getModuleCall().getCallId(), pBufferName, pBufferAttachFoxId, pModalOptions);

    if(lCurrentPopover != null) {
      updateExistingFacet(lNewPopover);
    }
    else {
      registerNewFacet(lNewPopover);
    }

    return lNewPopover;
  }

  @Override
  public ModalPopover getCurrentPopoverOrNull() {
    //Note: there should only ever be at most 1 modal facet in this provider
    if(getAllFacets().size() > 0) {
      return getAllFacets().iterator().next();
    }
    else {
      return null;
    }
  }

  @Override
  public void closeCurrentPopover() {

    ModalPopover lCurrentPopover = getCurrentPopoverOrNull();
    if(lCurrentPopover != null) {
      deleteFacet(lCurrentPopover);
    }
    else {
      Track.info("NoModalToClose");
    }
  }

  private static class Builder extends ModuleCallFacetProvider.Builder<ModalPopoverProvider> {

    private Builder() {
      super(ModalPopoverProvider.class);
    }

    @Override
    public ModalPopoverProvider createNewProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall) {
      return new ModuleCallModalPopoverProvider(pPersistenceContext, pModuleCall, new HashMap<>());
    }

    @Override
    public ModalPopoverProvider deserialiseExistingProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall) {
      return new ModuleCallModalPopoverProvider(pPersistenceContext, pModuleCall, deserialiseFacets(pPersistenceContext, pModuleCall, ModuleFacetType.MODAL_POPOVER, ModalPopover.class));
    }
  }
}

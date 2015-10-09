package net.foxopen.fox.thread.facet;

import net.foxopen.fox.module.facet.ModalPopover;
import net.foxopen.fox.module.facet.ModalPopoverOptions;
import net.foxopen.fox.module.facet.ModuleFacetType;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;

import java.util.Collection;
import java.util.Collections;

/**
 * Standard implementation of a ModalPopover.
 */
public class ModuleCallModalPopover
implements ModalPopover {

  //Each popover instance has a unique ID so edge cases where more than one popover exists in a churn are correctly handled
  private final String mPopoverId;
  private final String mModuleCallId;
  private final String mBufferName;
  private final String mBufferAttachFoxId;

  private final ModalPopoverOptions mModalPopoverOptions;

  private int mScrollPosition;

  ModuleCallModalPopover(String pPopoverId, String pModuleCallId, String pBufferName, String pBufferAttachFoxId, ModalPopoverOptions pModalPopoverOptions) {
    mPopoverId = pPopoverId;
    mModuleCallId = pModuleCallId;
    mBufferName = pBufferName;
    mBufferAttachFoxId = pBufferAttachFoxId;
    mModalPopoverOptions = pModalPopoverOptions;
  }

  @Override
  public String getBufferName() {
    return mBufferName;
  }

  @Override
  public String getBufferAttachFoxId() {
    return mBufferAttachFoxId;
  }

  @Override
  public void setScrollPosition(PersistenceContext pPersistenceContext, int pScrollPosition) {
    mScrollPosition = pScrollPosition;
    pPersistenceContext.requiresPersisting(this, PersistenceMethod.UPDATE);
  }

  @Override
  public int getScrollPosition() {
    return mScrollPosition;
  }

  @Override
  public ModalPopoverOptions getModalPopoverOptions() {
    return mModalPopoverOptions;
  }

  @Override
  public ModuleFacetType getFacetType() {
    return ModuleFacetType.MODAL_POPOVER;
  }

  @Override
  public String getFacetKey() {
    return mPopoverId;
  }

  @Override
  public String getModuleCallId() {
    return mModuleCallId;
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().createModuleFacet(this);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.CREATE));
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().updateModuleFacet(this);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.UPDATE));
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().deleteModuleFacet(this);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.DELETE));
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.MODULE_FACET;
  }

}

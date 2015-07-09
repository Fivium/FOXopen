package net.foxopen.fox.thread;

import net.foxopen.fox.database.storage.WorkDocValidator;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;

import java.util.Collection;


public interface DOMHandlerProvider {

  Collection<DOMHandler> createDefaultDOMHandlers(String pModuleCallId);

  Collection<DOMHandler> restoreDefaultDOMHandlers(PersistenceContext pPersistenceContext, String pModuleCallId);

  DOMHandler createDOMHandlerForWSL(WorkingDataDOMStorageLocation pWorkingStoreLocation, String pModuleCallId);

  DOMHandler restoreDOMHandlerForWSL(PersistenceContext pPersistenceContext, WorkingDataDOMStorageLocation pWorkingStoreLocation, String pModuleCallId);

  /**
   * Validates any WorkDocs which have been opened by this handler and marked as requiring validation since this method was last invoked.
   * The pending list will be cleared by calling this method.
   * @param pRequestContext Current RequestContext.
   * @return Result of WorkDoc validation. Consumers must decided how to behave in the event of an invalid WorkDoc.
   */
  WorkDocValidator.Result validatePendingWorkDocs(ActionRequestContext pRequestContext);
}

package net.foxopen.fox.thread;

import java.util.Collection;

import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;


public interface DOMHandlerProvider { 
  
  public Collection<DOMHandler> createDefaultDOMHandlers(String pModuleCallId);
  
  public Collection<DOMHandler> restoreDefaultDOMHandlers(PersistenceContext pPersistenceContext, String pModuleCallId);
  
  public DOMHandler createDOMHandlerForWSL(WorkingDataDOMStorageLocation pWorkingStoreLocation, String pModuleCallId);
  
  public DOMHandler restoreDOMHandlerForWSL(PersistenceContext pPersistenceContext, WorkingDataDOMStorageLocation pWorkingStoreLocation, String pModuleCallId);
}

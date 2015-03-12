package net.foxopen.fox.thread.persistence;

import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.persistence.SharedDOMManager.SharedDOMType;

public interface PersistenceContext {
  
  public void startPersistenceCycle(RequestContext pRequestContext);
  
  public void registerListeningPersistable(ListeningPersistable pPersistable);
  
  public void requiresPersisting(Persistable pPersistable, PersistenceMethod pMethod);
  
  public void endPersistenceCycle(RequestContext pRequestContext);
  
  public String getThreadId();  
  
  public Serialiser getSerialiser();
  
  public Deserialiser setupDeserialiser(RequestContext pRequestContext);
  
  public Deserialiser getDeserialiser();
  
  public SharedDOMManager getSharedDOMManager(SharedDOMType pDOMType, String pDOMId);
  
}

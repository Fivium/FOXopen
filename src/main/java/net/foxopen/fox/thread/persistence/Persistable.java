package net.foxopen.fox.thread.persistence;

import java.util.Collection;


public interface Persistable {  
  
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext);  
  
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext);  
  
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext);  
  
  public PersistableType getPersistableType();
  
}

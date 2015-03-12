package net.foxopen.fox.thread.persistence;

public class PersistenceResult {

  private final Persistable mPersistable;
  private final PersistenceMethod mMethod;

  public PersistenceResult(Persistable pPersistable, PersistenceMethod pMethod) {
    mPersistable = pPersistable;
    mMethod = pMethod;
  }

  public Persistable getPersistable() {
    return mPersistable;
  }

  public PersistenceMethod getMethod() {
    return mMethod;
  }

}

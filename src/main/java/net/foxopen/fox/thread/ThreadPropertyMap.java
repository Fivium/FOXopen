package net.foxopen.fox.thread;

import net.foxopen.fox.thread.persistence.PersistenceContextProxy;

import java.util.EnumMap;
import java.util.Map;

public class ThreadPropertyMap {

  private final Map<ThreadProperty.Type, ThreadProperty> mMap;
  /** Used to tell a PersistenceContext when an update has occurred */
  private transient PersistenceContextProxy mPersistenceContextProxy = () -> {};

  public static ThreadPropertyMap createDefaultPropertyMap() {

    Map<ThreadProperty.Type, ThreadProperty> lDefaultMap = new EnumMap<>(ThreadProperty.Type.class);
    for(ThreadProperty.Type lPropertyType : ThreadProperty.Type.values()) {
      lDefaultMap.put(lPropertyType, new ThreadProperty(lPropertyType, lPropertyType.getDefaultValue()));
    }

    return new ThreadPropertyMap(lDefaultMap);
  }

  private ThreadPropertyMap(Map<ThreadProperty.Type, ThreadProperty> pMap) {
    mMap = pMap;
  }

  public void setBooleanProperty(ThreadProperty.Type pType, boolean pValue) {
    mMap.put(pType, new ThreadProperty(pType, pValue));
    mPersistenceContextProxy.updateRequired();
  }

  public boolean getBooleanProperty(ThreadProperty.Type pType) {
    return mMap.get(pType).booleanValue();
  }

  public void setStringProperty(ThreadProperty.Type pType, String pValue) {
    mMap.put(pType, new ThreadProperty(pType, pValue));
    mPersistenceContextProxy.updateRequired();
  }

  public String getStringProperty(ThreadProperty.Type pType) {
    return mMap.get(pType).stringValue();
  }

  void setPersistenceContextProxy(PersistenceContextProxy pPersistenceContextProxy) {
    mPersistenceContextProxy = pPersistenceContextProxy;
  }

  @Override
  public String toString() {
    return mMap.toString();
  }
}

package net.foxopen.fox.thread;

import java.util.EnumMap;
import java.util.Map;

public class ThreadPropertyMap {

  private final Map<ThreadProperty.Type, ThreadProperty> mMap;

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
  }

  public boolean getBooleanProperty(ThreadProperty.Type pType) {
    return mMap.get(pType).booleanValue();
  }

  public void setStringProperty(ThreadProperty.Type pType, String pValue) {
    mMap.put(pType, new ThreadProperty(pType, pValue));
  }

  public String getStringProperty(ThreadProperty.Type pType) {
    return mMap.get(pType).stringValue();
  }

  @Override
  public String toString() {
    return mMap.toString();
  }
}

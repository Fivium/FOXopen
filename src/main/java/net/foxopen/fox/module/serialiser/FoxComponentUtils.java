package net.foxopen.fox.module.serialiser;

import net.foxopen.fox.FoxComponent;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;

/**
 * Utility methods for accessing fox components
 */
public class FoxComponentUtils {
  private FoxComponentUtils() {
  }

  /**
   * Returns a FOX component from the serialisation context application
   * @param pSerialisationContext The serialisation context containing the application to access the components table
   * @param pComponentPath The path to the component
   * @return The FOX component resolved from the path
   * @throws net.foxopen.fox.ex.ExInternal If the component could not be found
   */
  public static FoxComponent getComponent(SerialisationContext pSerialisationContext, String pComponentPath) throws ExInternal {
    try {
      return pSerialisationContext.getApp().getComponent(pComponentPath, true);
    }
    catch (ExServiceUnavailable | ExModule | ExApp | ExUserRequest e) {
      throw new ExInternal("Failed to get component from path '" + pComponentPath + "'", e);
    }
  }
}

package net.foxopen.fox.plugin.api;

import net.foxopen.fox.FoxComponent;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;

import java.io.InputStream;
import java.io.Reader;

public class PluginUtils {

  private static final PluginUtils INSTANCE = new PluginUtils();

  public static PluginUtils instance() {
    return INSTANCE;
  }

  public static boolean isValidAppMnem(String pAppMnem) {
    return FoxGlobals.getInstance().getFoxEnvironment().isValidAppMnem(pAppMnem);
  }

  public static FoxComponent getComponent(String pURLPath) {
    FoxComponent lComponent;
    try {
      lComponent = ComponentManager.getComponent(new StringBuilder(pURLPath));
    }
    catch (ExServiceUnavailable | ExModule | ExApp | ExUserRequest e) {
      throw new ExInternal("Failed to find docgen app", e);
    }

    return lComponent;
  }

  public static InputStream getComponentInputStream(String pURLPath) {
    FoxComponent lComponent = getComponent(pURLPath);
    return lComponent.getInputStream();
  }

  public static Reader getComponentReader(String pURLPath) {
    FoxComponent lComponent = getComponent(pURLPath);
    return lComponent.getReader();
  }

  public static String getRealPath(String pRelativePath) {
    return FoxGlobals.getInstance().getServletContext().getRealPath(pRelativePath.replace('\\','/'));
  }
}

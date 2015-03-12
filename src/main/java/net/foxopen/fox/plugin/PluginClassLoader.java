package net.foxopen.fox.plugin;


import com.google.common.base.Joiner;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.Set;
import java.util.TreeSet;

import net.foxopen.fox.plugin.api.PluginManagerContext;
import net.foxopen.fox.plugin.api.command.FxpCommandContext;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * URLClassLoader which gives precedence to its known classes BEFORE delegating to the parent. Used by plugins to ensure
 * their libraries are loaded before the main engine's.
 */
public class PluginClassLoader
extends URLClassLoader {

  /** All class names loaded by this loader, for debug info */
  private final Set<String> mLoadedClasses = new TreeSet<>();

  PluginClassLoader(URL[] pUrls, ClassLoader pParent) {
    super(pUrls, pParent);
  }

  public Class<?> loadClass(String pName)
  throws ClassNotFoundException {

    Class<?> lFindClass = findLoadedClass(pName);

    if(lFindClass != null) {
      return lFindClass;
    }
    else {
      try {
        //Very important: search THIS classloader first
        lFindClass = findClass(pName);
      }
      catch (ClassNotFoundException ignore) {}

      if(lFindClass != null) {
        //Record the loaded classname for debug purposes
        mLoadedClasses.add(pName);
        return lFindClass;
      }
      else {
        return super.loadClass(pName);
      }
    }
  }

  public String getDebugInfo() {
    return "URLs:\n\n" + Joiner.on("\n").join(getURLs()) + "\n\nLoaded Classes:\n\n" + Joiner.on("\n").join(mLoadedClasses);
  }

  public FxpCommandContext createCommandContext(ActionRequestContext pRequestContext, PluginManagerContext pPluginManagerContext) {
    return new PluginCommandRequestContextWrapper(pRequestContext, pPluginManagerContext);
  }
}

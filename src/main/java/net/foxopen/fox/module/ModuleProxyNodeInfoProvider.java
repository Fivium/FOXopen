package net.foxopen.fox.module;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.datanode.NodeInfo;

/**
 * A NodeInfoProvider which should be used when a direct module reference cannot be held (for instance for long-term caching).
 * This instead resolves the Module definition just in time when required to provide a NodeInfo.
 */
public class ModuleProxyNodeInfoProvider
implements NodeInfoProvider {

  private final String mAppMnem;
  private final String mModuleName;

  /**
   * Creates a ProxyNodeInfoProvider for the given module.
   * @param pModule Module to use to resolve NodeInfos. A reference to the module is not kept, instead it is resolved
   *                from the module cache just in time.
   * @return New ModuleProxyNodeInfoProvider.
   */
  public static NodeInfoProvider create(Mod pModule) {
    return new ModuleProxyNodeInfoProvider(pModule.getApp().getMnemonicName(), pModule.getName());
  }

  /**
   * Creates a ProxyNodeInfoProvider for the given module details. Both parameters must be valid names.
   * @param pAppMnem Owning app mnem of the module.
   * @param pModuleName Module name to be resolved.
   * @return New ModuleProxyNodeInfoProvider.
   */
  public static NodeInfoProvider create(String pAppMnem, String pModuleName) {
    return new ModuleProxyNodeInfoProvider(pAppMnem, pModuleName);
  }

  private ModuleProxyNodeInfoProvider(String pAppMnem, String pModuleName) {
    mAppMnem = pAppMnem;
    mModuleName = pModuleName;
  }

  @Override
  public NodeInfo getNodeInfo(String pAbsolutePath) {
    return Mod.getModuleForAppMnemAndModuleName(mAppMnem, mModuleName).getNodeInfo(pAbsolutePath);
  }

  @Override
  public NodeInfo getNodeInfo(DOM pNode) {
    return Mod.getModuleForAppMnemAndModuleName(mAppMnem, mModuleName).getNodeInfo(pNode);
  }
}

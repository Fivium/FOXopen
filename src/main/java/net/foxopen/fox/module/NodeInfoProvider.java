package net.foxopen.fox.module;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.datanode.NodeInfo;

/**
 * Implementors of this interface are able to provide {@link NodeInfo} objects to consumers. NodeInfos are typically
 * resolved from a {@link Mod}, but it is not always appropriate to retain a reference to a Mod object.
 */
public interface NodeInfoProvider {

  /**
   * Gets a NodeInfo based on an absolute path. Will be null if no node definition is available.
   * @param pAbsolutePath Path of the node for which the NodeInfo is required.
   * @return NodeInfo or null.
   */
  NodeInfo getNodeInfo(String pAbsolutePath);

  /**
   * Gets a NodeInfo based on the absolute path of the given Node. Will be null if no node definition is available.
   * @param pNode Node to use to resolve NodeInfo.
   * @return NodeInfo or null.
   */
  NodeInfo getNodeInfo(DOM pNode);

}

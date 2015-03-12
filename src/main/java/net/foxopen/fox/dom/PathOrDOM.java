package net.foxopen.fox.dom;

import net.foxopen.fox.XFUtil;

/**
 * Convenience wrapper for interfaces which require either a String XPath or a DOM value, which may already have been resolved.
 * The DOM and Path are mutually exclusive. Both can be null.
 */
public class PathOrDOM {
  
  private final String mPath;
  private final DOM mDOM;
  
  private static final PathOrDOM EMPTY_INSTANCE = new PathOrDOM();
  
  public static PathOrDOM emptyInstance() {
    return EMPTY_INSTANCE;
  }
  
  private PathOrDOM() {
    mPath = null;
    mDOM = null;
  }
  
  public PathOrDOM(String pPath) {
    mPath = pPath;
    mDOM = null;
  }
  
  public PathOrDOM(DOM pDOM) {
    mPath = null;
    mDOM = pDOM;
  }
  
  public boolean isDOM() {
    return mDOM != null;
  }
  
  public boolean isPath() {
    return !XFUtil.isNull(mPath);
  }

  public String getPath() {
    return mPath;
  }

  public DOM getDOM() {
    return mDOM;
  }
}

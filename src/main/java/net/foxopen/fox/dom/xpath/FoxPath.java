package net.foxopen.fox.dom.xpath;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XPathWrapper;
import net.foxopen.fox.dom.DOM;

/**
 * Interface representing any expression which FOX might need to treat as an XPath.
 * FoxPaths may be constant values, simple paths or XPaths.
 */
public interface FoxPath {

  /**
   * Execute this XPath and return the wrapped result.
   * @param pContextNode The Context Node to evaluate the path from.
   * @param pContextUElem An ContextUElem for resolving :{context}s.
   * @param pResultType The expected result type.
   * @return The wrapped result of the XPath.
   */
  public XPathResult execute(DOM pContextNode, ContextUElem pContextUElem, FoxXPathResultType pResultType, XPathWrapper pXPathWrapper);

  /**
   * Get the original path used to construct this FoxPath, as specified by the application developer. This may contain
   * additional debug information and is not guaranteed to be a valid path.
   * @return The original path String, for debug and error reporting.
   */
  public String getOriginalPath();

  /**
   * Get the path after any processing or rewrites have occurred.
   * @return The processed Path String.
   */
  public String getProcessedPath();

  /**
   * Establishes the ContextualityLevel for this FoxPath. Refer to {@link ContextualityLevel} for a description of
   * contextuality levels.
   * @param pContextUElem A ContextUElem may be required by implementors to establish the contextuality of :{labels}.
   * @param pContextNodeContextualityLevel Optional. The ContextualityLevel of the FoxPath's context node. If null,
   * defaults to ContextualityLevel.ITEM.
   * @return This path's ContextualityLevel.
   * @see ContextualityLevel
   */
  public ContextualityLevel getContextualityLevel(ContextUElem pContextUElem, ContextualityLevel pContextNodeContextualityLevel);
}

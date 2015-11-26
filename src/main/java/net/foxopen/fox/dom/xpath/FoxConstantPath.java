package net.foxopen.fox.dom.xpath;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XPathWrapper;
import net.foxopen.fox.dom.DOM;

/**
 * A FoxConstantPath represents a constant value which needs to be treated as a FoxPath. Currently only numeric
 * expressions are supported.<br/><br/>
 *
 * This is needed in the case that an expression is expected to be an XPath or might be an XPath, for instance
 * <code>&lt;fm:init new-target-count="count({theme}/*)"&gt;</code> versus <code>&lt;fm:init new-target-count="5"&gt;</code>
 */
public class FoxConstantPath
implements FoxPath {

  private String mOriginalPath;
  private XPathResult mXPathResult;

  /**
   * Determines if the given String can be represented by a FoxConstantPath, and constructs one if it does.
   * If not, returns null.
   * @param pPathString The String to attempt to convert to a FoxConstantPath.
   * @return A new FoxConstantPath, or null.
   */
  static FoxConstantPath getFoxConstantPathOrNull(String pPathString){

    //If the first character of this 'path' is a number or sign character, chances are it is a number literal
    //Attempt to parse it as a double and short-circuit XPath evaluation if it is.
    if((pPathString.charAt(0) >= '0' &&  pPathString.charAt(0) <= '9') || pPathString.charAt(0) == '+' || pPathString.charAt(0) == '-'){
      try {
        FoxConstantPath lNewPath = new FoxConstantPath(pPathString, Double.valueOf(pPathString));
        return lNewPath;
      }
      catch(NumberFormatException ignore){}
    }

    return null;
  }

  /**
   * Construct a FoxConstantPath which will return the given result when executed.
   * @param pOriginalPath The original path String.
   * @param pResultObject The result to return when executed.
   */
  FoxConstantPath(String pOriginalPath, Object pResultObject) {
    mOriginalPath = pOriginalPath;
    mXPathResult = new XPathResult(pResultObject, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XPathResult execute(DOM pContextNode, ContextUElem pContextUElem, FoxXPathResultType pResultType, XPathWrapper pOptionalWrapper) {
    return mXPathResult;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOriginalPath() {
    return mOriginalPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getProcessedPath() {
    return mOriginalPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ContextualityLevel getContextualityLevel(ContextUElem pContextUElem, ContextualityLevel pContextNodeContextualityLevel) {
    return ContextualityLevel.CONSTANT;
  }

  XPathResult getXPathResult() {
    return mXPathResult;
  }
}

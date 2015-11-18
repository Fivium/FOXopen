package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.option.xom.XOMNodeWrapper;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import nu.xom.Node;


public class FunctionUtils {
  private FunctionUtils() {}

  static DOM unwrapNode(XOMNodeWrapper pNodeWrapper) {
    //WARNING: dependency on the Saxon internals here, this might not always return a wrapper
    Node lXOMNode = (Node) pNodeWrapper.getUnderlyingNode();
    if(lXOMNode != null) {
     return new DOM(lXOMNode);
    }
    else {
      return null;
    }
  }

  static DOM getDOMNodeOrNull(Sequence[] pArguments, int pIndex, String pFunctionName)
  throws XPathException {
    DOM lDOM = null;
    if(pArguments.length > pIndex) {
      Item lAttachParam = pArguments[pIndex].head();
      if(lAttachParam != null) {
        if(!(lAttachParam instanceof XOMNodeWrapper)) {
          throw new ExInternal("Argument " + (pIndex+1) + " to " + pFunctionName + " function must be a node, was a " + lAttachParam.getClass().getName());
        }
        lDOM = unwrapNode((XOMNodeWrapper) lAttachParam);
      }
    }
    return lDOM;
  }

  /**
   * Retrieves a DOM node from given index of an argument array, if that index exists in the array.
   * @param pArguments
   * @param pIndex
   * @param pFunctionName Name of consumer for debug/reporting purposes.
   * @param pStrict If true, an error is raised if the argument is not a node.
   * @return
   * @throws XPathException
   */
  static DOM getDOMNodeOrNull(XdmValue[] pArguments, int pIndex, String pFunctionName, boolean pStrict)
  throws XPathException {
    DOM lDOM = null;
    if(pArguments.length > pIndex) {
      Object lAttachParam = pArguments[pIndex].getUnderlyingValue();
      if(!(lAttachParam instanceof XOMNodeWrapper)) {
        if(pStrict) {
          throw new ExInternal("Argument " + (pIndex+1) + " to " + pFunctionName + " function must be a node, was a " + lAttachParam.getClass().getName());
        }
        else {
          return null;
        }
      }
      lDOM = unwrapNode((XOMNodeWrapper) lAttachParam);
    }
    return lDOM;
  }

  static String getStringValueOrNull(Sequence[] pArguments, int pIndex, String pFunctionName)
  throws XPathException {
    return getStringValueOrNull(pArguments, pIndex, pFunctionName, true);
  }

  static String getStringValueOrNull(Sequence[] pArguments, int pIndex, String pFunctionName, boolean pStrict)
  throws XPathException {
    String lResult = null;
    if(pArguments.length > pIndex) {
      Item lItem = pArguments[pIndex].head();
      if(!(lItem instanceof StringValue) && pStrict) {
        throw new ExInternal("Argument " + (pIndex+1) + " to " + pFunctionName + " function must be a string.");
      }

      if(lItem != null) {
        lResult = lItem.getStringValue();
      }
    }
    return lResult;
  }

}

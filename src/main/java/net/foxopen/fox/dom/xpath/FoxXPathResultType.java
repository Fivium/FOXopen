package net.foxopen.fox.dom.xpath;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;

/**
 * Enum for representing XPath result types.
 */
public enum FoxXPathResultType {
  DOM_NODE,
  DOM_LIST,
  STRING,
  BOOLEAN,
  NUMBER;

  public QName asQName(){
    switch(this){
      case DOM_LIST:
        return XPathConstants.NODESET;
      case DOM_NODE:
        return XPathConstants.NODE;
      case STRING:
        //Tragically "XPathConstants.STRING" causes Saxon to wrap the XPath with a string() function call, which was not legacy behaviour.
        //XPathResult should handle returning the shallow value of the first item in a result sequence. If string() behaviour
        //is genuinely required it will be explicitly specified on the XPath.
        return XPathConstants.NODESET;
        //return XPathConstants.STRING;
      case BOOLEAN:
        return XPathConstants.BOOLEAN;
      case NUMBER:
        return XPathConstants.NUMBER;
      default:
        return null;
    }
  }

}

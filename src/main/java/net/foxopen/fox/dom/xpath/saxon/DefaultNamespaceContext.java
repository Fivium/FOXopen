package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.module.Mod;
import net.sf.saxon.lib.NamespaceConstant;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;


/**
 * Basic resolver for mapping Fox built-in namespace prefixes to URIs and vice versa. Used by Saxon evaluator.
 * This is a singleton class and does not need to be instantiated by application code.
 */
public class DefaultNamespaceContext
implements NamespaceContext {

  public static final String FO_URI = "http://www.w3.org/1999/XSL/Format";
  public static final String FO_PREFIX = "fo";

  //For supporting use of an explicit "fn" namespace when built-in functions are called
  public static final String FN_URI = "http://www.w3.org/2005/xpath-functions";
  public static final String FN_PREFIX = "fn";

  //For XPath 3.0 "math" functions
  public static final String MATH_URI = "http://www.w3.org/2005/xpath-functions/math";
  public static final String MATH_PREFIX = "math";

  /**
   * This class should only be constructed by SaxonEnvironment.
   */
  DefaultNamespaceContext(){}

  public String getNamespaceURI(String prefix){
    switch (prefix) {
      case SaxonEnvironment.FOX_NS_PREFIX:
        return SaxonEnvironment.FOX_NS_URI;
      case Mod.FOX_MODULE_NS_PREFIX:
        return Mod.FOX_MODULE_URI;
      case SaxonEnvironment.XML_SCHEMA_NS_PREFIX:
        return NamespaceConstant.SCHEMA;
      case FO_PREFIX:
        return FO_URI;
      case MATH_PREFIX:
        return MATH_URI;
      case FN_PREFIX:
        return FN_URI;
      default:
        return "";
    }
  }

  public String getPrefix(String namespace){
    if(Mod.FOX_MODULE_URI.equals(namespace)) {
      return Mod.FOX_MODULE_NS_PREFIX;
    }
    else if (NamespaceConstant.SCHEMA.equals(namespace)){
      return SaxonEnvironment.XML_SCHEMA_NS_PREFIX;
    }
    else if (FO_URI.equals(namespace)){
      return FO_PREFIX;
    }
    else {
      return SaxonEnvironment.FOX_NS_PREFIX;
    }
  }

  public Iterator getPrefixes(String namespace){
    return null;
  }
}

/*

Copyright (c) 2012, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
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
   * This class should only be constructed by SaxonEnviornment.
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

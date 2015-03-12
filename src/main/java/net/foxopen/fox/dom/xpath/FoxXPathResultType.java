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

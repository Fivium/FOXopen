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

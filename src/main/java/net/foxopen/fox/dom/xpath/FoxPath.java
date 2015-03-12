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
   * Get the original path used to construct this FoxPath, as specified by the application developer.
   * @return The original path String.
   */
  public String getOriginalPath();

  /**
   * Get the path after any processing or rewrites have occured.
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

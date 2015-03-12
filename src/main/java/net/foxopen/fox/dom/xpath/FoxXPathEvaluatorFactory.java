/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
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


/**
 * Static factory object for the creation of a {@link FoxXPathEvaluator}.
 * This should be used sparingly as the created FoxXPathEvaluator should be cacheable and reusable.
 */
public class FoxXPathEvaluatorFactory {

  static enum EvaluatorType{
    STANDARD
  }

  /**
   * Instantiation not allowed.
   */
  private FoxXPathEvaluatorFactory(){}

  private static final EvaluatorType DEFAULT_EVALUATOR_TYPE = EvaluatorType.STANDARD;

  /**
   * Create a new FoxXPathEvaluator using the default EvaluatorType (DEFAULT).
   * @param pUseXPathBackwardsCompatibility If true, the FoxXPathEvaluator will operate in XPath 1.0 compatibility mode.
   * @return A new XPath evaluator.
   */
  public static FoxXPathEvaluator createEvaluator(boolean pUseXPathBackwardsCompatibility){
    return createEvaluator(DEFAULT_EVALUATOR_TYPE, pUseXPathBackwardsCompatibility);
  }

  /**
   * Create a new FoxXPathEvaluator of the given EvaluatorType.
   * @param pUseXPathBackwardsCompatibility If true, the FoxXPathEvaluator will operate in XPath 1.0 compatibility mode.
   * @param pEvaluatorType The type of FoxXPathEvaluator to create.
   * @return A new XPath evaluator.
   */
  public static FoxXPathEvaluator createEvaluator(EvaluatorType pEvaluatorType, boolean pUseXPathBackwardsCompatibility){
    switch(pEvaluatorType){
      case STANDARD:
        return new FoxXPathEvaluator(pUseXPathBackwardsCompatibility);
    }
    return null;
  }

}

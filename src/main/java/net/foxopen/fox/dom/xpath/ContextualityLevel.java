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

/**
 * The ContextualityLevel of an XPath is used to determine its maximum specificity and establish at what level
 * the result can be cached. See the enum descriptions for more information.<br/><br/>
 * 
 * The main factors which affect the ContextualityLevel of a path are its use of the initial context item and its use of
 * context labels. In order to ascertain an XPath's ContextualityLevel, a {@link net.foxopen.fox.ContextUElem ContextUElem}
 * is required to track the ContextualityLevels of the context labels used in the XPath. It is possible for a context
 * label's ContextualityLevel to change based on the current state of the thread - for instance from within an
 * <code>fm:context-localise</code> command.<br/><br/>
 * 
 * As a general rule, the label with the highest (most specific) ContextualityLevel determines the XPath's overall
 * ContextualityLevel. The use of the context node within the path usually causes it to report the maximum ContextualityLevel.
 * However, if it is known that the context node equates to a context label with a lower ContextualityLevel, this information
 * can be provided to the {@link FoxPath#getContextualityLevel} method.<br/><br/>
 * 
 * When establishing the ContextualityLevel of an XPath, two assumptions are made:
 * <ol>
 * <li>All DOMs implicated by the XPath are read-only and immutable</li>
 * <li>The {@link net.foxopen.fox.ContextUElem ContextUElem} executing the XPath is immutable</li>
 * </ol>
 * If these assumptions do not hold, that does not mean the ContextualityLevel is unreliable, but care must be taken if
 * it is being used to control caching. For instance, XPaths referencing the :{attach} context (which has a default
 * ContextualityLevel of STATE) may be cacheable even if the ContextUElem is mutable, as long as the caching mechanism 
 * considers the current state of the ContextUElem (specifically the :{attach} node in that case).<br/><br/>
 */
public enum ContextualityLevel {
  
  /**
   * Indicates that an XPath is a constant and will return the same result regardless of the context node or ContextUElem
   * used to evaluate it. This could be a numerical constant, a call to an XPath function, etc. It would be considered to
   * be globally cacheable.
   */
  CONSTANT(0),
  
  /**
   * Indicates that an XPath references the root node of one or more documents. The results of such XPaths would be 
   * identical for as long as the documents they implicate remain immutable.
   */
  DOCUMENT(1),

  /**
   * Indicates that an XPath has a contextual reference which applies to the current state in the module callstack.
   * A typical example of this will be the state's attach point or a custom context defined with <code>fm:context-set</code>.
   * <br/><br/>
   * The STATE contextuality level does not assert that the result of an XPath will be different if it is executed in a 
   * different state, only that it could be. Ultimately if all of the implicated STATE level labels within a 
   * ContextUElem remain the same between executions of a STATE level XPath, the result will be the same.
   */
  STATE(2),
  
  /**
   * Indicates that an XPath has a reference to a context label which is localised within a nested execution block.
   * This means that the result of the XPath will (most likely) be different unless it is executed repeatedly within
   * the exact same localised context.<br/><br/>
   * Typically this will be caused by looping using <code>fm:for-each</code> or <code>fm:context-localise</code>.
   * Any XPaths within these constructs which reference the loop item or localised context would be considered LOCALISED.
   */
  LOCALISED(3),
  
  /**
   * Indicates that an XPath has a dynamic reference to an individual node. Usually this will be the context item or a 
   * context label which is automatically assigned to nodes multiple times in the same processing cycle, such as 
   * :{itemrec}.<br/><br/>
   * Effectively the ITEM contextuality level means the XPath has a very high degree of specificity and will most
   * likely be different for each execution, unless the ContextUElem is in exactly the same state and the same context 
   * node is used.
   */
  ITEM(4);
  
  private int mLevel;
  
  private ContextualityLevel(int pLevel){
    mLevel = pLevel;
  }
  
  /**
   * Returns this ContextualityLevel expressed as a number. Higher numbers indicate a higher degree of contextuality 
   * (higher specificity).
   */
  public int asInt(){
    return mLevel;
  }
  
}

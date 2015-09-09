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
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExPathInternal;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Representation of a simple path. Simple paths are used to avoid the overhead of invoking an XPath evaluation when
 * only basic path steps are required.<br><br>
 * The following XPath-style tokens are supported by FoxSimplePath:
 * <ul>
 *  <li>/</li>
 *  <li>.</li>
 *  <li>..</li>
 *  <li>*</li>
 *  <li>ELEMENT_NAME</li>
 * </ul>
 * <br>
 *
 * Expressions wrapped in <code>string()</code> are unwrapped and the argument is evaluated as the path.<br>
 * <code>/text()</code> is also allowed as long as it is the final token of the path.<br><br>
 *
 * Simple paths may start with a :{context} reference - when executed this is rewritten to a '.' and the relevant
 * DOM is used as the initial context item.<br/><br>
 *
 * Element names in simple paths may include a namespace prefix; it is the consumer's responsibility to ensure that
 * the simple path is executed with the correct method for resolving these prefixes.
 */
public class FoxSimplePath
implements FoxPath {

  private final XPathDefinition mXPathDefinition;
  private final String mProcessedPathString;
  private final boolean mIsStringExpression;
  private final boolean mIsTextNodeExpression;

  /**
   * The :{label} this simple path begins with, if it begins with one. Null otherwise.
   */
  private final String mContextItemLabel;

  /**
   * Regex pattern to determine if a path starts with a ":{context}" style reference.
   */
  private final static Pattern gStartsWithContextLabelPattern = Pattern.compile("^:\\{([^}]+)\\}([^\\{\\}]*)");

  /**
   * Checks if the path specified by pString is a valid simple path which can be evaluated by the simple path evaluator.
   * @param pString The path String.
   * @return True if the path is simple, false otherwise.
   */
  private static boolean isSimplePathInternal(String pString) {

    String lPathString = pString;

    //Looks like an invalid path - we certainly can't deal with it
    if(lPathString.endsWith("/")){
      return false;
    }

    char[] buf = lPathString.toCharArray();
    char c;
    int len = buf.length;

    if(len == 0) {
      return true;
    }

    int p = 0;
    c = buf[p];
    LOCATOR_LOOP: for(;;) {

      // Validate / and ///
      if(c == '/') {
        p++;
        if(p == len) {
          return true;
        }
        c = buf[p];
        if(c == '/') {
          return false;
        }

      }

      // Validate *
      else if(c == '*') {

        p++;
        if(p == len) {
          return true;
        }
        c = buf[p];
        if(c != '/') {
          return false;
        }

      }

      // Validate . and ..
      else if(c == '.') {
        p++;
        if(p == len) {
          return true;
        }
        c = buf[p];
        if(c == '.') {
          p++;
          if(p == len) {
            return true;
          }
          c = buf[p];
          if(c != '/') {
            return false;
          }
        }
      }

      // Validate element name
      else {
        if( !Character.isLetter(c) && c != '_') {
          return false;
        }
        ELEMENT_NAME_LOOP: for(;;) {
          p++;
          if(p == len) {
            return true;
          }
          c = buf[p];
          if(c == '/') {
            continue LOCATOR_LOOP;
          }
          else if(!Character.isLetter(c)
          && c != '_' && c != '-' && c != '.'
          && !Character.isDigit(c)
          ) {
            return false;
          }
        } // end ELEMENT_NAME_LOOP
      }
    }
  }

  /**
   * Process a path by taking the following steps:
   * <ol>
   * <li>Unnest from string() function call</li>
   * <li>Remove /text() text node step if it is the final step of the path</li>
   * <li>Replace a :{context} label with self node step (i.e. '.') if is the first step in the path</li>
   * <li>Validate if the path is simple</li>
   * </ol>
   * If the path is a supported simple path, this method returns a path object representing it.
   * Otherwise it returns null.
   * @param pXPathDefinition The path to process.
   * @return A new FoxSimplePath object if the path string is compatible, or null.
   */
  public static FoxSimplePath getFoxSimplePathOrNull(XPathDefinition pXPathDefinition){

    String lSimplePath = pXPathDefinition.getExecutableXPath().trim();
    boolean lStringExpression = false;
    boolean lTextNodeExpression = false;

    if(lSimplePath.startsWith("string(")){
      //Trim off the string() function call from the path
      lSimplePath = lSimplePath.substring("string(".length(), lSimplePath.length() - 1);
      lStringExpression = true;
    }

    if(lSimplePath.endsWith("/text()")){
      //Trim /text() from the end of the path
      lSimplePath = lSimplePath.substring(0, lSimplePath.length() - "/text()".length());
      lTextNodeExpression = true;
    }

    String lContextItemLabel = null;

    //If the path starts with a context label, we can replace it with . and make sure the path is evaluated from
    //the node corresponding to that context label.
    //Start with a charAt to avoid wasting time on regex evaluation if it's not needed.
    if(lSimplePath.charAt(0) == ':'){
      Matcher lMatcher = gStartsWithContextLabelPattern.matcher(lSimplePath);
      if(lMatcher.matches()){
        lContextItemLabel = lMatcher.group(1);
        lSimplePath = "." + lMatcher.group(2);
      }
    }

    if(isSimplePathInternal(lSimplePath)){
      return new FoxSimplePath(pXPathDefinition, lSimplePath, lStringExpression, lTextNodeExpression, lContextItemLabel);
    }
    else {
      return null;
    }
  }


  private FoxSimplePath(XPathDefinition pXPathDefinition, String pSimplifiedPath, boolean pIsStringExpression, boolean pIsTextNodeExpression, String pContextItemLabel){
    mXPathDefinition = pXPathDefinition;
    mProcessedPathString = pSimplifiedPath;
    mIsStringExpression = pIsStringExpression;
    mIsTextNodeExpression = pIsTextNodeExpression;
    mContextItemLabel = pContextItemLabel;
  }

  /**
   * Converts pResult into a String, if necessary.
   * If the original expression was wrapped in the string() function, applies XPath string() function logic to pResult.
   * Otherwise, if a String or Number was requested, gets the shallow value of the first node in the result list.
   * @param pResult The result object.
   * @return A String if the path contained the string() function, or pResult unchanged if not.
   */
  private Object applyStringEvalToResult(Object pResult, FoxXPathResultType pResultType){

    if(mIsStringExpression || pResultType == FoxXPathResultType.STRING || pResultType == FoxXPathResultType.NUMBER){

      if(pResult instanceof List){
        //This will be either a DOM list or list of Strings
        List lResultAsList = (List) pResult;
        if(lResultAsList.size() == 1 || (lResultAsList.size() > 1 && !mIsStringExpression)){
          Object lResult = lResultAsList.get(0);
          if(lResult instanceof String){
            return lResult;
          }
          else if (lResult instanceof DOM){
            //If this is a string() expression we want the DEEP value of the node
            //If not, the consumer has explicitly requested an atomic string-type result so only do a shallow evaluation
            return ((DOM) lResult).value(mIsStringExpression);
          }
          else {
            throw new ExPathInternal("Expected String or DOM in list result object but got a " + lResult.getClass().getName());
          }
        }
        else if (lResultAsList.size() > 1 && mIsStringExpression) {
          //XPath 2.0 spec says the string() function only takes a single node; if we're dealing with a larger sequence we should error
          throw new ExPathInternal("string() takes a single-item sequence as an argument but " + lResultAsList.size() + " items found");
        }
        else {
          return "";
        }
      }
      else if(pResult instanceof DOM){
        //If this is a string() expression we want the DEEP value of the node
        //If not, the consumer has explicitly requested an atomic string-type result so only do a shallow evaluation
        return ((DOM) pResult).value(mIsStringExpression);
      }

    }

    return pResult;
  }

  @Override
  public XPathResult execute(DOM pContextNode, ContextUElem pContextUElem, FoxXPathResultType pResultType, XPathWrapper pXPathWrapper){
    Object lResult = null;

    DOM lContextNode = pContextNode;

    //If the path starts with a context, then we evaluate the XPath relative to that context
    if(mContextItemLabel != null){
      if(pContextUElem == null){
        throw new ExPathInternal("FoxSimplePath uses context label '" + mContextItemLabel + "' but no ContextUElem supplied to execute()");
      }
      else {
        lContextNode = pContextUElem.getUElem(mContextItemLabel);
        //Check the labelled DOM has not been removed from its DOM tree
        if(!lContextNode.isAttached()) {
          //This should probably throw/invalidate label mapping - warn for now so we can assess impact - FOXRD-661
          Track.alert("SimplePathUnattachedContextLabel", "Context label " + mContextItemLabel + " points to an unattached node - XPath may produce unexpected results " +
            "(this message will become an error in a future release)", TrackFlag.CRITICAL);
        }
      }
    }

    switch(pResultType){
      case DOM_LIST:
      case BOOLEAN:
      case STRING:
      case NUMBER:
        //Get a result list of all nodes hit by the path. For BOOLEAN, STRING or NUMBER, the first node in the list will be taken (see applyStringEvalToResult)
        lResult = lContextNode.getUL(mProcessedPathString);
        if(mIsTextNodeExpression){
          //If expression ended in /text(), get the child text nodes of the resolve nodes
          lResult = ((DOMList) lResult).allChildTextNodesAsDOMList(false);
        }
        break;
      case DOM_NODE:
        lResult = lContextNode.get1EOrNull(mProcessedPathString);
        if(mIsTextNodeExpression && lResult != null){
          List<DOM> lTextList = ((DOM) lResult).childTextNodesAsDOMList(false);
          if(lTextList.size() > 1){
            throw new ExPathInternal("Error evaluating simple path '" + getOriginalPath() + "'. Expected at most 1 node, got " + lTextList.size());
          }
          else if (lTextList.size() == 1){
            lResult = lTextList.get(0);
          }
          else {
            lResult = lTextList;
          }
        }
        break;
    }

    //If the expression is wrapped in string(), or a STRING or NUMBER result was requested, evaluate accordingly
    lResult = applyStringEvalToResult(lResult, pResultType);

    //PN TODO this is probably redundant
    if(!(lResult instanceof String) && (pResultType == FoxXPathResultType.STRING || pResultType == FoxXPathResultType.NUMBER)){
      throw new ExInternal("FoxSimplePath '" + getOriginalPath() + "' did not return a String despite String or Number ResultType being specified. Instead returned a " + lResult.getClass().getName());
    }

    //Belt and braces null test (XPathResult cannot be constructed with a null result object)
    if(lResult == null){
      if(pResultType == FoxXPathResultType.DOM_NODE){
        throw new ExPathInternal("Path '" + getOriginalPath() + "' resolved no nodes but single node result is required in this context.");
      }
      else {
        //This shouldn't happen as we should have arrived at least with an empty string or empty list the processing above
        throw new ExInternal("Unexpected state evaluating path '" + getOriginalPath() + "': null result object");
      }
    }

    return new XPathResult(lResult, this, lContextNode, pXPathWrapper);
  }

  public String getOriginalPath(){
    return mXPathDefinition.getPathForDebug();
  }

  public String getProcessedPath(){
    return mProcessedPathString;
  }

  public ContextualityLevel getContextualityLevel(ContextUElem pContextUElem, ContextualityLevel pContextNodeContextualityLevel) {

    if(mContextItemLabel != null){
      //The XPath is relative to a context, so delegate the question to the ContextUElem
      return pContextUElem.getLabelContextualityLevel(mContextItemLabel);
    }
    else if(mProcessedPathString.charAt(0) == '/'){
      //If the first step is the root node step, this path is document-based
      return ContextualityLevel.DOCUMENT;
    }
    else {
      //For all other cases the context item is the determinant - use the specified value, or default to ITEM if not specified
      return pContextNodeContextualityLevel != null ? pContextNodeContextualityLevel : ContextualityLevel.ITEM;
    }

  }
}

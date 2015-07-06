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
package net.foxopen.fox;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.XQueryUtil;
import net.foxopen.fox.dom.XSLTransformerUtil;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.dom.xpath.FoxConstantPath;
import net.foxopen.fox.dom.xpath.FoxPath;
import net.foxopen.fox.dom.xpath.FoxSimplePath;
import net.foxopen.fox.dom.xpath.FoxXPath;
import net.foxopen.fox.dom.xpath.FoxXPathEvaluator;
import net.foxopen.fox.dom.xpath.FoxXPathEvaluatorFactory;
import net.foxopen.fox.dom.xpath.FoxXPathResultType;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.dom.xpath.saxon.SaxonEnvironment;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExPathInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.thread.ActionRequestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class ContextUElemTest {

  ContextUElem mContextUElem;
  DOM mRootDOM;
  DOM mThemeDOM;
  DOM mHTML;
  FoxXPathEvaluator mEvaluator = FoxXPathEvaluatorFactory.createEvaluator(true);


  static String ROOT_DOC =
"<root>\n" +
"  <SEARCH_CRITERIA>\n" +
"    <FIRST_NAME>John</FIRST_NAME>\n" +
"    <LAST_NAME>Smith</LAST_NAME>\n" +
"    <AGE>22</AGE>\n" +
"  </SEARCH_CRITERIA>\n" +
"  <RESULT_LIST>\n" +
"    <RESULT>\n" +
"      <NAME>John Smith</NAME>\n" +
"      <NAME_TYPE>FULL</NAME_TYPE>\n" +
"    </RESULT>\n" +
"    <RESULT>\n" +
"      <NAME>Alex Smith</NAME>\n" +
"      <NAME_TYPE>FULL</NAME_TYPE>\n" +
"    </RESULT>\n" +
"    <RESULT>\n" +
"      <NAME>A. Jones</NAME>\n" +
"      <NAME_TYPE>ABBR</NAME_TYPE>\n" +
"    </RESULT>\n" +
"  </RESULT_LIST>\n" +
"</root>";

  static String THEME_DOC =
"<theme foxid=\"foxid1\">\n" +
"  <CONFIG>\n" +
"    <NAME_TYPE>FULL</NAME_TYPE>\n" +
"    <DATE_TYPE>YEAR</DATE_TYPE>\n" +
"    <SEEK_NAME>A. Jones</SEEK_NAME>\n" +
"  </CONFIG>\n" +
"</theme>";

  static String HTML_DOC =
"<html>\n" +
"  <body><p>Paragraph 1 <b>bold text</b></p><p>Paragraph 2</p><p>Paragraph 3 <b>bold text</b> further text</p></body>\n" +
"</html>";

  private static class TestDOMHandler
  implements DOMHandler {

    DOM mDOM;
    String mContextLabel;

    private TestDOMHandler(DOM pDOM, String pContextLabel){
      mDOM = pDOM;
      mContextLabel = pContextLabel;
    }

    @Override
    public void open(ActionRequestContext pRequestContext) {}

    @Override
    public DOM getDOM() {
      return mDOM;
    }

    @Override
    public void close(ActionRequestContext pRequestContext) {}

    @Override
    public boolean isTransient() {
      return true;
    }

    @Override
    public String getContextLabel() {
      return mContextLabel;
    }


    @Override
    public int getLoadPrecedence() {


      return LOAD_PRECEDENCE_MEDIUM;
    }
  }

  @Before
  public void setup() throws ExFoxConfiguration {
    mRootDOM = DOM.createDocumentFromXMLString(ROOT_DOC);
    mThemeDOM = DOM.createDocumentFromXMLString(THEME_DOC);
    mHTML = DOM.createDocumentFromXMLString(HTML_DOC);
    mContextUElem = new ContextUElem(mRootDOM, ContextLabel.ROOT);
    mContextUElem.registerDOMHandler(new TestDOMHandler(mRootDOM, ContextLabel.ROOT.asString()));
    mContextUElem.registerDOMHandler(new TestDOMHandler(mThemeDOM, ContextLabel.THEME.asString()));
    mContextUElem.registerDOMHandler(new TestDOMHandler(mHTML, "html"));

    DOM lSimpleTextDOM = DOM.createDocumentFromXMLString("<p>text 1 <b>bold</b> text 2</p>");
    mContextUElem.registerDOMHandler(new TestDOMHandler(lSimpleTextDOM, "simple-text"));
  }

  @Test
  public void testExtendedXPathUL()
  throws ExActionFailed {
    DOMList lResult = mContextUElem.extendedXPathUL(mRootDOM, ":{root}/RESULT_LIST/RESULT[NAME_TYPE = :{theme}/CONFIG/NAME_TYPE]");
    assertEquals("extendedXPathUL gets 2 elements", 2, lResult.getLength());
    assertEquals("extendedXPathUL gets the correct elements (1st element John Smith)", "John Smith", lResult.item(0).get1SNoEx("./NAME"));

    lResult = mContextUElem.extendedXPathUL(mRootDOM, ":{root}/RESULT_LIST/RESULT[NAME_TYPE = :{theme}/CONFIG/NAME_TYPE]");
  }

  @Test
  public void testExtendedXPath1E()
  throws ExActionFailed, ExTooMany, ExTooFew {
    DOM lResult = mContextUElem.extendedXPath1E(mRootDOM, ":{root}/RESULT_LIST/RESULT[NAME = :{theme}/CONFIG/SEEK_NAME]", false);
    assertEquals("extendedXPath1E gets the targeted node", "A. Jones", lResult.get1SNoEx("./NAME"));
  }

  @Test
  public void testExtendedXPath1E_WithBaseself()
  throws ExActionFailed, ExTooMany, ExTooFew {
    DOM lResult = mContextUElem.extendedXPath1E(mRootDOM, ":{root}/RESULT_LIST/RESULT[NAME = :{baseself}/RESULT_LIST/RESULT[1]/NAME]", false);
    assertEquals("extendedXPath1E with baseself gets the targeted node", "John Smith", lResult.get1SNoEx("./NAME"));
  }

  @Test (expected = ExTooFew.class)
  public void testExtendedXPath1E_FailsOnTooFew()
  throws ExActionFailed, ExTooMany, ExTooFew {
    mContextUElem.extendedXPath1E(mRootDOM, ":{root}/RESULT_LIST/RESULT[NAME = 'Not a name']", false);
  }

  @Test (expected = ExTooMany.class)
  public void testExtendedXPath1E_FailsOnTooMany()
  throws ExActionFailed, ExTooMany, ExTooFew {
    mContextUElem.extendedXPath1E(mRootDOM, ":{root}/RESULT_LIST/RESULT", false);
  }

  @Test
  public void testStringResults()
  throws ExActionFailed {
    String lResult = mContextUElem.extendedStringOrXPathString(mRootDOM, "string(:{root}/RESULT_LIST/RESULT[NAME = :{theme}/CONFIG/SEEK_NAME]/NAME)");
    assertEquals("Gets A.Jones", "A. Jones", lResult);

    lResult = mContextUElem.extendedXPathResult(mRootDOM, ":{root}/RESULT_LIST/RESULT[NAME = :{theme}/CONFIG/SEEK_NAME]/NAME").asString();
    assertEquals("Gets A.Jones", "A. Jones", lResult);
  }

  @Test
  public void testExtendedStringOrXPathString()
  throws ExActionFailed {

    String lResult = mContextUElem.extendedStringOrXPathString(mRootDOM, "string(:{root}/RESULT_LIST/RESULT[1]/NAME)");
    assertEquals("string() nesting executes nested XPath", "John Smith", lResult);

    lResult = mContextUElem.extendedStringOrXPathString(mRootDOM, "unescaped-string(:{root}/RESULT_LIST/RESULT[1]/NAME)");
    assertEquals("unescaped-string() nesting executes nested XPath", "John Smith", lResult);

    lResult = mContextUElem.extendedStringOrXPathString(mRootDOM, "number(count(:{root}/RESULT_LIST/RESULT))");
    assertEquals("number() nesting executes nested XPath", "3", lResult);

    lResult = mContextUElem.extendedStringOrXPathString(mRootDOM, "string(:{html}/body/p[1])");
    assertEquals("string() nesting only returns SHALLOW value of resolved node", "Paragraph 1 ", lResult);

    lResult = mContextUElem.extendedStringOrXPathString(mRootDOM, "string(string(:{html}/body/p[1]))");
    assertEquals("Double string() nesting returns deep value of resolved node", "Paragraph 1 bold text", lResult);

    lResult = mContextUElem.extendedStringOrXPathString(mRootDOM, "Hello World");
    assertEquals("No string() nesting returns argument as is", "Hello World", lResult);

    lResult = mContextUElem.extendedStringOrXPathString(mRootDOM, ":{html}/body/p[1]");
    assertEquals("No string() nesting returns argument as is, even if it is an XPath", ":{html}/body/p[1]", lResult);
  }

  @Test
  public void testConstantStringOrXPathResult()
  throws ExActionFailed {

    String lStringResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "string(:{root}/RESULT_LIST/RESULT[1]/NAME)").asString();
    assertEquals("string() nesting executes nested XPath", "John Smith", lStringResult);

    lStringResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "unescaped-string(:{root}/RESULT_LIST/RESULT[1]/NAME)").asString();
    assertEquals("unescaped-string() nesting executes nested XPath", "John Smith", lStringResult);

    lStringResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "string(:{html}/body/p[1])").asString();
    assertEquals("string() nesting only returns SHALLOW value of resolved node", "Paragraph 1 ", lStringResult);

    lStringResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "unescaped-string(:{html}/body/p[1])").asString();
    assertEquals("unescaped-string() nesting only returns SHALLOW value of resolved node", "Paragraph 1 ", lStringResult);

    lStringResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "string(string(:{html}/body/p[1]))").asString();
    assertEquals("Double string() nesting returns deep value of resolved node", "Paragraph 1 bold text", lStringResult);

    lStringResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "Hello World").asString();
    assertEquals("No wrapper nesting returns argument as is", "Hello World", lStringResult);

    lStringResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, ":{html}/body/p[1]").asString();
    assertEquals("No wrapper nesting returns argument as is, even if it is an XPath", ":{html}/body/p[1]", lStringResult);

    int lIntResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "1").asNumber().intValue();
    assertEquals("No wrapper nesting returns argument which can be converted to an integer", 1, lIntResult);

    lIntResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "number(count(:{root}/RESULT_LIST/RESULT))").asNumber().intValue();
    assertEquals("Wrapping XPath in number() function returns the evaluated value", 3, lIntResult);

    double lDoubleResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "1.0").asNumber().doubleValue();
    assertEquals("No wrapper nesting returns argument which can be converted to a double", 1.0d, lDoubleResult, 0.01d);
  }

  @Test
  public void testStringEscapingForXPathResults()
  throws ExActionFailed {

    XPathResult lResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "Hello World");
    assertFalse("Constant string does not require escaping", lResult.isEscapingRequired());

    lResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "string(:{root}/RESULT_LIST/RESULT[1]/NAME)");
    assertTrue("string() nesting of DOM-based XPath requires escaping", lResult.isEscapingRequired());

    lResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "unescaped-string(:{root}/RESULT_LIST/RESULT[1]/NAME)");
    assertFalse("unescaped-string() nesting of DOM-based XPath does not require escaping", lResult.isEscapingRequired());

    lResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "string(concat('a', 'b'))");
    assertFalse("string() nesting of non DOM-based XPath does not require escaping", lResult.isEscapingRequired());

    lResult = mContextUElem.extendedConstantOrXPathResult(mRootDOM, "unescaped-string(concat('a', 'b'))");
    assertFalse("unescaped-string() nesting of non DOM-based XPath does not require escaping", lResult.isEscapingRequired());
  }

  @Test
  public void testGetNumber()
  throws ExActionFailed {
    Number lResult = mContextUElem.extendedXPathResult(mRootDOM, "count(:{root}/RESULT_LIST/RESULT)").asNumber();
    assertEquals("Number retrieved from non-node item", 3, lResult.intValue());

    lResult = mContextUElem.extendedXPathResult(mRootDOM, ":{root}/SEARCH_CRITERIA/AGE").asNumber();
    assertEquals("Number retrieved from node's text value", 22, lResult.intValue());

    lResult = mContextUElem.extendedXPathResult(mRootDOM, ":{root}/SEARCH_CRITERIA/AGE/text()").asNumber();
    assertEquals("Number retrieved from text node", 22, lResult.intValue());

    lResult = mContextUElem.extendedXPathResult(mRootDOM, "false()").asNumber();
    assertEquals("Number retrieved from boolean (false=0)", 0, lResult.intValue());

    lResult = mContextUElem.extendedXPathResult(mRootDOM, "true()").asNumber();
    assertEquals("Number retrieved from boolean (true=0)", 1, lResult.intValue());

    lResult = mContextUElem.extendedXPathResult(mRootDOM, "(4,5,6)").asNumber();
    assertEquals("First number retrieved from sequence", 4, lResult.intValue());
  }

  @Test
  public void testAttachContext()
  throws ExActionFailed, ExTooFew, ExTooMany {
    mContextUElem.setUElem(ContextLabel.ATTACH, mThemeDOM.get1E("CONFIG/SEEK_NAME"));
    String lResult = mContextUElem.extendedXPathResult(mRootDOM, ":{root}/RESULT_LIST/RESULT[NAME = :{attach}/text()]/NAME").asString();
    assertEquals(":{attach} context resolves correct node", "A. Jones", lResult);
  }

  @Test
  public void testAttachNodeUsedAsContextItem()
  throws ExActionFailed, ExTooFew, ExTooMany {

    mContextUElem.setUElem(ContextLabel.ATTACH, mThemeDOM.get1E("CONFIG/SEEK_NAME"));

    String lResult = mContextUElem.extendedXPathResult(mContextUElem.attachDOM(), "./text()").asString();
    assertEquals("Dot in simple path resolves to attach node", "A. Jones", lResult);

    lResult = mContextUElem.extendedXPathResult(mContextUElem.attachDOM(), "./text()[string-length() > 0]").asString();
    assertEquals("Dot in XPath resolves to attach node", "A. Jones", lResult);
  }

  @Test
  public void testXPathFunctionNesting()
  throws ExActionFailed, ExTooFew, ExTooMany {

    String lResult = mContextUElem.extendedXPathResult(mRootDOM, "string(name(:{root}))").asString();
    assertEquals("Gets root", "root", lResult);
  }

  @Test
  public void testGetCreateUL()
  throws ExActionFailed, ExTooFew, ExTooMany {
    DOMList lResult = mContextUElem.getCreateXPathUL(":{root}/RESULT_LIST/RESULT[1]/NEW_ELEMENT");
    assertEquals("Creates and gets new element", "NEW_ELEMENT", lResult.item(0).getLocalName());

    lResult = mContextUElem.getCreateXPathUL(":{root}/RESULT_LIST/RESULT[not(NEW_ELEMENT)]");
    assertEquals("Gets list without creating new element", 2, lResult.getLength());
  }

  @Test
  public void testGetCreateXPath1E()
  throws ExActionFailed, ExTooFew, ExTooMany {
    DOM lResult = mContextUElem.getCreateXPath1E(":{root}/RESULT_LIST/RESULT[1]/NEW_ELEMENT");
    assertEquals("Gets new element", "NEW_ELEMENT", lResult.getLocalName());
  }

  @Test(expected = ExTooMany.class)
  public void testGetCreateXPath1E_FailsOnTooMany()
  throws ExActionFailed, ExTooMany {
    DOM lResult = mContextUElem.getCreateXPath1E(":{root}/RESULT_LIST/RESULT/NEW_ELEMENT");
  }

  @Test(expected = ExDOMName.class)
  public void testGetCreateXPath1E_FailsWhenNotMatchedAndPredicateInPath()
  throws ExActionFailed, ExTooFew, ExTooMany {
    DOM lResult = mContextUElem.getCreateXPath1E(":{root}/RESULT_LIST/RESULT[failed_predicate]/NEW_ELEMENT");
  }

  @Test
  public void testSimpleXPathRewriting(){
    String lString;
    lString = FoxSimplePath.getFoxSimplePathOrNull(":{root}/ELEMENT/ELEMENT").getProcessedPath();
    assertEquals("Simple path which starts with context is rewritten", "./ELEMENT/ELEMENT", lString);

    lString = FoxSimplePath.getFoxSimplePathOrNull("string(:{root}/ELEMENT/ELEMENT)").getProcessedPath();
    assertEquals("Simple path which starts with context is rewritten when nested in string function", "./ELEMENT/ELEMENT", lString);

    lString = FoxSimplePath.getFoxSimplePathOrNull(":{root}/ELEMENT/ELEMENT/text()").getProcessedPath();
    assertEquals("Simple path which starts with context is rewritten when text node is requested", "./ELEMENT/ELEMENT", lString);

    assertEquals("Non-simple path is not rewritten", null, FoxSimplePath.getFoxSimplePathOrNull(":{root}/ELEMENT/ELEMENT//text()"));
    assertEquals("Non-simple path is not rewritten", null, FoxSimplePath.getFoxSimplePathOrNull(":{root}/ELEMENT/ELEMENT[attr = :{theme}/X2]"));

    assertEquals("Invalid path with multiple contexts is not rewritten", null, FoxSimplePath.getFoxSimplePathOrNull(":{root}/ELEMENT/ELEMENT/:{theme}/X2"));
  }

  @Test
  public void testSimpleXPathContexts()
  throws ExActionFailed {
    assertEquals("Theme DOM used even when root DOM is supplied as context node", "FULL", mContextUElem.extendedXPathString(mRootDOM, ":{theme}/CONFIG/NAME_TYPE"));
  }

  @Test
  public void testSetUElem()
  throws ExActionFailed, ExTooFew, ExTooMany {
    DOM lDOM = mThemeDOM.get1E("/*/CONFIG");
    mContextUElem.setUElem(ContextLabel.ACTION, lDOM);
    assertEquals("Action context is set", "CONFIG", mContextUElem.extendedXPathString(mRootDOM, "name(:{action})"));

    mContextUElem.setUElem("testContext", ContextualityLevel.STATE, lDOM);
    assertEquals("testContext context is set", "CONFIG", mContextUElem.extendedXPathString(mRootDOM, "name(:{testContext})"));
  }

  @Test(expected = ExInternal.class)
  public void testSetUElem_failsWhenSettingReservedLabelName()
  throws ExActionFailed, ExTooFew, ExTooMany {
    mContextUElem.setUElem("root", ContextualityLevel.DOCUMENT, mThemeDOM);
  }

  @Test
  public void testClearUElem()
  throws ExActionFailed, ExTooFew, ExTooMany {
    DOM lDOM = mThemeDOM.get1E("/*/CONFIG");

    mContextUElem.setUElem("customAction", ContextualityLevel.STATE, lDOM);
    assertTrue("customAction context is set", mContextUElem.getUElemOrNull("customAction") != null);
    mContextUElem.removeUElem("customAction");
    assertTrue("customAction context is cleared", mContextUElem.getUElemOrNull("customAction") == null);
  }

  @Test(expected = ExInternal.class)
  public void testClearUElem_failsWhenClearingBuiltinDOM()
  throws ExActionFailed, ExTooFew, ExTooMany {
    mContextUElem.removeUElem("root");
  }

  private void contextualityLevelTest(String pPath, ContextualityLevel pExpectedLevel)
  throws ExBadPath {
    contextualityLevelTest(pPath, pExpectedLevel, null);
  }

  private void contextualityLevelTest(String pPath, ContextualityLevel pExpectedLevel, ContextualityLevel pContextNodeContextualityLevel)
  throws ExBadPath {
    FoxPath lPath = mEvaluator.getOrCompilePath(pPath, mRootDOM);
    assertEquals("Path has correct contextuality", pExpectedLevel, lPath.getContextualityLevel(mContextUElem, pContextNodeContextualityLevel));
  }

  @Test
  public void testContextualityLevel_ConstantPaths()
  throws ExBadPath {
    //Test integers/decimals
    contextualityLevelTest("3.14",  ContextualityLevel.CONSTANT);
    contextualityLevelTest("5",  ContextualityLevel.CONSTANT);
    contextualityLevelTest("-3",  ContextualityLevel.CONSTANT);
    contextualityLevelTest("+223.34", ContextualityLevel.CONSTANT);
  }

  @Test
  public void testContextualityLevel_SimplePaths()
  throws ExBadPath {
    contextualityLevelTest("/NAME",  ContextualityLevel.DOCUMENT);
    contextualityLevelTest("string(/NAME)",  ContextualityLevel.DOCUMENT);
    contextualityLevelTest("/NAME/text()",  ContextualityLevel.DOCUMENT);
    contextualityLevelTest(":{root}/NAME", ContextualityLevel.DOCUMENT);
    contextualityLevelTest("string(:{root}/NAME)", ContextualityLevel.DOCUMENT);
    contextualityLevelTest("string(:{root}/NAME/text())", ContextualityLevel.DOCUMENT);

    contextualityLevelTest(":{attach}/NAME", ContextualityLevel.STATE);

    //Check that custom contexts affect the contexuality level
    mContextUElem.setUElem("customContext", ContextualityLevel.STATE, mThemeDOM);
    contextualityLevelTest(":{customContext}/*/*", ContextualityLevel.STATE);

    mContextUElem.setUElem("customContext", ContextualityLevel.LOCALISED, mThemeDOM);
    contextualityLevelTest(":{customContext}/*/*", ContextualityLevel.LOCALISED);

    mContextUElem.setUElem("customContext", ContextualityLevel.ITEM, mThemeDOM);
    contextualityLevelTest(":{customContext}/*/*", ContextualityLevel.ITEM);

    contextualityLevelTest("./NAME", ContextualityLevel.ITEM);
    contextualityLevelTest("NAME", ContextualityLevel.ITEM);
    contextualityLevelTest("string(NAME)", ContextualityLevel.ITEM);
    contextualityLevelTest("NAME/text()", ContextualityLevel.ITEM);
    contextualityLevelTest("string(NAME/text())", ContextualityLevel.ITEM);

    //Check that overriding the context element's contextuality level works
    contextualityLevelTest("./NAME", ContextualityLevel.LOCALISED, ContextualityLevel.LOCALISED);
    contextualityLevelTest("NAME", ContextualityLevel.LOCALISED, ContextualityLevel.LOCALISED);

    //Check that overriding the context element's contextuality level doesn't have an effect when it shouldn't
    contextualityLevelTest("/NAME",  ContextualityLevel.DOCUMENT, ContextualityLevel.LOCALISED);
    contextualityLevelTest(":{root}/NAME", ContextualityLevel.DOCUMENT, ContextualityLevel.LOCALISED);
  }

  @Test
  public void testContextualityLevel_ComplexPaths()
  throws ExBadPath {

    mContextUElem.defineUElem(ContextLabel.ITEM, mRootDOM.get1EOrNull("/*/SEARCH_CRITERIA/FIRST_NAME"));

    contextualityLevelTest(":{root}/*/ancestor-or-self::*", ContextualityLevel.DOCUMENT);
    contextualityLevelTest(":{root}/*[X = :{theme}/Y]", ContextualityLevel.DOCUMENT);
    contextualityLevelTest(":{root}/* | :{theme}/*", ContextualityLevel.DOCUMENT);
    contextualityLevelTest("/*/*[name() = 'A']", ContextualityLevel.DOCUMENT);

    contextualityLevelTest(":{action}/*/parent::*", ContextualityLevel.STATE);
    contextualityLevelTest(":{root}/* | :{attach}/*", ContextualityLevel.STATE);
    contextualityLevelTest(":{root}/*/*[X = :{attach}/Y]", ContextualityLevel.STATE);

    contextualityLevelTest(":{root}/* | :{item}/*", ContextualityLevel.ITEM);
    contextualityLevelTest(":{root}/* | ./*", ContextualityLevel.ITEM);
    contextualityLevelTest("./*/*[1]", ContextualityLevel.ITEM);
    contextualityLevelTest("*[1]", ContextualityLevel.ITEM);
    contextualityLevelTest(".[1]", ContextualityLevel.ITEM);

    //Check that overriding the context element's contextuality level works
    contextualityLevelTest(".[1]", ContextualityLevel.LOCALISED, ContextualityLevel.LOCALISED);
    contextualityLevelTest("CONTEXT_NODE[1]", ContextualityLevel.LOCALISED, ContextualityLevel.LOCALISED);
    contextualityLevelTest(":{root}/* | .[1]", ContextualityLevel.LOCALISED, ContextualityLevel.LOCALISED);
    contextualityLevelTest(":{attach}/* | .[1]", ContextualityLevel.LOCALISED, ContextualityLevel.LOCALISED);

    //presence of :{item} should bump it down
    contextualityLevelTest("./* | :{item}/*", ContextualityLevel.ITEM, ContextualityLevel.LOCALISED);
    //attach should take precedence here
    contextualityLevelTest(":{attach}/* | .[1]", ContextualityLevel.STATE, ContextualityLevel.DOCUMENT);


    contextualityLevelTest("3.14[. > 3]", ContextualityLevel.CONSTANT);
    contextualityLevelTest("xs:date('2010-01-01')", ContextualityLevel.CONSTANT);
    contextualityLevelTest("(1 to 5)[. mod 2 = 1]", ContextualityLevel.CONSTANT);
    contextualityLevelTest("string-length('string')", ContextualityLevel.CONSTANT);

    //Edge case - times are ITEM not CONSTANT
    contextualityLevelTest("(1, 2, 3, current-dateTime())", ContextualityLevel.ITEM);
    contextualityLevelTest("(:{root}/BLAH, 2, 3, current-time())", ContextualityLevel.ITEM);
    contextualityLevelTest("(:{attach}/BLAH, 2, 3, current-date())", ContextualityLevel.ITEM);

    //Check that custom contexts affect the contexuality level
    mContextUElem.setUElem("customContext", ContextualityLevel.STATE, mThemeDOM);
    contextualityLevelTest(":{customContext}/*[1]/* | :{root}/*", ContextualityLevel.STATE);

    mContextUElem.setUElem("customContext", ContextualityLevel.LOCALISED, mThemeDOM);
    contextualityLevelTest(":{customContext}/*[1]/* | :{root}/*", ContextualityLevel.LOCALISED);

    mContextUElem.setUElem("customContext", ContextualityLevel.ITEM, mThemeDOM);
    contextualityLevelTest(":{customContext}/*[1]/* | :{root}/*", ContextualityLevel.ITEM);

    //Test "attach" contextuality level is overriden
    mContextUElem.setUElem(ContextLabel.ATTACH.asString(), ContextualityLevel.LOCALISED, mThemeDOM);
    contextualityLevelTest(":{attach}/*[1]/*", ContextualityLevel.LOCALISED);
  }

  @Test( expected = IllegalArgumentException.class )
  public void testContextualityLevel_FailsOnUnknownContext()
  throws ExBadPath {
    contextualityLevelTest(":{not-a-context}/*/ancestor-or-self::*", ContextualityLevel.DOCUMENT);
  }


  @Test
  public void testXPathEvaluator_SimplePaths()
  throws ExBadPath {
    FoxXPathEvaluator lEvaluator = FoxXPathEvaluatorFactory.createEvaluator(true);

    FoxPath lPath = lEvaluator.getOrCompilePath("/NAME", mRootDOM);
    assertEquals("Simple Path supports initial /", FoxSimplePath.class, lPath.getClass());

    //TODO MORE TESTS for different node steps etc
  }

  @Test
  public void testXPathEvaluator_ConstantPaths()
  throws ExBadPath {
    FoxXPathEvaluator lEvaluator = FoxXPathEvaluatorFactory.createEvaluator(true);

    FoxPath lPath = lEvaluator.getOrCompilePath("3.14", mRootDOM);
    assertEquals("Numerical constants are a constant path", FoxConstantPath.class, lPath.getClass());

    //TODO MORE TESTS for different node steps etc
  }

  @Test
  public void testXPathEvaluator_ComplexPaths()
  throws ExBadPath {
    FoxXPathEvaluator lEvaluator = FoxXPathEvaluatorFactory.createEvaluator(true);

    FoxPath lPath = lEvaluator.getOrCompilePath(":{root} | :{theme}", mRootDOM);
    assertEquals("Numerical constants are a constant path", FoxXPath.class, lPath.getClass());

    //TODO MORE TESTS for different node steps etc
  }


  @Test
  public void testSimpleXPathStringFunctionality()
  throws ExActionFailed {

    assertTrue("Simple path boolean text condition", mContextUElem.extendedXPathBoolean(mRootDOM, ":{root}/SEARCH_CRITERIA/FIRST_NAME/text()"));
    assertTrue("Simple path boolean text condition in string function", mContextUElem.extendedXPathBoolean(mRootDOM, "string(:{root}/SEARCH_CRITERIA/FIRST_NAME/text())"));
    assertTrue("Simple path boolean string function", mContextUElem.extendedXPathBoolean(mRootDOM, "string(:{root}/SEARCH_CRITERIA/FIRST_NAME)"));
    assertTrue("Simple path boolean string function", mContextUElem.extendedXPathBoolean(mRootDOM, "string(:{root})"));
    assertTrue("Simple path boolean string function", mContextUElem.extendedXPathBoolean(mRootDOM, "string(.)"));

    assertEquals("Simple path wrapped in string function", "John", mContextUElem.extendedXPathString(mRootDOM, "string(:{root}/SEARCH_CRITERIA/FIRST_NAME)"));
    assertEquals("Simple path returns string function", "John", mContextUElem.extendedXPathString(mRootDOM, ":{root}/SEARCH_CRITERIA/FIRST_NAME"));
    assertEquals("Simple path returns string function", "John", mContextUElem.extendedXPathString(mRootDOM, ":{root}/SEARCH_CRITERIA/FIRST_NAME/text()"));

    assertEquals("Simple path as string gets string value of first item in list", "John Smith", mContextUElem.extendedXPathString(mRootDOM, ":{root}/RESULT_LIST/RESULT/NAME"));

    assertFalse("Simple path boolean text condition", mContextUElem.extendedXPathBoolean(mRootDOM, ":{root}/NON_EXISTENT_ELEMENT/text()"));

    assertEquals("XPath result as String", "John", mContextUElem.extendedXPathResult(mRootDOM, ":{root}/SEARCH_CRITERIA/FIRST_NAME/text()").asString());

    assertEquals("XPath result as String List", 3, mContextUElem.extendedXPathResult(mRootDOM, ":{root}/RESULT_LIST/RESULT/NAME/text()").asStringList().size());

    //Check that asking for a DOM result actually yields a node (not a string)
    assertTrue("ExtendedXPath DOM node result is a text node", mContextUElem.extendedXPathResult(mRootDOM, ":{root}/SEARCH_CRITERIA/FIRST_NAME/text()", FoxXPathResultType.DOM_NODE).asResultDOMOrNull().isText());
    assertEquals("ExtendedXPath DOM node result is a text node with correct value", "John", mContextUElem.extendedXPathResult(mRootDOM, ":{root}/SEARCH_CRITERIA/FIRST_NAME/text()", FoxXPathResultType.DOM_NODE).asResultDOMOrNull().value());

    assertTrue("ExtendedXPath DOM list result is a list containing one text node", mContextUElem.extendedXPathResult(mRootDOM, ":{root}/SEARCH_CRITERIA/FIRST_NAME/text()", FoxXPathResultType.DOM_LIST).asDOMList().get(0).isText());
    assertEquals("ExtendedXPath String result is the correct string", "John", mContextUElem.extendedXPathResult(mRootDOM, ":{root}/SEARCH_CRITERIA/FIRST_NAME/text()", FoxXPathResultType.STRING).asString());

    //Check we can get multiple text nodes
    assertEquals("ExtendedXPath DOM list result is a DOM list containing 4 text nodes", 4, mContextUElem.extendedXPathResult(mRootDOM, ":{html}/body/p/text()", FoxXPathResultType.DOM_LIST).asDOMList().size());
    assertEquals("ExtendedXPath DOM list result text nodes are in document order", "Paragraph 1 ", mContextUElem.extendedXPathResult(mRootDOM, ":{html}/body/p/text()", FoxXPathResultType.DOM_LIST).asDOMList().get(0).value());
    assertEquals("ExtendedXPath DOM list result text nodes are in document order", " further text", mContextUElem.extendedXPathResult(mRootDOM, ":{html}/body/p/text()", FoxXPathResultType.DOM_LIST).asDOMList().get(3).value());

    //Check casting to a String list
    assertEquals("ExtendedXPath DOM list result is a String list containing 4 text nodes", 4, mContextUElem.extendedXPathResult(mRootDOM, ":{html}/body/p/text()", FoxXPathResultType.DOM_LIST).asStringList().size());

    //Check that asking for a String gets the first text node of the result list
    assertEquals("ExtendedXPath String result is string value of first node in result list", "Paragraph 1 ", mContextUElem.extendedXPathResult(mRootDOM, ":{html}/body/p/text()", FoxXPathResultType.STRING).asString());

    //Asking for string gets first node result in list
    assertEquals("ExtendedXPath DOM list result is a String list containing 4 text nodes", "text 1 ",  mContextUElem.extendedXPathResult(mRootDOM, ":{simple-text}/text()", FoxXPathResultType.STRING).asString());

    assertEquals("String function concats text nodes recursively", "\n    John\n    Smith\n    22\n  ", mContextUElem.extendedXPathResult(mRootDOM, "string(:{root}/SEARCH_CRITERIA)").asString());
  }

  @Test(expected = ExPathInternal.class)
  public void testSimpleXPathStringFunctionality_textNodeCardinality()
  throws ExActionFailed {
    //This will return 2 text nodes but we only asked for one node; so should error
    mContextUElem.extendedXPathResult(mRootDOM, ":{simple-text}/text()", FoxXPathResultType.DOM_NODE);
  }

  @Test
  public void testStringValueLogic()
  throws ExActionFailed {

    assertEquals("Simple path - XPath result - shallow value of first p", "Paragraph 1 ", mContextUElem.extendedXPathResult(mHTML, ":{html}/body/*").asString());
    assertEquals("Complex path - XPath result - shallow value of first p", "Paragraph 1 ", mContextUElem.extendedXPathResult(mHTML, ":{html}/body/*[1=1]").asString());

    assertEquals("Simple path - String result - shallow value of first p", "Paragraph 1 ", mContextUElem.extendedXPathString(mHTML, ":{html}/body/*"));
    assertEquals("Complex path - String result - shallow value of first p", "Paragraph 1 ", mContextUElem.extendedXPathString(mHTML, ":{html}/body/*[1=1]"));


    assertEquals("Simple path - XPath result - string() value of body", "Paragraph 1 bold textParagraph 2Paragraph 3 bold text further text", mContextUElem.extendedXPathResult(mHTML, "string(:{html}/body)").asString());
    assertEquals("Complex path - XPath result - string() value of body", "Paragraph 1 bold textParagraph 2Paragraph 3 bold text further text", mContextUElem.extendedXPathResult(mHTML, "string(:{html}/body[1=1])").asString());

    assertEquals("Simple path - String result - string() value of body", "Paragraph 1 bold textParagraph 2Paragraph 3 bold text further text", mContextUElem.extendedXPathString(mHTML, "string(:{html}/body)"));
    assertEquals("Complex path - String result - string() value of body", "Paragraph 1 bold textParagraph 2Paragraph 3 bold text further text", mContextUElem.extendedXPathString(mHTML, "string(:{html}/body[1=1])"));

  }

  @Test (expected = ExPathInternal.class)
  public void testStringValueLogic_failsOnElementCardinality()
  throws ExActionFailed {

    try {
      mContextUElem.extendedXPathString(mHTML, "string(/*/body/p)");
    }
    catch(ExPathInternal ex) {
      if(ex.getMessage().indexOf("string() takes a single-item sequence as an argument but") != -1){
        throw ex;
      }
    }
  }

  @Test (expected = ExPathInternal.class)
  public void testStringValueLogic_failsOnTextNodeCardinality()
  throws ExActionFailed {
    try {
      mContextUElem.extendedXPathString(mHTML, "string(/*/body/p/text())");
    }
    catch(ExPathInternal ex) {
      if(ex.getMessage().indexOf("string() takes a single-item sequence as an argument but") != -1){
        throw ex;
      }
    }
  }

  @Test
  public void testAtomicValueSerialisation()
  throws ExTooFew, ExTooMany, ExActionFailed {

    assertEquals("Dates serialised as expected", "2012-03-22", mContextUElem.extendedXPathResult(mRootDOM, "xs:date('2012-03-22')").asString());
    assertEquals("DateTimes serialised as expected", "2012-03-22T13:05:02", mContextUElem.extendedXPathResult(mRootDOM, "xs:dateTime('2012-03-22T13:05:02')").asString());

    assertEquals("YearMonthDurations serialised as expected", "P1Y2M", mContextUElem.extendedXPathResult(mRootDOM, "xs:yearMonthDuration('P1Y2M')").asString());
    assertEquals("DayTimeDurations serialised as expected", "P1DT22H5M3S", mContextUElem.extendedXPathResult(mRootDOM, "xs:dayTimeDuration('P1DT22H5M3S')").asString());

    assertEquals("Booleans serialise as expected", "true", mContextUElem.extendedXPathResult(mRootDOM, "xs:boolean('true')").asString());

    assertEquals("UntypedAtomics serialise as expected", "this is a string", mContextUElem.extendedXPathResult(mRootDOM, "xs:untypedAtomic('this is a string')").asString());
  }

  @Test
  public void testXSLT()
  throws ExTooMany, ExActionFailed, ExTooFew {

    String lXSLT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:fo=\"http://www.w3.org/1999/XSL/Format\" >\n" +
    "  <xsl:template match=\"RESULT_LIST\">\n" +
    "    <html>\n" +
    "      <head>\n" +
    "        <title>Output</title>\n" +
    "      </head>\n" +
    "      <body>\n" +
    "        <p><xsl:value-of select=\"string(name(:{root}))\"/></p>\n" +
    "        <p><xsl:value-of select=\"string(name(.))\"/></p>\n" +
    "        <ul>\n" +
    "          <xsl:for-each select=\"./RESULT\">\n" +
    "            <li>\n" +
    "              <xsl:value-of select=\"./NAME\"/>\n" +
    "            </li>\n" +
    "          </xsl:for-each>\n" +
    "        </ul>\n" +
    "      </body>\n" +
    "    </html>\n" +
    "  </xsl:template>\n" +
    "</xsl:stylesheet>\n";

    DOM lDest = mThemeDOM.getCreate1E("/*/XSL_OUTPUT");

    XSLTransformerUtil.transformToDOM(mRootDOM, mRootDOM.get1E("/*/RESULT_LIST"), lXSLT, lDest, mContextUElem);

    assertEquals("XSLT creates new elements", "John Smith", mContextUElem.extendedXPathString(mThemeDOM, ":{theme}/XSL_OUTPUT/html/body/ul/li[1]"));
    assertEquals("XSLT supports :{contexts}", "root", mContextUElem.extendedXPathString(mThemeDOM, ":{theme}/XSL_OUTPUT/html/body/p[1]"));
    assertEquals("XSLT allows initial context node to be set", "RESULT_LIST", mContextUElem.extendedXPathString(mThemeDOM, ":{theme}/XSL_OUTPUT/html/body/p[2]"));
  }

  @Test
  public void testXQuery()
  throws ExTooMany, ExActionFailed {
    DOM lDest = mThemeDOM.getCreate1E("/*/XQUERY_OUTPUT");

    String lXQuery =
"for $r in /*/RESULT_LIST/RESULT\n" +
"order by $r/NAME\n" +
"return <p_name>{$r/NAME/text()}</p_name>";

    XQueryUtil.runXQuery(mRootDOM, mRootDOM, lDest, lXQuery, mContextUElem);

    assertEquals("XQuery creates new elements", "A. Jones", mContextUElem.extendedXPathString(mThemeDOM, ":{theme}/XQUERY_OUTPUT/p_name[1]"));

  }

  @Test
  public void testXQuery_WithFoxContext()
  throws ExTooMany, ExActionFailed {
    DOM lDest = mThemeDOM.getCreate1E("/*/XQUERY_OUTPUT");

    String lXQuery =
  "for $r in :{root}/RESULT_LIST/RESULT\n" +
  "order by $r/NAME\n" +
  "return <p_name>{$r/NAME/text()}</p_name>";

    XQueryUtil.runXQuery(mRootDOM, mRootDOM, lDest, lXQuery, mContextUElem);

    assertEquals("XQuery creates new elements", "A. Jones", mContextUElem.extendedXPathString(mThemeDOM, ":{theme}/XQUERY_OUTPUT/p_name[1]"));
  }

  @Test
  public void testXQuery_WithXSRef()
  throws ExTooMany, ExActionFailed {
    DOM lDest = mThemeDOM.getCreate1E("/*/XQUERY_OUTPUT");

    String lXQuery =
  "for $r in ./RESULT_LIST/RESULT\n" +
  "order by $r/NAME\n" +
  "return <p_date>{xs:date('2010-01-01')}</p_date>";

    XQueryUtil.runXQuery(mRootDOM, mRootDOM, lDest, lXQuery, mContextUElem);
    assertEquals("XQuery can access the xs namespace", "2010-01-01", mContextUElem.extendedXPathString(mThemeDOM, ":{theme}/XQUERY_OUTPUT/p_date[1]"));
  }

  @Test
  public void testXQuery_ContextNode()
  throws ExTooMany, ExActionFailed, ExTooFew {
    DOM lDest = mThemeDOM.getCreate1E("/*/XQUERY_OUTPUT");

    String lXQuery =
  "for $r in ./RESULT\n" +
  "order by $r/NAME\n" +
  "return <p_name>{$r/NAME/text()}</p_name>";

    XQueryUtil.runXQuery(mRootDOM, mRootDOM.get1E("./RESULT_LIST"), lDest, lXQuery, mContextUElem);
    assertEquals("XQuery uses the specified context node", "A. Jones", mContextUElem.extendedXPathString(mThemeDOM, ":{theme}/XQUERY_OUTPUT/p_name[1]"));
  }

  @Test
  public void testExistsContextFunction()
  throws ExActionFailed {
    assertEquals("Root context exists", true, mContextUElem.extendedXPathBoolean(mRootDOM, "exists-context(:{root})"));
    assertEquals("Theme context exists", true, mContextUElem.extendedXPathBoolean(mRootDOM, "exists-context(:{theme})"));
    assertEquals("Nonsense context does not exist", false, mContextUElem.extendedXPathBoolean(mRootDOM, "exists-context(:{non-existent})"));
  }

  @Test
  public void testXPathStringRewriting()
  throws ExActionFailed {

    LinkedHashSet<String> lhs = new LinkedHashSet<String>();

    assertEquals(":{context} is rewritten", "fox:ctxt('root')/ABC", SaxonEnvironment.replaceFoxMarkup(":{root}/ABC", lhs));

    assertEquals(":{contexts} are rewritten", "fox:ctxt('root')/ABC/E[fox:ctxt('theme')/A]", SaxonEnvironment.replaceFoxMarkup(":{root}/ABC/E[:{theme}/A]", lhs));
    assertEquals("First label in set is root", "root", lhs.iterator().next());

    assertEquals("exists-context is rewritten", "fox:exists-context('theme')", SaxonEnvironment.replaceFoxMarkup("exists-context(:{theme})", lhs));

    assertEquals("Both exists-context and :{contexts} are rewritten", "fox:ctxt('root')/A[fox:exists-context('theme')]", SaxonEnvironment.replaceFoxMarkup(":{root}/A[exists-context(:{theme})]", lhs));

    assertEquals(
      "Labels in single quotes are not rewritten"
    , "string(concat('<call-module module=\"', fox:ctxt('sys')/module/name, '\" params=\":{params}/*\" />'))"
    , SaxonEnvironment.replaceFoxMarkup("string(concat('<call-module module=\"', :{sys}/module/name, '\" params=\":{params}/*\" />'))", lhs)
    );

    assertEquals("fox:exists-context has context rewritten but prefix is preserved", "fox:exists-context('theme')", SaxonEnvironment.replaceFoxMarkup("fox:exists-context(:{theme})", lhs));

    assertEquals("fox:exists-context is not rewritten if passed string instead of context reference", "fox:exists-context('theme')", SaxonEnvironment.replaceFoxMarkup("fox:exists-context('theme')", lhs));

  }

  @Test
  public void testContextLocalise(){
    mContextUElem.defineUElem(ContextLabel.ACTION, mRootDOM.get1EOrNull("/*/SEARCH_CRITERIA"));
    assertEquals("Action context is as expected before localise", "SEARCH_CRITERIA", mContextUElem.getUElem("action").getName());
    assertEquals("Action context contextuality level is as expected before localise",  ContextualityLevel.STATE, mContextUElem.getLabelContextualityLevel("action"));
    assertFalse("ContextUElem is not localised", mContextUElem.isLocalised());

    mContextUElem.localise("test");
    assertEquals("Action context is the same immediately after localise", "SEARCH_CRITERIA", mContextUElem.getUElem("action").getName());
    assertTrue("ContextUElem is now localised", mContextUElem.isLocalised());

    mContextUElem.setUElem(ContextLabel.ACTION.asString(), ContextualityLevel.LOCALISED, mThemeDOM.get1EOrNull("/*/CONFIG"));
    assertEquals("Action context is correctly changed", "CONFIG", mContextUElem.getUElem("action").getName());
    assertEquals("Action context contextuality level is correctly changed",  ContextualityLevel.LOCALISED, mContextUElem.getLabelContextualityLevel("action"));


    mContextUElem.delocalise("test");
    assertEquals("Action context is reset after localise", "SEARCH_CRITERIA", mContextUElem.getUElem("action").getName());
    assertEquals("Action context contextuality level is reset after localise",  ContextualityLevel.STATE, mContextUElem.getLabelContextualityLevel("action"));
    assertFalse("ContextUElem is not localised", mContextUElem.isLocalised());
  }

  @Test
  public void testContextLocalise_GlobalSet(){
    mContextUElem.defineUElem(ContextLabel.ACTION, mRootDOM.get1EOrNull("/*/SEARCH_CRITERIA"));
    assertEquals("Action context is as expected before localise", "SEARCH_CRITERIA", mContextUElem.getUElem("action").getName());
    assertEquals("Action context contextuality level is as expected before localise",  ContextualityLevel.STATE, mContextUElem.getLabelContextualityLevel("action"));
    assertFalse("ContextUElem is not localised", mContextUElem.isLocalised());

    mContextUElem.localise("test");
    assertEquals("Action context is the same immediately after localise", "SEARCH_CRITERIA", mContextUElem.getUElem("action").getName());
    assertTrue("ContextUElem is now localised", mContextUElem.isLocalised());

    mContextUElem.setUElemGlobal(ContextLabel.ACTION.asString(), ContextualityLevel.LOCALISED, mThemeDOM.get1EOrNull("/*/CONFIG"));
    assertEquals("Action context is correctly changed", "CONFIG", mContextUElem.getUElem("action").getName());
    assertEquals("Action context contextuality level is correctly changed",  ContextualityLevel.LOCALISED, mContextUElem.getLabelContextualityLevel("action"));


    mContextUElem.delocalise("test");
    assertEquals("Action context remains the same after delocalise", "CONFIG", mContextUElem.getUElem("action").getName());
    assertEquals("Action contextuality level remains the same after delocalise",  ContextualityLevel.LOCALISED, mContextUElem.getLabelContextualityLevel("action"));
    assertFalse("ContextUElem is not localised", mContextUElem.isLocalised());
  }

  @Test
  public void testContextLocalise_clearLabel(){
    mContextUElem.setUElem("myLabel", ContextualityLevel.STATE, mRootDOM.get1EOrNull("/*/SEARCH_CRITERIA"));
    assertEquals("myLabel context is as expected before localise", "SEARCH_CRITERIA", mContextUElem.getUElem("myLabel").getName());

    mContextUElem.localise("test");
    assertEquals("myLabel context is the same immediately after localise", "SEARCH_CRITERIA", mContextUElem.getUElem("myLabel").getName());

    mContextUElem.removeUElem("myLabel");

    assertNull("myLabel is removed from localised context", mContextUElem.getUElemOrNull("myLabel"));

    mContextUElem.delocalise("test");
    assertEquals("myLabel context is available after delocalise", "SEARCH_CRITERIA", mContextUElem.getUElem("myLabel").getName());
  }

  @Test
  public void testContextLocalise_clearLabel_global(){
    mContextUElem.setUElem("myLabel", ContextualityLevel.STATE, mRootDOM.get1EOrNull("/*/SEARCH_CRITERIA"));

    mContextUElem.localise("test");

    mContextUElem.removeUElemGlobal("myLabel");

    assertNull("myLabel is removed from localised context", mContextUElem.getUElemOrNull("myLabel"));

    mContextUElem.delocalise("test");

    assertNull("myLabel context is not available after delocalise", mContextUElem.getUElemOrNull("myLabel"));
  }

  private ContextUElem constructNewContextFromExisting() {
    Collection<ContextUElem.SerialisedLabel> lSerialisedLabels = mContextUElem.getSerialisedContextualLabels(ContextualityLevel.STATE);

    ContextUElem lNewContextUElem = new ContextUElem(mRootDOM, ContextLabel.ROOT);
    lNewContextUElem.registerDOMHandler(new TestDOMHandler(mRootDOM, ContextLabel.ROOT.asString()));
    lNewContextUElem.registerDOMHandler(new TestDOMHandler(mThemeDOM, ContextLabel.THEME.asString()));

    lNewContextUElem.deserialiseContextualLabels(lSerialisedLabels);

    return lNewContextUElem;
  }

  @Test
  public void testGetByRefAfterFlush_WhenRefIsLabelled() throws ExActionFailed, ExTooMany {

    //Needed otherwise attach/action are serialised without knowing their DOM's root name
    DOM lRootDOM = mContextUElem.getCreateXPath1E(":{root}");
    DOM lConfigDOM = mContextUElem.getCreateXPath1E(":{theme}/CONFIG");

    //Create a label referencing the target element
    mContextUElem.setUElem("config", ContextualityLevel.STATE, lConfigDOM);

    //Clone the context - this replicates an app server bounce
    ContextUElem lNewContextUElem = constructNewContextFromExisting();

    DOM lRetrievedDOM = lNewContextUElem.getElemByRefOrNull(lConfigDOM.getRef());

    assertNotNull("DOM should be retrived", lRetrievedDOM);
  }

  @Test
  public void testGetByRefAfterFlush_WhenRefNotLabelled() throws ExActionFailed, ExTooMany {

    //Needed otherwise attach/action are serialised without knowing their DOM's root name
    DOM lConfigDOM = mContextUElem.getCreateXPath1E(":{theme}/CONFIG");

    //Clone the context - this replicates an app server bounce
    ContextUElem lNewContextUElem = constructNewContextFromExisting();

    //Add a new DOM handler which errors when DOM is requested so we can assert that the load precedence is obeyed
    TestDOMHandler lDOMHandler = new TestDOMHandler(mHTML, "html") {
      public DOM getDOM() {
        throw new ExInternal("Should not be called");
      }

      public int getLoadPrecedence() {
        return LOAD_PRECEDENCE_LOW; //Others are medium so this should be searched last
      }
    };

    //Register the unused DOM handler
    lNewContextUElem.registerDOMHandler(lDOMHandler);

    DOM lRetrievedDOM = lNewContextUElem.getElemByRefOrNull(lConfigDOM.getRef());

    assertNotNull("DOM should be retrived", lRetrievedDOM);

  }

  /**
   * Should throw an ExInternal because the overloaded TestDOMHandler is loaded before the others
   * @throws ExActionFailed
   * @throws ExTooMany
   */
  @Test(expected = ExInternal.class)
  public void testGetByRef_PrecedenceOrderUsed() throws ExActionFailed, ExTooMany {

    //Needed otherwise attach/action are serialised without knowing their DOM's root name
    DOM lConfigDOM = mContextUElem.getCreateXPath1E(":{theme}/CONFIG");

    //Clone the context - this replicates an app server bounce
    ContextUElem lNewContextUElem = constructNewContextFromExisting();

    //Add a new DOM handler which errors when DOM is requested so we can assert that the load precedence is obeyed
    TestDOMHandler lDOMHandler = new TestDOMHandler(mHTML, "html") {
      public DOM getDOM() {
        throw new ExInternal("Should not be called");
      }

      public int getLoadPrecedence() {
        return LOAD_PRECEDENCE_HIGH; //Others are medium so this should be searched first
      }
    };

    //Register the unused DOM handler
    lNewContextUElem.registerDOMHandler(lDOMHandler);

    //Should cause error
    lNewContextUElem.getElemByRefOrNull(lConfigDOM.getRef());
  }


}


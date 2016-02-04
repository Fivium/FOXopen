package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.thread.ActionRequestContext;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StoredXPathTranslationTest {

  private static final String XPATH_1 = "/*/ELEMENT_1";
  private static final String XPATH_2 = "/*/ELEMENT_2";

  @Test
  public void testBasicXPathTranslation() {

    String lResult;

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1}");
    assertEquals("Basic XPath reference is replaced", XPATH_1, lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "/*/${xpath1}");
    assertEquals("Basic XPath reference is replaced (leading content)", "/*/" + XPATH_1, lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1}/*");
    assertEquals("Basic XPath reference is replaced (trailing content)", XPATH_1 + "/*", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "string('${xpath1}')");
    assertEquals("XPath reference is replaced even if in quotes", "string('" + XPATH_1 + "')", lResult);
  }

  @Test
  public void testInvalidSyntaxTranslation() {

    String lResult;

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "$ {xpath1}");
    assertEquals("XPath not replaced (space between $ and {)", "$ {xpath1}", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "$\\{xpath1}");
    assertEquals("XPath not replaced (slash between $ and {)", "$\\{xpath1}", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1");
    assertEquals("XPath not replaced (missing trailing })", "${xpath1", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath 1}");
    assertEquals("XPath not replaced (space in reference)", "${xpath 1}", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${}");
    assertEquals("XPath not replaced (no reference string)", "${}", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1 ${xpath2}");
    assertEquals("First XPath not replaced (missing trailing }) but second allowed", "${xpath1 " + XPATH_2, lResult);
  }

  @Test
  public void testXPathNameValidation() {

    assertTrue("Name containing alphabetic characters is valid",  StoredXPathTranslator.instance().validateXPathName("myXpath"));
    assertTrue("Name containing numbers is valid",  StoredXPathTranslator.instance().validateXPathName("123"));
    assertTrue("Name containing colon and hyphen is valid",  StoredXPathTranslator.instance().validateXPathName("namespace:my-xpath"));
    assertFalse("Name containing whitespace is not valid",  StoredXPathTranslator.instance().validateXPathName("my xpath"));
    assertFalse("Name containing dollar character is not valid",  StoredXPathTranslator.instance().validateXPathName("$myxpath"));
    assertFalse("Name containing brace characters is not valid",  StoredXPathTranslator.instance().validateXPathName("{myxpath}"));
  }

  @Test
  public void testMultipleXPathTranslation() {

    String lResult;
    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1} | ${xpath2}");
    assertEquals("Multiple XPath references in same string are replaced (with separator)", XPATH_1 + " | " + XPATH_2, lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "/*/${xpath1} | ${xpath2}/text()");
    assertEquals("Multiple XPath references in same string are replaced (with separator and leading/trailing content)", "/*/" + XPATH_1 + " | " + XPATH_2 + "/text()", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1}${xpath2}");
    assertEquals("Multiple XPath references in same string are replaced (no separator)", XPATH_1 + XPATH_2, lResult);
  }

  @Test
  public void testXPathIntegration()
  throws ExTooMany, ExTooFew, ExBadPath {
    //Tests the XPath engine can use the local RequestContext to translate XPath references
    ActionRequestContext lRequestContext = Mockito.mock(ActionRequestContext.class);
    Mockito.when(lRequestContext.getStoredXPathResolver()).thenReturn(new TestXPathResolver());

    SaxonEnvironment.setThreadLocalRequestContext(lRequestContext);

    DOM lDOM = DOM.createDocumentFromXMLString("<ROOT><ELEMENT_1/><ELEMENT_2/><ELEMENT_3/></ROOT>");

    DOM lResult = lDOM.xpath1E("${xpath1}");
    assertNotNull("XPath selected element (simple XPath)", lResult);
    assertEquals("XPath selected correct element", "ELEMENT_1", lResult.getName());

    lResult = lDOM.xpath1E("${xpath1}[1][substring(name(), 1, 1) = 'E']");
    assertNotNull("XPath selected element (complex XPath)", lResult);
    assertEquals("XPath selected correct element", "ELEMENT_1", lResult.getName());

    DOMList lListResult = lDOM.xpathUL("${xpath1} | ${xpath2}");
    assertEquals("XPath selected 2 elements (complex XPath UL)", 2, lListResult.getLength());
  }

  @Test
  public void testXPathReplacementContents() {
    //Tests XPaths with special characters in are substituted into the result XPath string correctly
    //Uses a FixedXPathResolver which always returns a fixed string regardless of the stored XPath name provided

    final String XPATH_STRING = "${xp}";

    String lResult = StoredXPathTranslator.instance().translateXPathReferences(new FixedXPathResolver("/*/SIMPLE"), XPATH_STRING);
    assertEquals("Simple XPath is replaced correctly", "/*/SIMPLE", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new FixedXPathResolver("for $i in //text() return $i"), XPATH_STRING);
    assertEquals("XPath containing $ is replaced correctly", "for $i in //text() return $i", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new FixedXPathResolver("//*[text() = 'A\\B']"), XPATH_STRING);
    assertEquals("XPath containing \\ is replaced correctly", "//*[text() = 'A\\B']", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new FixedXPathResolver("//*[text() = '$A\\B$']"), XPATH_STRING);
    assertEquals("XPath containing $ and \\ is replaced correctly", "//*[text() = '$A\\B$']", lResult);
  }

  private class TestXPathResolver
  implements StoredXPathResolver {
    @Override
    public String resolveXPath(String pXPathName) {
      switch (pXPathName) {
        case "xpath1":
          return XPATH_1;
        case "xpath2":
          return XPATH_2;
        default:
          return null;
      }
    }
  }

  private class FixedXPathResolver
  implements StoredXPathResolver {

    private final String mXPath;

    public FixedXPathResolver(String pXPath) {
      mXPath = pXPath;
    }

    @Override
    public String resolveXPath(String pXPathName) {
      return mXPath;
    }
  }
}

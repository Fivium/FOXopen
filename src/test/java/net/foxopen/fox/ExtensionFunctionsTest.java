package net.foxopen.fox;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExFoxConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class ExtensionFunctionsTest {

  ContextUElem mContextUElem;
  DOM mRootDOM;
  DOM mThemeDOM;

  @Before
  public void setup() throws ExFoxConfiguration {
    mRootDOM = DOM.createDocumentFromXMLString(ContextUElemTest.ROOT_DOC);
    mThemeDOM = DOM.createDocumentFromXMLString(ContextUElemTest.THEME_DOC);

    mContextUElem = new ContextUElem(mRootDOM, ContextLabel.ROOT);
//    mContextUElem.defineUElem(ContextLabel.ROOT, mRootDOM);
//    mContextUElem.defineUElem(ContextLabel.THEME, mThemeDOM);
  }


  @Test
  public void testNvlFunction()
  throws ExActionFailed {
    assertEquals("NVL returns 2nd argument if 1st does not exist", "John",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl(/*/NON_EXISTENT, /*/SEARCH_CRITERIA/FIRST_NAME)"));
    assertEquals("NVL returns first argument if not null (nodes)", "John",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl(/*/SEARCH_CRITERIA/FIRST_NAME, /*/SEARCH_CRITERIA/LAST_NAME)"));
    assertEquals("NVL treats empty strings as null", "hello",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl('', 'hello')"));
    assertEquals("NVL handles mixed argument types", "hello",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl(/*/NON_EXISTENT, 'hello')"));
    assertEquals("NVL handles empty sequences", "hello",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl((), 'hello')"));
    assertEquals("NVL returns numbers", "1",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl((), 1)"));
    assertEquals("NVL returns dates", "2010-01-01",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl((), xs:date('2010-01-01'))"));
    assertEquals("NVL can be treated as path step", "John",  mContextUElem.extendedXPathString(mRootDOM, "/*/SEARCH_CRITERIA/fox:nvl(FIRST_NAME, LAST_NAME)"));
  }

  @Test
  public void testNvl2Function()
  throws ExActionFailed {
    assertEquals("NVL2 returns 3rd argument if 1st does not exist", "Smith",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl2(/*/NON_EXISTENT, /*/SEARCH_CRITERIA/FIRST_NAME, /*/SEARCH_CRITERIA/LAST_NAME)"));
    assertEquals("NVL2 returns 2nd argument if 1st exists", "John",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl2(/*, /*/SEARCH_CRITERIA/FIRST_NAME, /*/SEARCH_CRITERIA/LAST_NAME)"));

    assertEquals("NVL2 returns 3rd argument if 1st does not exist (strings)", "world",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl2('', 'hello', 'world')"));
    assertEquals("NVL2 returns 2nd argument if 1st exists (strings)", "hello",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl2('exists', 'hello', 'world')"));

    assertEquals("NVL2 tests strings and returns nodes", "Smith",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl2('', /*/SEARCH_CRITERIA/FIRST_NAME, /*/SEARCH_CRITERIA/LAST_NAME)"));
    assertEquals("NVL2 tests nodes and returns strings", "hello",  mContextUElem.extendedXPathString(mRootDOM, "fox:nvl2(/*/SEARCH_CRITERIA/FIRST_NAME, 'hello', 'world')"));

  }

}

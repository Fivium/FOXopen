package net.foxopen.fox.module;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.saxon.StoredXPathResolver;
import net.foxopen.fox.ex.ExModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModuleStoredXPathResolverTest {

  @Test
  public void testBasicDefinitions()
  throws ExModule {

    DOM lDefn = DOM.createDocumentFromXMLString("<xpath-list>\n" +
                                                "  <xpath name=\"x1\" value=\"/*/TEST_1\"/>\n" +
                                                "  <xpath name=\"x2\" value=\"/*/TEST_2\"/>\n" +
                                                "</xpath-list>");


    StoredXPathResolver lXPathResolver = ModuleStoredXPathResolver.createFromDOMList(lDefn.getUL("./*"));

    assertEquals("Path correctly resolved", "/*/TEST_1", lXPathResolver.resolveXPath("x1"));
    assertEquals("Path correctly resolved", "/*/TEST_2", lXPathResolver.resolveXPath("x2"));
  }

  @Test
  public void testMultipleReferenceDefinition()
  throws ExModule {

    DOM lDefn = DOM.createDocumentFromXMLString("<xpath-list>\n" +
                                                "  <xpath name=\"x1\" value=\"/*/TEST_1\"/>\n" +
                                                "  <xpath name=\"x2\" value=\"/*/TEST_2\"/>\n" +
                                                "  <xpath name=\"x3\" value=\"${x1} | ${x2}\"/>\n" +
                                                "</xpath-list>");


    StoredXPathResolver lXPathResolver = ModuleStoredXPathResolver.createFromDOMList(lDefn.getUL("./*"));

    assertEquals("Multiple references in a path correctly resolved", "/*/TEST_1 | /*/TEST_2", lXPathResolver.resolveXPath("x3"));
  }


  @Test
  public void testNestedReferences_twoLevels()
  throws ExModule {

    DOM lDefn = DOM.createDocumentFromXMLString("<xpath-list>\n" +
                                                "  <xpath name=\"x1\" value=\"${x2}/TEST_1\"/>\n" +
                                                "  <xpath name=\"x2\" value=\"/*/TEST_2\"/>\n" +
                                                "</xpath-list>");


    StoredXPathResolver lXPathResolver = ModuleStoredXPathResolver.createFromDOMList(lDefn.getUL("./*"));

    assertEquals("Path with nested reference correctly resolved (when declared above)", "/*/TEST_2/TEST_1", lXPathResolver.resolveXPath("x1"));
    assertEquals("Path without nested reference correctly resolved", "/*/TEST_2", lXPathResolver.resolveXPath("x2"));

    lDefn = DOM.createDocumentFromXMLString("<xpath-list>\n" +
                                            "  <xpath name=\"x1\" value=\"/*/TEST_1\"/>\n" +
                                            "  <xpath name=\"x2\" value=\"${x1}/TEST_2\"/>\n" +
                                            "</xpath-list>");


    lXPathResolver = ModuleStoredXPathResolver.createFromDOMList(lDefn.getUL("./*"));

    assertEquals("Path without nested reference correctly resolved", "/*/TEST_1", lXPathResolver.resolveXPath("x1"));
    assertEquals("Path with nested reference correctly resolved (when declared below)", "/*/TEST_1/TEST_2", lXPathResolver.resolveXPath("x2"));
  }

  @Test(expected = ExModule.class)
  public void testFailsOnReferenceCycle()
  throws ExModule {

    DOM lDefn = DOM.createDocumentFromXMLString("<xpath-list>\n" +
                                                "  <xpath name=\"x1\" value=\"${x2}\"/>\n" +
                                                "  <xpath name=\"x2\" value=\"${x1}\"/>\n" +
                                                "</xpath-list>");
    //Should fail because x1 references x2 which references x1, etc
    ModuleStoredXPathResolver.createFromDOMList(lDefn.getUL("./*"));
  }


  @Test(expected = ExModule.class)
  public void testFailsOnSelfReferenceCycle()
  throws ExModule {
    DOM lDefn = DOM.createDocumentFromXMLString("<xpath-list><xpath name=\"x1\" value=\"${x1}\"/></xpath-list>");
    //Should fail because x1 references itself
    ModuleStoredXPathResolver.createFromDOMList(lDefn.getUL("./*"));
  }

  @Test(expected = ExModule.class)
  public void testDuplicatesNotAllowed()
  throws ExModule {

    DOM lDefn = DOM.createDocumentFromXMLString("<xpath-list>\n" +
                                                "  <xpath name=\"x1\" value=\"/*/TEST_1\"/>\n" +
                                                "  <xpath name=\"x1\" value=\"/*/TEST_1\"/>\n" +
                                                "</xpath-list>");
    //Should fail because x1 is defined twice
    ModuleStoredXPathResolver.createFromDOMList(lDefn.getUL("./*"));
  }

  @Test(expected = ExModule.class)
  public void testFailsOnUndefinedXPath()
  throws ExModule {
    DOM lDefn = DOM.createDocumentFromXMLString("<xpath-list><xpath name=\"x1\" value=\"${x2}\"/></xpath-list>");
    //Should fail because x2 is not defined
    ModuleStoredXPathResolver.createFromDOMList(lDefn.getUL("./*"));
  }

  @Test
  public void testParentResolver()
  throws ExModule {
    DOM lDefn = DOM.createDocumentFromXMLString("<xpath-list><xpath name=\"x1\" value=\"${x2}\"/></xpath-list>");
    //Tests the construction of a ModuleXPath resolver with a given parent resolver
    StoredXPathResolver lXPathResolver = ModuleStoredXPathResolver.createFromDOMList(lDefn.getUL("./*"), x -> {if("x2".equals(x)) return "/*/TEST_2"; else return null;} );

    assertEquals("Path with nested reference defined in parent resolver correctly resolved", "/*/TEST_2", lXPathResolver.resolveXPath("x1"));
  }

}

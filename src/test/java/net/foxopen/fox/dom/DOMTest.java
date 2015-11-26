package net.foxopen.fox.dom;

import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDOM;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.ex.ExValidation;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class DOMTest {

  DOM mDOM;

  static final String TEST_XML_BASIC =
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<ROOT>\n" +
"  <ELEMENT_1 attr1=\"attr 1\">Element 1</ELEMENT_1>\n" +
"  <ELEMENT_2>Element 2</ELEMENT_2>\n" +
"  <ELEMENT_LIST attr2=\"attr 2\">\n" +
"    <ELEMENT_1>List Item 1</ELEMENT_1>\n" +
"    <ELEMENT_2>List Item 2</ELEMENT_2>\n" +
"    <ELEMENT_3>List Item 3</ELEMENT_3>\n" +
"  </ELEMENT_LIST>\n" +
"  <ELEMENT_3>\n" +
"    <SUB_ELEMENT>Sub Element</SUB_ELEMENT>\n" +
"  </ELEMENT_3>\n" +
"  <ELEMENT_4>\n" +
"    <SUB_ELEMENT_2>Sub Element</SUB_ELEMENT_2>\n" +
"  </ELEMENT_4>\n" +
"</ROOT>\n" ;

  static final String TEST_XML_NAMESPACES =
"<ns1:root xmlns:ns1=\"http://ns1.com\" xmlns:ns2=\"http://ns2.com\" ns1:attr1=\"root_attr_ns1\">\n" +
"  <ns1:element_1 ns1:attr1=\"attr 1\" ns1:attr2=\"attr 2\" ns2:attr2=\"attr 2\">Element 1</ns1:element_1>\n" +
"  <ns2:element_2>Element 2</ns2:element_2>\n" +
"  <ns1:same_name>Same Name NS1</ns1:same_name>\n" +
"  <ns2:same_name>Same Name NS2</ns2:same_name>\n" +
"</ns1:root>";

    static final String TEST_XML_NESTED_NAMESPACES =
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<XML xmlns=\"http://www.ns.com/default_ns\" xmlns:ns1=\"http://www.ns.com/ns1\">\n" +
"  <ns1:LEVEL_1>\n" +
"    <ns1:element/>\n" +
"    <LEVEL_2 xmlns:ns2=\"http://www.ns.com/ns2\">\n" +
"      <LEVEL_3/>\n" +
"    </LEVEL_2>\n" +
"  </ns1:LEVEL_1>\n" +
"  <LEVEL_1>\n" +
"    <LEVEL_2_OVERRIDE_NS xmlns:ns1=\"http://www.ns.com/ns1_override\">\n" +
"      <ns1:element>\n" +
"        <ns1:sub_element/>\n" +
"      </ns1:element>\n" +
"    </LEVEL_2_OVERRIDE_NS>\n" +
"  </LEVEL_1>\n" +
"  <LEVEL_1 xmlns:ns1=\"http://www.ns.com/ns1_override\">\n" +
"    <LEVEL_2 xmlns:ns2=\"http://www.ns.com/ns2\">\n" +
"      <LEVEL_3 xmlns=\"http://www.ns.com/default_ns_alt\"/>\n" +
"    </LEVEL_2>\n" +
"  </LEVEL_1>\n" +
"</XML>\n";

  static final String TEST_HTML =
"<html><head><title>Title Text</title><meta>Meta Text</meta></head><body><p>Paragraph <b>with nested bold</b> text.</p></body></html>";

  /**
   * Tests a DOM can be built from a String and the correct elements are created
   */
  @Test
  public void testDOMCreate_FromString()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("Created DOM has root element ROOT", "ROOT", mDOM.get1E(".").getLocalName());
    assertEquals("Created DOM has sub-element ELEMENT_1", "ELEMENT_1", mDOM.get1E("./ELEMENT_1").getLocalName());
    assertEquals("Created DOM has sub-element ELEMENT_3/SUB_ELEMENT", "SUB_ELEMENT", mDOM.get1E("./ELEMENT_3/SUB_ELEMENT").getLocalName());
  }

  /**
   * Tests a basic DOM can be created from an element name only.
   */
  @Test
  public void testDOMCreate_FromElementName()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocument("ROOT");
    assertEquals("Created DOM has root element ROOT", "ROOT", mDOM.get1E(".").getLocalName());
  }

  /**
   * Tests a namespace-aware DOM can be created from an element name and namespace URI only.
   */
  @Test
  public void testDOMCreate_FromElementName_WithNamespace()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocument("ns1:ROOT", "http://ns1.com", true);
    assertEquals("Created DOM has root element ns1:ROOT", "ns1:ROOT", mDOM.get1E(".").getName());
    assertEquals("Created DOM has root element ns1:ROOT and ROOT has correct namespace URI", "http://ns1.com", mDOM.get1E(".").getNamespaceURI());
  }

  /**
   * Tests elements can be added to a DOM.
   */
  @Test
  public void testAddElem()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocument("ROOT");
    mDOM.addElem("ELEMENT_1");
    assertEquals("Created DOM has sub-element ELEMENT_1", "ELEMENT_1", mDOM.get1E("./ELEMENT_1").getLocalName());
  }

  /**
   * Tests elements can be added to a DOM.
   */
  @Test
  public void testGettingNames()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocument("ROOT");
    assertEquals("Non-namespace element's full name", "ROOT", mDOM.getName());
    assertEquals("Non-namespace element's local name", "ROOT", mDOM.getLocalName());
    mDOM = DOM.createDocument("ns1:ROOT", "http://ns1.com", true);
    assertEquals("Non-namespace element's full name contains namespace prefix", "ns1:ROOT", mDOM.getName());
    assertEquals("Non-namespace element's local name does not contain namespace prefix", "ROOT", mDOM.getLocalName());
  }

  /**
   * Tests elements in a namespace can be added to a DOM.
   */
  @Test
  public void testAddElem_WithNamespace()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocument("ROOT");
    mDOM.addElemWithNamespace("ns1:ELEMENT_1", "http://ns1.com");
    assertEquals("Created DOM has sub-element ns1:ELEMENT_1", "ns1:ELEMENT_1", mDOM.get1E("./*").getName());
    assertEquals("Created DOM has sub-element ns1:ELEMENT_1 with correct namespace URI", "http://ns1.com", mDOM.get1E("./*").getNamespaceURI());
  }

  /**
   * Tests elements in a namespace can be added to a DOM if they don't have an explicit prefix (i.e. in a default namespace).
   */
  @Test
  public void testAddElem_WithNamespace_NoPrefix()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocument("ROOT");
    mDOM.addElemWithNamespace("ELEMENT_1", "http://ns1.com");
    assertEquals("Created DOM has sub-element ELEMENT_1", "ELEMENT_1", mDOM.get1E("./*").getName());
    assertEquals("Created DOM has sub-element ELEMENT_1 with correct namespace URI", "http://ns1.com", mDOM.get1E("./*").getNamespaceURI());
  }

  /**
   * Tests comments can be added to an element and are subsequently serialised.
   */
  @Test
  public void testAddComment()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocument("ROOT");
    mDOM.addComment("Test comment");
    assertEquals("Serialised DOM contains comment", "<ROOT>\n  <!--Test comment-->\n</ROOT>", mDOM.outputNodeToString(true));
  }

  /**
   * Tests an XML doctype can be set on a document.
   */
  @Test
  public void testSetDocType()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocument("ROOT");
    mDOM.setDocType("ROOT","TestPubId","TestSysId");
    assertEquals("Serialised document contains doctype declaration", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<!DOCTYPE ROOT PUBLIC \"TestPubId\" \"TestSysId\">\n<ROOT/>\n", mDOM.outputDocumentToString(false));
  }

  /**
   * Tests the various tokens of simple paths work for DOM navigation.
   */
  @Test
  public void testGet1E_PathSyntax()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    assertEquals("'/' is document", DOM.NodeType.DOCUMENT, mDOM.get1E("/").nodeType());

    assertEquals("'.' is root element", "ROOT", mDOM.get1E(".").getLocalName());
    assertEquals("'/*' is root element", "ROOT", mDOM.get1E("/*").getLocalName());

    assertEquals("'.' keeps path pointer at same level", "ROOT", mDOM.get1E("././.").getLocalName());

    assertEquals("Element names are stepped through", "SUB_ELEMENT", mDOM.get1E("/ROOT/ELEMENT_3/SUB_ELEMENT").getLocalName());
    assertEquals("'..' steps path up (1 level)", "ELEMENT_3", mDOM.get1E("/ROOT/ELEMENT_3/SUB_ELEMENT/..").getLocalName());
    assertEquals("'..' steps path up (2 levels)", "ROOT", mDOM.get1E("/ROOT/ELEMENT_3/SUB_ELEMENT/../..").getLocalName());

    assertEquals("'*' retrieves elements of any name", "SUB_ELEMENT_2", mDOM.get1E("/ROOT/ELEMENT_4/*").getLocalName());

    assertEquals("All syntax works together", "SUB_ELEMENT_2", mDOM.get1E("/ROOT/ELEMENT_1/../ELEMENT_4/./*").getLocalName());
  }

  /**
   * Tests the absolute() function, used to print an absolute path to an element.
   */
  @Test
  public void testAbsolutePathValue()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("Root node is resolved from self", "/ROOT", mDOM.get1E(".").absolute());
    assertEquals("Path is stepped up multiple levels", "/ROOT/ELEMENT_4/SUB_ELEMENT_2", mDOM.get1E("/ROOT/ELEMENT_4/SUB_ELEMENT_2").absolute());
  }

  /**
   * Tests String logic for basic XML with no non-leaf text nodes.
   */
  @Test
  public void testGet1S_BasicXML()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("get1S gets the correct string", "Element 1", mDOM.get1S("./ELEMENT_1"));
    assertEquals("get1S gets the correct string", "Sub Element", mDOM.get1S("./ELEMENT_4/SUB_ELEMENT_2"));
  }

  /**
   * Tests String logic for basic XML with no non-leaf text nodes.
   */
  @Test
  public void testGet1SNoEx_BasicXML()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    assertEquals("get1SNoEx gets a string when path matched", "Element 1", mDOM.get1SNoEx("./ELEMENT_1"));
    assertEquals("get1SNoEx returns empty string when path not matched", "", mDOM.get1SNoEx("./NON_EXISTENT_ELEMENT"));
  }

  @Test
  public void testXPath1S()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("xpath1S gets string value of an element", "Element 1", mDOM.xpath1S("/*/ELEMENT_1[1=1]"));
    assertEquals("xpath1S gets string value of an element (with text nodestep)", "Element 1", mDOM.xpath1S("/*/ELEMENT_1[1=1]/text()"));
  }

  /**
   * Tests Xpath1S can get attribute values
   */
  @Test
  public void testXPath1S_AttrValue()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("xpath1S gets string value of an attribute", "attr 1", mDOM.xpath1S("/*/ELEMENT_1/@attr1"));
  }

  @Test
  public void testXPath1E()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("xpath1E gets 1 element", "ELEMENT_1", mDOM.xpath1E("/*/ELEMENT_1[1]").getName());
  }

  @Test
  public void testXPath1E_Namespaces()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_NAMESPACES, true);
    assertEquals("xpath1E gets 1 element", "ns2:same_name", mDOM.xpath1E("/ns1:root/ns2:same_name").getName());
  }

    /**
   * Tests advanced String logic for XML with non-leaf text nodes.
   */
  @Test
  public void testStringValueRetrieval()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_HTML);

    //note: 2 spaces between words
    assertEquals("get1S concatenates same-level text nodes", "Paragraph  text.", mDOM.get1S("/html/body/p"));

    assertEquals("DOM.value method gets leaf text node.", "with nested bold", mDOM.get1E("/html/body/p/b").value());
    //note: 2 spaces between words
    assertEquals("DOM.value method concatenates same-level text nodes", "Paragraph  text.", mDOM.get1E("/html/body/p").value());
    assertEquals("DOM.value method concatenates child's text nodes with target's text nodes", "Paragraph with nested bold text.", mDOM.get1E("/html/body/p").value(true));

    assertEquals("get1SNoEx returns empty string when target has no direct child text nodes", "", mDOM.get1S("/html/head"));
    assertEquals("DOM.value method concatenates all child text nodes, even when target has none", "Title TextMeta Text", mDOM.get1E("/html/head").value(true));

    assertEquals("DOM.value method concatenates text nodes recursively", "Title TextMeta TextParagraph with nested bold text.", mDOM.get1E("/html").value(true));
  }

  /**
   * Tests attribute values can be retrieved correctly.
   */
  @Test
  public void testGetAttr()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("getAttr gets the value of an attribute", "attr 1", mDOM.get1E("/ROOT/ELEMENT_1").getAttr("attr1"));
    assertEquals("getAttr gets an empty string when the attribute does not exist", "", mDOM.get1E("/ROOT/ELEMENT_1").getAttr("not_an_attr"));
  }

  /**
   * Basic test that get1E behaves as expected. If this fails then most other tests in this class will...
   */
  @Test
  public void testGet1E()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("get1E gets an element", "ELEMENT_1", mDOM.get1E("/ROOT/ELEMENT_1").getName());
  }

  /**
   * Tests that get1EOrNull either gets an element if one matches, or returns null.
   */
  @Test
  public void testGet1EOrNull()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("get1EOrNull gets an element if it exists", "ELEMENT_1", mDOM.get1EOrNull("/ROOT/ELEMENT_1").getName());
    assertEquals("get1EOrNull gets null if the element does not exist", null, mDOM.get1EOrNull("/ROOT/NOT_AN_ELEMENT"));
  }

  /**
   * Tests an ExTooMany exception is raised when more than one node matches a path.
   */
  @Test(expected = ExTooMany.class)
  public void testGet1E_FailsOnTooMany()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    //Expected to throw ExTooMany
    String lResult = mDOM.get1E("./ELEMENT_LIST/*").getLocalName();
  }

  /**
   * Tests an ExDOM exception is raised when more than one node matches a path for get1EOrNull.
   */
  @Test(expected = ExDOM.class)
  public void testGet1EOrNull_FailsOnTooMany()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    //Expected to throw ExTooMany
    String lResult = mDOM.get1EOrNull("./ELEMENT_LIST/*").getLocalName();
  }

  /**
   * Test an ExTooFew exception is raised no nodes match a path.
   */
  @Test(expected = ExTooFew.class)
  public void testGet1E_FailsOnTooFew()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    //Expected to throw ExTooFew
    String lResult = mDOM.get1E("./NON_EXISTENT_ELEMENT").getLocalName();
  }

  @Test
  public void testGet1EByLocalName()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("get1EByLocalName gets elements by local name (in DOM with no namespace prefixes)", "ELEMENT_1", mDOM.get1EByLocalName("/*/ELEMENT_1").getName());
  }

  @Test
  public void testGet1EByLocalName_Namespaces()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_NAMESPACES, true);
    assertEquals("get1EByLocalName gets prefixed elements by local name", "ns1:element_1", mDOM.get1EByLocalName("/root/element_1").getName());
  }


  /**
   * Test the previous/next sibling element navigation functions.
   */
  @Test
  public void testSiblingNavigation()
  throws ExTooFew, ExTooMany {

    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    assertEquals("getNextSiblingOrNull gets the element's next sibling", "ELEMENT_3", mDOM.get1E("/ROOT/ELEMENT_LIST/ELEMENT_2").getNextSiblingOrNull(true).getLocalName());
    assertEquals("getPreviousSiblingOrNull gets the element's previous sibling", "ELEMENT_1", mDOM.get1E("/ROOT/ELEMENT_LIST/ELEMENT_2").getPreviousSiblingOrNull(true).getLocalName());

    assertEquals("getNextSiblingOrNull returns null if no next siblings", null, mDOM.get1E("/ROOT/ELEMENT_LIST/ELEMENT_3").getNextSiblingOrNull(true));
    assertEquals("getPreviousSiblingOrNull returns null if no previous siblings", null, mDOM.get1E("/ROOT/ELEMENT_LIST/ELEMENT_1").getPreviousSiblingOrNull(true));
  }

  /**
   * Test the getLastChildElem DOM function works when an element has children.
   */
  @Test
  public void testGetLastChildElem()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("getLastChildElem gets the last child of the node", "ELEMENT_4", mDOM.get1E("/ROOT").getLastChildElem().getLocalName());
  }

  /**
   * Test the getLastChildElem DOM function errors when an element has no children.
   * @throws ExTooFew
   * @throws ExTooMany
   */
  @Test(expected = ExTooFew.class)
  public void testGetLastChildElem_FailsOnNoChildren()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("getLastChildElem fails when node has no children", "ELEMENT_4", mDOM.get1E("/ROOT/ELEMENT_1").getLastChildElem().getLocalName());
  }

  /**
   * Tests the getUL DOM methods for getting DOMLists
   */
  @Test
  public void testGetUL()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    assertEquals("getUL gets all children of a single node (wildcard)", 3, mDOM.getUL("/ROOT/ELEMENT_LIST/*").getLength());
    assertEquals("getUL gets targeted children of a single node (named path)", 1, mDOM.getUL("/ROOT/ELEMENT_LIST/ELEMENT_1").getLength());

    assertEquals("getUL gets all children of multiple nodes", 5, mDOM.getUL("/ROOT/*/*").getLength());

    assertEquals("getUL gets an empty list for no matches", 0, mDOM.getUL("/ROOT/NON_EXISTENT_NODE").getLength());
  }

  @Test
  public void testGetULByLocalName()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    assertEquals("getULByLocalName gets targeted children of a single node (named path)", 1, mDOM.getULByLocalName("/ROOT/ELEMENT_LIST/ELEMENT_1").getLength());

    mDOM = DOM.createDocumentFromXMLString(TEST_XML_NAMESPACES);

    assertEquals("getULByLocalName gets nodes by local name regardless of prefix", 2, mDOM.getULByLocalName("/root/same_name").getLength());
  }

  /**
   * Tests namespace-dependent XML builds and can be processed correctly.
   */
  @Test
  public void testXMLWithNamespaces()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_NAMESPACES);

    assertEquals("Local name of element excludes prefix", "root", mDOM.get1E(".").getLocalName());
    assertEquals("Full name of element includes prefix", "ns1:root", mDOM.get1E(".").getName());

    assertEquals("Path walking works when nodes are in namespaces", "ns1:element_1", mDOM.get1E("/ns1:root/ns1:element_1").getName());

    assertEquals("absolute() includes namespace prefixes", "/ns1:root/ns1:element_1", mDOM.get1E("/ns1:root/ns1:element_1").absolute());

    assertEquals("Path walking handles nodes with same name in different namespaces", "ns1:same_name", mDOM.get1E("/ns1:root/ns1:same_name").getName());
  }

  /**
   * Tests attribute map functionality for basic cases where no namespaces are involved.
   */
  @Test
  public void testAttributeMap_Basic(){
    mDOM = DOM.createDocumentFromXMLString("<ROOT attr1=\"attr 1\"  attr2=\"attr 2\"/>");
    Map<String, String> lMap = mDOM.getAttributeMap();
    assertEquals("Full attribute map contains 2 attributes", 2, lMap.size());

    assertEquals("Full attribute map contains expected attribute", "attr 1", lMap.get("attr1"));
    assertEquals("Full attribute map contains expected attribute", "attr 2", lMap.get("attr2"));

  }

  /**
   * Tests attribute map functionality when multiple namespaces are involved.
   */
  @Test
  public void testAttributeMap_Namespaces()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_NAMESPACES);

    Map<String, String> lMap = mDOM.get1E("/ns1:root/ns1:element_1").getAttributeMap();
    assertEquals("Full attribute map contains 3 attributes", 3, lMap.size());

    assertEquals("Full attribute map contains expected attribute", "attr 1", lMap.get("ns1:attr1"));
    assertEquals("Full attribute map contains expected attribute", "attr 2", lMap.get("ns2:attr2"));
    assertEquals("Full attribute map contains expected attribute", "attr 2", lMap.get("ns1:attr2"));

    lMap = mDOM.get1E("/ns1:root/ns1:element_1").getAttributeMap("http://ns1.com");
    assertEquals("Attribute map for ns1 contains 2 attributes", 2, lMap.size());
    assertEquals("Full attribute map contains expected attribute", "attr 1", lMap.get("ns1:attr1"));
    assertEquals("Full attribute map contains expected attribute", "attr 2", lMap.get("ns1:attr2"));

    lMap = mDOM.get1E("/ns1:root/ns1:element_1").getAttributeMap(null, true);
    assertEquals("When no namespace specified and local names requested, same-name attributes exist only once", 2, lMap.size());
    assertTrue("No-namespace attribute map contains expected attribute", lMap.containsKey("attr1"));
    assertTrue("No-namespace attribute map contains expected attribute", lMap.containsKey("attr2"));

    lMap = mDOM.get1E("/ns1:root/ns1:element_1").getAttributeMap("http://ns2.com");
    assertEquals("Attribute map for ns2 contains 1 attribute", 1, lMap.size());
    assertEquals("Attribute map for ns2 contains expected attribute", "attr 2", lMap.get("ns2:attr2"));
  }

  /**
   * Tests the DOM.createDocument method for DOM cloning (deep clones).
   */
  @Test
  public void testCreateDocument_Deep()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    DOM lClone = mDOM.createDocument();
    assertEquals("Deep clone of root document clones whole document", mDOM.outputDocumentToString(false), lClone.outputDocumentToString(false));

    lClone = mDOM.get1E("/ROOT/ELEMENT_LIST").createDocument();
    assertEquals("Deep clone of sub-root node clones all node's children", "ELEMENT_1", lClone.get1E("/ELEMENT_LIST/ELEMENT_1").getLocalName());
  }

  /**
   * Tests the clone method for DOM cloning (deep clones).
   */
  @Test
  public void testClone_Deep()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    DOM lClone = mDOM.clone(true);
    assertEquals("Deep clone of root document clones whole document", mDOM.outputNodeToString(true), lClone.outputNodeToString(true));
    assertFalse("Cloned node is not in a document", lClone.getDocControl().isAttachedDocControl());
    mDOM.rename("NEWROOT");
    assertEquals("Change to source does not affect clone", "ROOT", lClone.getLocalName());
  }

  /**
   * Tests the DOM.createDocument method for DOM cloning (shallow clones).
   */
  @Test
  public void testCreateDocument_Shallow()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    DOM lClone = mDOM.createDocument(false);
    assertEquals("Shallow clone of root node clones root node only (root node is copied)", "ROOT", lClone.get1E("/ROOT").getLocalName());
    assertEquals("Shallow clone of root node clones root node only (sub-element is not copied)", "", lClone.get1SNoEx("/ROOT/ELEMENT_1"));

    lClone = mDOM.get1E("/ROOT/ELEMENT_1").createDocument(false);
    assertEquals("Shallow clone of sub-root node clones node only (node is copied)", "ELEMENT_1", lClone.get1E("/ELEMENT_1").getLocalName());
    assertEquals("Shallow clone of sub-root node clones node only (attrs are copied)", "attr 1", lClone.get1E("/ELEMENT_1").getAttr("attr1"));
    assertEquals("Shallow clone of sub-root node clones node only (child text node is not copied)", "", lClone.get1SNoEx("/ELEMENT_1"));
  }

  /**
   * Tests the clone method for DOM cloning (shallow clones).
   */
  @Test
  public void testClone_Shallow()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    DOM lClone = mDOM.get1E("ELEMENT_1").clone(false);
    assertEquals("Only element name and attributes are copied (not contents)", "<ELEMENT_1 attr1=\"attr 1\"/>", lClone.outputNodeToString(false));
  }

  /**
   * Tests the DOM serializer for whole documents.
   */
  @Test
  public void testDOMSerializing_Document()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("Serialized version of the DOM matches input version",  TEST_XML_BASIC, mDOM.outputDocumentToString(false));
    //trim input string to remove trailing linebreak which node serializer doesn't add
    assertEquals("Serialized version of DOM as node does not have XML declaration",  TEST_XML_BASIC.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "").trim(), mDOM.outputNodeToString(true));
  }

  @Test
  public void testDOMSerializing_Document_Namespaces()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_NAMESPACES);
    //Note: canonical version required for deterministic attribute order
    assertEquals("Serialized version of the DOM matches input version",  TEST_XML_NAMESPACES, mDOM.outputCanonicalDocumentToString());
  }

  @Test
  public void testDOMSerializing_Document_NestedNamespaces()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_NESTED_NAMESPACES);
    assertEquals("Serialized version of the DOM matches input version",  TEST_XML_NESTED_NAMESPACES, mDOM.outputDocumentToString(false));
  }

  /**
   * Tests the DOM serializer for individual nodes.
   * @throws ExTooFew
   * @throws ExTooMany
   */
  @Test
  public void testDOMSerializing_Node()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("Only the targeted node is serialized", "<ELEMENT_1 attr1=\"attr 1\">Element 1</ELEMENT_1>" , mDOM.get1E("/ROOT/ELEMENT_1").outputNodeToString(false));

    assertEquals("Only the targeted node's contents is serialized", "Element 1" , mDOM.get1E("/ROOT/ELEMENT_1").outputNodeContentsToString(false));

    mDOM = DOM.createDocumentFromXMLString(TEST_HTML);
    assertEquals("Node serialisation serialises nested elements (no pretty print)", "Paragraph <b>with nested bold</b> text." , mDOM.get1E("/html/body/p").outputNodeContentsToString(false));

    assertEquals("Node serialisation serialises nested elements (pretty print)", "Paragraph \n<b>with nested bold</b>\ntext." , mDOM.get1E("/html/body/p").outputNodeContentsToString(true));

    assertEquals("Node serialisation pretty prints nested elements when requested",
    "<head>\n" +
    "  <title>Title Text</title>\n" +
    "  <meta>Meta Text</meta>\n" +
    "</head>\n" +
    "<body>\n" +
    "  <p>Paragraph \n" +
    "    <b>with nested bold</b> text.\n" +
    "  </p>\n" +
    "</body>", mDOM.get1E("/html").outputNodeContentsToString(true));

    assertEquals("Node serialisation does not pretty print nested elements when requested",
    "<head><title>Title Text</title><meta>Meta Text</meta></head><body><p>Paragraph <b>with nested bold</b> text.</p></body>", mDOM.get1E("/html").outputNodeContentsToString(false));
  }

  @Test
  public void testRename() {
    mDOM = DOM.createDocument("ROOT");
    mDOM.rename("RENAMED");
    assertEquals("Root node is renamed", "RENAMED" , mDOM.getName());
    assertEquals("Root node is renamed", "<RENAMED/>" , mDOM.outputNodeToString(false));
  }


  @Test
  public void testCreate1E()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("create1E creates a new Element" , "NEW_ELEMENT", mDOM.create1E("/ROOT/NEW_ELEMENT").get1E("/ROOT/NEW_ELEMENT").getLocalName());
    assertEquals("create1E creates a new Element, even if one of the same name exists" , 2, mDOM.create1E("/ROOT/NEW_ELEMENT").getUL("/ROOT/NEW_ELEMENT").getLength());
    assertEquals("create1E creates new Elements as it walks a path" , "NEW_ELEMENT", mDOM.create1E("/ROOT/NEW_ELEMENT_LEVEL1/NEW_ELEMENT_LEVEL2/NEW_ELEMENT").get1E("/ROOT/NEW_ELEMENT_LEVEL1/NEW_ELEMENT_LEVEL2/NEW_ELEMENT").getLocalName());
  }

  @Test(expected = ExTooMany.class)
  public void testCreate1E_FailsOnTooMany()
  throws ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.create1E("/ROOT/NEW_ELEMENT");
    mDOM.create1E("/ROOT/NEW_ELEMENT");
    mDOM.create1E("/ROOT/NEW_ELEMENT/ANOTHER_NEW_ELEMENT");
  }

  @Test(expected = ExDOMName.class)
  public void testGetCreate1E_FailsOnWildcardElementName()
  throws ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    //This should fail as an element called '*' cannot be created.
    mDOM.getCreate1E("/ROOT/ELEMENT_1/*");
  }

  @Test(expected = ExDOMName.class)
  public void testGetCreate1E_FailsOnBadElementName()
  throws ExTooMany, Exception {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    //This should fail as an element called '123_INVALID_NAME' cannot be created.
    mDOM.getCreate1E("/ROOT/ELEMENT_1/123_INVALID_NAME");
  }

  @Test
  public void testGetCreate1E()
  throws ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("getCreate1E creates a new Element if the path matches no nodes" , "NEW_ELEMENT", mDOM.getCreate1E("/ROOT/NEW_ELEMENT").getLocalName());
    assertEquals("create1E creates a new Element, even if one of the same name exists" , 2, mDOM.create1E("/ROOT/NEW_ELEMENT").getUL("/ROOT/NEW_ELEMENT").getLength());
  }

  @Test(expected =  ExTooMany.class)
  public void testGetCreate1E_FailsOnTooMany()
  throws ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.getCreate1E("/ROOT/ELEMENT_LIST/*");
  }

  @Test
  public void testGetChildElements()
  throws ExTooMany, ExTooFew {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    DOMList lList = mDOM.get1E("/ROOT/ELEMENT_LIST").getChildElements();
    assertEquals("getChildElements gets all child elements (list size is correct)", 3, lList.size());
    assertEquals("getChildElements gets all child elements (element in list is correct)", "ELEMENT_1", lList.item(0).getName());
    assertEquals("getChildElements gets all child elements (element in list is correct)", "ELEMENT_3", lList.item(2).getName());
  }

  @Test
  public void testGetChildNodes()
  throws ExTooMany, ExTooFew {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    //add a new child node for testing purposes
    mDOM.get1E("/ROOT/ELEMENT_LIST").setText("text node");
    DOMList lList = mDOM.get1E("/ROOT/ELEMENT_LIST").getChildNodes();
    assertEquals("getChildNodes gets all child nodes (list size is correct)", 4, lList.size());
    assertEquals("getChildNodes gets all child nodes (text node in list is correct)", "text node", lList.item(0).value());
    assertEquals("getChildNodes gets all child nodes (element in list is correct)", "ELEMENT_1", lList.item(1).getName());
    assertEquals("getChildNodes gets all child nodes (element in list is correct)", "ELEMENT_3", lList.item(3).getName());
  }


  @Test
  public void testCopyToParent()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    mDOM.get1E("/ROOT/ELEMENT_1").copyToParent(mDOM.get1E("/ROOT/ELEMENT_LIST"));

    assertEquals("After copyToParent, new element is in designated parent" , 2, mDOM.getUL("/ROOT/ELEMENT_LIST/ELEMENT_1").getLength());
    assertEquals("After copyToParent, new element has text contents copied" , "Element 1", mDOM.getUL("/ROOT/ELEMENT_LIST/ELEMENT_1").item(1).get1S("."));
    assertEquals("After copyToParent, new element has attributes copied" , "attr 1", mDOM.getUL("/ROOT/ELEMENT_LIST/ELEMENT_1").item(1).getAttr("attr1"));
    assertEquals("After copyToParent, old element remains" , "ELEMENT_1", mDOM.get1E("/ROOT/ELEMENT_1").getLocalName());
  }

  @Test
  public void testMoveToParent()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    mDOM.get1E("/ROOT/ELEMENT_1").moveToParent(mDOM.get1E("/ROOT/ELEMENT_LIST"));

    assertEquals("After moveToParent, new element is in designated parent" , 2, mDOM.getUL("/ROOT/ELEMENT_LIST/ELEMENT_1").getLength());
    assertEquals("After moveToParent, new element has text contents copied" , "Element 1", mDOM.getUL("/ROOT/ELEMENT_LIST/ELEMENT_1").item(1).get1S("."));
    assertEquals("After moveToParent, new element has attributes copied" , "attr 1", mDOM.getUL("/ROOT/ELEMENT_LIST/ELEMENT_1").item(1).getAttr("attr1"));
    assertEquals("After moveToParent, old element has moved" , "", mDOM.get1SNoEx("/ROOT/ELEMENT_1"));
  }

  @Test
  public void testMoveToParentBefore()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    mDOM.get1E("/ROOT/ELEMENT_4").moveToParentBefore(mDOM.get1E("/ROOT/ELEMENT_LIST"), mDOM.get1E("/ROOT/ELEMENT_LIST/ELEMENT_2"));

    assertEquals("After moveToParentBefore, new element is in designated parent" , 1, mDOM.getUL("/ROOT/ELEMENT_LIST/ELEMENT_4").getLength());
    assertEquals("After moveToParentBefore, new element has contents copied" , "Sub Element", mDOM.get1E("/ROOT/ELEMENT_LIST/ELEMENT_4/SUB_ELEMENT_2").get1S("."));
    assertEquals("After moveToParentBefore, old element has moved" , 0, mDOM.getUL("/ROOT/ELEMENT_4").getLength());

    assertEquals("Moved element inserted in position 2" , "ELEMENT_4", mDOM.xpath1E("/ROOT/ELEMENT_LIST/*[2]").getName());
    assertEquals("First element still in first position" , "ELEMENT_1", mDOM.xpath1E("/ROOT/ELEMENT_LIST/*[1]").getName());
    assertEquals("Second element now in positon 3" , "ELEMENT_2", mDOM.xpath1E("/ROOT/ELEMENT_LIST/*[3]").getName());
  }


  @Test
  public void testCopyContentsTo()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    mDOM.get1E("/ROOT/ELEMENT_LIST").copyContentsTo(mDOM.get1E("/ROOT/ELEMENT_4"));

    assertEquals("After copyContentsTo, new elements are in designated parent (list length check)" , 4, mDOM.getUL("/ROOT/ELEMENT_4/*").getLength());
    assertEquals("After copyContentsTo, new elements are in designated parent (element name check)" , 1, mDOM.getUL("/ROOT/ELEMENT_4/ELEMENT_1").getLength());
    assertEquals("copyContentsTo does not copy attributes" , "", mDOM.get1E("/ROOT/ELEMENT_4").getAttr("atrr2"));
    assertEquals("After copyContentsTo, old elements remain", 3, mDOM.getUL("/ROOT/ELEMENT_LIST/*").getLength());
  }

  @Test
  public void testMoveContentsTo()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);

    mDOM.get1E("/ROOT/ELEMENT_LIST").moveContentsTo(mDOM.get1E("/ROOT/ELEMENT_4"));

    assertEquals("After moveContentsTo, new elements are in designated parent (list length check)" , 4, mDOM.getUL("/ROOT/ELEMENT_4/*").getLength());
    assertEquals("After moveContentsTo, new elements are in designated parent (element name check)" , 1, mDOM.getUL("/ROOT/ELEMENT_4/ELEMENT_1").getLength());
    assertEquals("moveContentsTo does not copy attributes" , "", mDOM.get1E("/ROOT/ELEMENT_4").getAttr("atrr2"));
    assertEquals("After moveContentsTo, old elements have moved", 0, mDOM.getUL("/ROOT/ELEMENT_LIST/*").getLength());
  }

  @Test
  public void testSetText()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_HTML);

    mDOM.get1E("/html/head/title").setText("New Title");
    assertEquals("setText overwrites existing text node" , "New Title", mDOM.get1E("/html/head/title").get1S("."));


    mDOM.get1E("/html/body/p").setText("New Paragraph");
    assertEquals("setText overwrites all existing text nodes" , "New Paragraph", mDOM.get1E("/html/body/p").get1S("."));

    mDOM.get1E("/html").setText("HTML Text");
    assertEquals("setText creates a new text node if none exist" , "HTML Text", mDOM.get1E("/html").get1S("."));
  }

  @Test
  public void testSetXMLOrText()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_HTML);

    mDOM.get1E("/html/head/title").setXMLOrText("New Title");
    assertEquals("setXMLOrText sets text content if not XML" , "New Title", mDOM.get1E("/html/head/title").get1S("."));

    mDOM.get1E("/html/head/title").setXMLOrText("<NEW_ELEMENT>Hello</NEW_ELEMENT>");
    assertEquals("setXMLOrText sets XML content if XML", "Hello", mDOM.get1E("/html/head/title/NEW_ELEMENT").get1S("."));
    assertEquals("setXMLOrText does not clear existing content" , "New Title", mDOM.get1E("/html/head/title").get1S("."));

    mDOM = DOM.createDocumentFromXMLString(TEST_HTML);
    mDOM.get1E("/html/head/title").setXMLOrText("<NEW_ELEMENT>Hello</NEW_ELEMENT_2>");
    assertEquals("setXMLOrText sets text content if XML was malformed" , "<NEW_ELEMENT>Hello</NEW_ELEMENT_2>", mDOM.get1E("/html/head/title").get1S("."));
  }

  @Test
  public void testAppendText()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_HTML);

    mDOM.get1E("/html/head/title").appendText(" Appended Text");
    assertEquals("appendText appends text to existing text content" , "Title Text Appended Text", mDOM.get1E("/html/head/title").get1S("."));


    mDOM.get1E("/html/body/p").appendText(" And some more.");
    assertEquals("appendText appends even if multiple text nodes already present" , "Paragraph with nested bold text. And some more.", mDOM.get1E("/html/body/p").value(true));

    mDOM.get1E("/html").appendText("HTML Text");
    assertEquals("appendText creates a new text node if none exist" , "HTML Text", mDOM.get1E("/html").get1S("."));
  }

  @Test(expected = ExTooFew.class)
  public void testRemoveAllChildren()
  throws ExTooFew, ExTooMany {

    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.get1E("/ROOT").removeAllChildren();
    //Test all children are removed - should throw ExTooFew
    mDOM.get1E("/ROOT/*");
  }

  @Test(expected = ExTooFew.class)
  public void testRemoveNode()
  throws ExTooFew, ExTooMany {

    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.get1E("/ROOT/ELEMENT_1").remove();
    //Test child is removed - should throw ExTooFew
    mDOM.get1E("/ROOT/ELEMENT_1");
  }

  @Test
  public void testIsNodeAttached()
  throws ExTooFew, ExTooMany {

    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    DOM lElem = mDOM.get1E("/ROOT/ELEMENT_1");
    assertTrue("DOM reports itself as attached", lElem.isAttached());
    lElem.remove();
    assertTrue("DOM reports itself as not attached", !lElem.isAttached());
  }

  @Test
  public void testSetAttr()
  throws ExTooFew, ExTooMany {

    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.get1E("/ROOT/ELEMENT_1").setAttr("attr1", "new attr1");

    assertEquals("setAttr sets an existing attribute" , "new attr1",  mDOM.get1E("/ROOT/ELEMENT_1").getAttr("attr1"));

    mDOM.get1E("/ROOT/ELEMENT_1").setAttr("new_attr", "testing1");
    assertEquals("setAttr creates a new attribute" , "testing1",  mDOM.get1E("/ROOT/ELEMENT_1").getAttr("new_attr"));
  }

  @Test
  public void testRemoveAttr()
  throws ExTooFew, ExTooMany {

    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.get1E("/ROOT/ELEMENT_1").removeAttr("attr1");

    assertEquals("removeAttr removes an existing attribute" , "",  mDOM.get1E("/ROOT/ELEMENT_1").getAttr("attr1"));

    //Test no exception raised
    mDOM.get1E("/ROOT/ELEMENT_1").removeAttr("non_existent_attr");
  }

  @Test
  public void testXPathUL()
  throws ExTooFew, ExTooMany, ExBadPath {

    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("xpathUL uses Saxon to evaluate complex expressions" , 1,  mDOM.xpathUL("/ROOT/ELEMENT_LIST/*[name() = 'ELEMENT_1']", null).getLength());
  }

  @Test
  public void testXPathBoolean()
  throws ExTooFew, ExTooMany, ExBadPath {

    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("Dot self reference short circuits to true" , true,  mDOM.xpathBoolean("."));
    assertEquals("Simple path short circuits to true" , true,  mDOM.xpathBoolean("ELEMENT_1"));
    assertEquals("Complex path returns true if element exists" , true,  mDOM.xpathBoolean("//ELEMENT_1[text() = 'List Item 1']"));
    assertEquals("Complex path returns true if Xpath function returns true" , true,  mDOM.xpathBoolean("boolean(//ELEMENT_1)"));
    assertEquals("Complex path returns true if Xpath number > 0" , true,  mDOM.xpathBoolean("count(//ELEMENT_1)"));

    assertEquals("Complex path returns false if element does not exist" , false,  mDOM.xpathBoolean("//ELEMENT_1[text() = 'Non Existent']"));
    assertEquals("Complex path returns false if Xpath function returns false" , false,  mDOM.xpathBoolean("boolean(//NON_EXISTENT_ELEMENT)"));
    assertEquals("Complex path returns false if Xpath number = 0" , false,  mDOM.xpathBoolean("count(//NON_EXISTENT_ELEMENT)"));
  }

  @Test
  public void testGetCreateXPathUL()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("getCreateXpathUL returns matched nodes" , "ELEMENT_LIST",  mDOM.getCreateXPathUL("/ROOT/ELEMENT_LIST").item(0).getLocalName());
    assertEquals("getCreateXpathUL creates nodes if required" , "CREATE_NEW_ELEMENT",  mDOM.getCreateXPathUL("/ROOT/ELEMENT_LIST[ELEMENT_1]/CREATE_NEW_ELEMENT").item(0).getLocalName());
  }

  @Test(expected = ExDOMName.class)
  public void testGetCreateXPathUL_FailsOnWildcardAsName()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.getCreateXPathUL("/ROOT/ELEMENT_1[true()]/*");
  }

  @Test(expected = ExBadPath.class)
  public void testGetCreateXPathUL_FailsOnInvalidName()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.getCreateXPathUL("/ROOT/ELEMENT_LIST[true()]/123_INVALID_NAME");
  }

  @Test(expected = ExDOMName.class)
  public void testGetCreateXPathUL_FailsOnUnsatisfiedPredicate()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.getCreateXPathUL("/ROOT[NOT_SATISFIED]/NEW_ELEMENT");
  }

  @Test
  public void testGetCreateXPath1E()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("getCreateXpath1E returns matched nodes" , "ELEMENT_LIST",  mDOM.getCreateXpath1E("/ROOT/ELEMENT_LIST").getLocalName());
    assertEquals("getCreateXpath1E creates nodes if required" , "CREATE_NEW_ELEMENT",  mDOM.getCreateXpath1E("/ROOT/ELEMENT_LIST[ELEMENT_1]/CREATE_NEW_ELEMENT").getLocalName());
  }

  @Test(expected = ExDOMName.class)
  public void testGetCreateXPath1E_FailsOnWildcardAsName()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.getCreateXpath1E("/ROOT/ELEMENT_1[true()]/*");
  }

  @Test(expected = ExBadPath.class)
  public void testGetCreateXPath1E_FailsOnInvalidName()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.getCreateXpath1E("/ROOT/ELEMENT_LIST[true()]/123_INVALID_NAME");
  }

  @Test(expected = ExDOMName.class)
  public void testGetCreateXPath1E_FailsOnUnsatisfiedPredicate()
  throws ExTooFew, ExTooMany, ExBadPath, ExFoxConfiguration {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    mDOM.getCreateXpath1E("/ROOT[NOT_SATISFIED]/NEW_ELEMENT");
  }

  @Test
  public void testGetAbsolutePathForCreateableXPath()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertEquals("getAbsolutePathForCreateableXPath returns correct path for existing nodes" , "/ROOT/ELEMENT_LIST",  mDOM.getAbsolutePathForCreateableXPath("/*/ELEMENT_LIST", null));
    assertEquals("getAbsolutePathForCreateableXPath resolves predicates on parts of the path which exist" , "/ROOT/ELEMENT_LIST/ELEMENT_1",  mDOM.getAbsolutePathForCreateableXPath("/*/ELEMENT_LIST[ELEMENT_1]/ELEMENT_1", null));
    assertEquals("getAbsolutePathForCreateableXPath returns paths for nodes which don't exist" , "/ROOT/ELEMENT_LIST/DOES/NOT_EXIST",  mDOM.getAbsolutePathForCreateableXPath("/ROOT/ELEMENT_LIST[ELEMENT_1]/DOES/NOT_EXIST", null));
  }

  @Test(expected = ExDOMName.class)
  public void testGetAbsolutePathForCreateableXPath_FailsOnInvalidName()
  throws ExTooFew, ExTooMany, ExBadPath, ExFoxConfiguration {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    //Should fail as ELEMENT_LIST[NO_NODE_HERE] is not matched and a node of that name cannot be created
    mDOM.getAbsolutePathForCreateableXPath("/ROOT/ELEMENT_LIST[NO_NODE_HERE]/ELEMENT_1", null);
  }

  @Test
  public void testXPathUL_Namespaces()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString("<root xmlns:fm=\"http://www.og.dti.gov/fox_module\"><fm:element-1><fm:element-2 attr=\"x\"/></fm:element-1></root>");
    assertEquals("Gets new element", 1, mDOM.get1E("fm:element-1").xpathUL("fm:element-2", null).getLength());
//    assertEquals("Gets new element", 1, mDOM.get1E("fm:element-1").xpathUL("./*[local-name() = 'element-2']", null).getLength());
  }

  @Test
  public void testChildTextNodeRetrieval()
  throws ExTooFew, ExTooMany, ExBadPath {
    mDOM = DOM.createDocumentFromXMLString(TEST_HTML);

    List<String> lList = mDOM.get1E("/html/body/p").childTextNodesAsStringList(false);
    assertEquals("Non-deep only gets text nodes of target element", 2, lList.size());
    assertEquals("Non-deep only gets text nodes of target element", "Paragraph ", lList.get(0));
    assertEquals("Non-deep only gets text nodes of target element", " text.", lList.get(1));

    lList = mDOM.get1E("/html/body/p").childTextNodesAsStringList(true);
    assertEquals("Deep gets all text nodes of target element", 3, lList.size());
    assertEquals("Deep gets all text nodes of target element", "with nested bold", lList.get(1));

    lList = mDOM.getUL("/html/*/*").allChildTextNodesAsStringList(false);
    assertEquals("Non-deep gets text nodes of all target elements", 4, lList.size());

    lList = mDOM.getUL("/html/*/*").allChildTextNodesAsStringList(true);
    assertEquals("Deep gets all text nodes of all target elements", 5, lList.size());
  }

  @Test
  public void testDesiraliseFromSerialiseLineBreaks()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<dummy/>");
    mDOM.addElem("ELEMENT1", "hello");
    mDOM.get1E("ELEMENT1").addElem("CHILD", " child");
    mDOM.addElem("ELEMENT2", "world");
    String lSerialisedString = mDOM.outputDocumentToString(false);
    DOM lDOM2 = DOM.createDocumentFromXMLString(lSerialisedString);
    //Note: pretty printing WILL introduce extra whitespace
    assertEquals("Serialisation and deserialisation does not affect content (no pretty print)", lSerialisedString, lDOM2.outputDocumentToString(false));
    assertEquals("Serialisation and deserialisation does not affect content (no pretty print)", "hello child", mDOM.get1E("ELEMENT1").value(true));
  }

  @Test
  public void testRemovedNodeStillHasActuator()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    DOM lElement = mDOM.get1E("ELEMENT_1");
    lElement.remove();
    lElement.addElem("SUB_ELEMENT", "subelement1");
    assertEquals("Add occurred even when the node was unattached.", "subelement1",  lElement.get1S("SUB_ELEMENT"));
  }

  private static final String TEST_XSD_VERY_BASIC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
  "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n" +
  "  <xs:element name=\"ROOT\">\n" +
  "    <xs:complexType>\n" +
  "      <xs:sequence>\n" +
  "        <xs:element name=\"ELEMENT_1\"/>\n" +
  "        <xs:element name=\"ELEMENT_2\"/>\n" +
  "      </xs:sequence>\n" +
  "    </xs:complexType>\n" +
  "  </xs:element>\n" +
  "</xs:schema>\n";

  private static final String TEST_XML_VERY_BASIC = "<ROOT>\n" +
    "  <ELEMENT_1/>\n" +
    "  <ELEMENT_2/>\n" +
    "</ROOT>\n";

  @Test
  public void testHasContent()
  throws ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    assertTrue("Root element has content (elements)", mDOM.get1E("/ROOT").hasContent());
    assertTrue("Sub-root element has content (text)", mDOM.get1E("/ROOT/ELEMENT_1").hasContent());

    mDOM = DOM.createDocumentFromXMLString("<EMPTY/>");
    assertFalse("Empty root element has no content", mDOM.get1E("/EMPTY").hasContent());
  }

  @Test
  public void testSchemaValidation_considersDOMValid()
  throws ExValidation {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_VERY_BASIC);
    DOM lSchema = DOM.createDocumentFromXMLString(TEST_XSD_VERY_BASIC);
    mDOM.validateAgainstSchema(lSchema);
    assertTrue("Worked without error", true);
  }

  @Test(expected = ExValidation.class)
  public void testSchemaValidation_considersDOMInvalid()
  throws ExValidation {
    mDOM = DOM.createDocumentFromXMLString(TEST_XML_BASIC);
    DOM lSchema = DOM.createDocumentFromXMLString(TEST_XSD_VERY_BASIC);
    mDOM.validateAgainstSchema(lSchema);
  }

  @Test
  public void testXXEInjectionNotPossible()
  throws ExValidation, ExTooFew, ExTooMany {

    String lPathToSecrets = this.getClass().getResource("testfiles/xxe-secret.properties").getPath();
    String lXML =
      "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \"file://"+lPathToSecrets+"\" >]>\n" +
      "<TEST><XXE>&xxe;</XXE></TEST>";

    mDOM = DOM.createDocumentFromXMLString(lXML);

    assertEquals("XXE element not populated (entity is null)", 0, mDOM.get1S("/*/XXE").length());
    assertFalse("File contents does not appear in DOM", mDOM.outputDocumentToString(false).contains("password"));

  }

}

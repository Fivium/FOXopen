package net.foxopen.fox.dom;

import java.util.List;

import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.ex.ExValidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


public class DOMListTest {  

  DOM mDOM;
  
  @Test
  public void testCopyContentsTo() 
  throws ExValidation, ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(DOMTest.TEST_XML_BASIC);
    DOMList lDOMList = mDOM.getUL("/*/ELEMENT_LIST/*");
    lDOMList.copyContentsTo(mDOM.get1E("/*/ELEMENT_3"));
    
    assertEquals("Contents copied to new parent", 4, mDOM.getUL("/*/ELEMENT_3/*").size());
    assertEquals("Contents still in old parent", 3, mDOM.getUL("/*/ELEMENT_LIST/*").size());
  }
  
  @Test
  public void testMoveContentsTo() 
  throws ExValidation, ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(DOMTest.TEST_XML_BASIC);
    DOMList lDOMList = mDOM.getUL("/*/ELEMENT_LIST/*");
    lDOMList.moveContentsTo(mDOM.get1E("/*/ELEMENT_3"));
    
    assertEquals("Contents copied to new parent", 4, mDOM.getUL("/*/ELEMENT_3/*").size());
    assertEquals("Contents no longer in old parent", 0, mDOM.getUL("/*/ELEMENT_LIST/*").size());
  }
  
  @Test
  public void testRenameAll() 
  throws ExValidation, ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(DOMTest.TEST_XML_BASIC);
    DOMList lDOMList = mDOM.getUL("/*/ELEMENT_LIST/*");
    lDOMList.renameAll("RENAMED_ELEMENT");
    
    assertEquals("All elements renamed", 3, mDOM.getUL("/*/ELEMENT_LIST/RENAMED_ELEMENT").size());   
  }
  
  @Test
  public void testChildNodesAsDOMList_Shallow() 
  throws ExValidation, ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(DOMTest.TEST_HTML);
    DOMList lDOMList = mDOM.getUL("/*/head/*").allChildTextNodesAsDOMList(false);    
    
    assertEquals("DOMList contains expected amount of text nodes", 2,  lDOMList.size());
    assertTrue("DOMList gets text nodes",  lDOMList.get(0).isText());
    assertEquals("DOMList contains text nodes in document order", "Title Text",  lDOMList.get(0).value());
    assertEquals("DOMList contains text nodes in document order", "Meta Text",  lDOMList.get(1).value());    
  }
  
  @Test
  public void testChildNodesAsDOMList_Deep() 
  throws ExValidation, ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(DOMTest.TEST_HTML);
    DOMList lDOMList = mDOM.getUL("/*/*").allChildTextNodesAsDOMList(true);
    
    assertEquals("DOMList contains expected amount of text nodes", 5,  lDOMList.size());
    assertEquals("DOMList contains text nodes in document order", "Title Text",  lDOMList.get(0).value());
    assertEquals("DOMList contains text nodes in document order", "Paragraph ",  lDOMList.get(2).value());
    assertEquals("DOMList contains text nodes in document order", " text.",  lDOMList.get(4).value());
  }
  
  @Test
  public void testChildNodesAsStringList_Shallow() 
  throws ExValidation, ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(DOMTest.TEST_HTML);
    List<String> lStringList = mDOM.getUL("/*/head/*").allChildTextNodesAsStringList(false);    
    
    assertEquals("String list contains expected amount of text nodes", 2,  lStringList.size());    
    assertEquals("String list contains text nodes in document order", "Title Text",  lStringList.get(0));
    assertEquals("String list contains text nodes in document order", "Meta Text",  lStringList.get(1));    
  }
  
  @Test
  public void testChildNodesAsStringList_Deep() 
  throws ExValidation, ExTooFew, ExTooMany {
    mDOM = DOM.createDocumentFromXMLString(DOMTest.TEST_HTML);
    List<String> lStringList = mDOM.getUL("/*/*").allChildTextNodesAsStringList(true);    
    
    assertEquals("String list contains expected amount of text nodes", 5,  lStringList.size());
    assertEquals("String list contains text nodes in document order", "Title Text",  lStringList.get(0));
    assertEquals("String list contains text nodes in document order", "Paragraph ",  lStringList.get(2));
    assertEquals("String list contains text nodes in document order", " text.",  lStringList.get(4));
  }
  
}

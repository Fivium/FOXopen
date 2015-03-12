package net.foxopen.fox.thread.persistence;

import java.io.StringWriter;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;

import net.foxopen.fox.thread.persistence.xstream.XStreamManager;

import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class XStreamTest {
  
  
  public static class TestDOMSerialise {
    DOM mDOM;
  }
  
  @Test
  public void testNestedDOMSerialisationL() 
  throws ExActionFailed {
    
    TestDOMSerialise lTestObject = new TestDOMSerialise();
    lTestObject.mDOM = DOM.createDocumentFromXMLString("<root><a>hello</a><b>world</b></root>");
    
    StringWriter lWriter = new StringWriter();

    XStreamManager.getXStream().toXML(lTestObject, lWriter);
    
    TestDOMSerialise lDeserialisedTestObject = (TestDOMSerialise) XStreamManager.getXStream().fromXML(lWriter.toString());
    
    assertEquals("Deserialised object contains correct element a", "hello", lDeserialisedTestObject.mDOM.get1EOrNull("/*/a").value());
    assertEquals("Deserialised object contains correct element b", "world", lDeserialisedTestObject.mDOM.get1EOrNull("/*/b").value());    
    
  }
  
}

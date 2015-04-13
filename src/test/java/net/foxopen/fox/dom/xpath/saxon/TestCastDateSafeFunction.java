package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExPathInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class TestCastDateSafeFunction {

  private static final String TESTING_XML_STRING = "" +
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
  "<FOO xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:fox=\"http://foxopen.net/\">\n" +
  "  <DATE>2001-01-01</DATE>\n" +
  "  <LOW_DATE>2000-01-01</LOW_DATE>\n" +
  "  <NOT_A_DATE>2001a-01-01</NOT_A_DATE>\n" +
  "</FOO>\n";

  public DOM mTestDOM;

  @Before
  public void before() {
    mTestDOM = DOM.createDocumentFromXMLString(TESTING_XML_STRING);
  }

  @Test
  public void testCall()
  throws ExTooMany, ExTooFew, ExBadPath {

    assertEquals("Text nodes containing a valid date return a valid date","2001-01-01", this.mTestDOM.xpath1S("fox:cast-date-safe(/FOO/DATE/text())"));
    assertEquals("Simple element nodes containing valid date text content return a valid date","2001-01-01", this.mTestDOM.xpath1S("fox:cast-date-safe(/FOO/DATE)"));
    assertEquals("Text nodes containing an invalid date return an empty sequence","", this.mTestDOM.xpath1S("fox:cast-date-safe(/FOO/NOT_A_DATE/text())"));
    assertEquals("A non existent node returns an empty sequence","", this.mTestDOM.xpath1S("fox:cast-date-safe(/FOO/NOT_A_NODE/text())"));
    assertEquals("A complex type returns an empty sequence","", this.mTestDOM.xpath1S("fox:cast-date-safe(/FOO)"));
    assertEquals("Dates returned can be compared to other dates","true", this.mTestDOM.xpath1S("fox:cast-date-safe(/FOO/LOW_DATE)<fox:cast-date-safe(/FOO/DATE)"));
    assertEquals("Dates returned can be passed into date functions","2001", this.mTestDOM.xpath1S("year-from-date(fox:cast-date-safe(/FOO/DATE))"));
  }

  @Test (expected = ExBadPath.class)
  public void testCallNoParams()
  throws ExTooMany, ExTooFew, ExBadPath {
    this.mTestDOM.xpath1S("fox:cast-date-safe()");
  }

  @Test  (expected = ExBadPath.class)
  public void testCallTwoParams()
  throws ExTooMany, ExTooFew, ExBadPath {
    this.mTestDOM.xpath1S("fox:cast-date-safe(/FOO, 'hello')");
  }

  @Test (expected = ExPathInternal.class)
  public void testManyNodesMatch()
  throws ExTooMany, ExTooFew, ExBadPath {
    this.mTestDOM.xpath1S("fox:cast-date-safe(/FOO/*)");
  }

}

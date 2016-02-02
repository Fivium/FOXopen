package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PluraliseFunctionTest {

  private DOM mDOM = DOM.createDocumentFromXMLString("<ROOT><SIMPLE>cat</SIMPLE><COMPLEX_SINGLE>country</COMPLEX_SINGLE><COMPLEX_PLURAL>countries</COMPLEX_PLURAL><COUNT>5</COUNT></ROOT>");

  @Test
  public void testSimplePluralise()
  throws ExTooMany, ExTooFew, ExBadPath {

    String lResult = mDOM.xpath1S("fox:pluralise(1, 'cat')");
    assertEquals("2 argument pluralise with item arguments", "1 cat", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(count(./*), 'cat')");
    assertEquals("2 argument pluralise with function result as first argument", "4 cats", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(./COUNT, 'cat')");
    assertEquals("2 argument pluralise with element node as first argument", "5 cats", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(./COUNT/text(), 'cat')");
    assertEquals("2 argument pluralise with text node as first argument", "5 cats", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(2, ./SIMPLE)");
    assertEquals("2 argument pluralise with node as second argument", "2 cats", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(2, '')");
    assertEquals("2 argument pluralise with empty second argument", "2", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(2, ./NOT_A_NODE)");
    assertEquals("2 argument pluralise with non existent node as second argument", "2", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(1.1, 'cat')");
    assertEquals("Floating point arguments are rounded", "1 cat", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(1.5, 'cat')");
    assertEquals("Floating point arguments are rounded", "2 cats", lResult);
  }

  @Test
  public void testExplicitPluralise()
  throws ExTooMany, ExTooFew, ExBadPath {

    String lResult = mDOM.xpath1S("fox:pluralise(1, 'country', 'countries')");
    assertEquals("3 argument pluralise with item arguments", "1 country", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(count(./*), 'country', 'countries')");
    assertEquals("3 argument pluralise with function result as first argument", "4 countries", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(./COUNT, 'country', 'countries')");
    assertEquals("3 argument pluralise with element node as first argument", "5 countries", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(./COUNT/text(), 'country', 'countries')");
    assertEquals("3 argument pluralise with text node as first argument", "5 countries", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(1, ./COMPLEX_SINGLE, ./COMPLEX_PLURAL/text())");
    assertEquals("3 argument pluralise with node arguments", "1 country", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(2, ./COMPLEX_SINGLE, ./COMPLEX_PLURAL/text())");
    assertEquals("3 argument pluralise with node arguments", "2 countries", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(1, ./NOT_A_NODE, ./NOT_A_NODE)");
    assertEquals("3 argument pluralise with missing node arguments", "1", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(2, '', '')");
    assertEquals("3 argument pluralise with empty string arguments", "2", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(1.1, 'country', 'countries')");
    assertEquals("Floating point arguments are rounded", "1 country", lResult);

    lResult = mDOM.xpath1S("fox:pluralise(2.6, 'country', 'countries')");
    assertEquals("Floating point arguments are rounded", "3 countries", lResult);
  }

  @Test(expected = ExBadPath.class)
  public void testFirstArgument_NotEnoughArgs()
  throws ExTooMany, ExTooFew, ExBadPath {
    //Should error on 1 argument
    mDOM.xpath1S("fox:pluralise(1)");
  }

  @Test(expected = ExBadPath.class)
  public void testFirstArgument_EmptySequence()
  throws ExTooMany, ExTooFew, ExBadPath {
    mDOM.xpath1S("fox:pluralise((), 'cat')");
  }

  @Test(expected = ExBadPath.class)
     public void testFirstArgument_NotANumber()
     throws ExTooMany, ExTooFew, ExBadPath {
    mDOM.xpath1S("fox:pluralise('string', 'cat')");
  }
}


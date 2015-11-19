package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExPathInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EncodeBase32FunctionTest {

  private DOM mDOM = DOM.createDocumentFromXMLString("<ROOT><TEXT>hello world</TEXT><GROUP_SIZE>4</GROUP_SIZE><SEPARATOR>_</SEPARATOR></ROOT>");

  @Test
  public void testBasicInvocation()
  throws ExTooMany, ExTooFew, ExBadPath {

    String lResult = mDOM.xpath1S("fox:encode-base32('hello world')");
    assertEquals("Can encode a fixed string", "NBSWY3DPEB3W64TMMQ======", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32(./TEXT)");
    assertEquals("Can encode a element's string value", "NBSWY3DPEB3W64TMMQ======", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32(./TEXT/text())");
    assertEquals("Can encode a text node's string value", "NBSWY3DPEB3W64TMMQ======", lResult);

  }

  @Test
  public void testPadding()
  throws ExTooMany, ExTooFew, ExBadPath {

    String lResult = mDOM.xpath1S("fox:encode-base32('hello world', true())");
    assertEquals("True padding argument includes padding", "NBSWY3DPEB3W64TMMQ======", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false())");
    assertEquals("False padding argument omits padding", "NBSWY3DPEB3W64TMMQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', ./TEXT)");
    assertEquals("Padding argument can be XPath result", "NBSWY3DPEB3W64TMMQ======", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', ./NOT_A_NODE)");
    assertEquals("Padding argument can be XPath result", "NBSWY3DPEB3W64TMMQ", lResult);
  }

  @Test
  public void testSeparator()
  throws ExTooMany, ExTooFew, ExBadPath {

    String lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), 4)");
    assertEquals("Output is separated every 4 characters, default separator is space", "NBSW Y3DP EB3W 64TM MQ", lResult);

     lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), 2)");
    assertEquals("Output is separated every 4 characters, default separator is space", "NB SW Y3 DP EB 3W 64 TM MQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), 0)");
    assertEquals("0 separator argument does not separate output", "NBSWY3DPEB3W64TMMQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), ./GROUP_SIZE)");
    assertEquals("Separator argument can be node based", "NBSW Y3DP EB3W 64TM MQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), ./NOT_A_VALUE)");
    assertEquals("Invalid separator argument results in no separator", "NBSWY3DPEB3W64TMMQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), 4, '-')");
    assertEquals("Separator character can be specified", "NBSW-Y3DP-EB3W-64TM-MQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), 4, '---')");
    assertEquals("Separator character sequence can be specified", "NBSW---Y3DP---EB3W---64TM---MQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), 4, '=')");
    assertEquals("Equals character can be used as a separator", "NBSW=Y3DP=EB3W=64TM=MQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), 4, ./SEPARATOR)");
    assertEquals("XPath node can be used as a separator", "NBSW_Y3DP_EB3W_64TM_MQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), 4, ./NOT_A_VALUE)");
    assertEquals("Empty separator argument results in no separator", "NBSWY3DPEB3W64TMMQ", lResult);

    lResult = mDOM.xpath1S("fox:encode-base32('hello world', false(), 4, '')");
    assertEquals("Empty separator argument results in no separator", "NBSWY3DPEB3W64TMMQ", lResult);
  }

  @Test(expected = ExPathInternal.class)
  public void testNullInput_Fails()
  throws ExTooMany, ExTooFew, ExBadPath {
    mDOM.xpath1S("fox:encode-base32(./NOT_A_VALUE)");
  }

  @Test(expected = ExPathInternal.class)
  public void testEmptyStringInput_Fails()
  throws ExTooMany, ExTooFew, ExBadPath {
    mDOM.xpath1S("fox:encode-base32('')");
  }
}


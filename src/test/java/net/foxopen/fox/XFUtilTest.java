package net.foxopen.fox;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class XFUtilTest {
  public XFUtilTest() {
  }

  /**
   * Test that XFUtil.sanitiseStringForOutput() is giving the expected output
   *
   * @see XFUtil#sanitiseStringForOutput(String,int)
   */
  @Test
  public void testSanitiseStringForOutput() {
    String lNormalString = "Hello World";
    String lDifficultString = "Hello World\r\nSecond line has an apostrophe & quote: ', \"\r\nThird line has a backslash: \\\r\nLast line has a HTML tag: <hr />";
    String lAlertSanitisedString = "Hello World\\nSecond line has an apostrophe & quote: \\', \\\"\\nThird line has a backslash: \\\\nLast line has a HTML tag: <hr />";
    String lHintSanitisedString = "Hello World<br />Second line has an apostrophe & quote: \\', \\\"<br />Third line has a backslash: \\\\<br />Last line has a HTML tag: <hr />";
    String lHTMLSanitisedString = "Hello World\r\nSecond line has an apostrophe &amp; quote: &apos;, &quot;\r\nThird line has a backslash: \\\r\nLast line has a HTML tag: &lt;hr /&gt;";

    // Test a normal string comes back the same
    assertTrue("Normal String remains unmodified", lNormalString.equals(XFUtil.sanitiseStringForOutput(lNormalString, XFUtil.SANITISE_ALERTS)));
    assertTrue("Normal String remains unmodified", lNormalString.equals(XFUtil.sanitiseStringForOutput(lNormalString, XFUtil.SANITISE_HINTS)));
    assertTrue("Normal String remains unmodified", lNormalString.equals(XFUtil.sanitiseStringForOutput(lNormalString, XFUtil.SANITISE_HTMLENTITIES)));

    // Test sanitising difficult strings
    assertTrue("Alerts are sanitised properly", lAlertSanitisedString.equals(XFUtil.sanitiseStringForOutput(lDifficultString, XFUtil.SANITISE_ALERTS)));
    assertTrue("Hints are sanitised properly", lHintSanitisedString.equals(XFUtil.sanitiseStringForOutput(lDifficultString, XFUtil.SANITISE_HINTS)));
    assertTrue("HTML is sanitised properly", lHTMLSanitisedString.equals(XFUtil.sanitiseStringForOutput(lDifficultString, XFUtil.SANITISE_HTMLENTITIES)));
  }

  /**
   * Test that XFUtil.initCap() is giving the expected output
   *
   * @see XFUtil#initCap(String)
   */
  @Test
  public void testInitCap() {
    String lExpectedOutput = "Hello World";
    String lLowerCased = "hello world";
    String lUpperCased = "HELLO WORLD";
    String lUnderscored = "hello_world";

    assertTrue("Text is InitCapped properly", lExpectedOutput.equals(XFUtil.initCap(lLowerCased)));
    assertTrue("Text is InitCapped properly", lExpectedOutput.equals(XFUtil.initCap(lUpperCased)));
    assertTrue("Text is InitCapped properly", lExpectedOutput.equals(XFUtil.initCap(lUnderscored)));
  }
}

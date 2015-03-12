package net.foxopen.fox.spell;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;


/**
 * The HtmlWordParser class contains one public static function to parse HTML looking for words that can then be spellchecked.
 * This tests various levels of awkward HTML for word finding
 */
public class HtmlWordParserTest {
  
  /**
   * HtmlWordParser.parseHtmlForWords() returns an array of HtmlWord objects, which is harder to test against.
   * This functions concatenated the words with spaces to make a sentence that's easily testable
   * @param pHtml
   * @return
   */
  private String concatHtmlParsedWords(String pHtml) {
    StringBuilder lResult = new StringBuilder();
    ArrayList<HtmlWord> lWords = HtmlWordParser.parseHtmlForWords(pHtml);
    for (int i = 0; i < lWords.size(); i++) {
      lResult.append(lWords.get(i).letters.toString());
      if (i != lWords.size()-1) {
        lResult.append(' ');
      }
    }
    return lResult.toString();
  }

  /**
   * @see HtmlWordParser#parseHtmlForWords(String)
   */
  @Test
  public void testParseHtmlForWords() {
    String lExpectedResult = "Hello World's";
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("Hello World's"));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("Hello \"World's\""));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("Hello <strong>World's</strong>"));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("Hello <strong>Wo<em>rl</em>d</strong>'s"));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("Hello&nbsp;<strong>Wo<em>rl</em>d'</strong>s"));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("Hello&nbsp;<strong>Wo&OElig;<em>rl</em>d'</strong>s"));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("H<span class=\"testcls\">ello</span>&nbsp;<strong>Wo&OElig;<em>rl</em>d'</strong>s"));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("H<span class=\"testcls\">ello</span>&nbsp;<strong>Wo&OElig;<em>rl</em>d'</strong>s 1999"));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("Hello&amp;World's"));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("Hello 'World's'"));
    Assert.assertEquals(lExpectedResult, concatHtmlParsedWords("-Hello- 'World's'"));
  }
}

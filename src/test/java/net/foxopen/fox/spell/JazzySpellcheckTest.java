package net.foxopen.fox.spell;


import com.swabunga.spell.engine.SpellDictionary;
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.engine.Word;

import java.io.IOException;
import java.io.StringReader;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * This is for testing the Jazzy Spellchecker library in FOX; Note that it is not testing the interface between clients
 * and FOX for spellchecking.
 */
public class JazzySpellcheckTest {
  private SpellDictionary mJazzyDictionary;

  /**
   * Set up a minimal dictionary with a few simple words to test against
   * @throws IOException
   */
  @Before
  public void constructDictionary() throws IOException {
    mJazzyDictionary = new SpellDictionaryHashMap(new StringReader( "spelled\n" +
                                                                    "spooled\n" +
                                                                    "correctly\n" +
                                                                    "correct\n" +
                                                                    "bad\n" +
                                                                    "badly\n" +
                                                                    "baddy\n" +
                                                                    "baldy\n" +
                                                                    "hyphenated-words\n" +
                                                                    "hyphenated\n" +
                                                                    "hydrated\n" +
                                                                    "words\n" +
                                                                    "word\n" +
                                                                    "apostrophe's\n" +
                                                                    "apostrophe\n" +
                                                                    "unic\u0248de\n"));
  }

  /**
   * Test that the Jazzy Spellchecker can recognise words spelled correctly and incorrectly from the dictionary
   */
  @Test
  public void testSpellcheck() {
    String[] lCorrectWords = {"words", "spelled", "correctly"};
    for (String lWord : lCorrectWords) {
      Assert.assertTrue("Jazzy is reporting words as incorrectly spelled that are correctly spelled: " + lWord, mJazzyDictionary.isCorrect(lWord));
    }
    
    String[] lInorrectWords = {"wurds", "spolled", "baddly"};
    for (String lWord : lInorrectWords) {
      Assert.assertFalse("Jazzy is reporting words as correctly spelled that are incorrectly spelled: " + lWord, mJazzyDictionary.isCorrect(lWord));
    }
    
    String[] lAwkwardWords = {"hyphenated-words", "apostrophe's", "unic\u0248de"};
    for (String lWord : lAwkwardWords) {
      Assert.assertTrue("Jazzy is reporting words as incorrectly spelled that are correctly spelled: " + lWord, mJazzyDictionary.isCorrect(lWord));
    }
  }

  /**
   * Test that the Jazzy spellchecker returns reasonable suggections from the dictionary
   */
  @Test
  public void testSpellcheckSuggestions() {
    List<Word> lCorrectSuggestions;
    List<Word> lSuggestions;

    lCorrectSuggestions = Arrays.asList(new Word("badly", 1), new Word("baddy", 1));
    lSuggestions = mJazzyDictionary.getSuggestions("baddly", 1);
    Assert.assertEquals(lCorrectSuggestions, lSuggestions);

    lCorrectSuggestions = Arrays.asList(new Word("hyphenated-words", 1));
    lSuggestions = mJazzyDictionary.getSuggestions("hyphanated-wurds", 1);
    Assert.assertEquals(lCorrectSuggestions, lSuggestions);

    lCorrectSuggestions = Arrays.asList(new Word("apostrophe's", 1), new Word("apostrophe", 1));
    lSuggestions = mJazzyDictionary.getSuggestions("apostrophes", 1);
    Assert.assertEquals(lCorrectSuggestions, lSuggestions);

    lCorrectSuggestions = Arrays.asList(new Word("unic\u0248de", 1));
    lSuggestions = mJazzyDictionary.getSuggestions("unicode", 1);
    Assert.assertEquals(lCorrectSuggestions, lSuggestions);
  }
}

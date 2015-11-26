package net.foxopen.fox.spell;

import net.foxopen.fox.App;

import java.util.List;


/*
 * The abstract template for FOX spell checker implementations
 */
public abstract class AbstractSpellChecker {

  /**
   * This method should be used to set the dictionary you require for this spell check object.  The actual implementation
   * might load dictionaries from several sources (Files, Database, Strings) and depending on the spell check implementation.
   * Dictionaries should be bootstrapped and cached to improve access times on subsequent uses.
   *
   * @param pApp - The app used to identify which dictionary to load.
   */
  public abstract void setDictionary(App pApp);

  /**
   * Method called to see if the spelling of the provided word is acceptable against the dictionary set for
   * this spell checker.
   *
   * @param pWord - String that should be a single word of which you would like to check the spelling.
   * @return - boolean which is true if the provided word is unknown.
   */
  public abstract boolean isCorrect(String pWord);

  /**
   * If a word has been spelt incorrectly get suggestions will try to return a list of possible spellings using the provided dictionary.  An
   * empty list will be returned if no suggestions are found.
   *
   * @param pWord - The word for which to get suggestions.
   * @return List - A list of possible spellings.  Will be an empty List if no suggestions can be made from the dictionary.
   */
  public abstract List getSuggestions(String pWord);

  /**
   * Split a string into its individual words and return them as a word list.  The word list inturn provides metadata about the
   * words original within the original string.
   *
   * @param pText - A string of text which you would like to spell check.
   * @return List - A list of tokens extracted from the text provided.
   */
  public abstract AbstractWordList tokenise(String pText);

}

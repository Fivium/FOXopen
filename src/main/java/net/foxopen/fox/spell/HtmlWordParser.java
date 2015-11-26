package net.foxopen.fox.spell;

import net.foxopen.fox.XFUtil;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Use the static parseHtmlForWords() function to find all the whole words in a HTML fragment and get their letters and
 * position for each character in the word
 *
 * This was created to enable spellchecking of the TinyMCE WYSIWYG editor
 */
public class HtmlWordParser {
  private static final String SEPARATORS = " !\"#$%()*+,.\\/:;=?@[]^_{|}\r\n\u00a0"; // Unicode at the end is a non-breaking space
  private static final String POTENTIAL_SEPARATORS = "-'";
  private static final String[] TAG_SEPARATORS = {"p", "br"};
  private static final String[] ENTITY_SEPARATORS = {"nbsp", "amp", "lt", "gt"};

  /**
   * Take in HTML as a regular String an walk through it ignoring tags and entities to find the whole words spread
   * throughout the document and the positional offset of each character in the word.
   *
   * @param pHTML HTML fragment as a simple string, not a DOM
   * @return List of HtmlWord objects containing letters in the word and their positional offset
   */
  public static ArrayList<HtmlWord> parseHtmlForWords(String pHTML) {
    ArrayList<HtmlWord> lWords = new ArrayList<>();
    char[] lString = pHTML.toCharArray();
    boolean inTagIgnore = false;
    StringBuilder lTagValue = new StringBuilder();
    boolean inEntityIgnore = false;
    StringBuilder lEntityValue = new StringBuilder();
    HtmlWord lBufferWord = new HtmlWord();

    // Loop through HTML String characters
    for(int i = 0; i < lString.length; i++){
      // Crude ignore HTML tags and entities
      if (lString[i] == '<' && inTagIgnore == false) {
        inTagIgnore = true;
        lTagValue = new StringBuilder();
      }
      else if (lString[i] == '>' && inTagIgnore == true) {
        inTagIgnore = false;
        if (Arrays.asList(TAG_SEPARATORS).contains(lTagValue.toString().replace("/", "").trim())) {
          // If the tag was a known word separator, add the buffered word so far
          bufferWord(lWords, lBufferWord);
          lBufferWord = new HtmlWord();
        }
      }
      else if (lString[i] == '&' && inEntityIgnore == false) {
        inEntityIgnore = true;
        lEntityValue = new StringBuilder();
      }
      else if (lString[i] == ';' && inEntityIgnore == true) {
        inEntityIgnore = false;
        if (Arrays.asList(ENTITY_SEPARATORS).contains(lEntityValue.toString())) {
          // If the entity was a known word separator character, add the buffered word so far
          bufferWord(lWords, lBufferWord);
          lBufferWord = new HtmlWord();
        }
      }
      else if (inTagIgnore == true) {
        lTagValue.append(lString[i]);
      }
      else if (inEntityIgnore == true) {
        lEntityValue.append(lString[i]);
      }
      else if (inTagIgnore == false && inEntityIgnore == false) {
        // If characters are not part of a html tag, they may be a word
        if (isSeparator(lString, i)) {
          // If it's a separator character, check to see if we buffered a word and add it
          bufferWord(lWords, lBufferWord);
          lBufferWord = new HtmlWord();
        }
        else {
          // Add character and offset position to current buffered word
          lBufferWord.offsets.add(i);
          lBufferWord.letters.append(lString[i]);
        }
      }
    } // End loop through HTML String characters

    // Add any final word that was buffered
    bufferWord(lWords, lBufferWord);

    return lWords;
  }

  /**
   * Check to see if current character is a separator, or a potential separator acting as a separator
   * Potential separators are characters that could be a separator in some positions or part of a word in others.
   * For example the hypen could be part of a hyphenated word, like "carbon-neutral", and this should return false.
   * Or it could be used in other ways, like "a - b" or "integer--;", where this should return true
   *
   * @param pString Array of characters that make up the html to find words in
   * @param pPosition Current position of character
   * @return true if the character at pPosition in pString is not part of a word
   */
  private static boolean isSeparator(char[] pString, int pPosition) {
    if (SEPARATORS.indexOf(pString[pPosition]) != -1) {
      // Return true if definitely a separator
      return true;
    }
    else if (POTENTIAL_SEPARATORS.indexOf(pString[pPosition]) != -1) {
      // If a potential separator, check to see if it's part of a word
      int lPreviousCharPosition = pPosition - 1;
      if (lPreviousCharPosition == -1
          || (lPreviousCharPosition > 0 && (POTENTIAL_SEPARATORS.indexOf(pString[lPreviousCharPosition]) != -1 || SEPARATORS.indexOf(pString[lPreviousCharPosition]) != -1))) {
        // Return true if the hyphen is at the start of pString or the character to the left is also a (potential_)separator
        return true;
      }

      int lNextCharPosition = pPosition + 1;
      if (lNextCharPosition == pString.length
          || (lNextCharPosition < pString.length && (POTENTIAL_SEPARATORS.indexOf(pString[lNextCharPosition]) != -1 || SEPARATORS.indexOf(pString[lNextCharPosition]) != -1))) {
        // Return true if the hyphen is at the end of pString or the character to the right is also a (potential_)separator
        return true;
      }
    }
    return false;
  }

  /**
   * Add the currently buffered HtmlWord to the word list if it's buffered something
   *
   * @param pWordList List of words to add the buffered word to
   * @param pWord Buffered up word, with letters and offsets, to add to the list
   */
  private static void bufferWord(ArrayList<HtmlWord> pWordList, HtmlWord pWord) {
    if (pWord.letters.length() > 1 && !XFUtil.isInteger(pWord.letters.toString())) {
      pWordList.add(pWord);
    }
  }
}

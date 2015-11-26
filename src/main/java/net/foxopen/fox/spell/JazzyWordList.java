package net.foxopen.fox.spell;

import com.swabunga.spell.event.StringWordTokenizer;

import net.foxopen.fox.ex.ExInternal;

/**
 * Wrapper class based on a FOX spelling word list for a Jazzy spell check implementation.
 */
public class JazzyWordList
extends AbstractWordList {

  private StringWordTokenizer mTokeniser;

  public JazzyWordList(String pText) {
    mTokeniser = new StringWordTokenizer(pText);
  }

  public String getCurrentWord() {
    if (mTokeniser == null) {
      throw(new ExInternal("Unexpected error: Jazzy tokeniser was null when trying to get current word."));
    }
    return mTokeniser.getContext();
  }

  public int getCurrentWordPosition() {
    if (mTokeniser == null) {
      throw(new ExInternal("Unexpected error: Jazzy tokeniser was null when trying to get current word position."));
    }
    return mTokeniser.getCurrentWordPosition();
  }

  public boolean hasMoreWords() {
    if (mTokeniser == null) {
      throw(new ExInternal("Unexpected error: Jazzy tokeniser was null and thus could not determine whether more words exist."));
    }
    return mTokeniser.hasMoreWords();
  }

  public String nextWord() {
    if (mTokeniser == null) {
      throw(new ExInternal("Unexpected error: Jazzy tokeniser was null and thus failed to get next word."));
    }
    return mTokeniser.nextWord();
  }

}

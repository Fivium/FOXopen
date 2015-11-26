package net.foxopen.fox.spell;

/**
 * Template for methods that should be published for word lists after a string has been tokenised.  Implementation
 * might be a wrapper for third party spell checker implementation.
 */
public abstract class AbstractWordList {

  abstract java.lang.String getCurrentWord();

  abstract int getCurrentWordPosition();

  abstract boolean hasMoreWords();

  abstract java.lang.String nextWord();

}

package net.foxopen.fox.spell;

import java.util.ArrayList;

/**
 * Small class to hold the letters and offsets of a HTML word
 * This is used when spellchecking HTML and you need to store the actual position for each letter in the word
 */
public class HtmlWord {
  public StringBuilder letters = new StringBuilder();
  public ArrayList<Integer> offsets = new ArrayList<Integer>();
}
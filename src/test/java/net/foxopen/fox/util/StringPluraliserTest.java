package net.foxopen.fox.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringPluraliserTest {

  @Test
  public void testPluralise() {

    assertEquals("Pluralisation of empty string returns number", "3", StringPluraliser.pluralise(3, ""));
    assertEquals("Pluralisation of null string returns number", "3", StringPluraliser.pluralise(3, null));

    assertEquals("Pluralisation of single item adds no suffix", "1 cat", StringPluraliser.pluralise(1, "cat"));

    assertEquals("Default pluralisation by adding 's' suffix", "2 cats", StringPluraliser.pluralise(2, "cat"));
    assertEquals("Pluralisation of no items adds 's' suffix", "0 cats", StringPluraliser.pluralise(0, "cat"));
  }

  @Test
  public void testExplicitPluralise() {

    assertEquals("Pluralisation of 1 item uses single form", "1 person", StringPluraliser.explicitPluralise(1, "person", "people"));
    assertEquals("Pluralisation of many items uses plural form", "2 people", StringPluraliser.explicitPluralise(2, "person", "people"));
    assertEquals("Pluralisation of 0 items uses plural form", "0 people", StringPluraliser.explicitPluralise(0, "person", "people"));

    assertEquals("Pluralisation of singular form handles null", "1", StringPluraliser.explicitPluralise(1, null, "people"));
    assertEquals("Pluralisation of plural form handles null", "2", StringPluraliser.explicitPluralise(2, "person", null));

    assertEquals("Pluralisation of singular form handles empty string", "1", StringPluraliser.explicitPluralise(1, "", "people"));
    assertEquals("Pluralisation of plural form handles empty string", "2", StringPluraliser.explicitPluralise(2, "person", ""));
  }
}
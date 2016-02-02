package net.foxopen.fox.util;

import net.foxopen.fox.XFUtil;

public class StringPluraliser {

  /**
   * Pluralises the given word by appending an 's' according to the value of pCount, and returns the count concatenated
   * to the word. E.g. "1 cat", "2 cats". More advanced pluralisation requirements should use {@link #explicitPluralise}.
   * If the word is null or empty, the count is returned on its own.
   * @param pCount Count to use to establish if the plural should be used.
   * @param pWordToPluralise Word being pluralised.
   * @return Count prepended to pluralised word (separated by a space).
   */
  public static String pluralise(int pCount, String pWordToPluralise) {

    pWordToPluralise = XFUtil.nvl(pWordToPluralise).trim();

    //Short circuit if word is null/empty
    if(pWordToPluralise.length() == 0) {
      return Integer.toString(pCount);
    }
    else {
      return pCount + " " + XFUtil.nvl(pWordToPluralise) + (pCount != 1 ? "s" : "");
    }
  }

  /**
   * Chooses the form of a word based on a count, and prepends the count to the correct form, specified by a space.
   * E.g. "1 country", "2 countries". The forms may be null or empty - in which case, the count is returned on its own.
   * @param pCount Count to use to establish if the plural should be used.
   * @param pSingularForm Form of word to use if pCount = 1.
   * @param pPluralForm Form of word to use for pCount != 1.
   * @return Count prepended to correct form of word (separated by a space).
   */
  public static String explicitPluralise(int pCount, String pSingularForm, String pPluralForm) {

    String lFormToUse = XFUtil.nvl(pCount == 1 ? pSingularForm : pPluralForm).trim();

    return (pCount + " " + lFormToUse).trim();
  }
}

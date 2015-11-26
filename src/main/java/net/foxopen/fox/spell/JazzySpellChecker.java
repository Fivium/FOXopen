package net.foxopen.fox.spell;


import com.swabunga.spell.engine.SpellDictionary;
import net.foxopen.fox.App;
import net.foxopen.fox.ex.ExInternal;

import java.util.List;

public class JazzySpellChecker
extends AbstractSpellChecker {
  private SpellDictionary mDictionary;

  public JazzySpellChecker(App pApp) {
    setDictionary(pApp);
  }

  public void setDictionary(App pApp) {
    Dictionary lDict = null;
    if (pApp != null) {
      // Get the dictionary file from the request app or the default
      lDict = pApp.getDictionary();
      if (lDict == null) {
        lDict = pApp.getDefaultDictionary();
      }
    }

    // If no dictionary defined as default or app level
    if (lDict == null) {
      throw new ExInternal("Failed to set dictionary, app level or default dictionary list badly defined or missing");
    }
    else {
      mDictionary = lDict.getJazzyDictionary();
    }
  }

  public boolean isCorrect(String pWord) {
    if (mDictionary == null) {
      throw(new ExInternal("Failed to find a dictionary to check spelling against.  Ensure that dictionary has been set before you check spelling."));
    }
    return mDictionary.isCorrect(pWord);
  }

  public List getSuggestions(String pWord) {
    if (mDictionary == null) {
      throw(new ExInternal("Failed to find a dictionary for spell check suggestions.  Ensure that dictionary has been set before asking for suggestions."));
    }
    return mDictionary.getSuggestions(pWord, 1);
  }

  public AbstractWordList tokenise(String pText) {
    return new JazzyWordList(pText);
  }

}

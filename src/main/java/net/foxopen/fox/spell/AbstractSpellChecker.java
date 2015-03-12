/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.spell;

import net.foxopen.fox.App;

import java.util.List;


/*
 * The abstract template for FOX spell checker implementions
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

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

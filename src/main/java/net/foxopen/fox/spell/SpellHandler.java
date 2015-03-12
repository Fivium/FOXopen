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


import com.swabunga.spell.engine.Word;
import net.foxopen.fox.App;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SpellHandler {

  /**
   * Parse a FoxRequest for a posted spellcheck request DOM containing a list of fields and their content, spellchecking
   * the content and returning a new DOM containing a list of all fields, the words in them that are spelled incorrectly
   * and a list of suggestions and index/offsets for the word.
   *
   * @param pFoxRequest FoxRequest containing the POSTed spellcheck DOM
   * @return A list of fields, incorrectly spelled words and their suggested corrections
   */
  public static final FoxResponse processRequest(FoxRequest pFoxRequest) {

    try {
//      Track.trackPush("ajax-spelling-request");
      // Attempt to give a real response
      DOM lRequestDOM = DOM.createDocument(pFoxRequest.getHttpRequest().getInputStream(), false);

      return processRequestInternal(
        pFoxRequest
      , lRequestDOM
      );
    }
    catch (Throwable ex) {
      Track.recordSuppressedException("SpellRequestError", ex);

      // Wrap any exception in XML and return
      DOM lDOM = DOM.createDocument("ajax-response");
      if (FoxGlobals.getInstance().isProduction()) {
        lDOM.addElem("error", "Failed to handle the spellcheck request. Please contact an administrator.").setAttr("severity", "warning");
      }
      else {
        lDOM.addElem("error", XFUtil.getJavaStackTraceInfo(ex)).setAttr("severity", "warning");
      }

      return new FoxResponseCHAR (
        "text/xml; charset=UTF-8"                       // content type
      , new StringBuffer(lDOM.outputDocumentToString()) // content
      , 0                                               // browser cache MS
      );
    }
  }

  /**
   * Process the spellcheck request
   *
   * @param pFoxRequest FoxRequest to find out the App to find out the dictionary to use
   * @param pRequestDOM Spellcheck request DOM
   * @return The list of fields, incorrectly spelled words and their suggested corrections
   */
  private static final FoxResponse processRequestInternal(FoxRequest pFoxRequest, DOM pRequestDOM) {

    DOM lResponseDOM = DOM.createDocument("ajax-response");
    Map lSuggestedCorrectionMap = new HashMap();
    DOM lFieldList = lResponseDOM.addElem("field-list");

    // Set the dictionary for this spell checking session
    // Get App to find dictionary

    //TODO this will need to get an app from the request context
    App lApp = null;
//
//    try {
//      lApp = pFoxRequest.getRequestApp();
//    }
//    catch (ExApp e) {
//    }
//    catch (ExServiceUnavailable e) {
//    }
    JazzySpellChecker lSpellChecker = new JazzySpellChecker(lApp);

    // Loop over the fields to be checked
    DOMList lRequestFields = null;
    try {
      lRequestFields = pRequestDOM.xpathUL("/*/field-list/field", null);
    }
    catch (ExBadPath e) {
      throw new ExInternal("Failed to run xpaths on spelling request xml");
    }

//    Track.trackPush("field-list");

    for (int i = 0; i < lRequestFields.getLength(); i++) {
//      Track.trackPush("field");

      DOM lRequestField = lRequestFields.item(i);
      DOM lResponseFieldDOM = DOM.createDocument("field");

      DOM lUnknownWordDOM = lResponseFieldDOM.addElem("unknown-word-list");
      String lName;
      String lFieldContent;

      try {
        lName = lRequestField.get1S("name");
        if (lName == null) {
          throw new ExInternal("null field name found in spelling request xml");
        }
      }
      catch (ExTooFew ex) {
        throw new ExInternal("name not specified for field in spelling request xml");
      }
      catch (ExTooMany ex) {
        throw new ExInternal("Only one name permitted in each field of spelling request");
      }

      lResponseFieldDOM.addElem("name", lName);
//      Track.trackAddElementChild("checking-field", lName);

      try {
        lFieldContent = lRequestField.get1S("value");
      }
      catch (ExTooFew ex) {
        throw new ExInternal("name not specified for field in spelling request xml for field '"+ lName +"'");
      }
      catch (ExTooMany ex) {
        throw new ExInternal("Only one value permitted in field '"+lName+"' for spelling request");
      }

//      Track.trackAddElementChild("field-content-length", Integer.toString(lFieldContent.length()));

      if (lRequestField.get1EOrNull("tinymce") != null) {
        ArrayList<HtmlWord> lWordList = HtmlWordParser.parseHtmlForWords(lFieldContent);
        for (HtmlWord lWord : lWordList) {
          if (!lSpellChecker.isCorrect(lWord.letters.toString())) {
            List<Word> suggestionsList = lSpellChecker.getSuggestions(lWord.letters.toString());
            DOM lUnknownDOM = buildTinyMCESuggestionXml(lWord, suggestionsList);
            if(lUnknownDOM != null) {
              lUnknownDOM.copyToParent(lUnknownWordDOM); // build suggestion xml moving to field unknown word DOM
            }
          }
        } // Word loop
      }
      else {
        // Tokenise the field
        AbstractWordList lFieldWords = lSpellChecker.tokenise(lFieldContent);

        // Loop over the individual tokens
        while (lFieldWords.hasMoreWords()) {

          String lWord = lFieldWords.nextWord();
          List lSuggestions;

          // if token is a number then ignore
          String lCharacterContents = lWord.replaceAll("[\\.0-9']*", "");
          if (lCharacterContents.equals("") || lCharacterContents == null) {
            continue;
          }

          // surpress tokens in the ignore list
          // TODO - fit in stop lists

          if(lSuggestedCorrectionMap.containsKey(lWord)) {
            lSuggestions = (List) lSuggestedCorrectionMap.get(lWord);
            DOM lUnknownDOM = buildSuggestionXml(lWord, lFieldWords.getCurrentWordPosition(), lSuggestions);
            if(lUnknownDOM != null) {
              lUnknownDOM.copyToParent(lUnknownWordDOM); // build suggestion xml moving to field unknown word DOM
            }
          }
          else if(!lSpellChecker.isCorrect(lWord)) { // check spelling
            lSuggestions = lSpellChecker.getSuggestions(lWord); // get suggestions if not already in map
            lSuggestedCorrectionMap.put(lWord, lSuggestions); // add to map
            DOM lUnknownDOM = buildSuggestionXml(lWord, lFieldWords.getCurrentWordPosition(), lSuggestions);
            if(lUnknownDOM != null) {
              lUnknownDOM.copyToParent(lUnknownWordDOM); // build suggestion xml moving to field unknown word DOM
            }
          }


        } // Word while loop
      }

      // If words in this field have been marked as unknown then move the field DOM under the field
      if(lUnknownWordDOM.getUL("./unknown-word").getLength() > 0) {
        lResponseFieldDOM.copyToParent(lFieldList);
//        Track.trackAddElementChild("unknown-terms-exist", "true");
      }

//      Track.trackPop();

    } // Spell check field loop

//    Track.trackPop();

    return new FoxResponseCHAR (
      "text/xml; charset=UTF-8"                               // content type
    , new StringBuffer(lResponseDOM.outputDocumentToString()) // content
    , 0                                                       // browser cache MS
    );

  }

  /**
   * Generate the DOM of the suggested word list for an incorrectly spelled word in a field
   *
   * @param pWord The word that is spelled incorrectly
   * @param pIndexStart The position of that word in the field
   * @param pSuggestions A list of suggested correction words
   * @return DOM containing the word suggestion structure
   */
  private static final DOM buildSuggestionXml(String pWord, int pIndexStart, List pSuggestions) {
    DOM lDOM = DOM.createDocument("unknown-word");
    lDOM.addElem("word", pWord);
    lDOM.addElem("index-start", String.valueOf(pIndexStart) );
    lDOM.addElem("index-end", String.valueOf( pIndexStart + pWord.length()) );
    DOM lSuggestionList = lDOM.addElem("suggestion-list");

    boolean lAllCapitalised = pWord.matches("^[A-Z]*"); // check if pWord is capitalised
    boolean lInitCapitalised = pWord.matches("^[A-Z][a-z]*"); // check if first letter of pWord is capitalised followed by lower case

    // Belt and braces to prevent duplicate suggestions
    Set lSuggestedWords = new HashSet();

    // Add the suggestions to xml fragment
    for (Iterator suggestedWord = pSuggestions.iterator(); suggestedWord.hasNext();) {
      String lWord = ((Word)suggestedWord.next()).getWord();
      // Set suggestion case, if word is capitalised offer capitalised suggestion, if init capped then offer init capped suggestions, else offer suggestion as is
      lWord = lAllCapitalised? lWord.toUpperCase() : (lInitCapitalised? lWord.substring(0,1).toUpperCase() + lWord.substring(1) : lWord );

      // if one of the suggested terms equal the unknown after case conversion then don't suggest
      if(pWord.equals(lWord)) {
        return null;
      }

      // prevent duplicate words from being suggested
      if(!lSuggestedWords.contains(lWord)) {
        lSuggestedWords.add(lWord);
        lSuggestionList.addElem("suggestion", lWord);
      }

    }
    return lDOM;
  }

  /**
   * Generate the DOM of the suggested word list for an incorrectly spelled word in a field
   *
   * @param pWord The word that is spelled incorrectly (HtmlWord includes the word and its individual character offsets)
   * @param pSuggestions A list of suggested correction words
   * @return DOM containing the word suggestion structure
   */
  private static final DOM buildTinyMCESuggestionXml(HtmlWord pWord, List pSuggestions) {
    DOM lDOM = DOM.createDocument("unknown-word");
    lDOM.addElem("word", pWord.letters.toString());

    // serialise offsets
    StringBuilder lOffsetsSerialised = new StringBuilder();
    for (Integer lOffset : pWord.offsets) {
      if (lOffsetsSerialised.length() > 0) {
        lOffsetsSerialised.append(',');
      }
      lOffsetsSerialised.append(lOffset);
    }
    lDOM.addElem("offsets", lOffsetsSerialised.toString() );

    DOM lSuggestionList = lDOM.addElem("suggestion-list");

    boolean lAllCapitalised = pWord.letters.toString().matches("^[A-Z]*"); // check if pWord is capitalised
    boolean lInitCapitalised = pWord.letters.toString().matches("^[A-Z][a-z]*"); // check if first letter of pWord is capitalised followed by lower case

    // Belt and braces to prevent duplicate suggestions
    Set lSuggestedWords = new HashSet();

    // Add the suggestions to xml fragment
    for (Iterator suggestedWord = pSuggestions.iterator(); suggestedWord.hasNext();) {
      String lWord = ((Word)suggestedWord.next()).getWord();

      // Try and keep case the same, so long as suggestion isn't case sensitive
      if (lAllCapitalised && !pWord.letters.toString().equals(lWord.toUpperCase())) {
        lWord = lWord.toUpperCase();
      }
      else if (lInitCapitalised && !pWord.letters.toString().equals(lWord.substring(0,1).toUpperCase() + lWord.substring(1))) {
        lWord = lWord.substring(0,1).toUpperCase() + lWord.substring(1);
      }

      // prevent duplicate words from being suggested
      if(!lSuggestedWords.contains(lWord)) {
        lSuggestedWords.add(lWord);
        lSuggestionList.addElem("suggestion", lWord);
      }

    }
    return lDOM;
  }
}

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


import com.swabunga.spell.engine.SpellDictionary;
import com.swabunga.spell.engine.SpellDictionaryHashMap;

import java.io.Reader;
import java.io.StringReader;

import java.util.HashMap;
import java.util.TreeSet;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.ScalarResultType;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;


/**
 * Class for getting and caching the combined-dictionaries from the database
 */
public class Dictionary {
  private static final String QUERY_GET_DICT =  "SELECT dictionary\n" +
                                          "FROM foxmgr.spellcheck_dictionaries\n" +
                                          "WHERE language_mnem = :1";

  private static final ParsedStatement QUERY_GET_DICT_PARSED_STATEMENT = StatementParser.parseSafely(QUERY_GET_DICT, "Get Dictionary");

  private static HashMap<String, Dictionary> CACHED_DICTS = new HashMap<String, Dictionary>();

  private String mName;
  private StringBuilder mDictionaryWords = new StringBuilder();
  private StringBuilder mKey = new StringBuilder();
  private SpellDictionary mJazzyDictionary;

  /**
   * Create and cache a new combined-ditionary from a list of sub-dictionaires stored in the database foxmgr.spellcheck_dictionaries
   *
   * @param pName Name of this combined dictionary
   * @param pDictionaries List of sub-dictionaries to combine
   * @throws ExInternal Throw an error if there is a problem getting the sub-dictionary from the database
   */
  private Dictionary(String pName, TreeSet<String> pDictionaries) throws ExInternal {
    mName = pName;

    char[] lBuff = new char[512];
    UCon lUCon = null;
    Reader lDictReader;
    try{
      lUCon = ConnectionAgent.getConnection(FoxGlobals.getInstance().getEngineConnectionPoolName(), "LoadDictionaries");
      for (String lDictName : pDictionaries) {
        // Construct the key from the list of sub-dictionaries in this combined-dictionary
        mKey.append(lDictName);
        mKey.append(",");

        // Get from database
        lDictReader = lUCon.queryScalarResult(QUERY_GET_DICT_PARSED_STATEMENT, ScalarResultType.CLOB, lDictName).getCharacterStream();
        while (lDictReader.read(lBuff) != -1) {
          mDictionaryWords.append(lBuff);
        }
      }
      mJazzyDictionary = new SpellDictionaryHashMap(new StringReader(mDictionaryWords.toString()));
      mDictionaryWords = null; // Attempt to reclaim some memory now the combined word list is in a Jazzy Dictionary object
    }
    catch(Exception ex) {
       throw new ExInternal("Error reading dictionary from database", ex);
    }
    finally {
      if(lUCon != null) {
        lUCon.closeForRecycle();
      }
    }
  }

  /**
   * Get or create a named combined-dictionary from a list of sub-dictionaries
   *
   * @param pName Name of the combined-dictionary
   * @param pDictionaries Set of sub-dictionaries
   * @return New Dictionary object or cached version if available
   * @throws ExInternal Throw an error if there is a problem getting the sub-dictionary from the database
   */
  public static Dictionary getOrCreateDictionary(String pName, TreeSet<String> pDictionaries) throws ExInternal {
    if (XFUtil.isNull(pName) || (pDictionaries != null && pDictionaries.size() == 0)) {
      //throw new ExInternal("Cannot create a combined-dictionary without a name or a list of sub-dictionaries");
      return null;
    }

    StringBuilder lKey = new StringBuilder();
    for (String lDictName : pDictionaries) {
      lKey.append(lDictName);
      lKey.append(",");
    }

    synchronized (CACHED_DICTS) {
      Dictionary lDict = CACHED_DICTS.get(lKey.toString());
      if (lDict != null) {
        return lDict;
      }
      else {
        lDict = new Dictionary(pName, pDictionaries);
        CACHED_DICTS.put(lKey.toString(), lDict);
        return lDict;
      }
    }
  }

  public String getName() {
    return mName;
  }

  public String getKey() {
    return mKey.toString();
  }

  public SpellDictionary getJazzyDictionary() {
    return mJazzyDictionary;
  }

  /**
   * Clear all the cached combined dictionaries
   */
  public static void flushCachedDictionaries() {
    CACHED_DICTS.clear();
  }
}

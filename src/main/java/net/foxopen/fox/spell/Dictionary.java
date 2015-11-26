package net.foxopen.fox.spell;


import com.swabunga.spell.engine.SpellDictionary;
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.ScalarResultType;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.TreeSet;


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
   * Create and cache a new combined-dictionary from a list of sub-dictionaries stored in the database foxmgr.spellcheck_dictionaries
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

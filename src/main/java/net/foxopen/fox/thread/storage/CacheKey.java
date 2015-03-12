package net.foxopen.fox.thread.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.Trackable;


/**
 * A storage location or MapSet cache key definition. This object only represents a <i>definition</i> of a cache key for
 * evaluation via the {@link #evaluate} method, which may add a prefix to increase the specificity of the result.
 */
public class CacheKey
implements Trackable {

  private final String mCacheKeyString;
  private final List<StorageLocationBind> mUsingBinds;

  /**
   * Constructs a new CacheKey from a DOM containing the relevant markup.
   * @param pCacheKeyDefinition DOM containing markup.
   * @param pCacheKeyStringRequired If true, validates that a cache key string is a required part of the markup. One can
   * be implicitly generated if this is false. If this is true and no cache key string is defined, an exception is raised.
   * @return New CacheKey definition.
   * @throws ExModule If markup is invalid.
   */
  public static CacheKey createFromDefinitionDOM(DOM pCacheKeyDefinition, boolean pCacheKeyStringRequired)
  throws ExModule {

    String lCacheKeyString = pCacheKeyDefinition.getAttr("string");
    String lCacheKeyType = pCacheKeyDefinition.getAttr("type");
    List<StorageLocationBind> lUsingBinds;

    if("unique".equals(lCacheKeyType)) {
      //Shortcut definition
      if(!XFUtil.isNull(lCacheKeyString)) {
        throw new ExModule("Cannot specify a string for a unique cache key");
      }
      else if(pCacheKeyDefinition.getUL("fm:using").size() > 0) {
        throw new ExModule("Cannot specify using clauses for a unique cache key");
      }

      lUsingBinds = Collections.singletonList(new StorageLocationBind(0, UsingType.UNIQUE, null));
    }
    else {
      //Full cache key definition specified - parse the nested list of using types
      lUsingBinds = StorageLocationBind.createListFromDOMDefinition(pCacheKeyDefinition);

      //Check the cache key is only using string bind types
      for(StorageLocationBind lCacheKeyBind : lUsingBinds) {
        if(!lCacheKeyBind.getUsingType().isAllowedInCacheKey()) {
          throw new ExModule("Cache-key binds can only be of using-type XPATH, STATIC or UNIQUE (was " + lCacheKeyBind.getUsingType() + ")");
        }
      }

      //Validate that a string was provided if it is required by the consumer
      if(lCacheKeyString.length()==0 && pCacheKeyStringRequired) {
        throw new ExModule("Cache-key string cannot be null");
      }
    }

    return new CacheKey(lCacheKeyString, lUsingBinds);
  }

  /**
   * Creates a default cache key composed entirely of XPATH type binds. Each string in the given list is treated as a seperate
   * XPATH bind.
   * @param pXPathList List of XPaths to create cache key from.
   * @return New default cache key.
   */
  public static CacheKey createFromXPathList(List<String> pXPathList) {

    List<StorageLocationBind> lUsingBinds = new ArrayList<>(pXPathList.size());
    int lIndex = 0;
    for(String lXPath : pXPathList) {
      lUsingBinds.add(new StorageLocationBind(lIndex++, UsingType.XPATH, lXPath));
    }

    //Pass a null cache key so a default is created
    return new CacheKey(null, lUsingBinds);
  }

  public static CacheKey createFromBindList(List<StorageLocationBind> pUsingBinds) {
    return new CacheKey(null, pUsingBinds);
  }

  /**
   * Creates a static cache key. Uniqueness should be provided by the prefix during evaluation.
   * @return
   */
  public static CacheKey createStaticCacheKey() {
    return new CacheKey("", Collections.<StorageLocationBind>emptyList());
  }

  /**
   * Constructs a new CacheKey. If the CacheKeyString is null one is generated based on the provided bind list.
   * @param pCacheKeyString String possibly containing bind variable placeholders. Can be null.
   * @param pUsingBinds Bind definitions for the CacheKey.
   */
  private CacheKey(String pCacheKeyString, List<StorageLocationBind> pUsingBinds) {
    String lCacheKeyString;
    if(XFUtil.isNull(pCacheKeyString)) {
      //Automatically generate a cache key string composed of bind variable placeholders
      StringBuilder lCacheKeyBuilder = new StringBuilder("IMPL_CACHEKEY");
      for(int i=1; i <= pUsingBinds.size(); i++) {
        lCacheKeyBuilder.append(" :" + i);
      }
      lCacheKeyString = lCacheKeyBuilder.toString();
    }
    else {
      lCacheKeyString = pCacheKeyString;
    }

    mCacheKeyString = lCacheKeyString;
    mUsingBinds = pUsingBinds;
  }

  /**
   * Evaluates this cache key by evaluating each bind variable in the definition string and appending the given prefix.
   * Any XPath bind variables are executed using the given ContextUElem. The unique constant is used to evaluate any UNIQUE
   * bind variables.
   * @param pPrefix A prefix determined by the consumer which should guarantee that the evaluated CacheKey will have the
   * correct level of uniqueness.
   * @param pContextUElem Context for XPath evaluation.
   * @param pUniqueConstant Unique value for bind evaluation.
   * @return The evaluated CacheKey string.
   */
  public String evaluate(String pPrefix, ContextUElem pContextUElem, String pUniqueConstant) {

    //Evaluate the cache key bind variables
    Map<StorageLocationBind, String> lEvaluatedCacheKeyBinds = StorageLocationBind.evaluateStringBinds(mUsingBinds, pContextUElem, pUniqueConstant);

    StringBuilder lCacheKeyBuffer = new StringBuilder(mCacheKeyString);
    //Get the StorageLocationBind object for each bind position, then use it for a map lookup in the evaluated string map
    //Loop in reverse order so ":11" is replaced before ":1" (for example)
    for(int i = mUsingBinds.size()-1; i >= 0; i--) {
      StorageLocationBind lBind = mUsingBinds.get(i);
      //Replace the ":1" string (for example) with the evaluated value
      if (XFUtil.replace((":"+(i+1)), lEvaluatedCacheKeyBinds.get(lBind), lCacheKeyBuffer) < 1) {
        throw new ExInternal("Bind variable " + i + " not used in " + mCacheKeyString);
      }
    }

    return pPrefix + "/" + lCacheKeyBuffer.toString();
  }

  public List<StorageLocationBind> getUsingBinds() {
    return mUsingBinds;
  }

  @Override
  public void writeTrackData() {
    Track.debug("CacheKeyString", mCacheKeyString);
    for(StorageLocationBind lBind : mUsingBinds) {
      Track.debug("Bind", lBind.getUsingType() + " - " + lBind.getUsingString());
    }
  }
}

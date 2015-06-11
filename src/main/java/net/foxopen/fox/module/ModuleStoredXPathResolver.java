package net.foxopen.fox.module;

import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.xpath.saxon.StoredXPathResolver;
import net.foxopen.fox.dom.xpath.saxon.StoredXPathTranslator;
import net.foxopen.fox.ex.ExModule;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class ModuleStoredXPathResolver
implements StoredXPathResolver {

  /** Map of XPath names to XPath strings. */
  private final Map<String, String> mNameToXPathMap;

  /** Resolver to delegate to if this resolver cannot resolve an XPath name. */
  private final StoredXPathResolver mParentResolver;

  /**
   * Constructs a new StoredXPathResolver from an XML definition. The DOMList should contain fm:xpath elements from a module
   * definition. Nested references are resolved and inlined into the referencing definition, so recursive evaluation is not
   * required at runtime. This signature should be used for top level resolvers which do not require a parent resolver.
   *
   * @param pXPathDefinitionList List of fm:xpath definition elements.
   * @return New StoredXPathResolver.
   * @throws ExModule If the XPath definitions are invalid (including unresolvable XPath names, reference cycles, etc).
   */
  static StoredXPathResolver createFromDOMList(DOMList pXPathDefinitionList)
  throws ExModule {
    //Construct with a dummy parent resolver which always returns null
    return createFromDOMList(pXPathDefinitionList, pXPathName -> null);
  }

  /**
   * Constructs a new StoredXPathResolver from an XML definition. The DOMList should contain fm:xpath elements from a module
   * definition. Nested references are resolved and inlined into the referencing definition, so recursive evaluation is not
   * required at runtime. This signature should be used for resolvers which may need to delegate to a parent in order to resolve some
   * XPath references.
   *
   * @param pXPathDefinitionList List of fm:xpath definition elements.
   * @param pParentResolver Parent StoredXPathResolver to delegate to if nested references cannot be resolved from the given
   *                        fm:xpath definitions.
   * @return New StoredXPathResolver which will delegate to the given parent if it cannot resolve an XPath.
   * @throws ExModule If the XPath definitions are invalid (including unresolvable XPath names, reference cycles, etc).
   */
  static StoredXPathResolver createFromDOMList(DOMList pXPathDefinitionList, StoredXPathResolver pParentResolver)
  throws ExModule {

    //Map of XPaths which may contain unresolved nested references
    Map<String, String> lPendingTranslation = new HashMap<>(pXPathDefinitionList.size());

    for(DOM lXPathDefn : pXPathDefinitionList) {
      String lName = lXPathDefn.getAttr("name");
      String lValue = lXPathDefn.getAttr("value");

      if(XFUtil.isNull(lName) || XFUtil.isNull(lValue)) {
        throw new ExModule("'name' and 'value' attributes must both be defined for an fm:xpath definition");
      }
      else if(!StoredXPathTranslator.instance().validateXPathName(lName)) {
        throw new ExModule("Not a valid XPath name: '" + lName + "'");
      }
      else if(lPendingTranslation.containsKey(lName)) {
        throw new ExModule("Duplicate XPath definition encountered for name '" + lName + "'");
      }

      lPendingTranslation.put(lName, lValue);
    }

    Map<String, String> lDone = new HashMap<>(pXPathDefinitionList.size());

    //Resolve nested references and place the resultant XPath strings in the "done" map
    for(String lXPathName : lPendingTranslation.keySet()) {
      try {
        translateNestedReferences(lXPathName, lPendingTranslation, lDone, new ArrayDeque<>(3), pParentResolver);
      }
      catch (ExModule e) {
        throw new ExModule("Failed to parse XPath definition for XPath '" + lXPathName + "'", e);
      }
    }

    return new ModuleStoredXPathResolver(Collections.unmodifiableMap(lDone), pParentResolver);
  }

  /**
   * Method for recursively resolving nested XPath references and "flattening" a nested reference structure. I.e. given
   * the following definitions:
   * <pre>
   *   x1 = /X1/${x2}
   *   x2 = X2/${x3}
   *   x3 = X3</pre>
   * The output will be:
   * <pre>
   *   x1 = /X1/X2/X3
   *   x2 = X2/X3
   *   x3 = X3</pre>
   *
   * @param pXPathName Name of XPath in the pending map to perform translation on.
   * @param pPending Map of untranslated XPath names to XPath values.
   * @param pDone Map to populate with completely resolved XPaths.
   * @param pCycleDetector Stack to use to detect cycles within the reference structure.
   * @param pParentResolver Resolver to delegate to if the pending map does not contain a reference.
   * @throws ExModule
   */
  private static void translateNestedReferences(String pXPathName, Map<String, String> pPending, Map<String, String> pDone, Deque<String> pCycleDetector, StoredXPathResolver pParentResolver)
  throws ExModule {

    //Push the current XPath name to the cycle detector stack
    pCycleDetector.addFirst(pXPathName);

    if(!pDone.containsKey(pXPathName)) {
      //Attempt to resolve the XPath value from either the pending map or the parent resolver
      String lXPathValue = new ModuleStoredXPathResolver(pPending, pParentResolver).resolveXPath(pXPathName);
      if(lXPathValue == null) {
        throw new ExModule("No definition found for XPath with name '" + pXPathName + "'");
      }

      //Recursively translate any nested references within the current XPath so it can be
      for(String lNestedReference : StoredXPathTranslator.instance().allReferencesInPath(lXPathValue)) {
        //If we're already processing an XPath which is referenced within a nested path, this implies a dodgy definition cycle such as x1 -> x2 -> x1
        if(pCycleDetector.contains(lNestedReference)) {
          throw new ExModule("Infinite cycle detected in XPath reference chain: " + Joiner.on(", ").join(pCycleDetector.descendingIterator()));
        }

        translateNestedReferences(lNestedReference, pPending, pDone, pCycleDetector, pParentResolver);
      }

      //Convert current path using the results - use a temporary StoredXPathResolver based on the XPaths which are currently done
      String lReplaceResult = StoredXPathTranslator.instance().translateXPathReferences(new ModuleStoredXPathResolver(pDone, pParentResolver), lXPathValue);
      pDone.put(pXPathName, lReplaceResult);
    }

    //Pop current XPath from cycle detector stack
    pCycleDetector.removeFirst();
  }

  private ModuleStoredXPathResolver(Map<String, String> pNameToXPathMap, StoredXPathResolver pParentResolver) {
    mNameToXPathMap = pNameToXPathMap;
    mParentResolver = pParentResolver;
  }

  @Override
  public String resolveXPath(String pXPathName) {
    String lXPath = mNameToXPathMap.get(pXPathName);
    if(lXPath == null) {
      return mParentResolver.resolveXPath(pXPathName);
    }
    else {
      return lXPath;
    }
  }
}

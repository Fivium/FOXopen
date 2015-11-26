package net.foxopen.fox.dom.xpath;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XPathWrapper;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.saxon.DynamicNamespaceContext;
import net.foxopen.fox.dom.xpath.saxon.StoredXPathTranslator;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;


/**
 * A FoxXPathEvaluator is used to determine how an XPath should be evaluated, execute it using the correct XPath engine,
 * and return the result. It is also responsible for any caching which takes place.
 * This version provides the default behaviour for FOX, but can be overloaded if required and delivered by the
 * {@link FoxXPathEvaluatorFactory}.<br><br>
 *
 * In this class, simple paths are evaluated using the FOX internal evaluator and
 * complex XPaths are delegated to Saxon. Compiled XPaths are cached for reuse.<br><br>
 *
 * This object is thread-safe and typically only one instantiation will be required.
 */
public class FoxXPathEvaluator {

  /**
   * Should XPaths be executed using XPath 1.0 compatibility mode?
   */
  private final boolean mUseXPathBackwardsCompatibility;

  static {
    EngineStatus.instance().registerStatusProvider(new XPathStatusProvider());
  }

  /**
   * Create a new FoxXPathEvaluator.
   * @param pUseXPathBackwardsCompatibility If true, the new evaluator will use XPath 1.0 compatibility mode.
   */
  FoxXPathEvaluator(boolean pUseXPathBackwardsCompatibility){
    mUseXPathBackwardsCompatibility = pUseXPathBackwardsCompatibility;
  }

  /**
   * Evaluates pXPath and returns the result of the evaluation. No :{context}s are supported in the XPath.
   * @param pXPath The XPath string to be evaluated.
   * @param pContextNode The initial context item of the XPath evaluation.
   * @return The result of the XPath evaluation.
   * @throws ExBadPath If the XPath syntax is invalid.
   */
  public XPathResult evaluate(String pXPath, DOM pContextNode)
  throws ExBadPath {
    return evaluate(pXPath, pContextNode, null, FoxXPathResultType.DOM_LIST);
  }

   /**
    * Evaluates pXPath and returns the result of the evaluation, using the default FoxXPathResultType (DOM List).
    * @param pXPath The XPath string to be evaluated.
    * @param pContextNode The initial context item of the XPath evaluation.
    * @param pContextUElem Optional ContextUElem for resolving :{context} references.
    * @return The result of the XPath evaluation.
    * @throws ExBadPath If the XPath syntax is invalid.
    * @see FoxXPathEvaluator#evaluate(String, DOM, ContextUElem, FoxXPathResultType)
    */
  public XPathResult evaluate(String pXPath, DOM pContextNode, ContextUElem pContextUElem)
  throws ExBadPath {
    return evaluate(pXPath, pContextNode, pContextUElem, FoxXPathResultType.DOM_LIST);
  }

  /**
   * Evaluates pXPath and returns the result of the evaluation. Internally, this may use Saxon or Fox's own simple
   * path evaluator, but this should be transparent to consumers.
   * @param pXPath The XPath string to be evaluated.
   * @param pContextNode The initial context item of the XPath evaluation.
   * @param pContextUElem Optional ContextUElem for resolving :{context} references.
   * @param pResultType The expected result type.
   * @return The result of the XPath evaluation.
   * @throws ExBadPath If the XPath syntax is invalid.
   */
  public XPathResult evaluate(String pXPath, DOM pContextNode, ContextUElem pContextUElem, FoxXPathResultType pResultType)
  throws ExBadPath {
    return evaluate(pXPath, pContextNode, pContextUElem, pResultType, XPathWrapper.NO_WRAPPER);
  }


  /**
   * Gets a FoxPath from the cache, or compiles one if it is not cached.
   * @param pPathString The Path String.
   * @param pOptionalContextNode The Context node of the path expression. Used for determining if namespace aware processing
   * is required. Can be null.
   * @return The compiled or cached FoxPath.
   * @throws ExBadPath If the path String is invalid.
   */
  public FoxPath getOrCompilePath(String pPathString, DOM pOptionalContextNode)
  throws ExBadPath {

    if(pPathString == null){
      throw new ExInternal("pPathString cannot be null.");
    }

    pPathString = pPathString.trim();

    if(pPathString.length() == 0){
      throw new ExBadPath("pPathString cannot be empty or all whitespace.");
    }

    //TODO benchmark performance of caching Simple Paths, or looking up in cache before doing constant/simple path check
    //(bear in mind this will skew cache miss ratios)
    FoxPath lPath;

    //First, see if this is a constant value (i.e. a number) - this method will return null if not
    lPath = FoxConstantPath.getFoxConstantPathOrNull(pPathString);
    if(lPath != null){
      return lPath;
    }

    XPathDefinition lXPathDefinition;

    //Do a quick check for stored XPath references which should be translated if any are found
    if(StoredXPathTranslator.instance().containsStoredXPathReference(pPathString)) {
      //Replace original XPathDefinition with translation result
      lXPathDefinition = StoredXPathTranslator.instance().translateXPathReferences(pPathString);
    }
    else {
      lXPathDefinition = XPathDefinition.forUnmodifiedXPath(pPathString);
    }

    //Now, try this as a simple path - this method will return null if it's not simple
    lPath = FoxSimplePath.getFoxSimplePathOrNull(lXPathDefinition);
    if(lPath != null){
      return lPath;
    }

    //Finally, must be a full XPath - look in the cache first
    //Note: XPathDefinition is used as a cache key to avoid clashes in the case that different combinations of template variables result in the same XPath
    lPath = (FoxPath) CacheManager.getCache(BuiltInCacheDefinition.FOX_XPATH_EVALUATORS).get(lXPathDefinition);
    if (lPath == null){
      //Not a constant or simple path so invoke full Saxon XPath processing.
      //If the Context Document is namespace aware, we need to use a special XPath constructor to resolve the namespaces
      if(pOptionalContextNode != null && pOptionalContextNode.getDocControl().isNamespaceAware()){
        lPath = new FoxXPath(lXPathDefinition, mUseXPathBackwardsCompatibility, new DynamicNamespaceContext(pOptionalContextNode.getRootElement()));
      }
      else {
        //The majority of XPaths will use the standard namespace resolver
        lPath = new FoxXPath(lXPathDefinition, mUseXPathBackwardsCompatibility, null);
        //Cache the compiled XPath for future executions (only cache non-namespace aware XPaths to avoid potential namespace conflicts)
        CacheManager.getCache(BuiltInCacheDefinition.FOX_XPATH_EVALUATORS).put(lXPathDefinition, lPath);
      }
    }

    return lPath;
  }

  /**
   * Evaluates pXPath and returns the result of the evaluation. Internally, this may use Saxon or Fox's own simple
   * path evaluator, but this should be transparent to consumers.
   * @param pXPath The XPath string to be evaluated.
   * @param pContextNode The initial context item of the XPath evaluation.
   * @param pContextUElem Optional ContextUElem for resolving :{context} references.
   * @param pResultType The expected result type.
   * @param pXPathWrapper The wrapping function which causes this XPath to be evaluated (i.e. string()). XPathWrapper.NO_WRAPPER
   * should be used to indicate that no wrapping function was used.
   * @return The result of the XPath evaluation.
   * @throws ExBadPath If the XPath syntax is invalid.
   */
  public XPathResult evaluate(String pXPath, DOM pContextNode, ContextUElem pContextUElem, FoxXPathResultType pResultType, XPathWrapper pXPathWrapper)
  throws ExBadPath {

    //Some validation
    if(pContextNode == null){
      throw new ExInternal("pContextNode cannot be null.");
    }

    //Shortcut self-expression to save time
    if(".".equals(pXPath)){
      return new XPathResult(pContextNode, FoxSimplePath.getFoxSimplePathOrNull(XPathDefinition.forUnmodifiedXPath(".")));
    }

    FoxPath lXPath = getOrCompilePath(pXPath, pContextNode);

    //Execute and return result wrapper
    return lXPath.execute(pContextNode, pContextUElem, pResultType, pXPathWrapper);
  }
}

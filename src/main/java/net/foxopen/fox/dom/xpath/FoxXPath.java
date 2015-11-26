package net.foxopen.fox.dom.xpath;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XPathWrapper;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DocControl;
import net.foxopen.fox.dom.xpath.saxon.DynamicNamespaceContext;
import net.foxopen.fox.dom.xpath.saxon.SaxonEnvironment;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExPathInternal;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.xpath.XPathEvaluator;
import net.sf.saxon.xpath.XPathExpressionImpl;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Wrapper for a compiled Saxon XPath. This class is used to compile a complex XPath which may contain FOX-specific
 * markup and function calls. The XPath may then be executed multiple times in multiple threads.
 */
public class FoxXPath
implements FoxPath {

  /**
   * The 'external' (i.e. FOX developer) representation of the XPath, i.e. :{theme}/ELEMENT_NAME[@X = :{root}/ELEMENT_2]
   * This may have originally contained stored XPath markup so the XPathDefinition is used to report the original path to the user
   */
  private final XPathDefinition mXPathDefinition;

  /**
   * The internally rewritten XPath to be executed by Saxon.
   * I.e. fox:ctxt('root')/ELEMENT_NAME[@X = fox:ctxt('root')/ELEMENT_2]
   */
  private final String mInternalXPathString;

  /**
   * The compiled JAXP/SAXON XPath expression. Saxon says this is Thread safe as long as certain methods are avoided.
   * See http://www.saxonica.com/documentation/javadoc/net/sf/saxon/xpath/XPathExpressionImpl.html
   */
  private final XPathExpression mXPathExpression;

  /**
   * The number of times this FoxXPath has been executed.
   */
  private int mUsageCount = 0;

  /**
   * The time in ms taken to compile this XPath.
   */
  private long mCompileTime;

  /**
   * Overall time in MS spent executing this XPath.
   */
  private long mCumulativeExecTimeMS = 0;

  /**
   * Set of the :{context} labels implicated by this XPath.
   * Note the linked set maintains insertion order, so iterating over this set will retrieve the labels in the order
   * they appear in the XPath.
   * Null if non exist.
   */
  private LinkedHashSet<String> mLabelSet = null;

  /**
   * Records whether this XPath is dependent on the context item or the context item's document.
   */
  private final boolean mUsesContextItem;
  private final boolean mUsesContextDocument;

  /**
   * Construct a new FoxXPath for the given XPath string.
   * @param pXPathDefinition XPath definition containing a FOX-compliant executable XPath string, possibly containing :{contexts} and/or custom FOX functions.
   * @param pUseXPathBackwardsCompatibility If true, switches XPath 1.0 backwards compatibility mode on.
   * @param pNamespaceMap Optional map for namespace-aware XPath processing.
   * @throws ExBadPath If the XPath cannot be compiled for any reason.
   */
  FoxXPath(XPathDefinition pXPathDefinition, boolean pUseXPathBackwardsCompatibility, DynamicNamespaceContext pNamespaceMap)
  throws ExBadPath {

    long lStartTime = System.currentTimeMillis();
    mXPathDefinition = pXPathDefinition;
    mLabelSet = new LinkedHashSet<>();
    mInternalXPathString = SaxonEnvironment.replaceFoxMarkup(pXPathDefinition.getExecutableXPath(), mLabelSet);

    //If there are no labels, clear away the set object as it's no longer needed
    if(mLabelSet.size() == 0){
      mLabelSet = null;
    }

    XPathEvaluator lXPathEvaluator = SaxonEnvironment.getXPathEvaluator(pUseXPathBackwardsCompatibility, pNamespaceMap);

    try {
      //Compile the XPath and store the result of the compilation.
      mXPathExpression = lXPathEvaluator.compile(mInternalXPathString);
    }
    catch (XPathExpressionException e) {
      throw new ExBadPath("Bad XPath for original extended XPath: '" + mXPathDefinition.getPathForDebug() +  "'. " +
        (mXPathDefinition.getExecutableXPath().equals(mInternalXPathString) ? "" : "\nNote: FOX markup in the XPath was rewritten to: '" + mInternalXPathString + "'"), e);
    }

    //Establish dependencies - Saxon gives us a bitmap which we AND against the relevant constants
    int lDeps = ((XPathExpressionImpl) mXPathExpression).getInternalExpression().getDependencies();
    mUsesContextItem = (StaticProperty.DEPENDS_ON_CONTEXT_ITEM & lDeps) > 0;
    mUsesContextDocument = (StaticProperty.DEPENDS_ON_CONTEXT_DOCUMENT & lDeps) > 0;

    //Store the compile time
    mCompileTime = System.currentTimeMillis() - lStartTime;
  }

  /**
   * Executes this XPath statement using Saxon's evaluator.
   * @param pContextNode The context node to use for the evaluation.
   * @param pContextUElem Optional ContextUElem. This must be provided if the expression uses :{contexts}.
   * @param pResultType The expected result type of the XPath statement.
   * @return An XPathResult which wraps the raw result object.
   */
  public XPathResult execute(DOM pContextNode, ContextUElem pContextUElem, FoxXPathResultType pResultType, XPathWrapper pXPathWrapper){

    //Validate that the node is attached to a document
    if(!pContextNode.isAttached()){
      throw new ExInternal("Cannot perform XPath evaluation on unattached node " + pContextNode.absolute());
    }

    mUsageCount++; //not thread safe but not important enough to incur the overhead
    long lStartTimeMS = System.currentTimeMillis();

    try {
      if(pContextUElem != null){
        SaxonEnvironment.setThreadLocalContextUElem(pContextUElem);
      }
      //Evaluate the compiled XPath expression using Saxon, then wrap the resulting object.
      Object lEvalResult = mXPathExpression.evaluate(pContextNode.wrap(), pResultType.asQName());
      return new XPathResult(lEvalResult, this, pContextUElem, pContextNode, pXPathWrapper);

    }
    catch (Throwable th) {
      throw new ExPathInternal("Error evaluating extended XPath: '" + mXPathDefinition.getPathForDebug() +  "'. " +
       (mXPathDefinition.getExecutableXPath().equals(mInternalXPathString) ? "" : "\nNote: FOX markup in the XPath was rewritten to: '" + mInternalXPathString + "'"), th);
    }
    finally {
      if(pContextUElem != null){
        SaxonEnvironment.clearThreadLocalContextUElem();
      }
      mCumulativeExecTimeMS += System.currentTimeMillis() - lStartTimeMS; //not thread safe but not important enough to incur the overhead
    }
  }

  /**
   * Returns the original XPath string used for this XPath expression.
   */
  public String getOriginalPath() {
    return mXPathDefinition.getPathForDebug();
  }

  /**
   * Returns the rewritten, executable XPath String.
   */
  public String getProcessedPath() {
    return mInternalXPathString;
  }

  /**
   * Returns the number of times this FoxXPath has been evaluated.
   */
  public int getUsageCount() {
    return mUsageCount;
  }

  /**
   * Returns true if this XPath contains a reference to the context item, i.e. "./X/Y" or "X/Y",
   * or the context document, i.e. "//X" or "./X[Y = //Z]".
   */
  public boolean usesContextItemOrDocument() {
    return mUsesContextItem || mUsesContextDocument;
  }

  /**
   * Returns true if this XPath contains a reference to the context item, i.e. "./X/Y" or "X/Y".
   * I.e. if is this a relative XPath.
   */
  public boolean usesContextItem() {
    return mUsesContextItem;
  }

  /**
   * Returns true if this XPath contains a reference to the context document, i.e. "/X/Y".
   */
  public boolean usesContextDocument() {
    return mUsesContextDocument;
  }

  /**
   * Returns a Set of all the :{context} labels used in this XPath expression. The Linked Set is ordered by the order
   * of the label's appearance in the XPath.
   * @return Set of label Strings, or null if none are defined on this FoxXPath.
   */
  public LinkedHashSet<String> getLabelSet() {
    if(mLabelSet != null){
      return new LinkedHashSet<String>(mLabelSet);
    }
    else {
      return null;
    }
  }

  /**
   * Gets a Set of all documents implicated during the execution of this XPath for the given ContextUElem.
   * Considers :{contexts} and the use of the context node (i.e. "./X" or "/X/Y" style expressions).
   * @param pContextUElem The ContextUElem used to evaluate the XPath.
   * @param pContextNode The Context Node of the XPath.
   * @return A Set containing all implicated DocControls, or an empty Set if no documents were implicated. This is possible
   * in the case of XPaths which do not reference a node, like "concat('x','y')", although such usage is probably incorrect.
   */
  public Set<DocControl> getImplicatedDocumentSet(ContextUElem pContextUElem, DOM pContextNode){
    //Start off getting the documents for all the labels in the XPath
    Set<DocControl> lDocControlSet = new HashSet<DocControl>();
    if(mLabelSet != null){
      if(pContextUElem != null){
        lDocControlSet.addAll(pContextUElem.labelListToDocControlSet(new ArrayList<String>(mLabelSet)));
      }
      else {
        throw new ExInternal("Context labels are referenced in path but no ContextUElem supplied to getImplicatedDocumentSet");
      }
    }

    //If the context node or context document is used, add that DocControl too
    if(usesContextItemOrDocument()){
      if(pContextNode != null){
        lDocControlSet.add(pContextNode.getDocControl());
      }
      else {
        throw new ExInternal("Context node is referenced in path but not supplied to getImplicatedDocumentSet");
      }
    }
    return lDocControlSet;
  }

  /**
   * Returns the time in milliseconds taken to compile this XPath. Useful for debug purposes.
   */
  public long getCompileTime(){
    return mCompileTime;
  }

  /**
   * Returns the cumulative time in milliseconds spent executing this XPath.
   */
  public long getCumulativeExecTimeMS(){
    return mCumulativeExecTimeMS;
  }

  /**
   * {@inheritDoc}
   */
  public ContextualityLevel getContextualityLevel(ContextUElem pContextUElem, ContextualityLevel pContextNodeContextualityLevel) {

    //Search for contextuality starting from the most contextual facets through to the least contextual.

    //If this XPath uses the context item, and it's got ITEM contextuality, we can save some time
    if(mUsesContextItem && (pContextNodeContextualityLevel == null || pContextNodeContextualityLevel == ContextualityLevel.ITEM)){
      return ContextualityLevel.ITEM;
    }

    //Belt and braces check for XPath 2.0 functions which will make the path APPEAR constant but are actually dynamic.
    //Sadly there's no nice way to interrogate a Saxon XPath for function calls so string searching will have to do.
    //Developers should NOT be using these functions so this is a last resort check.
    if(mInternalXPathString.indexOf("current-date()") != -1 || mInternalXPathString.indexOf("current-dateTime()") != -1 ||
       mInternalXPathString.indexOf("current-time()") != -1) {
      return ContextualityLevel.ITEM;
    }

    //The context node's contextuality should not be overridden by a label reference if it is higher than a label's
    ContextualityLevel lMaxContextualityLevel = mUsesContextItem ? pContextNodeContextualityLevel : ContextualityLevel.CONSTANT;
    //Loop through context labels and establish the one with the highest (i.e. most contextual) level
    if(mLabelSet != null){
      for(String lLabel : mLabelSet){
        ContextualityLevel lLevel = pContextUElem.getLabelContextualityLevel(lLabel);
        if(lLevel.asInt() > lMaxContextualityLevel.asInt()){
          lMaxContextualityLevel = lLevel;
        }
      }
      return lMaxContextualityLevel;
    }

    //No labels in the path, but an overridden contextuality level was provided for the context node - so use that
    if(mUsesContextItem){
      return pContextNodeContextualityLevel;
    }

    //If we don't have any labels but do use the context document, we know this is document level
    if(mUsesContextDocument){
      return ContextualityLevel.DOCUMENT;
    }

    //We don't use the context item, any context labels, or the context document. This must be a "static" XPath
    //such as (1 to 5)[. mod 2 = 1]
    return ContextualityLevel.CONSTANT;
  }
}

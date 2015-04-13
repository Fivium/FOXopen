/*

Copyright (c) 2012, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
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
package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.CollationURIResolver;
import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.xpath.XPathEvaluator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class holds static references to Saxon configration objects and methods.
 * These are thread-safe and reusable so they are encapsulated here for convenience.
 */
public class SaxonEnvironment {

  public static final String XML_SCHEMA_NS_PREFIX = "xs";
  public static final String FOX_NS_PREFIX = "fox";
  public static final String FOX_NS_URI = "http://foxopen.net/";

  private static final String SQ_OPEN = "##__SQ_OPEN__##";
  private static final String SQ_CLOSE = "##__SQ_CLOSE__##";

  /**
   * Regex support for converting exists-context function call into a Saxon-compatible function call.
   */
  private static final Pattern EXISTS_CONTEXT_MATCH_PATTERN = Pattern.compile("(fox:)?exists-context\\(:\\{(.*?)\\}\\)");
  private static final String EXISTS_CONTEXT_REPLACE_STRING = "fox:exists-context\\('$2'\\)";

  /**
   * Regex support for converting :{contexts} into a Saxon-compatible function call.
   */
  private static final Pattern FOX_CONTEXT_MATCH_PATTERN = Pattern.compile(":\\{(.*?)\\}");
  private static final String FOX_CONTEXT_REPLACE_STRING = "fox:ctxt\\('$1'\\)";

  /**
   * The universal Saxon configuration. All FoxXPaths are compiled against this configuration.
   */
  private static final Configuration gSaxonConfiguration;

  /**
   * The universal Saxon processor. This should reference the universal configuration. All Saxon s9api calls
   * should be run using this processor.
   */
  private static final Processor gSaxonProcessor;

  private static final XQueryCompiler gXQueryCompiler;

  private static final XsltCompiler gXsltCompiler;

  /**
   * Namespace context passed to Saxon so function prefixes (i.e. "fox:ctxt") can be resolved.
   */
  private static final NamespaceContext gFoxNamespaceContext = new DefaultNamespaceContext();

  /**
   * ThreadLocal reference to a ContextUElem.
   * This is used by the FoxContext (:{x}) function to resolve a context name to a node.
   */
  private static final ThreadLocal<WeakReference<ContextUElem>> gThreadLocalContextUElem;

  private static final ThreadLocal<WeakReference<ActionRequestContext>> gThreadLocalRequestContext;

  /**
   * This class cannot be instantiated.
   */
  private SaxonEnvironment() {
  }

  /**
   * Setup for the XPathFactory - registers function resolvers, namespace resolvers etc.
   * The static block is thread safe.
   */
  static {
    //Create a global configuration and a global processor.
    gSaxonConfiguration = new Configuration();

    gSaxonConfiguration.setURIResolver(new URIResolver() {
      @Override
      public Source resolve(String href, String base)
      throws TransformerException {
        throw new ExInternal("External URIs are not permitted in FOX XPaths");
      }
    });

    gSaxonConfiguration.setCollectionURIResolver(new CollectionURIResolver() {
      @Override
      public SequenceIterator resolve(String href, String base, XPathContext context)
      throws XPathException {
        throw new ExInternal("External URIs are not permitted in FOX XPaths");
      }
    });

    gSaxonConfiguration.setCollationURIResolver(new CollationURIResolver() {
      @Override
      public StringCollator resolve(String collationURI, Configuration config)
      throws XPathException {
        throw new ExInternal("External URIs are not permitted in FOX XPaths");
      }
    });

    gSaxonProcessor = new Processor(gSaxonConfiguration);

    //Register extension functions.
    //Note that these are actually registered on the Global Saxon Configuration so will be available to all XPaths,
    //XQueries and XSLTs created from the global configuration.
    gSaxonProcessor.registerExtensionFunction(new FoxContextFunction());
    gSaxonProcessor.registerExtensionFunction(new ExistsContextFunction());
    gSaxonProcessor.registerExtensionFunction(new NvlFunction());
    gSaxonProcessor.registerExtensionFunction(new Nvl2Function());
    gSaxonProcessor.registerExtensionFunction(new MapSetKeyFunction());
    gSaxonProcessor.registerExtensionFunction(new MapSetFunction());
    gSaxonProcessor.registerExtensionFunction(new PagerStatusFunction());
    gSaxonProcessor.registerExtensionFunction(new UploadTypeInfoFunction());
    gSaxonProcessor.registerExtensionFunction(new UserPrivilegeFunction());
    gSaxonProcessor.registerExtensionFunction(new CastDateSafeFunction());

    //Create and set up a global XQuery compiler.
    gXQueryCompiler = gSaxonProcessor.newXQueryCompiler();
    //Declare fox namespace for function evaluation
    gXQueryCompiler.declareNamespace(FOX_NS_PREFIX, FOX_NS_URI);

    //Create a global XSLT compiler.
    gXsltCompiler = gSaxonProcessor.newXsltCompiler();

    //Set up the ThreadLocal object for storing ContextUElems
    gThreadLocalContextUElem = new ThreadLocal<>();

    gThreadLocalRequestContext = new ThreadLocal<>();
  }

  /**
   * Get this thread's current ContextUElem for use in XPath evaluation. Errors if not set.
   *
   * @return The thread's ContextUElem.
   */
  static ContextUElem getThreadLocalContextUElem() {
    WeakReference<ContextUElem> lRef = gThreadLocalContextUElem.get();
    if (lRef != null) {
      ContextUElem lContext = lRef.get();
      if (lContext != null) {
        return lContext;
      }
      else {
        throw new ExInternal("getThreadLocalContextUElem would have returned null due to WeakReference being null.");
      }
    }
    else {
      throw new ExInternal("getThreadLocalContextUElem would have returned null due to ThreadLocal reference not being set. " +
                           "setThreadLocalContextUElem must be called before running any XPaths which use FOX context functions.");
    }
  }

  /**
   * Gets the RequestContext for the request currently active on the underlying Java thread. If it has not been set, an
   * error is raised.
   *
   * @return
   */
  static ActionRequestContext getThreadLocalRequestContext() {
    WeakReference<ActionRequestContext> lRef = gThreadLocalRequestContext.get();
    if (lRef != null) {
      ActionRequestContext lRequestContext = lRef.get();
      if (lRequestContext != null) {
        return lRequestContext;
      }
      else {
        throw new ExInternal("getThreadLocalRequestContext would have returned null due to WeakReference being null.");
      }
    }
    else {
      throw new ExInternal("getThreadLocalRequestContext would have returned null due to ThreadLocal reference not being set. " +
                           "setThreadLocalRequestContext must be called before running any XPaths which use module state based functions.");
    }
  }

  /**
   * @return The global Saxon configuration for this FOX engine.
   */
  public static Configuration getSaxonConfiguration() {
    return gSaxonConfiguration;
  }

  /**
   * @return The global Saxon processor for this FOX engine.
   */
  public static Processor getSaxonProcessor() {
    return gSaxonProcessor;
  }

  /**
   * @return The global XQuery compiler for this FOX engine. This object is thread-safe.
   */
  public static XQueryCompiler getXQueryCompiler() {
    return gXQueryCompiler;
  }

  /**
   * @return The global XSLT compiler for this FOX engine. This object is thread-safe.
   */
  public static XsltCompiler getXsltCompiler() {
    return gXsltCompiler;
  }

  /**
   * Set the ThreadLocal reference to a ContextUElem so context-aware Fox XPaths can resolve :{contexts}. Be sure to call
   * {@link SaxonEnvironment#clearThreadLocalContextUElem} in a finally block after calling this method.
   *
   * @param pContextUElem The contextUElem for this thread.
   */
  public static void setThreadLocalContextUElem(ContextUElem pContextUElem) {
    //Sanity check
    if (gThreadLocalContextUElem.get() != null) {
      //Don't error, this was being called from getMapset within an XPath function which is fine
      Track.debug("ThreadLocalContextUElem", "setThreadLocalContextUElem called when already set");
    }
    else {
      gThreadLocalContextUElem.set(new WeakReference<>(pContextUElem));
    }
  }

  public static void setThreadLocalRequestContext(ActionRequestContext pRequestContext) {
    //Sanity check
    if (gThreadLocalRequestContext.get() != null) {
      throw new ExInternal("Cannot set ThreadLocal RequestContext because it is already set. Ensure calls to this method " +
                           "are properly cleaned up by using clearThreadLocalRequestContext().");
    }
    gThreadLocalRequestContext.set(new WeakReference<>(pRequestContext));
  }

  /**
   * Clean up the ThreadLocal ContextUElem after XPath execution has completed.
   */
  public static void clearThreadLocalContextUElem() {
    gThreadLocalContextUElem.remove();
  }

  public static void clearThreadLocalRequestContext() {
    gThreadLocalRequestContext.remove();
  }

  /**
   * Get a Saxon XPathEvaluator for the purposes of compiling an XPath.
   *
   * @param pBackwardsCompatible If true, turns XPath 1.0 backwards compatibility mode on.
   * @param pNamespaceMap        Optional NamespaceContext for resolution of arbitrary namespaces. Leave null to use default.
   * @return A new single-use XPathEvaluator.
   */
  public static XPathEvaluator getXPathEvaluator(boolean pBackwardsCompatible, DynamicNamespaceContext pNamespaceMap) {

    XPathEvaluator lXPE = new XPathEvaluator(SaxonEnvironment.getSaxonConfiguration());
    lXPE.getStaticContext().setBackwardsCompatibilityMode(pBackwardsCompatible);

    if (pNamespaceMap == null) {
      lXPE.setNamespaceContext(gFoxNamespaceContext);
    }
    else {
      lXPE.setNamespaceContext(pNamespaceMap);
    }
    return lXPE;
  }

  /**
   * Rewrites the given 'external' XPath (i.e. as the developer wrote it) into an internally executable XPath, namely by
   * replacing <i>:{context}</i>s to <i>fox:ctxt('context')</i> function calls. Also populates pLabelSet, if provided,
   * with a list of the context labels in the expression, ordered by the order of their occurence in the original string.
   *
   * @param pExternalXPath A FOX XPath string.
   * @param pLabelSet      An optional set to populate with context labels.
   * @return A Saxon-compliant XPath 2.0 String.
   */
  public static String replaceFoxMarkup(final String pExternalXPath, final LinkedHashSet<String> pLabelSet) {

    //Sanity check that the replacement tokens do not already exist in the XPath
    if (pExternalXPath.indexOf(SQ_OPEN) != -1 || pExternalXPath.indexOf(SQ_CLOSE) != -1) {
      throw new ExInternal("Invalid String sequence found in XPath: " + pExternalXPath);
    }

    StringBuilder lXPathBuilder = new StringBuilder();
    boolean lIsInQuotes = false;

    //Do a basic parse of the string to make sure we don't rewrite :{context} labels within quoted strings
    //I.e. being used in an eval expression - concat('<fm:run-query name="', :{theme}/QRY, '" match=":{action}">')
    for (int i = 0; i < pExternalXPath.length(); i++) {
      char lChar = pExternalXPath.charAt(i);

      //If we enter/leave single quotes, flip the bit
      //NOTE: weakness here is that either " or ' is a valid string delimeter in XPath
      //However the vast majority of Fox XPaths will use ' as they are specified in XML attributes.
      if (lChar == '\'') {
        lIsInQuotes = !lIsInQuotes;
      }

      if (lChar == '{' && lIsInQuotes) {
        lXPathBuilder.append(SQ_OPEN);
      }
      else if (lChar == '}' && lIsInQuotes) {
        lXPathBuilder.append(SQ_CLOSE);
      }
      else {
        lXPathBuilder.append(lChar);
      }
    }

    String lXPath = lXPathBuilder.toString();

    //Replace "exists-context(:{context})" with fox:exists-context('context')
    if (lXPath.indexOf("exists-context") != -1) {
      Matcher m = EXISTS_CONTEXT_MATCH_PATTERN.matcher(lXPath);
      lXPath = m.replaceAll(EXISTS_CONTEXT_REPLACE_STRING);
    }

    //Replace ":{context}" with "fox:ctxt('context')"
    if (lXPath.indexOf(":{") != -1) {
      Matcher m = FOX_CONTEXT_MATCH_PATTERN.matcher(lXPath);
      if (pLabelSet != null) {
        pLabelSet.clear();
        while (m.find()) {
          pLabelSet.add(m.group(1));
        }
      }
      lXPath = m.replaceAll(FOX_CONTEXT_REPLACE_STRING);
    }

    //Replace squiggly brackets which were within quotes back in
    lXPath = lXPath.replaceAll(SQ_OPEN, "{");
    lXPath = lXPath.replaceAll(SQ_CLOSE, "}");

    return lXPath;
  }

  public static FoxXsltErrorListner newFoxXsltErrorListener() {
    return new FoxXsltErrorListner();
  }

  /**
   * Tests if the given data model value is null or equivalent to null (including empty string).
   *
   * @param pXdmValue Value to test.
   * @return True if the value is null.
   */
  static boolean isValueNull(XdmValue pXdmValue) {
    if (pXdmValue == null) {
      return true;
    }
    else if (pXdmValue instanceof XdmEmptySequence) {
      return true;
    }
    else if (pXdmValue instanceof XdmAtomicValue) {
      String lResultString = ((XdmAtomicValue) pXdmValue).getStringValue();
      return XFUtil.isNull(lResultString);
    }
    else {
      return false;
    }
  }

  /**
   * Basic class for tracking XSLT compilation errors.
   */
  public static class FoxXsltErrorListner
  implements ErrorListener {

    ArrayList<Exception> mExceptionList = new ArrayList<Exception>();

    public void warning(TransformerException pException) {
      Track.alert("XsltCompilationWarning", pException.getMessage());
    }

    public void error(TransformerException pException) {
      Track.alert("XsltCompilationError", pException.getMessage());
      mExceptionList.add(pException);
    }

    public void fatalError(TransformerException pException) {
      Track.alert("XsltCompilationFatalError", pException.getMessage());
      mExceptionList.add(pException);
    }

    public void processErrors() {
      if (mExceptionList.size() > 0) {
        throw new ExInternal(mExceptionList.size() + " exception(s) occured during transform. " +
                             "See nested for first; examine Track for all.", mExceptionList.get(0));
      }
    }

  }

}

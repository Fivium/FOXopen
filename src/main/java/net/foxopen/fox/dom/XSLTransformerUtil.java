package net.foxopen.fox.dom;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.xpath.saxon.SaxonEnvironment;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import net.sf.saxon.s9api.SAXDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import java.io.InputStream;


/**
 * Utility methods for performing XSL transformations within FOX.
 * NOTE: This class is an experimental proof-of-concept and requires more testing before it is suitable for production use.
 */
public class XSLTransformerUtil {

  /**
   * Runs an XSLT transformation of the given source DOM using the String provided in pXsltXML as the XSLT document.
   * The output is written to pDestinationDOM. Any :{context}s in the XSLT are resolved using pContextUElem.
   * @param pSourceDOM The root DOM to be used in the transformation. Additional DOMs can be referenced by :{context}
   * references.
   * @param pAttachPoint The attach point to be used as the initial context item (".") of the transformation.
   * @param pXsltXML String representation of the XSLT.
   * @param pDestinationDOM The destination for the transformation.
   * @param pContextUElem For resolving :{context}s.
   */
  public static void transformToDOM(DOM pSourceDOM, DOM pAttachPoint, String pXsltXML, DOM pDestinationDOM,
                                    ContextUElem pContextUElem){
    pXsltXML = SaxonEnvironment.replaceFoxMarkup(pXsltXML, null);
    transformToDOM(pSourceDOM, pAttachPoint, DOM.createDocumentFromXMLString(pXsltXML), pDestinationDOM, pContextUElem);
  }

  /**
   * Runs an XSLT transformation of the given source DOM using the InputStream provided to resolve the XSLT document.
   * The output is written to pDestinationDOM. Using :{context} syntax is not currently supported by this flavour -
   * the XSLT document must reference the fox:ctxt() function instead.
   * @param pSourceDOM The root DOM to be used in the transformation. Additional DOMs can be referenced by :{context}
   * references.
   * @param pAttachPoint The attach point to be used as the initial context item (".") of the transformation.
   * @param pXsltInputStream InputStream for reading in the XSLT document.
   * @param pDestinationDOM The destination for the transformation.
   * @param pContextUElem For resolving DOMs referred to by the fox:ctxt() function.
   */
  public static void transformToDOM(DOM pSourceDOM, DOM pAttachPoint, InputStream pXsltInputStream, DOM pDestinationDOM, ContextUElem pContextUElem){
    //TODO this method does not currently rewrite :{context} references
    transformToDOM(pSourceDOM, pAttachPoint, DOM.createDocument(pXsltInputStream, false), pDestinationDOM, pContextUElem);
  }

   /**
    * Runs an XSLT transformation of the given source DOM using the DOM provided as an XSLT document.
    * The output is written to pDestinationDOM. Using :{context} syntax is not currently supported by this flavour -
    * the XSLT document must reference the fox:ctxt() function instead.
    * @param pSourceDOM The root DOM to be used in the transformation. Additional DOMs can be referenced by :{context}
    * references.
    * @param pAttachPoint The attach point to be used as the initial context item (".") of the transformation.
    * @param pXsltDOM DOM containing the XSLT document.
    * @param pDestinationDOM The destination for the transformation.
    * @param pContextUElem For resolving DOMs referred to by the fox:ctxt() function.
    */
  public static void transformToDOM(DOM pSourceDOM, DOM pAttachPoint, DOM pXsltDOM, DOM pDestinationDOM,
                                    ContextUElem pContextUElem){

    //Ensure the FOX namespace is declared on the XSLT so Fox function calls can be evaluated
    pXsltDOM.getRootElement().addNamespaceDeclaration(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI);

    XsltCompiler lCompiler = SaxonEnvironment.getXsltCompiler();
    SaxonEnvironment.FoxXsltErrorListener lErrorListener = SaxonEnvironment.newFoxXsltErrorListener();

    XsltExecutable lXsltExecutable = null;

    try {
      Track.pushDebug("XsltCompile");
      lCompiler.setErrorListener(lErrorListener);
      lXsltExecutable = lCompiler.compile(pXsltDOM.wrap());
    }
    catch (SaxonApiException e) {
      //This will throw ExInternals if something went wrong
      lErrorListener.processErrors();
    }
    finally {
      Track.pop("XsltCompile");
    }

    XsltTransformer lTransformer = lXsltExecutable.load();

    pDestinationDOM.removeAllChildren();
    DOMContentHandler lContentHandler = new DOMContentHandler(pDestinationDOM);

    try {
      Track.pushDebug("XsltPrepare");
      //TODO PN - this doesn't seem to work
      lTransformer.setInitialContextNode(new XdmNode(pAttachPoint.wrap()));
      lTransformer.setSource(pSourceDOM.wrap());
      lTransformer.setDestination(new SAXDestination(lContentHandler));
    }
    catch (SaxonApiException e) {
      throw new ExInternal("Error occurred when setting up XSLT transformation", e);
    }
    finally {
      Track.pop("XsltPrepare");
    }

    try {
      Track.pushDebug("XsltExecute");
      SaxonEnvironment.setThreadLocalContextUElem(pContextUElem);
      lTransformer.transform();
    }
    catch (SaxonApiException e) {
      throw new ExInternal("Error during transformation", e);
    }
    finally {
      Track.pop("XsltExecute");
      SaxonEnvironment.clearThreadLocalContextUElem();
    }
  }

}

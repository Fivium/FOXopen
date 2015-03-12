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
package net.foxopen.fox.webservices;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.entrytheme.ThemeParam;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * SOAP-specific implementation for wrapping web service requests. Provides
 * error handling, data extraction and response methods in a generic way.
 */
public class WebServiceRequestWrapperSOAP implements WebServiceRequestWrapper
{
  public static final String SOAP_NAMESPACE_URI = "http://schemas.xmlsoap.org/soap/envelope/";
  public static final String SOAP_BODY_ELEM_NAME = "Body";
  public static final String SOAP_HEADER_ELEM_NAME = "Header";
  public static final String SOAP_MUST_UNDERSTAND_ATTR_NAME = "mustUnderstand";
  public static final String SOAP_MUST_UNDERSTAND_TRUE = "1";
  public static final String NS_DEFAULT_PREFIX = "ns";

  // Web service errors (special case error codes)
  public static final String SERVICE_ERROR_SERVER = "WEBSERVICE_SERVER";
  public static final String SERVICE_ERROR_CLIENT = "WEBSERVICE_CLIENT";
  public static final String SERVICE_ERROR_HEADER = "WEBSERVICE_HEADER";

  // Fault constants
  public static final String SOAP_FAULT_SERVER = "soap:Server";
  public static final String SOAP_FAULT_CLIENT = "soap:Client";
  public static final String SOAP_FAULT_VERSION = "soap:VersionMismatch";
  public static final String SOAP_FAULT_HEADER = "soap:MustUnderstand";

  // Namespace and element name for FOX thread ref SOAP header
  public static final String FOX_NAMESPACE_URI = "http://www.foxopen.net/soap/";
  public static final String FOX_NAMESPACE_PREFIX = "fox";
  public static final String FOX_THREAD_REF_HEADER_NAME = "threadref";
  public static final String FOX_XFSESSION_HEADER_NAME = "xfsessionid";

  public static final HashMap gSupportedWebServiceErrorCodeMap = new HashMap();
  static {
    gSupportedWebServiceErrorCodeMap.put(SERVICE_ERROR_SERVER, SOAP_FAULT_SERVER);
    gSupportedWebServiceErrorCodeMap.put(SERVICE_ERROR_CLIENT, SOAP_FAULT_CLIENT);
    gSupportedWebServiceErrorCodeMap.put(SERVICE_ERROR_HEADER, SOAP_FAULT_HEADER);
  }

  private DOM mRequestDataDOM;            // Request DOM
  private FoxRequest mFoxRequest;         // Reference to original FoxRequest
  private DOM mSOAPBody;                  // Reference to SOAP Body element
  private DOM mSOAPHeader;                // Reference to SOAP Header element
  private String mOperationName;          // Operation name
  private String mOperationNamespaceURI;  // Operation name including namespace
  private DOMList mOperationParams;       // Forest within operation
  private DOMList mOperationHeaderParams; // Forest within operation
  private boolean mInitialised = false;   // Flag that indicates we have JIT initialised
  private boolean mRPCStyle = false;      // Flag that indicates RPC or document style request
  private Mod mMod;                       // Target endpoint module
  private String mFoxThreadRef;           // FOX thread ref (for persistent XThread usage)
  private String mXfsessionId;            // FOX Xfsessionid (for persistent XThread usage)

  /**
   * Protected constructor that requires a request and the parsed XML body of
   * that request. Request used to interrogate headers.
   * @param pFoxRequest the request
   * @param pRequestDataDOM the request body as a DOM
   * @throws ExUserRequest invalid SOAP syntax
   */
  protected WebServiceRequestWrapperSOAP (FoxRequest pFoxRequest, DOM pRequestDataDOM, Mod pMod)
  throws ExUserRequest {
    // Sanity check, should never be a problem
    if (pRequestDataDOM == null) {
      throw new ExInternal("A null request data DOM was passed to WebServiceRequestWrapperSOAP");
    }
    else {
      mRequestDataDOM = pRequestDataDOM;
    }

    // Sanity check, should never be a problem
    if (pMod == null) {
      throw new ExInternal("A null module was passed to WebServiceRequestWrapperSOAP");
    }
    else {
      mMod = pMod;
    }

    // Get element prefix
    if (!SOAP_NAMESPACE_URI.equals(mRequestDataDOM.getNamespaceURI())) {
      throw new ExInternal("Non-SOAP request passed to SOAP Web Service Wrapper, expected root namespace '" + SOAP_NAMESPACE_URI + "'");
    }

    mFoxRequest = pFoxRequest;
  } // WebServiceRequestWrapperSOAP

  /**
   * Extract the operation that the request is attempting to call.
   * @return name of operation (should map to a service-based entry theme).
   * @throws ExUserRequest soap request was invalid
   * @throws ExActionFailed soap request was invalid or we don't support a required header
   */
  public String getOperationName()
  throws ExUserRequest, ExActionFailed {
    init();
    return mOperationName;
  } // getOperationName

  /**
   * Extract the request payload (the parameters to the operation).
   * @return xml request headerbody
   * @throws ExUserRequest soap request was invalid
   * @throws ExActionFailed soap request was invalid or we don't support a required header
   */
  public DOMList getRequestData()
  throws ExUserRequest, ExActionFailed {
    init();
    if (mOperationParams != null) {
      return mOperationParams;
    }
    else {
      return mOperationParams = new DOMList();
    }
  } // getRequestData

  public DOMList getRequestHeader()
  throws ExUserRequest, ExActionFailed {
    init();
    if (mOperationHeaderParams != null) {
      return mOperationHeaderParams;
    }
    else {
      return mOperationHeaderParams = new DOMList();
    }
  } // getRequestHeader

  /**
   * Extract the FOX thread ref (if present on the request).
   * @return thread id as a String
   */
  public String getFoxThreadRef()
  throws ExUserRequest, ExActionFailed {
    init();
    return mFoxThreadRef;
  } //getFoxThreadRef

  /**
   * Extract the FOX XfsessionId (if present on the request).
   * @return thread id as a String
   */
  public String getXfsessionId()
  throws ExUserRequest, ExActionFailed {
    init();
    return mXfsessionId;
  } // getXfsessionId

  /**
   * Creates a response based on a response data DOM.
   * @param pResponseDataDOM data to wrap in a web service response
   * @return the prepared response
   */
  public FoxResponse createResponse (DOM pResponseDataDOM) {
    // Wrap DOM in DOMList and pass to real method
    DOMList lTempDOMList = new DOMList();
    lTempDOMList.add(pResponseDataDOM);
    return createResponse(lTempDOMList);
  } // createResponse

  /**
   * Creates a response based on a response data DOMList.
   * @param pResponseDataDOMList data to wrap in a web service response
   * @return the prepared response
   */
  public FoxResponse createResponse (DOMList pResponseDataDOMList) {
    return createResponse(pResponseDataDOMList, null, null, null);
  }

  /**
   * Creates a response based on a response data DOMList.
   * @param pResponseDataDOMList data to wrap in a web service response
   * @param pResponseHeaderDOMList headers to wrap in a web service response
   * @return the prepared response
   */
  public FoxResponse createResponse (DOMList pResponseDataDOMList, DOMList pResponseHeaderDOMList) {
    return createResponse(pResponseDataDOMList, pResponseHeaderDOMList, null, null);
  }

  /**
   * Creates a response based on a response data DOMList.
   * @param pResponseDataDOMList data to wrap in a web service response
   * @param pResponseHeaderDOMList headers to wrap in a web service response
   * @param pOptionalFoxThreadRef optional thread ref to append to outgoing headers
   * @param pOptionalXfsessionId optional xfsessionid to append to outgoing headers
   * @return the prepared response
   */
  public FoxResponse createResponse (DOMList pResponseDataDOMList, DOMList pResponseHeaderDOMList, String pOptionalFoxThreadRef, String pOptionalXfsessionId) {
    // Create an envelope and append the node into the body
    DOM lEnvelope = createEnvelope(pOptionalFoxThreadRef, pOptionalXfsessionId);

    // Add operation namespace declaration to envelope
    lEnvelope.addNamespaceDeclaration(NS_DEFAULT_PREFIX, mOperationNamespaceURI);
    DOM lEnvelopeBody = lEnvelope.get1EOrNull("/*/soap:Body");
    DOM lEnvelopeHeader = lEnvelope.get1EOrNull("/*/soap:Header");

    // If we have SOAP headers to append, do so here regardless of type
    if (pResponseHeaderDOMList != null) {
      pResponseHeaderDOMList.copyContentsTo(lEnvelopeHeader);
    }

    // By convention, response element should be request element name with
    // "Response" appended (in the case of RPC), otherwise we're just copying
    // the return values
    if (mRPCStyle) {
      DOM lResponseDOM = DOM.createDocument(NS_DEFAULT_PREFIX + ":" + mOperationName+"Response", mOperationNamespaceURI, false);
      pResponseDataDOMList.copyContentsTo(lResponseDOM);

      // Copy response into SOAP body
      lResponseDOM.copyToParent(lEnvelopeBody);
    }
    else {
      // Document style responses are supposed to be in context of a namespace
      // so here we just-in-time assign the namespace prefix for this service
      for (int i = 0; i < pResponseDataDOMList.getLength(); i++) {
        DOM lResponseItem = pResponseDataDOMList.item(i);
        // Add namespace prefix to elements on the way out
        DOM lNewDOM = lEnvelopeBody.addElem(NS_DEFAULT_PREFIX + ":" + lResponseItem.getLocalName());
        lResponseItem.copyContentsTo(lNewDOM);
      }
    }

    // Remove all foxid attrs
    lEnvelope.removeRefsRecursive();

    // Wrap response and return
    return createFoxResponseCHAR(lEnvelope);
  } // createResponse

  /**
   * Creates a response based on an thrown exception.
   * @param pThrowable the exception to wrap
   * @return the prepared response
   */
  public FoxResponse createResponse (Throwable pThrowable) {
    return createResponse(pThrowable, null, null);
  }

  /**
   * Creates a response based on an thrown exception.
   * @param pThrowable the exception to wrap
   * @param pOptionalFoxThreadRef optional thread ref to append to outgoing headers
   * @param pOptionalXfsessionId optional xfsessionid to append to outgoing headers
   * @return the prepared response
   */
  public FoxResponse createResponse (Throwable pThrowable, String pOptionalFoxThreadRef, String pOptionalXfsessionId) {
    DOM lEnvelope = createEnvelope(pOptionalFoxThreadRef, pOptionalXfsessionId);
    try {
      DOM lFault = lEnvelope.create1E("/soap:Envelope/soap:Body/soap:Fault");
      FoxResponse lFoxResponse;
      Throwable lRootCauseThrowable;
      final String lFaultCode;

      // Chain through exceptions until we get to the root cause
      lRootCauseThrowable = pThrowable;
      while (lRootCauseThrowable.getCause() != null) {
        lRootCauseThrowable = lRootCauseThrowable.getCause();
      }

      // ExActionFailed can be thrown by fm:throw
      if (lRootCauseThrowable instanceof ExActionFailed) {
        ExActionFailed lEx = (ExActionFailed) lRootCauseThrowable;
        String lExceptionCode = lEx.getCode();
        lFaultCode = XFUtil.nvl((String) gSupportedWebServiceErrorCodeMap.get(lExceptionCode), SOAP_FAULT_SERVER);
      }
      // ExUserRequest thrown if validation fails on the way in
      else if (lRootCauseThrowable instanceof ExUserRequest) {
        lFaultCode = SOAP_FAULT_CLIENT;
      }
      // FOX has hit an unexpected error, so we have to assume it was server-side (module or engine itself)
      else {
        lFaultCode = SOAP_FAULT_SERVER;
      }

      lFault.addElem("faultcode", lFaultCode);
      // Belt and braces, if for some reason the root cause doesn't have a message, just take the higher level message
      lFault.addElem("faultstring", XFUtil.nvl(lRootCauseThrowable.getMessage(), pThrowable.getMessage()));
      lFoxResponse = createFoxResponseCHAR(lEnvelope);

      // Set HTTP status 500 on all soap:Fault responses to be WS-I compliant
      ((FoxResponseCHAR) lFoxResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

      return lFoxResponse;
    }
    catch (ExCardinality ex) {
      throw new ExInternal("Internal xpath error processing SOAP Fault", ex);
    }
  } // createResponse

  private FoxResponseCHAR createFoxResponseCHAR (DOM pFinalResponseDOM) {
    return new FoxResponseCHAR(
      "text/xml;charset=UTF-8"
    , new StringBuffer(pFinalResponseDOM.getRootElement().outputNodeToString()) // outputNodeToString suppresses FOX comment
    , 0
    );
  } // createFoxResponseCHAR

  /**
   * Convenience method that creates a basic soap Envelope element.
   * @param pOptionalFoxThreadRef optional thread ref to append to outgoing headers
   * @param pOptionalXfsessionId optional xfsessionid to append to outgoing headers
   * @return SOAP envelope as DOM
   */
  private DOM createEnvelope (String pOptionalFoxThreadRef, String pOptionalXfsessionId) {
    DOM lEnvelope = DOM.createDocument("soap:Envelope", SOAP_NAMESPACE_URI, true);
    DOM lEnvelopeHeader = lEnvelope.addElem("soap:Header");
    lEnvelope.addElem("soap:Body");

    // If we have a fox thread ref, write it into the outgoing SOAP headers
    // so that other calls in can resume the same thread
    if (!XFUtil.isNull(pOptionalFoxThreadRef)) {
      lEnvelope.setAttr("xmlns:" + FOX_NAMESPACE_PREFIX, FOX_NAMESPACE_URI);
      lEnvelopeHeader.addElem(FOX_NAMESPACE_PREFIX + ":" + FOX_THREAD_REF_HEADER_NAME, pOptionalFoxThreadRef);
    }

    // If we have a fox xfsessionid, write it into the outgoing SOAP headers
    // so that other calls in can resume the same thread
    if (!XFUtil.isNull(pOptionalXfsessionId)) {
      lEnvelope.setAttr("xmlns:" + FOX_NAMESPACE_PREFIX, FOX_NAMESPACE_URI);
      lEnvelopeHeader.addElem(FOX_NAMESPACE_PREFIX + ":" + FOX_XFSESSION_HEADER_NAME, pOptionalXfsessionId);
    }

    return lEnvelope;
  } // createEnvelope

  private void init ()
  throws ExUserRequest, ExActionFailed {
    // Bail out if we've done this already, wrappers are immutable
    if (mInitialised) {
      return;
    }

    // Loop through all SOAP envelope child nodes and hunt for the Body and Header elements
    DOMList lSOAPEnvelopChildElements = mRequestDataDOM.getChildElements();
    ENVELOPE_CHILD_LOOP: for (int i = 0; i < lSOAPEnvelopChildElements.getLength(); i++) {
      DOM lCurrentNode = lSOAPEnvelopChildElements.item(i);
      if (SOAP_NAMESPACE_URI.equals(lCurrentNode.getNamespaceURI())) {
        // Check to see which SOAP elements exist that we support
        String lLocalName = lCurrentNode.getLocalName();
        if (SOAP_BODY_ELEM_NAME.equals(lLocalName)) {
          if (mSOAPBody == null) {
            mSOAPBody = lCurrentNode;
          }
          else {
            throw new ExActionFailed(SERVICE_ERROR_CLIENT, "Expected exactly one SOAP Body element");
          }
        }
        else if (SOAP_HEADER_ELEM_NAME.equals(lLocalName)) {
          if (mSOAPHeader == null) {
            mSOAPHeader = lCurrentNode;
          }
          else {
            throw new ExActionFailed(SERVICE_ERROR_CLIENT, "Expected 0 or 1 SOAP Header elements");
          }
        }
      }
    } // ENVELOPE_CHILD_LOOP

    if (mSOAPBody == null) {
      throw new ExActionFailed(SERVICE_ERROR_CLIENT, "Expected exactly one SOAP Body element");
    }

    EntryTheme lMatchedTheme = null;

    // Look at the HTTP headers for the SOAPAction
    DOMList lSOAPBodyContents = mSOAPBody.getChildElements();

    // We have nothing to work with DOM-wise, so we have to take the SOAPAction header value
    if (lSOAPBodyContents.getLength() == 0) {

      String lHeader = mFoxRequest.getHttpRequest().getHeader("SOAPAction").replaceAll("\\\"", ""); // SOAPAction is delimited by double quotes
      if (!XFUtil.isNull(lHeader)) {
        mOperationNamespaceURI = getDefaultNamespaceURI();

        if (lHeader.indexOf(mOperationNamespaceURI) != -1) {
          StringBuffer lSOAPAction = new StringBuffer(lHeader);
          mOperationName = XFUtil.pathPopTail(lSOAPAction);
        }
        else {
          mOperationName = lHeader;
        }

        // Empty params forest
        mOperationParams = new DOMList();

        try {
          EntryTheme lTheme = mMod.getEntryTheme(mOperationName);
          mRPCStyle = lTheme.getType().equals(EntryTheme.gTypeServiceRPC);
          lMatchedTheme = lTheme;
        }
        catch (ExUserRequest ex) {
          throw new ExActionFailed(SERVICE_ERROR_CLIENT, "Unrecognised operation specified in SOAPAction header.");
        }
      }
      else {
        throw new ExActionFailed(SERVICE_ERROR_CLIENT, "Invalid SOAP Request, no soap:Body child elements or SOAPAction provided.");
      }
    } // lSOAPBodyContents.getLength() == 0

    // If we have exactly one element, then we could be receiving a document wrapped or rpc request
    // so we need to look at the target endpoint module, seeking an entry-theme with a name that
    // matches the soap:Body child element name
    if (lSOAPBodyContents.getLength() == 1) {
      // Look at the root name
      String lContentChildElemName = lSOAPBodyContents.item(0).getLocalName().intern();

      // Loop through entry-themes and see if the root element of the request
      // matches something we're expecting
      Iterator i = mMod.getServiceEntryThemes().iterator();
      THEME_LOOP: while (i.hasNext()) {
        EntryTheme lTheme = (EntryTheme) i.next();

        // If we have a matching name, validate that it's RPC style and we're done
        if (lTheme.getName().equals(lContentChildElemName)) {
          // Take all the values we want
          mOperationName = lContentChildElemName;
          mOperationNamespaceURI = lSOAPBodyContents.item(0).getNamespaceURI();
          mRPCStyle = lTheme.getType().equals(EntryTheme.gTypeServiceRPC);
          lMatchedTheme = lTheme;
          // TODO: Child elements shouldn't have namespace declarations on them
          // ideally we should check this and throw errors here (parsing will
          // fail later)
          mOperationParams = lSOAPBodyContents.item(0).getChildElements();

          // Assert that the matched operation is RPC style, otherwise a document
          // request root element is clashing with an RPC style operation name
          if (mRPCStyle == false) {
            throw new ExActionFailed(SERVICE_ERROR_CLIENT, "Element '" + lContentChildElemName + "' was provided in the soap:Body, but operation '" + lTheme.getName() + "' is not type rpc");
          }

          break THEME_LOOP;
        }
      } // THEME_LOOP
    } // lSOAPBodyContents.getLength() == 1

    // If we get here, we either have more than one child element (not WS-I
    // compliant, but we're nice, so we will tolerate it) or we didn't find
    // an RPC style request in the above loop
    if (mOperationName == null || lSOAPBodyContents.getLength() > 1) {

      boolean lOperationMatched = false;

      // Loop through entry themes again and find a case where the soap:Body
      // child elements exactly match a document type operation part list
      Iterator i = mMod.getServiceEntryThemes().iterator();
      THEME_LOOP: while (i.hasNext()) {
        EntryTheme lTheme = (EntryTheme) i.next();

        // Skip over RPC style operations
        if (lTheme.getType().equals(EntryTheme.gTypeServiceRPC)) {
          continue THEME_LOOP;
        }

        Iterator p = lTheme.getParamList().iterator(); // TODO - NP - this now gets a typed list, foreach it and avoid casts
        ELEM_LOOP: for (int n = 0; n < lSOAPBodyContents.getLength(); n++) {
          boolean lMatched = false;
          PARAM_LOOP: while (p.hasNext()) {
            ThemeParam lParam = (ThemeParam) p.next();
            if (lParam.getName().equals(lSOAPBodyContents.item(n).getLocalName())) {
              // Request element matches a part
              lMatched = true;
              break PARAM_LOOP;
            }
          } // PARAM_LOOP

          // No match found for an element, skip to next entry-theme
          if (lMatched == false) {
            continue THEME_LOOP;
          }
        } // ELEM_LOOP

        // Check if we've already matched an operation
        if (lOperationMatched) {
          throw new ExActionFailed(SERVICE_ERROR_CLIENT, "Request matches more than one operation in the WSDL for this service.");
        } else {
          // If we get here, we've paired a request forest and a document style
          // operation (take full list of params forward)
           lOperationMatched = true;
           lMatchedTheme = lTheme;
           processSeekMatches(lTheme, lSOAPBodyContents);
        }

      } // THEME_LOOP
    } // lSOAPBodyContents.getLength() > 1

    // No operation was matched by name, check if there is an operation with xs:any
    // as the (only) parameter.
    if (mOperationName == null && lSOAPBodyContents.getLength() == 1) {
      List lServiceEntryThemes = mMod.getServiceEntryThemes();
      Iterator i = lServiceEntryThemes.iterator();
      while (i.hasNext())
      {
        EntryTheme lTheme = ((EntryTheme) i.next());
        List lParamList = lTheme.getParamList(); // TODO - NP - this now gets a typed list, no need to cast
        if (lParamList.size() == 1 && ((ThemeParam) lParamList.get(0)).getType().equals("xs:any")) {
          if (lServiceEntryThemes.size() > 1) {
            throw new ExActionFailed(SERVICE_ERROR_CLIENT, "Request matched more than one defined operation in the WSDL for this service due to ambiguously defined parameters");
          } else {
           // Request matches the only theme in the module which takes an
           // xs:any as the only parameter. Use this as our operation
            lMatchedTheme = lTheme;
            processSeekMatches(lTheme, lSOAPBodyContents);
          }
        }
      }
    }
    // Request didn't match an operation, throw it out
    if (mOperationName == null) {
      throw new ExActionFailed(SERVICE_ERROR_CLIENT, "Request did not match any operation provided in the WSDL for this service");
    }

    // Check headers. If they state we must understand them (soap:mustUnderstand="1"), we should server-fault
    // unless they are declared in the theme's header param list, or are FOX web service implementation
    // specific headers
    if (mSOAPHeader != null) {
      mOperationHeaderParams = mSOAPHeader.getChildElements();
      DOMList lSOAPHeaderContents = mSOAPHeader.getChildElements();
      HEADER_CONTENT_LOOP: for (int i = 0; i < lSOAPHeaderContents.getLength(); i++) {

        DOM lHeader = lSOAPHeaderContents.item(i);
        if (FOX_NAMESPACE_URI.equals(lHeader.getNamespaceURI())) {
          if (FOX_THREAD_REF_HEADER_NAME.equals(lHeader.getLocalName())) {
            mFoxThreadRef = lHeader.value();
          }
          else if (FOX_XFSESSION_HEADER_NAME.equals(lHeader.getLocalName())) {
            mXfsessionId = lHeader.value();
          }
          continue;
        }

        // For each child of our soap:Header, check to see if we *must* understand it
        Map lAttrsMap = lSOAPHeaderContents.item(i).getAttributeMap(
          SOAP_NAMESPACE_URI // pNamespaceURI - filter on SOAP namespace
        , true               // pLocalNames - suppress namespace prefixes
        );

        String lMustUnderstand = (String) lAttrsMap.get(SOAP_MUST_UNDERSTAND_ATTR_NAME);
        if (SOAP_MUST_UNDERSTAND_TRUE.equals(lMustUnderstand) && !lMatchedTheme.getHeaderParamMap().containsKey(lSOAPHeaderContents.item(i).getName())) {
          throw new ExActionFailed(SERVICE_ERROR_HEADER, "Server does not understand header '" + lSOAPHeaderContents.item(i).getName() + "'");
        }
      } // HEADER_CONTENT_LOOP
    }

    // This only needs to be done once, so flag that this wrapper is fully initialised
    mInitialised = true;
  }

  private void processSeekMatches(EntryTheme pTheme, DOMList pSOAPBodyContents) {
    mOperationName = pTheme.getName();

    // Default the namespace, regardless of what we were sent, as we'll be specifying our own schema definition for
    // output in the WSDL
    mOperationNamespaceURI = getDefaultNamespaceURI();

    mRPCStyle = pTheme.getType().equals(EntryTheme.gTypeServiceRPC);

    mOperationParams = new DOMList();
    if (mRPCStyle) {
      // Child elements probably (and certainly should) have a namespace URI
      // on them, if WS-I is being followed, so we need to discard the prefixes
      // before using as parameters to an entry-theme
      for (int j = 0; j < pSOAPBodyContents.getLength(); j++) {
        DOM lCurrentItem = pSOAPBodyContents.item(j);
        DOM lNewDOM = DOM.createUnconnectedElement(lCurrentItem.getLocalName());
        mOperationParams.add(lNewDOM);
        // TODO: Child elements shouldn't have namespace declarations on them
        // ideally we should check this and throw errors here (parsing will
        // fail later)
        lCurrentItem.getChildElements().copyContentsTo(lNewDOM);
      }
    }
    else {
      for (int j = 0; j < pSOAPBodyContents.getLength(); j++) {
        mOperationParams.add(pSOAPBodyContents.item(j));
      }
    }
  }

  /**
   * Returns a default namespace for this request.
   * @return namespace as string
   */
  private String getDefaultNamespaceURI () {
    return WSDLGenerator.BASE_NAMESPACE_URI + mMod.getApp().getMnemonicName() + "/" + mMod.getName();
  }

} // WebServiceRequestWrapperSOAP

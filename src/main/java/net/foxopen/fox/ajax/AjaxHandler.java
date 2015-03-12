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
package net.foxopen.fox.ajax;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.cache.FoxTTLCache;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Interrogates and redirects incoming Ajax requests to appropriate concrete handlers.
 */
public abstract class AjaxHandler {

  private final String mHandlerMnem;
  private static final HashMap mAjaxHandlers = new HashMap();


  public final static String FOX_HEARTBEAT_COOKIE_NAME = "fox-heartbeat";
  public final static String AJAX_DATA_FIELD_NAME = "ajax_data";


  // Used temporarily to cache unique keys sent by AJAX requests
  // for detecting/preventing duplicate requests from IE6
  public static final Map gAjaxUniqueKeys = new FoxTTLCache("AjaxUniqueKeys",10,50, 1000 * 20, false, 1);

  /**
   * Processes a response.
   * @param pFoxRequest the original request to parse and process
   * @return a FoxResponse for the Ajax request
   */
  public static FoxResponse processResponse (FoxRequest pFoxRequest) {
    try {
      // Attempt to give a real response
      //TODO PN this has been modified from original impelmentation and not tested
      String lString;
      try {
        lString = IOUtils.toString(pFoxRequest.getHttpRequest().getReader());
      }
      catch (IOException e) {
        throw new ExInternal("Failed to read request body",e);
      }

      DOM lRequestDOM = DOM.createDocumentFromXMLString(lString);
      return processResponseInternal(
        pFoxRequest
      , lRequestDOM
      , null // XThread
      );
    }
    catch (Throwable ex) {
      // Wrap any exception in XML and return
      DOM lDOM = DOM.createDocument("ajax-response");
      lDOM.addElem("error", XFUtil.getJavaStackTraceInfo(ex)).setAttr("severity", "warning");
      return new FoxResponseCHAR (
        "text/xml; charset=UTF-8"                       // content type
      , new StringBuffer(lDOM.outputDocumentToString()) // content
      , 0                                               // browser cache MS
      );
    }
  } // processResponse

  /**
   * Processes an ajax data package "request" coming from a FOX form POST.
   * @param pFoxRequest the origin fox request
   * @param pAjaxDataDOMString the request DOM as a string
   */
  public static void processResponse (FoxRequest pFoxRequest, String pAjaxDataDOMString, Object pXThread) {
    DOM lRequestDOM = DOM.createDocumentFromXMLString(pAjaxDataDOMString);

    // Throw away the response, as we're dealing with this internally from an XThread
    FoxResponse lDummy = processResponseInternal(pFoxRequest, lRequestDOM, pXThread);
  } // processResponse

  /**
   * Sets an attribute on an HTML elemement.
   * @param pDataPackage the data package DOM to append data to
   * @param pHtmlElementId the id of the HTML element to target
   * @param pAttributeName attr name as a string
   * @param pAttributeValue attr value as a string
   */
  protected static void setElementAttributeCommand (DOM pDataPackage, String pHtmlElementId, String pAttributeName, String pAttributeValue) {
    DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
    DOM lCommand = lCommandList.addElem("command").addElem("set-element-attribute");
    lCommand.addElem("element-id", pHtmlElementId);
    lCommand.addElem("attribute-name", pAttributeName);
    lCommand.addElem("attribute-value", pAttributeValue);
  } // setElementAttributeCommand

  /**
   * Replaces an HTML element.
   * @param pDataPackage the data package DOM to append data to
   * @param pHtmlElementId the id of the HTML element to target
   * @param pElementType the type of element to create
   * @param pAttrMap a mapping of name, value pairs to write out as attributes
   */
  protected static void replaceElementCommand (DOM pDataPackage, String pHtmlElementId, String pElementType, HashMap pAttrMap) {
    DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
    DOM lCommand = lCommandList.addElem("command").addElem("replace-element");

    lCommand.addElem("element-id", pHtmlElementId);
    lCommand.addElem("element-type", pElementType);

    DOM lAttrList = lCommand.addElem("append-attribute-list");
    Iterator i = pAttrMap.keySet().iterator();
    while (i.hasNext()) {
       String key = (String)i.next();
       String value = (String)pAttrMap.get(key);
       DOM lAttrEntry = lAttrList.addElem("append-attribute");
       lAttrEntry.addElem("name", key);
       lAttrEntry.addElem("value", value);
    }
  } // replaceElementCommand

  /**
   * Replaces the child nodes of an HTML element.
   * @param pDataPackage the data package DOM to append data to
   * @param pHtmlElementId the id of the HTML element to target
   * @param pElementType the type of element to create
   * @param pAttrMap a mapping of name, value pairs to write out as attributes
   */
  protected static void replaceChildElementsCommand (DOM pDataPackage, String pHtmlElementId, String pElementType, HashMap pAttrMap) {
    DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
    DOM lCommand = lCommandList.addElem("command").addElem("replace-child-elements");

    lCommand.addElem("element-id", pHtmlElementId);
    lCommand.addElem("element-type", pElementType);

    DOM lAttrList = lCommand.addElem("append-attribute-list");
    Iterator i = pAttrMap.keySet().iterator();
    while (i.hasNext()) {
       String key = (String)i.next();
       String value = (String)pAttrMap.get(key);
       DOM lAttrEntry = lAttrList.addElem("append-attribute");
       lAttrEntry.addElem("name", key);
       lAttrEntry.addElem("value", value);
    }
  } // replaceElementCommand

   /**
    * Sets the background of an element
    * @param pDataPackage the data package DOM to append data to
    * @param pHtmlElementId the id of the HTML element to target
    * @param pImageUrl the image url to use
    * @param pRemoveContents remove the container contents (if any)
    */
   protected static void setElementBackgroundImage (DOM pDataPackage, String pHtmlElementId, String pImageUrl, boolean pRemoveContents) {
     DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
     DOM lCommand = lCommandList.addElem("command").addElem("set-background-image");
     lCommand.addElem("element-id", pHtmlElementId);
     lCommand.addElem("image-url", pImageUrl);
     lCommand.addElem("remove-contents", String.valueOf(pRemoveContents));
   } // setElementBackgroundImage

  /**
   * Adds a data package to the service queue.
   * @param pDataPackage the data package DOM to append data to
   * @param pPriority the priority to assign for processing
   */
  protected static void addToServiceQueueCommand (DOM pDataPackage, String pPriority) {
    DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
    DOM lCommand = lCommandList.addElem("command").addElem("add-to-service-queue");
    lCommand.addElem("priority", pPriority);
  }

  /**
   * Registers an HTML element with the synchroniser client-side code, thus
   * registering interest between the client-side element and script, and a
   * server-based Ajax handler.
   * @param pDataPackage the data package DOM to append data to
   * @param pHtmlElementId the html element to target
   * @param pAttrArray the attributes that the client should report with its requests
   */
  protected static void registerHtmlElementCommand (DOM pDataPackage, String pHtmlElementId, ArrayList pAttrArray) {
    DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
    DOM lCommand = lCommandList.addElem("command").addElem("register-html-element");
    lCommand.addElem("element-id", pHtmlElementId);

    DOM lAttrList = lCommand.addElem("attribute-list");
    Iterator i = pAttrArray.iterator();
    while (i.hasNext()) {
      lAttrList.addElem("attribute", (String) i.next());
    }
  }

  /**
   * Sets click capture mode for a client-side widget.
   * @param pDataPackage the data package DOM to append data to
   * @param pHtmlElementId the html element to target
   * @param pClicksToCapture number of clicks to capture
   * @param pEventLabelToRaise the event label to raise when the number of clicks is reached
   */
  protected static void setClickCaptureModeCommand (DOM pDataPackage, String pHtmlElementId, int pClicksToCapture, String pEventLabelToRaise, String pCursorName) {
    DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
    DOM lCommand = lCommandList.addElem("command").addElem("set-click-capture-mode");
    lCommand.addElem("element-id", pHtmlElementId);
    lCommand.addElem("clicks", String.valueOf(pClicksToCapture));
    lCommand.addElem("event-label", pEventLabelToRaise);
    lCommand.addElem("cursor-name", pCursorName);
  }

  /**
   * Sets mouse wheel capture mode for a client-side image.
   * @param pDataPackage the data package DOM to append data to
   * @param pHtmlElementId the html element to target
   * @param pEventLabelToRaise the event label to raise when the image is zoomed
   */
  protected static void setImageZoomModeCommand (DOM pDataPackage, String pHtmlElementId, int pZoomPct, String pEventLabelToRaise, boolean pEnableMouse, boolean pEnableButtons, boolean pEnableAnimation) {
    DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
    DOM lCommand = lCommandList.addElem("command").addElem("set-image-zoom-mode");
    lCommand.addElem("element-id", pHtmlElementId);
    lCommand.addElem("event-label", pEventLabelToRaise);
    lCommand.addElem("zoom-pct", String.valueOf(pZoomPct));
    lCommand.addElem("enable-mouse", String.valueOf(pEnableMouse));
    lCommand.addElem("enable-buttons", String.valueOf(pEnableButtons));
    lCommand.addElem("enable-animation", String.valueOf(pEnableAnimation));
  }

  /**
   * Sets mouse panning mode for a client-side image.
   * @param pDataPackage the data package DOM to append data to
   * @param pHtmlElementId the html element to target
   * @param pPanEventLabel the event label to raise when the image is panned
   * @param pResetEventLabel the event label to raise when the image is reset
   */
  protected static void setImagePanModeCommand (DOM pDataPackage, String pHtmlElementId, String pPanEventLabel, String pResetEventLabel, boolean pEnableMouse, boolean pEnableButtons, boolean pEnableAnimation) {
    DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
    DOM lCommand = lCommandList.addElem("command").addElem("set-image-pan-mode");
    lCommand.addElem("element-id", pHtmlElementId);
    lCommand.addElem("event-label", pPanEventLabel);
    lCommand.addElem("reset-event-label", pResetEventLabel);
    lCommand.addElem("enable-mouse", String.valueOf(pEnableMouse));
    lCommand.addElem("enable-buttons", String.valueOf(pEnableButtons));
    lCommand.addElem("enable-animation", String.valueOf(pEnableAnimation));
  }

  /**
   * Sets mouse panning mode for a client-side image.
   * @param pDataPackage the data package DOM to append data to
   * @param pHtmlElementId the html element to target
   * @param pEventLabelToRaise
   * @param pPointInput
   * @param pLineInput
   * @param pConnectedLineInput
   * @param pPolygonInput
   * @param pSnapPointDOMList
   * @param pPreventDiagonals
   */
  protected static void setCanvasDrawCommand (
    DOM pDataPackage
  , String pHtmlElementId
  , String pEventLabelToRaise
  , boolean pPointInput
  , boolean pLineInput
  , boolean pConnectedLineInput
  , boolean pPolygonInput
  , DOMList pSnapPointDOMList
  , boolean pPreventDiagonals
  ) {
    DOM lCommandList = pDataPackage.getCreate1ENoCardinalityEx("./command-list");
    DOM lCommand = lCommandList.addElem("command").addElem("set-canvas-draw-mode");
    lCommand.addElem("element-id", pHtmlElementId);
    lCommand.addElem("event-label", pEventLabelToRaise);

    // Only show buttons if there's more than one valid mode
    int lTrueCount = 0;
    lTrueCount += pPointInput         ? 1 : 0;
    lTrueCount += pLineInput          ? 1 : 0;
    lTrueCount += pConnectedLineInput ? 1 : 0;
    lTrueCount += pPolygonInput       ? 1 : 0;
    lCommand.addElem("enable-mode-buttons", lTrueCount > 1 ? "true" : "false");

    // Establish which modes are enabled
    lCommand.addElem("point-mode", String.valueOf(pPointInput));
    lCommand.addElem("line-mode", String.valueOf(pLineInput));
    lCommand.addElem("connected-line-mode", String.valueOf(pConnectedLineInput));
    lCommand.addElem("poly-line-mode", String.valueOf(pPolygonInput));

    // Set the starting input mode starting with the lowest available facet
    if (pPointInput) {
      lCommand.addElem("starting-mode", "point");
    }
    else if (pLineInput) {
      lCommand.addElem("starting-mode", "line");
    }
    else if (pConnectedLineInput) {
      lCommand.addElem("starting-mode", "connected-line");
    }
    else if (pPolygonInput) {
      lCommand.addElem("starting-mode", "poly-line");
    }

    if (pSnapPointDOMList.getLength() > 0) {
      DOM lCoordList = lCommand.addElem("coord-list");
      pSnapPointDOMList.copyContentsTo(lCoordList);
    }

    lCommand.addElem("prevent-diagonals", String.valueOf(pSnapPointDOMList.getLength() > 0 && pPreventDiagonals));
  }

  /**
   * Inits a data package on the client page (by writing out through HtmlGenerator).
   * @param pHtmlGenerator the HtmlGenerator to write to
   * @param pDataPackage the data package to write out
   */
  protected static void initPackageOnClient (/*HtmlGenerator pHtmlGenerator,*/ String pHtmlElementId, DOM pDataPackage) {
    // Add required scripts for synchroniser
    // TODO PN - should be handled by widget/js dependency mapping
//    pHtmlGenerator.appendScriptToLoad("js/cookiepoller");
//    pHtmlGenerator.appendScriptToLoad("js/ajaxrequest");
//    pHtmlGenerator.appendScriptToLoad("js/servicequeue");
//    pHtmlGenerator.appendScriptToLoad("js/wgxpath");
//    pHtmlGenerator.appendScriptToLoad("js/foxdom");
//    pHtmlGenerator.appendScriptToLoad("js/synchroniser");
//    pHtmlGenerator.prependScriptToLoad("js/jquery");
//    pHtmlGenerator.appendScriptToLoad("js/jquery_mousewheel");
//    pHtmlGenerator.appendScriptToLoad("js/raphael");
//    pHtmlGenerator.appendScriptToLoad("js/canvas");
//
//    // Append to outgoing Ajax field
//    pHtmlGenerator.addAjaxPackage(pHtmlElementId, pDataPackage);
  }

  /**
   * Remove a data package from the client (drops it from the HtmlGenerator map).
   * @param pHtmlGenerator the HtmlGenerator instance to remove a data package from
   * @param pHtmlElementId the html element id to remove
   */
//  protected static void removePackageFromClient (HtmlGenerator pHtmlGenerator, String pHtmlElementId) {
//    pHtmlGenerator.removeAjaxPackage(pHtmlElementId);
//  }

  /**
   * Construct an AjaxHandler instance.
   * @param pHandlerMnem the unique mnemonic of the handler
   */
  public AjaxHandler (String pHandlerMnem) {
    // Mnem is required for later lookup and script generation
    if (XFUtil.isNull(pHandlerMnem)) {
      throw new ExInternal("Cannot construct an AjaxHandler with a null mnemonic");
    }
    mHandlerMnem = pHandlerMnem;

    // Put handler in HashMap for later lookup
    synchronized(mAjaxHandlers) {
      mAjaxHandlers.put(pHandlerMnem, this);
    }
  } // AjaxHandler

  /**
   * Get the current AjaxHandler mnemonic.
   * @return mnem as string
   */
  public String getMnem () {
    return mHandlerMnem;
  }

  /**
   * Builds a basic skeleton data package.
   * @param pDataIdentifier the data identifier to assign to this package
   * @param pServicePriority the service priority with which to treat this package
   * @param pDataToWrap the package payload
   * @return skeleton package DOM
   */
  protected DOM buildDataPackage (String pDataIdentifier, String pServicePriority, DOM pDataToWrap) {
    // Set up standard elements
    DOM lDOM = DOM.createDocument("data-package");
    lDOM.addElem("handler-mnem", getMnem());
    lDOM.addElem("data-identifier", pDataIdentifier);
    lDOM.addElem("default-service-priority", pServicePriority);
    lDOM.addElem("command-list");

    // Append data
    DOM lData = lDOM.addElem("data");
    pDataToWrap.copyContentsTo(lData);
    return lDOM;
  }

  /**
   * Processes a request, returns a FoxResponse to be served out to the calling Ajax code.
   * @param pFoxRequest the original request
   * @return the wrapped response
   * @throws ExInternal
   */
  private static FoxResponse processResponseInternal (FoxRequest pFoxRequest, DOM pRequestDOM, Object pOptionalXThread)
  throws ExInternal {

    DOM lResponseDOM = DOM.createDocument("ajax-response");
    DOMList lRequestBlocks = pRequestDOM.getUL("/*/data-package-list/data-package");
    DOM lPackageList = lResponseDOM.getCreate1ENoCardinalityEx("data-package-list");

    try {
      StringBuffer lUniqueKeySB = new StringBuffer(pRequestDOM.get1S("unique-key"));
      int lLength = lRequestBlocks.getLength();
      for (int i = 0; i < lLength; i++) {
        DOM lRequestBlock = lRequestBlocks.item(i);
        lUniqueKeySB.append("/");
        lUniqueKeySB.append(lRequestBlock.get1S("data-identifier"));
      }

      String llUniqueKey = lUniqueKeySB.toString();
      synchronized(gAjaxUniqueKeys) {
        if(((String) gAjaxUniqueKeys.get(llUniqueKey)) != null) {
          return new FoxResponseCHAR (
            "text/xml; charset=UTF-8"                               // content type
          , new StringBuffer(lResponseDOM.outputDocumentToString()) // content
          , 0                                                       // browser cache MS
          );
        }
      };
    }
    catch (ExCardinality ex) {
      throw new ExInternal("Failed to generate unique ajax request key", ex);
    }

    for (int i = 0; i < lRequestBlocks.getLength(); i++) {
      // Get each request fragment and look for mnemonic
      DOM lRequestFragment = lRequestBlocks.item(i);
      String lMnem;
      try {
        lMnem = lRequestFragment.get1S("handler-mnem");
      }
      catch (ExTooFew ex) {
        throw new ExInternal("handler-mnem not specified in request block");
      }
      catch (ExTooMany ex) {
        throw new ExInternal("Only one handler-mnem permitted in each data-package block");
      }

      // Look for Ajax Handler by mnemonic
      AjaxHandler lAjaxHandler = null;
      synchronized (mAjaxHandlers) {
        lAjaxHandler = (AjaxHandler) mAjaxHandlers.get(lMnem);
      }

      // Process the request and move the response DOM fragment to the overall response DOM
      if (lAjaxHandler != null) {
        DOM lResponse = lAjaxHandler.getAjaxResponse(pFoxRequest, lRequestFragment, pOptionalXThread);
        if (lResponse != null) {
          lResponse.copyToParent(lPackageList);
        }
      }
      else {
        throw new ExInternal("AjaxHandler '" + lMnem + "' not registered");
      }
    }

    // Send response back as XML
    return new FoxResponseCHAR (
      "text/xml; charset=UTF-8"                               // content type
    , new StringBuffer(lResponseDOM.outputDocumentToString()) // content
    , 0                                                       // browser cache MS
    );
  } // processResponse

  /**
   * Increments the fox heartbeat cookie to indicate to Ajax widgets in other
   * windows of the same browser that they  should request updates.
   * @param pFoxRequest the fox request on which to set the cookie
   */
  public void setUpdateCheckRequired(FoxRequest pFoxRequest) {
    String lCookieStrVal = pFoxRequest.getCookieValue(FOX_HEARTBEAT_COOKIE_NAME);
    // Cookie may not have been set or could have been lost, either way this is recoverable
    if (!XFUtil.isNull(lCookieStrVal)) {
      int lCookieVal = Integer.parseInt(lCookieStrVal);
      pFoxRequest.addCookie(FOX_HEARTBEAT_COOKIE_NAME, String.valueOf(lCookieVal+1));
    }
    else {
      pFoxRequest.addCookie(FOX_HEARTBEAT_COOKIE_NAME, "1");
    }
  }

  /**
   * Interpret the Ajax request and prepare a response DOM to send back.
   * @param pFoxRequest the original request
   * @param pRequestDOM the parsed DOM from the request
   * @return response as a DOM
   * @throws ExInternal
   */
  public abstract DOM getAjaxResponse (FoxRequest pFoxRequest, DOM pRequestDOM, Object pOptionalXThread)
  throws ExInternal;

  /**
   * Registers the element in question with a HtmlGenerator, which in turn
   * causes it to be registered with the client-side synchroniser.
   * @param pHtmlGenerator the HtmlGenerator instance to target
   * @param pHtmlElementId the HTML element id that has been written out
   * @param pData the data to register against the HTML element
   */
  public abstract void registerHtmlElementWithSynchroniser (/*HtmlGenerator pHtmlGenerator,*/ String pHtmlElementId, DOM pData);

} // AjaxHandler

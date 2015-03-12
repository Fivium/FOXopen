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

import java.util.ArrayList;
import java.util.HashMap;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.spatial.SpatialEngine;
import net.foxopen.fox.spatial.SpatialRenderingResult;


/**
 * Handles Ajax requests instigated from the cartographic widget,
 * interfacing with the spatial engine.
 */
public class AjaxHandlerSpatial extends AjaxHandler {

  public static final String DEFAULT_CHANGE_NUMBER = "-1";
  SpatialEngine mOwnerSpatialEngine;

  public AjaxHandlerSpatial (SpatialEngine pSpatialEngine) {
    super("spatial-" + pSpatialEngine.getAppMnem());
    mOwnerSpatialEngine = pSpatialEngine;
  } // AjaxHandlerSpatial

  /**
   * Interpret the Ajax request and prepare a response DOM to send back.
   * @param pFoxRequest the original request
   * @param pRequestDOM the parsed DOM from the request
   * @return response as a DOM
   * @throws ExInternal
   */
  public DOM getAjaxResponse (FoxRequest pFoxRequest, DOM pRequestDOM, Object pOptionalXThread)
  throws ExInternal {

    try {
      String lDataIdentifier = pRequestDOM.get1S("./data-identifier");
      DOM lElementData = pRequestDOM.get1E("./data");

      // Build up spatial rendering request based on data provided
      DOM lSpatialData = DOM.createDocument("spatial-data");
      lElementData.getUL("./*").copyContentsTo(lSpatialData);

      // Get request width and height for rendering purposes
      String lHtmlElementId = pRequestDOM.get1S("./html-element-data/element-id");

      // Get current change number
      String lChangeNumber = lElementData.get1S("./change-number");

      String lReqWidth;
      String lReqHeight;

      // We have to look at both sets of attributes here to work around problems with
      // clientWidth and clientHeight being 0 for some reason when the element is relatively
      // positioned in IE6/7 - this only ever happens after the first response is sent, so we
      // can branch on the change number
      if (!lChangeNumber.equals(DEFAULT_CHANGE_NUMBER)) {
        lReqWidth = pRequestDOM.xpath1S("./html-element-data/html-attribute-list/attribute[name='width']/value");
        lReqHeight = pRequestDOM.xpath1S("./html-element-data/html-attribute-list/attribute[name='height']/value");
      }
      else {
        lReqWidth = pRequestDOM.xpath1S("./html-element-data/html-attribute-list/attribute[name='clientWidth']/value");
        lReqHeight = pRequestDOM.xpath1S("./html-element-data/html-attribute-list/attribute[name='clientHeight']/value");
      }

      // Add image size to request
      lSpatialData.addElem("image-width", lReqWidth);
      lSpatialData.addElem("image-height", lReqHeight);

      // Run spatial events (image height and width may be a factor, so above
      // processing is necessary)
      String lEventLabel = pRequestDOM.get1SNoEx("./event-label");

      // Update lSpatialData with result of the spatial operation,
      // if we find an event label
      // NB: Happens whether in Ajax or in a FOX thread
      if (!XFUtil.isNull(lEventLabel)) {
        lSpatialData = mOwnerSpatialEngine.performSpatialOperation(
          lEventLabel
        , lSpatialData
        , pFoxRequest
        );
      }

      // Otherwise just rebootstrap the canvas to get the latest data
      // NB: This is called from WidgetBuilder, so if we're in a FOX
      // thread we don't need to worry about this
      else if (pOptionalXThread == null) {
        lSpatialData = mOwnerSpatialEngine.refreshSpatialCanvas(
          lSpatialData
        , pFoxRequest
        );
      }

      // If we're in a standard FOX XThread, we don't want to return a response
      if (pOptionalXThread != null) {
        return null;
      }
      // We're in Ajax if XThread is null, so we need to send back
      // an Ajax response telling the client what to do (i.e. refresh
      // the image, set up a user interaction mode
      else {
        // Get the change number
        String lNewChangeNumber = null;
        try {
          lNewChangeNumber = lSpatialData.get1S("./change-number");
        }
        catch (ExCardinality ex) {
          throw new ExInternal("Change number not found", ex);
        }

        // Change number has incremented, need to send back a response
        if (!lNewChangeNumber.equals(lChangeNumber)) {

          // Check to see if change number has been forced to default
          // If not, this isn't the first time in and it's reasonable to
          // ask other widgets to check for updates
          if (!lChangeNumber.equals(DEFAULT_CHANGE_NUMBER)) {
            setUpdateCheckRequired(pFoxRequest);
          }

          // Prepare the response
          DOM lOutgoingSpatialData = DOM.createDocument("spatial-canvas");
          lSpatialData.copyContentsTo(lOutgoingSpatialData);

          // Build up data package for the widget with which we are
          // currently liasing
          // N.B: Prunes out nodes that shouldn't be sent to client
          DOM lResponseDOM = buildDataPackageDOM (
            lDataIdentifier
          , lOutgoingSpatialData
          );

          String lAction = lSpatialData.get1S("./callback-action");
          if (!XFUtil.isNull(lAction)) {
            DOM lCommandList = lResponseDOM.getCreate1ENoCardinalityEx("./command-list");
            DOM lCommand = lCommandList.addElem("command").addElem("run-action");
            lCommand.addElem("action-name", lAction);
          }

          // Render the image and get the url/size
          SpatialRenderingResult lSpatialRR = mOwnerSpatialEngine.getCanvasRendering(lSpatialData, pFoxRequest);

          String lCanvasId = lHtmlElementId + "_c" + lChangeNumber;

          // First time in only - swap the wrapping table for a div
          if (lChangeNumber.equals(DEFAULT_CHANGE_NUMBER)) {
            // Register attributes to alter on the client
            HashMap lDivAttrsMap = new HashMap();
            lDivAttrsMap.put("id", lHtmlElementId);
            lDivAttrsMap.put("style", "width: " + lReqWidth + "px; height: " + lReqHeight + "; " + "position:relative; display: block; overflow: hidden; background-color: white; color: #555555; border: 1px solid #aaaaaa; text-align: center; padding: 0;");
            lDivAttrsMap.put("width", lReqWidth);
            lDivAttrsMap.put("height", lReqHeight);

            replaceElementCommand(
              lResponseDOM
            , lHtmlElementId
            , "div"
            , lDivAttrsMap
            );
          }

           // Register attributes to alter on the client
          HashMap lImgAttrsMap = new HashMap();
          lImgAttrsMap.put("id", lCanvasId);
          lImgAttrsMap.put("src", lSpatialRR.getURI());

          replaceChildElementsCommand(
            lResponseDOM
          , lHtmlElementId
          , "img"
          , lImgAttrsMap
          );

          // If spatial pl/sql returned behaviour XML, behave in that way
          DOM lBehaviourDOM = lSpatialData.get1EOrNull("/*/canvas-behaviour");
          if (lBehaviourDOM == null) {
            throw new ExInternal("Expected exactly one canvas-behaviour node");
          }

          // TODO: Do something about ensuring the order of execution client-side:
          // Replace html after setClickCapture causes loss of functionality
          setCanvasBehaviour(lResponseDOM, lCanvasId, lBehaviourDOM);

          return lResponseDOM;
        }
        else {
          return null;
        }
      }
    }

    catch (ExTooFew ex) {
      throw new ExInternal("Cardinality error", ex);
    }
    catch (ExTooMany ex) {
      throw new ExInternal("Cardinality error", ex);
    }
    catch (ExBadPath ex) {
      throw new ExInternal("Bad XPath in AjaxHandlerSpatial", ex);
    }
  } // getAjaxResponses

  /**
   * Convenience method to build up a data package (defaulting priority to low).
   * @param pDataIdentifier the identifier of the widget
   * @param pData the data to store against the widget
   * @return DOM instantiation of data package
   */
  private DOM buildDataPackageDOM (String pDataIdentifier, DOM pData) {
    return buildDataPackage(
      pDataIdentifier
    , XFUtil.nvl(pData.get1SNoEx("service-priority"), "LOW")
    , pData.pruneDocumentXPath("/*/*[name(.)='canvas-usage-id' or name(.)='canvas-id' or name(.)='canvas-hash' or name(.)='coord-list' or name(.)='change-number']").removeRefsRecursive()
    );
  } // buildDataPackageDOM

  /**
   * Registers the element in question with a HtmlGenerator, which in turn
   * causes it to be registered with the client-side synchroniser.
   * @param pHtmlGenerator the HtmlGenerator instance to target
   * @param pHtmlElementId the HTML element id that has been written out
   * @param pData the data to register against the HTML element
   */
  public void registerHtmlElementWithSynchroniser (/*HtmlGenerator pHtmlGenerator,*/ String pHtmlElementId, DOM pData) {

    // Derive service priority
    String lServicePriority = XFUtil.nvl(pData.get1SNoEx("service-priority"), "LOW");

    // Fudge the outgoing change number to always force an initial rendering
    try {
      pData.get1E("change-number").setText(DEFAULT_CHANGE_NUMBER);
    }
    catch (ExCardinality ex) {
      throw new ExInternal("Failed to fudge change number in outgoing canvas metadata", ex);
    }

    // Build up a package of data to send to client with HTML
    DOM lDataPackage = buildDataPackageDOM(pHtmlElementId, pData);

    // Set up list of attributes for the client to send back with any requests it makes
    // For spatial, we're almost always going to want the width and height of the canvas
    ArrayList lAttrsToWatch = new ArrayList();
    lAttrsToWatch.add("width");
    lAttrsToWatch.add("height");
    lAttrsToWatch.add("clientWidth");
    lAttrsToWatch.add("clientHeight");

    // Add "register-html-element" command to outgoing data-package
    registerHtmlElementCommand(lDataPackage, pHtmlElementId, lAttrsToWatch);

    // Add "add-to-service-queue" command to outgoing data-package
    addToServiceQueueCommand(lDataPackage, lServicePriority);

    // Pass on to HtmlGenerator
    initPackageOnClient(/*pHtmlGenerator,*/ pHtmlElementId, lDataPackage);
  } // registerHtmlElementWithSynchroniser

  /**
   * Writes a change in behaviour (i.e. click capture, etc) to the response DOM
   * for the current spatial canvas.
   * @param pResponseDOM the response DOM to write out to
   * @param pHtmlElementId the HTML element id in question
   * @param pBehaviourDOM the new behaviour description as a DOM fragment
   */
  private void setCanvasBehaviour (DOM pResponseDOM, String pHtmlElementId, DOM pBehaviourDOM)
  {
    String lZoomPctStr = pBehaviourDOM.get1SNoEx("zoom-pct");

    // Standard zoom/pan controls
    boolean lMouseZoom = "true".equals(pBehaviourDOM.get1SNoEx("mouse-zoom"));
    boolean lMousePan = "true".equals(pBehaviourDOM.get1SNoEx("mouse-pan"));
    boolean lZoomButtons = "true".equals(pBehaviourDOM.get1SNoEx("zoom-buttons"));
    boolean lPanButtons = "true".equals(pBehaviourDOM.get1SNoEx("pan-buttons"));
    boolean lAnimation = true;

    // Digitising controls
    DOM lDigitisingDOM = pBehaviourDOM.get1EOrNull("digitising-parameters");
    DOM lClickCaptureDOM = pBehaviourDOM.get1EOrNull("click-capture-parameters");

    // Client-side code is not ready to cope with the animations or
    // processing for mouse movements if digitising is turned on, so force
    // disable it for now
    if (lDigitisingDOM != null || lClickCaptureDOM != null) {
      lMouseZoom = false;
      lMousePan = false;
      lAnimation = false;
    }

    int lZoomPct = 0;

    try {
      lZoomPct = Math.max(Integer.parseInt(lZoomPctStr), 0);
    }
    catch (NumberFormatException ex) {
      // Do nothing
    }

    if (lMousePan || lPanButtons) {
      setImagePanModeCommand(
        pResponseDOM          // pDataPackage
      , pHtmlElementId        // pHtmlElementId
      , "!CENTROID-ZOOM"      // pPanEventLabel
      , "!MAP-EXTENT-INITIAL" // pResetEventLabel
      , lMousePan             // pEnableMouse
      , lPanButtons           // pEnableButtons
      , lAnimation            // pEnableAnimation
      );
    }

    if (lZoomPct != 0 && (lMouseZoom || lZoomButtons)) {
      setImageZoomModeCommand(
        pResponseDOM     // pDataPackage
      , pHtmlElementId   // pHtmlElementId
      , lZoomPct         // pZoomPct
      , "!CENTROID-ZOOM" // pEventLabelToRaise
      , lMouseZoom       // pEnableMouse
      , lZoomButtons     // pEnableButtons
      , lAnimation       // pEnableAnimation
      );
    }


    if (lDigitisingDOM != null) {
      boolean lPointInput = "true".equals(lDigitisingDOM.get1SNoEx("point-input"));
      boolean lLineInput = "true".equals(lDigitisingDOM.get1SNoEx("line-input"));
      boolean lPolygonInput = "true".equals(lDigitisingDOM.get1SNoEx("polygon-input"));
      boolean lPreventDiagonals = "true".equals(lDigitisingDOM.get1SNoEx("prevent-diagonals"));

      DOMList lSnapPoints = lDigitisingDOM.getUL("./snap-grid/coord");

      setCanvasDrawCommand(
        pResponseDOM      // pDataPackage
      , pHtmlElementId    // pHtmlElementId
      , "!DIGITISING"     // pEventLabelToRaise
      , lPointInput       // pPointInput
      , lLineInput        // pLineInput
      , lLineInput        // pConnectedLineInput
      , lPolygonInput     // pPolygonInput
      , lSnapPoints       // pSnapPointDOMList
      , lPreventDiagonals // pPreventDiagonals
      );
    }
    else if (lClickCaptureDOM != null) {
      String lClicks = XFUtil.nvl(lClickCaptureDOM.get1SNoEx("clicks"),"1");
      String lCursor = XFUtil.nvl(lClickCaptureDOM.get1SNoEx("cursor"),"pointer");

      setClickCaptureModeCommand(
        pResponseDOM
      , pHtmlElementId
      , Integer.valueOf(lClicks).intValue()
      , "!CLICK-CAPTURE"
      , lCursor
      );
    }

  } // setCanvasBehaviour

} // AjaxHandlerSpatial

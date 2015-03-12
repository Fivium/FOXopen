/**
 * Synchronises data from a controlling Ajax engine and updates the
 * HTML DOM where necessary, including assignment of event handlers.
 */
function Synchroniser () {
  // Enforce singleton existance per-page
  if (Synchroniser.prototype.instantiated) {
    throw "Synchroniser already instantiated.";
  }
  Synchroniser.prototype.instantiated = true;
}

// Class body
Synchroniser.prototype = {

  // Member variables
  _dataMap: null
, _cookiePoller: null
, _requestPending: false
, _handlerURL: null
, _interval: null
, _polling: false
, _initialised: false
, _serviceQueue: null
, _throttleBlockingAjax: false
, _htmlAttrsMap: null
, _dataIdToHtmlIdMap: null
, _dataInputElement: null
, _dataIdToCleanupMap: null
, _pollTimeout: null
, _pollIntervalMs: null
, _errorIntervalMS: null
, _targetDOMNodes: null
, _postProcessFunctions: null
, _postReplacementCallbacks: null
, _dataPackageLocks: null
, _foxCanvasMap: null
, _foxCanvasEventLabelMap: null

  // "Constants"
, AJAX_REQUEST_THROTTLE_MS: 50
, SERVICE_QUEUE_POLLING_MS: 50
, COOKIE_POLLING_MS: 500
, PRIORITY_HIGH: "HIGH"
, PRIORITY_MEDIUM: "MEDIUM"
, PRIORITY_LOW: "LOW"
, ELEMENT_NODE: 1
, ATTRIBUTE_NODE: 2
, TEXT_NODE: 3
, CDATA_SECTION_NODE: 4
, ENTITY_REFERENCE_NODE: 5
, ENTITY_NODE: 6
, PROCESSING_INSTRUCTION_NODE: 7
, COMMENT_NODE: 8
, DOCUMENT_NODE: 9
, DOCUMENT_TYPE_NODE: 10
, DOCUMENT_FRAGMENT_NODE: 11
, NOTATION_NODE: 12

  /**
   * Initialises the Synchroniser.
   * @param aHandlerURL the URL of the Ajax handling server
   * @param aDataInputId the id of the HTML element containing the source data
   */
, init: function (aHandlerURL, aDataInputId, aPollIntervalMs) {
    
    // Store data in id -> data associative array
    this._dataMap = new Array();
    
    // HTML id to attribute list mapping
    this._htmlAttrsMap = new Array();
    this._dataIdToHtmlIdMap = new Array();
    this._dataIdToCleanupMap = new Array();
    this._targetDOMNodes = new Array();
    this._postPackageFunctions = new Array();
    this._postReplacementCallbacks = new Array();
    this._dataPackageLocks = new Array();
    this._foxCanvasMap = new Array();
    this._foxCanvasEventLabelMap = new Array();
    this._noAjax = new Array();
    
    // Validate URL
    if (!aHandlerURL) {
      throw "Must pass Ajax handler URL to Synchroniser.init()";
    }
    
    // Get input and validate
    var lInputElement = this._getElementById(aDataInputId);
    if (!lInputElement) {
      throw "Could not find input element with id='"+aDataInputId+"'";
    }
    
    // Poll defaults to 60 seconds
    this._pollIntervalMs = 60 * 1000;
    if (aPollIntervalMs) {
      this._pollIntervalMs = aPollIntervalMs;
    }
    
    // 240 seconds if we error
    this._errorIntervalMS = 240 * 1000;
    
    this._handlerURL = aHandlerURL;
    this._dataInputElement = lInputElement;
    
    // Start up internal pollers
    this._run();
    
    // Parse DOM from data source
    var lDOM = FoxDOM.parseDOMFromString(this._dataInputElement.value);
    this._processXMLDataInternal(lDOM);
  }
  
  /**
   * Initially registers or replaces the data that Synchroniser holds for a 
   * given data-identifier. Additionally will run any commands that are part
   * of the package.
   * @param aDOM the data-package to process
   */
, _registerOrUpdateFromDOM: function (aDOM) 
  {    
    // 1. Switch context from Document node to root element node
    //    (allows for relative XPath evaluation and reuse of this function)
    var lContextNode = aDOM.ownerDocument == null ? aDOM.documentElement : aDOM;
    
    // 2. Retrieve and valdiate composite key values
    var lDataIdentifier = FoxDOM.get1SByXPath(lContextNode, "./data-identifier/text()");
    var lHandlerMnem = FoxDOM.get1SByXPath(lContextNode, "./handler-mnem/text()");

    // 3. Store DOM against identifier in case any commands reference the data
    this._dataMap[lDataIdentifier] = aDOM;
    
    // 4. Empty out the post-processing array for this identifier
    this._postPackageFunctions[lDataIdentifier] = new Array();
    
    // 5. Run commands in data package 
    var lCommandListNode = FoxDOM.getNodesByXPath(lContextNode, "./command-list")[0];
    if (lCommandListNode != null) {
      var lCommandListChildNodes = FoxDOM.getNodesByXPath(lCommandListNode, "./command");
      this._processCommandList(lDataIdentifier, lCommandListChildNodes);
    }
    
    // 6. Call any applicable post processing functions
    for (var n in this._postPackageFunctions[lDataIdentifier]) {
      this._postPackageFunctions[lDataIdentifier][n].call(this);
    }
    
    // 7. Pull command list out of the DOM once processed
    if (lCommandListNode != null) {
      lContextNode.removeChild(lCommandListNode);
    }
    
    // 8. Put data back in the map after commands have been processed and removed so
    //    that any listening actions have the most up-to-date data to work on
    this._dataMap[lDataIdentifier] = aDOM;
    
    // 9. Remove any locking we had on this identifier
    this._dataPackageLocks[lDataIdentifier] = false;
  }
  
  /**
   * Initialises and starts up the polling/cookie polling process.
   */
, _run: function () {
    // Only init/run once
    if (this._initialised) {
      return;
    }
    
    // Self reference for closures
    var self = this;

    // Construct service queue with callback
    this._serviceQueue = new ServiceQueue(
      [this.PRIORITY_HIGH, this.PRIORITY_MEDIUM, this.PRIORITY_LOW]
    , this.SERVICE_QUEUE_POLLING_MS
    , function (aId, aDOM) {
        return self._sendPackageAsAjax(aId, aDOM, false);
      }
    );
    
    // Set up cookie poller, callback is run on change
    this._cookiePoller = new CookiePoller(
      "fox-heartbeat"
    , this.COOKIE_POLLING_MS
    , function () {
        // Send all data
        self._sendAllPackagesAsAjax();
      }
    );
    
    // Set up Ajax poller
    this._pollTimeout = window.setTimeout(function () {
      self._sendAllPackagesAsAjax();
    }, this._pollIntervalMs);
    
    // Start polling
    this.startPollers();
    
    // Don't init again
    this._initialised = true;
  }
  
  /**
   * Starts the polling processes.
   */
, startPollers: function () {
    this._serviceQueue.startPolling();
    this._cookiePoller.startPolling();
  }
  
  /**
   * Stops the polling processes.
   */
, stopPollers: function () {
    this._cookiePoller.stopPolling();
    this._serviceQueue.stopPolling();
  }
  
  /**
   * Callback executed when a ServiceQueue item is serviced.
   * @param aId the data-identifier referenced by the callback, for convenience
   * @param aDOM the data-package to process
   * @param aForceConnection bypass any restriction on number of connections
   * @return boolean {success = true, blocked = false}
   */
, _sendPackageAsAjax: function (aId, aDOM, aForceConnection) {
    // Lock the data package while we wait for a response
    this._dataPackageLocks[aId] = true;

    // Build request DOM with single <data-package/>
    var lReqDOM = this._buildReqDOM(
      [aDOM] // DOM list
    , true   // Merge HTML Attrs
    , true   // Force send any/all packages present
    );
   
    // Serialize to string
    var lXMLAsString = FoxDOM.serializeDOMToString(lReqDOM);
    
    // Attempt to send Ajax request (could be blocked so return
    // result to caller)   
    return this._sendAjaxRequest(lXMLAsString, aForceConnection);
  }
  
  /**
   * Sends all data immediately.
   */
, _sendAllPackagesAsAjax: function () {
    // Reset the timeout
    window.clearTimeout(this._pollTimeout);
    
    // Self ref for closure
    var self = this;
    
    var lReqBody = this.getAjaxDataAsXMLString();
    if (lReqBody == false) {
      return;
    }
      
    // Send!
    var lRequestSent = this._sendAjaxRequest(
      lReqBody
    , true // force connection
    );

    // Reset poller
    this._pollTimeout = window.setTimeout(function () {
      self._sendAllPackagesAsAjax();
    }, this._pollIntervalMs);
    
    // Check
    if (!lRequestSent) {
      throw "Synchroniser._sendAllPackagesAsAjax() failed to make a request";
    }
  }
  
  /**
   * Merges HTML attributes into a <data-package/>, for purposes of
   * notifying the server as part of the request. Server may act
   * on this data and return a different sized image, for example.
   * @param aId the data-identifier on which we are acting
   * @param aDOM the data-package DOM to merge attributes into
   * @return data-package DOM with merged HTML attributes
   */
, _mergeHtmlAttributes: function (aDOM) {
    // Take a local copy and merge in required HTML attrs
    var lOwnerDocument = aDOM.ownerDocument ? aDOM.ownerDocument : aDOM;
    var lDOM = aDOM.cloneNode(true);   
    var lDataId = FoxDOM.get1SByXPath(aDOM, "./data-identifier/text()");
    
    // Resolve stored id mapping and attrs required
    var lHtmlId = this._dataIdToHtmlIdMap[lDataId];
    var lHtmlAttrsRequired = this._htmlAttrsMap[lHtmlId];
    var lHtmlElem = this._getElementById(lHtmlId);
    
    // Belt and braces
    if (lHtmlElem == null) {
      throw "Could not locate HTML element with id '" + lHtmlId + "' in Synchroniser._mergeHtmlAttributes";
    }
  
    // Using createElement instead of createDocumentFragment for IE5.5 support
    var lDocFragment = document.createElement("html-element-data");
    
    // If we have a list of required attributes, tack this onto the package
    if (lHtmlAttrsRequired != null && lHtmlAttrsRequired.length > 0) {
      var lHtmlElementDataElem = lDOM.appendChild(lOwnerDocument.createElement("html-element-data"));     
      FoxDOM.appendChildToDOM(lHtmlElementDataElem, "element-id", lHtmlId);
      
      var lAttrList = lHtmlElementDataElem.appendChild(lOwnerDocument.createElement("html-attribute-list"));
      for (var n in lHtmlAttrsRequired) {
        // Create container element
        var lHtmlAttrElem = lAttrList.appendChild(lOwnerDocument.createElement("attribute"));
        
        // Add name
        FoxDOM.appendChildToDOM(lHtmlAttrElem, "name", lHtmlAttrsRequired[n]);
        
        // Get value and append        
        var lAttrValue = this._getAttributeExhaustive(lHtmlElem, lHtmlAttrsRequired[n]);
        if (this.getObjectIndexInArray(
          lHtmlAttrsRequired[n]
        , ["width","height","clientWidth","clientHeight","offsetTop","offsetLeft"]
        ) != -1 && lAttrValue != null) {
        
          // Recast as string
          lAttrValue = "" + lAttrValue;
          
          // Trim "px" from measurements if name is in this array
          lAttrValue = lAttrValue.replace("px","");
        }
        
        FoxDOM.appendChildToDOM(lHtmlAttrElem, "value", lAttrValue);
        
        // Add attr to list
        lAttrList.appendChild(lHtmlAttrElem);
      }
    }
    return lDOM;
  }
 
  /**
   * Tries to get an attribute value in various ways (attempting to cope
   * with HTML DOM attribute quirks and cross-browser discrepencies).
   * @param aHtmlElement the HTML element to obtain the attribute value from
   * @param aAttrName the attribute to seek
   * @return the attribute value as a string, or null
   */
, _getAttributeExhaustive: function (aHtmlElement, aAttrName) {
    // 1. Start off null
    var lValue = null;
    
    // 2. Attempt a standard getAttribute call
    lValue = aHtmlElement.getAttribute(aAttrName);
    if (lValue != null && lValue != undefined) {
      return lValue;
    }

    // 3. Try to get value from elem directly 
    try {
      var lEvalString = "lValue = aHtmlElement." + aAttrName + ";";
      eval(lEvalString);
    }
    catch (e) {
      // do nothing
    }
    if (lValue != null && lValue != undefined) {
      return lValue;
    }
    
    // 4. Try to get the value from style
    try {
      var lEvalString = "lValue = aHtmlElement.style." + aAttrName + ";";
      eval(lEvalString);
    }
    catch (e) {
      // do nothing
    }
    if (lValue != null && lValue != undefined) {
      return lValue;
    }
    
    // 5. Nothing found, time wasted, return null
    return null;
  }
  
  /**
   * Sends an Ajax request to the handler.
   * @param aXMLAsString the message body to send
   * @param aForceConnection whether or not this call should force a connection
   * @return boolean indicator of whether the request was sent or not
   */
, _sendAjaxRequest: function (aMsgBody, aForceConnection) {
    // Don't send another request until the pending request is complete
    // however, allow concurrent connections if we're not running a callback
    // (i.e. don't hold up user interaction)
    if (!aForceConnection && (this._requestPending || !this._initialised)) {
      // Indicate to caller that request was not sent
      return false;
    }
    
    // References for closure
    var self = this;
    var lAjaxRequest;
    
    // Build request
    lAjaxRequest = new AjaxRequest (
      this._handlerURL        // aURL
    , "POST"                  // aMethod
    , ""                      // aParams
    , aMsgBody                // aMsgBody
    , "text/xml"              // aContentType
    , function (aData) {      // aCallbackFunction
        self._handleAjaxResponse(aData);        
      }
    );

    // If desired, throttle request intervals to maximum of 1
    // every AJAX_REQUEST_THROTTLE_MS milliseconds
    if (this._throttleBlockingAjax == true && !aForceConnection) {
      // Indicate to caller that request wasn't sent
      return false;
    }
    
    if (this.AJAX_REQUEST_THROTTLE_MS > 0) {    
      this._throttleBlockingAjax = true;
      window.setTimeout(
        function (){
          self._throttleBlockingAjax = false;
        }
        , this.AJAX_REQUEST_THROTTLE_MS
      );
    }
    
    // Block additional requests that aren't forced
    this._requestPending = true;
    
    // Send!
    lAjaxRequest.sendRequest();
    
    // Indicate to caller that request was sent
    return true;
  }

  /**
   * Handles the Ajax response and processes it via the standard load routines.
   * @param aAjaxResponse arbitrary object containing the following fields: 
   *        {statusCode, statusMsg, textData, xmlData, responseOk}
   */
, _handleAjaxResponse: function (aAjaxResponse) {
    // Permit further requests
    this._requestPending = false;
    // aAjaxResponse: {statusCode, statusMsg, textData, xmlData, responseOk}
    if (aAjaxResponse.responseOk && aAjaxResponse.xmlData != null) {
      
      // Check for error
      var lErrors = FoxDOM.getNodesByXPath(aAjaxResponse.xmlData, "/*/error[@severity!='warning']");
      if (lErrors[0] != null) {
        // Stop polling, alert out error
        this._alertError(aAjaxResponse);
        
        // When error is dismissed, alter timeout and start polling again 
        // (extends time between alerts if this doesn't recover)
        this._pollIntervalMs = this._errorIntervalMS;
        
        // Break out, nothing further to do here
        // Avoid wasting time evaluating following XPath
        return;
      }
      
      // Pass DOM to internal handler
      this._processXMLDataInternal(aAjaxResponse.xmlData);
    }
    
    // FOX has thrown an Internal Server Error - should not happen (exceptions should be wrapped as XML)
    // so something extremely core has gone wrong, i.e. FOX failed to boot, connect to DB, etc.
    else if (aAjaxResponse.statusCode == 500) {
      // Stop polling, alert out error
      this._stopAjaxPolling();
      this._alertError(aAjaxResponse);
      
      // When error is dismissed, alter timeout and start polling again 
      // (extends time between alerts if this doesn't recover)
      this._pollIntervalMs = this._errorIntervalMS;
      this._startAjaxPolling();
    }
    
    // HTTP 200 - OK, but no body
    else if (aAjaxResponse.responseOk && aAjaxResponse.xmlData == null) {
      // Do nothing
    }
    
    // Networking error, AjaxRequest returns -99 when unknown or network error occurs
    // and no data to describe the error is readily available
    else if (aAjaxResponse.statusCode == -99) {
      // Do nothing
    }
    
    // Something happened that we weren't expecting, throw
    else {
      throw "Ajax request failed, HTTP Status: " + aAjaxResponse.statusCode
        + "\n, Message: " + aAjaxResponse.statusMsg;
    }
  }
  
  /**
  * Splits out <data-package/> nodes from an ajax data set 
  * with a <data-package-list/> and processes each individually.
  * @param aXMLData the dataset to process as a DOM
  */
, _processXMLDataInternal: function (aXMLData) {
    // Get responses and branch based on request type
    var lNodesToProcess = FoxDOM.getNodesByXPath(aXMLData, "/*/data-package-list/data-package");
    
    if (lNodesToProcess != null && lNodesToProcess.length > 0) {
      for (var i = 0; i < lNodesToProcess.length; i++) {
        this._registerOrUpdateFromDOM(lNodesToProcess[i]);
      }
    }
  }
  
  /**
   * Processes an array of command nodes.
   * @param aDataIdentifier the identifier of the package being processed
   * @param aCommandList array of <command/> nodes
   */
, _processCommandList: function (aDataIdentifier, aCommandList) {
    // Belt & braces
    if (aCommandList == null || aCommandList.length == 0) {
      throw "Null or empty list of commands passed to Synchroniser._processCommandList";
    }
    // Step through command nodes
    // Note: for (var n in aCommandList) passes unwanted child elements
    for (var i = 0; i < aCommandList.length; i++) {
      var lCommand = aCommandList[i];
      var lHtmlId;
      
      try {
        lHtmlId = FoxDOM.get1SByXPath(lCommand, "./*/element-id/text()");
      }
      catch (e) {}
      
      // If we have no HTML id, or we have one and it exists in the DOM, process immediately
      if (lHtmlId == null || document.getElementById(lHtmlId)) {
        this._processCommand(aDataIdentifier, lCommand);
      }
      
      // Otherwise if we're queued up to insert the element, put this 
      // on the post-replacement callback queue
      else if (lHtmlId != null && this._getElementById(lHtmlId)) {
        this._addPostReplacementCallback(lHtmlId, $.proxy(        
          (function (aCommand) {
            return function () {
              this._processCommand(aDataIdentifier, aCommand)
            }
          })(lCommand)
        , this)
        );
      }
      else {
        throw "HTML identifier was provided in a command, but could not be accounted for: " + lHtmlId;
      }
    }    
  }
  
  /**
   * Processes an individual <command/> node.
   * @param aDataIdentifier the identifier of the package being processed
   * @param aCommandNodeWrapper a <command/> node wrapping a command element.
   */
, _processCommand: function (aDataIdentifier, aCommandNodeWrapper) {
    // Validate input
    if (aCommandNodeWrapper == null || aCommandNodeWrapper.tagName != "command") {
      throw "Invalid parameter passed to Synchroniser._processCommand. Expected 'command' node, found '"
        + aCommandNodeWrapper == null ? "null" : aCommandNodeWrapper.tagName + "'";
    }
    // Pull out the child node and branch based on node name
    try {
      var lCommandNode = FoxDOM.getNodesByXPath(aCommandNodeWrapper, "./*[1]")[0];
      switch (lCommandNode.tagName) {
        case "alert": {
          this._alert(lCommandNode);
          break;
        }
        case "run-action": {
          var lText;
          try {
            lText = FoxDOM.get1SByXPath(lCommandNode, "./action-name/text()");
            ss(lText);
          }
          catch (e) {
            alert("Failed...");
          }
          break;
        }
        case "set-element-attribute": {
          this._setElementAttribute(lCommandNode);
          break;
        }
        case "set-background-image": {
          this._setBackgroundImage(lCommandNode);
          break;
        }
        case "replace-element": {
          this._replaceElement(lCommandNode, aDataIdentifier);
          break;
        }
        case "replace-child-elements": {
          this._replaceChildElements(lCommandNode, aDataIdentifier);
          break;
        }
        case "register-html-element": {
          this._registerHtmlElement(lCommandNode, aDataIdentifier);
          break;
        }
        case "add-to-service-queue": {
          this._addToServiceQueue(lCommandNode, aDataIdentifier);
          break;
        }
        case "set-click-capture-mode": {
          this._setClickCaptureMode(lCommandNode, aDataIdentifier);
          break;
        }
        case "set-image-zoom-mode": {
          this._setImageZoomMode(lCommandNode, aDataIdentifier);
          break;
        } 
        case "set-image-pan-mode": {
          this._setImagePanMode(lCommandNode, aDataIdentifier);
          break;
        }
        case "set-canvas-draw-mode": {
          this._setCanvasDrawMode(lCommandNode, aDataIdentifier);
          break;
        }
        default: {
          throw "Invalid command, '" + lCommandNode.nodeName + "'";
        }
      }
    }
    catch (e) {
      throw "Error processing response node " + lCommandNode.nodeName + "\n" + e;
    }
  }
  
  /**
  * Alerts out a message.
  * @param aCommandNode the <alert/> node to process
  */
, _alert: function (aCommandNode) {
    var lText = FoxDOM.get1SByXPath(aCommandNode, "./message/text()");
    if (lText == null || lText == "") {
      throw "Invalid alert message - null or empty string found";
    }
    alert(lText);
  }
  
  /**
   * Sets the background image of an element and optionally removes its contents.
   * @param aCommandNode the <set-background-image> node to process
   */
, _setBackgroundImage: function (aCommandNode) {
     
    var lHtmlId = FoxDOM.get1SByXPath(aCommandNode, "./element-id/text()");
    var lImageSrc = FoxDOM.get1SByXPath(aCommandNode, "./image-url/text()");
    
    var lHtmlElem = this._getElementById(lHtmlId);
    $(lHtmlElem).css("background-image","url(" + lImageSrc + ")"); 
    if ("true" == FoxDOM.get1SByXPath(aCommandNode, "./remove-contents/text()")) {
      $(lHtmlElem).empty();
    }
  }
    
  /**
   * Replaces an HTML element.
   * @param aCommandNode the <replace-element/> node to process
   */
, _replaceElement: function (aCommandNode, aDataIdentifier) {
    // Find target to replace and check it exists
    var lOldElemId = FoxDOM.get1SByXPath(aCommandNode, "./element-id/text()");
    var lOldElem = this._getElementById(lOldElemId);
    
    // Belt and braces
    if (lOldElem == null) {
      throw "Could not locate element with id '" + lOldElemId + "' in Synchroniser._replaceElement";
    }
    
    var lParentElem = lOldElem.parentNode;
    
    // Get element name to create and attrs
    var lNewElemName = FoxDOM.get1SByXPath(aCommandNode, "./element-type/text()");
    
    // Create a new image object
    var lNewElem = document.createElement(lNewElemName);
    
    // Copy over key attributes
    // TODO: Allow replacement in more intelligent way
    lNewElem.setAttribute("id", lOldElem.getAttribute("id"));
    lNewElem.setAttribute("style", lOldElem.getAttribute("style"));
    lNewElem.setAttribute("width", lOldElem.clientWidth);
    lNewElem.setAttribute("height", lOldElem.clientHeight);

    var lDeferReplacement = false;
    
    // Set new attrs
    var lAppendAttrsList = FoxDOM.getNodesByXPath(aCommandNode, "./append-attribute-list/append-attribute");
    for (var n = 0; n < lAppendAttrsList.length; n++) {
      var lAttrName = FoxDOM.get1SByXPath(lAppendAttrsList[n], "./name/text()");
      var lAttrValue = FoxDOM.get1SByXPath(lAppendAttrsList[n], "./value/text()");
      if (lNewElemName == "img" && lAttrName == "src") {
        lDeferReplacement = true;
        $(lNewElem).bind("load", $.proxy(function () {
          lNewElem = lParentElem.replaceChild(lNewElem, lOldElem);
          var lElemId = lNewElem.getAttribute("id");
          if (lElemId) {
            this._targetDOMNodes[lElemId] = lNewElem;
          }
          setTimeout($.proxy(function () {
            this._invokePostReplacementCallbacks(lHtmlId);
          }, this), 0);
        }, this));
      }
      lNewElem.setAttribute(lAttrName, lAttrValue);
      // For IE6/7
      if (lAttrName == "style") {
        lNewElem.style.cssText = lAttrValue;
      }
    }
    
    // Make the replacement, if not deferred
    if (!lDeferReplacement) {
      lNewElem = lParentElem.replaceChild(lNewElem, lOldElem);
    }
    
    var lElemId = lNewElem.getAttribute("id");
    if (lElemId) {
      this._targetDOMNodes[lElemId] = lNewElem;
    }
  }
  
  /**
   * Replaces all child elements of a chosen parent with a new element.
   */
, _replaceChildElements: function (aCommandNode, aDataIdentifier) {
    // Find target to replace and check it exists
    var lParentElemId = FoxDOM.get1SByXPath(aCommandNode, "./element-id/text()");
    var lParentElem = this._getElementById(lParentElemId);
    
    // Belt and braces
    if (lParentElem == null) {
      throw "Could not locate element with id '" + lOldElemId + "' in Synchroniser._replaceChildElements";
    }
    
    // Get element name to create and attrs
    var lNewElemName = FoxDOM.get1SByXPath(aCommandNode, "./element-type/text()");
    
    // Create a new element
    var lNewElem = document.createElement(lNewElemName);

    // Copy over key attributes
    lNewElem.setAttribute("width", lParentElem.clientWidth);
    lNewElem.setAttribute("height", lParentElem.clientHeight);

    var lDeferReplacement = false;
    
    // Set new attrs
    var lAppendAttrsList = FoxDOM.getNodesByXPath(aCommandNode, "./append-attribute-list/append-attribute");
    for (var n = 0; n < lAppendAttrsList.length; n++) {
      var lAttrName = FoxDOM.get1SByXPath(lAppendAttrsList[n], "./name/text()");
      var lAttrValue = FoxDOM.get1SByXPath(lAppendAttrsList[n], "./value/text()");
      
      // If we have an image, we need to preload it to 
      // make the replacement look good, so defer it
      if (lNewElemName == "img" && lAttrName == "src") {
        lDeferReplacement = true;
        
        $(lNewElem).bind("load", $.proxy(function () {
          $(lParentElem).empty();
          delete this._foxCanvasMap[aDataIdentifier];
          lNewElem = lParentElem.appendChild(lNewElem);
          this._wrapImage(lNewElem);
          
          var lHtmlId = lNewElem.getAttribute("id");
          if (lHtmlId) {
            this._targetDOMNodes[lHtmlId] = lNewElem;
            setTimeout($.proxy(function () {
              this._invokePostReplacementCallbacks(lHtmlId);
            }, this), 0);
          }
        }, this));
      }
      
      // In all cases, set the attr
      lNewElem.setAttribute(lAttrName, lAttrValue);
      // For IE6/7
      if (lAttrName == "style") {
        lNewElem.style.cssText = lAttrValue;
      }
    }

    var lHtmlId = lNewElem.getAttribute("id");
    
    // Make the replacement immediately if not deferred
    if (!lDeferReplacement) {
      $(lParentElem).empty();
      delete this._foxCanvasMap[aDataIdentifier];
      lNewElem = lParentElem.appendChild(lNewElem);
      this._wrapImage(lNewElem);
      
      if (lHtmlId) {
        this._invokePostReplacementCallbacks(lId);
      }
    }
    
    if (lHtmlId) {
      this._targetDOMNodes[lHtmlId] = lNewElem;
    }
  }
  
  /**
   * Registers an HTML element against a <data-package/> node's
   * data-identifier, allowing the client to send attribute data
   * to the server at the server's request. Synchroniser will send
   * attribute data for this HTML element with every subsequent 
   * Ajax request or page POST until unregistered.
   * @param aCommandNode the <register-html-element/> node to process
   * @param aDataIdentifier the identifier of the package being processed
   */
, _registerHtmlElement: function (aCommandNode, aDataIdentifier) {
    // Get and validate HTML identifier
    var lHtmlId = FoxDOM.get1SByXPath(aCommandNode, "./element-id/text()");
    if (lHtmlId == null) {
      throw "Null HTML id found in Synchroniser._registerHtmlElement";
    }
    if (this._getElementById(lHtmlId) == null) {
      throw "HTML element with id='" + lHtmlId + "' not found in Synchroniser._registerHtmlElement";
    }
    
    // Init an array for attr names
    this._dataIdToHtmlIdMap[aDataIdentifier] = lHtmlId;
    var lArray = new Array();
    
    // Iterate through and push attributes to the array
    var lAttrNodeList = FoxDOM.getNodesByXPath(aCommandNode, "./attribute-list/attribute/text()");
    for (var i = 0; i < lAttrNodeList.length; i++) {
      lArray.push(lAttrNodeList[i].nodeValue);
    }
    this._htmlAttrsMap[lHtmlId] = lArray;
  }
  
  /**
   * Sets the attribute of a HTML element.
   * @param aCommandNode the <set-element-attribute/> node to process.
   */
, _setElementAttribute: function (aCommandNode) {
    // Get and validate HTML identifier
    var lHtmlId = FoxDOM.get1SByXPath(aCommandNode, "./element-id/text()");
    if (lHtmlId == null) {
      throw "Null HTML id found in Synchroniser._setElementAttribute";
    }
    
    var lAttrName = FoxDOM.get1SByXPath(aCommandNode, "./attribute-name/text()");
    var lAttrValue = FoxDOM.get1SByXPath(aCommandNode, "./attribute-value/text()");
    
    this._getElementById(lHtmlId).setAttribute(lAttrName, lAttrValue);
  }
  
  /**
   * Appends the containing <data-package/> to the service queue.
   * @param aCommandNode the <add-to-service-queue/> node to process
   * @param aDataIdentifier the data identifier of the package to be processed
   */
, _addToServiceQueue: function (aCommandNode, aDataIdentifier) {
    // Establish priority from command
    var lPriority = FoxDOM.get1SByXPath(aCommandNode, "./priority/text()");
    
    // Otherwise try default in DOM
    if (lPriority == null) {
      lPriority = FoxDOM.get1SByXPath(aCommandNode, "../../../default-service-priority/text()");
    }
    
    // Finally, default to lowest priority
    if (lPriority == null) {
      lPriority = this.PRIORITY_LOW;
    }
    
    // Copy the fragent we're working on (in case more processing will be carried out after this method call exits)
    // NB: parentNode = <command/>, parentNode.parentNode <command-list>, parentNode.parentNode.parentNode = <data-package/>
    var lCloneNode = aCommandNode.parentNode.parentNode.parentNode.cloneNode(true);
    
    // Wipe out the command structure (no need to send it)
    var lCommandListNode = lCloneNode.getElementsByTagName("command-list")[0];
    lCloneNode.removeChild(lCommandListNode);
    
    // Add it to the queue with specified priority or default
    this._serviceQueue.addToQueue(lPriority, aDataIdentifier, lCloneNode);
  }
  
, _elemArrayToTableRow: function (aTable, aElemArray) {
    var lTr = aTable.appendChild(document.createElement("tr"));
    for (var n in aElemArray) {
      var lTd = document.createElement("td");
      if (aElemArray[n] != null) {
        $(lTd).append(aElemArray[n]);
      }
      lTr.appendChild(lTd);
    }
  }
  
  /**
   * Sets an HTML element in panning mode. This should be an <img> element,
   * and has not been tested with any others.
   * @param aCommandNode the <set-image-pan-mode/> node to process
   * @param aDataIdentifier the data identifier of the package to be processed
   */
 , _setImagePanMode: function (aCommandNode, aDataIdentifier) {
    
    var lHtmlElementId = FoxDOM.get1SByXPath(aCommandNode, "./element-id/text()");   
    var lHtmlElem = this._getElementById(lHtmlElementId);
    var lEventLabel = FoxDOM.get1SByXPath(aCommandNode, "./event-label/text()");
    var lResetEventLabel = FoxDOM.get1SByXPath(aCommandNode, "./reset-event-label/text()");
    var lEnableMouse = FoxDOM.get1SByXPath(aCommandNode, "./enable-mouse/text()");    
    var lEnableButtons = FoxDOM.get1SByXPath(aCommandNode, "./enable-buttons/text()"); 
    var lEnableAnimation = FoxDOM.get1SByXPath(aCommandNode, "./enable-animation/text()");
    
    var lDragSetup = function (aEvent) {
        
      var lOriginLeft = $(lHtmlElem).offset().left;
      var lOriginTop = $(lHtmlElem).offset().top;
      var lDiffX = aEvent.clientX - lOriginLeft;
      var lDiffY = aEvent.clientY - lOriginTop;
      var lNewTop;
      var lNewLeft;
      
      var lMouseMoveHandler = function (aEvent) {
      
        var lCurrMouseX = aEvent.clientX - lOriginLeft;
        var lCurrMouseY = aEvent.clientY - lOriginTop;
        
        $(lHtmlElem).css({
          position: "absolute"
        , left: lNewLeft = lCurrMouseX - lDiffX
        , top: lNewTop = lCurrMouseY - lDiffY
        });
        
        aEvent.preventDefault();
        return false;
      }
      
      var lMouseUpHandler = function (aEvent) {
        
        var lNewCentroidX;
        var lNewCentroidY;
        
        // Mouse is up, so unbind the movement tracking handlers
        $(lHtmlElem).unbind("mousemove", lMouseMoveHandler);
        $(lHtmlElem).unbind("mouseup", lMouseUpHandler);
        $(lHtmlElem).unbind("mouseout", lMouseUpHandler);
        
        // If these are null, the mouse hasn't moved
        if (lNewLeft && lNewTop) {
          lNewCentroidX = ($(lHtmlElem).width() / 2) - lNewLeft;
          lNewCentroidY = ($(lHtmlElem).height() / 2) - lNewTop;
          
          // Remove listeners
          $(lHtmlElem).unbind("mousedown", $.proxy(lDragSetup, this));
          $(lHtmlElem).css("cursor", "progress");
          $(lHtmlElem).parent().css("cursor", "progress");
          
          this._sendCentroidEvent(
            aDataIdentifier
          , lEventLabel
          , lNewCentroidX
          , lNewCentroidY
          , "none"
          );
        }
        else {
          $(lHtmlElem).css("cursor", "default");
        }
      }
      
      aEvent.preventDefault();
      $(lHtmlElem).bind("mousemove", $.proxy(lMouseMoveHandler, this));
      $(lHtmlElem).bind("mouseup", $.proxy(lMouseUpHandler, this));
      $(lHtmlElem).bind("mouseout", $.proxy(lMouseUpHandler, this));
      $(lHtmlElem).css("cursor", "all-scroll");
    }
    
    if (lEnableMouse == "true") {
      $(lHtmlElem).bind("mousedown", $.proxy(lDragSetup, this));
      
      this._addCleanupFunction(aDataIdentifier, "*", function () {
        $(lHtmlElem).unbind("mousedown", lDragSetup);
      });
    }
    
    if (lEnableButtons == "true") {
      // Post process these buttons as DOM elements may be replaced 
      // during command processing
      var lSetupPanelFn = function(){
      
        var lButtonPanel = this._getButtonPanel(aDataIdentifier, "nav", 2, 2, null, null, 10);
        var lImageBase = document.forms["mainForm"].action + "/" + document.forms["mainForm"]["app_mnem"].value + "/";
        
        this._elemArrayToTableRow(lButtonPanel, [
          null
        , $(document.createElement("img")).attr({"class": "panelbutton pan-up", src: lImageBase + "img/pan_up", title: "Pan up"})
        , null
        ]);
        
        this._elemArrayToTableRow(lButtonPanel, [
          $(document.createElement("img")).attr({"class": "panelbutton pan-left", src: lImageBase + "img/pan_left", title: "Pan left"})
        , $(document.createElement("img")).attr({"class": "panelbutton pan-home", src: lImageBase + "img/pan_home", title: "Return to the original position"})
        , $(document.createElement("img")).attr({"class": "panelbutton pan-right", src: lImageBase + "img/pan_right", title: "Pan right"})
        ]);
        
        this._elemArrayToTableRow(lButtonPanel, [
          null
        , $(document.createElement("img")).attr({"class": "panelbutton pan-down", src: lImageBase + "img/pan_down", title: "Pan down"})
        , null
        ]);
        
        this._elemArrayToTableRow(lButtonPanel, [null, document.createElement("br"), null]);
        
        var lClosure = function(aEvent) {
          var lNewCentroidX;
          var lNewCentroidY;
          var lNewLeft = 0;
          var lNewTop = 0;
          var lEventToRaise = lEventLabel;
          
          if ($(aEvent.target).hasClass("pan-left")) {
            lNewCentroidX = 0;
            lNewCentroidY = $(lHtmlElem).height()/2;
            lNewLeft = $(lHtmlElem).width() / 2;
          }
          else if ($(aEvent.target).hasClass("pan-right")) {
            lNewCentroidX = $(lHtmlElem).width();
            lNewCentroidY = $(lHtmlElem).height()/2;
            lNewLeft = -1 * ($(lHtmlElem).width() / 2);
          }
          else if ($(aEvent.target).hasClass("pan-up")) {
            lNewCentroidX = $(lHtmlElem).width()/2;
            lNewCentroidY = 0;
            lNewTop = $(lHtmlElem).height() / 2;
          }
          else if ($(aEvent.target).hasClass("pan-down")) {
            lNewCentroidX = $(lHtmlElem).width()/2;
            lNewCentroidY = $(lHtmlElem).height();
            lNewTop = -1 * ($(lHtmlElem).height() / 2);
          }
          else if ($(aEvent.target).hasClass("pan-home")) {
            // No effects
            lEventToRaise = lResetEventLabel;
          }
          
          if (lEnableAnimation == "true") {
            $(lHtmlElem).css({display:"block", position: "relative"});
            $(lHtmlElem).animate({left: lNewLeft, top: lNewTop}, 300);
          }
          
          this._sendCentroidEvent(
            aDataIdentifier
          , lEventToRaise
          , lNewCentroidX
          , lNewCentroidY
          , "none"
          );
        }
        
        $(lButtonPanel).find("img.panelbutton").bind("click", $.proxy(lClosure, this)).css({cursor: "pointer", opacity: 0.75});
        
        this._addCleanupFunction(aDataIdentifier, "*", function () {
          $(lButtonPanel).find("img.panelbutton").unbind("click", lClosure).css({cursor: "progress"});
          $(lHtmlElem).css({cursor: "progress"});
          $(lHtmlElem).parent().css({cursor: "progress"});
        });
      }
       
      if (this._isPendingInsertion(lHtmlElementId)) { 
        this._addPostReplacementCallback(lHtmlElementId, lSetupPanelFn);
      }
      else {
        lSetupPanelFn.call(this);
      }
    }
  }
  
, _addPostReplacementCallback: function (aHtmlElemId, aFunction) {
    var lFunctionArray = this._postReplacementCallbacks[aHtmlElemId];
    if (!lFunctionArray) {
      lFunctionArray = this._postReplacementCallbacks[aHtmlElemId] = new Array();
    }
    lFunctionArray.push(aFunction);
  }
  
, _invokePostReplacementCallbacks: function (aHtmlElemId) {
    var lFunctionArray = this._postReplacementCallbacks[aHtmlElemId];
    if (lFunctionArray) {
      for (var n in lFunctionArray) {
        lFunctionArray[n].call(this);
      }
    }
    this._postReplacementCallbacks[aHtmlElemId] = null;
  }
  
  /**
   * Wraps the image element provided with an html <div> element,
   * using the calculated width/height provided, or otherwise
   * calculates the width and height of the element itself.
   * @param aImageElem the element to wrap
   * @param aWidth the precalcualted width (if known)
   * @param aHeight the precalculated height (if known)
   */
, _wrapImage: function (aImageElem, aWidth, aHeight) {

    // If image is already wrapped, no need to do it again
    if ($(aImageElem).parent().hasClass("sync-image-wrapper")) {
      return $(aImageElem).parent();
    }
    
    // Use width/height if provided, or fallback to HTML element
    var lHeight = aHeight ? aHeight : $(aImageElem).height();
    var lWidth = aWidth ? aWidth : $(aImageElem).width();
   
    // For zooming and panning, we need to wrap the image in an 
    // element we can force to respect the overflow property
    var lDiv = document.createElement("div");
    $(lDiv).addClass("sync-image-wrapper");
    $(lDiv).css({
      "height": lHeight
    , "width": lWidth
    , "overflow": "hidden"
    , "background-color": "#DDDDDD"
    });
    lDiv = $(aImageElem).wrap(lDiv);
    
    return lDiv;
  }
  
  /**
   * Bootstrap a button panel (table) in the top left of a widget.
   */
, _getButtonPanel: function (aIdentifier, aPurpose, aTop, aLeft, aBottom, aRight, aZIndex) {
    var lHtmlElem = this._getElementById(this._dataIdToHtmlIdMap[aIdentifier]);
    var lButtonPanelId = aIdentifier + "_buttonPanel_" + aPurpose;
    var lTBody = document.getElementById(lButtonPanelId);
    
    if (!lTBody) {
      var lButtonPanel = document.createElement("table");
      var lTBody = document.createElement("tbody");
      lButtonPanel.appendChild(lTBody);
      
      lTBody.setAttribute("id", lButtonPanelId);
      var lWrapDiv = document.createElement("div");
      $(lWrapDiv).css({
        "position": "absolute"
      , "top": aTop ? aTop + "px" : "auto"
      , "left": aLeft ? aLeft + "px" : "auto"
      , "bottom": aBottom ? aBottom + "px" : "auto"
      , "right": aRight ? aRight + "px" : "auto"
      , "border": "none"
      , "z-index": aZIndex ? aZIndex : 0
      , "background-image": "url(#)"
      });
      lHtmlElem.appendChild(lButtonPanel);
      $(lButtonPanel).wrap(lWrapDiv);
    }
    return lTBody;
  }
 
  /**
   * Sets scroll wheel zooming on an element, which should ideally be an <img> element.
   * @param aCommandNode the <set-image-zoom-mode> node to process
   * @param aDataIdentifier the data identifier of the package to be processed
   */
, _setImageZoomMode: function (aCommandNode, aDataIdentifier) {
    
    var lHtmlElementId = FoxDOM.get1SByXPath(aCommandNode, "./element-id/text()");   
    var lHtmlElem = this._getElementById(lHtmlElementId);    
    var lZoomPctDecimal = parseFloat(FoxDOM.get1SByXPath(aCommandNode, "./zoom-pct/text()")) / 100.00;
    var lEventLabel = FoxDOM.get1SByXPath(aCommandNode, "./event-label/text()");
    var lEnableMouse = FoxDOM.get1SByXPath(aCommandNode, "./enable-mouse/text()");
    var lEnableButtons = FoxDOM.get1SByXPath(aCommandNode, "./enable-buttons/text()");
    var lEnableAnimation = FoxDOM.get1SByXPath(aCommandNode, "./enable-animation/text()");
    
    var lClosure = function (aEvent, aDelta) {

      // Immediately unbind
      $(lHtmlElem).unbind("mousewheel");
    
      // Stop the page scrolling
      if (aEvent.preventDefault) {
        aEvent.preventDefault();
      }
      
      var lCentroid = this._zoomImageInternal(
        aEvent
      , aDelta
      , lHtmlElem
      , lZoomPctDecimal
      , (lEnableAnimation == "true")
      );
      
      if (!lCentroid) {
        // Something has gone wrong, rebind
        $(lHtmlElem).bind("mousewheel", $.proxy(lClosure, this));
        return false;
      }
      
      var lDirection;
      if (aDelta > 0 ) {
        lDirection = "in";
      }
      else if (aDelta < 0) {
        lDirection = "out";
      }

      // Send Result to FOX
      this._sendCentroidEvent(
        aDataIdentifier
      , lEventLabel
      , lCentroid.x
      , lCentroid.y
      , lDirection
      );
    };
    
    if (lEnableMouse == "true") {
      $(lHtmlElem).bind("mousewheel", $.proxy(lClosure, this));
      
      this._addCleanupFunction(aDataIdentifier, "*", function () {
        $(lHtmlElem).unbind("mousewheel", lClosure);
        $(lHtmlElem).bind("mousewheel", function (aEvent) {
          aEvent.preventDefault();
        });
      });
    }
    
    if (lEnableButtons == "true") {
      // Post process these buttons as DOM elements may be replaced 
      // during command processing
      var lSetupPanelFn = function(){
        var lButtonPanel = this._getButtonPanel(aDataIdentifier, "nav", 2, 2, null, null, 10);
        var lImageBase = document.forms["mainForm"].action + "/" + document.forms["mainForm"]["app_mnem"].value + "/";
        
        this._elemArrayToTableRow(lButtonPanel, [null, $(document.createElement("img")).attr({"class": "panelbutton zoom-in", src: lImageBase + "img/zoom_in", title: "Zoom in"}), null]);
        this._elemArrayToTableRow(lButtonPanel, [null, $(document.createElement("img")).attr({"class": "panelbutton zoom-out", src: lImageBase + "img/zoom_out", title: "Zoom out"}), null]);
        
        $(lButtonPanel).find("img.zoom-in").bind("click", $.proxy(function(aEvent){
          var lX = ($(lHtmlElem).offset().left - $(window).scrollLeft()) + ($(lHtmlElem).width()/2);
          var lY = ($(lHtmlElem).offset().top - $(window).scrollTop()) + ($(lHtmlElem).height()/2);
          lClosure.call(this, {clientX: lX, clientY: lY}, 1);
        }, this)).css({cursor: "pointer", opacity: 0.75});
        
        $(lButtonPanel).find("img.zoom-out").bind("click", $.proxy(function(aEvent){
          var lX = ($(lHtmlElem).offset().left - $(window).scrollLeft()) + ($(lHtmlElem).width()/2);
          var lY = ($(lHtmlElem).offset().top - $(window).scrollTop()) + ($(lHtmlElem).height()/2);
          lClosure.call(this, {clientX: lX, clientY: lY}, -1);
        }, this)).css({cursor: "pointer", opacity: 0.75});
        
        this._addCleanupFunction(aDataIdentifier, "*", function () {
          $(lButtonPanel).find("img.panelbutton").unbind("click").css({cursor: "progress"});
          $(lHtmlElem).css({cursor: "progress"});
          $(lHtmlElem).parent().css({cursor: "progress"});
        });
      };
      
      if (this._isPendingInsertion(lHtmlElementId)) { 
        this._addPostReplacementCallback(lHtmlElementId, lSetupPanelFn);
      }
      else {
        lSetupPanelFn.call(this);
      }
    }
  }
  
  /**
   * Handles the internal process of zooming.
   * @param aEvent the scroll wheel event object (or equivalent)
   * @param aDelta the scroll delta (1 = zoom in, -1 = zoom out)
   * @param aHtmlElem the target html element to zoom (and ultimately replace)
   * @param aZoomPctDecimal the zoom percentage
   * @param aAnimate animate or not (boolean)
   * @return arbitrary object containing zoom centroid {x,y}
   */
, _zoomImageInternal: function (aEvent, aDelta, aHtmlElem, aZoomPctDecimal, aAnimate) {
   
    var lHeight = $(aHtmlElem).height();
    var lWidth = $(aHtmlElem).width();
    
    // Get the mouse position within the image
    var lMouseX = aEvent.clientX - $(aHtmlElem).offset().left + $(window).scrollLeft();
    var lMouseY = aEvent.clientY - $(aHtmlElem).offset().top + $(window).scrollTop();
    var lAnimateTarget;
    var lCentroidX;
    var lCentroidY;
    var lZoomDirection;
    
    // Zooming in
    if (aDelta > 0) {
      // Work out the new bounding box that we want to display based on the zoom percentage
      var lWindowLeft = lMouseX * aZoomPctDecimal;
      var lWindowTop = lMouseY * aZoomPctDecimal;
      var lWindowRight = lMouseX + ((lWidth - lMouseX) * aZoomPctDecimal);
      var lWindowBottom = lMouseY + ((lHeight - lMouseY) * aZoomPctDecimal);
    
      // New centroid to pass to the back-end
      lCentroidX = lWindowLeft + ((lWindowRight-lWindowLeft)/2);
      lCentroidY = lWindowTop + ((lWindowBottom-lWindowTop)/2);
      
      lAnimateTarget = {
        top: lWindowTop / (-1 * aZoomPctDecimal)
      , left: lWindowLeft / (-1 * aZoomPctDecimal)
      , width: lWidth + ((lWidth - lWindowRight) / aZoomPctDecimal) + (lWindowLeft / aZoomPctDecimal)
      , height: lHeight + ((lHeight - lWindowBottom) / aZoomPctDecimal) + (lWindowTop / aZoomPctDecimal)
      };
    }
    // Zooming out
    else if (aDelta < 0) {
      
      var lImageCenterX = lWidth/2;
      var lImageCenterY = lHeight/2;
    
      // New centroid to pass to the back-end
      lCentroidX = lImageCenterX + ((lImageCenterX - lMouseX) * aZoomPctDecimal); 
      lCentroidY = lImageCenterY + ((lImageCenterY - lMouseY) * aZoomPctDecimal); 
      
      // Work around possible jQuery 1.5 bug, where animating to the target width
      // causes the left position to be incorrect
      var lLeftPosHack = (lWidth - (lWidth / (1+aZoomPctDecimal)))/2;
      
      lAnimateTarget = {
        top: lMouseY - (lMouseY / (1+aZoomPctDecimal))
      , left: lMouseX - (lMouseX / (1+aZoomPctDecimal))
      , width: lWidth / (1+aZoomPctDecimal)
      , height: lHeight / (1+aZoomPctDecimal)
      };
    }
    
    // Prepare for animation
    $(aHtmlElem).css({position: "absolute", cursor: "progress", top: 0, left: 0});
    $(aHtmlElem).parent().css("cursor", "progress");
    
    // Zoom in or out in an animated way
    if (aAnimate) {
      $(aHtmlElem).animate(lAnimateTarget, 300);
    }
    
    return {x: lCentroidX, y: lCentroidY};
  }
  
  /**
   * Sends a centroid x,y coord with an event label for a given element. 
   * @param aIdentifier the unique data identifier
   * @param aEventLabel the event label to raise
   * @param aCentroidX centroid x ordinate
   * @param aCentroidY centroid y ordinate
   * @param aZoomDirection zoom direction ["in","out","none"]
   */
, _sendCentroidEvent: function (aIdentifier, aEventLabel, aCentroidX, aCentroidY, aZoomDirection) {

    // Clean down any remaining listeners so we can POST with safety
    this._invokeCleanupFunctions(aIdentifier, "centroid-event");
    
    var lWorkingDOM = this._dataMap[aIdentifier];    
    FoxDOM.appendChildToDOM(lWorkingDOM, "event-label", aEventLabel);
    
    var lData = FoxDOM.getNodesByXPath(lWorkingDOM, "./data")[0];
    FoxDOM.appendChildToDOM(lData, "zoom-direction", aZoomDirection);
    var lCoordSet = FoxDOM.appendChildToDOM(lData, "coord-set", null);
    
    FoxDOM.appendChildToDOM(lCoordSet, "mnem", aEventLabel);
    var lCoordList = FoxDOM.appendChildToDOM(lCoordSet, "coord-list", null);
    
    FoxDOM.appendChildToDOM(lCoordList, "coord", parseInt(aCentroidX) + "," + parseInt(aCentroidY));
    this._sendPackageAsAjax(aIdentifier, lWorkingDOM, true);   
    
  }
 
  /**
   * Sets up click capture mode on an element, will raise an event.
   * @param aCommandNode the <set-click-capture-mode/> node to process
   * @param aDataIdentifier the data identifier of the package to be processed
   */
, _setClickCaptureMode: function (aCommandNode, aDataIdentifier) {
    // Get command-specific attrs
    var lEventLabel = FoxDOM.get1SByXPath(aCommandNode, "./event-label/text()");
    var lNumberOfClicks = FoxDOM.get1SByXPath(aCommandNode, "./clicks/text()");
    var lHtmlElementId = FoxDOM.get1SByXPath(aCommandNode, "./element-id/text()");
    var lCursorName = FoxDOM.get1SByXPath(aCommandNode, "./cursor-name/text()");
    
    // Check params
    if (lEventLabel == null || lEventLabel == "") {
      throw "Event label is null, must provide event-label to raise when click-capture has completed.";
    }
    if (lNumberOfClicks == null || lNumberOfClicks == "" || lNumberOfClicks == 0) {
      throw "Number of clicks to capture not specified.";
    }
    if (lHtmlElementId == null || lHtmlElementId == "") {
      throw "Must provide an HTML Element ID when setting click capture mode.";
    }
    // Simply default this if missing
    if (lCursorName == null || lCursorName == "") {
      lCursorName = "default";
    }
    
    // Get HTML Element reference 
    var lElem = this._getElementById(lHtmlElementId);
    if (lElem == null) {
      throw "Could not locate HTML Element with id '" + lHtmlElementId + "'";
    }
    
    // Closure self-reference
    var self = this;
    
    var lClosure = function (aEvent) {
      var lWorkingDOM = self._dataMap[aDataIdentifier];
      var lCoordSet = FoxDOM.getNodesByXPath(lWorkingDOM, "./data/coord-set[mnem='"+lEventLabel+"']")[0];
      var lCoordList;
      
      // Create it just-in-time if necessary
      if (!lCoordSet) {
        var lData = FoxDOM.getNodesByXPath(lWorkingDOM, "./data")[0];
        var lCoordSet = FoxDOM.appendChildToDOM(lData, "coord-set", null);
        FoxDOM.appendChildToDOM(lCoordSet, "mnem", lEventLabel);
        lCoordList = FoxDOM.appendChildToDOM(lCoordSet, "coord-list", null);
      }
      else {
        lCoordList = FoxDOM.getNodesByXPath(lCoordSet, "./coord-list")[0];
      }
      
      var lEvent = aEvent ? aEvent : window.event;
      var lClickTarget = lEvent.target ? lEvent.target : lEvent.srcElement;
      
      // Belt and braces
      if (lClickTarget != lElem) {
        throw "Click target is not the expected subject of this closure";
      }
      
      // Get coords from click
      var lCoordX;
      var lCoordY;
      
      // If browser offers us offsetX and offsetY - take it
      if (lEvent.offsetX) {
        lCoordX = lEvent.offsetX;
        lCoordY = lEvent.offsetY
      } 
      
      // Otherwise walk the tree and calculate the position of the image
      // and subtract from page coordinate of mouse click
      else {
        var lObject = lClickTarget;
        var lOffsetLeft = 0;
        var lOffsetTop = 0;

        do {
          lOffsetLeft += lObject.offsetLeft;
          lOffsetTop += lObject.offsetTop;
        } while (lObject = lObject.offsetParent);
        
        lCoordX = lEvent.pageX - lOffsetLeft;
        lCoordY = lEvent.pageY - lOffsetTop;
      }
      
      // Append to outgoing DOM
      FoxDOM.appendChildToDOM(lCoordList, "coord", lCoordX + "," + lCoordY);
      // Goal reached?
      if (lCoordList.childNodes.length == lNumberOfClicks) {
        // Reset cursor
        try { 
          // IE5.5 doesn't support this (and actually throws an error!)
          lClickTarget.style.cursor = "progress";
        }
        catch (e) {
          lClickTarget.style.cursor = "wait";
        }
        // Tidy up event listeners, etc
        this._invokeCleanupFunctions(aDataIdentifier, "set-click-capture-mode");
        
        // Send DOM to FOX
        FoxDOM.appendChildToDOM(lWorkingDOM, "event-label", lEventLabel);
        self._sendPackageAsAjax(aDataIdentifier, lWorkingDOM, true);
      }
    };
    
    // Use FOX event assignment
    $(lElem).bind("click", $.proxy(lClosure, this));   
    
    this._addCleanupFunction(aDataIdentifier, "*", function () {
      $(lElem).unbind("click", lClosure);
    });
    
    // Swap cursor style
    // (if not MSIE, and "hand", then switch to "pointer"
    lCursorName = (!document.all && lCursorName == "hand") ? "pointer" : lCursorName;
    lElem.style.cursor = lCursorName;
  }
  
, _serialiseCanvasToPackage: function (aDataIdentifier) {
    // Pick out the canvas by identifier
    var lFoxCanvas = this._foxCanvasMap[aDataIdentifier];
    var lEventLabel = this._foxCanvasEventLabelMap[aDataIdentifier];
    
    // Try to clean up if the user hasn't finalised the current shape yet
    lFoxCanvas.finaliseOrDiscardCurrentShape();
    
    // Now get an array of basic shape objects {type,nodes}
    var lBasicShapeArray = lFoxCanvas.getFinalisedShapes();
    
    // Serialise each basic shape just-in-time into the data package and post to the back-end
    var lWorkingDOM = this._dataMap[aDataIdentifier];   
    BASIC_SHAPE_LOOP: for (var n in lBasicShapeArray) {
    
      var lSetMnem = lEventLabel + "/" + lBasicShapeArray[n].type + "/" + n;
      var lCoordSet = FoxDOM.getNodesByXPath(lWorkingDOM, "./data/coord-set[mnem='" + lSetMnem + "']")[0];
      var lCoordList;
      
      // Create it just-in-time if necessary
      if (!lCoordSet) {
        var lData = FoxDOM.getNodesByXPath(lWorkingDOM, "./data")[0];
        var lCoordSet = FoxDOM.appendChildToDOM(lData, "coord-set", null);
        FoxDOM.appendChildToDOM(lCoordSet, "mnem", lSetMnem);
        lCoordList = FoxDOM.appendChildToDOM(lCoordSet, "coord-list", null);
      }
      else {
        lCoordList = FoxDOM.getNodesByXPath(lCoordSet, "./coord-list")[0];
      }
      
      for (var i = 0; i < lBasicShapeArray[n].nodes.length; i++) {
        var lCoord = FoxDOM.appendChildToDOM(lCoordList, "coord", lBasicShapeArray[n].nodes[i].x + "," + lBasicShapeArray[n].nodes[i].y);
        lCoord.setAttribute("xequiv", lBasicShapeArray[n].nodes[i].xequiv);
        lCoord.setAttribute("yequiv", lBasicShapeArray[n].nodes[i].yequiv);
      }
    } // BASIC_SHAPE_LOOP
    
    if (lBasicShapeArray.length > 0 && lEventLabel != null && lEventLabel != "") {
      FoxDOM.appendChildToDOM(lWorkingDOM, "event-label", lEventLabel);
    }
    
    return lWorkingDOM;
  }
  
  /**
   * Sets up a canvas to be drawn on client-side. This should be an <img> element,
   * and has not been tested with any others.
   * @param aCommandNode the <set-canvas-draw-mode/> node to process
   * @param aDataIdentifier the data identifier of the package to be processed
   */
 , _setCanvasDrawMode: function (aCommandNode, aDataIdentifier) {
    // Extract all the necessary parameters
    var lHtmlElementId = FoxDOM.get1SByXPath(aCommandNode, "./element-id/text()");   
    var lHtmlElem = this._getElementById(lHtmlElementId);
    var lEventLabel = FoxDOM.get1SByXPath(aCommandNode, "./event-label/text()");    
    var lEnableModeButtons = FoxDOM.get1SByXPath(aCommandNode, "./enable-mode-buttons/text()");
    
    var lPointMode = FoxDOM.get1SByXPath(aCommandNode, "./point-mode/text()");
    var lLineMode = FoxDOM.get1SByXPath(aCommandNode, "./line-mode/text()");
    var lConnectedLineMode = FoxDOM.get1SByXPath(aCommandNode, "./connected-line-mode/text()");
    var lPolyLineMode = FoxDOM.get1SByXPath(aCommandNode, "./poly-line-mode/text()");
    
    // Modes: {"point","line","connected-line","poly-line"}
    var lStartingMode = FoxDOM.get1SByXPath(aCommandNode, "./starting-mode/text()");
    var lPreventDiagonals = FoxDOM.get1SByXPath(aCommandNode, "./prevent-diagonals/text()");
    
    // Immediately remove from the service queue so that digitising isn't interrupted if
    // we're put in the service queue somehow
    this._serviceQueue.removeFromAnyQueue(aDataIdentifier);
    
    // Also, prevent automatic ajax requests for this id
    this._noAjax[aDataIdentifier] = true;
    
    var lFoxCanvas = this._foxCanvasMap[aDataIdentifier];
    if (!lFoxCanvas) {
      lFoxCanvas = new FoxCanvas(lHtmlElementId);
      this._foxCanvasMap[aDataIdentifier] = lFoxCanvas;
      this._foxCanvasEventLabelMap[aDataIdentifier] = lEventLabel;
    }
    
    lFoxCanvas.clear();
    lFoxCanvas.clearDigitisingPoints();
    lFoxCanvas.setMode(lStartingMode);
    
    var lDigitisingPoints = new Array();
    var lDigitisingNodeXMLList = FoxDOM.getNodesByXPath(aCommandNode, "./coord-list/coord");
    for (var n = 0; n < lDigitisingNodeXMLList.length; n++) {
      lDigitisingPoints.push({
        x: FoxDOM.get1SByXPath(lDigitisingNodeXMLList[n], "./x/text()")
      , y: FoxDOM.get1SByXPath(lDigitisingNodeXMLList[n], "./y/text()")
      , desc: FoxDOM.get1SByXPath(lDigitisingNodeXMLList[n], "./desc/text()")
      , xequiv: FoxDOM.get1SByXPath(lDigitisingNodeXMLList[n], "./xequiv/text()")
      , yequiv: FoxDOM.get1SByXPath(lDigitisingNodeXMLList[n], "./yequiv/text()")
      });
    }
    
    if (lDigitisingPoints.length > 0) {
      lFoxCanvas.setDigitisingPoints(lDigitisingPoints);
      lFoxCanvas.setSnapping(true);
      lFoxCanvas.preventDiagonals(lPreventDiagonals == "true");
    }
    
    var lButtonPanel = this._getButtonPanel(aDataIdentifier, "draw",  null, 2, 2, null, 10);
    var lImageBase = document.forms["mainForm"].action + "/" + document.forms["mainForm"]["app_mnem"].value + "/";
    
    var lButtonArray = new Array();
    // lButtonArray.push($(document.createElement("img")).attr({"class": "panelbutton draw-save", src: lImageBase + "img/draw_save", title: "Save changes"}));
    // lButtonArray.push($(document.createElement("img")).attr({"class": "panelbutton draw-cancel", src: lImageBase + "img/draw_cancel", title: "Undo changes"}));
    
    if (lPointMode == "true") {
      lButtonArray.push($(document.createElement("img")).attr({"class": "panelbutton draw-point", src: lImageBase + "img/draw_point", title: "Point drawing mode"}));
    }
    if (lLineMode == "true") {
      lButtonArray.push($(document.createElement("img")).attr({"class": "panelbutton draw-line", src: lImageBase + "img/draw_line", title: "Line drawing mode"}));
    }
    if (lConnectedLineMode == "true") {
      lButtonArray.push($(document.createElement("img")).attr({"class": "panelbutton draw-connected-line", src: lImageBase + "img/draw_connected_line", title: "Continuous line drawing mode"}));
    }
    if (lPolyLineMode == "true") {
      lButtonArray.push($(document.createElement("img")).attr({"class": "panelbutton draw-poly-line", src: lImageBase + "img/draw_poly_line", title: "Polygon drawing mode"}));
    }
    
    this._elemArrayToTableRow(lButtonPanel, lButtonArray);
    $(lButtonPanel).find("img.panelbutton").css({cursor: "pointer", opacity: 0.75});
    
    $(lButtonPanel).find("img.draw-save").bind("click", $.proxy(function(aEvent){
      // Post to FOX
      this._sendPackageAsAjax(aDataIdentifier, this._dataMap[aDataIdentifier], true);   
      
      // Tidy up event listeners, etc
      this._invokeCleanupFunctions(aDataIdentifier, "canvas-draw-save");
    }, this));
        
    $(lButtonPanel).find("img.draw-cancel").bind("click", $.proxy(function(aEvent){
      if (confirm("This will discard all unsaved shapes. Are you sure you want to continue?")) {
        lFoxCanvas.clear();
      }
    }, this));
    
    $(lButtonPanel).find("img.draw-point").bind("click", $.proxy(function(aEvent){
      lFoxCanvas.setMode("point");
    }, this));
    
    $(lButtonPanel).find("img.draw-line").bind("click", $.proxy(function(aEvent){
      lFoxCanvas.setMode("line");
    }, this));
    
    $(lButtonPanel).find("img.draw-connected-line").bind("click", $.proxy(function(aEvent){
      lFoxCanvas.setMode("connected-line");
    }, this));
    
    $(lButtonPanel).find("img.draw-poly-line").bind("click", $.proxy(function(aEvent){
      lFoxCanvas.setMode("poly-line");
    }, this));
    
    this._addCleanupFunction(aDataIdentifier, "*", function () {
      lFoxCanvas.setMode("none");
      lFoxCanvas.setSnapping(false);
      lFoxCanvas.clearTooltip();
      $(lButtonPanel).find("img.panelbutton").unbind("click").css({cursor: "progress"});
      $(lHtmlElem).css({cursor: "progress"});
      $(lHtmlElem).parent().css({cursor: "progress"});
      $(lFoxCanvas._anchorElem).css({cursor: "progress"});
      this._noAjax[aDataIdentifier] = false;
    });
      
    this._addCleanupFunction(aDataIdentifier, "centroid-event", function () {
      lFoxCanvas.clear();
    });
  }
  
, _addCleanupFunction: function (aIdentifier, aTopic, aFunction) {
    var lFunctionArray = this._dataIdToCleanupMap[aIdentifier + "/" + aTopic];
    if (!lFunctionArray) {
      lFunctionArray = this._dataIdToCleanupMap[aIdentifier + "/" + aTopic] = new Array();
    }
    lFunctionArray.push(aFunction);
  }
  
, _invokeCleanupFunctions: function (aIdentifier, aTopic) {
    var lFunctionArray = this._dataIdToCleanupMap[aIdentifier + "/*"];
    if (lFunctionArray) {
      for (var n in lFunctionArray) {
        lFunctionArray[n].call(this);
      }
    }
   
    lFunctionArray = this._dataIdToCleanupMap[aIdentifier + "/" + aTopic];
    if (lFunctionArray) {
      for (var n in lFunctionArray) {
        lFunctionArray[n].call(this);
      }
    }
    
    this._dataIdToCleanupMap[aIdentifier + "/*"] = null;
    this._dataIdToCleanupMap[aIdentifier + "/" + aTopic] = null;
  }
  
  /**
   * Convenience method that alerts out an error from an 
   * ajax response arbitrary object.
   * @param aAjaxResponse arbitrary object containing the following fields: 
   *        {statusCode, statusMsg, textData, xmlData, responseOk}
   */
, _alertError: function (aAjaxResponse) {
    // Build up error string
    var lErrorString = "Invalid Ajax Response...\nHTTP Status Code: " + aAjaxResponse.statusCode
      + "\nHTTP Status Message: " + aAjaxResponse.statusMsg;
    if (aAjaxResponse.textData) {
      lErrorString += "\nMessage Body: " + aAjaxResponse.textData;
    }
    alert(lErrorString);
  }
  
  /**
   * Wraps passed array members in standard <ajax-request/> 
   * structure, returns as a DOM.
   * @param aDOMArray an array of DOM objects to wrap
   * @param aMergeAttrs boolean merge attributes or not
   * @param aForceSendAllPackages boolean send all packages even if they opted out
   * @return DOM object wrapping DOM objects from aDOMArray
   */
, _buildReqDOM: function (aDOMArray, aMergeAttrs, aForceSendAllPackages) {
    // Create a new request DOM
    var lReqDOM = FoxDOM.newDOM("ajax-request");
    var lPkgList = lReqDOM.createElement("data-package-list");
    
    // Import the node
    for (var n in aDOMArray) {
      // Only send a package if it's not locked (we're sending it already and waiting
      // on a response) and if it's not known to the service queue (i.e. will be serviced
      // shortly, and will therefore sort itself out in due course).
      if (!this._dataPackageLocks[n] && !this._serviceQueue.isItemInAnyQueue(n) && (aForceSendAllPackages || !this._noAjax[n])) {
      
        var lDOM = aDOMArray[n];
        var lDataIdentifier = FoxDOM.get1SByXPath(lDOM, "./data-identifier/text()");
        
        // Rebuild the data package in case of a canvas
        if (this._foxCanvasMap[lDataIdentifier] != null) {
          lDOM = this._serialiseCanvasToPackage(lDataIdentifier);
        }
        
        var lDataPkg = FoxDOM.importNode(lReqDOM, lDOM, true);
        if (aMergeAttrs) {
          lDataPkg = this._mergeHtmlAttributes(lDataPkg);
        }
        lPkgList.appendChild(lDataPkg);
      }
    }
    
    if (lPkgList.childNodes.length == 0) {
      return false;
    }
    
    // Workaround to prevent double-requests through misbehaving browsers/proxies
    var lUniqueKeyDOM = lReqDOM.createElement("unique-key");
    var lFieldSet = document.mainForm["field_set"].value;
    var lUniqueKey = lReqDOM.createTextNode(lFieldSet + "_" + new Date().getTime());
    
    lUniqueKeyDOM.appendChild(lUniqueKey);
    lReqDOM.documentElement.appendChild(lUniqueKeyDOM);

    // Build document and return
    lReqDOM.documentElement.appendChild(lPkgList);
    return lReqDOM;
  }

  /**
   * Populates the service queue with all internal data packages.
   */
, _fullyPopulateServiceQueue: function () {
    // Stop the pollers so we don't have to worry about the order
    // in which we add things
    this._serviceQueue.stopPolling();
    
    // Loop through all the data we're holding in memory and
    // add each item to the queue
    for (var n in this._dataMap) {
      var lDOM = this._dataMap[n];
      var lIdentifier = FoxDOM.get1SByXPath(lDOM, "./data-identifier/text()");
      var lPriority = FoxDOM.get1SByXPath(lDOM, "./default-service-priority/text()");
      this._serviceQueue.addToQueue(lPriority, lIdentifier, lDOM);
    }
    
    // Restart poller
    this._serviceQueue.startPolling();
  }
  
  /**
   * Builds up an <ajax-request/> containing all data and
   * serialises to a string.
   * @return string representation of full request
   */
, getAllDataAsXMLString: function () {
    var lReqDOM = this._buildReqDOM(this._dataMap, true, true);
    return lReqDOM ? FoxDOM.serializeDOMToString(lReqDOM) : false;
  }
  
  /**
   * Builds up an <ajax-request/> containing all data that can
   * be dynamically refreshed (i.e. data packages that have not 
   * opted out, and serialises to a string).
   * @return string representation of ajax request to make
   */
, getAjaxDataAsXMLString: function () {
    var lReqDOM = this._buildReqDOM(this._dataMap, true, false);
    return lReqDOM ? FoxDOM.serializeDOMToString(lReqDOM) : false;
  }

  /**
   * Utility method to implement indexOf in an array.
   * @param aObject the object to seek
   * @param aArray the array in which to seek the object
   * @return index position or -1
   */
, getObjectIndexInArray: function (aObject, aArray) {
    for (var i = 0; i < aArray.length; i++) {
      if (aArray[i] == aObject) {
        return i;
      }
    }
    return -1;
  }
  
  /**
   * Retrieves element from document DOM tree, or
   * local map which contains queued elements pending
   * DOM insertion. Allows listeners, etc to be attached,
   * even if the element hasn't been appended yet.
   * @param aId the html element id
   */
, _getElementById: function (aId) {
    var lElem = document.getElementById(aId);
    return lElem ? lElem : this._targetDOMNodes[aId];
  }
  
  /**
   * Tests to see if the element is pending insertion or not
   * @param aId the html element id
   */
, _isPendingInsertion: function (aId) {
    return document.getElementById(aId) == null;
  }
}

// Immediately instantiate a single global instance of Synchroniser
var gSynchroniser = new Synchroniser();

/**
* Constructs an Ajax wrapper that copes with cross-browser pitfalls
* and abstracts some of the typical functionality required to make Ajax
* useful.
* @param aURL the URL to which the request should be dispatched
* @param aMethod request type {POST|GET|HEAD}
* @param aParams optional params (ampersand separated name/value pairs)
* @param aMsgBody optional message body (eg. XML)
* @param aContentType content type of message - defaulted for standard name/value POST
* @param aCallbackFunction function to run if request is successful (may not be null)
*/ 
function AjaxRequest (aURL, aMethod, aParams, aMsgBody, aContentType, aCallbackFunction) {
  // Set up the internal request
  var lXMLHttpRequest = this._newXMLHttpRequest();

  // Safety check, break construction if necessary
  if (lXMLHttpRequest == null) {
    throw "Could not construct XMLHttpRequest.";
  }
  
  // Belt and braces
  if (aCallbackFunction == null) {
    throw "No callback function specified";
  }
  else {
    this._callbackFunction = aCallbackFunction;
  }
  
  // Default content type to form post
  if (aMethod == "POST" && aContentType == null) {
    aContentType = "application/x-www-form-urlencoded";
  }
  
  // Decide where to put params and/or body
  if (aMsgBody != null) {
    // Message body exists, any params specified are appended to query string, if there are any
    if(aParams!=null) {
      aURL = aURL + "?" + aParams;
    }
  }
  else {
    // Message body is null, use params, i.e. for POST
    // (This is unused for GET/HEAD)
    aMsgBody = aParams;
  }
  
  // Branch based on method
  switch (aMethod) {
    case "POST": {
      // Open POST request and add headers
      lXMLHttpRequest.open(aMethod, aURL, true);
      lXMLHttpRequest.setRequestHeader("Content-Type", aContentType);
      lXMLHttpRequest.setRequestHeader("Content-Length", aMsgBody ? aMsgBody.length : 0);
      lXMLHttpRequest.setRequestHeader("Connection", "close");
      break;
    }
    case "HEAD":
    case "GET": {
      // Appended params as string, nullify message body
      aMsgBody = null;
      // Open standard request with modified URL
      lXMLHttpRequest.open(aMethod, aURL, true);
    }
  }
  
  // Self-reference for closure
  var self = this;
  
  // Set callback to internal callback
  lXMLHttpRequest.onreadystatechange = function () {
    self._callback(lXMLHttpRequest);
  };
  
  // Store whatever we need for later
  this._xmlHttpRequest = lXMLHttpRequest;
  this._msgBody = aMsgBody;
}

// Class body
AjaxRequest.prototype = 
{ 
  // Member variables
  _xmlHttpRequest: null
, _msgBody: null
, _callbackFunction: null

  /**
  * Initiates the request.
  */
, sendRequest: function () {
    this._xmlHttpRequest.send(this._msgBody);
  }
  
  /**
  * Checks a response code.
  * @param aCode HTTP status code to check
  */
, _responseOk: function (aCode) {
    // Desired codes
    // 200 - OK, 304 - Document not modified
    if ((aCode >= 200 && aCode < 300) || aCode == 304) {
      return true;
    }
    // Safari bug - returns undefined status if document is not modified
    else if (navigator.userAgent.indexOf("Safari") >= 0 && typeof aCode == "undefined") {
      return true;
    }
    // Unwanted code
    else {
      return false;
    }
  }

  /**
  * Standard internal callback handler for request.
  * @param aXMLHttpRequest the XMLHttpRequest self-reference
  */
, _callback: function (aXMLHttpRequest) {
    // Response received
    if (aXMLHttpRequest.readyState == 4) {
      var lCallbackData = null;
      try {
        // Set up arbitrary object wrapping useful data
        lCallbackData = {
          statusCode: aXMLHttpRequest.status
        , statusMsg: aXMLHttpRequest.statusText
        , textData: aXMLHttpRequest.responseText
        , xmlData: aXMLHttpRequest.responseXML != null ? aXMLHttpRequest.responseXML.documentElement.cloneNode(true).ownerDocument : null
        , responseOk: this._responseOk(aXMLHttpRequest.status)
        };
      }
      // Firefox and possibly other browsers throw when
      // networking exceptions occur. Rethrow with non-standard
      // error code.
      catch (e) {
        lCallbackData = {
            statusCode: -99
          , statusMsg: e
          , textData: null
          , xmlData: null
          , responseOk: false
        };
      }
      finally {
        // Clean up request object - prevent memory leak
        aXMLHttpRequest = null;
      }
      // Pass data to callback
      this._callbackFunction(lCallbackData);
    }
  }

  /**
  * Constructs a new XMLHttpRequest in a cross-browser way.
  * @return an XMLHttpRequest object
  */
, _newXMLHttpRequest: function () 
  {
    // Check for XMLHttpRequest and implement for MSIE
    if (typeof XMLHttpRequest == "undefined") {
      XMLHttpRequest = function () {
        return new ActiveXObject(
          navigator.userAgent.indexOf("MSIE 5") >= 0 ? 
          "Microsoft.XMLHTTP" : "Msxml2.XMLHTTP"
        );
      }
    }
    
    // Try to return a new instance
    try {
      return new XMLHttpRequest();
    }
    // IE failed to construct an XMLHttpRequest or non-IE browser without XMLHttpRequest
    catch (e) {
      alert("XMLHttpRequest not supported: " + e);
    }
  }
}
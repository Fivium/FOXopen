var gSpellAjaxTimeout;
var gPostSpellFunction;
var gParamArray;
var gSuccessMsg;
var gSpellCheckTimeout;
var gSpellCheckTimestamp;

function spellingXHRPost(pStrURL, pPostData, pCallback) {
  
  lAjaxRequest = new AjaxRequest (
    pStrURL
  , "POST"
  , null
  , pPostData
  , "text/xml"
  , pCallback
  );
  
  lAjaxRequest.sendRequest();
  
}

function removeSpellingDiv() {
  var loading = document.getElementById("spellchecking");
  loading.style.visibility = "hidden";
  loading.style.display = "none";
  var iframe = document.getElementById("iframe-wrapper");
  iframe.style.visibility = "hidden";
  iframe.style.display  = "none";
}

function spellComplete() {
  removeSpellingDiv();
  document.body.style.cursor = "default";
  window.setPageDisabled(false);
}

function spellScreen (aTimeStamp) {
  var lSpellDiv = document.getElementById("spellchecking");
  lSpellDiv.style.visibility = "visible";
  lSpellDiv.style.display = "block";
  var iframe = document.getElementById("iframe-wrapper");
  iframe.style.visibility = "visible";
  iframe.style.display  = "block";
  window.setPageDisabled(true);
  
  // take down the spellchecker screen if response not recieved within 20 seconds
  gSpellAjaxTimeout = window.setTimeout(function(){
    if(aTimeStamp == gSpellCheckTimestamp) { // Only bring veneer down for last known request
      gSpellCheckTimeout = true;
      spellComplete(lSpellDiv);
      alert("The spellcheck timed out.  Please try again or if the problem persists contact support.");
    }
  }, 1000*60); // 60 second timeout on spell check
  
  // stop resubmission in IE
  lSpellDiv.focus();
}

function ajaxSpellCheck(aSuccessMsg, aPostURL, aPostSpellFunction, aParamArray) {
  
  gSpellCheckTimestamp = new Date().getTime(); // set the global timestamp
  var lSpellCheckTimestamp = gSpellCheckTimestamp; // local copy for closure
  gSpellCheckTimeout = false;
  var lPostDOM = FoxDOM.newDOM("spellcheck");
  
  if(gSCArray.length == 0) {
    alert("None of the fields on this page are subject to a spell check.");
  }
  else {
    gPostSpellFunction = aPostSpellFunction;
    gParamArray = aParamArray;
    gSuccessMsg = aSuccessMsg;
    
    try {
      FoxDOM.appendChildToDOM(lPostDOM.documentElement, "dictionary", "en_GB"); // hard code dictionary for now
      var lFieldListDOM = FoxDOM.getCreate1E(lPostDOM, "/spellcheck/field-list");
      for(i in gSCArray) {
        var lFieldDOM = FoxDOM.appendChildToDOM(lFieldListDOM, "field");
        FoxDOM.appendChildToDOM(lFieldDOM, "name", gSCArray[i]);
        FoxDOM.appendChildToDOM(lFieldDOM, "value", getDataToSpellCheck(gSCArray[i]));
        if (document.mainForm.elements[gSCArray[i]].getAttribute('tinymce') == 'true') {
          FoxDOM.appendChildToDOM(lFieldDOM, 'tinymce', 'true');
        }
      }
    }
    catch (err1) {
      alert("Error: An unexpected error occurred while constructing spelling request");
    }
    
    spellingXHRPost(aPostURL, lPostDOM, function localCallback(pCallbackData){handleAjaxStatus(pCallbackData, lSpellCheckTimestamp)} );
    spellScreen(lSpellCheckTimestamp);
  }
    
}

function getDataToSpellCheck(pInputIdentifier) {
  var lElement = document.mainForm.elements[pInputIdentifier];
  if (lElement.getAttribute('tinymce') == 'true') {
    return tinyMCE.get(pInputIdentifier).getContent();
  }
  else {
    return lElement.value;
  }
}

function handleAjaxStatus(pCallbackData, aTimestamp) {
  
  window.clearTimeout(gSpellAjaxTimeout);
  
  // only respond to the last know ajax request to prevent running into problems regarding request timeout
  if(aTimestamp == gSpellCheckTimestamp) {
  
    try {
      if (pCallbackData.responseOk && pCallbackData.statusCode == 200) {
        
        // If the time out didnt run then handle the ajax response
        if(!gSpellCheckTimeout) {
          
          // If the spell checker has returned observations
          if(FoxDOM.getNodesByXPath(pCallbackData.xmlData, "/*/error").length) {
            alert("An error occured while trying to spellcheck.  Contact support if the problem persists.");
            spellComplete();
          }
          else if(FoxDOM.getNodesByXPath(pCallbackData.xmlData, "/*/field-list/field").length) {
            
            var lSpellCheck = new SpellCheck(
                document.body
              , "spell-checker-div"
              , pCallbackData.xmlData
              , function() {
                  //alert(gSuccessMsg);
                  spellComplete();
                  applySafe(this, gPostSpellFunction, gParamArray);
                }
              );
              
            lSpellCheck.processSpellingResults();
            removeSpellingDiv();
          }
          else {
            alert(gSuccessMsg);
            spellComplete();
            applySafe(this, gPostSpellFunction, gParamArray);
          }
        }
      }
      else {
        alert("An error occured while trying to spellcheck.  Contact support if the problem persists ("+ pCallbackData.statusCode+").");
        spellComplete();
      }
    }
    catch (ex){
      alert("An error occured while trying to spellcheck.  Contact support if the problem persists.");
      spellComplete();
    }
  } // correct timeout if
    
}


// Following generic window functions could be moved to foxjs
function getContentWidth() {
  return processWindowValues (
    window.innerWidth ? window.innerWidth : 0
  , document.documentElement ? document.documentElement.clientWidth : 0
  , document.body ? document.body.clientWidth : 0
  );
}

function getContentHeight() {
	return processWindowValues (
		window.innerHeight ? window.innerHeight : 0
	, document.documentElement ? document.documentElement.clientHeight : 0
	,	document.body ? document.body.clientHeight : 0
	);
}

function getHorizontalScroll() {
	return processWindowValues (
		window.pageXOffset ? window.pageXOffset : 0,
		document.documentElement ? document.documentElement.scrollLeft : 0,
		document.body ? document.body.scrollLeft : 0
	);
}

function getVerticalScroll() {
	return processWindowValues (
		window.pageYOffset ? window.pageYOffset : 0,
		document.documentElement ? document.documentElement.scrollTop : 0,
		document.body ? document.body.scrollTop : 0
	);
}

function processWindowValues(aWindow, aDocElem, aDocBody) {
	var lResult = aWindow ? aWindow : 0;
	if (aDocElem && (!lResult || (lResult > aDocElem))) {
		lResult = aDocElem;
  }
	return aDocBody && (!lResult || (lResult > aDocBody)) ? aDocBody : lResult;
}




function SpellCheck (aAttachDOM, aIdentifier, aSpellingResultXml, aCallback) {
  
  this._gSpellCheckObj = new Object();
  this._gSpellingXml = aSpellingResultXml;
  this._gCallback = aCallback;
  this._gIgnoreList  = new Array();
  this._gReplaceList = new Array();
  
  var self = this; // required to retain this meaning for event raised methods
  
  // Resolve owning document reference
  var lDocument = aAttachDOM.ownerDocument ? aAttachDOM.ownerDocument : aAttachDOM;
  var lTempDOM;
  var lTableDOM;
  var lTableBodyDOM;
  var lTableDataDOM;
  var lErrorInputDOM;
  var lButtonDOM;
  var lSelectorDOM;
  
  // BUILD THE HTML FOR THE SPELL CHECK WIDGET ON OBJECT CONSTRUCTION
  
  var lSpellWidth  = this._gSpellingDivWidth;
  var lSpellHeight = this._gSpellingDivHeight;
  var lSpellPositionTop  = (100 + getVerticalScroll()); // 100 pixels from the scroll top
  var lSpellPositionLeft = (getContentWidth()/2); // start windows left edge in middle of page content
  
  // Set up IFrame wrapper to prevent bleed through
  this._gIFrameWrapper = document.getElementById("spelling-iframe-wrapper");
  this._gIFrameWrapper.style.width  = (lSpellWidth + 4) + "px";
  this._gIFrameWrapper.style.height = (lSpellHeight + 4) + "px";
  this._gIFrameWrapper.style.top = (lSpellPositionTop - 2) + "px";
  this._gIFrameWrapper.style.left = (lSpellPositionLeft - 2) + "px";
  this._gIFrameWrapper.style.visibility = "visible";
  this._gIFrameWrapper.style.display = "block";
  
  // Main spelling div
  this._gMainSpellingDiv = lDocument.createElement("div");
  this._gMainSpellingDiv.setAttribute("id", aIdentifier);
  this._gMainSpellingDiv.setAttribute("name", aIdentifier);
  // Set the width and height explicitly here using constants (Important as computed style
  // can cause problems and style.width/height only works for inline style)
  this._gMainSpellingDiv.style.width  = lSpellWidth + "px";
  this._gMainSpellingDiv.style.height = lSpellHeight + "px";
  this._gMainSpellingDiv.style.top = lSpellPositionTop + "px";
  this._gMainSpellingDiv.style.left = lSpellPositionLeft + "px";
  this._gMainSpellingDiv.style.position = "absolute";
  this._gMainSpellingDiv.style.zIndex = 105;
  this._gMainSpellingDiv.className = "spellchecker";
  
  // Title bar
  lTempDOM = lDocument.createElement("div");
  lTempDOM.setAttribute("id", aIdentifier + "DragBar");
  lTempDOM.setAttribute("name", aIdentifier + "DragBar");
  lTempDOM.className = "spellcheckerDragBar";
  lTempDOM.appendChild(lDocument.createTextNode("Spell Checker"));
  assignEventListener(lTempDOM, "mousedown", function (aEvent) {self._startDrag(aEvent);}); // use a closure so this has correct meaning
  this._gMainSpellingDiv.appendChild(lTempDOM);
  
  // Table for layout
  lTableDOM = lDocument.createElement("table");
  lTableDOM.style.margin = "20px";
  
  // Table body
  lTableBodyDOM = lDocument.createElement("tbody");
  
  // Table Rows
  // row1
  lTempDOM = lDocument.createElement("tr");
  lTableDataDOM = lDocument.createElement("td");
  lTableDataDOM.appendChild(lDocument.createTextNode("Not in Dictionary"));
  lTempDOM.appendChild(lTableDataDOM);
  lTableDataDOM = lDocument.createElement("td");
  lTableDataDOM.colSpan = "2"
  lTempDOM.appendChild(lTableDataDOM);
  lTableBodyDOM.appendChild(lTempDOM);
  
  // row2
  lTempDOM = lDocument.createElement("tr");
  lTableDataDOM = lDocument.createElement("td");
  lTableDataDOM.colSpan = "2";
  lErrorInputDOM = lDocument.createElement("input");
  lErrorInputDOM.setAttribute("id", "unknown-spelling");
  lErrorInputDOM.setAttribute("type", "text");
  lErrorInputDOM.className = "unknown-spelling";
  assignEventListener(lErrorInputDOM, "keydown", function(aEvent){self.misspellEdit();} );
  this._gMispeltWordInput = lErrorInputDOM;
  lTableDataDOM.appendChild(lErrorInputDOM);
  lTempDOM.appendChild(lTableDataDOM);
  lTableDataDOM = lDocument.createElement("td");
  lTableDataDOM.rowSpan = "3";
  
  // Create the spelling GUI buttons
  lButtonDOM = lDocument.createElement("input");
  lButtonDOM.setAttribute("id", "spelling-ignore");
  lButtonDOM.setAttribute("type", "button");
  lButtonDOM.setAttribute("value", "Ignore Once");
  lButtonDOM.style.width = "100px";
  lButtonDOM.style.display = "inline";
  lButtonDOM.className = "button";
  assignEventListener(lButtonDOM, "click", function(aEvent){self.ignoreOccurrence();} );
  this._gIgnoreButton = lButtonDOM;
  lTableDataDOM.appendChild(lButtonDOM);
  // Undo edit button
  lButtonDOM = lDocument.createElement("input");
  lButtonDOM.setAttribute("id", "spelling-undo-edit");
  lButtonDOM.setAttribute("type", "button");
  lButtonDOM.setAttribute("value", "Undo Edit");
  lButtonDOM.style.width = "100px";
  lButtonDOM.style.display = "none";
  lButtonDOM.className = "button";
  assignEventListener(lButtonDOM, "click", function(aEvent){self.undoMisspellEdit();} );
  this._gUndoEditButton = lButtonDOM;
  lTableDataDOM.appendChild(lButtonDOM);
  lTableDataDOM.appendChild(lDocument.createElement("br"));
  
  lButtonDOM = lDocument.createElement("input");
  lButtonDOM.setAttribute("id", "spelling-ignore-all");
  lButtonDOM.setAttribute("type", "button");
  lButtonDOM.setAttribute("value", "Ignore All");
  lButtonDOM.style.width = "100px";
  lButtonDOM.className = "button";
  assignEventListener(lButtonDOM, "click", function(aEvent){self.ignoreEveryOccurrence();} );
  this._gIgnoreAllButton = lButtonDOM;
  lTableDataDOM.appendChild(lButtonDOM);
  lTableDataDOM.appendChild(lDocument.createElement("br"));
  
  lButtonDOM = lDocument.createElement("input");
  lButtonDOM.setAttribute("id", "spelling-replace");
  lButtonDOM.setAttribute("type", "button");
  lButtonDOM.setAttribute("value", "Replace");
  lButtonDOM.style.width = "100px";
  lButtonDOM.style.marginTop = "6px";
  lButtonDOM.className = "button";
  assignEventListener(lButtonDOM, "click", function(aEvent){self.replaceOccurrence();} );
  lTableDataDOM.appendChild(lButtonDOM);
  lTableDataDOM.appendChild(lDocument.createElement("br"));
  
  lButtonDOM = lDocument.createElement("input");
  lButtonDOM.setAttribute("id", "spelling-replace-all");
  lButtonDOM.setAttribute("type", "button");
  lButtonDOM.setAttribute("value", "Replace All");
  lButtonDOM.style.width = "100px";
  lButtonDOM.className = "button";
  assignEventListener(lButtonDOM, "click", function(aEvent){self.replaceEveryOccurrence();} );
  lTableDataDOM.appendChild(lButtonDOM);
  lTableDataDOM.appendChild(lDocument.createElement("br"));
  
  lButtonDOM = lDocument.createElement("input");
  lButtonDOM.setAttribute("id", "spelling-add");
  lButtonDOM.setAttribute("type", "button");
  lButtonDOM.setAttribute("value", "Add");
  lButtonDOM.style.width = "100px";
  lButtonDOM.style.marginTop = "6px";
  lButtonDOM.className = "button";
  lButtonDOM.style.display = "none"; // TODO - fill in implementation
  assignEventListener(lButtonDOM, "click", function(aEvent){self.addToDictionary();} );
  lTableDataDOM.appendChild(lButtonDOM);
  lTableDataDOM.appendChild(lDocument.createElement("br"));
  
  lButtonDOM = lDocument.createElement("input");
  lButtonDOM.setAttribute("id", "spelling-finish");
  lButtonDOM.setAttribute("type", "button");
  lButtonDOM.setAttribute("value", "Finish");
  // assignEventListener(lButtonDOM, "click", scrollPos);
  lButtonDOM.style.width = "100px";
  lButtonDOM.className = "button";
  assignEventListener(lButtonDOM, "click", function(aEvent){self.processRemainingItems();} );
  lTableDataDOM.appendChild(lButtonDOM);
  
  lTempDOM.appendChild(lTableDataDOM);
  lTableBodyDOM.appendChild(lTempDOM);
  
  // row3
  lTempDOM = lDocument.createElement("tr");
  lTableDataDOM = lDocument.createElement("td");
  lTableDataDOM.appendChild(lDocument.createTextNode("Suggestions"));
  lTempDOM.appendChild(lTableDataDOM);
  lTempDOM.appendChild(lDocument.createElement("td"));
  lTableBodyDOM.appendChild(lTempDOM);
  
  // row4
  lTempDOM = lDocument.createElement("tr");
  lTableDataDOM = lDocument.createElement("td");
  lTableDataDOM.colSpan = "2";
  lSelectorDOM = lDocument.createElement("select");
  lSelectorDOM.setAttribute("size", "6");
  lSelectorDOM.id = "suggestion-selector";
  lSelectorDOM.style.width = "300px";
  assignEventListener(lSelectorDOM, "dblclick", function(aEvent){self.replaceOccurrence();} );
  this._gSuggestionsSelector = lSelectorDOM;
  lTableDataDOM.appendChild(lSelectorDOM);
  lTempDOM.appendChild(lTableDataDOM);
  lTableBodyDOM.appendChild(lTempDOM);
  
  // row5
  lTempDOM = lDocument.createElement("tr");
  lTableDataDOM = lDocument.createElement("td");
  lTableDataDOM.colSpan = "3";
  lMsgArea = lDocument.createElement("span");
  lMsgArea.id = "spell-message-area";
  this._gMessageArea = lMsgArea;
  lTableDataDOM.appendChild(lMsgArea);
  lTempDOM.appendChild(lTableDataDOM);
  lTableBodyDOM.appendChild(lTempDOM);
  
  // add the body to table and table to div
  lTableDOM.appendChild(lTableBodyDOM);
  this._gMainSpellingDiv.appendChild(lTableDOM);
  
  // attach the spelling div DOM to the specified attach point
  aAttachDOM.appendChild(this._gMainSpellingDiv);
  
  this._gSpellCheckObj.spellingDiv = document.getElementById(aIdentifier);
}

// Class body
SpellCheck.prototype = 
{
  // Static variables
  _gSpellingDivWidth: 450
, _gSpellingDivHeight: 250

  // Member variables
, _gSpellingXml: null
, _gSpellCheckObj: null
, _gCallback: null
, _gIgnoreList: null
, _gReplaceList: null

, _gDragEvent: null
, _gDragEndEvent: null

, _gUndoEditButton: null
, _gIgnoreButton: null
, _gIgnoreAllButton: null
, _gIFrameWrapper: null
, _gMainSpellingDiv: null
, _gMispeltWordInput: null
, _gSuggestionsSelector: null
, _gMessageArea: null

, _gFieldListing: null
, _gFieldIndex: null
, _gContextElemDOM: null
, _gTinyMCE: null
, _gUnknownTerms: null
, _gUnknownIndex: null
, _gUnknownTermObj: null
, _gFieldOffset: 0

  // methods
, _startDrag: function(aEvent)
  {
    
    aEvent = (!aEvent) ? window.event : aEvent;
    
    var lx, ly;
    
    // Get cursor position accounting for scroll
    lx = (aEvent.clientX ? aEvent.clientX : window.event.clientX) + getHorizontalScroll();
    ly = (aEvent.clientY ? aEvent.clientY : window.event.clientY) + getVerticalScroll();
    
    // Save starting positions of cursor and element.
    this._gSpellCheckObj.cursorStartX = lx;
    this._gSpellCheckObj.cursorStartY = ly;
    this._gSpellCheckObj.divStartX    = parseInt(this._gSpellCheckObj.spellingDiv.style.left, 10);
    this._gSpellCheckObj.divStartY    = parseInt(this._gSpellCheckObj.spellingDiv.style.top,  10);
  
    if (isNaN(this._gSpellCheckObj.DivStartLeft)) this._gSpellCheckObj.DivStartLeft = 0;
    if (isNaN(this._gSpellCheckObj.DivStartTop))  this._gSpellCheckObj.DivStartTop  = 0;
    
    // remove any previously attached events
    if(this._gDragEvent) {
      unassignEventListener(document, "mousemove", this._gDragEvent);
    }
    if(this._gDragEndEvent) {
      unassignEventListener(document, "onmouseup", this._gDragEndEvent);
    }
    
    var self = this;
    this._gDragEvent = function(aEvent) {self._drag(aEvent);};
    this._gDragEndEvent = function(aEvent) {self._dragEnd(aEvent);};
  
    // Capture mousemove and mouseup events on the page.
    assignEventListener(document, "mousemove", this._gDragEvent);
    assignEventListener(document, "mouseup", this._gDragEndEvent);
    
    // prevent event propagation by ending this event here
    if (window.event) {
      window.event.cancelBubble = true;
      window.event.returnValue = false;
    }
    else {
      aEvent.preventDefault();
    }
    
  }
  
, _dragEnd: function(aEvent)
  {
    // deregister mouse events
    unassignEventListener(document, "mousemove", this._gDragEvent);
    unassignEventListener(document, "onmouseup", this._gDragEndEvent);
  }

, _drag: function(aEvent)
  {

    var lx, ly;
  
    // Get cursor position accounting for scroll
    lx = (aEvent.clientX ? aEvent.clientX : window.event.clientX) + getHorizontalScroll();
    ly = (aEvent.clientY ? aEvent.clientY : window.event.clientY) + getVerticalScroll();
    
    var lNewX = (lx + this._gSpellCheckObj.divStartX - this._gSpellCheckObj.cursorStartX);
    var lNewY = (ly + this._gSpellCheckObj.divStartY - this._gSpellCheckObj.cursorStartY);
    
    // Work out max x and y accounting for scroll (minus 10 px to prevent scroll bars appearing in some browsers)
    var lMaxPositionX = getContentWidth()  - this._gSpellingDivWidth + getHorizontalScroll() - 10;
    var lMaxPositionY = getContentHeight() - this._gSpellingDivHeight + getVerticalScroll() - 10;
    
    // ensure that the spell check window remains within scrollable area
    lNewX = (lNewX >= lMaxPositionX) ? lMaxPositionX : ((lNewX < 0) ? 0 : lNewX);
    lNewY = (lNewY >= lMaxPositionY) ? lMaxPositionY : ((lNewY < 0) ? 0 : lNewY);
    
    // Adjust spelling div position but stop it going off the top of the screen
    this._gSpellCheckObj.spellingDiv.style.left = lNewX + "px";
    this._gSpellCheckObj.spellingDiv.style.top  = lNewY + "px";
    
    // Move the Wrapping IFrame with the div to prevent selector bleed through
    this._gIFrameWrapper.style.left = (lNewX - 1) + "px";
    this._gIFrameWrapper.style.top  = (lNewY - 1) + "px";
    
    // prevent event propagation by ending this event here
    if (window.event) {
      window.event.cancelBubble = true;
      window.event.returnValue = false;
    }
    else {
      aEvent.preventDefault();
    }
  }
  
, _NextUnknown: function()
  {
    // first time in we need to bootstrap a field
    if(this._gFieldIndex == null) {
      this._nextField();
    }
    
    // first time in for field extract unknown words
    if(this._gUnknownIndex == null) {
      this._gUnknownTerms = FoxDOM.getNodesByXPath(this._gFieldListing[this._gFieldIndex], "unknown-word-list/unknown-word");
      this._gUnknownIndex = 0;
    }
    else {
      // Otherwise increment unknown word pointer
      this._gUnknownIndex = ++this._gUnknownIndex;
    }
    
    // on processing all field unknowns try and get next field
    if(this._gUnknownIndex == this._gUnknownTerms.length) {
      if(this._nextField()) {
        this._gUnknownTerms = FoxDOM.getNodesByXPath(this._gFieldListing[this._gFieldIndex], "unknown-word-list/unknown-word"); // refresh unknowns for new field
        this._gUnknownIndex = 0; // reset unknown pointer
      }
      else {
        return;
      }
    }
    
    var lUnkownObj;
    if (this._gTinyMCE) {
      lUnkownObj = {
        gUnknownTerm: FoxDOM.get1SByXPath(this._gUnknownTerms[this._gUnknownIndex], "word/text()")
      , gOffsets: FoxDOM.get1SByXPath(this._gUnknownTerms[this._gUnknownIndex], "offsets/text()").split(',')
      , gSuggectionList: FoxDOM.getNodesByXPath(this._gUnknownTerms[this._gUnknownIndex], "suggestion-list/suggestion")
      };
    }
    else {
      lUnkownObj = {
        gUnknownTerm: FoxDOM.get1SByXPath(this._gUnknownTerms[this._gUnknownIndex], "word/text()")
      , gStartIndex: FoxDOM.get1SByXPath(this._gUnknownTerms[this._gUnknownIndex], "index-start/text()")
      , gEndIndex: FoxDOM.get1SByXPath(this._gUnknownTerms[this._gUnknownIndex], "index-end/text()")
      , gSuggectionList: FoxDOM.getNodesByXPath(this._gUnknownTerms[this._gUnknownIndex], "suggestion-list/suggestion")
      };
    }
    
    return lUnkownObj;
  }

, _nextField: function()
  {
    // If the current field is a tiny MCE one, clear any slect spans
    if (this._gTinyMCE) {
      this._deselectTinyMCE();
    }

    // first time round load the field array and set pointer
    if(this._gFieldIndex == null) {
      this._gFieldListing = FoxDOM.getNodesByXPath(this._gSpellingXml, "/*/field-list/field");
      this._gFieldIndex = 0;
    }
    else {
      this._gFieldIndex = ++this._gFieldIndex;
    }
    
    if(this._gFieldIndex == this._gFieldListing.length) {
      return false;
    }
    
    // Attach context to current field being checked
    var name = FoxDOM.get1SByXPath(this._gFieldListing[this._gFieldIndex], "name/text()");
    this._gContextElemDOM = document.getElementById(name);
    
    this._gTinyMCE = (this._gContextElemDOM.getAttribute('tinymce') == 'true');
    
    this._gFieldOffset = 0;
  
    return true;
  }
  
, _resolveNewTerm: function() {
    var lReplaceWith;
    
    if(this._gSuggestionsSelector.disabled) {
      lReplaceWith = this._gMispeltWordInput.value;
    }
    else if(this._gSuggestionsSelector.selectedIndex >= 0) {
      lReplaceWith =  this._gSuggestionsSelector.options[this._gSuggestionsSelector.selectedIndex].value;
    }
    else {
      lReplaceWith = null;
    }
    return lReplaceWith;
  }
  
, _replaceWithNewTerm: function (aReplaceWithTerm)
  {
    if (aReplaceWithTerm == undefined || aReplaceWithTerm == null || aReplaceWithTerm == '') {
      return;
    }
    if (this._gTinyMCE) {
      // If the current field is a tiny MCE one, clear any slect spans
      this._deselectTinyMCE();

      // Do spellcheck replace
      var lTinyMCEInstance = tinymce.get(this._gContextElemDOM.name);
      var lOriginalContent = lTinyMCEInstance.getContent();
      var lReplacedContent = lOriginalContent.split('');
      var lOffset;
      var lOffsetOffset = this._gFieldOffset; // Offset the character offsets by a field-level value

      // Replace old word characters and add new ones if the replace word in bigger
      for (var i = 0; i < aReplaceWithTerm.length; i++) {
        lO = parseInt(this._gUnknownTermObj.gOffsets[i]) + lOffsetOffset;
        if (i < this._gUnknownTermObj.gOffsets.length) {
          // Replace characters
          lOffset = parseInt(this._gUnknownTermObj.gOffsets[i]) + lOffsetOffset;
          lReplacedContent[lOffset] = aReplaceWithTerm.substr(i, 1);
        }
        else {
          // Add characters at the end
          lOffset++;
          lReplacedContent.splice(lOffset, 0, aReplaceWithTerm.substr(i, 1));
        }
      }
      // Remove any old word characters if the replace word is smaller
      if (aReplaceWithTerm.length < this._gUnknownTermObj.gUnknownTerm.length) {
        for (var i = aReplaceWithTerm.length; i < this._gUnknownTermObj.gUnknownTerm.length; i++) {
          lOffset = parseInt(this._gUnknownTermObj.gOffsets[i]) + lOffsetOffset;
          lReplacedContent.splice(lOffset, 1);
          lOffsetOffset--; // Decrement our offsets by one as we've removed a character
        }
      }
      lTinyMCEInstance.setContent(lReplacedContent.join(''));

      this._gFieldOffset += (aReplaceWithTerm.length - this._gUnknownTermObj.gUnknownTerm.length);
    }
    else {
      var lEnd, lBegining, lDomValue, lTextToReplace;
      var lDocument;
      var lDifferenceInLength;
      
      lDomValue = this._gContextElemDOM.value.replace(/\r\n/g, "\n"); // remove and MS \r\n linefeeds
      
      lTextToReplace = lDomValue.substring(this._gUnknownTermObj.gStartIndex, this._gUnknownTermObj.gEndIndex);
      
      // verify that the text to be replaced matches what we expect it to be
      if(lTextToReplace == this._gUnknownTermObj.gUnknownTerm) {
        lBegining = lDomValue.slice(0,this._gUnknownTermObj.gStartIndex);
        lEnd = lDomValue.slice(this._gUnknownTermObj.gEndIndex);
        
        lDifferenceInLength = aReplaceWithTerm.length - this._gUnknownTermObj.gUnknownTerm.length;

        this._gFieldOffset += lDifferenceInLength;

        var newSentence = lBegining + aReplaceWithTerm + lEnd;
        this._gContextElemDOM.value = newSentence;
        
        lDocument = this._gUnknownTerms[this._gUnknownIndex].ownerDocument ? this._gUnknownTerms[this._gUnknownIndex].ownerDocument : this._gUnknownTerms[this._gUnknownIndex];
        
        // Adjust index values
        for(var i = this._gUnknownIndex + 1; i < this._gUnknownTerms.length; i++) {
          var lIndexStartDOM = FoxDOM.getNodesByXPath(this._gUnknownTerms[i], "index-start/text()")[0];
          var lIndexEndDOM   = FoxDOM.getNodesByXPath(this._gUnknownTerms[i], "index-end/text()")[0];
          
          // Repoint start index
          FoxDOM.getNodesByXPath(this._gUnknownTerms[i], "index-start")[0].removeChild(lIndexStartDOM);
          FoxDOM.getNodesByXPath(this._gUnknownTerms[i], "index-start")[0].appendChild(lDocument.createTextNode(parseInt(lIndexStartDOM.nodeValue) + lDifferenceInLength));
          
          // Repoint end index
          FoxDOM.getNodesByXPath(this._gUnknownTerms[i], "index-end")[0].removeChild(lIndexEndDOM);
          FoxDOM.getNodesByXPath(this._gUnknownTerms[i], "index-end")[0].appendChild(lDocument.createTextNode(parseInt(lIndexEndDOM.nodeValue) + lDifferenceInLength));
        }
      }
      else {
        alert("An unexpected error occured and the spellcheck failed to replace the term '" + this._gUnknownTermObj.gUnknownTerm + "'");
      }
    }
  }
  
, _processKnownTerms: function()
  {
    // while next term exists in replace or ignore list
    while(this._gUnknownTermObj && (this._gReplaceList[this._gUnknownTermObj.gUnknownTerm] || this._gIgnoreList[this._gUnknownTermObj.gUnknownTerm]) ) {
      // replace term if in replace list otherwise ignore
      if (this._gReplaceList[this._gUnknownTermObj.gUnknownTerm]) {
        this._replaceWithNewTerm(this._gReplaceList[this._gUnknownTermObj.gUnknownTerm]);
      }
      this._gUnknownTermObj = this._NextUnknown(); // use internal and don't update GUI
    }
  }
  
, _refreshGUI: function()
  {
    var lDocument= null;
    var lOptionDOM;
    var lOptionList;
    
    // if an unkown has been returned
    if(this._gUnknownTermObj) {
    
      lDocument = this._gContextElemDOM.ownerDocument ? this._gContextElemDOM.ownerDocument : this._gContextElemDOM;
      
      this._gMispeltWordInput.style.color = "#FF0000";
      
      // Set the mispelt word in the GUI
      this._gMispeltWordInput.value = this._gUnknownTermObj.gUnknownTerm;
      
      // clear down selector
      lOptionList = this._gSuggestionsSelector.childNodes;
      for(var i = lOptionList.length-1; i >= 0; i--) {
        this._gSuggestionsSelector.removeChild(lOptionList[i]);
      }
      
      // do we have any suggestions
      if(this._gUnknownTermObj.gSuggectionList.length == 0) {
        lOptionDOM = lDocument.createElement("option");
        lOptionDOM.appendChild(lDocument.createTextNode("[no suggestions]"));
        this._gSuggestionsSelector.appendChild(lOptionDOM);
        this._gSuggestionsSelector.options[0].selected = false;
        this._gSuggestionsSelector.disabled = true;
      }
      else {
        // Populate the selector with new suggestions
        for(var i = 0; i < this._gUnknownTermObj.gSuggectionList.length; i++) {        
          var lSuggestedValue = FoxDOM.get1SByXPath(this._gUnknownTermObj.gSuggectionList[i], "text()");
          lOptionDOM = lDocument.createElement("option");
          lOptionDOM.value = lSuggestedValue;
          lOptionDOM.appendChild(lDocument.createTextNode(lSuggestedValue));
          this._gSuggestionsSelector.appendChild(lOptionDOM);
        }
        
        // Select first suggestion
        this._gSuggestionsSelector.selectedIndex = 0;
      }
      
      this._enableSelection();
      
      var lBeforeFocusTop  = getVerticalScroll();
      var lBeforeFocusLeft = getHorizontalScroll();
      
      // focus on the field containing this word (may need to move GUI here)
      if (this._gTinyMCE) {
        var lTinyMCEInstance = tinymce.get(this._gContextElemDOM.name);

        // Remove previous selections (only select one word at a time)
        this._deselectTinyMCE();
        
        // Wrap word in selection span
        var lWordStart = parseInt(this._gUnknownTermObj.gOffsets[0]) + this._gFieldOffset;
        var lWordEnd = parseInt(this._gUnknownTermObj.gOffsets[this._gUnknownTermObj.gOffsets.length-1]) + 1 + this._gFieldOffset;
        var lOriginalContent = lTinyMCEInstance.getContent();
        var lSelectableContent = lOriginalContent.substring(0, lWordStart);
        lSelectableContent += '<span id="fox_selected">';
        lSelectableContent += lOriginalContent.substring(lWordStart, lWordEnd);
        lSelectableContent += '</span>';
        lSelectableContent += lOriginalContent.substring(lWordEnd);
        lTinyMCEInstance.setContent(lSelectableContent);
     
        // Select selection span
        lTinyMCEInstance.focus();
        lTinyMCEInstance.selection.select(lTinyMCEInstance.dom.get('fox_selected'));
      }
      else {
        setSelection (this._gContextElemDOM, parseInt(this._gUnknownTermObj.gStartIndex), parseInt(this._gUnknownTermObj.gEndIndex));
      }
      
      var lNewTop  = (parseInt(this._gMainSpellingDiv.style.top)  + (getVerticalScroll()-lBeforeFocusTop));
      var lNewLeft = (parseInt(this._gMainSpellingDiv.style.left) + (getHorizontalScroll()-lBeforeFocusLeft));
      
      // reposition spelling div
      this._gMainSpellingDiv.style.top  = lNewTop + "px"; // top position
      this._gMainSpellingDiv.style.left = lNewLeft + "px"; // left position
      
      // reposition wrapper IFrame
      this._gIFrameWrapper.style.top  = (lNewTop  - 1) + "px";
      this._gIFrameWrapper.style.left = (lNewLeft - 1) + "px";
    }
    else {
      this.finishSpellChecking();
    }
  }
, _deselectTinyMCE: function()
  {
    var lTinyMCEInstance = tinymce.get(this._gContextElemDOM.name);
    var lSelectSpan = lTinyMCEInstance.dom.get('fox_selected');
    if (lSelectSpan != null) {
      var lChildren = lSelectSpan.childNodes;
      var lChildLength = lChildren.length;
      for (var i = 0; i < lChildLength;  i++) {
        lSelectSpan.parentNode.insertBefore(lChildren[0], lSelectSpan);
      }
      lSelectSpan.parentNode.removeChild(lSelectSpan);
    }
  }
, _disableSelection: function()
  {
    this._gUndoEditButton.style.display = "inline";
    this._gIgnoreButton.style.display = "none";
    this._gIgnoreAllButton.disabled = true;
    this._gSuggestionsSelector.disabled = true;
  }
  
, _enableSelection: function()
  {
    if(this._gUnknownTermObj.gSuggectionList.length > 0) {
      this._gSuggestionsSelector.disabled = false;
    }
    this._gUndoEditButton.style.display = "none";
    this._gIgnoreButton.style.display = "inline";
    this._gIgnoreAllButton.disabled = false;
  }
  
, processSpellingResults: function()
  {
    // load the field array
    this.getNextUnknown();
    this._refreshGUI();
  }
  
, getNextUnknown: function()
  {
    this._gUnknownTermObj = this._NextUnknown();
  }
  
, ignoreOccurrence: function()
  {
    if (this._gTinyMCE) {
      this._deselectTinyMCE();
    }
    this.getNextUnknown();
    if(this._gUnknownTermObj) {
      this._processKnownTerms();
    }
    this._refreshGUI();
  }
  
, undoMisspellEdit: function()
  {
    this._gMispeltWordInput.value = this._gUnknownTermObj.gUnknownTerm;
    this._enableSelection();
    this._gMispeltWordInput.style.color = "#FF0000";
  }

, misspellEdit: function()
  {
    this._gMispeltWordInput.style.color = "#000000";
    this._disableSelection();
  }
  
, ignoreEveryOccurrence: function()
  {
    // populate ignore list
    this._gIgnoreList[this._gUnknownTermObj.gUnknownTerm] = 1;
    this.ignoreOccurrence();
  }
  
, replaceOccurrence: function()
  {
    var lReplaceWith = this._resolveNewTerm();
    
    if(lReplaceWith != null) {
      // perform replace
      this._replaceWithNewTerm(lReplaceWith);
      this.getNextUnknown();
      if(this._gUnknownTermObj) {
        this._processKnownTerms();
      }
      this._refreshGUI();
    }
    else {
      alert("To replace a spelling mistake you must indicate an alternative value.");
    }
  }
  
, replaceEveryOccurrence: function()
  {
    var lReplaceWith = this._resolveNewTerm();
    
    if(lReplaceWith != null) {
      // populate replace list
      this._gReplaceList[this._gUnknownTermObj.gUnknownTerm] = lReplaceWith;
      this.replaceOccurrence();

      var lReplaceList = [];
      for(var key in this._gReplaceList){
        lReplaceList.push(key);
      }
      while (this._gMessageArea.hasChildNodes()) {
        this._gMessageArea.removeChild(this._gMessageArea.lastChild);
      }
      this._gMessageArea.appendChild(document.createTextNode('All occurrences of "' + lReplaceList.join('", "') +  '" will be replaced.'));
    }
    else {
      alert("To replace a spelling mistake you must indicate an alternative value.");
    }
  }
  
, addToDictionary: function()
  {
    // TODO - create a ajax request to add word into users stop list
    alert("Not implemented");
  }

, processRemainingItems: function()
  {
    var lCount = 0;
    for(i in this._gReplaceList) {
      lCount++;
      break;
    }
  
    if(lCount > 0) {
      // while unknowns exist loop through and ensure that we have replace all occurances in replace all list
      while(this._gUnknownTermObj) {
        // get the next term
        this._gUnknownTermObj = this._NextUnknown();
        this._processKnownTerms();
      }
    }
    this.finishSpellChecking();
  }

, finishSpellChecking: function()
  {
    if (this._gTinyMCE) {
      this._deselectTinyMCE();
    }
    var lContainingDOM = this._gMainSpellingDiv.parentNode;
    lContainingDOM.removeChild(this._gMainSpellingDiv);
    this._gIFrameWrapper.style.visibility = "hidden";
    this._gIFrameWrapper.style.display = "none";
    this._gCallback();
    this._gCallback = null;
  }
  
}

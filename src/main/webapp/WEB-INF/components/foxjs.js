// Global
var gBeforeSubmitEvents = new Array();
var linksDivFader;
var gPresentationPreservationElems = new Array();
var gFocusWindowOnUpdateComplete;

// Moved from HtmlGenerator
function rs (nt) {
  replaceStatus(nt);
  return true;
}

function replaceStatus(newText)
{
  self.status=newText;
  return true;
}

// Replace Status 's for all the links
function ars() {
  for (var i=0;i<document.links.length;i++) {
    if (!document.links[i].onmouseover) {
      document.links[i].onmouseover = function() {
        try {
          rs('');
        } catch (e) {}
        return true;
      }
    }
    if (!document.links[i].onmouseout) {
      document.links[i].onmouseout = function() {
        try {
          rs('');
        } catch (e) {}
        return true;
      }
    }
  }
}

// Create Code for the drop downs
function cc(groupid,ac,mapset,u,selected,multi,onchg,listSize,disabled,mapsetIndexes,historical) {
  var options= "";
  var s = "";
  var real = mapset.length;
  var realoffset = 0;
  for (var c=0;c<u.length/3;c++) {
    var pos = c*3+1;
    if (u[pos]!=null&&(u[pos].indexOf("/") != -1)) {
      real++;
      realoffset++;
    }
  }

  var noUfound = 0;  
  // Loop around the normal real values
  for (var i=0; i<real; i++) {
    var ref = null;
    var tail = "R"+ac+"/"+mapsetIndexes[i-realoffset];
    // Build up buffer, we don't know if we want to output yet
    var option_buffer = "<option ";
    var charVar = "O";
    for (var c=0;c<u.length/3;c++) {
      var pos = c*3+1;
      if (u[pos]!=null&&u[pos]==tail) {
        charVar=u[pos-1];
        ref=u[pos+1];
        noUfound++;
        u[pos]=null;
        break;
      }
    }
    if (ref == null) {
      ref = mapset[i-noUfound];
    }
    option_buffer += "value=\""+charVar+tail+"\"";
    
    var selected_flag = false;
    // Check to see if any of the items are selected
    for (var s=0;s<selected.length;s++) {
      if (selected[s]!=null&&selected[s]==tail) {
        option_buffer += " selected=''";
        selected_flag = true;
        selected[s]=null;
      }
    }
    option_buffer += ">"+ref+"</option>\n";
    // Only output historical values if they are selected for this instance
    if(historical[i-realoffset] == false || selected_flag == true) {
      options += option_buffer;
    }
  }
  // Loop around the specials
  for (var c=0;c<u.length/3;c++) {
    var pos = c*3+1;
    if (u[pos]!=null) {
       var fin = "";
      if (u[pos-1]!="U") {
        fin += options;
      }
      fin += "<option value='"+u[pos-1]+u[pos]+"'";
      for (var s=0;s<selected.length;s++) {
        if (selected[s]!=null&&selected[s]==u[pos]) {
          fin += "selected=''";
          selected[s]=null;
        }
      }
      fin += ">"+u[pos+1]+"</option>\n";
      if (u[pos-1]=="U") {
        fin += options;
      }
      u[pos]=null;
      options = fin;
    }
  }
  selectTag = "<select name=\""+groupid+"\" id=\""+groupid+"\" onmousewheel=\"return false;\"";
  if (multi) {
    selectTag += " multiple=''";
  }
  if (listSize) {
    selectTag += " size=\""+listSize+"\"";
  }
  if (disabled) {
    selectTag += " disabled=''";
  }
  selectTag += ">\n"+options+"</select>";
  document.write(selectTag);
  if (onchg) {
    var changeScript = "<script type=\"text/javascript\">$(\"#"+groupid+"\").change(function() {"+onchg+"});</script>";
    document.write(changeScript);
  }
}

// Screen loaded
function ld() {
  var loading = document.getElementById("loading");
  loading.style.visibility = "hidden";
  loading.style.display = "none";
  var iframe = document.getElementById("iframe-wrapper");
  iframe.style.visibility = "hidden";
  iframe.style.display  = "none";
  document.body.style.cursor = "default";
}

// Loading screen
function ldScreen () {
  var loading = document.getElementById("loading");
  loading.style.visibility = "visible";
  loading.style.display = "block";
  var iframe = document.getElementById("iframe-wrapper");
  iframe.style.visibility = "visible";
  iframe.style.display  = "block";
  document.body.style.cursor = "wait";
}

// Updating screen
function upScreen()
{
  var updating = document.getElementById("updating");
  updating.style.visibility = "visible";
  updating.style.display = "block";
  var iframe = document.getElementById("iframe-wrapper");
  iframe.style.visibility = "visible";
  iframe.style.display  = "block";
  // update image url to itself - fixes IE animation haltage
  updating.firstChild.src = updating.firstChild.src;
  document.body.style.cursor = "wait";
  window.setPageDisabled(true);
  window.setTimeout(function(){
    var closeButton = document.getElementById("updating-close-button");
    closeButton.style.visibility = "visible";
    closeButton.style.display = "block";
  }, 1000*60*5); // 5 minutes
  // stop resubmission in IE
  updating.focus();

  //used to re-focus upload window so user can see green tick etc
  //on completion of an upload with a completion action
  if(gFocusWindowOnUpdateComplete != null) {
    gFocusWindowOnUpdateComplete.focus();
    gFocusWindowOnUpdateComplete = null;
  }
}

function closeStatusDiv (aTarget)
{
  aTarget.parentNode.style.visibility = "hidden";
  aTarget.parentNode.style.display = "none";
  aTarget.style.visibility = "hidden";
  aTarget.style.display = "none";
  var iframe = document.getElementById("iframe-wrapper");
  iframe.style.visibility = "hidden";
  iframe.style.display  = "none";
  window.setPageDisabled(false);
  setPageExpired(gPageExpired);
  document.body.style.cursor = "default";
}

/*
 * cross browser scroll position
*/
function getScrollPosition ()
{
  // Firefox et al
  if (typeof(window.pageYOffset) != "undefined") {
    return window.pageYOffset;
  }
  // IE differs based on doctype, so if this returns 0, try looking for a non-zero value in documentElement
  else if (typeof(document.body.scrollTop) != "undefined" && document.body.scrollTop > 0) {
    return document.body.scrollTop;
  }
  else if (typeof(document.documentElement.scrollTop) != "undefined" && document.documentElement.scrollTop > 0) {
    return document.documentElement.scrollTop;
  }
  return 0;
}

//Finds y value of given object
function findPos(obj) {
	var curtop = 0;
	if (obj.offsetParent) {
		do {
			curtop += obj.offsetTop;
		} while (obj = obj.offsetParent);
	return [curtop];
	}
}

function submitSection(actionValue, contextValue)
{
  var stopNavigation = pageExpired();
  // detect erroneous navigation
  if (stopNavigation)
  {
    var goForward = confirm("Warning: this page has expired.\n\nIt looks like you have navigated back to this page using the browser back button. You will need to navigate forwards to the most recent page before you can activate any links or buttons.\n\nClick OK to be taken back to your current workflow position.\nClick CANCEL to remain on this expired page.");

    // A bit ugly, but worst case scenario is that we go nowhere and user has to use browser forward button
    // If problematic, could add a form POST timeout to make sure users go somewhere
    if (goForward)
    {
      // IE, Opera will go to top of history stack with this
      // assuming < 999 pages navigated
      for (var i = 999; i > 0; i--) {
        window.history.go(i);
      }

      // Firefox prefers this instead
      for (var i = 1; i <= 999; i++) {
        window.history.go(i);
      }
      return false;
    }
    else {
      return false;
    }
  }

  if (!gPageDisabled) {
    var scrollPos = getScrollPosition();
    document.mainForm.scroll_position.value = scrollPos;
    document.mainForm.action_name.value = actionValue;
    document.mainForm.context_ref.value = contextValue;
    document.mainForm.submit_count.value = parseInt(document.mainForm.submit_count.value)+1;
    // process HTMLArea code, or anything else pre-submit
    document.mainForm.onsubmit();
    // Call all requested functions
    for (var e in gBeforeSubmitEvents) {
      gBeforeSubmitEvents[e].call(window); // window context
    }
    // POST form
    document.mainForm.submit();
    // show "loading/updating"
    window.setTimeout(function(){
      // must run after form submit otherwise IE halts animation
      upScreen();
    },0);
  }
}

function reset() {
 document.main_form.reset();
}

function confirmExit() {
  var msg = "Are you sure you want to exit this application?";
  if (confirm(msg)) {
    window.close();
  }	
}

function windowOpenSafe(aURL, aWindowName, aJSWinFeatures) {
  window.open(aURL.replace(/&amp;/g,"&"), aWindowName, aJSWinFeatures);
}

function openwin (aURL, aWinType)
{
  if (aWinType == "appwin") {
    windowOpenSafe(aURL, '', "toolbar=0,location=0,directories=0,status=1,menubar=0,scrollbars=1,resizable=1,bgcolor=#003399,left=0,top=0,width=" + (screen.availWidth-10) + ",height=" + (screen.availHeight-25));
  }
  if (aWinType == "searchwin") {
    windowOpenSafe(aURL, aWinType, "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=1,resizable=0,bgcolor=#003399,width=600,height=700,left=100,top=10");
  }
  if (aWinType.indexOf("filewin") >= 0) {
    windowOpenSafe(aURL, aWinType, "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=1,resizable=1,bgcolor=#003399,width=1013,height=680,left=100,top=10");
  }
  if (aWinType == "fullwin") {
    windowOpenSafe(aURL, '', "toolbar=1,location=1,directories=1,status=1,menubar=1,scrollbars=1,resizable=1,bgcolor=#003399,width=900,height=700,left=50,top=10");
  }
  if (aWinType == "refwin") {
    windowOpenSafe(aURL, aWinType, "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=1,resizable=1,bgcolor=#000066,width=800,height=600,left=100,top=75");
  }
  if (aWinType == "helpwin") {
    windowOpenSafe(aURL, '', "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=1,resizable=1,bgcolor=#003399,width=600,height=500,left=100,top=10");
  }
  if (aWinType == "flushwin") {
    windowOpenSafe(aURL, aWinType, "toolbar=1,location=0,directories=0,status=0,menubar=1,scrollbars=1,resizable=1,bgcolor=#003399,width=638,height=105,left=100,top=100");
  }
}


function confirmSubmitSection(aText, aFunction, aParamArray)
{
  if (confirm(aText)) {
    applySafe(this, aFunction, aParamArray);
  }
}


function applySafe(aContext, aFunction, aParamArray) {
  if(aFunction != null) {
    var lParamArray = (aParamArray == null) ? new Array() : aParamArray;
    aFunction.apply(aContext, aParamArray);
  }
}


function focusSubmitSection(actionValue, contextValue, focusValue) {
 document.mainForm.auto_focus.value = focusValue;
 submitSection(actionValue, contextValue);
}


function addClass(aTarget, aClassValue) {    
  var pattern = new RegExp("(^| )" + aClassValue + "( |$)");    
  if (!pattern.test(aTarget.className)) {
    if (aTarget.className == "") {    
      aTarget.className = aClassValue;    
    } else {    
      aTarget.className += " " + aClassValue;    
    }    
 }    
 return true;    
}

function removeClass(aTarget, aClassValue) {    
  var removedClass = aTarget.className;    
  var pattern = new RegExp("(^| )" + aClassValue + "( |$)");    
  removedClass = removedClass.replace(pattern, "$1");    
  removedClass = removedClass.replace(/ $/, "");     
  aTarget.className = removedClass;    
  return true;    
}

function fieldHasValue(aTarget) {
  if(aTarget.type == 'select-one') {
    return true;
  } else if (aTarget.value != '') {
    return true;
  } else {
    return false;
  }
}

/**
* @param aGroup base group name before subsequence is added, provides access to ref array
*/ 
function groupedFieldChange(aGroup) {
  var arr = gGroupRefArray[aGroup];
  var fref = null;
  // Loop through fields in group and store a reference to a filled field
  for(var i in arr) {
    var el = document.getElementById(arr[i]);
    if(fieldHasValue(el)) {
      fref = arr[i];
    }  
  }
  // Make a second pass, if no field is filled in then activate all fields,
  // if the field currently iterated over is the previously marked field then
  // activate, otherwise deactivate
  for(var k in arr) {
    var f = document.getElementById(arr[k]);
    if(fref==null || fref==arr[k]) {
      enableField(f);
    } else {
      disableField(f);
    }
  }
}

function enableField(aTarget) {
  if(aTarget.type == 'select-one') {
    aTarget.disabled = false;
  } else {
    aTarget.readOnly = false;
  }
  removeClass(aTarget, 'readonly');
}

function disableField(aTarget) {
  if(aTarget.type == 'select-one') {
    aTarget.disabled = true; // disabled fields can't 'succeed' and don't get posted as a value
  } else {
    aTarget.readOnly = true;
  }
  addClass(aTarget, 'readonly');
}

function disableGroup(aGroup) {
  disableGroupExcept(aGroup, null);
}

function disableGroupExcept(aGroup, aExceptionField) {
  var arr = gGroupRefArray[aGroup];
  for(var i in arr) {
    if(arr[i]!=aExceptionField) {
      var el = document.getElementById(arr[i]);
      disableField(el);
    }
  }
}

/**
* Belt and braces check to ensure we are allowed to provide input on this field
* @param aGroup base group name before subsequence is added, provides access to ref array
* @param aRef reference of triggering field
*/ 
function isKeyAllowed(e, aGroup, aRef) {
  var code;
  if (!e) var e = window.event; // IE event handling
  if (e.keyCode) code = e.keyCode;
  else if (e.which) code = e.which;
  if (code < 48 || code > 90) { return true; }; // Auto-allow non-alphanumeric keys such as Tab
  var arr = gGroupRefArray[aGroup];
  for(var i in arr) {
    // If any other fields in this group have a value then disallow input
    if(arr[i] != aRef) {
      var f = document.getElementById(arr[i]);
      if(fieldHasValue(f)) {
        if (e && e.stopPropagation) { //if stopPropagation method supported
          e.stopPropagation()
        } else {
          event.cancelBubble=true // IE event handling
        }        
        return false;
      }
    }    
  } 
  return true;
}

/*
 * Limits text to maxChars
 * pass onkeydown, onkeyup, onkeypress or onpaste event for handling
 * Only restricts pasting text once the text limit has already been reached (the target field can end up being over the limit if the pasted data is longer than the remaining space)
 */
function LimitText(aEvent, maxChars)
{
  var result = true;
  var event = aEvent ? aEvent : window.event;
  var fieldObj = event.target ? event.target : event.srcElement;

  addSelectionProperties(fieldObj); //Add selectionStart/selectionEnd for IE.
  if (fieldObj.value.length - (fieldObj.selectionEnd - fieldObj.selectionStart) >= maxChars)
  {
    switch(event.keyCode)
    {
      case 8: //Backspace
      case 33: //Page up
      case 34: //Page down
      case 35: //End
      case 36: //Home
      case 37: //Arrow left
      case 38: //Arrow up
      case 39: //Arrow right
      case 40: //Arrow down
      case 46: //Delete
      break;
      default:
      result = false;
      if (event.preventDefault) {
        event.preventDefault();
      }
    }
  }

  event.returnValue = result;
  return result;
}

/*
 * Add selectionStart/selectionEnd for IE.
 */
function addSelectionProperties(pElement) {
  if(document.selection) {
    // The current selection
    var lRange = document.selection.createRange();
    // We'll use this as a 'dummy'
    var lStoredRange = lRange.duplicate();
    // Select all text
    lStoredRange.moveToElementText( pElement );
    // Now move 'dummy' end point to end point of original range
    lStoredRange.setEndPoint( 'EndToEnd', lRange );
    // Now we can calculate start and end points
    pElement.selectionStart = lStoredRange.text.length - lRange.text.length;
    pElement.selectionEnd = pElement.selectionStart + lRange.text.length;
  }
}

function showSysMenu() { setCookie('sysMenu','true',1); document.getElementById('sysMenu').style.visibility='visible'; document.getElementById('sysMenu2').style.visibility='hidden';}
function hideSysMenu() { delCookie('sysMenu'); document.getElementById('sysMenu').style.visibility='hidden'; document.getElementById('sysMenu2').style.visibility='visible'; }

$(document).ready(function() {
  $('#timingSpanLink').on('mouseover', function() { populateSysMenuTimings(); });  
});

function populateSysMenuTimings(){
  var lResult = "";
  var lRegExp = /[^:]+: [0-9]+ms/g;
  
  var nodes = document.childNodes;
  for (var i = 0; i < nodes.length; i++) {
    if (nodes[i].nodeType === 8 && nodes[i].nodeValue.match(/TIMING_SUMMARY/g) != null) {
      var lComment = nodes[i].nodeValue.replace(/TIMING_SUMMARY/g, "");
      while((lMatch = lRegExp.exec(lComment)) != null) {      
        if(lMatch[0].match(/Total:/)){
          lResult += "<b>" + lMatch[0] + "</b>" + "<br/>";
        } 
        else {
          lResult += lMatch[0] + "<br/>";
        }
      }
    }  
  }
  
  $('#timingSpan').html("" == lResult ? "No timing info found (NB Chrome not supported)" : lResult);
}

function checkSysMenu() {var displaySys = getCookie('sysMenu'); if (displaySys!=null) { showSysMenu(); } else { hideSysMenu(); } }
function getCookie(NameOfCookie) { if (document.cookie.length > 0) { begin = document.cookie.indexOf(NameOfCookie+'='); if (begin != -1) { begin += NameOfCookie.length+1; end = document.cookie.indexOf(';', begin); if (end == -1) end = document.cookie.length; return unescape(document.cookie.substring(begin, end)); } } return null; }
function setCookie(NameOfCookie, value, expiredays) { var ExpireDate = new Date (); ExpireDate.setTime(ExpireDate.getTime() + (expiredays * 24 * 3600 * 1000)); document.cookie = NameOfCookie + '=' + escape(value) + ((expiredays == null) ? '' : '; expires=' + ExpireDate.toGMTString()); }
function delCookie(NameOfCookie) { if (getCookie(NameOfCookie)) { document.cookie = NameOfCookie + '=' + '; expires=Thu, 01-Jan-70 00:00:01 GMT'; } }
var childWin = null;
var childAppMnemonic = null;
var childThreadId = null;
var childSessionId = null;
var allowChildClosureChecks = 1;
function checkForClosure()
{
  window.opener.registerCheckForChildClosure(window, document.mainForm.app_mnem.value, document.mainForm.thread_id.value, document.mainForm.xfsessionid.value);
}
function registerCheckForChildClosure(windowToCheck, appMnemonic, threadId, sessionId)
{
    childWin = windowToCheck;
    childAppMnemonic = appMnemonic;
    childThreadId = threadId;
    childSessionId = sessionId;
    setTimeout('checkForChildClosure()', 200);
}

function checkForChildClosure() {
   if (childWin.closed)
   {
     if (allowChildClosureChecks > 0)
     {
        window.focus();
        result = confirm("You have just attempted to close a popup window, have you saved all required changes to data?\nIf not *Cancel* and then save changes.\nOtherwise select *OK* to continue.\n");
        if (result == true)
        {
           window.open('abnormalWorkflowTermination.html?app_mnem='+childAppMnemonic+'&thread_id='+childThreadId, '', 'resizable=no,scrollbars=no,status=no,width=10,height=10');
        }
        else
        {
           window.open('?app_mnem='+childAppMnemonic+'&thread_id='+childThreadId+'&xfsessionid='+childSessionId);
        }
      }
   }
   if (allowChildClosureChecks == 0)
   {
     allowChildClosureChecks=1;
   }
}
function focusChildModuleWindow()
{
//   opener.focus();
//   window.focus();
}
function ss(a,c) { submitSection(a,c); }
function ssc(t,a,p) { confirmSubmitSection(t,a,p); }

function changeXPathEvaluator(urlPath, selectWidget)
{
   for (n=0; n < selectWidget.options.length; n++)
   {
      if (selectWidget.options[n].selected)
      {
      	 var childWin = window.open(urlPath+'&dom_type=xpathEvaluator&idx='+n, null, 'toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=,resizable=0,width=50,height=50,left=100,top=10');
         childWin.close();
         break;
      }
   }
}

/*
 * blanks out password-style fields on value change
 */
function clearValueIfObfuscated (e)
{
  try {
    if (!e) {
      var e = window.event;
    }

    if (!e || e.type != "focus") {
      alert("Error: clearValueIfObfuscated did not detect a focus event");
      return false;
    }
  }
  catch (ex) {
    alert("Error: "+ ex);
    return false;
  }

  var targ = e.target ? e.target : e.srcElement;
  var value = targ.value;

  if (value.indexOf(String.fromCharCode(160))!=-1) {
    assignEventListener(targ, "blur", function() {
      if (targ.value == "") {
        targ.value = value;
      }
    });
    targ.value="";
  }
}

// Cross browser event assignment
function assignEventListener (aElem, aEventName, aFunction) {
  // W3C events  model
  if (aElem.addEventListener) {
    aElem.addEventListener(aEventName, aFunction, false);
  }
  // MSIE event model
  else {
    aElem.attachEvent("on"+aEventName, aFunction);
  }
}

// Cross browser event removal
function unassignEventListener(aElem, aEventName, aFunction) {
  if (aElem.removeEventListener) {
    aElem.removeEventListener(aEventName, aFunction, false);
  }
  else {
    aElem.detachEvent("on"+aEventName, aFunction);
  }
}

/*
 * Resizable text area widget code
 */
var gTextAreaArray = new Array();

// cross browser computed style implementation
function computedStyle (aElement, aProperty)
{
  var computedStyle;
  if (typeof aElement.currentStyle != 'undefined') {
    computedStyle = aElement.currentStyle;
  }
  else {
    computedStyle = document.defaultView.getComputedStyle(aElement, null);
  }

  return computedStyle[aProperty];
}

// for convenience, fake an event object to init on startup
function resizeTextAreaInit (aId, aMaxHeight)
{
  var event = {
    target: document.getElementById(aId)
  , type: "fox_page_load"
  };

  // deal with event hookups as this isn't handled at WidgetBuilder level for html-resizable
  if (event.target.nodeName.toLowerCase() == "iframe") {
    assignEventListener(event.target, "keypress", function () {
      resizeTextArea (event, aMaxHeight);
    });
    //event.target.onpaste    = "resizeTextArea (event, " + aMaxHeight + ");";
  }

  resizeTextArea (event, aMaxHeight);
}

function resizeTextArea (aEvent, aMaxHeight)
{
  // cross browser malarkey
  var event = aEvent ? aEvent : window.event;
  var target = event.target ? event.target : event.srcElement;
  var nodeName = target.nodeName.toLowerCase();
  var fontSizeTarget = null;

  if (nodeName == "textarea") {
    fontSizeTarget = target;
  }
  else if (nodeName == "iframe") {
    fontSizeTarget = target.contentWindow.document.body;
  }
  else {
    // should never get here, but exit gracefully
    return;
  }

  if (!gTextAreaArray[target]) {
    gTextAreaArray[target] = {
      fontSize: parseInt(computedStyle(fontSizeTarget, "fontSize"))
    , maxHeight: parseInt(aMaxHeight) // could be rows (textarea) or px (iframe)
    , type: nodeName
    }
  }

  window.setTimeout(function(){resizeTextAreaTimeout(target)}, 0);
}

function resizeTextAreaTimeout (aTarget)
{
  var textAreaDets = gTextAreaArray[aTarget];
  var scrollReached = false;
  var currHeight;

  if (textAreaDets.type == "textarea") {
    currHeight = aTarget.rows;
  }
  else if (textAreaDets.type == "iframe") {
    currHeight = parseInt(aTarget.style.height); // trim px
  }

  // branch based on horrible browser differences with textareas
  if (aTarget.scrollHeight == aTarget.clientHeight)
  {
    // IE gets here whenever it needs to grow
    // Firefox gets here all the time it doesn't need to grow - breaks everything
    // Resort to IE-only hack horrors, but other browsers shouldn't fall over
    if (document.all) {
      scrollReached = aTarget.scrollHeight > aTarget.clientHeight - textAreaDets.fontSize;
    }
    else {
      scrollReached = false;
    }
  }
  else {
    // IE gets here every time it doesn't need to grow - no consequence
    // Firefox gets here if it needs to grow
    scrollReached = aTarget.scrollHeight >= aTarget.clientHeight - textAreaDets.fontSize;
  }

  if (currHeight < textAreaDets.maxHeight && scrollReached)
  {
    if (textAreaDets.type == "textarea") {
      aTarget.rows = aTarget.rows + 1;
    }
    else if (textAreaDets.type == "iframe") {
      aTarget.style.height = (currHeight + textAreaDets.fontSize) + "px";
    }

    window.setTimeout(function(){resizeTextAreaTimeout(aTarget)}, 0);
  }
}

/*
 * Page expired?
 */
function pageExpired ()
{
  //Get the cookie's raw value (decoded from URI encoding)
  var fieldSetCookie = decodeURIComponent(getCookie("field_set"));
  //Parse into an object
  var fieldSetArray = $.parseJSON(fieldSetCookie);
  //Loop through each item in the array and find the object with a matching thread id
  for(var i =0; i < fieldSetArray.length; i++){
    //t = thread id, f = field set id
    if(document.mainForm["thread_id"].value == fieldSetArray[i].t){
      return (fieldSetArray[i].f != document.mainForm["field_set"].value);
    }
  }
  //This thread ID not found in cookie
  return false;
}

/*
 * On-load processing
 */
function processOnload ()
{
  // get rid of "Loading..."
  ld();

  // preserve scroll position
  var scrollPos = parseInt(document.mainForm.scroll_position.value);
  if (scrollPos > 0) {
    window.scrollTo(0, scrollPos);
  }

  // run page scripts or catch erroneous navigation
  if (!pageExpired())
  {
    ars();
    conditionalLoadScript();
    // okay to give 'back' nav warning again
    delCookie("backWarningGiven_"+document.mainForm["thread_id"].value);
  }
  else {
    if (!(typeof(auditOutputFlag) != "undefined" && auditOutputFlag == true)) {
      erroneousNavigation();
    }
  }

  // prevent page presentation caching in browsers that support it
  assignEventListener(window, "unload", function () {
    // do nothing
  });

  //reload JS-modified data for file/image widgets
  reloadModifiedPagePresentation();
}

function reloadModifiedPagePresentation() {

  //fieldArray =  document.getElementsByName('presentation_preservation');

  for(var i=0; i< gPresentationPreservationElems.length; i++) {

    var field = document.getElementById(gPresentationPreservationElems[i]);

    if(field.id.lastIndexOf('_hidden_image') != -1 && field.value != '') {
      var lGroupingRef = field.id.substr(0, field.id.length-'_hidden_image'.length);
      document.getElementById(lGroupingRef + "_image").src = field.value;
      document.getElementById(lGroupingRef + "_image_preview").style.display = 'block';
      document.getElementById(lGroupingRef + "_rotate_left_link").href = 'javascript:rotateImage("'+lGroupingRef+'",-90)';
      document.getElementById(lGroupingRef + "_rotate_right_link").href = 'javascript:rotateImage("'+lGroupingRef+'",90)';
    } else if(field.id.lastIndexOf('hidden-df') != -1 && field.value != '') {
      var elemId = field.id.substr('hidden-'.length);
      document.getElementById(elemId).innerHTML = field.value;
    } else if(field.id.lastIndexOf('hidden-modal-lock') != -1 && field.value == 'true') {
      lockScreenForModalUpload();
    }
  }

}

/*
 * Greys out page using opacity, text/widgets selectable
 */
function setPageExpired (aExpired)
{
  try
  {
    // cope with IE
    document.body.style.opacity = aExpired ? "0.6" : "1.0";
    document.body.style.filter = aExpired ? "alpha(opacity=60)" : "alpha(opacity=100)";
  } catch (e) {
    // doesn't matter
  }

  if (aExpired) {
    document.body.setAttribute("class", "disabled");
  }
  else {
    document.body.removeAttribute("class");
  }
}

/*
 * Greys out page using div - nothing selectable
 */
var gPageDisabled = false;
var gPageExpired = false;

function setPageDisabled (aDisable)
{
  if (aDisable == gPageDisabled) {
    return;
  }

  if (gPageExpired) {
    // page shouldn't be expired and disabled
    setPageExpired(false);
  }

  var a;
  for (var i = 0; (a = document.getElementsByTagName("select")[i]); i++)
  {
    if (aDisable && document.all) {
      a.setAttribute("disabled", "disabled");
      a.style.backgroundColor = "#B3B3B3";
    }
    else {
      a.removeAttribute("disabled");
      a.style.backgroundColor = "#FFFFFF";
    }
	}

  if (aDisable) {
    var blockingDiv = document.createElement("div");
    blockingDiv.setAttribute("id", "blocking-div");
    blockingDiv.style.height = document.body.scrollHeight;
    blockingDiv.style.width = document.body.scrollWidth;

    // Dynamically resize blocking div
    assignEventListener(window, "resize", function() {
      blockingDiv.style.height = document.body.scrollHeight;
      blockingDiv.style.width = document.body.scrollWidth;
    });

    document.body.appendChild(blockingDiv);
  }
  else {
    var blockingDiv = document.getElementById("blocking-div");
    document.body.removeChild(blockingDiv);
  }

  gPageDisabled = aDisable;
}

/*
 * Erroneous navigation
*/
function erroneousNavigation()
{
  var cookie_id = "backWarningGiven_"+document.mainForm["thread_id"].value;
  setPageExpired(true);
  gPageExpired = true;
  if (!getCookie(cookie_id)) {
    alert("Warning: You appear to have navigated to this page using the Back button of your web browser.\n\nYou may view or copy information from this expired page, but you cannot use any of the links or buttons. You must navigate forward again in order to continue work.");
    // warn only on first back
    setCookie(cookie_id, "true", 1);
  }
}

/*
 * Lock read-only select widgets
*/
function lockReadOnlySelectorPlus(aSelectNode)
{
  var childNodes = aSelectNode.childNodes;

  setTimeout(function() {
    for (var n in childNodes) {
      if (childNodes[n].tagName && childNodes[n].tagName.toLowerCase() == "option") {
        childNodes[n].selected = childNodes[n].defaultSelected;
      }
    }
  }, 0);
}


/**
 * Sets a selection within a textbox, from aStartPos to aEndPos in characters
*/
function setSelection (aTextbox, aStartPos, aEndPos) {
  if (aTextbox.createTextRange) {  // Internet Explorer before version 9
    var inputRange = aTextbox.createTextRange();
    inputRange.collapse(true);
    inputRange.moveEnd("character", aEndPos);
    inputRange.moveStart("character", aStartPos);
    inputRange.select();
  }
  else if (aTextbox.selectionStart) {
    aTextbox.selectionStart = aStartPos;
    aTextbox.selectionEnd = aEndPos;
  }
  else if (aTextbox.setSelectionRange) {
    aTextbox.setSelectionRange(aStartPos, aEndPos);
  }
  aTextbox.focus();
}

// Hold an array of child upload windows to prevent opening two
var gUploadArray = new Array();
// Hold a record of the submit button which was clicked to open the window so it can be re-enabled later
var gModalUploadButtonClicked;

function openwinFileUpload(pURL, pWinRef, pModal, pButton) {

  if(pModal) {
    //prevent multiple button clicks
    gModalUploadButtonClicked = pButton;
    gModalUploadButtonClicked.disabled = true;
  }

  if (gUploadArray[pWinRef] == null || gUploadArray[pWinRef].closed) {
    lWindowHandle = window.open(pURL.replace(/&amp;/g,"&"), pWinRef, "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=1,resizable=1,bgcolor=#003399,width=600,height=200,left=100,top=10");
    gUploadArray[pWinRef] = lWindowHandle;

    if(pModal) {
      lockScreenForModalUpload(lWindowHandle);
    }
  }
  else {
    alert("You currently have a window open for uploading to this section of the form.  Please wait for this upload to complete or close the upload window before trying again.");
    gUploadArray[pWinRef].focus();
  }
}

var gModalUploadWindowCheckInterval;

function lockScreenForModalUpload(pWindowHandle)
{
  var updating = document.getElementById("modalupload");
  updating.style.visibility = "visible";
  updating.style.display = "block";
  var iframe = document.getElementById("iframe-wrapper");
  iframe.style.visibility = "visible";
  iframe.style.display  = "block";
  window.setPageDisabled(true);
  //belt and braces - if the user closes the window before it loads the screen will be locked.
  //allow the child time to load and unlock the screen if the window has been closed in the meantime
  gModalUploadWindowCheckInterval = setInterval(function(){checkForModalUploadClosure(pWindowHandle, false);}, 1000);
  assignEventListener(window, "beforeunload", function(){pWindowHandle.forceUploadInterrupt()});
}

function clearModalUploadWindowCheckInterval() {
  clearInterval(gModalUploadWindowCheckInterval);
}

function unlockScreenForModalUpload() {
  var updating = document.getElementById("modalupload");
  updating.style.visibility = "hidden";
  updating.style.display = "none";
  var iframe = document.getElementById("iframe-wrapper");
  iframe.style.visibility = "hidden";
  iframe.style.display  = "none";
  window.setPageDisabled(false);
  gModalUploadButtonClicked.disabled = false;
}

function checkForModalUploadClosure(pWindowHandle, pRetry) {
  if(pWindowHandle != null && pWindowHandle.closed) {
    unlockScreenForModalUpload();
    clearModalUploadWindowCheckInterval();
  } else if(pRetry) {
    setTimeout(function(){checkForModalUploadClosure(pWindowHandle, true);}, 1000);
  }
}

function openAjaxSyncWindow () {
  var w = window.open("about:blank","","toolbar=0,location=0,directories=0,status=1,menubar=0,scrollbars=1,resizable=1,bgcolor=#003399,left=0,top=0,width=" + (screen.availWidth-10) + ",height=" + (screen.availHeight-25));
  w.document.open("text/plain");
  w.document.write(gSynchroniser.getAllDataAsXMLString());
  w.document.close();
}

function changeSrc(pElementId, pNewSrc) {
  document.getElementById(pElementId).src = pNewSrc;
}

function getParam(pUrl, pParamName) {
  var lParamNameIndex = pUrl.lastIndexOf(pParamName+'=');
  if(lParamNameIndex == -1) {
    return null;
  }
  var lParamVal = pUrl.substring(lParamNameIndex+pParamName.length+1);
  if(lParamVal.indexOf('&')!=-1) {
    lParamVal = lParamVal.substring(0,lParamVal.indexOf('&'));
  }
  return lParamVal;
}

function previewRotateImage(pElementId, pRotation) {
  var lSrc = document.getElementById(pElementId).src, lCurrentDeg = parseInt(getParam(lSrc,'rotation')), lNewDeg = lCurrentDeg + pRotation;
  lNewDeg = lNewDeg % 360;
  if(lNewDeg < 0) {
    lNewDeg = lNewDeg + 360;
  }
  changeSrc(pElementId, lSrc.replace('rotation='+lCurrentDeg, lNewDeg));
}

function resetRotateDiv(pGroupingRef) {
  document.getElementById(pGroupingRef + '_rotate_div').style.display = 'none';
  if(document.getElementById(pGroupingRef + '_rotate_right_link')) {
    document.getElementById(pGroupingRef + '_rotate_right_link').href = 'javascript:rotateImage("'+pGroupingRef+'",90)';
    document.getElementById(pGroupingRef + '_rotate_left_link').href = 'javascript:rotateImage("'+pGroupingRef+'",-90)';
  }
}

function addRotateFinishedOnLoad(pGroupingRef) { //This is in a function so that pGroupingRef will stay correct (when looping and adding multiple anonymous onload functions it used the last lGroupingRef set for all of them)
  document.getElementById(pGroupingRef + '_image').onload =
    function() {
      resetRotateDiv(pGroupingRef);
    };
}

//2D array mapping image cache key which is being processed to one or more grouping refs
var gImageProcessingCacheKeys = new Object(); //Javascript doesn't have associative arrays, they're objects instead.
var gImageProcessingAjaxPollTimeMS = 2000;

function rotateImage(pGroupingRef, pDegrees) {
  var lSrc = document.getElementById(pGroupingRef + '_image').src;
  var lNewDeg = parseInt(getParam(lSrc, 'rotation')) + pDegrees;
  var lGroupingRefs;
  var i;

  for(var lCacheKey in gImageProcessingCacheKeys) {
    for(i=0; i < gImageProcessingCacheKeys[lCacheKey].length; i++) {
      if(gImageProcessingCacheKeys[lCacheKey][i]==pGroupingRef) {
        lGroupingRefs = gImageProcessingCacheKeys[lCacheKey];
        break;
      }
    }
  }

  //Bug in IE6 on elgar seems to send 2 HTTP requests - set a unique ID so the 2nd can be discarded by FOX
  var lUnique = document.mainForm.thread_id.value + new Date().getTime();
  var lUrl = lSrc.replace('!GET-IMAGE','!ROTATE-IMAGE') + '&newrotation=' + lNewDeg + '&unique=' + lUnique;

  for(i=0; i < lGroupingRefs.length; i++) {
    document.getElementById(lGroupingRefs[i] + '_rotate_div').style.display = 'block';
    if(document.getElementById(lGroupingRefs[i] + '_rotate_right_link')) {
      document.getElementById(lGroupingRefs[i] + '_rotate_right_link').href = 'javascript:';
      document.getElementById(lGroupingRefs[i] + '_rotate_left_link').href = 'javascript:';
    }
  }

  var lRequest = new AjaxRequest(lUrl, "GET", null, null, null,
      function(aCallbackData) {

        var lMsg;
        if(aCallbackData.xmlData == null || aCallbackData.xmlData.getElementsByTagName('message') == null) {
          lMsg = "There was an error attempting to rotate the image.";
        } else {
          lMsg = aCallbackData.xmlData.getElementsByTagName('message')[0].childNodes[0].nodeValue;
        }

        if(lMsg != "OK") {
          alert(lMsg);
        }

        for(i=0; i < lGroupingRefs.length; i++) {
          if(aCallbackData.xmlData != null && aCallbackData.xmlData.getElementsByTagName('img-src') != null) {
            addRotateFinishedOnLoad(lGroupingRefs[i]);
            changeSrc(lGroupingRefs[i] + '_image',aCallbackData.xmlData.getElementsByTagName('img-src')[0].childNodes[0].nodeValue);
          }
          else { //No new image to load so onload will never be called - means we need to reset the rotate div here.
            resetRotateDiv(lGroupingRefs[i]);
          }
        }
      }
    );
  lRequest.sendRequest();
}

function addCacheKeyGroupingRefs(pImageCacheKey, pGroupingRef) {
  if(document.getElementById(pGroupingRef+"_image_preview") != null) {
    //Remove any current pGroupingRef references in gImageProcessingCacheKeys
    for(var lCacheKey in gImageProcessingCacheKeys) {
      for(var i=0; i < gImageProcessingCacheKeys[lCacheKey].length; i++) {
        if(pGroupingRef == gImageProcessingCacheKeys[lCacheKey][i]) {
          gImageProcessingCacheKeys[lCacheKey].splice(i,1); //Splice for removing array element.
        }
      }
      if(gImageProcessingCacheKeys[lCacheKey].length==0) {
        delete gImageProcessingCacheKeys[lCacheKey]; //Delete for unsetting object property.
      }
    }

    var lGroupingRefs = gImageProcessingCacheKeys[pImageCacheKey];

    if(lGroupingRefs == null) {
      //This is the first time this image cache key has been requested
      var lArray = new Array();
      lArray.push(pGroupingRef);
      //Add grouping ref to array
      gImageProcessingCacheKeys[pImageCacheKey] = lArray;
    } else {
      lGroupingRefs.push(pGroupingRef);
    }
  }
}

function sendImageProcessingAjaxRequest(pAjaxURL, pImageCacheKey) {
    var lRequest = new AjaxRequest(pAjaxURL + '/' + pImageCacheKey.replace(/&amp;/g,"&"), "GET", null, null, null,
      function(aCallbackData) {
        handleImageProcessingAjaxResponse(pAjaxURL, pImageCacheKey, aCallbackData.xmlData);
      }
    );
   lRequest.sendRequest();
}

//AJAX poll for status updates on the image processing for a given image
//Displays a placeholder div until processing and load is complete
function waitForImageProcessing(pImageCacheKey, pGroupingRef, pAjaxURL) {

  //Only do this if the requested grouping ref has an image preview area present in the HTML DOM
  if(document.getElementById(pGroupingRef+"_image_preview") != null) {

    if(gImageProcessingCacheKeys[pImageCacheKey] == null) {
      //initialise the AJAX handler
      sendImageProcessingAjaxRequest(pAjaxURL, pImageCacheKey);
    }

    addCacheKeyGroupingRefs(pImageCacheKey, pGroupingRef);

    //Set the please wait etc div on the widget's HTML
    document.getElementById(pGroupingRef+"_image_preview").style.display = "block";

    //hide rotation if rotation divs are shown
    if(document.getElementById(pGroupingRef+"_rotate_left_link") != null) {
      document.getElementById(pGroupingRef+"_rotate_left_link").style.display = 'none';
      document.getElementById(pGroupingRef+"_rotate_right_link").style.display = 'none';
    }

    var lDiv = document.getElementById(pGroupingRef+"_processing_text_div");
    lDiv.style.display = "block";
    document.getElementById(pGroupingRef+"_image").style.display = 'none';

    //get width/height from cache key - deal with &amp encoding if needed
    var lSplit;
    if(pImageCacheKey.indexOf("&amp;") > 0) {
      lSplit = pImageCacheKey.split("&amp;");
    } else {
      lSplit = pImageCacheKey.split("&");
    }

    var lWidth = getParam(pImageCacheKey, 'width'), lHeight = getParam(pImageCacheKey, 'height');
    if(lWidth==null) {
      lWidth = 200;
    }
    if(lHeight==null) {
      lHeight = 200;
    }

    lDiv.style.width = lWidth + "px";
    lDiv.style.height = lHeight + "px";
  }

}

function handleImageProcessingAjaxResponse(pAjaxURL, pImageCacheKey, pXML) {
  var lStatus = '';
  var lPct = 0;
  var i;
  if(pXML != null && pXML.getElementsByTagName('status') != null) {
    lStatus = pXML.getElementsByTagName('status')[0].childNodes[0].nodeValue;
    lPct = pXML.getElementsByTagName('pct-done')[0].childNodes[0].nodeValue;
  } else {
    return;
  }

  var lGroupingRefs = gImageProcessingCacheKeys[pImageCacheKey];
  if(lGroupingRefs===null) { //Our grouping refs have been cleared, so stop AJAX requests.
    return;
  }

  switch(lStatus) {
    case 'RESIZE_BASE':
    case 'TRUE_SIZE':
    case 'ORIGINAL':
    case 'PENDING':

      for(i=0; i < lGroupingRefs.length; i++) {
        document.getElementById(lGroupingRefs[i] + '_processing_text_span').innerHTML = lPct + '% complete.';
      }
      //keep sending ajax requests
      setTimeout(function(){sendImageProcessingAjaxRequest(pAjaxURL, pImageCacheKey);}, gImageProcessingAjaxPollTimeMS);
      break;
    case 'DONE':

      var lImageURL = pXML.getElementsByTagName('img-src')[0].childNodes[0].nodeValue;
      for(i=0; i < lGroupingRefs.length; i++) {
        var lGroupingRef = lGroupingRefs[i];
        changeSrc(lGroupingRef+"_image", lImageURL);
        document.getElementById(lGroupingRef + '_processing_text_div').style.display = 'none';
        document.getElementById(lGroupingRef+"_image").style.display = 'block';
        document.getElementById(lGroupingRef+"_hidden_image").value = lImageURL;
        if(document.getElementById(lGroupingRef+"_rotate_left_link") != null) {
          document.getElementById(lGroupingRef+"_rotate_left_link").href = 'javascript:rotateImage("'+lGroupingRef+'",-90)';
          document.getElementById(lGroupingRef+"_rotate_left_link").style.display = 'block';
          document.getElementById(lGroupingRef+"_rotate_right_link").href = 'javascript:rotateImage("'+lGroupingRef+'",90)';
          document.getElementById(lGroupingRef+"_rotate_right_link").style.display = 'block';
        }
      }

  }

}

function calcNvl(pValue, pNullValue) {
  if(pValue==null || pValue=='') {
    return pNullValue;
  } else {
    return pValue;
  }
}

function calcFormatNumberResult(pValue, pBadVal, pDP, pForceDPPadding) {
  if(isNaN(pValue) || pValue==undefined) {
    return pBadVal;
  } else {
    if(pDP==null) {
      return pValue;
    } else {
      var lResult = Math.round(pValue*Math.pow(10,pDP))/Math.pow(10,pDP);
      if(pForceDPPadding) {
        var lStr = ""+lResult;
        var lDPIdx = lStr.lastIndexOf(".");
        if(lDPIdx==-1) {
         lStr += ".";
         var lDPnums = 0;
        } else {
         var lStrLength = lStr.length;
         var lDPnums = lStrLength-lDPIdx-1;
        }
        var lPadSize = pDP - lDPnums;
        for(i=0;i<lPadSize;i++) {
         lStr += "0";
        }
        lResult = lStr;
      }
      return lResult;
    }
  }      
}

function startSessionTimeoutTracker(pWusID, pSessionTimeoutSecs, pPromptNotificationSecs, pBangExtendSessionURL){
  // Timestamp at which to initiate the window confirm for session extension
  var lEndDate = (new Date()).getTime() + ((pSessionTimeoutSecs - pPromptNotificationSecs) * 1000);
  // Long stage
  var lTimerFirstStageIntervalMS = ((pSessionTimeoutSecs - ( 2 * pPromptNotificationSecs)) / 10) * 1000;
  var lTimerFirstStageCounter = 0;
  // Short stage
  var lTimerSecondStageIntervalMS = (pPromptNotificationSecs / 10) * 1000;
  var lTimerSecondStageCounter = 0;
  // Burst stage, used just before the timeout window confirm 'should' appear
  var lTimerSmallestTimeResolutionMS = lTimerSecondStageIntervalMS / 10;

  // Get the current JS setTimeout length, decreases the nearer to lEndDate it is
  function getTimeoutMS(){
    if(lTimerFirstStageCounter < 10){
      return lTimerFirstStageIntervalMS;
    }
    else if(lTimerSecondStageCounter < 9){
      return lTimerSecondStageIntervalMS;
    }
    else{
      return lTimerSmallestTimeResolutionMS;
    }
  };
  
  // Increment stage counters
  function incTimeoutCounter(){
    if(lTimerFirstStageCounter < 10){
      lTimerFirstStageCounter++;
    }
    else if(lTimerSecondStageCounter < 9){
      lTimerSecondStageCounter++;
    }
  };
  
  function extendUserSession(pWusID, pSessionTimeoutSecs, pPromptNotificationSecs, pTimingOffsetMS, pBangExtendSessionURL){
    // Timestamp before window confirm blocks 
    var lBeforeDate = (new Date()).getTime();
    lConfirmResult = window.confirm("Your session is about to expire in: " + Math.round(pPromptNotificationSecs / 60) + " minute(s), click OK to extend the current session.");
    // Timestamp after user has cleared window confirm block
    var lAfterDate = (new Date()).getTime();
    if(lConfirmResult){
      // Needed for IE
      $.ajaxSetup ({  
        cache: false  
      });
      var lAjaxRequest = $.ajax({
          url: pBangExtendSessionURL
        , type: "GET"
        , data: {wus_id : pWusID}
        , dataType: "xml"
        , success: function(xml) {
          var lStatus = $(xml).find("status").text();
          switch(lStatus){
            case "INVALID":
              alert("An error occurred while trying to extend your session, contact support for further assistance.");
              break;
            case "EXTENDED":
              alert("Your session has been extended for a further " + Math.round(pSessionTimeoutSecs / 60) + " minute(s).");
              startSessionTimeoutTracker(pWusID, pSessionTimeoutSecs, pPromptNotificationSecs, pBangExtendSessionURL);
              break;
            case "EXPIRED":
              alert("Your session could not be extended as you did not respond in the allotted " + Math.round(pPromptNotificationSecs / 60) + " minute(s).");
              location.reload();
              break;
            default:
              alert("An unexpected error occurred while trying to extend your session, contact support for further assistance.");
          }
        }
      });
    }
    // If the user took longer than the notification period to clear the window confirm block, their session has expired
    else if ((lAfterDate-lBeforeDate) > ((pPromptNotificationSecs * 1000) - pTimingOffsetMS)){
      alert("You did not respond within the allotted " + Math.round(pPromptNotificationSecs / 60) + " minute(s), your session has expired.");
      location.reload();
    }
  }

  // Create JS setTimeout with the current timeout interval (decreases over time)
  function iterateTimeout(){
    setTimeout(function(){
      var lCurrentDate = (new Date()).getTime();
      incTimeoutCounter();
      // If the current timestamp is past the trigger time for window confirm
      if(lCurrentDate > lEndDate){
        var lTimingOffsetMS = lCurrentDate - lEndDate;
        extendUserSession(pWusID, pSessionTimeoutSecs, pPromptNotificationSecs, lTimingOffsetMS, pBangExtendSessionURL);
      }
      else {
        iterateTimeout(); 
      }  
    },getTimeoutMS());
  }
  
  //Initiate session timeout tracking
  iterateTimeout();
}

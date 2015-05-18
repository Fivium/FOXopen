// TODO - NP - Lint this and sort it out
var FOXjs = {

  gPageDisabled: false,
  gPageExpired: false,
  gBeforeSubmitEvents: new Array(),

  // Hold timers to clear later
  gTimers: {},
  gBlockSubmitReason: null,

  /**
   * Remove cookie by name
   */
  removeCookie: function(name) {
    this.setCookie(name, '', -1);
  },

  getCookie: function(name) {
    if (document.cookie.length > 0) {
      begin = document.cookie.indexOf(name+'=');
      if (begin != -1) {
        begin += name.length+1;
        end = document.cookie.indexOf(';', begin);
        if (end == -1) {
          end = document.cookie.length;
        }
        return decodeURIComponent(document.cookie.substring(begin, end));
      }
    }
    return null;
  },

  setCookie: function(name, value, expiredays, path) {
    var ExpireDate = new Date();
    ExpireDate.setTime(ExpireDate.getTime() + (expiredays * 24 * 3600 * 1000));
    document.cookie = name + '=' + escape(value) + ((expiredays == null) ? '' : '; expires=' + ExpireDate.toGMTString()) + ((path == null) ? '' : '; path=' + path);
  },

  /*
   * On-load processing
   */
  processOnload: function() {

    // check icon font has loaded
    // glyphs have to be ones that are more than 1em wide otherwise this doesn't work in IE
    fontSpy('icomoon',{glyphs: '\ue9be\ue90e\ue91b\ue920'});

    // preserve scroll position
    var scrollPos = parseInt(document.mainForm.scroll_position.value);
    if (scrollPos > 0) {
      window.scrollTo(0, scrollPos);
    }

    $.fn.tooltipster('setDefaults', {
      contentAsHTML: true,
      delay: 0,
      speed: 100,
      position: 'bottom',
      maxWidth: 250
    });

    // prevent page presentation caching in browsers that support it
    $(window).on("unload", function () {
      // do nothing
    });

    // Reset dev toolbar handlers and state
    if (this.getCookie("devtoolbar") == "true") {
      $('#dev-toolbar-list').show();
    }
    var that = this;
    $('#devToolbar p').click(
      function () {
        $('#dev-toolbar-list').toggle();
        if ($('#dev-toolbar-list').is(":visible")) {
          that.setCookie("devtoolbar", "true", 1);
        }
        else {
          that.removeCookie("devtoolbar");
        }
      }
    );

    // Enable autoside on textareas with the autosize data attribute
    $("textarea[data-auto-resize = 'true']").autosize();

    // TODO - NP - Implement text area input limiting
    //$("textarea[data-maxlength]").limitOrSomething();

    // Enable hints on focus of elements with hint icons, rather than requiring hovering the icon
    $('[data-hint-id]').on('focus', function() {$('#'+$(this).data('hint-id')).tooltipster('show')});
    $('[data-hint-id]').on('blur', function() {$('#'+$(this).data('hint-id')).tooltipster('hide')});

    $('.hint, .tooltip').each(
      function() {
          if ($(this).attr('title').indexOf('href') >= 0) {
              $(this).tooltipster({interactive: true});
          }
          else {
              $(this).tooltipster();
          }

          if ($(this).attr('data-tooltip-title') != undefined) {
              $(this).tooltipster('content', '<h4>' + $(this).attr('data-tooltip-title') + '</h4>' + $(this).tooltipster('content'));
          }
      }
    );

    // Enable debug information tooltips on any debug icons
    $('[data-debug-id]').each(
      function() {
        $(this).tooltipster({
          'content': $('#'+$(this).attr('data-debug-id')).children()
        , 'maxWidth' : 768
        });
      }
    );

    // Enable date pickers
    $( ".date-input").not(".date-time-input").not('[readonly="readonly"]').datepicker({
      changeMonth: true,
      changeYear: true,
      dateFormat: "dd'-'M'-'yy",
      showButtonPanel: true,
      yearRange: "c-100:c+100"
    });

    $( ".date-time-input" ).not('[readonly="readonly"]').datetimepicker({
      controlType: $.timepicker.textTimeControl,
      changeMonth: true,
      changeYear: true,
      dateFormat: "dd'-'M'-'yy",
      showButtonPanel: true,
      yearRange: "c-100:c+100"
    });

    $( ".date-icon").click(function(){
      var inputId = '#' + $(this).attr("id").replace("icon","");
      if($(inputId).datepicker( "widget" ).is(":visible")) {
        $(inputId).datepicker("hide");
      }
      else {
        $(inputId).datepicker("show");
      }
    });

    // run page scripts or catch erroneous navigation
    if (!this._isExpired()) {
      conditionalLoadScript();
      // Okay to give 'back' nav warning again
      this.removeCookie("backWarningGiven_"+document.mainForm["thread_id"].value);
    }
    else {
      if (!(typeof(auditOutputFlag) != "undefined" && auditOutputFlag == true)) {
        this.erroneousNavigation();
      }
    }

    this.allowSubmit();
  },

  erroneousNavigation: function() {
    var cookie_id = "backWarningGiven_"+document.mainForm["thread_id"].value;
    this._setPageExpired(true);
    gPageExpired = true;
    if (!this.getCookie(cookie_id)) {
      alert("Warning: You appear to have navigated to this page using the Back button of your web browser.\n\nYou may view or copy information from this expired page, but you cannot use any of the links or buttons. You must navigate forward again in order to continue work.");
      // warn only on first back
      this.setCookie(cookie_id, "true", 1);
    }
  },

  /**
   * Check to see if the page has "expired"
   */
  _isExpired: function() {
    // Get the cookie's raw value (decoded from URI encoding)
    var fieldSetCookie = this.getCookie("field_set");
    // Parse into an object
    var fieldSetArray = $.parseJSON(fieldSetCookie);
    // Loop through each item in the array and find the object with a matching thread id
    for(var i = 0; i < fieldSetArray.length; i++){
      // t = thread id, f = field set id
      if(document.mainForm["thread_id"].value == fieldSetArray[i].t){
        return (fieldSetArray[i].f != document.mainForm["field_set"].value);
      }
    }
    // This thread ID not found in cookie
    return false;
  },

  /**
   * Greys out page and marks it as expired if passed true
   */
  _setPageExpired: function(pExpired) {
    try {
      // cope with IE
      document.body.style.opacity = aExpired ? "0.6" : "1.0";
      document.body.style.filter = aExpired ? "alpha(opacity=60)" : "alpha(opacity=100)";
    }
    catch (e) {
      // doesn't matter
    }

    if (pExpired) {
      $(document.body).addClass("disabled");
    }
    else {
      $(document.body).removeClass("disabled");
    }
  },

  /**
   * Block out the page and "disable" it if passed true
   */
  _setPageDisabled: function(pDisable) {
    if (pDisable == this.gPageDisabled) {
      return;
    }

    if (this.gPageExpired) {
      // Page shouldn't be expired and disabled
      this._setPageExpired(false);
    }

    if (pDisable) {
      var blockingDiv = document.createElement("div");
      blockingDiv.setAttribute("id", "blocking-div");
      blockingDiv.style.height = document.body.scrollHeight;
      blockingDiv.style.width = document.body.scrollWidth;

      // Dynamically resize blocking div
      $(window).resize(function() {
        blockingDiv.style.height = document.body.scrollHeight;
        blockingDiv.style.width = document.body.scrollWidth;
      });

      document.body.appendChild(blockingDiv);
    }
    else {
      document.body.removeChild(document.getElementById("blocking-div"));
    }

    this.gPageDisabled = pDisable;
  },

  /**
   * Display optional loading blocker when sending data to update
   */
  _showUpdating: function() {
    var updatingDiv = $('#updating');

    if (!updatingDiv) {
      // If no loading div on the page ignore this fucntion
      return;
    }

    updatingDiv.show();

    var blankingFrame = $('#iframe-wrapper');
    blankingFrame.show();

    // Update src URLs of images in the updating div so IE continues to run their animation
    $('img', updatingDiv).each(function() {
        this.src = this.src;
      }
    );

    document.body.style.cursor = "wait";

    this._setPageDisabled(true);

    window.setTimeout(function(){
      var closeButton = document.getElementById("updating-close-button");
      closeButton.style.visibility = "visible";
      closeButton.style.display = "block";
    }, 1000*60*5); // 5 minutes
    // Stop resubmission in IE
    if ("activeElement" in document) {
      document.activeElement.blur();
    }
    this.blockSubmit("The page is already loading. Please wait for the page to finish loading before performing further actions.");
  },

  /**
   * Run client side actions
   */
  action: function(options) {
    var settings = $.extend({
      ref: null
    , ctxt: null
    , confirm: null
    , params: null
    }, options);

    if (settings.ref === null) {
      return; // Can't run an action without reference
    }

    if (settings.confirm !== null && !confirm(settings.confirm)) {
      return; // Can't run an action if a confirm is defined and not okay'd by the user
    }

    if (this._isExpired()) {
      // If the page has expired notify them
      var goForward = confirm("Warning: this page has expired.\n\nIt looks like you have navigated back to this page using the browser back button. You will need to navigate forwards to the most recent page before you can activate any links or buttons.\n\nClick OK to be taken back to your current workflow position.\nClick CANCEL to remain on this expired page.");
      // A bit ugly, but worst case scenario is that we go nowhere and user has to use browser forward button
      // If problematic, could add a form POST timeout to make sure users go somewhere
      if (goForward) {
        // IE, Opera will go to top of history stack with this
        // assuming < 999 pages navigated
        for (var i = 999; i > 0; i--) {
          window.history.go(i);
        }

        // Firefox prefers this instead
        for (var i = 1; i <= 999; i++) {
          window.history.go(i);
        }
        return;
      }

      return;
    }
    else if (this.gBlockSubmitReason != null) {
      alert(this.gBlockSubmitReason);
    }
    else if (!this.gPageDisabled) {
      // If the page is not disabled, set up some values and submit the form
      document.mainForm.scroll_position.value = Math.round($(document).scrollTop());
      document.mainForm.action_name.value = settings.ref;
      document.mainForm.context_ref.value = settings.ctxt;

      //Action params - write to JSON
      document.mainForm.action_params.value = JSON.stringify(settings.params);

      if(this.gClientActionQueue.length > 0) {
        $(document.mainForm).append($('<input type="hidden" name="client_actions""/>'));
        document.mainForm.client_actions.value = JSON.stringify(this.gClientActionQueue);
      }

      // process HTMLArea code, or anything else pre-submit
      document.mainForm.onsubmit();

      // POST form
      document.mainForm.submit();

      // Show "loading/updating"
      this._showUpdating();
    }
  },

  /**
   * Open popup windows
   */
  openwin: function(options) {
    var settings = $.extend({
      url: null
    , windowName: ""
    , windowOptions: "default"
    , windowProperties: ""
    }, options);

    if (settings.url) {
      settings.url = settings.url.replace(/&amp;/g,"&");

      switch (settings.windowOptions) {
        case "default":
          window.open(settings.url, settings.windowName, "");
          break;
        case "custom":
          window.open(settings.url, settings.windowName, settings.windowProperties);
          break;
        case "appwin":
          window.open(settings.url, settings.windowName, "toolbar=0,location=0,directories=0,status=1,menubar=0,scrollbars=1,resizable=1,left=0,top=0,width=" + (screen.availWidth-10) + ",height=" + (screen.availHeight-25));
          break;
        case "searchwin":
          window.open(settings.url, "searchwin", "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=1,resizable=0,bgcolor=#003399,width=600,height=700,left=100,top=10");
          break;
        case "filewin":
          window.open(settings.url, "filewin", "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=1,resizable=1,bgcolor=#003399,width=1013,height=680,left=100,top=10");
          break;
        case "refwin":
          window.open(settings.url, "refwin", "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=1,resizable=1,bgcolor=#000066,width=800,height=600,left=100,top=75");
          break;
        case "helpwin":
          window.open(settings.url, "", "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=1,resizable=1,bgcolor=#003399,width=600,height=500,left=100,top=10");
          break;
        case "fullwin":
          window.open(settings.url, settings.windowName, "toolbar=1,location=1,directories=1,status=1,menubar=1,scrollbars=1,resizable=1,width=900,height=700,left=50,top=10");
          break;
        case "flushwin":
          window.open(settings.url, "flushwin", "toolbar=1,location=0,directories=0,status=0,menubar=1,scrollbars=1,resizable=1,width=638,height=105,left=100,top=100");
          break;
        default:
          window.open(settings.url, settings.windowName, "");
      }
    }
  },

  /**
   * Left pad pString with pPadCharacter until pString is pLength long
   */
  leftPad: function(pString, pLength, pPadCharacter) {
    pString = pString.toString();
    return pString.length < pLength ? this.leftPad(pPadCharacter + pString, pLength) : pString;
  },

  /**
   * Run timer code for elements with a value of MM:SS with a callback at the deadline
   */
  startTimer: function(pElement, pCallback) {
    var that = this;
    this.gTimers[pElement.attr('id')] = setInterval(function() {
        var lTimeParts = pElement.val().split(':');
        var lMinutes = parseInt(lTimeParts[0]);
        var lSeconds = parseInt(lTimeParts[1]);
        if (lMinutes === 0 && lSeconds === 0) {
          // Bail out if there's no time on the clock
          clearInterval(that.gTimers[pElement.attr('id')]);
          return;
        }
        else if (lSeconds === 0) {
          // If no seconds left, decrement the minutes and reset the seconds
          lMinutes--;
          lSeconds = "60";
        }

        lSeconds--;

        pElement.val(that.leftPad(lMinutes, 2, '0') + ':' + that.leftPad(lSeconds, 2, '0'));

        // If we just got to the deadline, clear this interval and run the action
        if (lMinutes === 0 && lSeconds === 0) {
          clearInterval(that.gTimers[pElement.attr('id')]);
          pCallback();
        }
      }
    , 1000);
  },

  blockSubmit: function(pReason) {
    this.gBlockSubmitReason = pReason;
  },

  allowSubmit: function() {
    this.gBlockSubmitReason = null;
  },

  focus: function(pExternalFoxId, pYOffset) {
    var lFocusTargets = $('*[data-xfid=' + pExternalFoxId + ']');
    //Scroll document to focus position
    if(lFocusTargets.offset() != null) {
      $(document).scrollTop(lFocusTargets.offset().top + pYOffset);
    }
    //Attempt to focus a focusable element
    //TODO PN this needs to be aware of element visibility (otherwise IE might have an error)
    lFocusTargets.children('input, select, textarea').focus()
  },

  gClientActionQueue: new Array(),

  enqueueClientAction: function(pActionType, pActionKey, pActionParams) {
    this.gClientActionQueue.push({action_type: pActionType, action_key: pActionKey, action_params: pActionParams});
  },

  dequeueClientActions: function(pActionType) {
    var lPreservedQueue = new Array();
    var lDequeuedItems = new Array();

    while(this.gClientActionQueue.length > 0) {
      //Remove items from the front of the queue
      var lItem = this.gClientActionQueue.splice(0, 1)[0];
      if(lItem.action_type == pActionType) {
        lDequeuedItems.push(lItem);
      }
      else {
        lPreservedQueue.push(lItem);
      }
    }

    //Replace the original queue with the preserved queue
    this.gClientActionQueue = lPreservedQueue;

    return lDequeuedItems;
  }
};

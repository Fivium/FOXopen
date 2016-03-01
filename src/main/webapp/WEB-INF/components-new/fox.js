/*
 * FOX JS
 *
 * Copyright 2015, Fivium Ltd.
 * Released under the BSD 3-Clause license.
 *
 * Dependencies:
 *   jQuery v1.11
 *   jQuery-FontSpy v3
 *   Tooltipster v3.2.6
 */

// Can be run through JSDoc (https://github.com/jsdoc3/jsdoc) and JSHint (http://www.jshint.com/)

/*jshint laxcomma: true, laxbreak: true, strict: false */

var FOXjs = {
  gPageDisabled: false,
  gPageExpired: false,

  // Store a reason for why the page cannot be submitted
  gBlockSubmitReason: null,

  // Hold timers to clear later
  gTimers: {},

  // Array of client actions
  gClientActionQueue: [],

  /**
   * Get a cookie for a given name
   * @param {string} name The name of the cookie to get
   * @returns {string} The value stored in the cookie
   * @static
   */
  getCookie: function(name) {
    if (document.cookie.length > 0) {
      var begin = document.cookie.indexOf(name + "=");
      if (begin != -1) {
        begin += name.length + 1;
        var end = document.cookie.indexOf(";", begin);
        if (end == -1) {
          end = document.cookie.length;
        }
        return decodeURIComponent(document.cookie.substring(begin, end));
      }
    }
    return null;
  },

  /**
   * Set the value of a cookie, optionally specifying when to expire and a path the cookie should be available on
   * @param {string} name The name of the cookie to set
   * @param {string} value The value of the cookie
   * @param {int} expireDays How many days should the cookie last for
   * @param {string} path The path to scope access to this cookie to
   * @static
   */
  setCookie: function(name, value, expireDays, path) {
    var newCookie = name + "=" + encodeURI(value);
    if (expireDays !== null) {
      var expiresDateTime = new Date();
      expiresDateTime.setTime(expiresDateTime.getTime() + (expireDays * 24 * 3600 * 1000));
      newCookie += "; expires=" + expiresDateTime.toUTCString();
    }
    if (path !== null) {
      newCookie += "; path=" + path;
    }
    document.cookie = newCookie;
  },

  /**
   * Remove a cookie on the current domain which has a given name
   * @param {string} name The name of the cookie to remove
   * @static
   */
  removeCookie: function(name) {
    this.setCookie(name, "", -1, null);
  },

  /**
   * Initialise everything needed for a FOX page e.g. Scroll position, hint icons, calendars...
   * @param {function} successFunction Function to run when a page is successfully initialised
   */
  init: function(successFunction) {
    // Check icon font has loaded
    // Glyphs have to be ones that are more than 1em wide otherwise this doesn't work in IE
    fontSpy("icomoon", {glyphs: "\ue9be\ue90e\ue91b\ue920"});

    // Init sticky panels
    $( document ).ready(function(){$(".sticky").stick_in_parent({parent: '.container'});});

    //https://github.com/leafo/sticky-kit/issues/31
    $('.sticky')
    .on('sticky_kit:bottom', function(e) {
        $(this).parent().css('position', 'static');
    })
    .on('sticky_kit:unbottom', function(e) {
        $(this).parent().css('position', 'relative');
    })

    // Preserve scroll position
    var scrollPosition = parseInt(document.mainForm.scroll_position.value);
    if (scrollPosition > 0) {
      window.scrollTo(0, scrollPosition);
    }

    // Prevent page presentation caching in browsers that support it
    $(window).on("unload", function () {
      // no body needed
    });

    // Set default properties for hint icons
    $.fn.tooltipster("setDefaults", {
      contentAsHTML: true,
      delay: 0,
      speed: 100,
      position: "bottom",
      maxWidth: 250
    });

    // Enable hints on focus of elements with hint icons, rather than requiring hovering the icon
    $("[data-hint-id]").on({
      focus: function() {
        $("#" + $(this).data("hint-id")).tooltipster("show");
      },
      blur: function() {
        $("#" + $(this).data("hint-id")).tooltipster("hide");
      }
    });

    // Add tooltipster hooks to elements with hints/tooltips
    $(".hint, .tooltip").each(
        function() {
          FOXjs.addHintToTarget(this);
        }
    );

    // Enable autosize on textareas with the autosize data attribute
    $("textarea[data-auto-resize = 'true']").autosize();

    // Limit textarea length in <= IE9
    if (!("maxLength" in document.createElement("textarea"))) {
      $("textarea[maxlength]").on('keyup blur paste drop', function () {
        var that = $(this);
        //setTimeout required so the browser has time to see the pasted value in a paste event
        setTimeout(function() {
          var maxLength = that.attr('maxlength');
          var val = that.val();

          if (val.length > maxLength) {
            that.val(val.slice(0, maxLength));
          }
        }, 0);
      });
    }

    // Initialise the date pickers for fields that need them
    $( ".date-input").not(".date-time-input").not("[readonly='readonly']").datepicker({
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

    // Enable onchange attribute on selectors in a keyboard-accessible way
    this._overrideSelectorOnChange();

    // Run page scripts or catch erroneous navigation
    if (!this.isExpired()) {
      successFunction();

      // Trigger any blocks of code inside a $(document).on('foxReady', function(){ });
      $(document).trigger('foxReady');

      // Okay to give 'back' nav warning again
      this.removeCookie("backWarningGiven_" + document.mainForm.thread_id.value);

      this.allowSubmit();
    }
    else {
      this.erroneousNavigation();
    }
  },

  /**
   * Add tooltip hint to a target element
   *
   * @param {object} targetElement Element to trigger the tooltip off
   * @param {string} hintContentID Optionally set the ID of the hint content element to make sure it's set on the target
   * @static
   */
  addHintToTarget: function(targetElement, hintContentID) {
    targetElement = $(targetElement);
    if (hintContentID) {
      // make sure the target element has an aria tag pointing to the content
      targetElement.attr('aria-describedby', hintContentID);
    }

    var hintContentElement = $('#' + targetElement.attr("aria-describedby"));
    targetElement.tooltipster({
      functionInit: function(){
        return hintContentElement.html();
      },
      functionReady: function(){
        hintContentElement.attr('aria-hidden', false);
      },
      functionAfter: function(){
        hintContentElement.attr('aria-hidden', true);
      },
      interactive: (hintContentElement.find('a').length > 0)
    });
  },

  /**
   * Set up selector elements to deal with OnChange events in a more accessible way
   */
  _overrideSelectorOnChange: function() {
    /**
     * Handle onchange event for the selector
     *
     * @param theElement
     * @returns {boolean}
     */
    function selectChanged(theElement) {
      var theSelect;

      if (theElement && theElement.value) {
        theSelect = theElement;
      }
      else {
        theSelect = this;
      }

      // Return false if nothing was noted as being changed and the element wasn't turned into a tagger
      if (!theSelect.changed && !$(theSelect).data('isTagger')) {
        return false;
      }

      theSelect.originalonchange();

      return true;
    }

    /**
     * Handle onclick events for the selector, marking it as changed as the click will have come from someone selecting
     * and item.
     */
    function selectClicked() {
      this.changed = true;
    }

    /**
     * Handle the focus event by storing the original value so we can test later to see if it actually changed before
     * firing the onchange event
     * @returns {boolean}
     */
    function selectFocused() {
      this.initValue = this.value;
      return true;
    }

    /**
     * Handle keydown events
     * @param e
     * @returns {boolean}
     */
    function selectKeyed(e) {
      var theEvent;
      var keyCodeTab = 9; // Tab
      var keyCodeEnter = 13; // Enter
      var keyCodeEsc = 27; // Esc

      if (e) {
        theEvent = e;
      }
      else {
        theEvent = event;
      }

      if ((theEvent.keyCode === keyCodeEnter || theEvent.keyCode === keyCodeTab) && this.value !== this.initValue) {
        this.changed = true;
        selectChanged(this);
      }
      else if (theEvent.keyCode === keyCodeEsc) {
        this.value = this.initValue;
      }
      else {
        this.changed = false;
      }

      return true;
    }

    $('select[onchange]').map(function(pIndex, pElement) {
      pElement.changed = false;
      pElement.originalonchange = pElement.onchange;
      pElement.onfocus = selectFocused;
      pElement.onchange = selectChanged;
      pElement.onkeydown = selectKeyed;
      pElement.onclick = selectClicked;
    });
  },



  /**
   * Show an alert with information telling the user that because of their backwards navigation links will not work until
   * they navigate forwards again.
   */
  erroneousNavigation: function() {
    var cookieId = "backWarningGiven_" + document.mainForm.thread_id.value;
    this.setPageExpired(true);
    if (!FOXjs.getCookie(cookieId)) {
      FOXalert.textAlert("<p>To avoid losing data, please don't use your browser's Back button.</p><p>You can read or copy information from this page, but you'll need to go forward again to continue.</p>", {"alertStyle": "warning", "title":"Page expired"});
      // warn only on first back
      FOXjs.setCookie(cookieId, "true", 1, null);
    }
  },

  /**
   * Check to see if the page has "expired"
   * @private
   */
  isExpired: function() {
    // Get the cookie's raw value (decoded from URI encoding)
    var fieldSetCookie = this.getCookie("field_set");
    // Parse into an object
    var fieldSetArray = $.parseJSON(fieldSetCookie);
    // Loop through each item in the array and find the object with a matching thread id
    for(var i = 0; i < fieldSetArray.length; i++){
      // t = thread id, f = field set id
      if(document.mainForm.thread_id.value == fieldSetArray[i].t){
        return (fieldSetArray[i].f != document.mainForm.field_set.value);
      }
    }
    // This thread ID not found in cookie
    return false;
  },

  /**
   * Greys out page and marks it as expired if passed true
   * @param {boolean} isExpired Is the page expired
   * @private
   */
  setPageExpired: function(isExpired) {

    if (isExpired) {
      this.gPageExpired = true;
      $(document.body).addClass("disabled");
    }
    else {
      this.gPageExpired = false;
      $(document.body).removeClass("disabled");
    }
  },

  /**
   * Block out the page and "disable" it if passed true
   * @param {boolean} isDisabled Is the page disabled
   * @protected
   */
  setPageDisabled: function(isDisabled) {
    if (isDisabled == this.gPageDisabled) {
      return;
    }

    if (this.gPageExpired) {
      // Page shouldn't be expired and disabled
      this.setPageExpired(false);
    }

    if (isDisabled) {
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

    this.gPageDisabled = isDisabled;
  },

  /**
   * Display optional loading blocker when sending data to update
   * @private
   */
  showUpdating: function() {
    var updatingDiv = $("#updating");

    if (!updatingDiv) {
      // If no loading div on the page ignore this fucntion
      return;
    }

    updatingDiv.show();

    var blankingFrame = $("#iframe-wrapper");
    blankingFrame.show();

    $("img", updatingDiv).each(function() {
          // Update src URLs of images in the updating div so IE continues to run their animation
          this.src = this.src;
        }
    );

    document.body.style.cursor = "wait";

    this.setPageDisabled(true);

    window.setTimeout(function(){
      var closeButton = document.getElementById("updating-close-button");
      closeButton.style.visibility = "visible";
      closeButton.style.display = "block";
    }, 1000*60*5); // 5 minutes
    // Stop resubmission in IE
    if ("activeElement" in document && document.activeElement !== document.body) {
      document.activeElement.blur();
    }
    this.blockSubmit("The page is already loading. Please wait for the page to finish loading before performing further actions.");
  },

  /**
   * Run a server side action. Make sure the main form is up to date and post it.
   * @param {object} options Information about the action to run
   * @param {HTMLElement} [triggerElement] Element used to trigger the action
   */
  action: function(options, triggerElement) {
    var settings = $.extend({
      ref: null,
      ctxt: null,
      confirm: null,
      params: null
    }, options);

    if (settings.ref === null) {
      return; // Can't run an action without reference
    }

    var that = this;
    if (this.isExpired()) {
      // If the page has expired notify them
      var goForwardConfirm = "<p>It looks like you have navigated back to this page using the browser back button.</p> " +
        "<p>You will need to go forward to the most recent page before you can click any links or buttons.</p>" +
        "<p>Click <strong>OK</strong> to be taken to your most recent page.</p>" +
        "<p>Click <strong>Cancel</strong> to remain on this expired page.</p>";

      FOXalert.textConfirm(goForwardConfirm, {title: 'Page expired', alertStyle: 'warning'}, function() { that._expiredPageGoForward(settings); }, null);
    }
    else if (settings.confirm) {
      // Can't run an action if a confirm is defined and not okay'd by the user
      // Note if confirm fails, option widgets (selectors/tickboxes/radios) must be reset to their initial value
      FOXalert.textConfirm(settings.confirm, {}, function() { that._runAction(settings); },  function() { FOXoptions.resetToInitialValue(triggerElement); } );
    }
    else {
      this._runAction(settings);
    }
  },

  _runAction : function(settings) {

    if (this.gBlockSubmitReason !== null) {
      alert(this.gBlockSubmitReason);
    }
    else if (!this.gPageDisabled) {

      //Notify listeners that the form is about to be submitted, giving them a chance to set field values etc
      $(document).trigger('foxBeforeSubmit');

      // If the page is not disabled, set up some values and submit the form
      document.mainForm.scroll_position.value = Math.round($(document).scrollTop());
      document.mainForm.action_name.value = settings.ref;
      document.mainForm.context_ref.value = settings.ctxt;

      // Action params - write to JSON
      document.mainForm.action_params.value = JSON.stringify(settings.params);

      if(this.gClientActionQueue.length > 0) {
        $(document.mainForm).append($("<input type=\"hidden\" name=\"client_actions\"/>"));
        document.mainForm.client_actions.value = JSON.stringify(this.gClientActionQueue);
      }

      // Process HTMLArea code, or anything else pre-submit
      document.mainForm.onsubmit();

      // POST form
      document.mainForm.submit();

      // Show "loading/updating"
      this.showUpdating();
    }
  },

  _expiredPageGoForward : function() {
    // A bit ugly, but worst case scenario is that we go nowhere and user has to use browser forward button
    // If problematic, could add a form POST timeout to make sure users go somewhere

    // IE, Opera will go to top of history stack with this
    // assuming < 999 pages navigated
    for (var b = 999; b > 0; b--) {
      window.history.go(b);
    }

    // Firefox prefers this instead
    for (var f = 1; f <= 999; f++) {
      window.history.go(f);
    }
  },

  /**
   * Open popup windows
   * @param {object} options Information about the window to pop up
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
   * Left pad string with paddingCharacter until string is resultLength long
   * @param {string} string String to add padding to
   * @param {string} resultLength Length of the string after padding
   * @param {string} paddingCharacter Character to pad with
   * @return {string} string padded with paddingCharacter until resultLength characters long
   */
  leftPad: function(string, resultLength, paddingCharacter) {
    string = string.toString();
    return string.length < resultLength ? this.leftPad(paddingCharacter + string, resultLength) : string;
  },

  /**
   * Run timer code for elements with a value of MM:SS with a callback at the deadline
   * @param {element} element Input field with a timeout defined in it with the format MM:SS
   * @param {function} callback Function to run after the timeout defined in element
   */
  startTimer: function(element, callback) {
    var that = this;
    this.gTimers[element.attr("id")] = setInterval(function() {
          var lTimeParts = element.val().split(':');
          var lMinutes = parseInt(lTimeParts[0]);
          var lSeconds = parseInt(lTimeParts[1]);
          if (lMinutes === 0 && lSeconds === 0) {
            // Bail out if there's no time on the clock to start with
            clearInterval(that.gTimers[element.attr('id')]);
            return;
          }
          else if (lSeconds === 0) {
            // If no seconds left, decrement the minutes and reset the seconds
            lMinutes--;
            lSeconds = "60";
          }

          lSeconds--;

          element.val(that.leftPad(lMinutes, 2, "0") + ":" + that.leftPad(lSeconds, 2, "0"));

          // If we just got to the deadline, clear this interval and run the action
          if (lMinutes === 0 && lSeconds === 0) {
            clearInterval(that.gTimers[element.attr("id")]);
            callback();
          }
        }
        , 1000);
  },

  /**
   * Mark the page as non-submittable, i.e. no actions should be run, with a reason
   * @param {string} reason Reason why the page cannot be submitted currently
   * @private
   */
  blockSubmit: function(reason) {
    this.gBlockSubmitReason = reason;
  },

  /**
   * Mark the page as submittable, clearing any previous blockage reason
   * @private
   */
  allowSubmit: function() {
    this.gBlockSubmitReason = null;
  },

  /**
   * Move focus to an element with an optional Y offset
   * @param {string} externalFoxId Value of a data-xfid on one or more elements
   * @param {int} yOffset Vertical offset, should you want the page to show information above/below the targeted elements
   */
  focus: function(externalFoxId, yOffset) {
    var lFocusTargets = $("*[data-xfid=" + externalFoxId + "]");
    // Scroll document to focus position
    if(lFocusTargets.offset()) {
      $(document).scrollTop(lFocusTargets.offset().top + yOffset);
    }
    // Attempt to focus a focusable element
    // TODO PN this needs to be aware of element visibility (otherwise IE might have an error)
    var lFocusTargetElement = lFocusTargets.find("input, select, textarea").first();
    if (!lFocusTargetElement.is(":visible")) {
      lFocusTargetElement.triggerHandler('focus');
    }
    else {
      lFocusTargetElement.focus();
    }
  },

  /**
   * Record a client side action with a given actionType for processing by the thread as part of the post data
   * @param {string} actionType
   * @param {string} actionKey
   * @param {object} actionParams
   */
  enqueueClientAction: function(actionType, actionKey, actionParams) {
    this.gClientActionQueue.push({action_type: actionType, action_key: actionKey, action_params: actionParams});
  },

  /**
   * Get all stored client side actions for a given actionType
   * @param actionType
   * @returns {array}
   */
  dequeueClientActions: function(actionType) {
    var lPreservedQueue = [];
    var lDequeuedItems = [];

    while(this.gClientActionQueue.length > 0) {
      // Remove items from the front of the queue
      var lItem = this.gClientActionQueue.splice(0, 1)[0];
      if(lItem.action_type == actionType) {
        lDequeuedItems.push(lItem);
      }
      else {
        lPreservedQueue.push(lItem);
      }
    }

    // Replace the original queue with the preserved queue
    this.gClientActionQueue = lPreservedQueue;

    return lDequeuedItems;
  }
};

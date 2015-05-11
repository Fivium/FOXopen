var DevToolbar = {
  gPrevScrollTop: 0,
  gShowAdvancedOpts: false,
  gBangUrlPrefix: "",
  gTrackId: "",
  gUserExperienceTime: null,

  processOnLoad: function() {

    //Reorder DOMs
    this.updateDomOrder();

    //Recalculate number of columns needed to contain DOM links
    this.updateDomColumnCount();

    //Restore state from cookies
    if(FOXjs.getCookie('devToolbarMode') == 'tall') {
      this.toggleDevToolbarMode();
    }

    if(FOXjs.getCookie('devToolbarPinned') == 'true') {
      this.toggleDevToolbarPinned();
    }

    if(FOXjs.getCookie('devToolbarHidden') == 'true') {
      this.toggleDevToolbarVisibility();
    }

    //Make the dev toolbar semi-sticky (visible on scroll up)
    this.gPrevScrollTop = $('body').scrollTop();
    $(window).scroll(function (e){
      DevToolbar.handleDevToolbarScroll();
    });

    //Handler for switching from short mode to tall mode
    $('#dev-toolbar-toggle-mode').click(function (e){
      DevToolbar.toggleDevToolbarMode();
    });

    //Handler for hiding-unhiding
    $('#dev-toolbar-icon').click(function (e){
      DevToolbar.toggleDevToolbarVisibility();
    });

    //Handler for pinning/unpinning
    $('#dev-toolbar-toggle-pin').click(function (e){
      DevToolbar.toggleDevToolbarPinned();
    });

    //Handler for flush link
    $('#dev-toolbar-flush a').click(function (e){
      DevToolbar.flush(e.currentTarget.href, e);
      return false;
    });

    //Handler for keydown/keyup to check if ctrl is pressed and toggle flush mode
    $(window).bind('keydown','ctrl',function (e){
      DevToolbar.toggleFlushMode(true);
    });

    $(window).bind('keyup','ctrl',function (e){
      DevToolbar.toggleFlushMode(false);
    });

    //Handlers for keyboard shortcuts
    //Flush
    $(window).bind('keydown','alt+f',function (e){
      e.preventDefault();
      DevToolbar.flush($('#dev-toolbar-flush').attr('href'), e);
    });

    $(window).bind('keydown','alt+ctrl+f',function (e){
      e.preventDefault();
      DevToolbar.flush($('#dev-toolbar-flush').attr('href'), e);
    });

    //DOMs
    $(window).bind('keydown','alt+r',function (e){
      $('a#root-dom-link').click()
    });

    $(window).bind('keydown','alt+t',function (e){
      $('a#theme-dom-link').click()
    });

    $(window).bind('keydown','alt+m',function (e){
      $('a#temp-dom-link').click()
    });

    //Advanced flags
    DevToolbar.toggleAdvancedOpts();

    $('#dev-toolbar-advanced').click(function (e){
      DevToolbar.toggleAdvancedOpts(e);
    });

    $('#devflag_TRACK_UNATTACHED_LABEL').change(function() {
      var isChecked = $('#devflag_TRACK_UNATTACHED_LABEL').prop('checked');
      $('#TRACK_UNATTACHED_LABEL_NAME').css('display', isChecked ? 'block' : 'none');
    });

    //Toggle toolbar mode
    $(window).bind('keydown','alt+down',function (e){
      DevToolbar.toggleDevToolbarMode();
    });

    $(window).bind('keydown','alt+up',function (e){
      DevToolbar.toggleDevToolbarMode();
    });

    this.getTimingsAndMessages(DevToolbar.gBangUrlPrefix + '!TRACKSUMMARY?id=' + DevToolbar.gTrackId);

    //Register change handlers on form inputs - this will submit the whole form on any value change
    $('#devToolbarForm input').change(function() {
      $.post(
        DevToolbar.gBangUrlPrefix + '!DEVTOOLBAROPTIONS?thread_id=' + $('#thread_id').val(),
        $('#devToolbarForm').serialize(),
        function(data) {
          //Check the request was serviced correctly
          var error = $(data).find('error').text();
          if(error != '') {
            alert(error);
          }
          $('#devToolbarForm input').prop('disabled', false);
        }
      );
      //Disable inputs while we wait for response
      $('#devToolbarForm input').prop('disabled', true);
    });
  },

  updateDomOrder: function() {
    var doms = ["root","theme","temp","params","return","result","error","user","session","sys","env"];
    var newList = "";
    for(var i = 0; i < doms.length; i++) {
      var domLink = $("a#" + doms[i] + "-dom-link");
      //DOM link for built-in DOM may not exist if there's no root DOM (for instance)
      if(domLink.length > 0) {
        newList += "<li>" + domLink.parent().html() + "</li>";
        domLink.parent().remove();
      }
    }
    $('ul#dev-toolbar-doms').append(newList);
  },

  updateDomColumnCount: function() {
    var colCount = 1;

    if($('#dev-toolbar').hasClass('tall-dev-toolbar')) {
      colCount = ""+(Math.ceil($('#dev-toolbar-doms li').length / 3)+1);
    }

    $('#dev-toolbar-doms').css('column-count', colCount);
  },

  handleDevToolbarScroll: function() {
    var delta = $(window).scrollTop() - this.gPrevScrollTop;
    this.gPrevScrollTop = $(window).scrollTop();

    if(!$('#dev-toolbar').hasClass('tall-dev-toolbar') || $('#dev-toolbar').hasClass('dev-toolbar-pinned')) {
      return true;
    }

    // delta > 0 - scrolling down, move the toolbar offscreen unless it's already hiddden
    if( delta > 0 && -$('#dev-toolbar').position().top <= $('#dev-toolbar').outerHeight()) {
      $('#dev-toolbar').css({top:$('#dev-toolbar').position().top-delta});
    // delta < 0 - scrolling up, move the toolbar onscreen unless it's already fully visibe
    } else if ( delta < 0 && $('#dev-toolbar').position().top < 0) {
      // make sure scroll doesn't make top > 0
      var newTop = ($('#dev-toolbar').position().top-delta > 0) ? 0 : $('#dev-toolbar').position().top-delta
      $('#dev-toolbar').css({top:newTop});
    }
  },

  toggleDevToolbarMode: function() {
    $('#dev-toolbar').toggleClass('tall-dev-toolbar short-dev-toolbar');

    if($('#dev-toolbar').hasClass('tall-dev-toolbar')) {
      $('#dev-toolbar-toggle-mode').removeClass('icon-circle-down');
      $('#dev-toolbar-toggle-mode').addClass('icon-circle-up');
      FOXjs.setCookie('devToolbarMode', 'tall', null, '/');
    } else {
      $('#dev-toolbar-toggle-mode').removeClass('icon-circle-up');
      $('#dev-toolbar-toggle-mode').addClass('icon-circle-down');
      FOXjs.setCookie('devToolbarMode', 'short', null, '/');
    }

    $('#dev-toolbar-spacer').toggleClass('tall-dev-toolbar-spacer short-dev-toolbar-spacer');
    $('#dev-toolbar').css({top:0});
    this.updateDomColumnCount();
  },

  toggleDevToolbarVisibility: function() {
    $('#dev-toolbar').toggleClass('dev-toolbar-hidden');
    $('#dev-toolbar-spacer').toggle();
    $('#environment-indicator').toggle();

    if($('#dev-toolbar').hasClass('dev-toolbar-hidden')) {
      FOXjs.setCookie('devToolbarHidden', 'true', null, '/');
    } else {
      FOXjs.setCookie('devToolbarHidden', 'false', null, '/');
    }
  },

  toggleDevToolbarPinned: function() {
    $('#dev-toolbar').toggleClass('dev-toolbar-pinned');

    if($('#dev-toolbar').hasClass('dev-toolbar-pinned')) {
      $('#dev-toolbar-toggle-pin').removeClass('toggle-pinned-to-unpinned');
      $('#dev-toolbar-toggle-pin').addClass('toggle-unpinned-to-pinned');
      FOXjs.setCookie('devToolbarPinned', 'true', null, '/');
    } else {
      $('#dev-toolbar-toggle-pin').removeClass('toggle-unpinned-to-pinned');
      $('#dev-toolbar-toggle-pin').addClass('toggle-pinned-to-unpinned');
      FOXjs.setCookie('devToolbarPinned', 'false', null, '/');
    }

    $('#dev-toolbar').css({top: 0});
  },

  flush: function(flushUrl, e) {

    var refresh = false;
    if(e.ctrlKey) {
      refresh = true;
    }

    function flushSuccess() {
      $('#dev-toolbar-flush a').removeClass('icon-animated-spinner icon-cross');
      $('#dev-toolbar-flush a').addClass('icon-checkmark');
      if(refresh) {
        document.mainForm.submit();
        $('#dev-toolbar-refresh > a').click();
      }
    }

    function flushError() {
      $('#dev-toolbar-flush a').removeClass('icon-animated-spinner icon-cross icon-checkmark');
      $('#dev-toolbar-flush a').addClass('icon-cross');
    }

    var settings =  {
      cache: false,
      success: flushSuccess,
      error: flushError
    }

    $.ajax(flushUrl,settings);

    $('#dev-toolbar-flush a').removeClass('icon-checkmark icon-cross icon-bin');
    $('#dev-toolbar-flush a').addClass('icon-animated-spinner');
  },

  toggleFlushMode: function(flushAndRefresh) {
    if(flushAndRefresh || $('#dev-toolbar-flush').hasClass('flushing')) {
      $('#dev-toolbar-refresh').hide(0);
      $('#dev-toolbar-flush a').text('Flush & refresh');
    } else {
      $('#dev-toolbar-refresh').show(0);
      $('#dev-toolbar-flush a').text('Flush');
    }
  },

  buildTimingGraph: function(timings) {
    var totalWidth = 150;

    var totalComponentTime = timings.componentTimes.reduce(function(previousValue, currentValue, index, array) {
      return { time: previousValue.time + currentValue.time };
    }).time;

    var timingSummary = [];
    for(var i = 0; i<timings.componentTimes.length; i++) {
      var width = Math.ceil(timings.componentTimes[i].time / totalComponentTime * totalWidth);
      if(width<5) { width = 5 };

      //Generate colour based on type run through java string hashcode algo
      var hue = 0;
      for (var c = 0; c < timings.componentTimes[i].type.length; c++) {
        hue = 31 * hue + timings.componentTimes[i].type.charCodeAt(c);
      }
      hue = hue % 360;

      var additionalClasses = (timings.componentTimes[i].time > timings.componentTimes[i].warningThreshold) ? 'slow-component' : '';

      var tooltip = timings.componentTimes[i].label+' &lt;b&gt;'+timings.componentTimes[i].time+'&lt;/b&gt;ms';
      var bar = '<span class="component-time '+additionalClasses+'" style="width: '+width+'px; background: hsla('+hue+', 50%, 60%, 0.8)" id="dev-toolbar-timing-component-'+timings.componentTimes[i].type+'" title="'+tooltip+'"></span>';
      timingSummary.push(timings.componentTimes[i].label+' <b>'+timings.componentTimes[i].time+'</b>ms');
      $('#dev-toolbar-timing').append(bar);
    }

    $('#dev-toolbar-timing').append('<span id="dev-toolbar-timing-total"><b>' + timings.pageTime + '</b>ms</span>');

    var cumulativeTimingInfo = $('<ul></ul>');
    for(var i = 0; i < timings.cumulativeTimes.length; i++) {
      cumulativeTimingInfo.append('<li>' + timings.cumulativeTimes[i].label + ' <strong>' + timings.cumulativeTimes[i].time +  '</strong>ms</li>' )
    }
    var _this = this;

    $('#dev-toolbar-timing-total').tooltipster({
      functionBefore: function(origin, continueTooltip) {

        var modifiedTimingInfo = cumulativeTimingInfo.clone();

        if(_this.gUserExperienceTime != null) {
          modifiedTimingInfo.append('<li>User experience <strong>' + _this.gUserExperienceTime +'</strong>ms</li>');
        }

        origin.tooltipster('content', modifiedTimingInfo);
        continueTooltip();
      },
	  position: 'bottom'
    });

    $('.component-time').tooltipster({
      position: 'bottom'
    });
    /*
    // TODO - NP/ME - Add this back in when tooltip code more robust/changed
    if (timingSummary.length > 0) {
      var summaryTooltip = '<span class="dev-toolbar-tooltip">' + timingSummary.join('<br />') + '</span>';
      $('#dev-toolbar-timing-total').append(summaryTooltip);
    }*/
  },

  parseMessages: function(messages) {
    var errorCount = 0;
    var warningCount = 0;

    for(var i=0; i<messages.length; i++) {
      var msg = messages[i];

      switch(msg.level) {
        case 'error':
          errorCount++;
          break;
        case 'warning':
          warningCount++;
          break;
      }

      var div = '<div class="dev-toolbar-message dev-toolbar-message-'+msg.level+'"><h4><span class="dev-toolbar-message-type">'+msg.type+'</span> '+msg.subject+'</h4><p>'+msg.message+'</p></div>';
      $('#dev-toolbar-messages-tooltip').append(div);
    }

    var countStrings = [];

    if(errorCount > 0) {
      $('#dev-toolbar-messages').addClass('dev-toolbar-messages-error-icon');
    }
    else if(warningCount > 0) {
      $('#dev-toolbar-messages').addClass('dev-toolbar-messages-warning-icon');
    }

    if (errorCount > 0) {
      countStrings.push(errorCount + ((errorCount==1)?' error':' errors'));
    }
    if(warningCount > 0) {
      countStrings.push(warningCount + ((warningCount==1)?' warning':' warnings'));
    }

    $('#dev-toolbar-messages').append(countStrings.join(', '));

  },

  getTimingsAndMessages: function(Url) {

    function timingsAndMessagesSuccess(data) {
      DevToolbar.buildTimingGraph(data.timings);
      DevToolbar.parseMessages(data.messages)
    }

    function timingsAndMessagesError(xhr, status, error) {
      $('#dev-toolbar-timing').append('<span id="dev-toolbar-timing-total">Error fetching stats</span>');
    }

    var settings =  {
      cache: false,
      dataType: 'json',
      success: timingsAndMessagesSuccess,
      error: timingsAndMessagesError
    }

    $.ajax(Url,settings);
  },

  toggleAdvancedOpts: function (e) {
    if((e != undefined && $('#dev-toolbar-advanced-tooltip').css('display') == 'none') || $('#dev-toolbar-advanced input').is(':checked')) {
      $('#dev-toolbar-advanced-tooltip').show(0);
    }
    else {
      $('#dev-toolbar-advanced-tooltip').hide(0);
    }
  },

  //Add a tooltip for the context label info link
  setContextLabelInfo: function(pInfo) {
    $('#dev-toolbar-contexts').tooltipster({
      content: pInfo,
      position: 'bottom'
    });
  },

  setDbmsOutputInfo: function(pInfo) {
    $('#dbmsOutputInfo').tooltipster({
      content: pInfo,
      position: 'bottom'
    });
  },

  setUserExperienceTime: function(pTime) {
    this.gUserExperienceTime = pTime;
  }
};

// Reduce polyfill for IE8
// Production steps of ECMA-262, Edition 5, 15.4.4.21
// Reference: http://es5.github.io/#x15.4.4.21
if (!Array.prototype.reduce) {
  Array.prototype.reduce = function(callback /*, initialValue*/) {
    'use strict';
    if (this == null) {
      throw new TypeError('Array.prototype.reduce called on null or undefined');
    }
    if (typeof callback !== 'function') {
      throw new TypeError(callback + ' is not a function');
    }
    var t = Object(this), len = t.length >>> 0, k = 0, value;
    if (arguments.length == 2) {
      value = arguments[1];
    } else {
      while (k < len && ! k in t) {
        k++;
      }
      if (k >= len) {
        throw new TypeError('Reduce of empty array with no initial value');
      }
      value = t[k++];
    }
    for (; k < len; k++) {
      if (k in t) {
        value = callback(value, t[k], k, t);
      }
    }
    return value;
  };
}

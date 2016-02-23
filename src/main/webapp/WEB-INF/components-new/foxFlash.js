/*
 * FOX Flash
 *
 * Copyright 2016, Fivium Ltd.
 * Released under the BSD 3-Clause license.
 *
 * Dependencies:
 *   jQuery v1.11
 */

// Can be run through JSDoc (https://github.com/jsdoc3/jsdoc) and JSHint (http://www.jshint.com/)

/*jshint laxcomma: true, laxbreak: true, strict: false */

var FOXflash = {

  /**
   * Gets a jQuery pointing to the container div for flash messages, creating it if it doesn't exist.
   * @returns {jQuery}
   * @private
   */
  _$getFlashContainer : function() {
    var container = $('.flash-message');
    if(container.length === 0) {
      container = $('<div class="flash-message"></div>').prependTo($(document.body));
      $('.flash-message').hide();
    }

    return container;
  },

  /**
   * Creates a flash div in the correct location.
   * @param message Message to display - may contain HTML for formatting.
   * @param infoBoxClass CSS classes to set on the div.
   * @private
   */
  _insertFlashHTML : function(message, infoBoxClass) {
    var flashContainer = this._$getFlashContainer();
    flashContainer.append('<div role="alert" class="info-box info-box-' + infoBoxClass + '" tabindex="0">' +
      '<button class="flash-message-close icon-cross" aria-label="Close this message">' +
    '</button>' + message +'</div>');
    flashContainer.find('.flash-message-close').click(function() { $(this).parent().fadeOut(100); });
    flashContainer.delay(200).fadeIn(300);
    // The focus has to happen once the element is visible, 500ms should be when it's fully visible
    setTimeout(function() {
      if ($(document.activeElement).parents('.flash-message').length === 0) {
        // Only focus the first child in the flash message container if the active element isn't already something in the flash message container
        flashContainer.children().first().focus();
      }
    }, 500);
  },

  /**
   * Displays a non-modal, text-based flash message at the top of the screen.
   * @param {string} message Message to display - may contain HTML for formatting.
   * @param {Object} flashProperties Additional properties such as style rules for the flash.
   */
  textFlash : function(message, flashProperties) {
    flashProperties = $.extend({
      displayType : 'info',
      cssClass: ''
    }, flashProperties);

    this._insertFlashHTML(message, flashProperties.displayType + ' ' + flashProperties.cssClass);
  }
};

FOXflash = {

  /**
   * Gets a jQuery pointing to the container div for flash messages, creating it if it doesn't exist.
   * @returns {jQuery}
   * @private
   */
  _$getFlashContainer : function() {
    var container = $('.flash-message');
    if(container.length == 0) {
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
    flashContainer.append('<div role="alert" class="info-box info-box-' + infoBoxClass + '">' +
      '<div tabindex="0" class="flash-message-close icon-cross" title="Close this message" aria-label="Close this message">' +
    '</div>' + message +'</div>');
    flashContainer.find('.flash-message-close').click(function() { $(this).parent().fadeOut(100) });
    flashContainer.delay(200).fadeIn(300);
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

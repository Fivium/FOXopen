var FOXmodal = {

  /** Current DisplayedModals, in stack order. */
  modalStack: [],

  /**
   * Handles any initialisation actions which need to be performed if the page has been rendered with a visible modal popover
   * in the HTML.
   */
  _handleWindowLoad: function() {
    var $engineModal = $('#engine-modal-popover');
    if ($engineModal.is(':visible')) {
      this._createDisplayedModal($engineModal, 'engine-internal', false, null);
    }
  },

  /**
   * Gets the top modal currently on the stack, or undefined if the stack is empty.
   * @returns {DisplayedModal}
   * @private
   */
  _topModal: function() {
    return this.modalStack[this.modalStack.length - 1];
  },

  /**
   * Gets the modal currently underneath the top modal on the stack, or undefined if the stack contains 1 or 0 modals.
   * @returns {DisplayedModal}
   * @private
   */
  _underTopModal: function () {
    return this.modalStack[this.modalStack.length - 2];
  },

  _keyEventHandler: function(event) {

    if (event.which == 27 && FOXmodal._topModal() && FOXmodal._topModal().escapeAllowed) {
      //Escape key - dismiss top modal if allowed
      FOXmodal.dismissTopModal();
    }
    else if (event.which == 9) {
      //Tab key - prevent tabbing out of modal
      AccessibleModal.trapTabKey(FOXmodal._topModal().$containerDiv, event);
    }
  },

  _registerKeyListeners: function() {
    $(document).on('keydown', this._keyEventHandler);
  },

  _deregisterKeyListeners: function() {
    $(document).off('keydown', this._keyEventHandler);
  },

  /**
   * Constructs a DisplayedModal and adds it onto the stack.
   * @param {jQuery} $modalContainer Div containing the rendered modal.
   * @param {string} modalKey Key for the new modal.
   * @param {boolean} escapeAllowed True if an 'escape' keypress can close the modal.
   * @param {HTMLElement} restoreFocusTo DOM element to restore focus to on modal close.
   * @param {function} closeCallback Callback function to run when the modal is closed.
   */
  _createDisplayedModal: function($modalContainer, modalKey, escapeAllowed, restoreFocusTo, closeCallback) {
    this.modalStack.push(new DisplayedModal($modalContainer, modalKey, escapeAllowed, restoreFocusTo, closeCallback));
    if (this.modalStack.length == 1) {
      $('body').addClass('contains-popover');
      this._registerKeyListeners();
    }

    if (this.modalStack.length > 1) {
      //Ensure the z-index of the top modal is higher than the modal it is over
      this._topModal().$containerDiv.zIndex(this._underTopModal().$containerDiv.zIndex() + 1);
      //Disallow scrolling in the modal which is now beneath the top modal
      this._underTopModal().$containerDiv.addClass('contains-popover');
    }
  },

  /**
   * Displays a modal popover on the screen, which blocks clicks to underlying content.
   * @param {jQuery} $modalContent jQuery for the container of the popover content. The contents of this target will be copied into a new modal div.
   * @param {string} modalKey Key to identify the new modal, used to prevent the same modal being displayed repeatedly.
   * @param {object} modalOptions Additional options for the modal - title, size, etc.
   * @param {function} closeCallback Callback function to run when the modal is closed. The callback may take an object
   * parameter to be passed from dismissTopModal().
   */
  displayModal: function($modalContent, modalKey, modalOptions, closeCallback) {

    //Short circuit out if there is already a modal with the sanme key in the stack
    var matchingKeys = $.grep(this.modalStack, function(displayedModal) {
      return displayedModal.modalKey === modalKey;
    });
    if (matchingKeys.length > 0) {
      return;
    }

    modalOptions = $.extend({
      title: '',
      cssClass: '',
      size: 'regular',
      dismissAllowed: false,
      escapeAllowed: false,
      ariaRole: 'dialog',
      icon: ''
    }, modalOptions);

    //Note: until we add full Mustache support this must be kept in sync with ModalPopover.mustache
    var $modalContainer = $('<div class="modal-popover-container"><div class="modal-popover"><div class="modal-popover-content"></div></div></div>');

    if (modalOptions.title) {
      $modalContainer.find('.modal-popover-content').append('<h2 id="modal-title-' + modalKey + '">' + modalOptions.title + '</h2>');
      $modalContainer.find('.modal-popover-content').attr('aria-labelledby', 'modal-title-' + modalKey);
      //TODO: aria-describedby, needs to be sensible
    }

    $modalContainer.find('.modal-popover').addClass(modalOptions.size + '-popover').addClass(modalOptions.cssClass);

    //Copy CLONED contents so they are not removed from the DOM when we dimsiss the modal div
    $modalContainer.find('.modal-popover-content').append($modalContent.contents().clone());

    $modalContainer.find('.modal-popover-icon').addClass(modalOptions.icon);

    //Accessibility role
    $modalContainer.find('.modal-popover-content').attr('role', modalOptions.ariaRole);

    //Add new modal container to the start of the page body
    $('body').prepend($modalContainer);

    //Add dismiss icon if client side dismiss is allowed
    if (modalOptions.dismissAllowed) {
      $modalContainer.find('.modal-popover-content').prepend('<div class="icon-cancel-circle modal-dismiss" tabindex="1"></div>');
      var that = this;
      $modalContainer.find('.modal-dismiss').click(function(){
        that.dismissTopModal();
      });
    }

    //Construct tracker object and add to stack
    this._createDisplayedModal($modalContainer, modalKey, modalOptions.escapeAllowed, document.activeElement, closeCallback);
  },

  /**
   * Dismisses (closes) the top modal.
   * @param {Object} [callbackArgs] Arguments to pass to the callback function.
   */
  dismissTopModal: function(callbackArgs) {

    var topModal =  this.modalStack.pop();

    if(topModal) {
      topModal.$containerDiv.remove(); //Note: this will remove the engine modal div, so will need to be recreated
    }

    if(this.modalStack.length == 0) {
      $('body').removeClass('contains-popover');
      this._deregisterKeyListeners();
    }
    else {
      //Still a modal on the stack, make sure scrolling is now allowed in it
      this._topModal().$containerDiv.removeClass('contains-popover');
    }

    //If we popped a DisplayedModal, run close actions
    if(topModal) {
      //Restore focus to whatever had it before
      if(topModal.restoreFocusTo) {
        $(topModal.restoreFocusTo).focus();
      }

      //Run close callback function
      if(topModal.closeCallback) {
        topModal.closeCallback(callbackArgs);
      }
    }
  },

  /**
   * Updates the hidden scroll position field to the current scroll position of the internal engine modal, if it is on the stack.
   */
  updateScrollPositionInput: function() {

    for (var i=0; i < this.modalStack.length; i++) {
      if (this.modalStack[i].isInternal) {
        $("input[name='modal_scroll_position']").val(Math.round(this.modalStack[i].$containerDiv.scrollTop()));
      }
    }
  },

  /**
   * Gets the jQuery object containing the currently displayed modal, or undefined if no modal is displayed.
   * @returns {jQuery}
   */
  getCurrentModalContainer: function() {
    return this._topModal().$containerDiv;
  }

};

/**
 * Creates a new DisplayedModal object which can be tracked by the FOXModal stack.
 * @param {jQuery} containerDiv element for the rendered modal
 * @param {string} modalKey key for the new modal.
 * @param {boolean} escapeAllowed True if an 'escape' keypress can close the modal.
 * @param {HTMLElement} restoreFocusTo DOM element to restore focus to on modal close.
 * @param {function} closeCallback Callback function to run when this modal is closed.
 * @constructor
 */
function DisplayedModal(containerDiv, modalKey, escapeAllowed, restoreFocusTo, closeCallback) {

  this.$containerDiv = containerDiv;
  this.modalKey = modalKey;
  this.isInternal = modalKey == 'engine-internal';
  this.escapeAllowed = escapeAllowed;
  this.restoreFocusTo = restoreFocusTo;
  this.closeCallback = closeCallback;

  if (this.isInternal && this.$containerDiv.data('initial-scroll-position')) {
    this.$containerDiv.scrollTop(this.$containerDiv.data('initial-scroll-position'));
  }
}

DisplayedModal.prototype = {
  $containerDiv: null,
  modalKey: null,
  isInternal: false,
  escapeAllowed: false,
  restoreFocusTo: null,
  closeCallback: null
};

//Modal event listeners

$(document).on('foxBeforeSubmit', function() {
  FOXmodal.updateScrollPositionInput();
});

$(document).ready(function() {
  FOXmodal._handleWindowLoad();
});

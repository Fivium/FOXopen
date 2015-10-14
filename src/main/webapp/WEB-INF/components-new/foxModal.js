var FOXmodal = {

  /** Current DisplayedModals, in stack order. */
  modalStack: [],

  /**
   * Handles any initialisation actions which need to be performed if the page has been rendered with a visible modal popover
   * in the HTML.
   */
  handleWindowLoad: function() {
    var $engineModal = $('#engine-modal-popover');
    if ($engineModal.is(':visible')) {
      this.createDisplayedModal($engineModal, 'engine-internal');
    }
  },

  /**
   * Gets the top modal currently on the stack, or undefined if the stack is empty.
   * @returns {DisplayedModal}
   * @private
   */
  topModal: function() {
    return this.modalStack[this.modalStack.length - 1];
  },

  /**
   * Gets the modal currently underneath the top modal on the stack, or undefined if the stack contains 1 or 0 modals.
   * @returns {DisplayedModal}
   * @private
   */
  underTopModal: function () {
    return this.modalStack[this.modalStack.length - 2];
  },

  /**
   * Constructs a DisplayedModal and adds it onto the stack.
   * @param $modalContainer Div containing the rendered modal.
   * @param modalKey Key for the new modal.
   */
  createDisplayedModal: function($modalContainer, modalKey) {
    this.modalStack.push(new DisplayedModal($modalContainer, modalKey));
    if (this.modalStack.length == 1) {
      $('body').addClass('contains-popover');
    }

    if (this.modalStack.length > 1) {
      //Ensure the z-index of the top modal is higher than the modal it is over
      this.topModal().$containerDiv.zIndex(this.underTopModal().$containerDiv.zIndex() + 1);
      //Disallow scrolling in the modal which is now beneath the top modal
      this.underTopModal().$containerDiv.addClass('contains-popover');
    }
  },

  /**
   * Displays a modal popover on the screen, which blocks clicks to underlying content.
   * @param {*|jQuery|HTMLElement} $modalContent jQuery for the container of the popver content. The contents of this target will be copied into a new modal div.
   * @param modalKey Key to identify the new modal, used to prevent the same modal being displayed repeatedly.
   * @param {object} modalOptions Additional options for the modal - title, size, etc.
   */
  displayModal: function($modalContent, modalKey, modalOptions) {

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
      dismissAllowed: false
    }, modalOptions);

    //Note: until we add full Mustache support this must be kept in sync with ModalPopover.mustache
    var $modalContainer = $('<div class="modal-popover-container"><div class="modal-popover"><div class="modal-popover-content"></div></div></div>');

    if (modalOptions.title) {
      $modalContainer.find('.modal-popover-content').append('<h2>' + modalOptions.title + '</h2>');
    }

    $modalContainer.find('.modal-popover').addClass(modalOptions.size + '-popover').addClass(modalOptions.cssClass);

    //Copy CLONED contents so they are not removed when we dimsiss the modal div
    $modalContainer.find('.modal-popover-content').append($modalContent.contents().clone());

    //Add new modal container to the start of the page body
    $('body').prepend($modalContainer);

    //Add dismiss icon if client side dismiss is allowed
    if (modalOptions.dismissAllowed) {
      $modalContainer.find('.modal-popover-content').prepend('<div class="icon-cancel-circle modal-dismiss"></div>');
      var that = this;
      $modalContainer.find('.modal-dismiss').click(function(){
        that.dismissTopModal();
      });
    }

    //Construct tracker object and add to stack
    this.createDisplayedModal($modalContainer, modalKey);
  },

  /**
   * Dismisses (closes) the top modal.
   */
  dismissTopModal: function() {

    var topModal =  this.modalStack.pop();

    if(topModal) {
      topModal.$containerDiv.remove(); //Note: this will remove the engine modal div, so will need to be recreated
    }

    if(this.modalStack.length == 0) {
      $('body').removeClass('contains-popover');
    }
    else {
      //Still a modal on the stack, make sure scrolling is now allowed in it
      this.topModal().$containerDiv.removeClass('contains-popover');
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
  }
};

/**
 * Creates a new DisplayedModal object which can be tracked by the FOXModal stack.
 * @param containerDiv {*|jQuery|HTMLElement} Div element for the rendered modal
 * @param modalKey {string} key for the new modal.
 * @constructor
 */
function DisplayedModal(containerDiv, modalKey) {

  this.$containerDiv = containerDiv;
  this.modalKey = modalKey;
  this.isInternal = modalKey == 'engine-internal';

  if (this.isInternal && this.$containerDiv.data('initial-scroll-position')) {
    this.$containerDiv.scrollTop(this.$containerDiv.data('initial-scroll-position'));
  }
}

DisplayedModal.prototype = {
  $containerDiv: null,
  modalKey: null,
  isInternal: false
};

//Modal event listeners

$(document).on('foxBeforeSubmit', function() {
  FOXmodal.updateScrollPositionInput();
});

$(document).ready(function() {
  FOXmodal.handleWindowLoad();
});
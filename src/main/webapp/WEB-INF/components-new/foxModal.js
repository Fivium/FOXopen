var FOXmodal = {

  /**
   * Gets the overall container div for the modal popover.
   * @returns {*|jQuery|HTMLElement}
   * @private
   */
  containerDiv: function() {
    return $('#modal-popover-container');
  },

  /**
   * Gets the div containing the modal popover's content.
   * @returns {*|jQuery|HTMLElement}
   * @private
   */
  contentDiv: function() {
    return $('#modal-popover-content');
  },

  /**
   * Handles any initialisation actions which need to be performed if the page has been rendered with a visible modal popover
   * in the HTML.
   */
  handleWindowLoad: function() {
    if(this.containerDiv().is(':visible') && this.containerDiv().data('initial-scroll-position')) {
      this.containerDiv().scrollTop(this.containerDiv().data('initial-scroll-position'));
    }
  },

  /**
   * Displays a modal popover on the screen, which blocks clicks to underlying content.
   * @param {string} contentLocator jQuery locator pointing to the container of the popver content. The contents of this target will be copied into the modal div.
   * @param {object} modalOptions Additional options for the modal - title, size, etc.
   */
  displayModal: function(contentLocator, modalOptions) {

    //Close any existing popover
    this.dismissModal();

    modalOptions = $.extend({
      title: '',
      cssClass: '',
      size: 'regular',
      saveScrollPosition: false
    }, modalOptions);

    $('body').addClass('contains-popover');

    this.containerDiv().toggle(true);
    this.containerDiv().data('save-scroll-position', modalOptions.saveScrollPosition);

    if(modalOptions.title) {
      this.contentDiv().append('<h2>' + modalOptions.title + '</h2>');
    }

    $('#modal-popover').addClass(modalOptions.size + '-popover').addClass(modalOptions.cssClass);

    this.contentDiv().append($(contentLocator).contents());
  },

  /**
   * Dismisses (closes) the current modal.
   */
  dismissModal: function() {
    this.containerDiv().toggle(false);
    $('body').removeClass('contains-popover');
    this.contentDiv().empty();
    this.contentDiv().attr('class','');
  },

  /**
   * Updates the hidden scroll position field to the current scroll position of the modal, if it is visible.
   */
  updateScrollPositionInput: function() {
    if (this.containerDiv().is(':visible') && this.containerDiv().data('save-scroll-position')) {
      $("input[name='modal_scroll_position']").val(Math.round(this.containerDiv().scrollTop()));
    }
  }
};

//Modal event listeners

$(document).on('foxBeforeSubmit', function() {
  FOXmodal.updateScrollPositionInput();
});

$(document).ready(function() {
  FOXmodal.handleWindowLoad();
});
FOXalert = {

  onLoadAlertQueue: [],

  alertCount: 0,

  /**
   * Gets a jQuery of HTML for an alert close button.
   * @param closePrompt
   * @returns {jQuery}
   * @private
   */
  _getAlertCloseControl: function(closePrompt) {
    return $('<ul class="modal-popover-actions"><li><button class="primary-button alert-dismiss" onclick="FOXmodal.dismissTopModal(); return false;">' + closePrompt + '</button></li></ul>');
  },

  /**
   * Gets a jQuery of HTML for an confirm "OK" and "Cancel" button.
   * @returns {jQuery}
   * @private
   */
  _getConfirmCloseControl: function() {
    return $('<ul class="modal-popover-actions">' +
      '<li><button class="primary-button alert-dismiss" onclick="FOXmodal.dismissTopModal({confirmResult: true}); return false;">OK</button></li>' +
      '<li><button class="link-button" onclick="FOXmodal.dismissTopModal({confirmResult: false}); return false;">Cancel</button></li></ul>');
  },

  /**
   * Gets an object containing styling properties to apply to an alert modal.
   * @param alertStyle Alert style enum.
   * @returns {{cssClass: string, icon: string}}
   * @private
   */
  _getAlertStyleData: function(alertStyle) {

    var result = {cssClass: 'modal-alert-' + alertStyle, icon: ''};

    switch (alertStyle) {
      case 'info':
        result.icon = 'icon-info'; break;
      case 'success':
        result.icon = 'icon-checkmark'; break;
      case 'warning':
        result.icon = 'icon-warning'; break;
      case 'danger':
        result.icon = 'icon-cross'; break;
      case 'confirm':
        result.icon = 'icon-question'; break;
    }

    return result;
  },

  /**
   * Displays an alert using FOXmodal.
   * @param $alertContent Alert content locator.
   * @param alertProperties Modal properties.
   * @param callback Callback to run on alert close.
   * @private
   */
  _displayAlert: function($alertContent, alertProperties, callback) {

    alertProperties = $.extend({
      size: 'small',
      alertStyle: 'normal',
      title: 'Alert',
      ariaRole: 'alertdialog',
      escapeAllowed: true,
      closePrompt: 'OK',
      isConfirm: false
    }, alertProperties);

    var alertKey = 'FOXalert' + (this.alertCount++);

    //Add close controls to the content container
    if (alertProperties.isConfirm) {
      $alertContent.append(this._getConfirmCloseControl());
    }
    else {
      $alertContent.append(this._getAlertCloseControl(alertProperties.closePrompt));
    }

    //Resolve alertStyle enum to icon/CSS classes
    var styleData = this._getAlertStyleData(alertProperties.alertStyle);

    alertProperties.cssClass = (alertProperties.cssClass ? alertProperties.cssClass + ' ' : '') + styleData.cssClass;
    alertProperties.icon = styleData.icon;

    //Show the modal
    FOXmodal.displayModal($alertContent, alertKey, alertProperties, callback);

    //Default focus on close action
    FOXmodal.getCurrentModalContainer().find('.alert-dismiss').focus();
  },

  /**
   * Displays a text-based alert.
   * @param alertText Alert text, which may contain HTML tags.
   * @param alertProperties Additional properties for the alert.
   * @param callback Callback to run on alert close.
   */
  textAlert: function(alertText, alertProperties, callback) {
    this._displayAlert($('<div><div class="modal-popover-icon"></div><div class="modal-popover-text">' + alertText +'</div></div>'), alertProperties, callback);
  },

  /**
   * Displays an alert with the contents of the given buffer.
   * @param $buffer Buffer locator.
   * @param alertProperties Additional properties for the alert.
   * @param callback Callback to run on alert close.
   */
  bufferAlert: function($buffer, alertProperties, callback) {
    this._displayAlert($buffer, alertProperties, callback);
  },

  /**
   * Displays a text-based confirm dialog.
   * @param confirmText Text of the confirm message.
   * @param confirmProperties Properties to pass to alert/modal functions.
   * @param successCallback Callback to run if the user clicks "OK".
   * @param cancelCallback Callback to run if the user clicks "Cancel".
   */
  textConfirm: function(confirmText, confirmProperties, successCallback, cancelCallback) {

    var callback = function(callbackResult) {
      if (callbackResult && callbackResult.confirmResult && successCallback) {
        successCallback();
      }
      else if (callbackResult && !callbackResult.confirmResult && cancelCallback) {
        cancelCallback();
      }
    };

    confirmProperties = $.extend({isConfirm: true, title: '', alertStyle: 'confirm'}, confirmProperties);

    this.textAlert(confirmText, confirmProperties, callback);
  },

  /**
   * Enqueues an alert for display when the page onLoad event fires.
   * @param alertProperties All alert properties, including message and alertType.
   */
  enqueueOnLoadAlert: function(alertProperties) {
    this.onLoadAlertQueue.push(alertProperties);
  },

  /**
   * Displays the next alert in the onLoad queue, if one exists.
   */
  processNextOnLoadAlert: function() {

    var alertProperties = this.onLoadAlertQueue.shift();
    if (alertProperties) {

      var that = this;
      var callback = function() { that.processNextOnLoadAlert(); };

      //Note callback chaining so only one alert is displayed at a time

      switch (alertProperties.alertType) {
        case 'native':
          //Convert newline strings to actual newlines (legacy behaviour migrated from HTML generator escaping logic)
          alert(alertProperties.message.replace(/\\n/g,'\n'));
          this.processNextOnLoadAlert();
          break;
        case 'text':
          this.textAlert(alertProperties.message, alertProperties, callback);
          break;
        case 'buffer':
          this.bufferAlert($('#' + alertProperties.bufferId), alertProperties, callback);
          break;
        default:
          throw 'Unknown alert type ' + alertProperties.alertType;
      }
    }
  }
};


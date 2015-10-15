FOXalert = {

  onLoadAlertQueue: [],

  alertCount: 0,

  _getAlertCloseControl: function(closePrompt) {
    return $('<ul class="modal-popover-actions"><li><button class="primary-button alert-dismiss" onclick="FOXmodal.dismissTopModal(); return false;">' + closePrompt + '</button></li></ul>');
  },

  _displayAlert: function($alertContent, alertProperties, callback) {

    alertProperties = $.extend({
      size: 'small',
      cssClass: 'modal-alert-info',
      title: 'Alert',
      ariaRole: 'alertdialog',
      closePrompt: 'OK'
    }, alertProperties);

    var alertKey = 'FOXalert' + (this.alertCount++);

    $alertContent.append(this._getAlertCloseControl(alertProperties.closePrompt));

    FOXmodal.displayModal($alertContent, alertKey, alertProperties, callback);

    //Default focus on close action
    FOXmodal.getCurrentModalContainer().find('.alert-dismiss').focus();
  },


  textAlert: function(alertText, alertProperties, callback) {
    this._displayAlert($('<div><span>' + alertText +'</span></div>'), alertProperties, callback);
  },

  bufferAlert: function($buffer, alertProperties, callback) {
    this._displayAlert($buffer, alertProperties, callback);
  },

  enqueueOnLoadAlert: function(alertProperties) {
    this.onLoadAlertQueue.push(alertProperties);
  },

  processNextOnLoadAlert: function() {

    var alertProperties = this.onLoadAlertQueue.shift();
    if (alertProperties) {

      var that = this;
      var callback = function() { that.processNextOnLoadAlert(); };

      switch (alertProperties.alertType) {
        case 'native':
          alert(alertProperties.message);
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


/*jshint laxcomma: true, laxbreak: true, strict: false */
var DELETE_UPLOAD_CLIENT_ACTION_TYPE  = 'DeleteUploadFile';
var SERVER_ERROR_START = 'start';
var SERVER_ERROR_RECEIVE = 'receive';
var POLL_STATUS_INTERVAL = 200;

function FileUpload (container, urlBase, urlParams, fileList, widgetOptions) {
  this.container = container;
  this.urlBase = urlBase;
  this.startUrlParams = urlParams;
  this.fileList = [];
  this.widgetOptions = widgetOptions;
  this.fileListContainer = container.children("ul");

  var _this = this;

  //Initialise the fileupload plugin on the file input
  var fileUpload = container.find('.fileUploadInput').fileupload({
    url: _this.generateURL('receive'),
    dataType: 'json',
    dropZone: $('div[data-dropzone-id="' + _this.container.attr('id') + '"]'),
    sequentialUploads: true,
    add: function(e, data) { _this.handleUploadAdd(e, data); },
    submit: function(e, data) { return _this.handleUploadSubmit(e, data); }
  });

  if(widgetOptions.readOnly) {
    //Remove the dropzone for read only widgets
    $('div[data-dropzone-id="' + _this.container.attr('id') + '"]').remove();
  }

  //Overload start/finish actions for modal + modeless widgets
  if(widgetOptions.widgetMode === "modal") {
    this.startUploadGUIActions = function(){
      FOXjs.setPageDisabled(true);
    };

    //TODO PN proper JS OO
    this.finishUploadGUIActions = function(wasSuccess) {
      //Completion handled flag prevents this running multiple times if completion handlers run simultaneously
      if (!this.completionHandled) {
        FOXjs.setPageDisabled(false);
        $('.modalUploadBlocker').remove();

        if (wasSuccess && this.widgetOptions.successAction !== null) {
          FOXjs.action(this.widgetOptions.successAction);
        }
        else if (!wasSuccess && this.widgetOptions.failAction !== null) {
          FOXjs.action(this.widgetOptions.failAction);
        }

        this.completionHandled = true;
      }
    };
  }

  //Convert existing files into file objects
  if(fileList !== null) {
    for(var i =0; i < fileList.length; i++) {
      this.addFileInfo(fileList[i]);
    }
  }

  //Make the dropzone display how many files can be dropped
  _this.updateDropzoneMaxFiles();

  //Disable if the file limit is already exceeded
  if(this.fileList.length >= this.widgetOptions.maxFiles) {
    this.disableUpload();
  }
  else {
    this.enableUpload();
  }

  //Attach event listeners to file upload

  fileUpload.bind('fileuploadstart', function (e) {
    //Called before any files are uploaded
    _this.startUploads(e);
  });

  fileUpload.bind('fileuploadsend', function (e, data) {
    //Called when an individual upload starts
    //Update GUI to reflect start
    data.files[0]._foxFileInfo.uploadStarted(e);

    //Re-disable uploads now the send has happened (they have to be temporarily enabled to allow the send)
    _this.getUploadInput().fileupload('disable');
  });

  fileUpload.bind('fileuploaddone', function (e, data) {
    //Individual upload has completed - update the GUI item
    if(data.result === null) {
      //Interpret a null result as a failure (iframe aborted requests return null and do not call fail event)
      _this.handleFail(null, data.files[0]._foxFileInfo, SERVER_ERROR_RECEIVE);
    }
    else {
      data.files[0]._foxFileInfo.uploadFinished(data.result);
    }

    //Unlock GUI if this was the last upload, or start the next upload
    _this.handleUploadCompletion();
  });

  fileUpload.bind('fileuploadfail', function (e, data) {
    ////Called in the event of a serious error (i.e. 500 response/request abort)
    _this.handleFail(null, data.files[0]._foxFileInfo, SERVER_ERROR_RECEIVE);
  });
}

// Prototype definition
FileUpload.prototype = {
  fileUploadInput: null,
  container: null,
  urlBase: null,
  startUrlParams: null, //Object containing params required for an upload start request (thread id, context ref, etc)
  fileList: null, //Array of file objects
  fileListContainer: null, //DOM UL containing the file list display
  widgetOptions: null,
  lastUploadHadError: false, //Tracker for if any file caused an error during the last upload
  pendingQueue: [],

  generateURL: function(action, params) {
    return this.urlBase + "/" + action  + (params !== null ? "?" + $.param(params) : "");
  },

  generateStartURL: function() {
    return this.generateURL('start', this.startUrlParams);
  },

  generateStartParams: function(filename) {
    return [
      {name: 'clientActions', value: JSON.stringify(FOXjs.dequeueClientActions(DELETE_UPLOAD_CLIENT_ACTION_TYPE))},
      {name: 'filename', value: filename}
    ];
  },

  submitNextPending: function() {
    //Submit (i.e. start) the next pending upload if there is one
    if(this.pendingQueue.length > 0) {
      this.pendingQueue.splice(0, 1)[0].submit();
    }
  },

  handleUploadAdd: function(e, data) {
    //Called once for each file added - data.originalFiles is a list of all files added in the change/drop event

    //Don't add if the widget is at capacity
    if (this.checkMaxFilesOnAdd(e, data)) {

      //Display the file info box in the page immediately
      var fileInfo = this.addFileInfo({filename: data.files[0].name});

      //Store a reference to our FileInfo in the library's object for retrieval later
      data.files[0]._foxFileInfo = fileInfo;

      //Enqueue this upload for later submission
      this.pendingQueue.push(data);

      //If this is the last file of this "batch", start queue processing
      if (data.files[0] === data.originalFiles[data.originalFiles.length - 1]) {
        this.submitNextPending();
      }
    }
  },

  handleUploadSubmit: function(e, data) {
    var _this = this;

    //Call start to get an upload ID - send client actions to apply any pending deletes
    $.post(_this.generateStartURL(), _this.generateStartParams(data.files[0].name), function(startResult) {
      //Merge uploadInfoId for this upload into the fileInfo
      $.extend(data.files[0]._foxFileInfo, startResult);

      //Send the uploadInfoId and DOM ref back to the servlet as part of the /receive request
      data.formData = startResult;

      //Just in time enable the fileupload, otherwise the plugin blocks the send (it's re-disabled in start handler)
      _this.getUploadInput().fileupload('enable');

      //Manually call the send event to start the upload
      data.jqXHR = _this.getUploadInput().fileupload('send', data);
      data.files[0]._foxFileInfo.jqXHR = data.jqXHR;
    }, 'json')
        .fail(function(jqXHR, textStatus, errorThrown) {
          _this.handleFail(jqXHR.responseJSON, data.files[0]._foxFileInfo, SERVER_ERROR_START);
        });

    return false;
  },

  startUploadGUIActions: function() {
    //Stop user navigating away from the page
    FOXjs.blockSubmit("A file upload is in progress. You must wait for it to complete before performing any further actions.");
  },

  finishUploadGUIActions: function(wasSuccess) {
    //Allow form to be submitted again
    FOXjs.allowSubmit();
    this.updateDropzoneMaxFiles();
  },

  addFileInfo: function(fileProps) {
    var fileInfo = $.extend(new FileInfo(this), fileProps);
    this.fileList.push(fileInfo);

    fileInfo.displayInPage();

    return fileInfo;
  },

  startUploads: function(e) {
    this.startUploadGUIActions();
    this.disableUpload();
  },

  getUploadInput: function() {
    return this.container.find('.fileUploadInput');
  },

  getUploadLink: function() {
    return this.container.find('.fileUploadLink');
  },

  enableUpload: function() {
    if(this.fileList.length < this.widgetOptions.maxFiles) {
      this.getUploadInput().toggle(true);
      this.getUploadLink().toggle(true);
      this.getUploadInput().fileupload('enable');
      this.container.removeClass('disableUpload');
      this.container.find('.dropzone').removeClass('disableUpload');
      $('body>.dropzone').removeClass('disableUpload');
    }
  },

  disableUpload: function() {
    this.getUploadInput().fileupload('disable');
    this.getUploadInput().toggle(false);
    this.getUploadLink().toggle(false);
    this.container.addClass('disableUpload');
    this.container.find('.dropzone').addClass('disableUpload');
    $('body>.dropzone').addClass('disableUpload');
  },

  //Common behaviour for success or failure
  handleUploadCompletion: function() {
    //If all uploads are complete, unlock page etc
    if(this.pendingQueue.length === 0) {
      this.finishUploadGUIActions(!this.lastUploadHadError);
      this.enableUpload();
    }

    //Submit the next upload
    this.submitNextPending();
  },

  handleFail: function(responseJSON, fileInfo, serverErrorType) {
    //If abortRequested is set we've already handled the fail
    if(!fileInfo.abortRequested) {
      //Called in the event of a serious error (i.e. 500 response, request abort)
      //Tell container an error occurred
      this.uploadHadError();

      //Process the next upload/unlock the page
      this.handleUploadCompletion();

      //Display failure information in UI element
      fileInfo.uploadFinished($.extend({serverError: serverErrorType}, responseJSON));
    }
  },

  uploadHadError: function() {
    this.lastUploadHadError = true;
  },

  deleteFile: function(fileInfo, skipConfirm) {

    var confirmDelete = false;
    if(this.widgetOptions.deleteConfirmText !== null && !skipConfirm) {
      confirmDelete = confirm(this.widgetOptions.deleteConfirmText);
    }
    else {
      confirmDelete = true;
    }

    if(confirmDelete) {
      FOXjs.enqueueClientAction(DELETE_UPLOAD_CLIENT_ACTION_TYPE, this.widgetOptions.deleteActionKey, {file_container_ref: fileInfo.uploadDomRef});

      //Search through and remove
      var lIndex = -1;
      for(i = 0; i < this.fileList.length; i++) {
        if(this.fileList[i] === fileInfo) {
          lIndex = i;
          break;
        }
      }

      //Remove the file info from the array
      if(lIndex !== -1) {
        this.fileList.splice(lIndex, 1);
      }
      else {
        throw "Could not locate file info object in list";
      }

      //Attempt to re-enable the widget if there is now space for more uploads
      this.enableUpload();

      this.updateDropzoneMaxFiles();

      return true;
    }
    else {
      return false;
    }
  },

  updateDropzoneMaxFiles: function() {
    if(this.widgetOptions.maxFiles === 1) {
      this.container.find('.dropzone .dropzone-text').text('Drop file here');
      return;
    }

    if(this.widgetOptions.maxFiles > 1000) {
      this.container.find('.dropzone .dropzone-text').text('Drop files here');
      return;
    }

    var maxFilesRemaining = this.widgetOptions.maxFiles - this.fileList.length;
    var maxFilesText;
    if(maxFilesRemaining === 1) {
      maxFilesText = '1 more file allowed';
    }
    else {
      maxFilesText = maxFilesRemaining + ' more files allowed';
    }

    this.container.find('.dropzone .dropzone-max-files-text').text(maxFilesText);
    $('body>.dropzone .dropzone-max-files-text').text(maxFilesText);
  },

  checkMaxFilesOnAdd: function(e, data) {
    //Checks that the files selected or dropped won't put us over the max file limit.
    //If they would, return false in the callback for submit to block the upload
    var _this = this;
    var filesAllowed = _this.widgetOptions.maxFiles - $.grep(_this.fileList,function(file, index) { return file.fileId !== undefined; }).length;

    if(data.originalFiles.length > filesAllowed) {
      //This will fire once for each file. We only want to alert once, so do it for the first file
      if (data.files[0] === data.originalFiles[0]) {
        alert('You tried to upload ' + data.originalFiles.length + ' files, but there\'s only room for ' + filesAllowed + '. Please try again with fewer files.');
      }
      return false;
    }
    else {
      return true;
    }
  }
};


//Drag and drop handling - TODO PN need to hide if widget disabled
$(document).ready(function() {

  //Don't bind drag and drop events if browser doesn't support HTML5 file API
  if(window.FileReader === undefined) {
    return;
  }

  $(document).on('dragenter', function (e) {
    var dt = e.originalEvent.dataTransfer;
    // Check drag contains files
    // 'types' is an array in Chrome and a DOMStringList in IE and Firefox so need to use .indexOf for Chrome and .contains for IE and Fx
    if(dt.types !== null && (dt.types.indexOf ? dt.types.indexOf('Files') !== -1 : dt.types.contains('application/x-moz-file') || dt.types.contains('Files'))) {
      $('.dropzone').not('.disableUpload').show();
      $('.fileUploadLink').hide();
    }
  });

  $(document).on('dragleave', function (e) {
    //Avoid IE/Chrome flickering bugs by checking if the drag was outside the window bounds
    if (e.originalEvent.clientX <= 0 || e.originalEvent.clientY <= 0 || e.originalEvent.clientX >= window.innerWidth || e.originalEvent.clientY >= window.innerHeight) {
      $('.dropzone').hide();
      $('.fileUploadLink').show();
    }
    else {
      return false;
    }
  });

  $(document).on('drop', function (e) {
    $('.dropzone').hide();
    $('.fileUploadLink').show();
  });

  $('.dropzone').on('dragover', function (e) {
    $(this).addClass('mouseOver');
  });

  $('.dropzone').on('drop dragleave', function (e) {
    $(this).removeClass('mouseOver');
  });

  //Prevents dragging into the rest of the window
  $(document).bind('drop dragover', function (e) {
    e.preventDefault();
  });

});


function FileInfo(owner) {
  this.owner = owner;
}

FileInfo.prototype = {
  owner: null, //FileUpload object which owns this FileInfo
  status: null,
  filename: null,
  fileId: null,
  errorMessage: null,
  errorStack: null,
  serverError: null, //type of server error (start/receive) if one occurred
  fileSize: null,
  container: null, //li element containing this file's details
  downloadUrl: null,
  uploadDomRef: null, //foxid of the element where this upload was delivered
  uploadInfoId: null,

  percentComplete: 0,
  statusInterval: null,
  allowStatusUpdates: true,
  statusUpdateFailures: 0,
  abortRequested: false, //if an abort has been requested due to an errorMessage in the status
  completionHandled: false, //if the completion handler has been called (modal only)
  jqXHR: null, //the JQXHR object performing this upload

  generateUploadInfoURL: function(action, extraParams) {
    return this.owner.generateURL(action, $.extend({uploadInfoId: this.uploadInfoId}, extraParams));
  },

  displayInPage: function() {

    this.container = $('<li class="fileInfo"></li>');
    this.container.appendTo(this.owner.fileListContainer);

    //Error messages take precedence
    if(this.errorMessage !== null) {
      this.displayErrorMessage();
    }
    else if(this.fileId !== null) {
      this.displayDownloadUrl();
    }
    else {
      this.displayPendingInfo();
    }
  },

  clearContainer: function() {
    //Remove pending info
    this.container.empty();
  },

  uploadStarted: function(e) {

    //Show progress bar container and status div
    this.clearContainer();
    this.createProgressBar();

    this.container.addClass('currentUpload');

    this.setStatusString('Upload starting...');
    this.setUploadSpeedString('?');
    this.setTimeRemainingString('unknown');
    this.allowStatusUpdates = true;

    var _this = this;
    this.statusInterval = setInterval(
        function() {
          _this.pollStatus();
        },
        POLL_STATUS_INTERVAL
    );
  },

  pollStatus: function() {
    var _this = this;
    $.ajax({
      url: _this.generateUploadInfoURL('status'),
      dataType: 'json'
    })
        .done(function(data) {
          _this.statusUpdateFailures = 0;
          _this.updateStatus(data);
        })
        .fail(function() {
          if(++_this.statusUpdateFailures > 10) {
            //Stop status polling if FOX is consistently reporting an error
            clearInterval(_this.statusInterval);
          }
        });
  },

  updateStatus: function(statusObject) {

    if(this.allowStatusUpdates) {
      //Status poll may report a failure for iframe transport (the iframe doesn't report aborted requests)
      if("errorMessage" in statusObject) {
        this.jqXHR.abort();
        this.abortRequested = true;
        this.owner.handleFail(statusObject, this, SERVER_ERROR_RECEIVE);
      }
      else if (statusObject.statusText !== "unknown") {
        this.setStatusString(statusObject.statusText);
        this.setUploadSpeedString(statusObject.uploadSpeed);
        this.setTimeRemainingString(statusObject.timeRemaining);
        this.setPercentComplete(statusObject.percentComplete, function(){});
      }
    }
  },

  setPercentComplete: function(percentComplete, callback) {
    var barPct = this.container.find('.progressBarPct');
    if(percentComplete > this.percentComplete) {
      barPct.animate({'width': percentComplete + '%'}, POLL_STATUS_INTERVAL, 'linear', callback);
      barPct.text(percentComplete + '%');
    }
    else {
      callback();
    }
    this.percentComplete = percentComplete;
  },

  setStatusString: function(statusString) {
    this.container.find('.uploadProgress .status').text(statusString);
  },

  setUploadSpeedString: function(uploadSpeed) {
    this.container.find('.uploadProgress .uploadSpeed').text(uploadSpeed);
  },

  setTimeRemainingString: function(timeRemaining) {
    this.container.find('.uploadProgress .timeRemaining').text(timeRemaining);
  },

  uploadFinished: function(result) {

    //Merge the result JSON into this object
    $.extend(this, result);

    //Stop receiving status updates
    this.allowStatusUpdates = false;

    //Stop polling for status
    clearInterval(this.statusInterval);

    //Error logic: errorMessage may have been set by either the response from the upload, or from a status poll.
    //In the event of a 500 error or upload abort, we may not have an error message, but serverError will be set - in this
    //case we need to go to the server again to ask for the error message.
    if(this.errorMessage !== null) {
      this.handleError();
      //Tell the container that there was an error
      this.owner.uploadHadError();
    }
    else if(result !== null && "serverError" in result) {
      //Ask server for error message if an error occurred but we don't know what it was
      this.handleServerError();
      //Tell the container that there was an error
      this.owner.uploadHadError();
    }
    else {
      this.handleSuccess();
    }
  },

  handleError: function() {
    this.clearContainer();
    this.displayErrorMessage();
  },

  handleServerError: function() {
    //Called in the event of a serious error (e.g. abort) where we couldn't get the error message from a response.
    //Poll the status endpoint to get the error message.
    var defaultMsg = 'An unexpected error occurred while uploading ' + this.filename;
    var _this = this;
    if(this.serverError === SERVER_ERROR_RECEIVE) {
      $.ajax({
        url: _this.generateUploadInfoURL('status'),
        dataType: 'json'
      })
          .done(function(data) {
            //If we got an error message from the status poll, show it to the user
            _this.errorMessage = data.errorMessage || defaultMsg;
          })
          .fail(function(){
            //Catch-all if the final error status poll failed
            _this.errorMessage = defaultMsg;
          })
          .always(function() {
            _this.handleError();
          });
    }
    else {
      var message;
      if(this.serverError === SERVER_ERROR_START) {
        message = 'An unexpected error occurred while starting your file upload. Please reload the page.';
      }
      else {
        message = defaultMsg;
      }

      _this.errorMessage = message;
      _this.handleError();
    }
  },

  handleSuccess: function() {
    var _this = this;
    //Tween progress bar to 100% then display the download URL etc
    this.setPercentComplete(100, function() {
      _this.clearContainer();
      _this.displayDownloadUrl();
      _this.container.removeClass('currentUpload');
    });
  },

  addDeleteButton: function(parentElement) {
    //Only show the delete button if we have a valid DOM ref to delete
    if(this.uploadDomRef !== null && !this.owner.widgetOptions.readOnly) {
      var deleteSpan = $('<span class="deleteUpload"><a href="#" class="icon-cross" title="Delete" aria-label="Delete ' + this.filename +'"></a></span>');
      deleteSpan.prependTo(parentElement);
      var _this = this;
      deleteSpan.click(function () {
        _this.deleteFile();
        return false;
      });
    }
    else if (this.owner.widgetOptions.readOnly) {
      this.container.addClass("readonly");
    }
  },

  displayDownloadUrl: function() {

    var downloadSpan = $('<span class="downloadUrl"><a class="downloadLink"></a><span class="filesize">' + this.fileSize +  '</span></span>');
    downloadSpan.appendTo(this.container);

    downloadSpan.children('.downloadLink').attr('href', this.downloadUrl + '?' + this.owner.widgetOptions.downloadModeParam);
    downloadSpan.children('.downloadLink').text(this.filename);

    this.addDeleteButton(this.container.find('.downloadUrl'));
  },

  displayErrorMessage: function() {
    this.addDeleteButton(this.container);
    var errorSpan = $('<span class="uploadError errorMessage\">' + this.errorMessage +  '<span class="errorStack" style="display:none;">' + this.errorStack + '</span></span>');
    errorSpan.appendTo(this.container);
  },

  displayPendingInfo: function() {
    this.container.append('<div class="filename">' + this.filename + ' - pending</span>');
  },

  createProgressBar: function() {
    this.container.append(
        '<div class="uploadProgress">' +
        '<div class="filename">' + this.filename +
        '<span class="cancelUpload deleteUpload"><a href="#" class="icon-cross" title="Cancel" aria-label="Cancel upload of ' + this.filename +'"></a></span>' +
        '</div>' +
        '<div class="statusContainer"><span class="status">&nbsp;</span></div>' +
        '<div class="uploadSpeedContainer">Speed: <span class="uploadSpeed">&nbsp;</span></div>' +
        '<div class="timeRemainingContainer">Time Remaining: <span class="timeRemaining">&nbsp;</span></div>' +
        '<div class="progressBar"><div class="progressBarPct">0%</div></div>' +
        '</div>'
    );

    if(this.owner.widgetOptions.widgetMode === "modal" && $('.modalUploadBlocker').length === 0) {
      $('body').append('<div class="modalUploadBlocker"></div>');
    }

    var _this = this;

    //Attach event listener for replace button
    this.container.find('.uploadProgress .cancelUpload').click(function() {
      _this.cancelUpload();
    });
  },

  cancelUpload: function() {
    var _this = this;
    $.ajax({
      url: _this.generateUploadInfoURL('cancel', {reason: 'requested'}),
      dataType: 'json'
    });
  },

  deleteFile: function() {
    //Check that the delete is OK - skip the confirm dialog for files with errors
    var deleteAllowed = this.owner.deleteFile(this, this.errorMessage !== null);
    if(deleteAllowed) {
      //Delete the LI from the DOM
      this.container.remove();
    }
  }
};

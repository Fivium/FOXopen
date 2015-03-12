var DELETE_UPLOAD_CLIENT_ACTION_TYPE  = 'DeleteUploadFile';
var POLL_STATUS_INTERVAL = 200;

function FileUpload (container, urlBase, urlParams, fileList, widgetOptions) {
  this.container = container;
  this.urlBase = urlBase;
  this.urlParams = urlParams;
  this.fileList = new Array();
  this.widgetOptions = widgetOptions;
  this.fileListContainer = container.children("ul");

  var _this = this;

  //Initialise the fileupload plugin on the file input
  var fileUpload = container.children('.fileUploadInput').fileupload({
    url: _this.generateURL('start', null),
    dataType: 'json',
    dropZone: $('div[data-dropzone-id="' + _this.container.attr('id') + '"]'),
    sequentialUploads: true,
    formData: function() { return [{name: 'clientActions', value: JSON.stringify(FOXjs.dequeueClientActions(DELETE_UPLOAD_CLIENT_ACTION_TYPE))}] }
  });

  if(widgetOptions.readOnly) {
    //Remove the dropzone for read only widgets
    $('div[data-dropzone-id="' + _this.container.attr('id') + '"]').remove();
  }

  //Overload start/finish actions for modal + modeless widgets
  if(widgetOptions.widgetMode == "modal") {
    this.startUploadGUIActions = function(){
      FOXjs._setPageDisabled(true);
    };

    //TODO PN proper JS OO
    this.finishUploadGUIActions = function(wasSuccess){
      FOXjs._setPageDisabled(false);
      $('.modalUploadBlocker').remove();

      if(wasSuccess && this.widgetOptions.successAction != null) {
        FOXjs.action(this.widgetOptions.successAction);
      }
      else if(!wasSuccess && this.widgetOptions.failAction != null) {
        FOXjs.action(this.widgetOptions.failAction);
      }
    };
  }

  //Convert existing files into file objects
  if(fileList != null) {
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

  //Attach event listener to redirect "choose file" click to file input
  container.children('.chooseFile').click(function() {
    container.children('.fileUploadInput').click();
  });

  //Attach event listeners to file upload

  fileUpload.bind('fileuploadadd', function (e, data) {
    //Called once for each file added by default. If singleFileUploads is set to false in the plugin options
    //this will fire once for each selection
    if (_this.checkMaxFilesOnAdd(e, data) == true) {
        $.each(data.files, function (index, file) {
          var fileInfo = _this.addFileInfo({filename: file.name});
          //Store a reference to our FileInfo in the library's object for retrieval later
          file._foxFileInfo = fileInfo;
        });
    }
  });

  fileUpload.bind('fileuploadstart', function (e) {
    //Called before any files are uploaded
    _this.startUploads(e);
  });


  fileUpload.bind('fileuploadsend', function (e, data) {
    //Called when an individual upload starts
    data.files[0]._foxFileInfo.uploadStarted(e);
  });

  fileUpload.bind('fileuploaddone', function (e, data) {
    //Individual upload completes
    data.files[0]._foxFileInfo.uploadFinished(e, data);

  });

  fileUpload.bind('fileuploadstop', function (e, data) {
    //Called when all uploads complete
    _this.finishUploads();
  });

  fileUpload.bind('fileuploadfail', function (e, data) {
    //Called in the event of a serious error (i.e. 500 response)
    _this.uploadHadError();
    _this.finishUploads();
  });
}

// Prototype definition
FileUpload.prototype = {
  container: null,
  urlBase: null,
  urlParams: null,
  fileList: null, //Array of file objects
  fileListContainer: null, //DOM UL containing the file list display
  widgetOptions: null,
  lastUploadHadError: false, //Tracker for if any file caused an error during the last upload

  generateURL: function(action, extraParams) {
    return this.urlBase + "/" + action  + "?" + this.urlParams + (extraParams != null ? extraParams : '');
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
    var lFileInfo = $.extend(new FileInfo(this), fileProps);
    this.fileList.push($.extend(lFileInfo, fileProps));

    lFileInfo.displayInPage();

    return lFileInfo;
  },

  startUploads: function(e) {
    this.startUploadGUIActions();
    this.disableUpload();
  },

  getUploadInput: function() {
    return this.container.children('.fileUploadInput');
  },

  enableUpload: function() {
    if(this.fileList.length < this.widgetOptions.maxFiles) {
      this.getUploadInput().toggle(true);
      this.getUploadInput().fileupload('enable');
      this.container.find('.dropzone').removeClass('disableUpload');
      $('body>.dropzone').removeClass('disableUpload');
    }
  },

  disableUpload: function() {
    this.getUploadInput().fileupload('disable');
    this.getUploadInput().toggle(false);
    this.container.find('.dropzone').addClass('disableUpload');
    $('body>.dropzone').addClass('disableUpload');
  },

  //Common behaviour for success or failure
  finishUploads: function() {
    this.finishUploadGUIActions(!this.lastUploadHadError);
    this.enableUpload();
  },

  uploadHadError: function() {
    this.lastUploadHadError = true;
  },

  deleteFile: function(fileInfo, skipConfirm) {

    var confirmDelete = false;
    if(this.widgetOptions.deleteConfirmText != null && !skipConfirm) {
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
        if(this.fileList[i] == fileInfo) {
          lIndex = i;
          break;
        }
      }

      //Remove the file info from the array
      if(lIndex != -1) {
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
    if(this.widgetOptions.maxFiles == 1) {
      this.container.find('.dropzone .dropzone-text').text('Drop file here');
      return;
    }

    if(this.widgetOptions.maxFiles > 1000) {
      this.container.find('.dropzone .dropzone-text').text('Drop files here');
      return;
    }

    var maxFilesRemaining = this.widgetOptions.maxFiles - this.fileList.length;
    var maxFilesText;
    if(maxFilesRemaining == 1) {
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
    var filesAllowed = _this.widgetOptions.maxFiles - $.grep(_this.fileList,function(file, index) { return file.fileId != undefined; }).length;

    if(data.originalFiles.length > filesAllowed) {
      this.container.children('.fileUploadInput').unbind('fileuploadsubmit');

      _this.container.children('.fileUploadInput').bind('fileuploadsubmit', function(e, data) {
        //This will fire once for each file. We only want to alert once, so do it for the first file
        if(data.files[0] == data.originalFiles[0]) {
          alert('You tried to upload ' + data.originalFiles.length + ' files, but there\'s only room for ' + filesAllowed + '. Please try again with fewer files.');
        }

        //Returning false blocks the upload in the plugin
        return false;
      });

      return false;
    }
    else {
      this.container.children('.fileUploadInput').unbind('fileuploadsubmit');

      return true;
    }
  }
};


//Drag and drop handling - TODO PN need to hide if widget disabled
$(document).ready(function() {

  //Don't bind drag and drop events if browser doesn't support HTML5 file API
  if(window.FileReader == undefined) {
    return;
  }

  $(document).on('dragenter', function (e) {
    $('.dropzone').not('.disableUpload').show();
    $('.fileUploadInput').css('visibility','hidden');
  });

  $(document).on('dragleave', function (e) {
    //Avoid IE/Chrome flickering bugs by checking if the drag was outside the window bounds
    if (e.originalEvent.pageX <= 0 || e.originalEvent.pageY <= 0 || e.originalEvent.pageX >= window.innerWidth || e.originalEvent.pageY >= window.innerHeight) {
      $('.dropzone').hide();
      $('.fileUploadInput').css('visibility','visible');
    }
    else {
      return false;
    }
  });

  $(document).on('drop', function (e) {
    $('.dropzone').hide();
    $('.fileUploadInput').css('visibility','visible');
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
  fileSize: null,
  container: null, //li element containing this file's details
  downloadUrl: null,
  uploadDomRef: null, //foxid of the element where this upload was delivered

  percentComplete: 0,
  statusInterval: null,
  allowStatusUpdates: true,

  displayInPage: function() {

    this.container = $('<li class="fileInfo"></li>');
    this.container.appendTo(this.owner.fileListContainer);

    //Error messages take precedence
    if(this.errorMessage != null) {
      this.displayErrorMessage();
    }
    else if(this.fileId != null) {
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
      url: _this.owner.generateURL('status', null),
      dataType: 'json',
      success: function(data, textStatus, jqXHR) {
        _this.updateStatus(data);
      },
      error : function( jqXHR, textStatus, errorThrown) {
        //alert('Error getting upload status ' + errorThrown);
      }
    });
  },

  updateStatus: function(statusObject) {
    if(this.allowStatusUpdates) {
      this.setStatusString(statusObject.statusText);
      this.setUploadSpeedString(statusObject.uploadSpeed);
      this.setTimeRemainingString(statusObject.timeRemaining);
      this.setPercentComplete(statusObject.percentComplete, function(){});
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

  uploadFinished: function(e, data) {

    //Merge the result JSON into this object
    $.extend(this, data.result);

    //Stop receiving status updates
    this.allowStatusUpdates = false;

    //Stop polling for status
    clearInterval(this.statusInterval);

    if(data.result.errorMessage != null) {
      this.handleError();
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

  handleSuccess: function() {
    var _this = this;
    //Tween progress bar to 100% then display the download URL etc
    this.setPercentComplete(100, function() {
      //_this.uploadFinished(true);
      _this.clearContainer();
      _this.displayDownloadUrl();
      _this.container.removeClass('currentUpload');
    });
  },

  addDeleteButton: function() {
    //Only show the delete button if we have a valid DOM ref to delete
    if(this.uploadDomRef != null && !this.owner.widgetOptions.readOnly) {
      var deleteSpan = $('<span class="deleteUpload"><a href="#">x</a></span>');
      deleteSpan.appendTo(this.container);
      var _this = this;
      deleteSpan.click(function () {
        _this.deleteFile();
        return false;
      });
    }
  },

  displayDownloadUrl: function() {

    this.addDeleteButton();

    var downloadSpan = $('<span class="downloadUrl"><a class="downloadLink"></a><span class="filesize">' + this.fileSize +  '</span></span>');
    downloadSpan.appendTo(this.container);

    downloadSpan.children('.downloadLink').attr('href', this.downloadUrl + '?' + this.owner.widgetOptions.downloadModeParam);
    downloadSpan.children('.downloadLink').text(this.filename);
  },

  displayErrorMessage: function() {
    this.addDeleteButton();
    var errorSpan = $('<span class="uploadError errorMessage\">' + this.errorMessage +  '<span class="errorStack" style="display:none;">' + this.errorStack + '</span></span>');
    errorSpan.appendTo(this.container);
  },

  displayPendingInfo: function() {
    this.container.append('<div class="filename">' + this.filename + ' - pending</span>');
  },

  createProgressBar: function() {
    this.container.append(
      '<div class="uploadProgress">' +
        '<span class="cancelUpload deleteUpload"><a href="#">x</a></span>' +
        '<div class="filename">' + this.filename + '</div>' +
        '<div class="statusContainer"><span class="status">&nbsp;</span></div>' +
        '<div class="uploadSpeedContainer">Speed: <span class="uploadSpeed">&nbsp;</span></div>' +
        '<div class="timeRemainingContainer">Time Remaining: <span class="timeRemaining">&nbsp;</span></div>' +
        '<div class="progressBar"><div class="progressBarPct">0%</div></div>' +
      '</div>'
    );

    if(this.owner.widgetOptions.widgetMode == "modal" && $('.modalUploadBlocker').length == 0) {
      $('body').append('<div class="modalUploadBlocker"></div>');
    }

    var _this = this;

    //Attach event listener for replace button
    this.container.find('.uploadProgress > .cancelUpload').click(function() {
      _this.cancelUpload();
    });
  },

  cancelUpload: function() {
    var _this = this;
    $.ajax({
      url: _this.owner.generateURL('cancel', '&reason=requested'),
      error: function(jqXHR, textStatus, errorThrown) {
        alert('Error cancelling upload ' + errorThrown);
        //_this.handleFailure("The upload was cancelled.");
      }
    });
  },

  deleteFile: function() {
    //Check that the delete is OK - skip the confirm dialog for files with errors
    var deleteAllowed = this.owner.deleteFile(this, this.errorMessage != null);
    if(deleteAllowed) {
      //Delete the LI from the DOM
      this.container.remove();
    }
  }
};

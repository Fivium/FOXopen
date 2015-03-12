/* Helper function to generate a div to act as a button */
function downloadMakeButton (pIDName, pClass, pEvent, pTextContent) {
  var lButton = document.createElement("div");
  lButton.setAttribute('id', pIDName);
  lButton.setAttribute("class", pClass);
  lButton.className = (pClass == null ? 'downloadButton' : pClass);
  lButton.onclick = pEvent;
  if (pTextContent != null) {
    lButton.appendChild(document.createTextNode(pTextContent));
  }
  return lButton;
}
function downloadSetCookie(NameOfCookie, value, expiredays) { var ExpireDate = new Date (); ExpireDate.setTime(ExpireDate.getTime() + (expiredays * 24 * 3600 * 1000)); document.cookie = NameOfCookie + '=' + escape(value) + ((expiredays == null) ? '' : '; expires=' + ExpireDate.toGMTString()) + '; path=/'; }
function downloadDelCookie(NameOfCookie) { if (FOXjs.getCookie(NameOfCookie)) { document.cookie = NameOfCookie + '=' + '; expires=Thu, 01-Jan-70 00:00:01 GMT; path=/'; } }


function downloadDoDownloads (pStreamParcelInfoArray) {
  // If cookie is already there show the user the links dialog and don't bother attempting popups
  if (FOXjs.getCookie("downloadShowHelper") != null) {
    downloadMakeHelperDiv(pStreamParcelInfoArray);
    return;
  }
  else {
    // Otherwise, attempt to make popups and show the helper config too
    for (i=0; i < pStreamParcelInfoArray.length; i++) {
    
      var lWinOptions = {
        url: pStreamParcelInfoArray[i][2].replace(/&amp;/g,"&"),
        windowName: pStreamParcelInfoArray[i][2].replace(/[^a-zA-Z0-9]/g, ""),
        windowOptions: "default",
        windowProperties: pStreamParcelInfoArray[i][4]
      };

      FOXjs.openwin(lWinOptions);
    }

    downloadMakeHelperConfigDiv(pStreamParcelInfoArray);
  }
}

/* Create a div to show when a download was attempted, click it to set the cookie to see the links div */
function downloadMakeHelperConfigDiv (pStreamParcelInfoArray) {

  // Find where to put the div
  var lParentDiv = document.getElementById("downloadHelperConfigAnchor");
  if (lParentDiv == null) {
    lParentDiv = document.body;
  }

  // Create the div for the helper config notification
  var lConfigDiv = document.createElement("div");
  lConfigDiv.setAttribute("id", "downloadHelperConfig");

  // Add a close button
  lConfigDiv.appendChild(
    downloadMakeButton(
      "downloadHelperConfigClose"
    , "downloadHelperClose"
    , function (e) {
        lParentDiv.removeChild(document.getElementById("downloadHelperConfig"));
        nd();
      }
    , null
    )
  );

  // Add the icon
  var lDownloadIcon = document.createElement("div");
  lDownloadIcon.setAttribute("id", "downloadHelperIcon");
  lConfigDiv.appendChild(lDownloadIcon);


  // Add tooltip text or content text depending on where it's embedded
  /* if (lParentDiv.localName == 'body') {
    lConfigDiv.setAttribute("onmouseover", "overlib('If your downloads were stopped by a popup blocker click this to get direct links to all downloadable files', CAPTION, 'Download Helper', HAUTO, VAUTO);");
    lConfigDiv.setAttribute("onmouseout", "nd(); return true;");
  }
  else { */
    var lConfigTextContainer = document.createElement("div");
    lConfigTextContainer.setAttribute("id", "downloadHelperText");

    // Add the click functionality to the text container
    lConfigTextContainer.onclick = function() {
      // Set the cookie to always show the helper div for ~6 months
      downloadSetCookie("downloadShowHelper", "TRUE", 6*30); // defined in foxjs.js
      // Display the helper div
      downloadMakeHelperDiv(pStreamParcelInfoArray);
      // Hide this div
      lParentDiv.removeChild(document.getElementById("downloadHelperConfig"));
      nd();
    };

    var lConfigTitle = document.createElement("h2");
    lConfigTitle.appendChild(document.createTextNode("We just tried to send you a file"));
    lConfigTextContainer.appendChild(lConfigTitle);

    lConfigTextContainer.appendChild(document.createTextNode("Click here if you didn't receive anything"));

    lConfigDiv.appendChild(lConfigTextContainer);
  /* } */

  // Put config div into an appropriate place
  lParentDiv.appendChild(lConfigDiv);
}


/* Create a div to show all links for files that may otherwise have been blocked by a download blocker */
function downloadMakeHelperDiv (pStreamParcelInfoArray) {

  // Find where to put the div
  var lParentDiv = document.getElementById("downloadHelperAnchor");
  if (lParentDiv == null) {
    lParentDiv = document.body;
  }

  // Create the div and set it's attributes
  var lHelperDiv = document.createElement("div");
  lHelperDiv.setAttribute("id", "downloadHelper");

  // Add a close button
  lHelperDiv.appendChild(
    downloadMakeButton(
      "downloadHelperClose"
    , "downloadHelperClose"
    , function () {
        lParentDiv.removeChild(document.getElementById("downloadHelper"));
      }
    , null
    )
  );

  // Add the icon
  var lDownloadIcon = document.createElement("div");
  lDownloadIcon.setAttribute("id", "downloadHelperIcon");
  lHelperDiv.appendChild(lDownloadIcon);
  
  // Add text container
  var lHelperTextContainer = document.createElement("div");
  lHelperTextContainer.setAttribute("id", "downloadHelperText");

  // Add a title
  var lHelperTitle = document.createElement("h2");
  lHelperTitle.appendChild(document.createTextNode("Download Helper"));
  lHelperTextContainer.appendChild(lHelperTitle);

  // Add some text above the links
  var lHelperTopText = document.createElement("p");
  lHelperTopText.setAttribute("class", "downloadHelperTopText");
  lHelperTopText.appendChild(document.createTextNode("We tried to send you the following file(s). Click the links below to download them."));
  lHelperTextContainer.appendChild(lHelperTopText);

  // Add the links
  var lLinkList = document.createElement("ul");
  lLinkList.setAttribute("class", "downloadHelperLinks");
  for (i=0; i < pStreamParcelInfoArray.length; i++) {
    var lLink = document.createElement("a");
    
    // Build up and set the link url
    lLink.setAttribute('href', pStreamParcelInfoArray[i][2].replace(/&amp;/g,"&").replace(/&mode=inline/g, ""));
    
    //If there is only one link, hide the links div after clicking to download
    /*if (pStreamParcelInfoArray.length == 1) {
      lLink.onclick = function () {
        lParentDiv.removeChild(document.getElementById("downloadHelper"));
      };
    }*/

    // If it's a window type, force open in a new window
    if (pStreamParcelInfoArray[i][1] == "WINDOW") {
      lLink.setAttribute('target', '_blank');
    }

    // Add the link to the list
    lLink.appendChild(document.createTextNode(pStreamParcelInfoArray[i][3]));
    var lLinkItem = document.createElement("li");
    lLinkItem.appendChild(lLink);
    lLinkList.appendChild(lLinkItem);
  }
  lHelperTextContainer.appendChild(lLinkList);

  // Add some text below the links
  var lHelperBottomText = document.createElement("p");
  lHelperBottomText.setAttribute("class", "downloadHelperBottomText");
  lHelperBottomText.appendChild(document.createTextNode("The Download Helper attempts to overcome issues with pop-up blockers stopping file downloads."));

  // Add a button to remove the cookie
  lHelperBottomText.appendChild(
    downloadMakeButton(
      "downloadHelperRemoveCookie"
    , ""
    , function (e) {
        downloadDelCookie("downloadShowHelper"); // defined in foxjs.js
        //Close the helper window too
        lParentDiv.removeChild(document.getElementById("downloadHelper"));
      }
    , "I don't need the Download Helper"
    )
  );

  lHelperTextContainer.appendChild(lHelperBottomText);
  
  // Add the text container to the helper div
  lHelperDiv.appendChild(lHelperTextContainer);
  
  // Add a div for styling
  var lClearingDiv = document.createElement("div");
  lClearingDiv.setAttribute("class", "clearBoth");
  lHelperDiv.appendChild(lClearingDiv);

  // Put helper div into an appropriate place
  lParentDiv.appendChild(lHelperDiv);
}
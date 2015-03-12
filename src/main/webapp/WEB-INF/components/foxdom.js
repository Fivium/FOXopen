/**
 * Static
 */
var FoxDOM = {

  /**
   * Creates a new DOM.
   * @param aRootNodeName the root element name to use
   * @return newly created DOM
   */
  newDOM: function (aRootNodeName) {
    // Cross platform "new DOM()"
    var xmlDoc;
    // W3C
    if (document.implementation && document.implementation.createDocument) {
      xmlDoc = document.implementation.createDocument(
        ""            // namespace uri
      , aRootNodeName // qualified name
      , null          // doctype
      );
    }
    // IE
    else if (window.ActiveXObject) {
      xmlDoc = new ActiveXObject('Microsoft.XMLDOM');
      xmlDoc.appendChild(xmlDoc.createElement(aRootNodeName));
    }
    // Unknown
    else {
      throw "Unsupported browser - does not support XML DOM.";
    }
    
    return xmlDoc;
  }
  
  /**
   * Parses a DOM from a string.
   * @param aDOMString the string to parse
   * @return parsed DOM object representation of string
   */
, parseDOMFromString: function (aDOMString) {
    // Fix up element delimiters
    var lString = aDOMString.replace(/&lt;/g,"<").replace(/&gt;/g,">");
    
    // W3C
    if (document.implementation && document.implementation.createDocument) {
      var lParser = new DOMParser();
      return lParser.parseFromString(lString, "text/xml");
    }
    // IE
    else if (window.ActiveXObject) {
      var xmlDoc = new ActiveXObject('Microsoft.XMLDOM');
      xmlDoc.async = "false";
      xmlDoc.loadXML(lString);
      return xmlDoc;
    }
    // Unknown
    else {
      throw "Unsupported browser - does not support XML DOM.";
    }
  }
  
  /**
   * Serializes a DOM to a string.
   * @param aDOM the DOM to serialize to a string
   * @return string value of DOM
   */
, serializeDOMToString: function (aDOM) {
    // Get parent ref
    var lDocument = aDOM.ownerDocument ? aDOM.ownerDocument : aDOM;
    // W3C
    if (typeof XMLSerializer != "undefined") {
      var lXMLSerializer = new XMLSerializer();
      return lXMLSerializer.serializeToString(lDocument);
    }
    // IE
    else {
      return lDocument.xml;
    }
  }
  
  /**
   * Returns an array of nodes from an XPath expression evaluated
   * against a context DOM reference. Note: does not return a Node List! 
   * @param aDOM context from which to resolve XPath
   * @param aXPathString the XPath to resolve to a list of nodes
   * @return array of DOM references
   */
, getNodesByXPath: function (aDOM, aXPathString) {
    // Sanity check
    if (aDOM == null) {
      throw "aDOM must not be null in FoxDOM.getNodesByXPath";
    }
    
    var lDocument;
    
    if (window.ActiveXObject) {
      // IE
      var lIEVersion = navigator.appVersion.indexOf('MSIE') > 0 ? parseFloat(navigator.appVersion.split('MSIE')[1]) : 999;
      
      if(lIEVersion <= 9){
        //IE versions 5-9 have their own XPath implementation - use that (wgxpath doesn't work in IE9)
        lDocument = aDOM.ownerDocument != null ? aDOM.ownerDocument : aDOM;
        lDocument.setProperty("SelectionLanguage","XPath");
        return aDOM.selectNodes(aXPathString);
      }
      else {
        // Newer versions of IE do not include XPath support, load wgxpath to supply this
        // wgxpath can then be used as if it was the standard W3 XPath provider
        if(!document.evaluate){
          //Only install if not already installed
          wgxpath.install();
        }
        lDocument = document;
      }
    }
    else if (document.evaluate && typeof XPathResult !== 'undefined') {
      // W3C
      // Make sure parent document is referenced for document.evaluate
      lDocument = aDOM.ownerDocument != null ? aDOM.ownerDocument : aDOM;      
    }    
    else {
      throw "Unsupported browser - does not support XML DOM";
    }
    
    // Build up an array
    var lNodes = new Array();
    var lXpResults = lDocument.evaluate(aXPathString, aDOM, null, XPathResult.ANY_TYPE, null);
    var lCurrNode = null;
    while (lCurrNode = lXpResults.iterateNext()) {
      lNodes.push(lCurrNode);
    }
    return lNodes;    
    
  }
  
  /**
   * Retrieves a single string by XPath.
   * @param aDOM context from which to resolve XPath
   * @param aXPathString the XPath to resolve to a string
   * @return string based on XPath
   */
, get1SByXPath: function (aDOM, aXPathString) {
    var lResultList = FoxDOM.getNodesByXPath(aDOM, aXPathString);
    if (lResultList.length != 1) {
      throw "Cardinality error in FoxDOM.get1SByXPath: " + aXPathString;
    }
    else {
      return lResultList[0].nodeValue;
    }
  }
  
  /**
   * Retrieves a single node by XPath, creating if necessary.
   * NB: Parent element must exist.
   * @param aDOM context from which to resolve XPath
   * @param aXPathString the XPath to get or create
   * @return element specified by XPath
   */
, getCreate1E: function (aDOM, aXPathString) {
    var lResultList = FoxDOM.getNodesByXPath(aDOM, aXPathString);
    
    // Result not found, create
    if (lResultList.length != 1) {
      // Get parent XPath and child node name
      var lParentXPath = aXPathString.substring(0, aXPathString.lastIndexOf("/"));
      var lChildName = aXPathString.substring(aXPathString.lastIndexOf("/")+1, aXPathString.length);
      
      var lParentResultsList = FoxDOM.getNodesByXPath(aDOM, lParentXPath);
      if (lParentResultsList.length != 1) {
        throw "Could not create element, parent does not exist: ";
      }
      else {
        return FoxDOM.appendChildToDOM(lParentResultsList[0], lChildName, null);
      }
    }
    // Result found, return
    else {
      return lResultList[0];
    }
  }
  
  /**
   * Creates an element with name aElemName, containing a
   * text node with value aTextValue, then appends it to the
   * desired parent node.
   * @param aDOM the parent for the new element
   * @param aElemName the name of the element to create
   * @param aTextValue the text value of the element to create
   * @return reference to created element
   */
, appendChildToDOM: function (aDOM, aElemName, aTextValue) {
    // Resolve owning document reference
    var lDocument = aDOM.ownerDocument ? aDOM.ownerDocument : aDOM;
    
    // Create elem and append text
    var lElem = lDocument.createElement(aElemName);
    // If text, add
    if (aTextValue) {
      lElem.appendChild(lDocument.createTextNode(aTextValue));
    }
    return aDOM.appendChild(lElem);
  }
  
  /**
   * Imports a node from one document to another. Attempts
   * to use native browser implementation where possible, but
   * implements a primitive script-based variant if necessary.
   * @param aDocument the document to which the node should be attached
   * @param aNode the node to attach to the target document
   * @param aAllChildren clone forward the node's child elements (recurse)
   * @return node that has been imported 
   */
, importNode: function (aDocument, aNode, aAllChildren) {
    
    // Make sure we're on a document node
    var lDocument = aDocument.ownerDocument ? aDocument.ownerDocument : aDocument;
    
    // Can just use the native implementation in competent browsers
    if (lDocument.importNode) {
      return lDocument.importNode(aNode, aAllChildren);
    }
    
    switch (aNode.nodeType) {
      case gSynchroniser.ELEMENT_NODE: {
        var lNewNode = lDocument.createElement(aNode.nodeName);
        if (aNode.attributes && aNode.attributes.length > 0) {
          for (var i = 0, il = aNode.attributes.length; i < il; i++) {
            lNewNode.setAttribute(aNode.attributes[i].nodeName, aNode.getAttribute(aNode.attributes[i].nodeName));
          }
        }
        if (aAllChildren && aNode.childNodes && aNode.childNodes.length > 0) {
          for (var i = 0, il = aNode.childNodes.length; i < il; i++) {
            lNewNode.appendChild(FoxDOM.importNode(lDocument, aNode.childNodes[i], aAllChildren));
          }
        }
        return lNewNode;
      }
      case gSynchroniser.TEXT_NODE:
      case gSynchroniser.CDATA_SECTION_NODE:
      case gSynchroniser.COMMENT_NODE: {
        return lDocument.createTextNode(aNode.nodeValue);
      }
      default: {
        alert("importNode failed");
      }
    }
  }
}
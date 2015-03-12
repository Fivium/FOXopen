/**
 * Constructs an editable canvas instance around a standard HTML image. 
 * Provides and abstracts drawing functionality using the Raphael JavaScript library.
 * @param aElemId the id of the image tag to wrap
 * @return a FoxCanvas instance
 */
function FoxCanvas (aElemId) {
  
  this._anchorElem = document.getElementById(aElemId);
  var lAnchorWrapper = $(this._anchorElem);

  var div = document.createElement("div");

  $(div).css({
    "background-image": "url(#)" // Hack for IE to allow mouse events on transparent div
  , "position": "absolute"
  , "top": "0px"
  , "left": "0px"
  , "width": lAnchorWrapper.width() + "px"
  , "height": lAnchorWrapper.height() + "px"
  , "z-index": "10"
  });

  lAnchorWrapper.parent().append(div);
  
  this._pushAnchorElem(div);
  this._raphael = new Raphael(div);

  this._modeToFoxShapeTypeMap["none"] = null;
  this._modeToFoxShapeTypeMap["point"] = FoxShape.TYPE_NODE;
  this._modeToFoxShapeTypeMap["line"] = FoxShape.TYPE_LINE;
  this._modeToFoxShapeTypeMap["connected-line"] = FoxShape.TYPE_CONNECTED_LINE;
  this._modeToFoxShapeTypeMap["poly-line"] = FoxShape.TYPE_POLY_LINE;
  this._modeToFoxShapeTypeMap["rubber-band"] = FoxShape.TYPE_RUBBER_BAND;
}

/**
 * The FoxCanvas function and member variable specification.
 */
FoxCanvas.prototype = {
  _raphael: null
, _anchorElemArray: new Array()
, _anchorElem: null
, _mode: null
, _handlerFunction: null
, _firstNode: null
, _joinDotsLastNode: null
, _joinIndicator: null
, _joinIndicatorHandler: null
, _lineClosed: null
, _currentFoxShape: null 
, _finalisedFoxShapes: new Array()
, _modeToFoxShapeTypeMap: new Array()
, _digitisingPointMap: new Array()
, _digitisingCoordMap: new Array()
, _xequivToPixelCoordArrayMap: new Array()
, _yequivToPixelCoordArrayMap: new Array()
, _snapX: new Array()
, _snapYForX: new Array()
, _snapY: new Array()
, _snapXForY: new Array()
, _snappingEnabled: false
, _diagonalPrevention: false
  
  /**
   * Sets the tool more we're using, a test function, and a
   * callback to invoke when the test condition is satisfied.
   * @param aMode the mode to set (must be one of "none","point"|"line"|"connected-line"|"poly-line"|"rubber-band")
   * @param aShapeCompleteCallback callback to run if a shape has been completed
   */
, setMode: function (aMode, aShapeCompleteCallback) {
    if (aMode == "none") {
      if (this._currentFoxShape) {
        this._currentFoxShape.destroy();
      }
    }
    
    var lShapeType = this._modeToFoxShapeTypeMap[aMode];
    if (lShapeType) {
      this._newFoxShape(lShapeType);
    }
    
    this._clearInternals();
    this._mode = aMode;
    if (aMode == "none" || aMode == "select" || aMode == "rubber-band") {
      this._anchorElem.style.cursor = "default";
    }
    else {
      this._anchorElem.style.cursor = "crosshair";
    }
  }
  
  /**
   * Sets digitising points using pixel coordinates (with a description provided).
   * @param aCoordinateArray an array of {x:0, y:0, desc: "53 21 N 13 03 E", xequiv: "53.21.00.000N", yequiv: "13.03.00.000E"} 
   * ordinate pairs with description
   */
, setDigitisingPoints: function (aCoordinateArray) {
    for (var n in aCoordinateArray) {
      var x = aCoordinateArray[n].x;
      var y = aCoordinateArray[n].y;
      var lPoint = this._drawPoint(x, y, 1, {fill: "#000000", stroke: "#000000", opacity: 0.2});
      this._snapX.push(x);
      this._snapY.push(y);
      
      if (!this._snapYForX[x]) {
        this._snapYForX[x] = new Array();
      }
      
      if (!this._snapXForY[y]) {
        this._snapXForY[y] = new Array();
      }
      
      this._snapYForX[x].push(y);
      this._snapXForY[y].push(x);
      
      this._digitisingPointMap[x+","+y] = lPoint;
      this._digitisingCoordMap[x+","+y] = aCoordinateArray[n];
      
      var lXToYPixelArray = this._xequivToPixelCoordArrayMap[aCoordinateArray[n].xequiv];
      if (!lXToYPixelArray) {
        lXToYPixelArray = this._xequivToPixelCoordArrayMap[aCoordinateArray[n].xequiv] = new Array();
      }
      lXToYPixelArray.push({x: aCoordinateArray[n].x, y: aCoordinateArray[n].y});
      
      var lYToXPixelArray = this._yequivToPixelCoordArrayMap[aCoordinateArray[n].yequiv];
      if (!lYToXPixelArray) {
        lYToXPixelArray = this._yequivToPixelCoordArrayMap[aCoordinateArray[n].yequiv] = new Array();
      }
      lYToXPixelArray.push({x: aCoordinateArray[n].x, y: aCoordinateArray[n].y});
    }
  }
  
  /** 
   * Clear all digitising / snap points.
   */
, clearDigitisingPoints: function () {

    // Clear down the visual guide-points and drop the event handler
    for (var n in this._digitisingPointMap) {
      this._digitisingPointMap[n].remove();
    }

    // Dereference arrays, replace and let GC tidy up
    this._snapX = new Array();
    this._snapYForX = new Array();
    this._snapY =  new Array();
    this._snapXForY = new Array();
    this._digitisingPointMap = new Array();
    this._digitisingCoordMap = new Array();
    this._xequivToPixelCoordArrayMap = new Array();
    this._yequivToPixelCoordArrayMap = new Array();
    
    // Tidy up any existing snap point as a belt and braces
    this._snapPoint = null;
    this._snapPointCoord = null;    
    
    // Turn off snapping as we now have nothing to snap to
    this.setSnapping(false);
    
    // Clear down the tooltip (if applicable)
    this._setCoordTooltip();
  }
  
  /**
   * Clears down the current tooltip (if any).
   */
, clearTooltip: function () {
    this._setCoordTooltip();
  }
   
  /** 
   * Set snapping on or off. Requires digitising points to have been set.
   * @param aSnappingEnable boolean - snapping on or off
   */
, setSnapping: function (aSnappingEnable) {
    if (aSnappingEnable) {
      if (this._snapX.length > 0) {
        $(this._anchorElem).bind("mousemove", $.proxy(function(aEvent){
          window.clearTimeout(this._snapTimeout);
          var self = this;
          this._snapTimeout = setTimeout(function () {
            self._snapToPoint(aEvent);
          }, 5); // 5ms buffer to ease performance while mouse is moving
        }, this));
        $(this._anchorElem).bind("mouseout", $.proxy(this._snapToPoint, this));
      }
      else {
        throw "Cannot enable snapping without digitising points to snap to";
      }
    }
    else {
      $(this._anchorElem).unbind("mousemove", this._snapToPoint);
      $(this._anchorElem).unbind("mouseout", this._snapToPoint);
    }
    this._snappingEnabled = aSnappingEnable;
  }
  
  /**
   * Set diagonal prevention mode on or off. Snapping mode must be on.
   * @param aPreventDiagonals boolean - prevention on or off
   */
, preventDiagonals: function (aPreventDiagonals) {
    if (aPreventDiagonals && this._snappingEnabled) {
      this._diagonalPrevention = aPreventDiagonals;  
    }
    else if (aPreventDiagonals) {
      throw "Cannot set diagonal prevention on if snapping is not enabled.";
    }
  }
  
  /**
   * Clears down all shapes.
   */
, clear: function () {
    for (var n in this._finalisedFoxShapes) {
      this._finalisedFoxShapes[n].destroy();
    }
    if (this._currentFoxShape) {
      this._currentFoxShape.destroy();
      this._currentFoxShape = null;
    }
    this._finalisedFoxShapes = new Array();
  }
  
  /**
   * Get a copy of the array containing the final shapes.
   * Allows for the interrogation of coordinates entered.
   */
, getFinalisedShapes: function () {
    var lBasicShapeArray = new Array();
    for (var n in this._finalisedFoxShapes) {
      var lFoxShape = this._finalisedFoxShapes[n];
      lBasicShapeArray.push({
        type: lFoxShape.getType()
      , nodes: lFoxShape.getNodes()
      });
    }
    return lBasicShapeArray;
  }
  
  /**
   * Attempt to finalise the current shape if it hasn't been
   * finished by the user, otherwise, discard if incomplete.
   */
, finaliseOrDiscardCurrentShape: function () {
    var lFoxShape = this._currentFoxShape;
    if (lFoxShape) {
      if (lFoxShape.isComplete()) {
        lFoxShape.finalise();
        this._finalisedFoxShapes.push(lFoxShape);
      }
      else {
        lFoxShape.destroy();
      }
    }
  }
  
  /**
   * Sets the tooltip showing the provided description of 
   * the current snap-to coordinate.
   * @param aCoord the coord object to use, or null
   */
, _setCoordTooltip: function (aCoord) {
    // First time in, init HTML fragment just in time
    if (!this._tooltipDiv) {
      var lDiv = document.createElement("div");
      $(lDiv).css({
        "position": "absolute"
      , "color": "#000000"
      , "background-color": "#CCCCCC"
      , "border": "2px solid #FFFFFF"
      , "font-size": "smaller"
      , "z-index": 100
      });

      this._tooltipDiv = document.body.appendChild(lDiv);
    }
    
    // We're clearing the tooltip
    if (aCoord == null) {
      this._tooltipDiv.style.display = "none";
      $(this._tooltipDiv).text("");
      return;
    }
    
    // Always return to the digitising map as a belt and braces
    var lCoord = this._digitisingCoordMap[aCoord.x + "," + aCoord.y];
    if (lCoord == null) {
      throw "Coord not found in digitising coord map";
    }
    else {
      $(this._tooltipDiv).text(lCoord.desc);
      this._tooltipDiv.style.top = $(this._anchorElem).offset().top + parseInt(lCoord.y) - $(this._tooltipDiv).height()/2;
      this._tooltipDiv.style.left = $(this._anchorElem).offset().left + parseInt(lCoord.x) + 35;
      this._tooltipDiv.style.display = "block";
    }
  }
  
  /**
   * Sets up some global variables that point to the nearest
   * snap point, providing a coordinate object and reference
   * to the Raphael object that illustrates the coordinate.
   * @param aEvent the event to be handled by the mouse coord finder code
   */
, _snapToPoint: function (aEvent) {
    // If we have an existing point, reverse any highlighting
    if (this._snapPoint) {
      this._snapPoint.attr({fill: "#000000", stroke: "#000000", opacity: 0.2});
    }
    
    // The cursor has left the building, don't allow
    // erroneous capture of the last point 
    if (aEvent.type == "mouseout") {
      this._snapPoint = null;
      this._snapPointCoord = null; 
      this._setCoordTooltip(null);
      return;
    }
    if (this._mode != "select") {
      var lCoord = this._getMouseCoordsWithinCanvas(aEvent);
      var lPoint = this._digitisingPointMap[lCoord.x + "," + lCoord.y];
      
      if (lPoint) {
        lPoint.attr({fill: "#FF0000", stroke: "#FF0000", opacity: 1});
        this._setCoordTooltip(lCoord);
      }
      this._snapPoint = lPoint;
      this._snapPointCoord = lCoord;
    }
  }
  
  /**
   * Internal event handler. Handles the mouse coordinate
   * derivation and snapping implicitly before delegation.
   * @param aEvent the event object to delegate
   */
, _eventHandler: function (aEvent) {

    var lCoords = this._getMouseCoordsWithinCanvas(aEvent);
    var lMouseButton = aEvent.which;
    var lFoxShape;
    var lResult;
    var lType = this._modeToFoxShapeTypeMap[this._mode];
    var lHandler = this._mode + "|" + aEvent.type;
    
    if (lMouseButton == 3) { // Right click, block event
      aEvent.stopPropagation();
    }
    else if (lMouseButton == 1) { // Left click, delegate event
      switch (lHandler) {
        case "select|mousedown": {
          if (aEvent.target != this._anchorElem) {
            this._pushAnchorElem(aEvent.target);
            aEvent.target.raphael.attr({fill: "yellow"});
            this._currentFoxShape = aEvent.target.foxshape;
          }
          break;
        }
        case "point|mousedown": {
          lResult = this._pointEventHandler(aEvent, lCoords.x, lCoords.y); 
          break;
        }
        case "line|mousedown": {
          lResult = this._lineEventHandler(aEvent, lCoords.x, lCoords.y); 
          break;
        }
        case "connected-line|mousedown": {
          lResult = this._lineEventHandler(aEvent, lCoords.x, lCoords.y); 
          break;
        }
        case "poly-line|mousedown": {
          lResult = this._lineEventHandler(aEvent, lCoords.x, lCoords.y);
          break;
        }
        case "rubber-band|mouseup": 
        case "rubber-band|mousedown": {
          lResult = this._rubberBandEventHandler(aEvent, lCoords.x, lCoords.y);
          break;
        }
      }
    }
    
    lFoxShape = this._getFoxShape(lType);
    
    // If we've got a result, we need to add it into the working shape
    if (lResult != null) {
      lFoxShape.addElements(lResult.shapes, lResult.nodes);
    }
    
    // For everything except a connected-line, we can detect whether we're
    // finished and move on to the next shape if we receive a left-click
    if (lMouseButton == 1 && lFoxShape.getType() != FoxShape.TYPE_CONNECTED_LINE && lFoxShape.isComplete()) {
      this._newFoxShape(lType);
      this._clearInternals();
    }
    // If we receive a right click for a connected-line, take this as a signal 
    // to stop drawing and move on to the next shape
    else if (lMouseButton == 3 && lFoxShape.getType() == FoxShape.TYPE_CONNECTED_LINE && lFoxShape.isComplete()) {
      this._newFoxShape(lType);
      this._clearInternals();
    }
    // If we receive a right click for a poly-line, and it is not complete
    // then destroy it and move on
    else if (lMouseButton == 3 && !lFoxShape.isComplete()) {
      lFoxShape.destroy();
      this._newFoxShape(lType);
      this._clearInternals();
    }
  }
  
  /**
   * Simply draws a point.
   * @param aEvent the original event
   * @param aX the x coordinate after snapping
   * @param aY the y coordinate after snapping
   * @return anonymous object containing a shape array and node array
   */
, _pointEventHandler: function (aEvent, aX, aY) {
    var lPoint = this._drawPoint(aX, aY, 3, {fill: "#0000FF", stroke: "#0000FF"});
    return {shapes: [lPoint], nodes: [{x:aX, y:aY}]};
  }
  
  /**
   * Handles the drawing of a line (in join indicator mode, where a dotted
   * line is shown as a template), whether we're in line, connected-line, 
   * or poly-line mode.
   * @param aEvent the original event
   * @param aX the current x coordinate after snapping
   * @param aY the current y coordinate after snapping
   * @return anonymous object containing a shape array and node array
   */
, _lineEventHandler: function (aEvent, aX, aY) {
    
    var lPoint;
    var lNode;
    var lLine;
    var lClosingFlag = false;
    var lX;
    var lY;
    
    if (this._snapPoint) {
      lX = this._snapPointCoord.x;
      lY = this._snapPointCoord.y;
    }
    else {
      lX = aX;
      lY = aY;
    }

    // Get the x/y equivs to post back later
    var lDigitisingPoint = this._digitisingCoordMap[lX + "," + lY];
    var lXEquiv = lDigitisingPoint.xequiv;
    var lYEquiv = lDigitisingPoint.yequiv;
    
    // If we're closing a line, don't place a node and null out the flag
    if (this._lineClosed == true) {
      this._lineClosed = null;
      lNode = this._firstNode;
      lPoint = null;
      lClosingFlag = true;
    }
    else {
      lPoint = this._drawPoint(lX, lY, 3, {fill: "#0000FF", stroke: "#0000FF"});
      lNode = {x: lX, y: lY, xequiv: lXEquiv, yequiv: lYEquiv};
    }
    
    // Clear the join indicator if it exists
    this._clearJoinIndicator();
    
    // If we have a prior node, then we're in join the
    // dots mode, and should draw the line.
    if (this._joinDotsLastNode != null) {
      var lLastNode = this._joinDotsLastNode;
      lLine = this._drawLine(
        [lLastNode, lNode]
      , {stroke: "#FF0000"}
      );
      lLine.toBack();
    }
    // If we have no prior node, then we're starting
    // out fresh and need to attach a listener on the 
    // start node that we're introducing, and also record
    // the start node for line-close detection
    else {
      var lSelf = this;
      $(lPoint.node).click(function(aEvent){
        self._lineClosed = true;
      });
      this._firstNode = lNode;
    }
    
    // If we're in line mode and we had a prior node, then we're finished here
    // NB: also finish if we're closing a connected line or a poly line
    if ((this._mode == "line" && this._joinDotsLastNode != null) || lClosingFlag) {
      this._joinDotsLastNode = null;
      return {shapes: [lPoint, lLine], nodes: [lNode]};
    }
    else if (this._mode == "line" || this._mode == "connected-line" || this._mode == "poly-line") {
      this._joinDotsLastNode = lNode;
      this._joinIndicator = this._drawLine(
        [lNode, lNode]
      , {stroke: "#FF0000", "stroke-dasharray": ". "}
      );
      
      this._joinIndicatorHandler = function (aEvent) {
        var lCoord = this._getMouseCoordsWithinCanvas(aEvent);
        this._setLine(
          this._joinIndicator
        , [lNode, lCoord]
        , null
        );
      }
      $(this._anchorElem).bind("mousemove", $.proxy(this._joinIndicatorHandler, this));
    }
    else {
      throw "Unexpected mode in FoxCanvas._lineEventHandler: " + this._mode;
    }
    
    var lShapeArray = [lPoint];
    if (lLine != null) {
      lShapeArray.push(lLine);
    }
    return {shapes: lShapeArray, nodes: [lNode]};
  }
  
  /**
   * Simply draws a rubber band.
   * @param aEvent the original event
   * @param aX the x coordinate after snapping
   * @param aY the y coordinate after snapping
   * @return anonymous object containing a shape array and node array
   */
, _rubberBandEventHandler: function (aEvent, aX, aY) {
    // Need to branch based on the incoming event type
    if (aEvent.type == "mousedown") {
      // Clear the join indicator if it exists
      this._clearJoinIndicator();
      
      var lStartX = aX;
      var lStartY = aY;
      var lStartNode = {x: aX, y: aY};
      
      this._joinIndicator = this._drawLine(
        [lStartNode, lStartNode, lStartNode, lStartNode, lStartNode]
      , {stroke: "#FF0000", "stroke-dasharray": ". "}
      );
      
      this._joinIndicatorHandler = function (aEvent) {
        var lCoord = this._getMouseCoordsWithinCanvas(aEvent);
        this._setLine(
          this._joinIndicator
        , [
            lStartNode
          , {x: lCoord.x, y: lStartNode.y}
          , {x: lCoord.x, y: lCoord.y}
          , {x: lStartNode.x, y: lCoord.y}
          , lStartNode
          ]
        );
      }
      
      $(this._anchorElem).bind("mousemove", $.proxy(this._joinIndicatorHandler, this));
      return {shapes: [], nodes: [lStartNode]};
    }
    else if (aEvent.type == "mouseup") {
      $(this._anchorElem).unbind("mousemove", this._joinIndicatorHandler);
      return {shapes: [], nodes: [{x: aX, y: aY}]};
    }
    else {
      throw "Unexpected event: " + aEvent.type;
    }
  }
   
  /**
   * Draws an SVG poing using Raphael and returns
   * a reference to the object created.
   * @param aX the x coordinate to use when drawing
   * @param aY the y coordinate to use when drawing
   * @param aR the radius with which to draw the circle
   * @param aAttrs the attributes to assign to the shape
   * @return the Raphael object created
   */
, _drawPoint: function (aX, aY, aR, aAttrs) {
    var circle = this._raphael.circle(aX, aY, aR);
    circle.attr(aAttrs);
    return circle;
  }
  
  /** 
   * Draws an SVG line using Raphael and returns
   * a reference to the object created.
   * @param aCoordArray the coordinates to use for drawing the line
   * @param aAttrs the attributes to assign to the shape
   * @return the Raphael object created
   */
, _drawLine: function (aCoordArray, aAttrs) {
    var path = this._raphael.path(this._createLinePath(aCoordArray));
    path.attr(aAttrs);
    return path;
  }
  
  /** 
   * Sets an existing Raphael line object to have new coordinates.
   * @param aLine the existing line to target
   * @param aCoordArray the coordinates to set the line to
   * @param aAttrs the attributes to assign to the shape (optional)
   */
, _setLine: function (aLine, aCoordArray, aAttrs) {
    aLine.attr({path: this._createLinePath(aCoordArray)});
    if (aAttrs != null) {
      aLine.attr(aAttrs);
    }
  }

   /**
    * Creates an SVG path string from an array of coordinates.
    * @param aCoordArray the array of coordinates to convert to a string
    * @return the SVG path string containing the provided coordinates
    */
, _createLinePath: function (aCoordArray) {
    var lPathStr = "M";
    if (aCoordArray.length < 2) {
      throw "Not enough coordinates to construct a line string";
    }
    for (var i = 0; i < aCoordArray.length; i++) {
      lPathStr += aCoordArray[i].x + " " + aCoordArray[i].y;
      if (i < aCoordArray.length - 1) {
        lPathStr += "L";
      }
    }
    return lPathStr;
  }
  
  /**
   * Takes a mouse event and derives the coordinates within a canvas
   * by using image offsets and any snapping information available,
   * thus implicitly handles snapping to reference points.
   * @param aEvent the mouse event to handle
   * @return a coord object of type {x,y} 
   */
, _getMouseCoordsWithinCanvas: function (aEvent) {
    // Normalise the pixel coord within the image
    var lImageX = aEvent.clientX - ($(this._anchorElem).offset().left - $(window).scrollLeft());
    var lImageY = aEvent.clientY - ($(this._anchorElem).offset().top - $(window).scrollTop());
    var lContextCoord;

    // If we have a previous node, use it as the context (retrieve
    // from digitising map)
    if (this._joinDotsLastNode) {
      lContextCoord = this._digitisingCoordMap[this._joinDotsLastNode.x+","+this._joinDotsLastNode.y];
    }
    
    if (this._snappingEnabled && this._diagonalPrevention && lContextCoord) {
      var lDiffX = Math.abs(lImageX - lContextCoord.x);
      var lDiffY = Math.abs(lImageY - lContextCoord.y);
      
      var lCoordList;
      if (lDiffX <= lDiffY) {
        // Lock X Equivalent
        lCoordList = this._xequivToPixelCoordArrayMap[lContextCoord.xequiv];
      }
      else {
        // Lock Y Equivalent
        lCoordList = this._yequivToPixelCoordArrayMap[lContextCoord.yequiv];
      }
      
      var lMinDiff;
      var lBestX;
      var lBestY;
      
      for (var n in lCoordList) {
        var lDiff = Math.abs(lImageX - lCoordList[n].x) + Math.abs(lImageY - lCoordList[n].y);
        if (!lMinDiff || lDiff <= lMinDiff) {
          lMinDiff = lDiff;
          lBestX = lCoordList[n].x;
          lBestY = lCoordList[n].y;
        }
      }
      
      lImageX = lBestX;
      lImageY = lBestY;
    }
    else if (this._snappingEnabled) {
      var lMinDiff;
      var lBestX;
      var lBestY;
      
      for (var i in this._snapX) {
        var x = this._snapX[i];
        for (var j in this._snapYForX[x]) {
          var y = this._snapYForX[x][j];
          var lDiff = Math.abs(lImageX - x) + Math.abs(lImageY - y);
          if (!lMinDiff || lDiff <= lMinDiff) {
            lMinDiff = lDiff;
            lBestX = x;
            lBestY = y;
          }
        }
      }
      
      lImageX = lBestX;
      lImageY = lBestY;
    }
    return {x: lImageX, y: lImageY};
  }
  
  /**
   * Convenience function that clears down object references
   * that may have been in use.
   */
  , _clearInternals: function () {
    this._joinDotsLastNode = null;
    this._clearJoinIndicator();
    this._lineClosed = null;
    this._firstNode = null;
  }
  
  /**
   * Clears the current join indicator (dotted line) if any.
   */
, _clearJoinIndicator: function () {
    if (this._joinIndicator != null) {
      this._joinIndicator.remove();
      this._joinIndicator = null;
      $(this._anchorElem).unbind("mousemove", this._joinIndicatorHandler);
    }
  }
  
  /** 
   * Crude snapping algorithm to allow snapping to the nearest value
   * within an array of values.
   */
, _snapTo: function (aSnapValues, aValue) {
    for (var i = 0; i < aSnapValues.length; i++) {
      if (i == aSnapValues.length - 1) {
        return aSnapValues[i];
      }
      if (aValue >= aSnapValues[i] && aValue < aSnapValues[i+1]) {
        var lDiffLeft = Math.abs(aValue - aSnapValues[i]);
        var lDiffRight = Math.abs(aValue - aSnapValues[i+1]);
        if (lDiffLeft < lDiffRight) {
          return aSnapValues[i];
        }
        else {
          return aSnapValues[i+1];
        }
      }
    }
  }
  
  /**
   * Blocks events.
   * @param aEvent the event to block
   */
, _eventBlock: function (aEvent) {
    aEvent.preventDefault();
  }
  
  /** 
   * If a new object is targetted, pushes it onto a stack
   * and handles the event listener reassignment.
   * @aElem the element that is now taking focus
   */
, _pushAnchorElem: function (aElem) {
    
    var lElemArray = this._anchorElemArray;
    var lCurrentAnchorElem = lElemArray[lElemArray.length - 1];
    
    if (lCurrentAnchorElem != null) {
      $(lCurrentAnchorElem).unbind("mousedown", this._eventHandler);
      $(lCurrentAnchorElem).unbind("mouseup", this._eventHandler);
      $(lCurrentAnchorElem).unbind("contextmenu", this._eventBlock);
    }

    lElemArray.push(aElem);
    this._anchorElem = aElem;
    $(aElem).bind("mousedown", $.proxy(this._eventHandler, this));
    $(aElem).bind("mouseup", $.proxy(this._eventHandler, this));
    $(aElem).bind("contextmenu", $.proxy(this._eventBlock, this));
  }
  
  /**
   * Pops the element focus stack and handles the event listener
   * reassignment.
   */
, _popAnchorElem: function () {

    var lElemArray = this._anchorElemArray;
    lElemArray.pop();
    
    $(this._anchorElem).unbind("click", this._eventHandler);
    this._anchorElem = lElemArray[lElemArray.length - 1];    
    $(this._anchorElem).bind("click", $.proxy(this._eventHandler, this));
  } 
  
  /**
   * Internal get or create function for the current working
   * shape. Will initialise a FoxShape of the required type if
   * required, or validate that the current FoxShape is of the
   * type specified.
   * @param aType the FoxShape type to return
   * @return an instance of FoxShape
   */
, _getFoxShape: function (aType) {
    var lFoxShape = this._currentFoxShape;
    if (lFoxShape == null) {
      lFoxShape = this._currentFoxShape = new FoxShape(aType, this._raphael);
    }
    if (lFoxShape.getType() != aType) {
      throw "Unexpected FoxShape type " + lFoxShape.getType() + ", expected " + aType;
    }
    return lFoxShape;
  }
  
  /** 
   * Internal FoxShape creation function. Attempts to finalise
   * the current shape object if there is one and it is complete,
   * otherwise destroys it and moves on.
   * @param aType the type of FoxShape to create
   */
, _newFoxShape: function (aType) {
    var lFoxShape = this._currentFoxShape;
    if (lFoxShape != null) {
      if (lFoxShape.isComplete()) {
        lFoxShape.finalise();
        this._finalisedFoxShapes.push(lFoxShape);
      }
      else {
        lFoxShape.destroy();
      }
    }
    this._currentFoxShape = new FoxShape(aType, this._raphael);
  }
}
    
/** 
 * A simple wrapper object that allows for easy grouping and interrogation
 * of coordinates (nodes) that have been input, and a mechanism to test whether
 * shapes are complete or not. Also allows for a quick reference of all SVG elements
 * that build up the same shape (i.e. for removal).
 * @param aType the type of FoxShape to construct
 * @param aRaphael the current Raphael object
 */
function FoxShape (aType, aRaphael) {
  this._type = aType;
  this._raphael = aRaphael;
}

FoxShape.TYPE_NODE = "NODE";
FoxShape.TYPE_LINE = "LINE";
FoxShape.TYPE_CONNECTED_LINE = "CLINE";
FoxShape.TYPE_POLY_LINE = "PLINE";
FoxShape.TYPE_POLYGON = "POLYGON";
FoxShape.TYPE_RUBBER_BAND = "RUBBERBAND";

/**
 * The FoxShape function and member variable specification.
 */
FoxShape.prototype = {
 
  _internalShapeArray: new Array()
, _internalNodeArray: new Array()
, _eventArray: new Array ()
, _type: null
, _raphael: null

  /**
   * Adds a new component or components to this shape.
   * @param aShapeArray the SVG shape objects (wrapped by Raphael)that have been created.
   * @param aNodeArray an array of the {x,y} nodes that build this shape.
   */
, addElements: function (aShapeArray, aNodeArray) {
    for (var n in aShapeArray) {
      for (var e in this._eventArray) {
        $(aShapeArray[n].node).bind(this._eventArray[e].eventName, this._eventArray[e].eventFunction);
        aShapeArray[n].node.foxshape = this;
      }
    }
    this._internalShapeArray = this._internalShapeArray.concat(aShapeArray);
    this._internalNodeArray = this._internalNodeArray.concat(aNodeArray);
  }
  
  /**
   * Returns the type of this FoxShape instance.
   * @return string that specifies the FoxShape type. See defined constants.
   */
, getType: function () {
    return this._type;
  }
  
  /**
   * Adds an event listener to the SVG components that build this shape.
   * (i.e. for drag/drop or selection purposes).
   * @param aEventName the event to listen for
   * @param aFunction the function to run if the event is triggered
   */
, addEventListeners: function (aEventName, aFunction) {
    for (var n in this._internalShapeArray) {
      $(this._internalShapeArray[n].node).bind(aEventName, aFunction);
    }
    this._eventArray.push({eventName: aEventName, eventFunction: aFunction});
  }
  
  /**
   * Removes a specific event listener from the SVG components that build 
   * this shape.
   * @param aEventName the event being listened for
   * @param aFunction the function that would be run if the event were triggered
   */
, removeEventListeners: function (aEventName, aFunction) {
    for (var n in this._internalShapeArray) {
      $(this._internalShapeArray[n].node).unbind(aEventName, aFunction);
    }
    // Not very elegant, but should be few items in the array
    for (var n in this._eventArray) {
      if (this._eventArray[n].eventName === aEventName && this._eventArray[n].eventFunction === aFunction) {
        this._eventArray.splice(n, 1);
      }
    }
  }
  
  /**
   * Removes all event listeners from the SVG components that 
   * build this shape.
   */
, removeAllEventListeners: function () {
    for (var n in this._internalShapeArray) {
      $(this._internalShapeArray[n].node).unbind();
    }
    this._eventArray = new Array();
  }

  /** 
   * Checks to see if this shape closes (i.e. for a polygon boundary).
   * @return boolean indicator of whether this shape closes
   */
, closes: function () {
    if (this._internalNodeArray.length < 3) {
      return;
    }
    var lFirstNode = this._internalNodeArray[0];
    var lLastNode = this._internalNodeArray[this._internalNodeArray.length-1];
    return (lFirstNode.x == lLastNode.x && lFirstNode.y == lLastNode.y);
  }
  
  /**
   * Checks to see if this shape is complete (i.e does it close, does it have
   * enough points, etc).
   * @return boolean indicator of whether this shape is complete
   */
, isComplete: function () {
    if (this._isCompleteFlag) {
      return true;
    }
    switch (this._type) {
      case FoxShape.TYPE_NODE: {
        this._isCompleteFlag = this._internalNodeArray.length == 1;
        break;
      }
      case FoxShape.TYPE_LINE: {
        this._isCompleteFlag = this._internalNodeArray.length == 2;
        break;
      }
      case FoxShape.TYPE_CONNECTED_LINE: {
        this._isCompleteFlag = this._internalNodeArray.length >= 2;
        break;
      }
      case FoxShape.TYPE_POLY_LINE: {
        this._isCompleteFlag = this._internalNodeArray.length >= 2 && this.closes();
        break;
      }
      case FoxShape.TYPE_RUBBER_BAND: {
        return false;
      }
    }
    return this._isCompleteFlag;
  }
 
  /**
   * Manually specify that this shape is now complete.
   */
, setComplete: function () {
    this._isCompleteFlag = true;
  }
  
  /**
   * Finish any processing required by this shape, assuming
   * that it is finished or complete. For example, if we're
   * drawing a polygon and we have mostly lines, then fill it
   * in to show a complete shape.
   */
, finalise: function () {
    switch (this._type) {
      case FoxShape.TYPE_POLY_LINE: {
        var lPolygon = this._raphael.path("M " + this.getCoordsAsString() + " z");
        lPolygon.attr({fill:"#FFCC33"});
        lPolygon.insertBefore(this._internalShapeArray[0]);
        lPolygon.node.foxshape = this;
        this._type = FoxShape.TYPE_POLYGON;
        break;
      }
      default: {
        return;
      }
    }
  }
  
  /**
   * Remove the SVG components that build this shape,
   * clean down any event listeners and clean out the
   * internal arrays of this object to allow for GC.
   */
, destroy: function () {
    // Clear down listeners to avoid memory leaks
    this.removeAllEventListeners();
    for (var n in this._internalShapeArray) {
      this._internalShapeArray[n].remove();
    }
    // Null out references to the old arrays and let GC 
    // handle this stuff for us
    this._internalShapeArray = new Array();
    this._internalNodeArray = new Array();
  }

  /**
   * Return the coordinates of this shape as a string.
   * @param aReverseOrder boolean to reverse the order
   * @return a string that describes the coordinates of this object
   */
, getCoordsAsString: function (aReverseOrder) {
    var lString = "";
    if (aReverseOrder == null || !aReverseOrder) {
      for (var n in this._internalNodeArray) {
        lString += this._internalNodeArray[n].x + " " + this._internalNodeArray[n].y;
        if (n != this._internalNodeArray.length) {
          lString += " ";
        }
      }
    }
    else {
      for (var n = this._internalNodeArray.length - 1; n >= 0; n--) {
        lString += this._internalNodeArray[n].x + " " + this._internalNodeArray[n].y;
        if (n != 0) {
          lString += " ";
        }
      }
    }
    return lString;
  }
  
  /**
   * Return a copy of the current node object array {x,y,etc}
   */
, getNodes: function () {
    return this._internalNodeArray;
  }
}

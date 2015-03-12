//Static Variables

var maxFrameRate = 30; //Frames per second

// Class Constructor
function OpacityFader(pObject, pInitialOpacity) {
  this.mObject = pObject;
  this._setOpacity(pInitialOpacity);
}
// Class body
OpacityFader.prototype = 
{ 
  // Member variables
    mObject: null
  , mOpacity: null
  , _Timer: null
  , _hoverOpacity: null  
  , _hoverReturnOpacity: null
  , _hoverSpeed: null
  , _hoverEnabled: false
  , _timerList: new Array()
  , _timer: null
  
  // Public Methods    
  , setOpacity: function (pOpacity) { 
      this.cancelFade();
      this._setOpacity(pOpacity);
    }
  
  , startFade: function (pOpacity, pSpeed, pEndFunction) {	  
	  this.cancelFade();
      var opacityDifference =  pOpacity - this.mOpacity;
      var jump;
      if (opacityDifference > 0) {
        jump = 1;
      }
      else if (opacityDifference < 0) {
        jump = -1;
      }
      else { return; }
      
      var timePerStep = Math.abs(pSpeed/opacityDifference);
      var speedStep = 1000/maxFrameRate/timePerStep;
      jump = jump * speedStep;
      timePerStep = timePerStep * speedStep;
      var self = this;
      this._Timer = setInterval( function(event) {
         self._setOpacity(self.mOpacity + jump);
         if ((self.mOpacity >= pOpacity && jump > 0)||(self.mOpacity <= pOpacity && jump < 0)) {
           self._setOpacity(pOpacity);
		   self.cancelFade();
		   if (pEndFunction != undefined) {
		     pEndFunction();
		   }	
         }		 
      }
      , timePerStep);
    }
    
  , cancelFade: function () {
      clearInterval (this._Timer);
    }
  
  , assignHover: function (pHoverOpacity, pSpeed, pReturnOpacity) {   
 
    this._hoverOpacity = pHoverOpacity;
    this._hoverSpeed = pSpeed;
    this._hoverReturnOpacity = pReturnOpacity;
    this._hoverEnabled = true;

    var self = this;    
    assignEventListener(this.mObject, "mouseover", function(event) {self._hoverIn.call(self, event);});
	  assignEventListener(this.mObject, "mouseout", function(event) {self._hoverOut.call(self, event);});
  }
  
  , removeHover: function () {  
      this._hoverEnabled = false;
    }
    
  , addTimer: function (pName, pDelay, pFunction) {
      this._timerList[pName] = null;
      this._timerList[pName] = setTimeout(pFunction, pDelay);
  }
  , removeTimer: function (pName) {
      clearTimeout(this._timerList[pName]);
  }
  
  
  
  // Private Methods     
  , _setOpacity: function(pOpacity) {      
      changeObjOpac(pOpacity, this.mObject)
      
      if (pOpacity == 0) {
        if( this.mObject.style ) {
          this.mObject.style.display = 'none';
        } 
        else {
          this.mObject.display = 'none';
        }
      }
      else {
        if( this.mObject.style ) {
          this.mObject.style.display = 'block';
        } 
        else {
          this.mObject.display = 'block';
        }
      }
      
      this.mOpacity = pOpacity;
    }
    
    , _hoverIn: function(e) {       		
      if (!this._hoverEnabled) return;
      if (!e) var e = window.event;
      var toElement = (window.event) ? e.srcElement : e.target;
      var fromElement = (e.relatedTarget) ? e.relatedTarget : e.fromElement;
      var baseElement = this.mObject; 	
      //Ignore if it is from a child of the baseElement
      while (fromElement != null && fromElement != baseElement && fromElement.nodeName != 'BODY') fromElement = fromElement.parentNode	
      if (fromElement == baseElement) return;
      this.startFade(this._hoverOpacity, this._hoverSpeed);
    }   
    
    , _hoverOut: function(e) {
      if (!this._hoverEnabled) return;    
      if (!e) var e = window.event;
      var fromElement = (window.event) ? e.srcElement : e.target;
      var toElement = (e.relatedTarget) ? e.relatedTarget : e.toElement;
      var baseElement = this.mObject;       
      //Ignore if it is to a child of the baseElement
      while (toElement != null && toElement != baseElement && toElement.nodeName != 'BODY') toElement = toElement.parentNode	
      if (toElement == baseElement) return;
      this.startFade(this._hoverReturnOpacity, this._hoverSpeed);
    }  
 
}

//change the opacity for different browsers
function changeObjOpac(opacity, object) {
  var obj = object.style;
  obj.opacity = (opacity / 100);
  obj.MozOpacity = (opacity / 100);
  obj.KhtmlOpacity = (opacity / 100);
  obj.filter = "alpha(opacity=" + opacity + ")";   
} 

// Cross browser event assignment
function assignEventListener (aElem, aEventName, aFunction) {
  // W3C events  model
  if (document.addEventListener) {
    aElem.addEventListener(aEventName, aFunction, false);
  } 
  // MSIE event model
  else {
    aElem.attachEvent("on"+aEventName, aFunction);
  }
}

// Cross browser event removal
function unassignEventListener(aElem, aEventName, aFunction) {
  if (document.removeEventListener) {
    aElem.removeEventListener(aEventName, aFunction, false);
  }
  else {
    aElem.detachEvent("on"+aEventName, aFunction);
  }
}


// Change the height of a div from one value to another, sliding smoothly in a certain time
function slideHeight (id, heightStart, heightEnd, animationTime) {
  var change = heightEnd - heightStart;
  var speed = Math.round(animationTime / Math.abs(change));

  slideInterval = setInterval('alterHeight("' + id + '", ' + (change/speed) + ', ' + heightEnd + ');', speed);
}

function alterHeight (id, change, limit) {
  var styleObject = document.getElementById(id).style;
  var currentHeight = parseInt(styleObject.height);

  // If IE is being a pain and occasionally returned null or empty string I change it to stop undefined issues
  if (styleObject.height == null || styleObject.height == "") {
    //I change it to 1 because IE doesn't allow 0px height divs with content in them: http://www.ozzu.com/html-tutorials/tutorial-making-div-0px-high-ie6-bug-t92878.html
    styleObject.height = '1px';
    currentHeight = 1;
    document.getElementById(id).style.height = '1px';
  }

  if (((currentHeight + change) <= limit && change < 0) || ((currentHeight + change) >= limit && change > 0)) {
    document.getElementById(id).style.height = limit + 'px';
    clearInterval(slideInterval);
    return;
  }

  styleObject.height = (currentHeight + change) + 'px';
}
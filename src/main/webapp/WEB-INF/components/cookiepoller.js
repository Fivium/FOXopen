/**
* Constantly polls a cookie for changes in value and runs 
* a callback function with the new value as the only
* parameter.
* @param aCookieName name of cookie to poll for changes
* @param aIntervalMs frequency of polling (milliseconds)
* @param aCallbackFunction callback function to run
*/
function CookiePoller (aCookieName, aIntervalMs, aCallbackFunction) 
{
  // Store params
  this._cookieName = aCookieName;
  this._intervalMs = aIntervalMs;
  this._callbackFunction = aCallbackFunction;

  // Derive start value from cookie
  this._value = this._getCookieValue();
  
  // Poll the cookie
  this.startPolling();
}

// Class body
CookiePoller.prototype = 
{
  // Member variables
  _interval: null
, _cookieName: null
, _value: null
, _callbackFunction: null
, _intervalMs: null
, _polling: false
  
  /**
  * Start polling the cookie.
  */
, startPolling: function () {
    // Belt and braces
    if (this._polling) {
      return;
    }
    
    // Self-reference to deal with context switches in closure
    var self = this;
    
    // Set up polling
    this._interval = window.setInterval(function () {
      var cookieValue = self._getCookieValue();
      if (cookieValue != self._value && cookieValue != null) {
        self._value = cookieValue;
        self._callbackFunction(cookieValue);
      }
    }, this._intervalMs);
    
    // Set global flag
    this._polling = true;
  }
  
  /**
  * Stop polling the cookie.
  */
, stopPolling: function () {
    // Belt and braces
    if (!this._polling) {
      return;
    }
    
    // Clear the interval
    window.clearInterval(this._interval);
    this._interval = null;
    
    // Set global flag
    this._polling = false;
  }
  
  /**
  * Sets the value of the poller, the cookie and
  * optionally calls the callback.
  * @param aValue the value to set the cookie to
  * @param aCallbackFlag run callback (boolean)
  */
, setValue: function (aValue, aCallbackFlag) {
    this._value = aValue;
    this._setCookieValue(aValue);
    if (aCallbackFlag) {
      this._callbackFunction(aValue);
    }
  }
  
  /**
  * Sets the current cookie to passed value.
  * @param aValue new value for cookie
  */
, _setCookieValue: function (aValue) {
    // fox.js dependency
    setCookie(this._cookieName, aValue, 1);
  }
  
  /**
  * Gets the current cookie value.
  * @return value as a string
  */
, _getCookieValue: function () {
    // fox.js dependency
    return getCookie(this._cookieName);
  }
}



function getCookie(NameOfCookie) { if (document.cookie.length > 0) { begin = document.cookie.indexOf(NameOfCookie+'='); if (begin != -1) { begin += NameOfCookie.length+1; end = document.cookie.indexOf(';', begin); if (end == -1) end = document.cookie.length; return unescape(document.cookie.substring(begin, end)); } } return null; }
function setCookie(NameOfCookie, value, expiredays) { var ExpireDate = new Date (); ExpireDate.setTime(ExpireDate.getTime() + (expiredays * 24 * 3600 * 1000)); document.cookie = NameOfCookie + '=' + escape(value) + ((expiredays == null) ? '' : '; expires=' + ExpireDate.toGMTString()); }

/**
 * ServiceQueue provides a multi-tiered queuing system that constantly polls 
 * one or more queues in order of priority for work to be processed. If an 
 * item is found, it will be passed to the callback function 
 * for processing and spliced out of the queue if the callback completes
 * successfully, at which point polling will resume again from the start.
 * In this way, work items are always processed in order of priority. It is
 * also possible to lock an item to prevent it from being processed, which
 * simply leads to the next highest priority item being chosen instead, and
 * the cycle repeats until the item is unlocked again.
 * @param aPriorityArray ordered list of priority queue names, highest first
 * @param aPollFrequency interval between queue polls in milliseconds
 * @param aCallbackFunction function that takes the parameters id and a DOM
 */ 
function ServiceQueue (aPriorityArray, aPollFrequency, aCallbackFunction) {

  // Validate priorities
  if (!aPriorityArray instanceof Array) {
    throw "Cannot construct ServiceQueue without a valid array of priorities";
  }
  
  // Validate poll frequency
  try {
    // Ensure it parses as a number
    var lPollFreq = parseInt(aPollFrequency);
    if (lPollFreq == null || lPollFreq == 0 || lPollFreq == NaN) {
      throw "Poll frequency may not be null or zero";
    }
    this._intervalMs = lPollFreq;
  }
  catch (e) {
    throw "Invalid poll frequency: " + e;
  }

  if (aCallbackFunction == null || (!aCallbackFunction instanceof Function)) {
    throw "Must pass a valid callback function to ServiceQueue constructor";
  }
  else {
    this._callbackFunction = aCallbackFunction;
  }
  
  // Map priority strings to numbers
  this._priorityToNumberMap = new Array();
  this._priorityArrays = new Array();
  this._errorCount = new Array();
  
  // Loop through priorities
  for (var i = 0; i < aPriorityArray.length; i++) {
    // Set up desired priority as associative array
    var lPriorityName = aPriorityArray[i];
    this._priorityToNumberMap[lPriorityName] = i;
    this._priorityArrays[i] = new Array();
  }
  
  // Associative array for locking queued items
  this._lockedItems = new Array();
  
  // Associative array for verifying item names are known
  // to this service queue instance
  this._knownItemNames = new Array();
}

// Class body
ServiceQueue.prototype = {
  
  // Member variables
  _priorityArrays: null
, _priorityToNumberMap: null
, _interval: null
, _intervalMs: null
, _lockedItems: null
, _polling: false
, _knownItemNames: null
, MAX_ERRORS: 5
  
  /**
   * Adds a data object to a named queue.
   * @param aPriorityName name of priority queue
   * @param aQueueObjectName identifier for data object
   * @param aQueueObjectData data object
   */
, addToQueue: function (aPriorityName, aQueueObjectName, aQueueObjectData) {
    // Resolve priority queue
    var lPriorityArray = this._getPriorityArray(aPriorityName);
    // Push arbitrary object with name/value pair
    lPriorityArray.push({
      name: aQueueObjectName
    , value: aQueueObjectData
    });
    
    this._knownItemNames[aQueueObjectName] = true;
  }
 
  /**
   * Removes a data object from any queue.
   * @param aQueueObjectName identifier for data object
   */
, removeFromAnyQueue: function (aQueueObjectName) {
    // If we know nothing about this object, simply return and ignore the call
    if (!this._knownItemNames[aQueueObjectName]) {
      return;
    }
    
    // If the object isn't locked, we can work to remove it
    if (!this._isItemLocked) {
    
      // Lock it to ensure we don't spawn off any requests
      this.lockItem(aQueueObjectName);
      
      // Remove object from any priority array
      for (var n in this._priorityArrays) {
        var lPriorityArray = this._priorityArrays[n];
        if (lPriorityArray[aQueueObjectName]) {
          delete lPriorityArray[aQueueObjectName];
        }
      }
      
      // Unlock and remove the object
      this.unlockItem(aQueueObjectName);
      delete this._knownItemNames[aQueueObjectName];
    }
  }
 
  /**
   * Starts the poller.
   */
, startPolling: function () {
    // Belt and braces
    if (this._polling) {
      return;
    }
    
    // Self-reference to deal with context switches in closure
    var self = this;
    
    // Step through array and process
    this._interval = window.setInterval(function () {
      try {
        self._processNextItem();
      }
      catch (e) {
        alert(e);
      }
    }, this._intervalMs);
    
    // Set flag
    this._polling = true;
  }
  
  /**
   * Stops the poller.
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
   * Locks an item by setting a value in an
   * associative array.
   * @param aItemName data identifier to lock
   */
, lockItem: function (aItemName) {
    this._lockedItems[aItemName] = true;
  }
  
  /**
   * Unlocks a locked item by unsetting the
   * associative array value (nullification)
   * @param aItemName data identifier to unlock
   */
, unlockItem: function (aItemName) {
    this._lockedItems[aItemName] = null;
  }
  
  /**
   * Seeks the next data item to pass to the callback,
   * executes the callback and splices the data item 
   * out of the queue if successful.
   */
, _processNextItem: function () {
    // Step through arrays in order that they were added (priority order)
    for (var n in this._priorityArrays) {
      // Step through items in order that they were added
      var lPriorityArray = this._priorityArrays[n];
      
      for (var i = 0; i < lPriorityArray.length; ) {
        // Get first item from priority array (remove it from array to stop other
        // interval runs from hitting it while it's being processed)
        var lElem = lPriorityArray[i];
        
        // Preconditions for running callback: item may not be locked, must be polling,
        // callback must not be active
        if (!this._isItemLocked(lElem.name) && this._polling && !this._callbackActive) {
          // Run callback
          try {
            var lResult = this._processCallback(lElem.name, lElem.value);
            if (lResult) {
              // Remove element from array
              lPriorityArray.splice(i, 1);
              
              // Remove from known names
              delete this._knownItemNames[lElem.name];
              
              // Jump out of this function to ensure that we
              // only process one queued item per interval
              return true;
            }
            else {
              return false;
            }
          }
          catch (e) {
            // Increment error count for this item
            if (!this._errorCount[lElem.name]) {
              this._errorCount[lElem.name] = 1;
            }
            else {
              this._errorCount[lElem.name]++;
            }
            
            // Item has hit max errors
            if (this._errorCount[lElem.name] == this.MAX_ERRORS) {
              // Drop item from array
              lPriorityArray.splice(i, 1);
              
              // Remove from known names
              delete this._knownItemNames[lElem.name];
            }
          
            // Throw on, real problem occurred
            throw "ServiceQueue callback threw an unexpected error: " + e;
          }
        }
        else {
          // Item was locked, put it back in the queue
          i++;
        }
      }
    }
    // Signal to caller that all queues were empty
    // or all queued items were locked
    return false;
  }

  /**
   * Process the externally provided callback.
   * @param aQueueObjectName data identifier next in the queue
   * @param aQueueObjectData data object next in the queue
   */
, _processCallback: function (aQueueObjectName, aQueueObjectData) {
    // Belt and braces
    if (aQueueObjectName == null || aQueueObjectName == "") {
      throw "Must pass a Queue Object Name to ServiceQueue._processCallback";
    }
    else if (aQueueObjectData == null) {
      throw "Must pass Queue Object Data to ServiceQueue._processCallback";
    }
    
    // Flag to allow other calls to check whether a callback is being processed
    this._callbackActive = true;
    try {
      // Run callback, catch no errors
      // IMPORTANT: callback errors must always be thrown to caller
      return this._callbackFunction(aQueueObjectName, aQueueObjectData);
    }
    finally {
      // Always reset active flag
      this._callbackActive = false;
    }
  }
  
  /**
   * Returns a priority queue reference.
   * @param aPriorityName name of priority queue
   * @return reference to priority queue array
   */
, _getPriorityArray: function (aPriorityName) {
    var lIndex = this._priorityToNumberMap[aPriorityName];
    if (lIndex == null) {
      throw "Priority '" + aPriorityName + "' not registered in ServiceQueue";
    }
    return this._priorityArrays[lIndex];
  }
  
  /**
   * Checks if an item is locked.
   * @param aItemName the data identifier to check
   * @return boolean indicating if the item is locked
   */
, _isItemLocked: function (aItemName) {
    // Convert null (not locked) to false for caller convenience
    return this._lockedItems[aItemName] ? true : false;
  }
  
  /**
   * Checks to see if an item is in any queue.
   * @param aItemName the data identifier to check
   * @return boolean indicating if the item is in any queue
   */
, isItemInAnyQueue: function (aItemName) {
    return this._knownItemNames[aItemName] == true;
  }
}















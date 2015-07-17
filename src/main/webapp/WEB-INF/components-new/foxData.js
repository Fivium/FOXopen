/*
 * FOX Data
 *
 * Copyright 2015, Fivium Ltd.
 * Released under the BSD 3-Clause license.
 */

// Can be run through JSDoc (https://github.com/jsdoc3/jsdoc) and JSHint (http://www.jshint.com/)

/*jshint laxcomma: true, laxbreak: true, strict: false */

var FOXdata = {
  /**
   * Object containing mapping of dataDefinitionName to dataObjects
   * @private
   */
  _dataMap: {},

  /**
   * Register the name of some data specified in a module via a data-definition and an object containing the data
   * or information for the getData function to use in getting the data later asynchronously
   * @param {string} dataDefinitionName Name of the data definition object to set/get the data
   * @param {object} dataObject The data itself, or information for the getData function to use in getting the data later
   * @private
   */
  registerFoxData: function(dataDefinitionName, dataObject) {
    this._dataMap[dataDefinitionName] = dataObject;
  },

  /**
   * Get a data object or null if no data found
   * @param {string} dataDefinitionName Name of the data definition object to set/get the data
   * @param {string} dataKey Extra key used to set/get the data if the data definition was run for multiple match nodes
   * @returns {*} data or null if no data found for dataKey
   */
  getData: function(dataDefinitionName, dataKey) {
    if (!this._dataMap.hasOwnProperty(dataDefinitionName)) {
      return null;
    }

    var cachedData = this._dataMap[dataDefinitionName];
    if (cachedData.isAJAX) {
      // Go get data asynchronously
      console.error("AJAX data loading not implemented yet");
      return null;
    }
    else if (dataKey !== null && typeof cachedData[dataKey] !== 'undefined') {
      return cachedData[dataKey];
    }
    else {
      return cachedData;
    }
  }
};
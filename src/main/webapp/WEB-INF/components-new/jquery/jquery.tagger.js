/*
 * jQuery UI Tagger
 *
 * @version v0.6.1 (02/2015)
 *
 * Copyright 2015, Fivium Ltd.
 * Released under the BSD 3-Clause license.
 * https://github.com/fivium/jquery-tagger/blob/master/LICENSE
 *
 * Homepage:
 *   https://github.com/fivium/jquery-tagger/
 *
 * Authors:
 *   Nick Palmer
 *   Ben Basson
 *
 * Maintainer:
 *   Nick Palmer - nick.palmer@fivium.co.uk
 *
 * Dependencies:
 *   jQuery v1.9+
 *   jQuery UI v1.10+
 */

// Can be run through JSDoc (https://github.com/jsdoc3/jsdoc) and JSHint (http://www.jshint.com/)

/*jshint laxcomma: true, laxbreak: true, strict: false */

/**
 * See (http://jquery.com/).
 * @name jQuery
 * @class
 * See the jQuery Library  (http://jquery.com/) for full details. This just
 * documents the function and classes that are added to jQuery by this plug-in.
 */

/**
 * See (http://jquery.com/)
 * @name widget
 * @class
 * See the jQuery Library  (http://jquery.com/) for full details. This just
 * documents the function and classes that are added to jQuery by this plug-in.
 * @memberOf jQuery
 */

/**
 * See (http://jquery.com/)
 * @name ui
 * @class
 * See the jQuery Library  (http://jquery.com/) for full details. This just
 * documents the function and classes that are added to jQuery by this plug-in.
 * @memberOf jQuery.widget
 */

(function ($) {
  /**
   * tagger - Autocomplete and tagging widget
   *
   * @class tagger
   * @memberOf jQuery.widget.ui
   * @param {object} options - Must pass in the available tags and optionally
   *                           other information also
   * @version 0.6.1
   * @license http://github.com/fivium/jquery-tagger/blob/master/LICENSE
   * @copyright Fivium ltd.
   * @author Nick Palmer
   */
  $.widget('ui.tagger', /** @lends jQuery.widget.ui.tagger */ {

    /**
     * Default options, can be overridden by passing in an object to the constructor with these properties
     * @property {array}    availableTags       - Array of JSON tag objects
     * @property {array}    ajaxURL             - URL to autocomplete webservice for updating available tags
     * @property {array}    preselectedTags     - Array of tag ID's that are selected in the element (helps performance)
     * @property {integer}  characterThreshold  - How many characters must be typed before searching
     * @property {integer}  typingTimeThreshold - How many milliseconds to wait after the last keypress before filtering
     * @property {boolean}  caseSensitive       - Case sensitive searching - defaults to false
     * @property {string}   placeholder         - Placeholder text for input area
     * @property {string}   baseURL             - Base URL used for images
     * @property {string}   imgDownArrow        - URL for down arrow image (after baseURL)
     * @property {string}   imgRemove           - URL for remove image (after baseURL)
     * @property {string}   imgSearch           - URL for search image (after baseURL)
     * @property {boolean}  sortedOutput        - Sort the suggestion lists by tag.sort
     * @property {boolean}  displayHierarchy    - Indent suggestions to show hierarchy
     * @property {integer}  indentMultiplier    - When indenting suggestions, how much to multiple tag.level by
     * @property {integer}  tabindexOffset      - Then creating items it can tab to, what the tabindex should initially be
     * @property {string}   noSuggestText       - Text to show when no suggestions can be found
     * @property {string}   emptyListText       - Text to show when no suggestions in the list
     * @property {string}   searchTooltipText   - Text to show as tooltip for the ajax search icon
     * @property {string}   ajaxErrorFunction   - Function definition to use in the event of an AJAX request error, function(tagger, data)
     * @property {string}   loadingClass        - Class on an sibling to the select used to fill while the js loads the tagger
     * @property {integer}  inputExpandExtra    - How many extra pixels to add on to the end of an input when expanding
     * @property {string}   fieldWidth          - Override width e.g. 20em
     * @property {string}   fieldHeight         - Override height e.g. 20em
     * @property {string}   suggestWidth        - Set a hard width for the suggestion list (overrides maxwidth) e.g. 50em
     * @property {string}   suggestMaxWidth     - Max width of the suggestion list (so it can be wider than the field) e.g. 50em
     * @property {string}   suggestMaxHeight    - Max height of the suggestion list e.g. 20em
     * @property {boolean}  mandatorySelection  - Make it mandatory that a value is chosen - defaults to false, no effect in multiselect mode
     */
    options: {
      availableTags       : null
      , ajaxURL             : null
      , preselectedTags     : null
      , characterThreshold  : 1
      , typingTimeThreshold : 200
      , caseSensitive       : false
      , placeholder         : null
      , baseURL             : '/img/'
      , imgDownArrow        : 'dropdown.png'
      , imgRemove           : 'remove.png'
      , imgSearch           : 'search.png'
      , sortedOutput        : false
      , displayHierarchy    : false
      , indentMultiplier    : 1
      , tabindexOffset      : null
      , noSuggestText       : 'No suggestions found'
      , emptyListText       : 'All items selected already'
      , limitedText         : 'There are too many results to show, type more characters to filter these results further'
      , searchTooltipText   : 'Enter text to get suggestions'
      , ajaxErrorFunction   : function(self, data){self._showMessageSuggestion('AJAX Search failed', 'error');}
      , loadingClass        : '.tagger-loading'
      , inputExpandExtra    : 14
      , fieldWidth          : '30em'
      , fieldHeight         : null
      , suggestWidth        : null
      , suggestMaxWidth     : null
      , suggestMaxHeight    : null
      , mandatorySelection  : false
    },

    /**
     * Tagger widget constructor
     *
     * Based on the select element it is created on it reads information from it,
     * Creates new elements for the tagger widget, adds event listeners and deals
     * with pre-selected tags.
     */
    _create: function () {
      var self = this;
      this.canFireActions = false;

      if (this.element.is('select')) {
        // Check readonly mode
        this.readonly = this.element.attr('readonly');

        // Set tabindexOffset
        if (this.options.tabindexOffset === null) {
          if (this.element.attr('tabindex')) {
            this.tabIndex = this.element.attr('tabindex')
          }
          else {
            this.tabIndex = '0';
          }
        }
        else {
          this.tabIndex = this.options.tabindexOffset;
        }

        // Check cardinality mode
        this.singleValue = !this.element.attr('multiple');

        // Initialise the tag counter
        this.tagCount = 0;

        // Hide select
        this.element.hide();

        // Remove any loading divs
        this.element.siblings(this.options.loadingClass).remove();

        // Construct tagger widget
        this.taggerWidget = $('<div class="tagger"></div>').insertAfter(this.element);
        if (this.readonly) {
          this.taggerWidget.addClass('tagger-readonly');
        }

        // Set dimensions
        if (this.options.fieldWidth !== null) {
          this.taggerWidget.css('width', this.options.fieldWidth);
        }
        else {
          // Default width to the width of the original select element if null passed in
          this.taggerWidget.css('width', this.element.css('width'));
        }
        if (this.options.fieldHeight !== null) {
          this.taggerWidget.css('height', this.options.fieldHeight);
        }

        if (!this.readonly) {
          // Add the suggestion drop arrow and and text input if not readonly
          this.taggerInput = $('<input type="text" class="intxt" autocomplete="off"/>').appendTo(this.taggerWidget);
          this.taggerButtonsPanel = $('<div class="tagger-buttons"></div>');
          this.taggerButtonsPanel.appendTo(this.taggerWidget);
          if (!this.options.ajaxURL) {
            this.taggerSuggestionsButton = $('<div class="droparrow hittarget"><img src="' + this.options.baseURL + this.options.imgDownArrow + '" /></div>').appendTo(this.taggerButtonsPanel);
          } else {
            this.taggerSuggestionsButton = $('<div class="search"><img src="' + this.options.baseURL + this.options.imgSearch + '" title="' + this.options.searchTooltipText + '" /></div>').appendTo(this.taggerButtonsPanel);
          }
          this.taggerSuggestionsButton.attr("tabindex", this.tabIndex);

          // Add placeholder text to text input field
          if (this.options.placeholder !== null) {
            this.taggerInput.attr("placeholder", this.options.placeholder);
          }

          // Set the tab index on the input field
          this.taggerInput.attr("tabindex", this.tabIndex);

          // Esc should hide the tagger suggestions globally
          this.taggerWidget.bind('keydown', function (event) {
            if (event.target && event.which === 27) { // Esc
              self.taggerSuggestions.hide();
            }
          });

          // Capture the keypress event for any child elements - redirect any chars to the current input field
          this.taggerWidget.bind('keypress', function (event) {
            if (event.which !== 0 && event.charCode !== 0  && !event.ctrlKey && !event.metaKey && !event.altKey) {
              // If the keypress came from the main input or the filter, ignore this event or we'll potentially
              // just get in the way of the character being inserted and it'll be put at the end, instead of wherever
              // typed
              if (event.target === self.taggerInput.get(0) || (self.taggerFilterInput && event.target === self.taggerFilterInput.get(0))) {
                return;
              }
              self._showSuggestions(true);
              self._appendCharAndFilter(event);
              event.preventDefault();
            }
          });
        }

        // Clearer div makes sure the widget div keeps its height
        $('<div class="clearer"></div>').appendTo(this.taggerWidget);

        if (!this.readonly) {
          // If not readonly, stub out an empty suggestion list
          this.taggerSuggestions = $('<div class="suggestions"></div>').appendTo(this.taggerWidget);

          // Put a filter at the top of the suggestion list in single-select mode
          if (this.singleValue) {
            this.taggerFilterInput = $('<input type="text" class="filtertxt" autocomplete="off"/>').appendTo(this.taggerSuggestions);
            this.taggerFilterInput.attr("tabindex", this.tabIndex);
            // Add placeholder text to text input field
            if (this.options.placeholder !== null) {
              this.taggerFilterInput.attr("placeholder", this.options.placeholder);
            }
            this.taggerFilterInput.hide();
          }

          this.taggerSuggestionsList = $('<ul></ul>').appendTo(this.taggerSuggestions);

          // Event listener to hide suggestions list if clicking outside this tagger widget
          // Using mousedown because IE11 reports the event.target for a mouseup as the HTML
          // root element rather than the original click target, mousedown seems to work
          // cross browser
          $(document).mousedown(function (event) {
            var selfTaggerWidget = self.taggerWidget.get(0);
            if ($(event.target).parents(".tagger").get(0) !== selfTaggerWidget && event.target !== selfTaggerWidget) {
              self.taggerSuggestions.hide();
            }
            // if clicking through to the parent div
            else if (event.target === selfTaggerWidget) {
              // focus the first focusable item
              if (!self.singleValue || self.tagCount === 0) {
                self.taggerWidget.find("input[tabindex]:visible").first().focus();
                event.preventDefault();
              }
              // For now, only show the list automatically on click if we have a single value selected
              // When performance of the suggestion list building is improved, we can enable this functionality
              // for multi selectors and empty taggers - note redundant boolean logic preserved so that the following
              // suggestion parameter is still valid if this check is removed
              if (self.singleValue && self.tagCount === 1) {
                // In single select mode, with a single tag selected already
                // we should focus the first item in the suggestion list (which
                // will be the filter input).
                // NB: Using setTimeout because trying to do this immediately causes
                // the focus to fail, presumably because the corresponding mouseup triggers
                // focus elsewhere.
                setTimeout(function(){
                  self._showSuggestions(self.singleValue && self.tagCount === 1);
                }, 0);
              }
            }
          });

          // Bind event to window to resize the suggestion list when the window's resized
          var self = this;
          $(window).resize(function() {
            self._setSuggestionListDimensions(self);
          });

          // Bind suggest list toggle to left clicking suggestion button
          if (!this.options.ajaxURL) {
            this.taggerSuggestionsButton.bind('mouseup keyup', function (event) {
              if ((event.type === "mouseup" && event.which === 1) // left click
                || (event.type === "keyup" && (event.which === 13 || event.which === 32 || event.which === 40))) { // enter || space || down arrow
                // If the suggestion list is visible already, then toggle it off
                if (self.taggerSuggestions.is(":visible")) {
                  self.taggerSuggestions.hide();
                }
                // otherwise show it
                else {
                  self._showSuggestions(true);
                }
                event.preventDefault();
              }
            });
          }
          // Expand the input field to fit its contents
          this._inputExpand(this.taggerInput);

          // Bind event to text input to expand input to fit contents and deal with key input
          this.taggerInput.bind({
            keydown: function (event) {
              // Expand the input field to fit its contents
              self._inputExpand($(this));

              if (event.target) {
                switch (event.which) {
                  case 13: // Enter key
                    // If they hit enter with just one item in the suggestion list, add it, otherwise focus the top item
                    if (self.taggerSuggestionsList.children('[tabindex]').length === 1) {
                      self._addTagFromID(self.taggerSuggestionsList.children('[tabindex]').first().data('tagid'));
                      self._selectionReset(true, true);
                    }
                    else {
                      self.taggerSuggestionsList.children('[tabindex]').first().focus();
                    }
                    event.preventDefault();
                    break;
                  case 8: // Backspace
                    if (self.taggerInput.val().length < 1) {
                      // If there is nothing in the input, change focus to the last tag
                      var removeTag = $('.tag', self.taggerWidget).last();
                      // Move focus to last tag if there is one
                      if (removeTag.length > 0) {
                        removeTag.focus();
                      }
                      event.preventDefault();
                    }
                    break;
                  case 27: // Esc
                    self.taggerSuggestions.hide();
                    event.preventDefault();
                    break;
                  default:
                    break;
                }
              }
            },
            keyup: function (event) {
              self._inputExpand(self.taggerInput);
              if (event.which !== 13 && event.which !== 40 && event.which !== 27) { // key up not enter or down arrow or esc key
                self._filterSuggestions($(this).val(), false);
              }
              else if (event.which === 40) { // Down Arrow
                if ( !self.options.ajaxURL || self.taggerSuggestions.is(":visible")) {
                  self._showSuggestions(true);
                }
              }
            },
            mouseup: function (event) {

              // For now, only show the list automatically on click if we have a single value selected
              // When performance of the suggestion list building is improved, we can enable this functionality
              // for multi selectors and empty taggers - note redundant boolean logic preserved so that the following
              // suggestion parameter is still valid if this check is removed
              if (self.singleValue && self.tagCount === 1) {
                // In single select mode, with a single tag selected already
                // we should focus the first item in the suggestion list (which
                // will be the filter input)
                self._showSuggestions(self.singleValue && self.tagCount === 1);
              }
            }
          });

          // If we have a list filter then bind events to it
          if (this.taggerFilterInput) {
            this.taggerFilterInput.bind({
              keyup: function (event) {
                if (event.target) {
                  if (event.which !== 13 && event.which !== 40) { // key up not enter or down arrow
                    self._filterSuggestions($(this).val(), true);
                  }
                  else if (event.which === 40) {
                    // Focus top item in suggestion list
                    self.taggerSuggestionsList.children('[tabindex]').first().focus();
                    event.preventDefault();
                  }
                }
              },
              keydown: function (event) {
                if (event.target) {
                  switch (event.which) {
                    case 8: // Backspace
                      if ($(this).val().length === 0 && self.loadedFiltered) {
                        if (this.singleValue && this.taggerFilterInput) {
                          // Ignore in single select mode with filter in suggestions to stop nav-back
                        }
                        else {
                          // Hide it
                          self.taggerSuggestions.hide();
                          // Focus the drop arrow
                          self.taggerSuggestionsButton.focus();
                        }
                        event.preventDefault();
                      }
                      break;
                    case 13: // Enter key
                      // If they hit enter with just one item in the suggestion list, add it, otherwise focus the top item
                      if (self.taggerSuggestionsList.children('[tabindex]').length === 1) {
                        self._addTagFromID(self.taggerSuggestionsList.children('[tabindex]').first().data('tagid'));
                        self._selectionReset(true, true);
                      }
                      else {
                        self.taggerSuggestionsList.children('[tabindex]').first().focus();
                      }
                      event.preventDefault();
                      break;
                    default:
                      break;
                  }
                }
              }
            });
          }

          // Capture focus on the underlying element and redirect that focus to the tagger
          this.element.get(0).focus = function () {
            self.taggerWidget.find('[tabindex]:visible').first().focus();
          };
        }

        // Let the available tags be accessed through a nicer name
        if (this.options.availableTags) {
          this.tagsByID = this.options.availableTags;
        }
        // Convert options to JS objects if no JSON is supplied
        else {
          this.tagsByID = {};
          this.element.children("option").each(function (index) {
            self.tagsByID[$(this).val()] = {id: $(this).val(), key: $(this).text(), hidden: '', level: 0, suggestable: true, historical: false, sort: index};
          });
        }

        var preselectedTags = this.options.preselectedTags;
        if (this.singleValue && this.options.mandatorySelection && preselectedTags === null) {
          preselectedTags = [this.element.children()[0].value];
        }

        // Deal with already selected options
        if (preselectedTags === null) {
          this.element.children("option:selected").each(function () {
            // Set any selected options that aren't in the availableTags as historical entries so they can be displayed and removed but not added
            if (!self.tagsByID[$(this).val()]) {
              self.tagsByID[$(this).val()] = {id: $(this).val(), key: $(this).text(), suggestion: $(this).text(), hidden: '', level: 0, suggestable: false, historical: true};
            }
            // Add tags for any selected options
            self._addTagFromID($(this).val());
          });
        }
        else {
          var preselectedTag = null;
          for (var i = 0; i < preselectedTags.length; i++) {
            preselectedTag = preselectedTags[i];
            // Set any selected options that aren't in the availableTags as historical entries so they can be displayed and removed but not added
            if (!self.tagsByID[preselectedTag]) {
              self.tagsByID[preselectedTag] = {id: preselectedTag, key: $($('option[value="'+preselectedTag+'"]', this.element)[0]).text(), suggestion: '', hidden: '', level: 0, suggestable: false, historical: true};
            }
            // Add tags for any selected options
            self._addTagFromID(preselectedTag);
          }
        }
        this.canFireActions = true;
      }
      else {
        // Stub out console.log if not supported in browser
        if (typeof console === "object") {
          if (typeof console.log === "function") {
            console.log('Tagger widget only works on select elements');
          }
        }
      }
    },

    /**
     * Filter the available tags by the input text and load suggestions into suggestion list
     * @param {string} value the string value to filter by
     */
    filterTags: function (value) {
      var searchString = value;
      var searchStringLowerCase = value.toLowerCase();
      var filteredResults = {};

      // Go through each tag
      for (var tagID in this.tagsByID) {
        if (this.tagsByID.hasOwnProperty(tagID)) {
          var tag = this.tagsByID[tagID];
          if (!tag.suggestable || tag.historical) {
            // Skip non-suggestable tags
            continue;
          }

          // Add tag to filteredResults object if it contains the search string in the key, hidden or suggestion fields
          if (this.options.caseSensitive) {
            if (tag.key.indexOf(searchString) >= 0
              || (tag.hidden && tag.hidden.indexOf(searchString) >= 0)
              || $('<div/>').html(tag.suggestion).text().replace(/<.*?[^>]>/g,'').indexOf(searchString) >= 0) {
              filteredResults[tagID] = $.extend(true, {}, tag);
              filteredResults[tagID].suggestable = true;
            }
          }
          else {
            if (tag.key.toLowerCase().indexOf(searchStringLowerCase) >= 0
              || (tag.hidden && tag.hidden.toLowerCase().indexOf(searchStringLowerCase) >= 0)
              || $('<div/>').html(tag.suggestion).text().replace(/<.*?[^>]>/g,'').toLowerCase().indexOf(searchStringLowerCase) >= 0) {
              filteredResults[tagID] = $.extend(true, {}, tag);
              filteredResults[tagID].suggestable = true;
            }
          }
        }
      }
      // Load filtered results into the suggestion list
      this._loadSuggestions(filteredResults, false);
      this.loadedFiltered = true;
    },

    /**
     * Load suggestions into suggestion list from ajaxURL
     * @param {string} value the string value to filter by
     * @protected
     */
    _ajaxLoadSuggestions: function (value) {
      var searchString = value;
      var self = this;

      // If we already have a filter pending, cancel it before making our new one
      if (this.pendingFilterEvent) {
        clearTimeout(this.pendingFilterEvent);
      }

      // Set a new pending Filter event to fire in this.options.typingTimeThreshold milliseconds
      this.pendingFilterEvent = setTimeout(
        function() {
          $.ajax({
            url: self.options.ajaxURL,
            type: "GET",
            data: {
              elementId: self.element.attr('id')
            , search: searchString
            },
            dataType: 'json',
            success: function (data) {
              // Make sure any tags already displayed are overlaid on their counterparts in the new list
              $.each(self.tagsByID, function(key, tag){
                if (self._isAlreadyDisplayingTag(key)) {
                  data[key] = tag;
                }
              });
              self.tagsByID = data;
              self._loadSuggestions(data, false);
              self.loadedFiltered = true;
              self._showSuggestions(false);
            },
            error: function(data) {
              self.options.ajaxErrorFunction(self, data);
            }
          });
          delete self.pendingFilterEvent;
        }
      , this.options.typingTimeThreshold
      );
    },

    /**
     * Show a message to the user in the suggestions list section instead of results
     * @param {string} msg Message to show
     * @protected
     */
    _showMessageSuggestion: function(msg, className) {
      // Set width
      this._setSuggestionListDimensions(this);

      // Show the container
      this.taggerSuggestions.show();

      // Clear out suggestion list
      this.taggerSuggestionsList.children().remove();

      // Add message
      $('<li class="message '+className+'">' + msg + '</li>').appendTo(this.taggerSuggestionsList);
    },

    /**
     * Returns the tagger input or the tagger filter input depending on which is visible.
     * @return jQuery wrapped InputElement
     * @protected
     */
    _getVisibleInput: function () {
      if (this.taggerFilterInput && this.taggerFilterInput.is(":visible")) {
        return this.taggerFilterInput;
      }
      else {
        return this.taggerInput;
      }
    },

    /**
     * Updates the input or filter input and filtes results. Also places focus in the input
     * after updating the value.
     *
     * @param {jQuery} targetInput the jQuery wrapped input element to manipulate and focus
     * @param {string} newValue the new value to set
     * @protected
     */
    _updateInputAndFilter: function (targetInput, newValue) {
      // Set focus and new value - order is important otherwise the cursor can
      // sometimes end up before the text was inserted
      targetInput.focus();
      targetInput.val(newValue);

      // The non-filter input needs to grow with its text content
      if (targetInput === this.taggerInput) {
        this._inputExpand(targetInput);
      }

      this._filterSuggestions(newValue, false);
    },

    /**
     * Diverts the key press event passed to this function to whichever input is currently
     * visible. Should be registered as an event handler for keypress events on elements
     * that may be focused but are not the input being used; i.e. the drop-down arrow,
     * suggestion items, tags, etc.
     *
     * @param {event} event the keypress event to handle
     * @protected
     */
    _appendCharAndFilter: function (event) {
      // Belt and braces
      if (event.type !== 'keypress') {
        throw "Wrong event type passed to _appendCharAndFilter(), expected keypress)";
      }

      // Decode char to concat onto existing filter string
      var newChar = String.fromCharCode(event.charCode);

      var targetInput = this._getVisibleInput();

      // Update the UI and filter
      var newVal = targetInput.val() + newChar;
      this._updateInputAndFilter(targetInput, newVal);
    },

    /**
     * Removes the last character
     * @param {event} event the keypress event to handle
     * @protected
     */
    _removeLastCharAndFilter: function (event) {
      var targetInput = this._getVisibleInput();

      // Update the UI and filter
      var newVal = targetInput.val().substring(0, targetInput.val().length-1);
      this._updateInputAndFilter(targetInput, newVal);
    },

    /**
     * Load tags into the suggestion list
     * @param {object} suggestableTags - Object containing members of tagID to tag object
     * @param {boolean} allowIndent - Allow indenting of suggestion lists if true
     * @protected
     */
    _loadSuggestions: function (suggestableTags, allowIndent) {
      var self = this;

      // Function to bind to suggestion list elements
      function suggestionBind(event) {
        var currentSelection = $(this);
        if (event.type === "mouseup" || event.type === "keyup" || event.type === "keydown") {
          if ((event.type === "mouseup" && event.which === 1) || (event.type === "keydown" && event.which === 13)) { // Click or enter
            // Handle suggestion adding
            self._addTagFromID(currentSelection.data('tagid'));
            self._selectionReset(true, true);
            event.preventDefault();
          }
          else if (event.type === "keydown" && (event.which === 38 || (event.which === 9 && event.shiftKey))) { // Up arrow / shift+tab (Move selection up and up into the input)
            // Override default browser tab control and allow arrow keys too
            var prevTarget = $(event.target).prevAll('li[tabindex]').first();
            if (prevTarget.is('li')) {
              prevTarget.focus();
            }
            else if (self.taggerFilterInput && self.taggerFilterInput.is(":visible")) {
              self.taggerFilterInput.focus();
            }
            else {
              self.taggerInput.focus();
            }
            event.preventDefault();
          }
          else if (event.type === "keydown" && (event.which === 40 || (event.which === 9 && !event.shiftKey))) { // Down arrow / tab (Move selection down, stop at the end)
            // Override default browser tab control and allow arrow keys too
            var nextTarget = $(event.target).nextAll('li[tabindex]').first();
            if (nextTarget.is('li')) {
              nextTarget.focus();
              event.preventDefault();
            }
          }
          else if (event.type === "keyup" && event.which === 36) { // Home key
            var prevTarget = $(event.target).prevAll('li[tabindex]').last();
            if (prevTarget.is('li')) {
              prevTarget.focus();
              event.preventDefault();
            }
          }
          else if (event.type === "keyup" && event.which === 35) { // End key
            var prevTarget = $(event.target).nextAll('li[tabindex]').last();
            if (prevTarget.is('li')) {
              prevTarget.focus();
              event.preventDefault();
            }
          }
          else if (event.type === "keyup" && event.which === 8) { // Backspace
            self._removeLastCharAndFilter(event);
            event.preventDefault();
          }
        }
        // If the user is typing, then divert that typing to the input field
        else if (event.type === "keypress" && event.which !== 0 && event.charCode !== 0 && !event.ctrlKey && !event.metaKey && !event.altKey) {
          self._appendCharAndFilter(event);
        }
        else {
          // Deal with setting focus properly and displaying the focus for IE6
          if (event.type === "focus") {
            $(this).addClass('focus');
          }
          else if (event.type === "blur") {
            $(this).removeClass('focus');
          }
          else if (event.type === "mouseenter") {
            $(this).addClass('focus');
            $(this).focus();
          }
          else if (event.type === "mouseleave") {
            $(this).removeClass('focus');
            $(this).blur();
          }
        }
      }

      // Clear out suggestion list
      this.taggerSuggestionsList.children().remove();

      // Load suggestions if there are some, or a message if not
      var suggestableTagArray = $.map(suggestableTags, function(n, i) { return [[i, n.sort]];});
      if (suggestableTagArray.length > 0) {
        if (this.options.sortedOutput) {
          // Sort based on the sort member of the tag objects passed in, serialised to [1] above
          suggestableTagArray.sort(
            function(a, b) {
              if (a[1] === undefined) {
                return b[1];
              }
              else if (b[1] === undefined) {
                return a[1];
              }
              return a[1] - b[1];
            }
          );
        }

        // Load in all suggestable tags
        for (var i = 0; i < suggestableTagArray.length; i++) {
          var tag = suggestableTags[suggestableTagArray[i][0]];
          // Don't add suggestion if the tag isn't selectable and it's not displaying hierarchy, the tag is historical
          //  or if the tag has no key and id tuple
          if ((!tag.suggestable && !this.options.displayHierarchy) || tag.historical || !(tag.key && tag.id)) {
            continue;
          }
          // Create and add the suggestion to the suggestion list
          var suggestion = $('<li></li>').attr("tabindex", this.tabIndex).appendTo(this.taggerSuggestionsList);
          if (tag.suggestion && tag.suggestion !== null && tag.suggestion !== '') {
            suggestion.html($('<div/>').html(tag.suggestion).text());
          }
          else {
            suggestion.text(tag.key);
          }

          // Bind actions to the suggestion
          suggestion.bind('mouseup keyup keydown mouseleave mouseenter blur focus', suggestionBind);

          // Attach data to it so when it's selected we can reference what it's for
          suggestion.data("tagid", tag.id);

          // Deal with hierarchy view
          if (this.options.displayHierarchy && allowIndent) {
            if (tag.level > 0) {
              // Indent suggestions
              suggestion.css('padding-left', (tag.level * this.options.indentMultiplier) + 'em');
            }
            if (!tag.suggestable) {
              // If it's not suggestable (already selected) then just grey it out, remove it from tabindex and unbind events
              suggestion.addClass('disabled');
              suggestion.unbind();
              suggestion.removeAttr('tabindex');
            }
          }
        }
      }
      else {
        // Add message if filtering meant no items to suggest
        $('<li class="missing">' + this.options.noSuggestText + '</li>').appendTo(this.taggerSuggestionsList);
      }

      // Add message if nothing ended up in the list (e.g. all selectable items selected)
      if (this.taggerSuggestionsList.children().length === 0) {
        $('<li class="missing">' + this.options.emptyListText + '</li>').appendTo(this.taggerSuggestionsList);
      }

      if (suggestableTags.limited) {
        $('<li class="limited">' + this.options.limitedText + '</li>').appendTo(this.taggerSuggestionsList);
      }
    },

    /**
     * Set the dimensions of the suggestion list container
     * @protected
     */
    _setSuggestionListDimensions: function(taggerInstance) {
      // Set width
      if (taggerInstance.options.suggestMaxWidth === null && taggerInstance.options.suggestWidth === null) {
        taggerInstance.taggerSuggestions.width(taggerInstance.taggerWidget.innerWidth());
      }
      else if (taggerInstance.options.suggestWidth !== null) {
        taggerInstance.taggerSuggestions.width(taggerInstance.options.suggestWidth);
      }
      else if (taggerInstance.options.suggestMaxWidth !== null) {
        taggerInstance.taggerSuggestions.css('min-width', taggerInstance.taggerWidget.innerWidth());
        taggerInstance.taggerSuggestions.css('max-width', taggerInstance.options.suggestMaxWidth);

        // Deal with quirks
        if (!jQuery.support.boxModel) {
          if (taggerInstance.taggerSuggestions.width() < taggerInstance.taggerWidget.innerWidth()) {
            taggerInstance.taggerSuggestions.width(taggerInstance.taggerWidget.innerWidth());
          }
          else if (taggerInstance.taggerSuggestions.width() > taggerInstance.options.suggestMaxWidth) {
            taggerInstance.taggerSuggestions.width(taggerInstance.options.suggestMaxWidth);
          }
        }
      }

      // Set height
      if (taggerInstance.options.suggestMaxHeight !== null) {
        taggerInstance.taggerSuggestions.css('max-height', taggerInstance.options.suggestMaxHeight);

        // Deal with quirks
        if (!jQuery.support.boxModel) {
          if (taggerInstance.taggerSuggestions.height() > taggerInstance.options.suggestMaxHeight) {
            taggerInstance.taggerSuggestions.height(taggerInstance.options.suggestMaxHeight);
          }
        }
      }
    },

    /**
     * Filters the suggestions, using a provided value.
     * @param {string} value the text string to filter by
     * @param {boolean} hideSuggestions boolean - should the suggestions be hidden
     *   if the value is less than the required character threshold?
     * @protected
     */
    _filterSuggestions: function (value, hideSuggestions) {
      if (value.length >= this.options.characterThreshold) {
        // If text is longer than the threshold start filtering and showing the filtered results
        if (!this.options.ajaxURL) {
          this.filterTags(value);
          this._showSuggestions(false);
        }
        // If ajaxURL is set, load the suggestions from URL instead of filtering the tag list
        else {
          this._ajaxLoadSuggestions(value);
        }
      }
      // If under the threshold and was previously filtered, reset the list
      else if (this.loadedFiltered) {
        if (hideSuggestions) {
          // Hide it
          this.taggerSuggestions.hide();
        }
        // Reload in all suggestions
        this._loadSuggestions(this.tagsByID, true);
        // Clear the flag
        this.loadedFiltered = false;
      }
    },

    /**
     * Show the suggestions list, making sure it's the correct size. Will initialise contents
     * if necessary. Will focus first list item if requested to do so.
     * @param {boolean} focusFirstItem whether the first item in the suggestion list received focus
     * @protected
     */
    _showSuggestions: function (focusFirstItem) {
      // Set width
      this._setSuggestionListDimensions(this);

      // Show the container
      this.taggerSuggestions.show();

      // Show the filter if necessary
      if (this.singleValue && this.taggerFilterInput && this.tagCount === 1) {
        this.taggerFilterInput.show();
      }
      else if (this.taggerFilterInput) {
        this.taggerFilterInput.hide();
      }

      var self = this;
      var loadSuggestionsInternal = function () {
        self._loadSuggestions(self.tagsByID, true);
        // Set the flag to show it's not loaded filtered results
        self.loadedFiltered = false;
        // Focus the first item in the list, which may be the filter, or may be an option
        if (focusFirstItem) {
          self.taggerSuggestions.find('[tabindex]:visible').first().focus();
        }
      }

      // Load suggestions on first hit
      if (this.taggerSuggestionsList.children().length === 0) {
        // If there are more than 300 items, show a loading item first as it could take a while
        if ($.map(this.tagsByID, function(n, i) { return i;}).length > 300) {
          $('<li class="missing">Loading...</li>').appendTo(this.taggerSuggestionsList);
          setTimeout(loadSuggestionsInternal, 300); // Fixed timeout of 300ms for now
        }
        // If less than 300 items just load all suggestions into the suggestions list
        else {
          loadSuggestionsInternal();
        }
      }
      else {
        // Focus the first item in the list, which may be the filter, or may be an option
        if (focusFirstItem) {
          this.taggerSuggestions.find('[tabindex]:visible').first().focus();
        }
      }
    },

    /**
     * Set the width of an input box to fit the value string
     * @param {InputElement} input - HTML Input Element to set the width of
     * @protected
     */
    _inputExpand: function (input) {
      // Create a hidden span to store the value of the input and read actual dimensions from
      var taggerInputSpan = $('<span class="hiddenInputSpan"></span>').appendTo(this.taggerWidget);
      // Make sure the hidden span has the same font properties
      taggerInputSpan.css({
        fontSize: this.taggerInput.css('fontSize'),
        fontFamily: this.taggerInput.css('fontFamily'),
        fontWeight: this.taggerInput.css('fontWeight'),
        letterSpacing: this.taggerInput.css('letterSpacing')
      });

      // Put the input contents (or placeholder) into the hidden span
      if (input.val() !== "") {
        taggerInputSpan.html(input.val().replace(/&/g, '&amp;').replace(/\s/g,'&nbsp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') + " ");
      }
      else if (input.attr('placeholder') && input.attr('placeholder') !== "") {
        taggerInputSpan.html(input.attr('placeholder').replace(/&/g, '&amp;').replace(/\s/g,'&nbsp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'));
      }

      // Set the width of the input to be the width of the span, making sure not to overflow the general widget bounds
      input.width(Math.min(input.parent().innerWidth() - this.taggerSuggestionsButton.outerWidth(), taggerInputSpan.width() + this.options.inputExpandExtra));

      taggerInputSpan.remove();
    },

    /**
     * After selecting a tag from the suggestions, reset the tagger widget
     * @param {boolean} shouldHideMenu should the menu be hidden?
     * @param {boolean} shouldClearInputs should the input fields be cleared?
     * @protected
     */
    _selectionReset: function (shouldHideMenu, shouldClearInputs) {
      // Clear input
      if (shouldClearInputs) {
        this.taggerInput.val('');
        if (this.taggerFilterInput) {
          this.taggerFilterInput.val('');
        }
      }
      // Expand properly
      this._inputExpand(this.taggerInput);
      // Clear filtered suggestions
      this._loadSuggestions(this.tagsByID, true);
      // Set the flag to show it's not loaded filtered results
      this.loadedFiltered = false;
      // Focus input
      this.taggerInput.focus();
      // Hide suggestion list
      if (shouldHideMenu) {
        this.taggerSuggestions.hide();
      }
    },

    /**
     * Add a tag, given a tags ID, to the widget and mark it as selected in the
     * underlying select elements option list
     * @param {string} tagID - ID of the tag to add
     * @protected
     */
    _addTagFromID: function (tagID) {
      var self = this;
      var tagData = this.tagsByID[tagID];
      var tag;

      // Check tag not already added
      if (this._isAlreadyDisplayingTag(tagID)) {
        return;
      }

      // Remove any other selected tag if in single mode
      //   Temporarily disable actions while doing the remove as you want to run
      //   the action after the subsequent add
      if (this.singleValue) {
        var tmpActionFireStatus = this.canFireActions;
        this.canFireActions = false;
        $('.tag', this.taggerWidget).each(function () {
          self._removeTagByElem($(this), true, true);
        });
        $('.removetag', this.taggerWidget).each(function () {
          $(this).remove();
        });
        this.canFireActions = tmpActionFireStatus;
      }

      if (!this.readonly) {
        // Select the option in the underlying select element
        if ($('option[value="'+tagID+'"]', this.element).length > 0) {
          $('option[value="'+tagID+'"]', this.element).attr("selected","selected");
        } else {
          $('<option value="'+tagID+'" selected="selected">'+$('<div/>').html(tagData.key).text()+'</option>').appendTo(this.element);
        }
        // Add the HTML to show the tag
        tag = $('<div class="tag"></div>').insertBefore(this.taggerInput);
        tag.attr("tabindex", this.tabIndex);
        tag.text($('<div/>').html(tagData.key).text());
        tag.data("tagid", tagID);
        var tagRemover = $('<span class="removetag hittarget"><img src="' + this.options.baseURL + this.options.imgRemove + '" /></span>');

        // Reusable tag removal closure
        var tagRemoveProcessing = function () {
          // If the menu is open, keep it open...
          if (self.taggerSuggestions.is(':visible')) {
            // Check to see if the filter has any value
            var shouldUseFilterValue = self.taggerFilterInput && self.taggerFilterInput.val().length > 0;
            // If the filter has a value, we can keep it, so don't clear the inputs just yet - we'll do that
            // manually here instead in the setTimeout() instead of immediately as part of removing the tag
            self._removeTagByElem(tag, false, !shouldUseFilterValue);
            self._showSuggestions(false);
            // Remove the tag (x) with a timeout, otherwise the suggestions will be hidden. This happens
            // because the mouseup event propagates to the document, and if the element has
            // been removed already, the event.target won't have the tagger div as its ancestor
            // and therefore it is assumed that the user has clicked outside of the tagger
            setTimeout(function(){
              tagRemover.remove();
              self.taggerInput.focus();
              // If the filter has a value we can use, move that value to the main
              // input and filter the suggestions
              if (shouldUseFilterValue) {
                self._updateInputAndFilter(self.taggerInput, self.taggerFilterInput.val());
              }
            }, 0);
          }
          else {
            // Remove the tag
            self._removeTagByElem(tag, false, true);
            tagRemover.remove();
            self.taggerInput.focus();
          }
        };

        // Bind event to the tag remover (x) to deal with mouse click and enter key
        tagRemover.bind({
          'mouseup': function (event) {
            if (event.which === 1) { // Left Mouse Click
              tagRemoveProcessing();
            }
            event.preventDefault();
          },
          'keyup': function (event) {
            if (event.which === 13) { // Enter key
              tagRemoveProcessing();
            }
          }
        });

        // Bind event to the whole tag to deal with backspaces, arrow keys
        tag.bind('keydown', function (event) {
          if (event.which === 8) { // Backspace
            self._removeTagByElem($(event.target), false, true);
            if (tagRemover) {
              tagRemover.remove();
            }
            event.preventDefault();
            self.taggerInput.focus();
          }
          if (event.which === 37 ) { // Left arrow
            // Shift focus to previous tab if there is one
            var prevTag = $(event.target).prev('.tag').get(0);
            if (prevTag) {
              prevTag.focus();
            }
          }
          if (event.which === 39 ) { // Right arrow
            // Shift focus to next tab if there is one, otherwise the input field
            var nextTag = $(event.target).next('.tag').get(0);
            if (nextTag) {
              nextTag.focus();
            }
            else {
              self.taggerInput.focus();
            }
          }
        });

        if (this.singleValue) {
          // In single select mode, with a single tag selected already
          // we should focus the first item in the suggstion list (which
          // will be the filter input)
          tag.bind('click', function (event) {
            self._showSuggestions(self.singleValue && self.tagCount === 1);
          });

          // Change the way it is displayed in single-value mode
          this.taggerInput.hide();
          tag.addClass('tag-single');

          // Remove ability to clear the selection if operating in mandatory mode
          if (!this.singleValue || !this.options.mandatorySelection) {
            tagRemover.addClass('removetag-single');
            tagRemover.attr("tabindex", this.tabIndex);
            tagRemover.insertBefore(this.taggerSuggestionsButton);
          }
        }
        else {
          tagRemover.appendTo(tag);
        }
      }
      else {
        tag = $('<div class="tag tag-readonly"></div>').prependTo(this.taggerWidget);
        tag.text($('<div/>').html(tagData.key).text());
        if (this.singleValue) {
          tag.addClass('tag-single');
        }
      }

      this.tagCount++;

      // Remove tag from tags object
      this.tagsByID[tagID].suggestable = false;

      // Mark this tag as being displayed
      this.tagsByID[tagID].displaying = true;

      // Fire onchange action
      if (this.canFireActions) {
        this._fireOnChangeAction();
      }
    },

    /**
     * Check to see if a tag has already been selected
     * @param {string} tagID - ID of the tag to check for
     * @returns {boolean} True if the tag is currently selected
     * @protected
     */
    _isAlreadyDisplayingTag: function (tagID) {
      if (this.tagsByID[tagID].displaying && this.tagsByID[tagID].displaying === true) {
        return true;
      }
      return false;
    },

    /**
     * Remove a tag, given a tags ID, to the widget and mark it as non-selected
     * in the underlying select elements option list
     * @param {HTMLFragment} tagElem - Div element of the tag clicked in the widget
     * @param {boolean} shouldHideMenu - should the menu be hidden?
     * @param {boolean} shouldClearInputs should the input fields be cleared?
     * @protected
     */
    _removeTagByElem: function (tagElem, shouldHideMenu, shouldClearInputs) {
      // Get ID of tag about to be removed
      var tagID = tagElem.data('tagid');
      // Remove tag div
      tagElem.remove();
      this.tagCount--;
      // Deselect from hidden select
      $('option[value="' + tagID + '"]', this.element).removeAttr("selected");

      // In single select mode, make sure no options are selected
      if (this.singleValue) {
        $(this.element).val([]);
      }

      // Add tag back into the suggestable list and mark is as no longer displayed if it's in the list of current tags
      if (this.tagsByID[tagID]) {
        // Add back into the selectable list
        this.tagsByID[tagID].suggestable = true;
        // Mark this tag as no longer being displayed
        this.tagsByID[tagID].displaying = false;
      }

      // Reset input
      this._selectionReset(shouldHideMenu, shouldClearInputs);

      // Show the input if it's in single-select mode
      if (this.singleValue) {
        this.taggerInput.show();
      }

      // Fire onchange action
      if (this.canFireActions) {
        this._fireOnChangeAction();
      }
    },

    /**
     * If there is any onchange function defined on the original element, run it
     * @protected
     */
    _fireOnChangeAction: function () {
      if (this.element[0].onchange) {
        this.element[0].onchange();
      }
    }
  });
})(jQuery);

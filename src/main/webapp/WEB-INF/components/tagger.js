/*
* jQuery UI Tagger
*
* @version v0.3.0 (10/2013)
*
* Copyright 2013, Fivium ltd.
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
   * @version 0.0.3
   * @license http://github.com/fivium/jquery-tagger/blob/master/LICENSE
   * @copyright Fivium ltd.
   * @author Nick Palmer
   */
  $.widget('ui.tagger', /** @lends jQuery.widget.ui.tagger */ {

    /**
     * Default options, can be overridden by passing in an object to the constructor with these properties
     * @property {array}    availableTags       - Array of JSON tag objects
     * @property {array}    preselectedTags     - Array of tag ID's that are selected in the element (helps performance)
     * @property {integer}  characterThreshold  - How many characters must be typed before searching
     * @property {boolean}  caseSensitive       - Case sensitive searching - defaults to false
     * @property {string}   placeholder         - Placeholder text for input area
     * @property {string}   baseURL             - Base URL used for images
     * @property {string}   imgDownArrow        - URL for down arrow image (after baseURL)
     * @property {string}   imgRemove           - URL for remove image (after baseURL)
     * @property {boolean}  sortedOutput        - Sort the suggestion lists by tag.sort
     * @property {boolean}  displayHierarchy    - Indent suggestions to show hierarchy
     * @property {integer}  indentMultiplier    - When indenting suggestions, how much to multiple tag.level by
     * @property {integer}  tabindexOffset      - Then creating items it can tab to, what the tabindex should initally be
     * @property {string}   noSuggestText       - Text to show when no suggestions can be found
     * @property {string}   emptyListText       - Text to show when no suggestions in the list
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
    , preselectedTags     : null
    , characterThreshold  : 1
    , caseSensitive       : false
    , placeholder         : null
    , baseURL             : ''
    , imgDownArrow        : '/img/dropdown.png'
    , imgRemove           : '/img/remove.png'
    , sortedOutput        : false
    , displayHierarchy    : false
    , indentMultiplier    : 1
    , tabindexOffset      : null
    , noSuggestText       : 'No suggestions found'
    , emptyListText       : 'No more suggestions'
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
            this._setTabindex(parseInt(this.element.attr('tabindex'), 10));
          }
          else {
            this._setTabindex(1);
          }
        }
        else {
          this._setTabindex(this.options.tabindexOffset);
        }

        // Check cardinality mode
        this.singleValue = !this.element.attr('multiple');

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
          this.taggerSuggestionsButton = $('<div class="droparrow hittarget"><img src="' + this.options.baseURL + this.options.imgDownArrow + '" /></div>').appendTo(this.taggerWidget);
          this.taggerInput = $('<input type="text" class="intxt"/>').appendTo(this.taggerWidget);
        }

        // Clearer div makes sure the widget div keeps its height
        $('<div class="clearer"></div>').appendTo(this.taggerWidget);

        if (!this.readonly) {
          // If not readonly, stub out an empty suggestion list
          this.taggerSuggestions = $('<div class="suggestions"></div>').appendTo(this.taggerWidget);
          this.taggerSuggestionsList = $('<ul></ul>').appendTo(this.taggerSuggestions);

          // Event listener to hide suggestions list if clicking outside this tagger widget
          $(document).mouseup(function (event) {
            if ($(event.target).parents(".tagger").get(0) !== self.taggerWidget.get(0)
                && $(event.target).get(0) !== self.taggerWidget.get(0)) {
              self.taggerSuggestions.hide();
            }
            else if (event.target === self.taggerWidget.get(0)) {
              self.taggerInput.focus();
            }
          });

          // Bind suggest list toggle to left clicking suggestion button
          this.taggerSuggestionsButton.bind('mouseup keyup', function (event) {
            if ((event.type === "mouseup" && event.which === 1) // left click
                || (event.type === "keyup" && (event.which === 13 || event.which === 32 || event.which === 40))) { // enter || space || down arrow
              self._setSuggestionListDimensions();
              self.taggerSuggestions.toggle();
              if (self.taggerSuggestions.is(":visible")) {
                self.taggerSuggestionsList.children('[tabindex]').first().focus();
              }

              // Load suggestions on first hit
              if (self.taggerSuggestionsList.children().length === 0) {
                if ($.map(self.tagsByID, function(n, i) { return i;}).length > 300) {
                  // If there are more than 300 items, show a loading item first as it could take a while
                  $('<li class="missing">Loading...</li>').appendTo(self.taggerSuggestionsList);
                  setTimeout(function(){
                      // Load all suggestions into the suggestions list
                      self._loadSuggestions(self.tagsByID, true);
                      // Set the flag to show it's not loaded filtered results
                      self.loadedFiltered = false;
                    }
                  , 300); // Fixed timeout of 300ms for now
                }
                else {
                  // If less than 300 items just load all suggestions into the suggestions list
                  self._loadSuggestions(self.tagsByID, true);
                  // Set the flag to show it's not loaded filtered results
                  self.loadedFiltered = false;
                }
              }

              event.preventDefault();
            }
          });

          // Add placeholder text to text input field
          if (this.options.placeholder !== null) {
            this.taggerInput.attr("placeholder", this.options.placeholder);
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
                      self._selectionReset();
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
                  default:
                    break;
                }
              }
            },
            keyup: function (event) {
              self._inputExpand(self.taggerInput);
              if (event.which !== 13 && event.which !== 40) { // key up not enter or down arrow
                if (self.taggerInput.val().length > (self.options.characterThreshold - 1)) {
                  // If text is longer than the threshold start filtering and showing the filtered results
                  self.filterTags();
                  self._showSuggestions();
                }
                else if (self.loadedFiltered) {
                  // If under the threshold and was previously filtered, reset the list
                  // Hide it
                  self.taggerSuggestions.hide();
                  // Reload in all suggestions
                  self._loadSuggestions(self.tagsByID, true);
                  // Clear the flag
                  this.loadedFiltered = false;
                }
              }
              else if (event.which === 40) { // Down Arrow
                self._showSuggestions();

                // Load suggestions on first hit
                if (self.taggerSuggestionsList.children().length === 0) {
                  if ($.map(self.tagsByID, function(n, i) { return i;}).length > 300) {
                    // If there are more than 300 items, show a loading item first as it could take a while
                    $('<li class="missing">Loading...</li>').appendTo(self.taggerSuggestionsList);
                    setTimeout(function(){
                        // Load all suggestions into the suggestions list
                        self._loadSuggestions(self.tagsByID, true);
                        // Set the flag to show it's not loaded filtered results
                        self.loadedFiltered = false;
                      }
                    , 300); // Fixed timeout of 300ms for now
                  }
                  else {
                  // If less than 300 items just load all suggestions into the suggestions list
                    self._loadSuggestions(self.tagsByID, true);
                    // Set the flag to show it's not loaded filtered results
                    self.loadedFiltered = false;
                  }
                }

                // Focus top item in suggestion list
                self.taggerSuggestionsList.children('[tabindex]').first().focus();

                event.preventDefault();
              }
            }
          });
        }

        // Let the available tags be accessed through a nicer name
        this.tagsByID = this.options.availableTags;
        
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

        if (!this.readonly) {
          // Set the tab indexes
          this._setWidgetTabIndexes();
        }

        this.canFireActions = true;
      }
      else {
        // Stub out console.log if not supported in browser
        if(typeof console === "object") {
          if(typeof console.log === "function") {
            console.log('Tagger widget only works on select elements');
          }
        }
      }
    },

    /**
     * Set the tab index offset for the widget
     * @param {integer} startingIndex - The starting offset for the widgets tab indexes
     * @protected
     */
    _setTabindex: function (startingIndex) {
      this.tabIndex = startingIndex;
    },

    /**
     * Increment the global tag index
     * @returns {integer} next tab index to use
     * @protected
     */
    _getNextWidgetTabIndex: function () {
      return ++this.tabIndex;
    },

    /**
     * Set tab index of input and droparrow after adding tags
     * @protected
     */
    _setWidgetTabIndexes: function () {
      // Set tabindexes of input and droparrow after adding tags
      this.taggerInput.attr('tabindex', this._getNextWidgetTabIndex());
      this.taggerSuggestionsButton.attr('tabindex', this._getNextWidgetTabIndex());
      // Reset tab index variable
      this._setTabindex(this.options.tabindexOffset);
    },

    /**
     * Filter the available tags by the input text and load suggestions into suggestion list
     * @protected
     */
    filterTags: function () {
      var searchString = this.taggerInput.val();
      var searchStringLowerCase = this.taggerInput.val().toLowerCase();
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
               || tag.hidden.indexOf(searchString) >= 0
               || $('<div/>').html(tag.suggestion).text().replace(/<.*?[^>]>/g,'').indexOf(searchString) >= 0) {
              filteredResults[tagID] = $.extend(true, {}, tag);
              filteredResults[tagID].suggestable = true;
            }
          }
          else {
            if (tag.key.toLowerCase().indexOf(searchStringLowerCase) >= 0
               || tag.hidden.toLowerCase().indexOf(searchStringLowerCase) >= 0
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
          if ((event.type === "mouseup" && event.which === 1) || (event.type === "keyup" && event.which === 13)) { // Click or enter
            // Handle suggestion adding
            self._addTagFromID(currentSelection.data('tagid'));
            self._selectionReset();
          }
          else if (event.type === "keydown" && (event.which === 38 || (event.which === 9 && event.shiftKey))) { // Up arrow / shift+tab (Move selection up and up into the input)
            // Override default browser tab control and allow arrow keys too
            var prevTarget = $(event.target).prevAll('li[tabindex]').first();
            if (prevTarget.is('li')) {
              prevTarget.focus();
            }
            else {
              self.taggerInput.focus();
            }
          }
          else if (event.type === "keydown" && (event.which === 40 || (event.which === 9 && !event.shiftKey))) { // Down arrow / tab (Move selection down, stop at the end)
            // Override default browser tab control and allow arrow keys too
            var nextTarget = $(event.target).nextAll('li[tabindex]').first();
            if (nextTarget.is('li')) {
              nextTarget.focus();
            }
          }
          else if (event.type === "keyup" && event.which === 36) { // Home key
            var prevTarget = $(event.target).prevAll('li[tabindex]').last();
            if (prevTarget.is('li')) {
              prevTarget.focus();
            }
          }
          else if (event.type === "keyup" && event.which === 35) { // End key
            var prevTarget = $(event.target).nextAll('li[tabindex]').last();
            if (prevTarget.is('li')) {
              prevTarget.focus();
            }
          }

          event.preventDefault();
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
        var idx = this._getNextWidgetTabIndex();
        for (var i = 0; i < suggestableTagArray.length; i++) {
          var tag = suggestableTags[suggestableTagArray[i][0]];
          if ((!this.options.displayHierarchy && !tag.suggestable) || tag.historical) {
            continue;
          }
          // Create and add the suggestion to the suggestion list
          var suggestion = $('<li></li>').attr("tabindex", idx).appendTo(this.taggerSuggestionsList);
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

          idx++;
        }
      }
      else {
        // Add message if filtering meant no items to suggest
        $('<li class="missing">' + this.options.noSuggestText + '</li>').appendTo(this.taggerSuggestionsList);
      }

      // Add message if nothing ended up in the list
      if (this.taggerSuggestionsList.children().length === 0) {
        $('<li class="missing">' + this.options.emptyListText + '</li>').appendTo(this.taggerSuggestionsList);
      }
    },

    /**
     * Set the dimensions of the suggestion list container
     * @protected
     */
    _setSuggestionListDimensions: function() {
      // Set width
      if (this.options.suggestMaxWidth === null && this.options.suggestWidth === null) {
        this.taggerSuggestions.width(this.taggerWidget.innerWidth());
      }
      else if (this.options.suggestWidth !== null) {
        this.taggerSuggestions.width(this.options.suggestWidth);
      }
      else if (this.options.suggestMaxWidth !== null) {
        this.taggerSuggestions.css('min-width', this.taggerWidget.innerWidth());
        this.taggerSuggestions.css('max-width', this.options.suggestMaxWidth);

        // Deal with quirks
        if (!jQuery.support.boxModel) {
          if (this.taggerSuggestions.width() < this.taggerWidget.innerWidth()) {
            this.taggerSuggestions.width(this.taggerWidget.innerWidth());
          }
          else if (this.taggerSuggestions.width() > this.options.suggestMaxWidth) {
            this.taggerSuggestions.width(this.options.suggestMaxWidth);
          }
        }
      }

      // Set height
      if (this.options.suggestMaxHeight !== null) {
        this.taggerSuggestions.css('max-height', this.options.suggestMaxHeight);

        // Deal with quirks
        if (!jQuery.support.boxModel) {
          if (this.taggerSuggestions.height() > this.options.suggestMaxHeight) {
            this.taggerSuggestions.height(this.options.suggestMaxHeight);
          }
        }
      }
    },

    /**
     * Show the suggestions list, making sure it's the correct size
     * @protected
     */
    _showSuggestions: function () {
      // Set width
      this._setSuggestionListDimensions();
      // Show list
      this.taggerSuggestions.show();
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
     * @protected
     */
    _selectionReset: function () {
      // Clear input
      this.taggerInput.val('');
      // Expand properly
      this._inputExpand(this.taggerInput);
      // Clear filtered suggestions
      this._loadSuggestions(this.tagsByID, true);
      // Set the flag to show it's not loaded filtered results
      this.loadedFiltered = false;
      // Focus input
      this.taggerInput.focus();
      // Hide suggestion list
      this.taggerSuggestions.hide();
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
          self._removeTagByElem($(this));
        });
        $('.removetag', this.taggerWidget).each(function () {
          $(this).remove();
        });
        this.canFireActions = tmpActionFireStatus;
      }

      if (!this.readonly) {
        // Select the option in the underlying select element
        $('option[value="'+tagID+'"]', this.element).attr("selected","selected");
        // Add the HTML to show the tag
        tag = $('<div class="tag" tabindex="' + this._getNextWidgetTabIndex() + '"></div>').insertBefore(this.taggerInput);
        tag.text(tagData.key);
        tag.data("tagid", tagID);
        var tagRemover = $('<span class="removetag hittarget"><img src="' + this.options.baseURL + this.options.imgRemove + '" /></span>');
        // Bind event to the tag remover to deal with mouse click
        tagRemover.bind('mouseup', function (event) {
          if (event.which === 1) { // Left Mouse Click
            self._removeTagByElem(tag);
            tagRemover.remove();
          }
        });
        // Bind event to the whole tag to deal with backspaces, arrow keys
        tag.bind('keydown', function (event) {
          if (event.which === 8) { // Backspace
            self._removeTagByElem($(event.target));
            if (tagRemover) {
              tagRemover.remove();
            }
            event.preventDefault();
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

        // Change the way it is displayed in single-value mode
        if (this.singleValue) {
          this.taggerInput.hide();
          tag.addClass('tag-single');
          
          // Remove ability to clear the selection if operating in mandatory mode
          if (!this.singleValue || !this.options.mandatorySelection) {
            tagRemover.addClass('removetag-single');
            tagRemover.insertAfter(tag);
          }
        }
        else {
          tagRemover.appendTo(tag);
        }

        // Update tab indexes
        this._setWidgetTabIndexes();
      }
      else {
        tag = $('<div class="tag tag-readonly"></div>').prependTo(this.taggerWidget);
        tag.text(tagData.key);
        if (this.singleValue) {
          tag.addClass('tag-single');
        }
      }

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
     * @protected
     */
    _removeTagByElem: function (tagElem) {
      // Get ID of tag about to be removed
      var tagID = tagElem.data('tagid');
      // Remove tag div
      tagElem.remove();
      // Deselect from hidden select
      $('option[value="'+tagID+'"]', this.element).removeAttr("selected");
      // In single select mode, make sure no options are selected 
      if (this.singleValue) {
        $(this.element).val([]);
      }
      // Add back into the selectable list
      this.tagsByID[tagID].suggestable = true;
      // Mark this tag as no longer being displayed
      this.tagsByID[tagID].displaying = false;
      // Reset input
      this._selectionReset();
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

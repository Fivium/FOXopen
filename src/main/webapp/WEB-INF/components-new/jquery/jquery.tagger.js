/*
 * jQuery UI Tagger
 *
 * @version v0.8.0 (05/2016)
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
   * @version 0.7.1
   * @license http://github.com/fivium/jquery-tagger/blob/master/LICENSE
   * @copyright Fivium ltd.
   * @author Nick Palmer
   */
  $.widget('ui.tagger', /** @lends jQuery.widget.ui.tagger */ {

    /**
     * Default options, can be overridden by passing in an object to the constructor with these properties
     * @property {Array}    availableTags       - Array of JSON tag objects
     * @property {Array}    ajaxURL             - URL to autocomplete webservice for updating available tags
     * @property {Array}    preselectedTags     - Array of tag ID's that are selected in the element (helps performance)
     * @property {number}   characterThreshold  - How many characters must be typed before searching
     * @property {number}   characterLimit      - How many characters can be entered into the input box
     * @property {number}   typingTimeThreshold - How many milliseconds to wait after the last keypress before filtering
     * @property {boolean}  caseSensitive       - Case sensitive searching - defaults to false
     * @property {string}   placeholder         - Placeholder text for input area
     * @property {string}   baseURL             - Base URL used for images
     * @property {string}   imgDownArrow        - URL for down arrow image (after baseURL)
     * @property {string}   imgRemove           - URL for remove image (after baseURL)
     * @property {string}   imgSearch           - URL for search image (after baseURL)
     * @property {boolean}  sortedOutput        - Sort the suggestion lists by tag.sort
     * @property {boolean}  displayHierarchy    - Indent suggestions to show hierarchy
     * @property {number}   indentMultiplier    - When indenting suggestions, how much to multiple tag.level by
     * @property {number}   tabindexOffset      - Then creating items it can tab to, what the tabindex should initially be
     * @property {string}   noSuggestText       - Text to show when no suggestions can be found
     * @property {string}   emptyListText       - Text to show when no suggestions in the list
     * @property {string}   searchTooltipText   - Text to show as tooltip for the ajax search icon
     * @property {string}   ajaxErrorFunction   - Function definition to use in the event of an AJAX request error, function(tagger, data)
     * @property {string}   loadingClass        - Class on an sibling to the select used to fill while the js loads the tagger
     * @property {number}   inputExpandExtra    - How many extra pixels to add on to the end of an input when expanding
     * @property {string}   fieldWidth          - Override width e.g. 20em
     * @property {string}   fieldHeight         - Override height e.g. 20em
     * @property {string}   suggestWidth        - Set a hard width for the suggestion list (overrides maxwidth) e.g. 50em
     * @property {string}   suggestMaxWidth     - Max width of the suggestion list (so it can be wider than the field) e.g. 50em
     * @property {string}   suggestMaxHeight    - Max height of the suggestion list e.g. 20em
     * @property {boolean}  mandatorySelection  - Make it mandatory that a value is chosen - defaults to false, no effect in multiselect mode
     * @property {boolean}  clearFilterOnBlur   - Clear the filter text if any was left when the field loses focus (stops users thinking typed in text will be sent)
     * @property {boolean}  freeTextInput       - Enable users to create options not defined in availableTags by hitting enter after typing text
     * @property {string}   freeTextPrefix      - Optional string to prefix all free text option values with (helpful to differentiate server-side)
     * @property {string}   freeTextMessage     - HTML string to show in the suggestions list containing the free text to hint that it can be added e.g. Add &lt;em&gt;%VALUE%&lt;/em&gt; to list
     * @property {string}   freeTextSuggest     - Allow free text values in the select to show up in the suggestions list
     */
    options: {
      availableTags       : null
    , ajaxURL             : null
    , preselectedTags     : null
    , characterThreshold  : 1
    , characterLimit      : null
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
    , clearFilterOnBlur   : false
    , freeTextInput       : false
    , freeTextPrefix      : null
    , freeTextMessage     : null
    , freeTextSuggest     : false
    },

    keyCodes: {
      BACKSPACE: 8
    , TAB: 9
    , ENTER: 13
    , ESC: 27
    , SPACE: 32
    , END: 35
    , HOME: 36
    , LEFT: 37
    , UP: 38
    , RIGHT: 39
    , DOWN: 40
    },
    mouseCodes: {
      LEFT: 1
    , MIDDLE: 2
    , RIGHT: 3
    },

    /**
     * Tagger widget constructor
     *
     * Based on the select element it is created on it reads information from it,
     * Creates new elements for the tagger widget, adds event listeners and deals
     * with pre-selected tags.
     */
    _create: function () {
      this.canFireActions = false;

      if (this.element.is('select')) {
        // Add a data attribute to the select so other code can reliably tell that it's been turned into a tagger
        this.element.data('isTagger', true);

        // Check readonly mode
        this.readonly = this.element.prop('readonly') || this.element.prop('disabled');

        // Set tabindexOffset
        if (this.options.tabindexOffset === null) {
          if (this.element.attr('tabindex')) {
            this.tabIndex = this.element.attr('tabindex');
          }
          else {
            this.tabIndex = '0';
          }
        }
        else {
          this.tabIndex = this.options.tabindexOffset;
        }

        // Check cardinality mode
        this.singleValue = !this.element.prop('multiple');

        // Initialise the tag counter
        this.tagCount = 0;

        // Hide select
        this.element.hide();

        // Remove any loading divs
        this.element.siblings(this.options.loadingClass).remove();

        var originalElementID = this.element.prop('id');
        this.taggerID = 'tagger' + originalElementID;
        this.suggestionsListID = 'suggestions' + originalElementID;

        // Construct tagger widget
        this.taggerWidget = $('<div>')
          .addClass('tagger')
          .prop('tabindex', 0)
          .attr('role', 'combobox')
          .attr('aria-expanded', 'false')
          .attr('aria-autocomplete', 'list')
          .attr('aria-owns', this.suggestionsListID)
          .insertAfter(this.element);

        if (this.element.attr('aria-label')) {
          this.taggerWidget.attr('aria-label', this.element.attr('aria-label'));
        }
        if ($('label[for=' + this.element.prop('id') + ']')) {
          this.taggerWidget.attr('aria-labelledby', $('label[for=' + this.element.prop('id') + ']').first().prop('id'));
        }

        if (this.element.attr('aria-required')) {
          this.taggerWidget.attr('aria-required', this.element.attr('aria-required'));
        }

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
          this.taggerInput = $('<input>')
            .attr('type', 'text')
            .attr('autocomplete', 'off')
            .addClass('intxt')
            .attr('role', 'textbox')
            .attr('aria-label', 'Autocomplete input box')
            .appendTo(this.taggerWidget);
          this.taggerButtonsPanel = $('<div>').addClass('tagger-buttons');
          this.taggerButtonsPanel.appendTo(this.taggerWidget);

          if (!this.options.ajaxURL) {
            this.taggerSuggestionsButton = $('<div>')
              .addClass('droparrow')
              .addClass('hittarget')
              .bind('mouseup keyup', $.proxy(this._handleSuggestionsButtonInteraction, this))
              .appendTo(this.taggerButtonsPanel);
            $('<img>')
              .attr('src', this.options.baseURL + this.options.imgDownArrow)
              .attr('alt', 'Toggle option display')
              .attr('role', 'presentation')
              .appendTo(this.taggerSuggestionsButton);
          }
          else {
            this.taggerSuggestionsButton = $('<div>')
              .addClass('search')
              .bind('mouseup keyup', $.proxy(this._handleSuggestionsButtonInteraction, this))
              .appendTo(this.taggerButtonsPanel);
            $('<img>')
              .attr('src', this.options.baseURL + this.options.imgSearch)
              .attr('alt', this.options.searchTooltipText)
              .attr('role', 'presentation')
              .appendTo(this.taggerSuggestionsButton);
          }

          this.taggerSuggestionsButton.attr("tabindex", this.tabIndex);

          // Add placeholder text to text input field
          if (this.options.placeholder !== null) {
            this.taggerInput.attr("placeholder", this.options.placeholder);
          }

          if (this.options.characterLimit !== null) {
            this.taggerInput.attr("maxlength", this.options.characterLimit);
          }

          // Set the tab index on the input field
          this.taggerInput.attr("tabindex", this.tabIndex);

          // Esc should hide the tagger suggestions globally
          this.taggerWidget.bind('keydown', $.proxy(function (event) {
            if (event.target && event.which === this.keyCodes.ESC) { // Esc
              this._hideSuggestions();

              // Select the widget itself again
              this._getWidgetFocusable().focus();
            }
          }, this));

          // Capture the keypress event for any child elements - redirect any chars to the current input field
          this.taggerWidget.bind('keypress', $.proxy(this._handleTaggerKeypressRedirect, this));
        }

        // Clearer div makes sure the widget div keeps its height
        $('<div>')
          .addClass('clearer')
          .appendTo(this.taggerWidget);

        if (!this.readonly) {
          // If not readonly, stub out an empty suggestion list
          this.taggerSuggestions = $('<div>')
            .addClass('suggestions')
            .appendTo(this.taggerWidget);

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

          this.taggerSuggestionsList = $('<ul>')
            .prop('id', this.suggestionsListID)
            .attr('role', 'listbox')
            .appendTo(this.taggerSuggestions);

          // Event listener to hide suggestions list if clicking outside this tagger widget
          // Using mousedown because IE11 reports the event.target for a mouseup as the HTML
          // root element rather than the original click target, mousedown seems to work
          // cross browser
          $(document.body).bind('mousedown keyup', $.proxy(this._handleDocumentInteraction, this));

          // Bind event to window to resize the suggestion list when the window's resized
          $(window).resize($.proxy(function() {
            this._setSuggestionListDimensions(this);
          }, this));

          // Expand the input field to fit its contents
          this._inputExpand(this.taggerInput);

          // Bind event to text input to expand input to fit contents and deal with key input
          this.taggerInput.bind('keydown keyup mouseup', $.proxy(this._handleFilterInputInteraction, this));

          // If we have a list filter then bind events to it
          if (this.taggerFilterInput) {
            this.taggerFilterInput.bind('keydown keyup mouseup', $.proxy(this._handleFilterInputInteraction, this));
          }

          // If the select was in focus already, make the tagger input focused
          if (this.element.is(':focus')) {
            this._focusWidget();
            this._getWidgetFocusable().focus();
          }
          // Capture focus on the underlying element and redirect that focus to the tagger
          this.element.focus($.proxy(function (e) {
            this._focusWidget();
            this._getWidgetFocusable().focus();
            e.preventDefault();
          }, this));
          // For some reason the jQuery focus overload doesn't fully work so we need both methods?
          this.element.get(0).focus = $.proxy(function () {
            this._focusWidget();
            this._getWidgetFocusable().focus();
          }, this);
          // Add a focus handler to any labels that are for the underlying select
          $('label[for=' + this.element.prop('id') + ']').bind('mouseup', $.proxy(function () {
            this._focusWidget();
            this._getWidgetFocusable().focus();
          }, this));
        }

        // Let the available tags be accessed through a nicer name
        if (this.options.availableTags) {
          this.tagsByID = this.options.availableTags;
        }
        // Convert options to JS objects if no JSON is supplied
        else {
          this.tagsByID = {};
          this.element.children("option").each($.proxy(function (index, element) {
            this.tagsByID[$(element).val()] = {
              id: $(element).val(),
              key: $(element).text(),
              hidden: '',
              level: 0,
              suggestable: true,
              historical: false,
              disabled: $(element).prop('disabled'),
              sort: index,
              freetext: (this.options.freeTextInput && $(element).val().startsWith(this.options.freeTextPrefix))};
          }, this));
        }

        var preselectedTags = this.options.preselectedTags;
        if (this.singleValue && this.options.mandatorySelection && preselectedTags === null) {
          preselectedTags = [this.element.children()[0].value];
        }

        // Deal with already selected options
        if (preselectedTags === null) {
          this.element.children("option:selected").each($.proxy(function (index, element) {
            // Set any selected options that aren't in the availableTags as historical entries so they can be displayed and removed but not added
            if (!this.tagsByID[$(element).val()]) {
              this.tagsByID[$(element).val()] = {
                id: $(element).val(),
                key: $(element).text(),
                suggestion: $(element).text(),
                hidden: '',
                level: 0,
                suggestable: false,
                historical: true,
                disabled: $(element).prop('disabled'),
                freetext: (this.options.freeTextInput && $(element).val().startsWith(this.options.freeTextPrefix))};
            }
            // Add tags for any selected options
            this._addTagFromID($(element).val());
          }, this));
        }
        else {
          var preselectedTag = null;
          for (var i = 0; i < preselectedTags.length; i++) {
            preselectedTag = preselectedTags[i];
            // Set any selected options that aren't in the availableTags as historical entries so they can be displayed and removed but not added
            if (!this.tagsByID[preselectedTag]) {
              this.tagsByID[preselectedTag] = {
                id: preselectedTag,
                key: $('option[value="'+preselectedTag+'"]', this.element).first().text(),
                suggestion: '',
                hidden: '',
                level: 0,
                suggestable: false,
                historical: true,
                disabled: $('option[value="'+preselectedTag+'"]', this.element).first().prop('disabled'),
                freetext: (this.options.freeTextInput && preselectedTag.startsWith(this.options.freeTextPrefix))};
            }
            // Add tags for any selected options
            this._addTagFromID(preselectedTag);
          }
        }
        this.canFireActions = true;
      }
      else {
        throw 'Tagger widget only works on select elements';
      }
    },

    /**
     * Handle keydown, keyup and mosueup events on filtering input boxes
     * @param event KeyDown, KeyUp and MosueUp Event
     * @private
     */
    _handleFilterInputInteraction: function(event) {
      var targetInput = $(event.target);
      var isMainInput = targetInput.get(0) === this.taggerInput.get(0);
      switch (event.type) {
        case "keydown":
          // Expand the input field to fit its contents
          if (isMainInput) {
            this._inputExpand(this.taggerInput);
          }

          if (event.target) {
            switch (event.which) {
              case this.keyCodes.ENTER: // Enter key
                // If they hit enter with just one item in the suggestion list, add it, otherwise focus the top item
                if (this.taggerSuggestionsList.children('[suggestion=tag]').length === 1) {
                  this._addTagFromID(this.taggerSuggestionsList.children('[suggestion=tag]').first().data('tagid'));
                  this._selectionReset(true, true);
                }
                else if (this.taggerSuggestionsList.children('[suggestion=tag]').length === 0 && this.options.freeTextInput) {
                  this._addFreeText(targetInput.val());
                  this._selectionReset(true, true);
                }
                else {
                  this.taggerSuggestionsList.children('[tabindex]').first().focus();
                }
                event.preventDefault();
                break;
              case this.keyCodes.BACKSPACE: // Backspace
                if (isMainInput) {
                  if (targetInput.val().length < 1) {
                    // If there is nothing in the input, change focus to the last tag
                    var removeTag = $('.tag', this.taggerWidget).last();
                    // Move focus to last tag if there is one
                    if (removeTag.length > 0) {
                      removeTag.focus();
                    }
                    event.preventDefault();
                  }
                  else if (targetInput.val().length <= this.options.characterThreshold) {
                    // If they're backspacing the last character that puts them over the filter threshold hide the suggestions
                      this._selectionReset(true, false);
                  }
                }
                else {
                  if (targetInput.val().length <= this.options.characterThreshold && this.loadedFiltered) {
                    if (this.singleValue && this.taggerFilterInput) {
                      // In single select mode we don't want to hide the filter input, just the suggestions
                      this._selectionReset(false, false);
                    }
                    else {
                      // Reset selection
                      this._selectionReset(true, false);
                      // Focus the drop arrow
                      this.taggerSuggestionsButton.focus();
                    }
                    //event.preventDefault();
                  }
                }
                break;
              case this.keyCodes.ESC: // Esc
                this._hideSuggestions();

                // Select the widget itself again
                this._getWidgetFocusable().focus();
                event.preventDefault();
                break;
              default:
                break;
            }
          }
          break;
        case "keyup":
          // Expand the input field to fit its contents
          if (isMainInput) {
            this._inputExpand(this.taggerInput);
          }

          if (event.which !== this.keyCodes.ENTER && event.which !== this.keyCodes.DOWN && event.which !== this.keyCodes.ESC) { // key up not enter or down arrow or esc key
            if (targetInput.val().length >= this.options.characterThreshold) {
              // Filter suggestions when they're over the threshold
              this._filterSuggestions(targetInput.val(), false);
            }
          }
          else if (event.which === this.keyCodes.DOWN) { // Down Arrow
            if (isMainInput) {
              if (!this.options.ajaxURL || this.taggerSuggestions.is(":visible")) {
                this._showSuggestions(true);
              }
            }
            else {
              // Focus top item in suggestion list
              this.taggerSuggestionsList.children('[tabindex]').first().focus();
              event.preventDefault();
            }
          }
          break;
        case "mouseup":
          // In single select mode, with a single tag selected already
          // we should focus the first item in the suggestion list (which
          // will be the filter input)
          this._showSuggestions(this.singleValue && this.tagCount === 1);
          break;
        default:
          throw 'Cannot handle interaction of this type on the filter input: ' + event.type + ' - ' + event.target;
      }
    },

    /**
     * Handle mouse and keyup events on the suggestions button (down arrow)
     *
     * @param event MouseUp or KeyUp event
     * @private
     */
    _handleSuggestionsButtonInteraction: function (event) {
      if ((event.type === "mouseup" && event.which === this.mouseCodes.LEFT) // left click
        || (event.type === "keyup" && (event.which === this.keyCodes.ENTER || event.which === this.keyCodes.SPACE || event.which === this.keyCodes.DOWN))) { // enter || space || down arrow
        if (this.options.ajaxURL) {
          this._focusWidget();

          // Just redirect focus in ajax mode
          this.taggerWidget.find("input[tabindex]:visible").first().focus();
        }
        else {
          // If the suggestion list is visible already, then toggle it off
          if (this.taggerSuggestions.is(":visible")) {
            this._hideSuggestions();
          }
          // otherwise show it
          else {
            this._showSuggestions(true);
          }
        }
        event.preventDefault();
      }
    },

    /**
     * When keypress events fire on the tagger widget redirect them to the filter input
     *
     * @param event KeyPress event
     * @private
     */
    _handleTaggerKeypressRedirect: function (event) {
      if (event.which !== 0 && event.charCode !== 0  && !event.ctrlKey && !event.metaKey && !event.altKey) {
        // If the keypress came from the main input or the filter, ignore this event or we'll potentially
        // just get in the way of the character being inserted and it'll be put at the end, instead of wherever
        // typed
        if (event.target === this.taggerInput.get(0) || (this.taggerFilterInput && event.target === this.taggerFilterInput.get(0))) {
          return;
        }
        this._appendCharAndFilter(event);
        event.preventDefault();
      }
    },

    /**
     * Hide suggestions list if clicking outside this tagger widget
     * (Using mousedown because IE11 reports the event.target for a mouseup as the HTML
     *  root element rather than the original click target, mousedown seems to work
     *  cross browser)
     * Also handling keyup events so that it can lose focus when tabbing away from the widget.
     * @param event MouseDown or KeyUp event
     * @private
     */
    _handleDocumentInteraction: function (event) {
      var selfTaggerWidget = this.taggerWidget.get(0);
      if (event.type === "mousedown") {
        if ($(event.target).parents(".tagger").get(0) !== selfTaggerWidget && event.target !== selfTaggerWidget) {
          // If clicking something which is not in this tagger widget we've effectively lost focus
          this._blurWidget();
        }
        else if (event.target === selfTaggerWidget) {
          this._focusWidget();

          // If clicking through to the parent div, focus the first focusable item
          if (!this.singleValue || this.tagCount === 0) {
            this.taggerWidget.find("input[tabindex]:visible").first().focus();
            event.preventDefault();
          }

          // In single select mode, with a single tag selected already
          // we should focus the first item in the suggestion list (which
          // will be the filter input).
          // NB: Using setTimeout because trying to do this immediately causes
          // the focus to fail, presumably because the corresponding mouseup triggers
          // focus elsewhere.
          setTimeout($.proxy(function () {
            this._showSuggestions(this.singleValue && this.tagCount === 1);
          }, this), 0);
        }
      }
      else if (event.type === "keyup") {
        if (event.which === this.keyCodes.TAB) {
          if ($(event.target).parents(".tagger").get(0) !== selfTaggerWidget) {
            this._blurWidget();
          }
          else if ($(event.target).parents(".tagger").get(0) === selfTaggerWidget) {
            this._focusWidget();
          }
        }
      }
    },

    /**
     * Apply focus to the widget
     *
     * @private
     */
    _focusWidget: function() {
      this.taggerWidget.addClass('focus');
    },

    /**
     * Blur action for the widget
     *
     * @private
     */
    _blurWidget: function() {
      this.taggerWidget.removeClass('focus');

      this._hideSuggestions();

      // If we're losing focus from the tagger optionally clear any left over filter text
      if (this.options.clearFilterOnBlur && this.taggerInput.val().length > 0) {
        this.taggerInput.addClass('filterCleared');
        setTimeout($.proxy(function () {
          this.taggerInput.removeClass('filterCleared');

          // Clear input
          this.taggerInput.val('');
          if (this.taggerFilterInput) {
            this.taggerFilterInput.val('');
          }

          // Call this so that the input is the right size for the placeholder text
          this._inputExpand(this.taggerInput);

          // Clear filtered suggestions
          this._loadSuggestions(this.tagsByID, true);
          // Set the flag to show it's not loaded filtered results
          this.loadedFiltered = false;
        }, this), 250);
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
     * @param {string} className Extra classes to add to the message item
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
      $('<li>')
        .attr('role', 'option')
        .attr('aria-selected', 'false')
        .addClass('extra')
        .addClass('message')
        .addClass(className)
        .text(msg)
        .appendTo(this.taggerSuggestionsList);
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
      else if (this.taggerInput && this.taggerInput.is(":visible")) {
        return this.taggerInput;
      }
      else {
        return this.taggerWidget;
      }
    },

    /**
     * Return the main tagger input, if it's visible, or the widget itself if the input is not visible (e.g. an item has
     * been selected)
     * @returns jQuery wrapped Element
     * @protected
     */
    _getWidgetFocusable: function() {
      if (this.taggerInput && this.taggerInput.is(":visible")) {
        return this.taggerInput;
      }
      else {
        return this.taggerWidget;
      }
    },

    /**
     * Updates the input or filter input and filters results. Also places focus in the input
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
        throw "Unhandled event type passed to _appendCharAndFilter(), expected keypress): " + event.type;
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
      // Clear out suggestion list
      this.taggerSuggestionsList.children().remove();

      // Load suggestions if there are some, or a message if not
      var suggestableTagArray = $.map(suggestableTags, function(n, i) { return [[i, n.sort]];});

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
        if ((!tag.suggestable && !this.options.displayHierarchy) || tag.historical || !(tag.key && tag.id) || (!this.options.freeTextSuggest && tag.freetext)) {
          continue;
        }
        // Create and add the suggestion to the suggestion list
        this._createSuggestionsItem(tag, allowIndent);
      }

      // When free text mode is on let users click this item to add whatever they typed to the selected tags
      if (this.options.freeTextInput && this._getVisibleInput().val().length > 0) {
        var message;
        if (this.options.freeTextMessage) {
          message = this.options.freeTextMessage.replace(/%VALUE%/g, $("<div>").text($.trim(this._getVisibleInput().val())).html());
        }
        else {
          message = this._getVisibleInput().val();
        }

        $('<li>')
          .attr('role', 'option')
          .attr('aria-selected', 'false')
          .addClass('extra')
          .addClass('addfreetext')
          .attr("tabindex", this.tabIndex)
          .html(message)
          .data("freetext", this._getVisibleInput().val())
          .bind('mouseup keyup keydown', $.proxy(this._handleSuggestionItemInteraction, this))
          .bind('mouseleave mouseenter blur focus', $.proxy(this._handleSuggestionItemFocus, this))
          .appendTo(this.taggerSuggestionsList);
      }

      // Add message if filtering meant no items to suggest and the noSuggestText option is not empty and the user has actually typed something
      if (suggestableTagArray.length === 0) {
        if (this.options.noSuggestText.length > 0 && this._getVisibleInput().val().length > 0) {
          $('<li>')
            .attr('role', 'option')
            .attr('aria-selected', 'false')
            .addClass('extra')
            .addClass('missing')
            .text(this.options.noSuggestText)
            .appendTo(this.taggerSuggestionsList);
        }
      }
      else if (this.taggerSuggestionsList.children().length === 0) {
        // Add message if nothing ended up in the list (e.g. all selectable items selected)
        $('<li>')
          .attr('role', 'option')
          .attr('aria-selected', 'false')
          .addClass('extra')
          .addClass('missing')
          .text(this.options.emptyListText)
          .appendTo(this.taggerSuggestionsList);
      }

      if (suggestableTags.limited) {
        $('<li>')
          .attr('role', 'option')
          .attr('aria-selected', 'false')
          .addClass('extra')
          .addClass('limited')
          .text(this.options.limitedText)
          .appendTo(this.taggerSuggestionsList);
      }
    },

    /**
     * Create the suggestion
     * @param {object} tag - Tag object
     * @param {boolean} allowIndent - Allow indenting of suggestion lists if true
     * @private
     */
    _createSuggestionsItem: function(tag, allowIndent) {
      // Create and add the suggestion to the suggestion list
      var suggestion = $('<li>')
        .attr("suggestion", "tag")
        .attr('role', 'option')
        .attr('aria-selected', 'false')
        .appendTo(this.taggerSuggestionsList);

      if (tag.suggestion && tag.suggestion !== null && tag.suggestion !== '') {
        suggestion.html($('<div/>').html(tag.suggestion).text());
      }
      else {
        suggestion.text(tag.key);
      }

      if (!tag.disabled) {
        suggestion.attr("tabindex", this.tabIndex);

        // Bind actions to the suggestion
        suggestion.bind('mouseup keyup keydown', $.proxy(this._handleSuggestionItemInteraction, this));
        suggestion.bind('mouseleave mouseenter blur focus', $.proxy(this._handleSuggestionItemFocus, this));
      }
      else {
        suggestion.addClass('extra');
        suggestion.addClass('disabled');
      }

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
          suggestion.addClass('extra');
          suggestion.addClass('disabled');
          suggestion.unbind();
          suggestion.removeAttr('tabindex');
        }
      }
    },

    /**
     * Function to bind to suggestion list elements
     * @param event
     */
    _handleSuggestionItemInteraction: function suggestionBind(event) {
      if (event.type !== "mouseup" && event.type !== "keyup" && event.type !== "keydown") {
        throw "Unhandled event type passed to _handleSuggestionItemInteraction(), expected mouseup, keyup or keydown): " + event.type;
      }

      var prevTargets = $(event.target).prevAll('li[tabindex]');
      var nextTargets = $(event.target).nextAll('li[tabindex]');

      if (  (event.type === "mouseup" && event.which === this.mouseCodes.LEFT)
        ||  (event.type === "keydown" && event.which === this.keyCodes.ENTER)) { // Click or enter
        // Handle suggestion adding
        var suggestionItem = $(event.target).closest('li');
        if (suggestionItem.data('tagid') && !suggestionItem.data('freetext')) {
          this._addTagFromID(suggestionItem.data('tagid'));
          this._selectionReset(true, true);
        }
        else if (suggestionItem.data('freetext') && !suggestionItem.data('tagid')) {
          this._addFreeText(suggestionItem.data('freetext'));
          this._selectionReset(true, true);
        }
        else {
          throw "Suggestion has both freetext and a tag id?";
        }
        event.preventDefault();
      }
      else if (event.type === "keydown" && (event.which === this.keyCodes.UP || (event.which === this.keyCodes.TAB && event.shiftKey))) { // Up arrow / shift+tab (Move selection up and up into the input)
        // Override default browser tab control and allow arrow keys too
        if (prevTargets.first().is('li')) {
          prevTargets.first().focus();
        }
        else if (this.taggerFilterInput && this.taggerFilterInput.is(":visible")) {
          this.taggerFilterInput.focus();
        }
        else {
          this.taggerInput.focus();
        }
        event.preventDefault();
      }
      else if (event.type === "keydown" && (event.which === this.keyCodes.DOWN || (event.which === this.keyCodes.TAB && !event.shiftKey))) { // Down arrow / tab (Move selection down, stop at the end)
        // Override default browser tab control and allow arrow keys too
        if (nextTargets.first().is('li')) {
          nextTargets.first().focus();
          event.preventDefault();
        }
      }
      else if (event.type === "keyup" && event.which === this.keyCodes.HOME) { // Home key
        if (prevTargets.last().is('li')) {
          prevTargets.last().focus();
          event.preventDefault();
        }
      }
      else if (event.type === "keyup" && event.which === this.keyCodes.END) { // End key
        if (nextTargets.last().is('li')) {
          nextTargets.last().focus();
          event.preventDefault();
        }
      }
      else if (event.type === "keydown" && event.which === this.keyCodes.BACKSPACE) { // Backspace
        this._removeLastCharAndFilter(event);
        event.preventDefault();
      }
    },

    /**
     * Deal with setting focus properly and displaying the focus for IE6
     * @param event
     * @private
     */
    _handleSuggestionItemFocus: function(event) {
      if (event.type === "focus") {
        $(event.target).addClass('focus');
      }
      else if (event.type === "blur") {
        $(event.target).removeClass('focus');
      }
      else if (event.type === "mouseenter") {
        $(event.target).addClass('focus');
        $(event.target).focus();
      }
      else if (event.type === "mouseleave") {
        $(event.target).removeClass('focus');
        $(event.target).blur();
        //this._getWidgetFocusable().focus();
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
          this._hideSuggestions();
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
      this._focusWidget();

      // Set width
      this._setSuggestionListDimensions(this);

      // Show the container
      this.taggerSuggestions.show();

      // Mark the aria expanded attr to true
      this.taggerInput.attr('aria-expanded', 'true');

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
      };

      // Load suggestions on first hit
      if (this.taggerSuggestionsList.children().length === 0) {
        // If there are more than 300 items, show a loading item first as it could take a while
        if ($.map(this.tagsByID, function(n, i) { return i;}).length > 300) {
          $('<li>')
            .attr('role', 'option')
            .attr('aria-selected', 'false')
            .addClass('extra')
            .addClass('missing')
            .text('Loading...')
            .appendTo(this.taggerSuggestionsList);
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

    _hideSuggestions: function() {
      // Hide the container
      this.taggerSuggestions.hide();

      // Mark the aria expanded attr to false
      this.taggerInput.attr('aria-expanded', 'false');
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
      this._getWidgetFocusable().focus();
      // Hide suggestion list
      if (shouldHideMenu) {
        this._hideSuggestions();
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
        if ($('option[value="' + tagID.replace(/"/g, '\\"') + '"]', this.element).length > 0) {
          $('option[value="' + tagID.replace(/"/g, '\\"') + '"]', this.element).prop("selected", true);
        }
        else {
          $('<option>')
            .prop("selected", true)
            .val(tagID)
            .text($('<div>').html(tagData.key).text())
            .appendTo(this.element);
        }
        // Add the HTML to show the tag
        tag = $('<div>')
          .addClass('tag')
          .attr("tabindex", this.tabIndex)
          .text($('<div/>').html(tagData.key).text())
          .data("tagid", tagID)
          .insertBefore(this.taggerInput);

        if (tagData.freetext) {
          tag.addClass('freetext');
        }

        var tagRemover = $('<span class="removetag hittarget" role="presentation"><img src="' + this.options.baseURL + this.options.imgRemove + '" alt="Deselect tag" /></span>');

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
        tagRemover.bind('mouseup keyup', $.proxy(function(event) {
          switch (event.type) {
            case "mouseup":
              if (event.which === this.mouseCodes.LEFT) { // Left Mouse Click
                tagRemoveProcessing();
              }
              event.preventDefault();
              break;
            case "keyup":
              if (event.which === this.keyCodes.ENTER) { // Enter key
                tagRemoveProcessing();
              }
              break;
          }
        }, this));

        // Bind event to the whole tag to deal with backspaces, arrow keys
        tag.bind('keydown', $.proxy(function (event) {
          if (event.which === this.keyCodes.BACKSPACE) { // Backspace
            this._removeTagByElem($(event.target), false, true);
            if (tagRemover) {
              tagRemover.remove();
            }
            event.preventDefault();
            this.taggerInput.focus();
          }
          if (event.which === this.keyCodes.LEFT) { // Left arrow
            // Shift focus to previous tab if there is one
            var prevTag = $(event.target).prev('.tag').get(0);
            if (prevTag) {
              prevTag.focus();
            }
          }
          if (event.which === this.keyCodes.RIGHT) { // Right arrow
            // Shift focus to next tab if there is one, otherwise the input field
            var nextTag = $(event.target).next('.tag').get(0);
            if (nextTag) {
              nextTag.focus();
            }
            else {
              this.taggerInput.focus();
            }
          }
        }, this));

        if (this.singleValue) {
          // In single select mode, with a single tag selected already
          // we should focus the first item in the suggestion list (which
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
     * Add a tag for given free text not specified in the available tags list
     * @param {string} freeTextValue - New text value to add an option for
     * @protected
     */
    _addFreeText: function(freeTextValue) {
      freeTextValue = $("<div>").text($.trim(freeTextValue)).html();

      // Stub in tag JIT
      var newTagID = (this.options.freeTextPrefix ? this.options.freeTextPrefix : '') + freeTextValue;

      this.tagsByID[newTagID] = {
        id: newTagID,
        key: freeTextValue,
        hidden: '',
        level: 0,
        suggestable: true,
        historical: false,
        disabled: false,
        sort: -1,
        freetext: true};

      this._addTagFromID(newTagID);

      delete this.tagsByID[newTagID];
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
     * @param {Object} tagElem - Div element of the tag clicked in the widget
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
      $('option[value="' + tagID + '"]', this.element).prop("selected", false);

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
        // Put the call to onchange in a setTimeout to stop IE11 hard crashing due to internal IE11 bug
        setTimeout($.proxy(this.element[0].onchange, this.element[0]), 0);
      }
    }
  });
})(jQuery);

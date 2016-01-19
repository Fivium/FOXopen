/**
 * Controls for option widgets (selects, tickboxes, radios, etc)
 */
var FOXoptions = {

  /**
   * Records the initial values for option widgets in data attributes.
   */
  registerInitialValues: function() {
    //Record initial values of option widgets
    $('select, input[type="radio"], input[type="checkbox"]').each(function(i, e) {
      var value;
      var $element = $(e);
      if ($element.is('select')) {
        value = $element.val();
      }
      else {
        value = $element.prop('checked');
      }

      $element.data('fox-initial-value', value);
    });
  },

  /**
   * Resets the given element to its original value recorded in registerInitialValues.
   * @param resetTarget HTMLElement - a radio, tickbox or selector element.
   */
  resetToInitialValue: function(resetTarget) {
    var $target = $(resetTarget);
    if ($target.is('select')) {
      //If the element is a select, reset the value (this may be an array for multi selects)
      $target.val($target.data('fox-initial-value'));
    }
    else if ($target.is('input')) {
      if ($target.attr('type') === 'radio') {
        //Changing one radio will usually change another indirectly so we must reset them all
        $('input[name="' + $target.attr('name') + '"]').each(function (i, e) {
          $(e).prop('checked', $(e).data('fox-initial-value'));
        });
      }
      else if ($target.attr('type') === 'checkbox') {
        //For checkboxes, we only need to reset the current element
        $target.prop('checked', $target.data('fox-initial-value'));
      }
    }
  }
};

$(document).ready(function() { FOXoptions.registerInitialValues(); });

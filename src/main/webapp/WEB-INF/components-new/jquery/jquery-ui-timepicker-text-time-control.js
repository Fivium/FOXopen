$.timepicker.textTimeControl = {
  create: function(tp_inst, obj, unit, val, min, max, step){

    if ((val+"").length === 1) {
      val = '0'+val;
    }

    var input = '<input value="'+val+'" style="width:50%"/>';

    $(input).appendTo(obj).change(function (e) {
      var $t = obj.children('input');

      if ($t.val().length === 0) {
        $t.val('00');
      } else if ($t.val().length === 1) {
        $t.val('0'+$t.val())
      }

      tp_inst._onTimeChange();
      tp_inst._onSelectHandler();
    }).keypress(function (e) {
      //http://stackoverflow.com/questions/891696/jquery-what-is-the-best-way-to-restrict-number-only-input-for-textboxes-all
      // Backspace, tab, enter, end, home, left, right
      // We don't support the del key in Opera because del == . == 46.
      var controlKeys = [8, 9, 13, 35, 36, 37, 39];
      // IE doesn't support indexOf
      var isControlKey = controlKeys.join(",").match(new RegExp(event.which));
      // Some browsers just don't raise events for control keys. Easy.
      // e.g. Safari backspace.
      if (!event.which || // Control keys in most browsers. e.g. Firefox tab is 0
        (49 <= event.which && event.which <= 57) || // Always 1 through 9
        (48 == event.which && $(this).attr("value")) || // No 0 first digit
        isControlKey) { // Opera assigns values for control keys.
        return;
      } else {
        event.preventDefault();
      }
    });

    return obj;
  },
  options: function(tp_inst, obj, unit, opts, val){
    return null;
  },
  value: function(tp_inst, obj, unit, val){
    var $t = obj.children('input');
    if (val !== undefined) {
      return $t.val(val);
    }
    return $t.val();
  }
};

var FOXtabs = {
  switchTab: function(pTabGroupKey, pTabKey) {
    $("div[data-tab-group='" + pTabGroupKey + "']").hide();
    $("div[data-tab-group='" + pTabGroupKey + "'][data-tab-key='" + pTabKey + "']").show();

    $("ul[data-tab-group='" + pTabGroupKey + "'] > li").removeClass("current-tab");
    $("ul[data-tab-group='" + pTabGroupKey + "'] > li[data-tab-key='" + pTabKey + "']").addClass("current-tab");

    $("input[data-tab-group='" + pTabGroupKey + "']").val(pTabKey);
  }
};
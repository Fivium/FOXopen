var FOXtabs = {
  switchTab: function(pTabGroupKey, pTabKey) {
    // Hide all tab content divs and just show the one with the selected key
    $("div[data-tab-group='" + pTabGroupKey + "']").hide();
    $("div[data-tab-group='" + pTabGroupKey + "'][data-tab-key='" + pTabKey + "']").show();

    // Remove the accessible hidden attribute from all tab content divs and re-apply it to the one with the selected key
    $("div[data-tab-group='" + pTabGroupKey + "']").attr("aria-hidden", "true");
    $("div[data-tab-group='" + pTabGroupKey + "'][data-tab-key='" + pTabKey + "']").attr("aria-hidden", "false");

    // Remove the current-tab class from all tab links and re-apply it to the one with the selected key
    $("ul[data-tab-group='" + pTabGroupKey + "'] > li").removeClass("current-tab");
    $("ul[data-tab-group='" + pTabGroupKey + "'] > li[data-tab-key='" + pTabKey + "']").addClass("current-tab");

    // Remove the accessible selected attribute from all tab links and re-apply it to the one with the selected key
    $("ul[data-tab-group='" + pTabGroupKey + "'] > li").attr("aria-selected", "false");
    $("ul[data-tab-group='" + pTabGroupKey + "'] > li[data-tab-key='" + pTabKey + "']").attr("aria-selected", "true");

    // Set the hidden input field to the selected key
    $("input[data-tab-group='" + pTabGroupKey + "']").val(pTabKey);
  }
};
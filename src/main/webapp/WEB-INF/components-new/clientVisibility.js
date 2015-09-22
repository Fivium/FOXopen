var gClientVisibilityConfig;

//For IE6/7/8 - debug fix
//if (!window.console) console = {log: function() {}};

/*
 * Note the pattern of creating functions within other functions - this is done so closures are properly set when the
 * functions are created.
 */

/**
 * Registers the configuration object for this page.
 * @param pConfig Rule configuration object.
 */
function registerClientVisibility(pConfig){
  gClientVisibilityConfig = pConfig;
}

/**
 * Creates a function which always returns a boolean.
 * @param pVal boolean to always return.
 */
function createFixedValueFunction(pVal){
  return function() {
    return pVal;
  }
}

/**
 * Creates a function which returns true if the object with pId has a value corresponding to pValue. If the object
 * is a multi selector this returns true if one of the selected values matches.
 * @param pId Id of element to target.
 * @param pVal Value to test for.
 * @param pInvert If true, the final result is inverted.

 */
function createEqualsFunction(pId, pVal, pInvert){
  return function() {
    var lVal = $("#" + pId).val();
    var lResult;
    if(lVal == null){
      //Sometimes selectors have no selection
      lResult = false;
    }
    else if(typeof lVal == "string"){
      //Single selector
      lResult = lVal == pVal;
    }
    else if(typeof lVal == "object") {
      //Multiselector - true if at least one selected value matches
      lResult = false; //If no match found
      for(var i=0; i < lVal.length; i++){
        if(lVal[i] == pVal){
          lResult = true;
          break;
        }
      }
    }
    else {
      throw "Unsupported value type";
    }

    //Invert the logic if needed
    return pInvert ? !lResult : lResult;
  }
}

/**
 * Gets the jQuery selector string for an input widget. If pVal is not null the value is used as an additional criterion.
 * @param pName HTML name of the tickbox widget (note cardinality: a multi-selector may have many tickboxes of the same name).
 * @param pVal HTML value of the tickbox. Leave null if the tickbox cardinality is one.
 */
function jQueryInputSelector(pName, pVal){
  return 'input[name="' + pName + '"]' + (pVal != null ? '[value="' + pVal + '"]' : '');
}

/**
 * Creates a function which returns boolean true if the input corresponding to the given type/id/value tuple is checked.
 * If pInvert is true, the function will return true if the input is NOT checked.
 */
function createInputCheckedFunction(pId, pVal, pInvert){
  return function() {
    var lChecked = $(jQueryInputSelector(pId, pVal)).is(':checked');
    return pInvert ? !lChecked : lChecked;
  }
}

/**
 * Creates a function which iterates over the functions in the given list and returns true only if every function in
 * the list returns true.
 * @param pFunctionList List of nested functions to evaluate.
 */
function createAndFunction(pFunctionList){
  return function() {
    for(var i = 0; i < pFunctionList.length; i++){
      if(!pFunctionList[i]()){
        return false;
      }
    }
    return true;
  }
}

/**
 * Creates a function which iterates over the functions in the given list and returns true if at least one function in
 * the list returns true.
 * @param pFunctionList List of nested functions to evaluate.
 */
function createOrFunction(pFunctionList){
  return function() {
    for(var i = 0; i < pFunctionList.length; i++){
      if(pFunctionList[i]()){
        return true;
      }
    }
    return false;
  }
}

/**
 * Recursively parses a visibility rule and returns a list of one or more functions which will return boolean true or false.
 * @param pRule Rule object to parse.
 * @param pTriggerMap Map of jQuery objects which each select a trigger object for the rule. Recursive calls to this function
 * add triggers to the list. The map key is used to avoid duplicate entries being added.
 * @return List of functions which will evaluate to true or false.
 */
function parseRule(pRule, pTriggerMap){

  var lFuncList = [];

  for(var i = 0; i < pRule.operation_list.length; i++){
    var lOp = pRule.operation_list[i];

    if(lOp.operation_type == "test"){
      //Operation is running a test of some form - either a widget test or a fixed xpath test
      if(lOp.test_type == "widget") {

        var lTriggers = $("*[name=" + lOp.trigger_name + "]");

        if(lTriggers.is("input") || lTriggers.is("select")) {
          var lType = lTriggers.attr("type");

          if(lType == "radio" || lType == "checkbox") {

            //This is an input type which can be "checked"
            lFuncList.push(createInputCheckedFunction(lOp.trigger_name, lOp.trigger_value, lOp.invert_condition));
            if(lType == "radio") {
              //For radios, attach to every individual radio as onChange events do not fire when a radio is deselected
              pTriggerMap[lOp.trigger_name] = $(jQueryInputSelector(lOp.trigger_name, null));
            }
            else {
              //name + value forms the unique ID of an individual tickbox for attaching to
              pTriggerMap[lOp.trigger_name + '/' + lOp.trigger_value] = $(jQueryInputSelector(lOp.trigger_name, lOp.trigger_value));
            }
          }
          else {
            //Input type=select/select widgets
            lFuncList.push(createEqualsFunction(lOp.trigger_name, lOp.trigger_value, lOp.invert_condition));
            pTriggerMap[lOp.trigger_name] = lTriggers;
          }
        }
        else {
          throw "Unsupported trigger widget type";
        }
      }
      else if(lOp.test_type == "fixed") {
        //A "fixed value" which has been pre-determined by the engine
        lFuncList.push(createFixedValueFunction(lOp.fixed_value));
      }
      else {
        throw "Unknown test type " + lOp.test_type;
      }
    }
    else if(lOp.operation_type == "and" || lOp.operation_type == "or") {

      var lNestedFuncList = parseRule(lOp, pTriggerMap);

      if(lOp.operation_type == "and"){
        lFuncList.push(createAndFunction(lNestedFuncList));
      }
      else if(lOp.operation_type == "or"){
        lFuncList.push(createOrFunction(lNestedFuncList));
      }
    }
    else {
      throw "Unknown operation " + lOp.operation_type;
    }
  }

  return lFuncList;
}

/**
 * Returns a function to be executed on the change event of a trigger element.
 * @param pTargetSelector
 * @param pParsedRule
 * @param pToggleVisibility
 * @param pHideContents
 */
function createChangeEvent(pTargetSelector, pParsedRule, pToggleVisibility, pHideContents){
  return function(){
    //Locate targets to be hidden/shown
    var lTargets = $(pTargetSelector);

    //Evaluate the rule
    var lRuleResult = pParsedRule();

    //Toggle the visibility or display property as required (also need to choose whether the targets' contents are hidden or the targets directly)
    lTargets.not(".clv-ignore").toggleClass("clv-hide-" + (pToggleVisibility ? "visibility" : "display") +  (pHideContents ? "-contents" : ""), !lRuleResult);
  };
}

/**
 * Parses visibility rules and attaches event handlers to trigger elements. This should be called after registerClientVisibility.
 */
function setupClientVisibility(){

  //For each client visibiltity rule in the config
  for(var i = 0; i < gClientVisibilityConfig.length; i++){
    var lRule = gClientVisibilityConfig[i];
    var lTriggerList = {};

    //Parse the rule and establish a list of trigger elements
    var lParsedRule = parseRule(lRule, lTriggerList)[0];

    //Establish a selector pattern to be used to identify target nodes
    var lTargetSelector =  '*[data-xfid=' + lRule.target_xfid + ']'

    //Quickly adding and removing a class to td elements that are descendents of the target_xfid
    //fixes a bug with IE where the elements are not rendered when a cvr hides them on page load
    var lRenderClass = "ie-quirks-cvr-force-re-render-class";
    $(lTargetSelector + " td").addClass(lRenderClass).removeClass(lRenderClass);

    //Attach change handlers to each trigger
    for (var lTrigger in lTriggerList) {
      lTriggerList[lTrigger].on('change', createChangeEvent(lTargetSelector, lParsedRule, lRule.toggle_visibility, lRule.hide_contents));
    }
  }
}

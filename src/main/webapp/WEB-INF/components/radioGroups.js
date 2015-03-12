function isTypeSelect(dataType) {
  if(dataType=="select-one" || dataType=="select-multiple") {
    return true;
  } else {
    return false;
  }
}

function getFoxId(elem) {
 if (elem.value) {
  var dataValue = elem.value;
  var dataIndexOf = dataValue.indexOf("/");
  if (dataIndexOf == -1) {
   dataIndexOf = dataValue.length;
  }
  return dataValue.substring(2,dataIndexOf);
 }
 return;
}

function getFoxType(elem) {
 if (elem.value) {
  var dataValue = elem.value;
  return dataValue.charAt(0)+dataValue.substring(dataValue.indexOf("/")+1,dataValue.length);
 }
 return;
}

function setDeselectedOption(option,isSelectorType) {
  if (isSelectorType) {
    option.selected = false;
  } else {
    option.checked = false;
  }
}

function setSelectedOption(option,isSelectorType) {
  if(isSelectorType) {
   option.selected = true;
  } else {
   //MF: this is a bug (should be option.checked=true) but to fix it would break frValidate()
   option.selected = true;  
  }
}

function deselectOptionsNot(theChar,dataOptions,isSelectorType) {
 for (var loop = 0; loop < dataOptions.length; loop++) {
  var dataOption = dataOptions[loop];
  var dataChar = dataOption.value.charAt(0);
  if (dataChar != theChar) {
   setDeselectedOption(dataOption,isSelectorType);
  }
 }
}

function deselectOptionsNotStore(theChar,dataOptions,hiddenOE,isSelectorType) {
  var found = false;
  for (var loop = 0; loop < dataOptions.length; loop++) {
  var dataOption = dataOptions[loop];
  var dataChar = dataOption.value.charAt(0);
  if (dataChar != theChar) {
   setDeselectedOption(dataOption,isSelectorType);
  } else if (!found) {
   hiddenOE.value = dataOption.value;
   found = true;
  }
 }
}

function resetAllToUNot(id,group,isSelectorType) {
 for (var loop = 0; loop < group.length; loop++) {
  var dataOption = group[loop];
  if (id != getFoxId(dataOption)) {
   if (isSelectorType) {
    var selectOptions = dataOption.options;
    for(var i = 0; i < selectOptions.length; i++) {
     var theOption = selectOptions[i];
     if (theOption.value.charAt(0) == "U") {
      setSelectedOption(theOption,isSelectorType); 
     } else {
      setDeselectedOption(theOption,isSelectorType); 
     }
    }
   } else {
    if (dataOption.value.charAt(0) == "U") {
     setSelectedOption(dataOption,isSelectorType);
    } else {
     setDeselectedOption(dataOption,isSelectorType);
    }
   }
  }
 }
}

function selectOptionChar(theChar,dataOptions,isSelectorType) {
 for (var loop = 0; loop < dataOptions.length; loop++) {
  var dataOption = dataOptions[loop];
  var dataChar = dataOption.value.charAt(0);
  if (dataChar == theChar) {
   setSelectedOption(dataOption,isSelectorType);
  }
 }
}

function frValidate(node) {
 var lastOE;
 var nextOE;
 var countE = 0;
 var countO = 0;
 var countU = 0;
 var inCurrentSection = false;
 if (node && node.name) {
  // Find the forms group
  var group = eval("document.forms[0]."+node.name);
	var hiddenOE = eval("document.forms[0].xx"+node.name);
	lastOE = getFoxType(hiddenOE);
  if (group && group.length > 0) {
   // Loop through every item in the group
   var dataOptions;
   var isSelectorType = isTypeSelect(node.type);
   // Select boxes
   if(isSelectorType) {
     dataOptions = node.options;
		 var thefoxid = 0;
     if (dataOptions.length > 0) {
	 // get id of the current
       thefoxid = getFoxId(dataOptions[0]); 
     }
     for(var i = 0; i < dataOptions.length; i++) {
      var data = dataOptions[i];
      if (data.selected == false) {
       continue;
      }
      var firstChar = data.value.charAt(0);
      if (firstChar == "E") {
       countE++;
      } else if (firstChar == "O") {
       countO++;
      } else if (firstChar == "U") {
       countU++;
      }
      if (hiddenOE.value == data.value) {
       inCurrentSection = true;
      }
     }

     var lastChar;
     if (lastOE) {
       lastChar = lastOE.charAt(0);
     } else {
       lastChar = "U";
     }
     // Decide which are valid decisions
     if (countO > 0 && countE > 0) {
      deselectOptionsNotStore("O",dataOptions,hiddenOE,isSelectorType);
      if (!inCurrentSection) {
       resetAllToUNot(thefoxid,group,isSelectorType);
      }
     } else if (inCurrentSection && countU > 0 && lastChar != "U") {
      deselectOptionsNotStore("U",dataOptions,hiddenOE,isSelectorType);
     } else if (countO > 0) {
      deselectOptionsNotStore("O",dataOptions,hiddenOE,isSelectorType);
      if (!inCurrentSection) {
       resetAllToUNot(thefoxid,group,isSelectorType);
      }
     } else if (countE > 0) {
      deselectOptionsNotStore("E",dataOptions,hiddenOE,isSelectorType);
      if (!inCurrentSection) {
        resetAllToUNot(thefoxid,group,isSelectorType);
      }
     } else if (countU == 0) {
      // if nothing is selected select the none/select one position
      selectOptionChar('U',dataOptions,isSelectorType);
     }
		
    // checkboxes
    } else {
    dataOptions = new Array();
    // get id of the current
    var thefoxid = getFoxId(node); 
    // hold the U of the item
    var dataLastU;
    // check all elements of the same id
    for(var i=0; i!=group.length; i++) {
     var data = group[i];
     var foxid = getFoxId(data);
     if (thefoxid == foxid) {
      var firstChar = data.value.charAt(0);
      if (firstChar == "U") {
       dataLastU = data;
      }
      dataOptions.push(data);
      if (firstChar == "E") {
       countE++;
      } else if (firstChar == "O") {
       countO++;
      } else if (firstChar == "U") {
       countU++;
      }
      if (hiddenOE.value == data.value) {
       inCurrentSection = true;
      }
      if (data.checked == false) {
       continue;
      }
     }
    }
    var nodeChar = node.value.charAt(0);
    deselectOptionsNot(nodeChar,dataOptions,isSelectorType);
    //selectOption(node);
    if (!inCurrentSection) {
     resetAllToUNot(thefoxid,group,isSelectorType); 
    }
   }
   // hold the selected node for next time
   hiddenOE.value = node.value;
  }
 }
}
function vd(t) { frValidate(t); }


//special version of the confirmSubmitSection function (in foxjs.js) that will set the radio
//group back to its original state if the confirm is not clicked
//NOTE: this is not currently designed to work with multiselectors
function radioGroupConfirmSubmitSection(actionValue, contextValue, test, inputElem) {
  if(inputElem.type && inputElem.type=='radio' && inputElem.checked && inputElem.checked == inputElem.defaultChecked){
    return;  //dont do anything if a selected radio is selected
  }
  if (confirm(test)) {
    vd(inputElem);
    submitSection(actionValue, contextValue); //see foxjs.js    
  } else {
    //user clicked cancel so uncheck the input item and put the radiogroup back to its prior position
    
    //if the element is a checkbox then we can switch its value back to its original state
    if(inputElem.type == 'checkbox'){
      if(inputElem.checked){
        inputElem.checked=false;
        }else{
        inputElem.checked=true;
      }
    } else {
        var group = eval("document.forms[0]."+inputElem.name);
        var hiddenOE = eval("document.forms[0].xx"+inputElem.name);
        var oldVal = null;
        if(hiddenOE.value){ //old value may be null
          oldVal=hiddenOE.value;  
        }
    
        var isSelectorType = isTypeSelect(inputElem.type);  ;
        var loopVal;
        var loopElem;
        //loop through values - select the previous value (oldVal) and deselect any others
        for(var i=0; i<group.length; i++){      
          loopElem = group[i];          
          loopVal = loopElem.value;
          if(oldVal==null && loopVal.indexOf('/')==-1){  // no '/' means its the null value
            //set the null value to selected if old value was null
            radioGroupSetSelectedOption(loopElem, isSelectorType);       
          }else{
            if(loopVal==oldVal){          
              radioGroupSetSelectedOption(loopElem, isSelectorType);
            } else {
              setDeselectedOption(loopElem, isSelectorType);
            }
          }
        }  
    }
  }
}

// MF: this function is only here because to fix the bug in setSelectedOption
// has a large impact on the existing javascript - maybe we will fix this some sunny day
function radioGroupSetSelectedOption(option,isSelectorType) {
  if(isSelectorType) {
   option.selected = true;
  } else {
   option.checked = true;
  }
}

//wrapper for radioGroupConfirmSubmitSection
function rgssc(actionValue, contextValue, test, inputElem){
  radioGroupConfirmSubmitSection(actionValue, contextValue, test, inputElem);
}
function rgss(actionValue, contextValue, test, inputElem){
  radioGroupConfirmSubmitSection(actionValue, contextValue, test, inputElem);
}
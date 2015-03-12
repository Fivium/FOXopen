
/*********************************************/
/*   Sortable Tables                         */
/*********************************************/

addEvent(window, "load", sortables_init);
var sortableStr = "&nbsp;&nbsp;&uarr;&darr;";
var sortedDownStr = "&nbsp;&nbsp;&darr;";
var sortedUpStr = "&nbsp;&nbsp;&uarr;";
var sortValIdSeq = 0; //used to give each sortSpan a unique id


function addEvent(elm, evType, fn, useCapture)
// addEvent 
// cross-browser event handling for IE5+,  NS6 and Mozilla
// By Scott Andrew
{
  if (elm.addEventListener){
    elm.addEventListener(evType, fn, useCapture);
    return true;
  } else if (elm.attachEvent){
    var r = elm.attachEvent("on"+evType, fn);
    return r;
  } else {
    null;
    //alert("Handler could not be removed");
  }
} 


//Initialisation function, called when window is loaded.
//Look for all table elements that have the "sortable" attribute set and make then sortable
function sortables_init() {
  if(TEST_FLAG){
    alert('Sort tables - Test mode on');
  }
  sortValIdSeq = 0;
  // Find all table elements with class sortable and make them sortable
  if (!document.getElementsByTagName) return;
  tblHeaders = document.getElementsByTagName("td"); 
  for (ti=0;ti<tblHeaders.length;ti++) {
  
    var thisHeader = tblHeaders[ti];    
    var sortable = thisHeader.getAttribute("sortable");    
    if (sortable!=null) { //((' '+thisHeader.className+' ').indexOf("sortable") != -1) && (thisHeader.id)
      //create a span element inside the header to show the sort order and store the id in the sortSpanId attr      
      thisHeader.setAttribute("sortSpanId", 'sortSpan'+ sortValIdSeq);
      //add the sortable arrows to the column header and make it clickable - this is lazy, the style attrs need to be added to the stylesheet really
      thisHeader.innerHTML = "<span width='100%' class='pt' onmouseover='javascript:sortMo();' onmouseout='javascript:sortMot();' onClick='javascript:sort(this)'>"+thisHeader.innerHTML+"<span id='sortSpan"+sortValIdSeq+"' class='sortSpanTableHeader'>"+sortableStr+"</span></span>"      
      sortValIdSeq++;
    }    
  }
}

function sortMo(){
  document.body.style.cursor='pointer'; 
  return true;
}

function sortMot(){
  document.body.style.cursor='default'; 
  return true;
}

TEST_FLAG = false; //do we want debug?
NULLS_LAST = true; //flag
var NaNFound; //flag that's set to indicate that we have to use a
              //string comparison when a non number is found


//myRow is an object that stores a table row against its sort value
function myRow(pRow, pColIdx){
  this.rowId;
  this.row;
  this.sortVal;
  this.originalIdx;
  if(pRow != null && pRow.nodeType == 1 && pRow.tagName && pRow.tagName.toLowerCase() == 'tr'){
    this.rowId = pRow.rowIndex;
    this.row = pRow;
    if(this.row.cells && this.row.cells.length > pColIdx){
        this.sortVal = this.row.cells[pColIdx].getAttribute('sortVal').toLowerCase();
        NaNFound = NaNFound || isNaN(this.sortVal);                    
    }else{
        this.sortVal=null;
    }
    if(!pRow.getAttribute('originalIdx')){
      this.originalIdx = pRow.rowIndex;
      pRow.setAttribute('originalIdx',this.originalIdx);      
    } else{
      this.originalIdx = pRow.getAttribute('originalIdx');
    }
  }
}

//===================================================================================
//Sort -
//This is called in context of the clickable span element in the header of the table.
//Reorganise the table rows based on the sort values of the cells in the same column
//as the table header that was clicked on
function sort(pSpanElem){
  var st = new Date(); //used for timing    
  try{
    var col = getParent(pSpanElem, 'td'); //get the td for the table header
    var oldCursor = document.body.style.cursor; 
    document.body.style.cursor = 'wait';   
    NaNFound = false; //see global variable
    var colIdx = col.cellIndex; 
    var tbl = getParent(col, 'table');  //get the table that we are sorting
    var row = getParent(col, 'tr');  //get the header row of the table
    var tblRows = tbl.tBodies[0].rows;  //get all the rows of the body of the table (ignore header and footers)
        
    //create a sortable myRow object for each row and store in an array
    var myRows = new Array();
    for(i=0; i<tblRows.length; i++){
      var r = tblRows[i];
      myRows[i] = new myRow(r, colIdx);       
    }                                 
    
    //reset the other sortable col heads
    for(i=0; i<row.cells.length; i++){
      var th = row.cells[i];
      var sortSpanId = th.getAttribute("sortSpanId");
      if(sortSpanId){
        var sortSpan = document.getElementById(sortSpanId);
        if(sortSpan){
          sortSpan.innerHTML = sortableStr;
        }
      }      
    }
    
    //set the arrow to indicate the sort order 
    //sort asc first, then desc then back to original order
    if(col.getAttribute('sortdir')==null || col.getAttribute('sortdir')=='none'){       
      myRows.sort(sortFn); //sort rows using special sort funciton
      col.setAttribute('sortdir', 'asc');
      document.getElementById(col.getAttribute("sortSpanId")).innerHTML=sortedDownStr;
    }else if(col.getAttribute('sortdir')=='asc'){
      myRows.sort(sortFn); //sort rows using special sort funciton
      myRows.reverse();
      col.setAttribute('sortdir', 'desc');     
      document.getElementById(col.getAttribute("sortSpanId")).innerHTML=sortedUpStr;
    }else if(col.getAttribute('sortdir')=='desc'){
      myRows.sort(removeSortFn);
      col.setAttribute('sortdir','none');
      document.getElementById(col.getAttribute("sortSpanId")).innerHTML=sortableStr;      
    }
    
    //add the sorted rows in order
    var resTbl = tbl;
    for(i in myRows){     
      if(myRows[i].row){
        tbl.tBodies[0].appendChild(myRows[i].row);         
      }
    }
/*  }catch(err){         
    if(TEST_FLAG){
      alert(err);
    }  */
  }finally{
    document.body.style.cursor = oldCursor; //always set the cursor back
    if(TEST_FLAG){
      var et = new Date();       
      var time = et-st;
      alert('sorted in '+time+' ms');
    }
  }
}

//Special function for sorting myRow objects based on their stored sort value.
//This delegates to the compare function below when sort values are present
//for both objects being compared, otherwise it obeys the NULL_LAST flag
function sortFn(a, b){
  //if (a is less than b by some ordering criterion)
  //     return -1
  //  if (a is greater than b by the ordering criterion)
  //     return 1
  // a must be equal to b
  //  return 0
  var rtn = -1000;
  //decide what do with nulls based on the global flag
  var nullVal = -1;
  if(!NULLS_LAST){
    nullVal = 1;
  }
  if(a.sortVal && b.sortVal){
    rtn = compare(a.sortVal,b.sortVal);
  }else if(a.sortVal){ //a!=null && b==null
    rtn = nullVal; 
  }else if(b.sortVal){//a==null && b!=null
    rtn = -nullVal;
  }else{
    //both are null
    rtn = 0;
  }  
  return rtn;
}


function removeSortFn(a, b){
  return a.originalIdx - b.originalIdx;
}


//Comparitor function - does a number comparision if all rows valid numbers
//(based on NaNFound flag), otherwise do a string based comparison
function compare(a,b){
  if(!NaNFound){
    //all values are numbers
    return a-b;
  }else if(a>b){ //string based comparison
    return 1;
  }else if(a<b){ //string based comparison
    return -1;
  }else{
    return 0;  //both are equal
  }
}


//Utility function to get the parent element of type pTagName of the specified element pElem
function getParent(pElem, pTagName) {
  if (pElem == null) 
    return null;
  else if (pElem.nodeType == 1 && pElem.tagName.toLowerCase() == pTagName.toLowerCase())   
    // Gecko bug, supposed to be uppercase
    return pElem;
   else
     return getParent(pElem.parentNode, pTagName);
}

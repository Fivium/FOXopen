/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.command.util;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;


@Deprecated
public class CompareXml {

  private static final String HISTORY_OP_STR_CHANGE = "update";
  private static final String HISTORY_OP_STR_INSERT = "insert";
  private static final String HISTORY_OP_STR_DELETE = "delete";

  public static final String COMPARE_DISPLAY_LEGACY = "legacy";
  public static final String COMPARE_DISPLAY_HINT = "hint";

  private HashMap changesMap;
  private DOM changesDOM;
  private Mod mSchemaModule;
  private ContextUElem mContextUElem;
  private String outputDisplayStyle;

  public CompareXml(ContextUElem pContextUElem, String pOutputStyle ) {
    mContextUElem = pContextUElem;
    outputDisplayStyle = pOutputStyle;
  }

  public DOM compareElements(DOM pElementOne, DOM pElementTwo, String pVersionLabel, Mod pSchemaModule) throws ExModule {
    // created a different dom so the xpaths aren't slowed down during the implementation
    changesMap = new HashMap();
    changesDOM = DOM.createDocument("CHANGES_DOM");
    mSchemaModule = pSchemaModule;
    DOM result = pElementOne.clone(true,pElementOne);
    compareElementsCloned(result, pElementTwo, pVersionLabel);
    // implement all the dom changes at the end so the xpaths aren't slowed down
    if (changesMap.size() > 0) {
      Iterator entriesItr = changesMap.entrySet().iterator();
      while(entriesItr.hasNext()) {
        Map.Entry entry = (Map.Entry)entriesItr.next();
        DOM key = (DOM)entry.getKey();   // the dom elements that has a new history child
        DOM value = (DOM)entry.getValue(); // the new history child dom
        value.moveToParent(key);
      }
    }
    return result;
  }

  private void compareElementsCloned(DOM pElementOne, DOM pElementTwo, String pVersionLabel) throws ExModule {
    // A distinct list of all the element names with the signature concat(name,position relative to same), e.g. B1,C1,B2
    ArrayList distinctList = new ArrayList();
    // Create the 2 key hashmaps of signature to element
    DOMList treeOneKids = pElementOne.getChildElements();
    HashMap keyMapOne = getKeyMap(treeOneKids, distinctList);
    DOMList treeTwoKids = pElementTwo.getChildElements();
    HashMap keyMapTwo = getKeyMap(treeTwoKids, distinctList);
    // Build the parent history list
    //DOMList historyList = null;
    HashMap kidsHistoryMap = null;
    // Create a history map of label -> operation ready to be added to
    //if (historyList == null) {
    //  historyList = pElementOne.getUL("./fox-history/history");
      kidsHistoryMap = new HashMap();
    //  for (int i=0; i<historyList.getLength(); i++) {
    //    DOM historyItem = historyList.item(i);
    //    String label = historyItem.get1SNoEx("label");
    //    String operation = historyItem.get1SNoEx("operation");
    //    kidsHistoryMap.put(label,new Integer(getOperationMask(operation,0)));
    //  }
    //}

    // Iterate around the list of distinct elements
    Iterator itr = distinctList.iterator();
    while(itr.hasNext()) {
      String elemName = (String)itr.next();
      DOM childOne = (DOM)keyMapOne.get(elemName);
      DOM childTwo = (DOM)keyMapTwo.get(elemName);
      if (childOne != null && childTwo != null) {
        // Calculate the children nodes history
        if (childOne.getChildElements().getLength()>0 || childTwo.getChildElements().getLength()>0) {
          compareElementsCloned(childOne,childTwo,pVersionLabel);
          // Add the history to the parent
          DOM childHistory = (DOM)changesMap.get(childOne);
          if (childHistory != null) {
          DOMList childHistoryList = childHistory.getUL("./history");
          // Add the kids labels and operations
            for (int i=0; i<childHistoryList.getLength(); i++) {
              DOM historyItem = childHistoryList.item(i);
              String label = historyItem.get1SNoEx("label");
              String operation = historyItem.get1SNoEx("operation");
              Integer currentOp = (Integer)kidsHistoryMap.get(label);
              kidsHistoryMap.put(label,new Integer(getOperationMask(HISTORY_OP_STR_CHANGE,(currentOp==null)?0:currentOp.intValue())));
            }
          }
        }

        // Calculate the nodes values
        String textOne = childOne.value(false).trim();
        String textTwo = childTwo.value(false).trim();
        if (!textOne.equals(textTwo)) {
          // Add history information to the leaf element
          addHistoryChild(childOne,pVersionLabel,textTwo,HISTORY_OP_STR_CHANGE);

          // Add history information to the parent element
          Integer currentOp = (Integer)kidsHistoryMap.get(pVersionLabel);
          kidsHistoryMap.put(pVersionLabel,new Integer(getOperationMask(HISTORY_OP_STR_CHANGE,(currentOp==null)?0:currentOp.intValue())));
        }

      } else if (childOne != null || childTwo != null) {
        String historyOperation;
        if (childOne == null) {
          // TODO: Should be added to the change later map. But not the map will history values
          DOM newElem = childTwo.copyToParent(pElementOne);
          historyOperation = HISTORY_OP_STR_DELETE;
          createRecursiveHistory(newElem,pVersionLabel,historyOperation);
        } else {
          historyOperation = HISTORY_OP_STR_INSERT;
          createRecursiveHistory(childOne,pVersionLabel,historyOperation);
        }
        Integer currentOp = (Integer)kidsHistoryMap.get(pVersionLabel);
        kidsHistoryMap.put(pVersionLabel,new Integer(getOperationMask(historyOperation,(currentOp==null)?0:currentOp.intValue())));

      }
    } // end of looping around distinct children
    // Add the children label -> version history to the parent

    if (kidsHistoryMap.size() > 0) {
      TreeSet keySet = new TreeSet(kidsHistoryMap.keySet());
      Iterator keyItr = keySet.iterator();
      while (keyItr.hasNext()) {
        String label = (String)keyItr.next();
        // a parent may have children which contain different history, combine them together
        Integer opItr = (Integer)kidsHistoryMap.get(label);
        int op = opItr.intValue();
        String opStr = "";
        if ((op&1) == 1) {
          opStr = HISTORY_OP_STR_CHANGE;
        }
        if ((op&2) == 2) {
          opStr += (opStr.length()>0?", ":"")+HISTORY_OP_STR_INSERT;
        }
        if ((op&4) == 4) {
          opStr += (opStr.length()>0?", ":"")+HISTORY_OP_STR_DELETE;
        }

        addHistoryChild(pElementOne,label,null,opStr);
      }
    }
  }

  private void addHistoryChild(DOM pParent, String pLabel, String pValue, String pOperation) {
    // Create the history dom
    DOM foxHistory = DOM.createUnconnectedElement("fox-history");
    DOM history = foxHistory.addElem("history");
    history.setAttr("display-style", outputDisplayStyle);
    history.addElem("label").setText(pLabel);
    if (pValue != null) {
      history.addElem("value").setText(pValue);
    }

    // Add the history to the change later dom
    DOM parentElement = (DOM)changesMap.get(pParent);
    if (parentElement == null) {
      history.addElem("operation").setText(pOperation);
      changesMap.put(pParent, foxHistory);
    }
    else {
      Track.debug("CompareXML", "No parent change found for label '" + pLabel + "'");
    }

  }

  private void createRecursiveHistory(DOM pElement, String pVersion, String pOperation) {
    DOMList kids = pElement.getChildElements();
    for (int i=0; i<kids.getLength(); i++) {
      DOM kid = kids.item(i);
      createRecursiveHistory(kid,pVersion,pOperation);
    }
    addHistoryChild(pElement,pVersion,null,pOperation);
  }

  private static int getOperationMask(String pOperation, int current) {
    if (pOperation.equals(HISTORY_OP_STR_CHANGE)) {
      return 1 | current;
    } else if (pOperation.equals(HISTORY_OP_STR_INSERT)) {
      return 2 | current;
    } else if (pOperation.equals(HISTORY_OP_STR_DELETE)) {
      return 4 | current;
    }
    return 0;
  }

  private HashMap getKeyMap(DOMList pChildElements, ArrayList pDistinctList) throws ExModule {
    HashMap keyMap = new HashMap();
    HashMap occuranceMap = new HashMap();
    for (int i=0; i<pChildElements.getLength(); i++) {
      DOM child = pChildElements.item(i);
      String childName = child.getName();
      NodeInfo nodeInfo = mSchemaModule.getNodeInfo(child);
      String compareid = null;
      if (nodeInfo != null) {
        compareid = nodeInfo.getAttribute("fox","compare-id");
      }
      if (compareid != null && compareid.length() > 0) {
        try {
          childName = mContextUElem.extendedXPathString(child,compareid);
        } catch (ExActionFailed ex) {
          throw new ExModule("Comparing documents compare id failed: "+compareid,ex);
        }
      } else {
        Integer occNoInt = (Integer)occuranceMap.get(childName);
        int occNo = 0;
        if (occNoInt != null) {
          occNo = occNoInt.intValue();
        }
        occNo++;
        occuranceMap.put(childName,new Integer(occNo));
        childName += "#"+occNo;
      }
      if (!pDistinctList.contains(childName)) {
        pDistinctList.add(childName);
      }
      keyMap.put(childName,child);
    }
    return keyMap;
  }

}

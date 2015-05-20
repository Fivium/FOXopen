package net.foxopen.fox.module.mapset;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.fieldset.fvm.DOMFVMOption;
import net.foxopen.fox.module.fieldset.fvm.FVMOption;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * MapSet in which at least one data item is a complex DOM with child elements (c.f. SimpleMapSet, in which all data
 * items are strings). Data to key lookup must be performed by iteration through each data item's DOM.
 */
public class ComplexMapSet
extends DOMMapSet {

  //These lists have corresponding indices - i.e. mDataDOMList[1] is the data item for mEntryList[1]
  private final List<DOM> mDataDOMList;
  private final List<MapSetEntry> mEntryList;
  private final List<FVMOption> mFVMOptionList;

  public static ComplexMapSet createFromDOMList(DOMList pRecordList, MapSetDefinition pMapSetDefinition, String pEvaluatedCacheKey) {

    List<DOM> lDataDOMList = new ArrayList<>(pRecordList.size());
    List<MapSetEntry> lEntryList = new ArrayList<>(pRecordList.size());
    for(DOM lRec : pRecordList) {

      DOM lDataDOM;
      try {
        lDataDOM = lRec.get1E(DATA_ELEMENT_NAME);
      }
      catch (ExCardinality e) {
        throw new ExInternal("Invalid mapset DOM - failed to resolve 'data' element", e);
      }

      //Check for dupes
      for(DOM lExistingData : lDataDOMList) {
        if(lDataDOM.contentEqualsOrSuperSetOf(lExistingData)) {
          throw new ExInternal("Invalid mapset DOM - duplicate 'data' element\n" + lDataDOM.outputNodeToString(true));
        }
      }

      lDataDOMList.add(lDataDOM);
      lEntryList.add(MapSetEntry.createFromDOM(lRec));
    }

    return new ComplexMapSet(lDataDOMList, lEntryList, pMapSetDefinition, pEvaluatedCacheKey);
  }

  private ComplexMapSet(List<DOM> pDataDOMList, List<MapSetEntry> pEntryList, MapSetDefinition pMapSetDefinition, String pEvaluatedCacheKey) {
    super(pMapSetDefinition, pEvaluatedCacheKey);
    mDataDOMList = Collections.unmodifiableList(pDataDOMList);
    mEntryList = Collections.unmodifiableList(pEntryList);

    List<FVMOption> lFVMOptionList = new ArrayList<>(mDataDOMList.size());
    for(DOM lDataDOM : mDataDOMList) {
      lFVMOptionList.add(new DOMFVMOption(lDataDOM));
    }
    mFVMOptionList = Collections.unmodifiableList(lFVMOptionList);
  }

  @Override
  public int indexOf(DOM pItemDOM) {

    for(int i=0; i < mDataDOMList.size(); i++) {
      if(pItemDOM.contentEqualsOrSuperSetOf(mDataDOMList.get(i))) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public List<FVMOption> getFVMOptionList() {
    return mFVMOptionList;
  }

  @Override
  public List<MapSetEntry> getEntryList() {
    return mEntryList;
  }

  @Override
  public DOM getMapSetAsDOM() {
    DOM lMapSetContainerDOM = DOM.createDocument(MAPSET_LIST_ELEMENT_NAME);
    DOM lMapSetDOM = lMapSetContainerDOM.addElem(MAPSET_ELEMENT_NAME);

    for(int i=0; i < mDataDOMList.size(); i++) {
      //Create a new rec, copy the data item in directly and add additional elements from the MapSetEntry
      DOM lRecDOM = lMapSetDOM.addElem(REC_ELEMENT_NAME);
      mDataDOMList.get(i).copyContentsTo(lRecDOM.addElem(DATA_ELEMENT_NAME));
      mEntryList.get(i).serialiseToRecDOM(lRecDOM);
    }

    return lMapSetContainerDOM;
  }

  @Override
  public String getKeyForDataString(ActionRequestContext pRequestContext, DOM pMapSetItem, String pDataString) {
    return null;
  }
}

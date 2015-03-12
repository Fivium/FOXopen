package net.foxopen.fox.module.mapset;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.fieldset.fvm.FVMOption;
import net.foxopen.fox.module.fieldset.fvm.StringFVMOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * MapSet in which all data elements are string values. Data to key lookup can be performed efficiently using a string HashMap.
 * Most MapSets should conform to this pattern.
 */
public class SimpleMapSet
extends MapSet {

  /** Data string to entry map - in mapset order */
  private final LinkedHashMap<String, MapSetEntry> mEntryMap;

  private final List<FVMOption> mFVMOptionList;

  public static SimpleMapSet createFromDOMList(DOMList pRecordList, MapSetDefinition pMapSetDefinition, String pEvaluatedCacheKey) {

    LinkedHashMap<String, MapSetEntry> lEntryMap = new LinkedHashMap<>(pRecordList.size());
    for(DOM lRec : pRecordList) {

      String lDataString = lRec.get1SNoEx(DATA_ELEMENT_NAME);
      if(XFUtil.isNull(lDataString)) {
        throw new ExInternal("Invalid mapset DOM - missing 'data' string value");
      }
      else if(lEntryMap.containsKey(lDataString)) {
        throw new ExInternal("Invalid mapset DOM - duplicate entry for 'data' value " + lDataString);
      }

      MapSetEntry lEntry = MapSetEntry.createFromDOM(lRec);


      lEntryMap.put(lDataString, lEntry);
    }

    return new SimpleMapSet(lEntryMap, pMapSetDefinition, pEvaluatedCacheKey);
  }

  private SimpleMapSet(LinkedHashMap<String, MapSetEntry> pEntryMap, MapSetDefinition pMapSetDefinition, String pEvaluatedCacheKey) {
    super(pMapSetDefinition, pEvaluatedCacheKey);
    mEntryMap = pEntryMap;

    List<FVMOption> lFVMOptionList = new ArrayList<>(mEntryMap.keySet().size());
    for(String lDataString : mEntryMap.keySet()) {
      lFVMOptionList.add(new StringFVMOption(lDataString));
    }
    mFVMOptionList = Collections.unmodifiableList(lFVMOptionList);
  }

  @Override
  public int indexOf(DOM pItemDOM) {
    String lItemValue = pItemDOM.value().trim();

    int i = 0;
    for(String lDataValue : mEntryMap.keySet()) {
      if(lDataValue.equals(lItemValue)) {
        return i;
      }
      i++;
    }

    return -1;
  }

  @Override
  public List<FVMOption> getFVMOptionList() {
    return mFVMOptionList;
  }

  @Override
  public List<MapSetEntry> getEntryList() {
    return new ArrayList<>(mEntryMap.values());
  }

  @Override
  public DOM getMapSetAsDOM() {
    DOM lMapSetContainerDOM = DOM.createDocument(MAPSET_LIST_ELEMENT_NAME);
    DOM lMapSetDOM = lMapSetContainerDOM.addElem(MAPSET_ELEMENT_NAME);

    for(Map.Entry<String, MapSetEntry> lMapEntry : mEntryMap.entrySet()) {
      DOM lRecDOM = lMapSetDOM.addElem(REC_ELEMENT_NAME);
      lRecDOM.addElem(DATA_ELEMENT_NAME, lMapEntry.getKey());
      lMapEntry.getValue().serialiseToRecDOM(lRecDOM);
    }

    return lMapSetContainerDOM;
  }

  @Override
  public String getKeyForDataString(String pDataString) {
    MapSetEntry lEntry = mEntryMap.get(pDataString);
    if(lEntry != null) {
      return lEntry.getKey();
    }
    else {
      return "";
    }
  }
}

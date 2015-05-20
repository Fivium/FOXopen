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
package net.foxopen.fox.module.mapset;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.fieldset.fvm.FVMOption;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.List;


/**
 * A set of key/data pairs as instantiated from a MapSetDefinition. Note: References to MapSet objects should NOT be held
 * in static or member variables as a MapSet may have a very short lifespan depending on its definition. Access to a MapSet
 * should always be via MapSetDefinition to ensure that the most up-to-date copy of the MapSet is always in use. A MapSet's
 * content is fixed after construction - "refreshing" a MapSet should always create a new copy. This ensures thread safety
 * for this class and its subclasses.
 */
public abstract class DOMMapSet implements MapSet {

  private final String mEvaluatedCacheKey;

  private final MapSetDefinition mMapSetDefinition; // The map set definition

  private final long mCreatedTimeMS; // The last time a refresh occurred

  protected DOMMapSet(MapSetDefinition pMapSetDefinition, String pEvaluatedCacheKey) {
    mMapSetDefinition = pMapSetDefinition;
    mEvaluatedCacheKey = pEvaluatedCacheKey;
    mCreatedTimeMS = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " CacheKey: " + mEvaluatedCacheKey + " LifetimeMins: " + ((System.currentTimeMillis() - mCreatedTimeMS) / 1000 / 60) +
      " TimeoutMins: " +  mMapSetDefinition.getRefreshTimeoutMins() + " IsDynamic: " + isDynamic();
  }

  @Override
  public String getMapSetName() {
    return mMapSetDefinition.getLocalName();
  }

  @Override
  public boolean isDynamic() {
    return mMapSetDefinition.isDynamic();
  }

  @Override
  public boolean isRefreshRequired() {
    if(!mMapSetDefinition.isDynamic()) {
      //If this mapset is not dynamic, it will never require a refresh - TODO make sure editable mapsets are dealt with
      return false;
    }
    if (mMapSetDefinition.getRefreshTimeoutMins() == 0 ) {
      //If refresh timeout is specified as 0, the mapset should always be refreshed
      return true;
    }
    else {
      //Otherwise, only refresh if the specified number of minutes has elapsed since the mapset was created
      return System.currentTimeMillis() - mCreatedTimeMS > mMapSetDefinition.getRefreshTimeoutMins() * 60 * 1000;
    }
  }

  /**
   * Constructs a new MapSet from a DOM consisting of repeating "rec" elements with "key" and "data" sub-elements. A
   * "map-set" element should contain the "rec" list.
   * @param pMapSetDOM DOM to create MapSet from.
   * @param pMapSetDefinition The definition of the new MapSet.
   * @param pEvaluatedCacheKey The cache key this MapSet will be stored against.
   * @return A new MapSet.
   */
  static MapSet createFromDOM(DOM pMapSetDOM, MapSetDefinition pMapSetDefinition, String pEvaluatedCacheKey) {

    DOMList lRecList = pMapSetDOM.getUL("map-set/rec");

    boolean lSimple = true;
    for(DOM lRec : lRecList) {
      try {
        if(!lRec.get1E("data").isSimpleElement()) {
          lSimple = false;
          break;
        }
      }
      catch (ExCardinality e) {
        throw new ExInternal("Invalid mapset DOM - failed to resolve 'data' element", e);
      }
    }

    if(lSimple) {
      return SimpleMapSet.createFromDOMList(lRecList, pMapSetDefinition, pEvaluatedCacheKey);
    }
    else {
      return ComplexMapSet.createFromDOMList(lRecList, pMapSetDefinition, pEvaluatedCacheKey);
    }
  }

  @Override
  public abstract DOM getMapSetAsDOM();

  /**
   * Gets the 0-based index of the specified item within this MapSet. If the item does not exist within the MapSet,
   * returns -1. For simple mapsets this is based on a string value comparison, for complex mapsets DOM content is
   * compared using {@link DOM#contentEqualsOrSuperSetOf(DOM)}.
   * @param pItemDOM DOM to search for in this MapSet.
   * @return Index of item or -1.
   */
  public abstract int indexOf(DOM pItemDOM);

  /**
   * Gets the complete list of FVMOptions represented by this MapSet.
   * @return This MapSet's FVMOptions.
   */
  public abstract List<FVMOption> getFVMOptionList();

  /**
   * Gets the complete list of MapSetEntries represented by this MapSet.
   * @return This MapSet's MapSetEntries.
   */
  public abstract List<MapSetEntry> getEntryList();

  @Override
  public boolean containsData(ActionRequestContext pRequestContextDOM, DOM pDataDOM) {
    return indexOf(pDataDOM) != -1;
  }

  @Override
  public String getKey(ActionRequestContext pRequestContext, DOM pDataDOM) {
    int lIndex = indexOf(pDataDOM);
    if(lIndex != -1) {
      return getEntryList().get(lIndex).getKey();
    }
    else {
      return "";
    }
  }

  @Override
  public abstract String getKeyForDataString(ActionRequestContext pRequestContext, DOM pMapSetItem, String pDataString);

  @Override
  public MapSetDefinition getMapSetDefinition() {
    return mMapSetDefinition;
  }

  @Override
  public String getEvaluatedCacheKey() {
    return mEvaluatedCacheKey;
  }
}

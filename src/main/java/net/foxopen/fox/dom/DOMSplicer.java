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
package net.foxopen.fox.dom;

import net.foxopen.fox.ex.ExInternal;

public final class DOMSplicer {

  private final DOMList mDOMList;
  private int mSize;
  private String mLastNodeValue;
  private int mLastNodeValueIndex = -1;

  public DOMSplicer(DOMList pDOMList) {
    mDOMList = pDOMList;
    mSize = mDOMList.getLength();
  }

  public final Index startIndex() {
    return new Index(0, 0);
  }

  public final Index indexOf(char pChar, Index pFromIndex) {
    Index lIndex = new Index(pFromIndex);
    while(!lIndex.EOF()) {
      String lText = lIndex.nodeValue();
      int p = lText.indexOf(pChar, lIndex.mOffset);
      if(p!=-1) {
        return new Index(lIndex.mNodeIndex, p);
      }
      lIndex.incSelfNode();
    }
    return null;
  }

  public final char charAt(Index pFromIndex) {
    return pFromIndex.nodeValue().charAt(pFromIndex.mOffset);
  }

  public final String substring(Index pFromIndex, Index pToIndex) {
    // Simple same node substring
    if(pFromIndex.mNodeIndex==pToIndex.mNodeIndex) {
      return pFromIndex.nodeValue().substring(pFromIndex.mOffset, pToIndex.mOffset);
    }
    // Complex across node substring
    StringBuffer lStringBuffer = new StringBuffer();
    lStringBuffer.append(pFromIndex.nodeValue().substring(pFromIndex.mOffset));
    for(int i=pFromIndex.mNodeIndex+1; i<pToIndex.mNodeIndex; i++) {
      lStringBuffer.append(mDOMList.item(i).value(false));
    }
    lStringBuffer.append(pToIndex.nodeValue().substring(0, pToIndex.mOffset));
    return lStringBuffer.toString();
  }

  public final void replace(Index pFromIndex, Index pToIndex, DOM pSpliceInNodeDOM) {

    // Determine first node location and parent (used below for splicing)
    DOM lFirstDOM = mDOMList.item(pFromIndex.mNodeIndex);
    DOM lParentDOM = lFirstDOM.getParentOrNull();
    if(lParentDOM==null) {
      throw new ExInternal("Cannot locate parent of node to splice");
    }

    // Splice before text (if any)
    DOM lBeforeSpliceDOM = null;
    if(pFromIndex.mOffset!=0) {
      lBeforeSpliceDOM = DOM.createUnconnectedText(pFromIndex.nodeValue().substring(0, pFromIndex.mOffset));
      lBeforeSpliceDOM.moveToParentBefore(lParentDOM, lFirstDOM);
    }

    // Splice in new node
    pSpliceInNodeDOM.moveToParentBefore(lParentDOM, lFirstDOM);

    // Special case when end spice is entire node
    DOM lAfterSpliceDOM = null;
    int lSpiceMax = pToIndex.mNodeIndex;
    if(pFromIndex.mNodeIndex!=pToIndex.mNodeIndex && pToIndex.mOffset==0) {
      lSpiceMax--;
    }

    // Splice after text (if any)
    else {
      if(pToIndex.mOffset!=pToIndex.nodeValue().length()) {
        lAfterSpliceDOM = DOM.createUnconnectedText(pToIndex.nodeValue().substring(pToIndex.mOffset));
        lAfterSpliceDOM.moveToParentBefore(lParentDOM, lFirstDOM);
      }
    }

    // Cut out unwanted nodes between splice range
    for(int i=lSpiceMax; i>=pFromIndex.mNodeIndex; i--) {
      mDOMList.item(i).remove();
      mDOMList.removeFromList(i);
    }

    // Graft new nodes into list
    if(lAfterSpliceDOM!=null) {
      mDOMList.add(pFromIndex.mNodeIndex, lAfterSpliceDOM);
    }
    mDOMList.add(pFromIndex.mNodeIndex, pSpliceInNodeDOM);
    if(lBeforeSpliceDOM!=null) {
      mDOMList.add(pFromIndex.mNodeIndex, lBeforeSpliceDOM);
    }

    // Reset node value cache and other controls
    mLastNodeValueIndex=-1;
    mSize = mDOMList.getLength();

  }

  public final class Index {

    private int mNodeIndex;
    private int mOffset;

    private Index(int pNodeIndex, int pOffset) {
      mNodeIndex = pNodeIndex;
      mOffset = pOffset;
    }

    private Index(Index pIndex) {
      mNodeIndex = pIndex.mNodeIndex;
      mOffset = pIndex.mOffset;
    }

    private final String nodeValue() {
      if(mNodeIndex!=mLastNodeValueIndex) {
        DOM lDOM = mDOMList.item(mNodeIndex);
        if(lDOM!=null && lDOM.isText()) {
          mLastNodeValue = lDOM.value(false);
        }
        else {
          mLastNodeValue = "";
        }
        mLastNodeValueIndex = mNodeIndex;
      }
      return mLastNodeValue;
    }

    private final Index incSelf(int pIncrement) {
      for(int i=0; i<pIncrement; i++) {
        mOffset++;
        if(mOffset>=nodeValue().length()) {
          incSelfNode();
          if(EOF()) {
            break;
          }
        }
      }
      return this;
    }

    public final Index incClone(int pIncrement) {
      return new Index(this).incSelf(pIncrement);
    }

    public final Index incSelfNode() {
      mNodeIndex++;
      mOffset=0;
      return this;
    }

    public final boolean lessThan(Index pCompareIndex) {
      return mNodeIndex<pCompareIndex.mNodeIndex || (mNodeIndex==pCompareIndex.mNodeIndex && mOffset<pCompareIndex.mOffset);
    }

    public final boolean EOF() {
      return mNodeIndex>=mSize;
    }

  }

}

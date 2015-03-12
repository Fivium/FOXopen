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
package net.foxopen.fox;

import net.foxopen.fox.entrypoint.FoxGlobals;

import java.util.Iterator;


class UniqueIterator
implements Iterator<String> {

    private long mNext = 0;
    private long mReset = 0;
    private String mSuffix;
    private final long mCacheSize;

    UniqueIterator(long pCacheSize) {
      mCacheSize = pCacheSize;
    }

     public final boolean hasNext() {
       return true;
     }

    public synchronized final String next() {
      if(mNext>=mReset) {
        reset();
      }
     return XFUtil._gUniquePrefix+XFUtil.toAlphaString(mNext++)+mSuffix;
    }

    public final void remove() {
    }

    private final void reset() {
      synchronized (XFUtil._gUniqueSyncObject) {
        mNext = XFUtil._gUniqueCount;
        XFUtil._gUniqueCount += mCacheSize;
        if(XFUtil._gUniqueCount>=XFUtil.UNIQUE_RESET_BOUNDARY) {
          String lHostName = FoxGlobals.getInstance().getServerHostName();
          XFUtil._gUniqueSuffix = "_"+ lHostName.substring(1, 2)+ lHostName.substring(lHostName.length() - 1)
            +XFUtil.toAlphaString(System.currentTimeMillis());
          XFUtil._gUniqueCount = mCacheSize;
          mNext = 0;
        }
        mSuffix = XFUtil._gUniqueSuffix;
      }
      mReset = mNext + mCacheSize;
    }
  }

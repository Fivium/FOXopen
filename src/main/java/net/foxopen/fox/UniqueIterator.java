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

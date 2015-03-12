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
package net.foxopen.fox.thread.storage;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.database.storage.lob.LOBWorkDoc;
import net.foxopen.fox.database.storage.lob.ReadOnlyLOBWorkDoc;
import net.foxopen.fox.database.storage.lob.WriteableLOBWorkDoc;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;

/**
 * WSL for accessing an arbitrary file (not XML) which is stored as a LOB on the database. This could be an upload target,
 * a generation destination, or a download target.
 * @param <T> LOB type (Blob or Clob) which this WFSL will be used to retrieve. Leave as Object if not known at evaluate time.
 */
public class WorkingFileStorageLocation<T>
extends WorkingStorageLocation<FileStorageLocation>  {

  protected final Class<T> mLOBClass;

  public WorkingFileStorageLocation(Class<T> pLOBClass, FileStorageLocation pStorageLocation, ContextUElem pContextUElem, boolean pReadOnly) {
    super(pStorageLocation, pContextUElem, null, true, pReadOnly);
    mLOBClass = pLOBClass;
  }

  protected WorkingFileStorageLocation(Class<T> pLOBClass, FileStorageLocation pStorageLocation, String pUniqueValue, boolean pReadOnly) {
    super(pStorageLocation, new ContextUElem(), pUniqueValue, true, pReadOnly);
    mLOBClass = pLOBClass;
  }

  //Overloaded to provide a FILE StorageLocation
  @Override
  public FileStorageLocation getStorageLocation() {
    if(mAppMnem != null && mModName != null){
      return Mod.getModuleForAppMnemAndModuleName(mAppMnem, mModName).getFileStorageLocation(mStorageLocationName);
    }
    else {
      return null;
    }
  }

  /**
   * Creates a new WorkDoc for this WSL. This must be opened, manipulated and closed using the standard LOBWorkDoc API.
   * @param pReadOnly If true, the WorkDoc will be read only. If false it will be writeable. Note that if this WSL is
   * not writeable, it will not be able to create a writeable WorkDoc. Note this is specified separately to the writeability
   * of the WSL because you may want to create a read-only "view" of a writeable WSL (this should be preferred unless writing
   * is definitely needed).
   * @return New LOBWorkDoc.
   */
  public LOBWorkDoc<T> createWorkDoc(boolean pReadOnly) {
    if(pReadOnly) {
      return new ReadOnlyLOBWorkDoc<>(mLOBClass, this);
    }
    else {
      if(isReadOnly()) {
        throw new ExInternal("Cannot create a Writeable WorkDoc for a read only WorkingFileStorageLocation");
      }
      return new WriteableLOBWorkDoc<>(mLOBClass, this);
    }
  }
}

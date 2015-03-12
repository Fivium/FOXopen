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

$Id: //streams/xthread-serialise/Fox/branches/dev/Fox5-Staging/source/oracle_jdev/Fox/src/net/foxopen/fox/StoreLocation.java#2 $

*/
package net.foxopen.fox.thread.storage;


import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.SyncMode;

/**
 * Implementation of a StorageLocation used for storing the data (root) DOMs for a StatefulXThread.
 */
public class DataDOMStorageLocation
extends StorageLocation
implements Validatable {

  /** Used to differentiate between Clob and Binary XML storage options. */
  private static enum XMLStorageType{
    BINARY,
    CLOB;

    public static XMLStorageType fromString(String pString){
      if(pString == null){
        return null;
      }
      pString = pString.toLowerCase();
      if("binary".equals(pString)){
        return BINARY;
      }
      else if ("clob".equals(pString)) {
        return CLOB;
      }
      else{
        return null;
      }
    }
  }

  /** Optional context label name for DOM SL definitions. */
  private final String mDocumentContextLabel;
  private final String mNewRootElementName;

  private final XMLStorageType mXMLStorageType;

  public DataDOMStorageLocation(Mod pMod, DOM pStoreLocationDOM)
  throws ExModule, ExInternal {

    super(pMod, pStoreLocationDOM, false);

    if(pStoreLocationDOM.get1EOrNull("fm:database") != null && getCacheKey() == null) {
      throw new ExModule("Storage location for a DOM must specify a cache key if a database definition is provided", pStoreLocationDOM);
    }

    // Check to see if storage location has fm:new-document definition
    DOM lNewDocDefinition = null;
    try{
      lNewDocDefinition= pStoreLocationDOM.get1E("fm:new-document");
    }
    catch (ExTooMany x) {
      throw new ExModule("Bad storage-location new-document", pStoreLocationDOM,x);
    }
    catch (ExTooFew x) {
      lNewDocDefinition = null;
    } // new-document does not have to be specified

    // When new-document specified
    if(lNewDocDefinition != null) {
      mNewRootElementName = lNewDocDefinition.get1SNoEx("fm:root-element");
      if(mNewRootElementName.length()==0) {
        throw new ExModule("Bad storage location " + getName() + ": new-document root-element cannot be null", pStoreLocationDOM);
      }
    }
    else {
      mNewRootElementName = null;
    }

    DOM lContextLabelDefinition = pStoreLocationDOM.get1EOrNull("fm:context-label");
    if(lContextLabelDefinition != null) {
      mDocumentContextLabel = lContextLabelDefinition.value();
      if(XFUtil.isNull(mDocumentContextLabel)) {
        throw new ExModule("fm:context-label cannot be an empty string", pStoreLocationDOM);
      }
      else if(!ContextLabel.isLabelNameValid(mDocumentContextLabel)) {
        throw new ExModule("Invalid label name: " + mDocumentContextLabel);
      }
    }
    else {
      mDocumentContextLabel = null;
    }

    //Establish XML storage type
    mXMLStorageType = XMLStorageType.fromString(pStoreLocationDOM.getAttr("xml-storage-type"));

    //Check that this is a valid definition for a binary XML store location
    if(mXMLStorageType == XMLStorageType.BINARY) {
      //Check an update statement has been defined
      if (!hasDatabaseStatement(StatementType.UPDATE)) {
        throw new ExInternal("Storage Locations for binary XML must have an update statement defined");
      }
      else {
        //Assert the XML column is bound in to the update statement
        DatabaseStatement lUpdateStatement = getDatabaseStatement(StatementType.UPDATE);
        boolean lXMLBindFound = false;
        BIND_LOOP:
        for(StorageLocationBind lBind : lUpdateStatement.getBindList()) {
          if(lBind.getUsingType() == UsingType.XMLTYPE) {
            lXMLBindFound = true;
            break BIND_LOOP;
          }
        }

        if(!lXMLBindFound) {
          throw new ExInternal("Storage Locations for binary XML must bind the DOM into the update statement with bind type " + UsingType.XMLTYPE.getExternalName());
        }
      }
    }
  }

//  /**
//   * Constructs a new WorkingStorageLocation representing the evaluated form of this StorageLocation, with a SyncMode of
//   * SYNCHRONISED.
//   * @param pContextUElem Context for XPath evaluation.
//   * @param pCallId Current module call ID for UNIQUE bind evaluation.
//   * @return A new WorkingStorageLocation.
//   * @throws ExModule
//   */
//  public WorkingDataDOMStorageLocation createWorkingStorageLocation(ContextUElem pContextUElem, String pCallId) {
//    return new WorkingDataDOMStorageLocation(this, pContextUElem, pCallId, hasQueryStatement(), SyncMode.SYNCHRONISED);
//  }

  /**
   * Constructs a new WorkingStorageLocation representing the evaluated form of this StorageLocation. The SyncMode is
   * determined by entry theme markup.
   * @param pContextUElem Context for XPath evaluation.
   * @param pCallId Current module call ID for UNIQUE bind evaluation.
   * @param pEntryTheme Entry theme to establish the SyncMode for the new WSL.
   * @return A new WorkingStorageLocation.
   * @throws ExModule
   */
  public WorkingDataDOMStorageLocation createWorkingStorageLocation(ContextUElem pContextUElem, String pCallId, EntryTheme pEntryTheme) {
    //Note: binds are only evaluated if a query statement is present
    return new WorkingDataDOMStorageLocation(this, pContextUElem, pCallId, hasQueryStatement(), pEntryTheme.getSyncModeForStorageLocation(getName()));
  }

  /**
   * Constructs a new WorkingStorageLocation representing the evaluated form of this StorageLocation with a pre-determined
   * SyncMode.
   * @param pContextUElem Context for XPath evaluation.
   * @param pCallId Current module call ID for UNIQUE bind evaluation.
   * @param pSyncMode Predetermined SyncMode.
   * @return A new WorkingStorageLocation.
   */
  public WorkingDataDOMStorageLocation createWorkingStorageLocation(ContextUElem pContextUElem, String pCallId, SyncMode pSyncMode) {
    //Note: binds are only evaluated if a query statement is present
    return new WorkingDataDOMStorageLocation(this, pContextUElem, pCallId, hasQueryStatement(), pSyncMode);
  }

  public void validate(Mod module) {
  }

  public final String getNewDocRootElementName() {
    return mNewRootElementName;
  }

  public boolean isBinaryXMLStorageType(){
    return mXMLStorageType == XMLStorageType.BINARY;
  }

  /**
   * Gets the context label defined for this SL. If not defined the default is "root" (for legacy/backwards compatibility reasons).
   * @return
   */
  public String getDocumentContextLabel() {
    return mDocumentContextLabel == null ? ContextLabel.ROOT.asString() : mDocumentContextLabel;
  }
}

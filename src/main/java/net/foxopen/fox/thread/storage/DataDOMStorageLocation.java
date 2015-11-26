package net.foxopen.fox.thread.storage;


import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.SyncMode;
import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.module.entrytheme.EntryTheme;

/**
 * Implementation of a StorageLocation used for storing the data (root) DOMs for a StatefulXThread.
 */
public class DataDOMStorageLocation
extends StorageLocation
implements Validatable {

  /** Used to differentiate between Clob and Binary XML storage options. */
  private enum XMLStorageType{
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

package net.foxopen.fox.thread.storage;

import java.sql.Blob;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.database.sql.ExecutableAPI;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.BlobBindObject;
import net.foxopen.fox.database.sql.bind.DOMBindObject;
import net.foxopen.fox.database.storage.WorkDoc;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.filetransfer.UploadInfo;


/**
 * Overloaded WorkingFileStorageLocation which is capable of providing file upload metadata as a UsingType. This is only
 * required for a storage location being used as a target of an active upload.
 */
public class WorkingUploadStorageLocation
extends WorkingFileStorageLocation<Blob> {

  private final UploadInfo mUploadInfo;

  public WorkingUploadStorageLocation(FileStorageLocation pStorageLocation, ContextUElem pContextUElem, UploadInfo pUploadInfo) {
    super(Blob.class, pStorageLocation, pContextUElem, false);
    mUploadInfo = pUploadInfo;
  }

  @Override
  protected BindObjectProvider createBindObjectProvider(DatabaseStatement pDatabaseStatement, StatementType pStatementType, WorkDoc pOptionalWorkDoc) {
    return new WSLBindObjectProvider(pDatabaseStatement, pStatementType, pOptionalWorkDoc);
  }

  /**
   * Gets the API statment for this upload which should be executed at each stage of the upload.
   * @param pWorkDoc
   * @return
   */
  public ExecutableAPI getExecutableAPIStatementOrNull(WorkDoc pWorkDoc) {
    return getExecutableAPIOrNull(pWorkDoc, StatementType.API);
  }

  /**
   * Overloaded parent implementation for providing upload metadata and withholding the LOB in case of an upload error.
   */
  private class WSLBindObjectProvider
  extends WorkingStorageLocation.WSLBindObjectProvider {

    public WSLBindObjectProvider(DatabaseStatement pDatabaseStatement, StatementType pStatementType, WorkDoc pWorkDoc) {
      super(pDatabaseStatement, pStatementType, pWorkDoc);
    }

    public BindObject getBindObject(String pBindName, int pIndex) {
      StorageLocationBind lBind = mDatabaseStatement.getBind(pBindName, pIndex);
      if(lBind.getUsingType() == UsingType.FILE_METADATA) {
        return new DOMBindObject(mUploadInfo.getMetadataDOM());
      }
      else if(lBind.getUsingType().isData() && mUploadInfo.isUploadFailed()) {
        //Don't allow the upload LOB to be bound if the upload was failed for any reason (LOB locator is invalid)
        return new BlobBindObject(null);
      }
      else {
        return super.getBindObject(pBindName, pIndex);
      }
    }
  }

  /**
   * This method seeks bind variables specified in the storage location that reference
   * the metadata supplied on a file upload, and reloads the evaluated string value arrays
   * accordingly. All other XPaths are unaffected and will remain in the state they were in
   * when this object is instantiated.
   * <br/><br/>
   * It is necessary to perform this step because when the file upload routine is writing
   * the uploaded data to a storage location, the metadata does not exist on a DOM. Additionally,
   * this WorkingFileStorageLocation will have been instantiated during HTML generation, so the
   * results of the initial XPath evaluation based on upload metadata are potentially stale.
   * <br/><br/>
   * It is envisaged that this routine will mostly be used to populate the correct file-id
   * into the WFSL before an update is attempted. FOX Developers should ensure they only reference
   * such metadata in a relative manner when the WFSL is being used to enable file uploads.
   * <br/><br/>
   */
  public void reEvaluateFileMetadataBinds() {

    DOM lUploadInfoDOM = mUploadInfo.getMetadataDOM();

    Set<String> lUploadInfoPathSet = new HashSet<>();
    getSimplePathsForAllNodesInDOM(lUploadInfoDOM, lUploadInfoPathSet);

    //For each statement type in the WSL
    for(Map.Entry<StatementType, Map<StorageLocationBind, String>> lStatementMapEntry : mStatementTypeToEvaluatedStringBindMap.entrySet()) {
      List<StorageLocationBind> lBindList = getStorageLocation().getDatabaseStatement(lStatementMapEntry.getKey()).getBindList();

      //Check every bind in the statement. If it's a relative path to one of the upload metadata elements, replace the evaluated
      //value with the latest available value.
      for(StorageLocationBind lBind : lBindList) {
        for(String lUploadInfoPath : lUploadInfoPathSet) {
          String lClauseXPath = lBind.getUsingString();
          if(lBind.getUsingType() == UsingType.XPATH && lClauseXPath.indexOf(lUploadInfoPath) != -1) {
            //Replace out :{item} references as these are technically relative xpaths
            lClauseXPath = lClauseXPath.replace(ContextLabel.ITEM.asColonSquiggly(), ".");

            //TODO PN better way of deciding if path is relative
            if (lClauseXPath.indexOf(lUploadInfoPath) != -1) {
              if (lClauseXPath.indexOf(":{") != -1 || lClauseXPath.charAt(0) == '/') {
                throw new ExInternal("Unsupported use of complex or absolute XPath detected referencing file upload metadata. This XPath is required to be relative to the upload element at this time.");
              }
              //A reference to file upload metadata has been found in an XPath - update the evaluated string value accordingly
              lStatementMapEntry.getValue().put(lBind, lUploadInfoDOM.get1SNoEx("./" + lUploadInfoPath));
            }
          }
        }
      }
    }
  }

  /**
   * Gets a Set of all the simple XPaths yielded from nodes in the given DOM,
   * excluding the name of the containing element. I.e. /file-id, /captured-field/description, etc
   * TODO PN move to DOMUtil subclass or similar?
   * @param pDOM
   * @param pSet
   */
  private static void getSimplePathsForAllNodesInDOM(DOM pDOM, Set<String> pSet) {

    DOMList lDOMList = pDOM.getChildElements();
    for (DOM lChild : lDOMList) {
      String lPath = lChild.absolute();
      //Trim off containing element name
      pSet.add(lPath.substring(lPath.substring(1).indexOf('/') + 1));
      getSimplePathsForAllNodesInDOM(lChild, pSet);
    }
  }
}

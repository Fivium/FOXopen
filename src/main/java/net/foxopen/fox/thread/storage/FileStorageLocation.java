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

import java.sql.Blob;

import java.util.Map;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.filetransfer.UploadInfo;
import net.foxopen.fox.module.Mod;


public class FileStorageLocation
extends StorageLocation
implements Validatable {

  private boolean mIsUploadTarget;

  /**
  * Constructs a FileStorageLocation from the given XML
  * specification.
  *
  * @param pDefinitionDOM the XML element that represents a <code>file-storage-location</code>.
  * @exception ExInternal thrown if an error occurs parsing the file storage location
  */
  public FileStorageLocation(Mod pMod, DOM pDefinitionDOM)
  throws ExModule {
    //Force the SL code to generate a cache key from select binds (ignore whatever cache key the user has put)
    super(pMod, pDefinitionDOM, true);

    // Parse the API statement if defined
    DOM lAPIDefinition = pDefinitionDOM.get1EOrNull("fm:api");
    if(lAPIDefinition != null) {
      DatabaseStatement lDatabaseStatement = DatabaseStatement.createFromDOM(lAPIDefinition, "fm:statement", StatementType.API, getName());
      addDatabaseStatement(StatementType.API, lDatabaseStatement);
    }

    mIsUploadTarget = Boolean.valueOf(XFUtil.nvl(pDefinitionDOM.getAttrOrNull("is-upload-target"), "true"));

    //Validate that a query or API was specified
    boolean isDMLBasedFile = pDefinitionDOM.getUL("fm:database").getLength() > 0;
    boolean isAPIBasedFile = lAPIDefinition != null;
    if (!isDMLBasedFile && !isAPIBasedFile) {
      throw new ExInternal("Error in file-storage-location "+ getName()+" - expected either a database or API driven file storage location, or both. "+
                           "Please use either a \"database\" and/or \"api\" sub-element to specify a way to query and store an uploaded file.");
    }
  }

  public FileStorageLocation(String pName, Map<StatementType, DatabaseStatement> pStatementMap) {
    super(pName, pStatementMap);
  }

  /**
   * Evaluates the binds for this storage location to create a working copy.
   * @param pContextUElem
   * @param pUploadTarget DOM node where upload metadata is stored.
   * @return
   */
  private <T> WorkingFileStorageLocation<T> createWorkingStorageLocation(Class<T> pLOBClass, ContextUElem pContextUElem, DOM pUploadTarget, boolean pReadOnly) {
    pContextUElem.localise("Create WFSL");
    try {
      //Set attach/item contexts for relative bind evaluation
      pContextUElem.setUElem(ContextLabel.ATTACH, pUploadTarget.getParentOrSelf());
      pContextUElem.setUElem(ContextLabel.ITEM, pUploadTarget);
      return new WorkingFileStorageLocation<T>(pLOBClass, this, pContextUElem, pReadOnly);
    }
    finally {
      pContextUElem.delocalise("Create WFSL");
    }
  }

  /**
   * Creates a read only WFSL for the download of a previously uploaded file.
   * @param pContextUElem For WFSL bind evaluation.
   * @param pUploadTarget DOM containing file upload metadata.
   * @return New WFSL which can be used for a download.
   */
  public WorkingFileStorageLocation<Blob> createWorkingStorageLocationForUploadDownload(ContextUElem pContextUElem, DOM pUploadTarget) {
    return createWorkingStorageLocation(Blob.class, pContextUElem, pUploadTarget, true);
  }

  /**
   * Creates a standard WFSL for either a generation destination or a download target.
   * @param <T> LOB type to be selected by the WFSL (usually Blob or Clob).
   * @param pLOBClass LOB type to be selected by the WFSL.
   * @param pContextUElem For WFSL bind evaluation.
   * @param pReadOnly If true, the created WFSL will only have SELECT bind variables evaluated, and will not be able
   * to insert/update rows.
   * @return New WFSL.
   */
  public <T> WorkingFileStorageLocation<T> createWorkingStorageLocation(Class<T> pLOBClass, ContextUElem pContextUElem, boolean pReadOnly) {
    return new WorkingFileStorageLocation<T>(pLOBClass, this, pContextUElem, pReadOnly);
  }

  /**
   * Creates a working storage location based on this storage location which can act as an upload target.
   * @param pContextUElem
   * @param pUploadTarget DOM node being uploaded into.
   * @param pUploadInfo For providing upload file metadata.
   * @return
   */
  public WorkingUploadStorageLocation createWorkingUploadStorageLocation(ContextUElem pContextUElem, DOM pUploadTarget, UploadInfo pUploadInfo) {
    pContextUElem.localise("Create WFSL");
    try {
      //Set attach/item contexts for relative bind evaluation
      pContextUElem.setUElem(ContextLabel.ATTACH, pUploadTarget.getParentOrSelf());
      pContextUElem.setUElem(ContextLabel.ITEM, pUploadTarget);
      return new WorkingUploadStorageLocation(this, pContextUElem, pUploadInfo);
    }
    finally {
      pContextUElem.delocalise("Create WFSL");
    }

  }

  /**
   * Validates that the module, and its sub-components, are valid.
   *
   * @param module the module where the component resides
   * @throws ExInternal if the component syntax is invalid.
   */
  public void validate(Mod module) {}

  public boolean isUploadTarget() {
    return mIsUploadTarget;
  }
}

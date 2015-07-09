package net.foxopen.fox.command.util;

import net.foxopen.fox.database.storage.dom.XMLWorkDoc;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.SyncMode;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.DataDOMStorageLocation;
import net.foxopen.fox.thread.storage.FileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;
import net.foxopen.fox.track.Track;

import java.sql.Blob;
import java.sql.Clob;

/**
 * GeneratorDestination for writing to a Clob or Blob identified by a Storage Location definition.
 */
public class StorageLocationGeneratorDestination
implements GeneratorDestination {

  private final String mStorageLocationName;

  public StorageLocationGeneratorDestination(String pStorageLocationName) {
    mStorageLocationName = pStorageLocationName;
  }

  private FileStorageLocation getStorageLocation(ActionRequestContext pRequestContext) {
    return pRequestContext.getCurrentModule().getFileStorageLocation(mStorageLocationName);
  }

  @Override
  public void generateToWriter(ActionRequestContext pRequestContext, WriterGenerator pGenerator) {

    FileStorageLocation lStorageLocation = getStorageLocation(pRequestContext);
    WorkingFileStorageLocation<Clob> lWFSL = lStorageLocation.createWorkingStorageLocation(Clob.class, pRequestContext.getContextUElem(), false);

    GeneratorDestinationUtils.clobWriteToWFSL(pRequestContext, pGenerator, lWFSL);
  }

  @Override
  public void generateToOutputStream(ActionRequestContext pRequestContext, OutputStreamGenerator pGenerator) {

    FileStorageLocation lStorageLocation = getStorageLocation(pRequestContext);
    WorkingFileStorageLocation<Blob> lWFSL = lStorageLocation.createWorkingStorageLocation(Blob.class, pRequestContext.getContextUElem(), false);

    GeneratorDestinationUtils.blobWriteToWFSL(pRequestContext, pGenerator, lWFSL);
  }

  @Override
  public void generateToDOM(ActionRequestContext pRequestContext, DOMGenerator pGenerator) {

    try {
      DataDOMStorageLocation lStorageLocation = pRequestContext.getCurrentModule().getDataStorageLocation(mStorageLocationName);

      WorkingDataDOMStorageLocation lWSL = lStorageLocation.createWorkingStorageLocation(pRequestContext.getContextUElem(), pRequestContext.getCurrentCallId(), SyncMode.SYNCHRONISED);
      XMLWorkDoc lWorkDoc = XMLWorkDoc.getOrCreateXMLWorkDoc(lWSL, false);

      lWorkDoc.open(pRequestContext.getContextUCon(), false);
      try {
        DOM lTargetDOM = lWorkDoc.getDOM();
        pGenerator.writeOutput(lTargetDOM);
        lWorkDoc.close(pRequestContext.getContextUCon());
      }
      catch(Throwable th) {
        //Release any lock on the WorkDoc in the event of an error
        try {
          lWorkDoc.abort();
        }
        catch(Throwable th2) {
          Track.recordSuppressedException("SLGenerateWorkDocAbort", th2);
        }

        //Rethrow the original error
        throw th;
      }
    }
    catch (ExModule e) {
      throw new ExInternal("Failed to locate storage location", e);
    }
  }
}

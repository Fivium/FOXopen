package net.foxopen.fox.plugin;

import net.foxopen.fox.command.util.DownloadGeneratorDestination;
import net.foxopen.fox.command.util.GeneratorDestination;
import net.foxopen.fox.command.util.StorageLocationGeneratorDestination;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.storage.dom.XMLWorkDoc;
import net.foxopen.fox.database.storage.lob.LOBWorkDoc;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.SyncMode;
import net.foxopen.fox.plugin.api.PluginManagerContext;
import net.foxopen.fox.plugin.api.command.FxpCommandContext;
import net.foxopen.fox.plugin.api.command.util.FxpConsumer;
import net.foxopen.fox.plugin.api.command.util.FxpDOMGenerator;
import net.foxopen.fox.plugin.api.command.util.FxpGenerator;
import net.foxopen.fox.plugin.api.command.util.FxpInputStreamConsumer;
import net.foxopen.fox.plugin.api.command.util.FxpOutputStreamGenerator;
import net.foxopen.fox.plugin.api.command.util.FxpReaderConsumer;
import net.foxopen.fox.plugin.api.command.util.FxpWriterGenerator;
import net.foxopen.fox.plugin.api.database.FxpContextUCon;
import net.foxopen.fox.plugin.api.dom.FxpContextUElem;
import net.foxopen.fox.plugin.api.dom.FxpDOM;
import net.foxopen.fox.plugin.util.DOMGeneratorAdaptor;
import net.foxopen.fox.plugin.util.OutputStreamGeneratorAdaptor;
import net.foxopen.fox.plugin.util.WriterGeneratorAdaptor;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.DataDOMStorageLocation;
import net.foxopen.fox.thread.storage.FileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;
import net.foxopen.fox.track.Track;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

public class PluginCommandRequestContextWrapper
implements FxpCommandContext {

  private final ActionRequestContext mRequestContext;
  private final PluginManagerContext mPluginManagerContext;

  PluginCommandRequestContextWrapper(ActionRequestContext pRequestContext, PluginManagerContext pPluginManagerContext) {
    mRequestContext = pRequestContext;
    mPluginManagerContext = pPluginManagerContext;
  }

  @Override
  public FxpContextUElem getContextUElem() {
    //Wrap the current ContextUElem to avoid directly exposing it
    return new PluginCommandContextUElemWrapper(mRequestContext.getContextUElem());
  }

  @Override
  public FxpContextUCon getContextUCon() {
    return mRequestContext.getContextUCon();
  }

  @Override
  public PluginManagerContext getPluginManagerContext() {
    return mPluginManagerContext;
  }

  private void readBlobStorageLocation(FileStorageLocation pFSL, FxpInputStreamConsumer pConsumer) {

    WorkingFileStorageLocation<Blob> lWFSL = pFSL.createWorkingStorageLocation(Blob.class, mRequestContext.getContextUElem(), true);
    LOBWorkDoc<Blob> lWorkDoc = lWFSL.createWorkDoc(true);

    UCon lUCon = mRequestContext.getContextUCon().getUCon("Blob SL Read");
    Track.pushInfo("PluginBlobRead");
    try {
      lWorkDoc.open(lUCon);
      InputStream lBinaryStream = lWorkDoc.getLOB().getBinaryStream();
      //Delegate input stream reading to the consumer
      pConsumer.consume(lBinaryStream);

      lWorkDoc.close(lUCon);
      lBinaryStream.close();
    }
    catch (IOException | SQLException e) {
      throw new ExInternal("Failed to read BLOB binary stream",e);
    }
    finally {
      mRequestContext.getContextUCon().returnUCon(lUCon, "Blob SL Read");
      Track.pop("PluginBlobRead");
    }
  }

  private void readClobStorageLocation(FileStorageLocation pFSL, FxpReaderConsumer pConsumer) {

    WorkingFileStorageLocation<Clob> lWFSL = pFSL.createWorkingStorageLocation(Clob.class, mRequestContext.getContextUElem(), true);
    LOBWorkDoc<Clob> lWorkDoc = lWFSL.createWorkDoc(true);

    UCon lUCon = mRequestContext.getContextUCon().getUCon("Clob SL Read");
    Track.pushInfo("PluginClobRead");
    try {
      lWorkDoc.open(lUCon);
      Reader lReader = lWorkDoc.getLOB().getCharacterStream();
      //Delegate character reading to the consumer
      pConsumer.consume(lReader);

      lWorkDoc.close(lUCon);
      lReader.close();
    }
    catch (IOException | SQLException e) {
      throw new ExInternal("Failed to read BLOB binary stream",e);
    }
    finally {
      mRequestContext.getContextUCon().returnUCon(lUCon, "Clob SL Read");
      Track.pop("PluginClobRead");
    }
  }

  @Override
  public void readStorageLocation(String pStorageLocationName, FxpConsumer pConsumer) {

    FileStorageLocation lFSL = mRequestContext.getCurrentModule().getFileStorageLocation(pStorageLocationName);

    if(pConsumer instanceof FxpInputStreamConsumer) {
      readBlobStorageLocation(lFSL, (FxpInputStreamConsumer) pConsumer);
    }
    else if(pConsumer instanceof FxpReaderConsumer) {
      readClobStorageLocation(lFSL, (FxpReaderConsumer) pConsumer);
    }
    else {
      throw new ExInternal("Don't know how to read a " + pConsumer.getClass().getName());
    }
  }

  @Override
  public FxpDOM readXMLStorageLocation(String pStorageLocationName) {

    Track.pushInfo("PluginXMLRead");
    try {
      DataDOMStorageLocation lDOMStorageLocation;
      try {
        lDOMStorageLocation = mRequestContext.getCurrentModule().getDataStorageLocation(pStorageLocationName);
      }
      catch (ExModule e) {
        throw new ExInternal("Failed to locate storage location " + pStorageLocationName, e);
      }

      WorkingDataDOMStorageLocation lWSL = lDOMStorageLocation.createWorkingStorageLocation(mRequestContext.getContextUElem(), mRequestContext.getCurrentCallId(), SyncMode.UNSYNCHRONISED);
      XMLWorkDoc lWorkDoc = XMLWorkDoc.getOrCreateXMLWorkDoc(lWSL, false);

      DOM lDOM;
      lWorkDoc.open(mRequestContext.getContextUCon(), false);
      try {
        lDOM = lWorkDoc.getDOM();
      }
      finally {
        lWorkDoc.close(mRequestContext.getContextUCon());
      }

      return lDOM;
    }
    finally {
      Track.pop("PluginXMLRead");
    }
  }

  private void writeToDestination(GeneratorDestination pGeneratorDestination, final FxpGenerator pPluginGenerator) {

    if(pPluginGenerator instanceof FxpOutputStreamGenerator) {
      pGeneratorDestination.generateToOutputStream(mRequestContext, new OutputStreamGeneratorAdaptor((FxpOutputStreamGenerator) pPluginGenerator));
    }
    else if(pPluginGenerator instanceof FxpWriterGenerator) {
      pGeneratorDestination.generateToWriter(mRequestContext, new WriterGeneratorAdaptor((FxpWriterGenerator) pPluginGenerator));
    }
    else if(pPluginGenerator instanceof FxpDOMGenerator) {
      pGeneratorDestination.generateToDOM(mRequestContext, new DOMGeneratorAdaptor((FxpDOMGenerator) pPluginGenerator));
    }
    else {
      throw new ExInternal("Don't know how to handle a " + pPluginGenerator.getClass().getName());
    }
  }

  @Override
  public void writeToDownload(String pFileName, String pContentType, String pServeAsAttachmentXPath, FxpGenerator pGenerator) {

    Track.pushInfo("PluginWriteDownload");
    try {
      DownloadGeneratorDestination lDestination = new DownloadGeneratorDestination(pFileName, pContentType, pServeAsAttachmentXPath, "false()", "00:00");
      writeToDestination(lDestination, pGenerator);
    }
    finally {
      Track.pop("PluginWriteDownload");
    }
  }

  @Override
  public void writeToStorageLocation(String pStorageLocationName, final FxpGenerator pGenerator) {
    Track.pushInfo("PluginWriteToSL");
    try {
      StorageLocationGeneratorDestination lDestination = new StorageLocationGeneratorDestination(pStorageLocationName);
      writeToDestination(lDestination, pGenerator);
    }
    finally {
      Track.pop("PluginWriteToSL");
    }
  }
}

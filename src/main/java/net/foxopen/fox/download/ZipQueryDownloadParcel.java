package net.foxopen.fox.download;


import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.sql.SQLException;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.download.QueryDownloadResultDeliverer.QueryFile;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;

import org.apache.commons.io.IOUtils;


/**
 * DownloadParcel for a query which returns one or more rows containing either Blobs or a Clobs. The files are zipped into
 * a single file as they are streamed out.
 */
class ZipQueryDownloadParcel 
extends AbstractDownloadParcel {
  
  private static final String ZIP_CONTENT_TYPE = "application/zip";

  private final List<ExecutableQuery> mQueryList;
  private final int mZipCompressionLevel;
  
  private boolean mStreamStarted = false;

  public ZipQueryDownloadParcel(String pParcelId, String pFilename, List<ExecutableQuery> pQueryList, int pZipCompressionLevel) {
    super(pParcelId, pFilename, ZIP_CONTENT_TYPE);
    mQueryList = pQueryList;
    mZipCompressionLevel = pZipCompressionLevel;
  }
    
  private String appendIndexToFilePath(String pFilePath, int pIndex) {
    int lExtIndex = pFilePath.lastIndexOf(".");
    String lExt = "";
    String lFileName = "";
    if (lExtIndex >= 0) {
      lExt = pFilePath.substring(lExtIndex);
      lFileName = pFilePath.substring(0, lExtIndex);
    }
    else {
      lFileName = pFilePath;
    }
    
    return lFileName + "(" + (pIndex) + ")" + lExt;
  }

  @Override
  public void prepareForDownload(UCon pUCon) {
  }
  
  @Override
  public void streamDownload(UCon pUCon, OutputStream pOutputStream) 
  throws IOException {
    
    mStreamStarted = true;
    
    //Run all queries - note the same deliverer is used for each one, as QueryFiles are added to its internal list
    QueryDownloadResultDeliverer lDeliverer = new QueryDownloadResultDeliverer();    
    for(ExecutableQuery lQuery : mQueryList) {
      try {
        lQuery.executeAndDeliver(pUCon, lDeliverer);
      }
      catch (ExDB e) {
        throw new ExInternal("Failed to execute query for ZIP download", e);
      }
    }
    
    List<QueryFile> lQueryFiles = lDeliverer.getQueryFiles();
    
    //Wrap fox output stream with a ZipOutputStream
    ZipOutputStream lZipOutputStream = new ZipOutputStream(pOutputStream);
    lZipOutputStream.setLevel(mZipCompressionLevel);
    
    //Multiset for tracking duplicate file paths
    Multiset lFullPaths = HashMultiset.create();
    
    //Loop through each selected file
    for(QueryFile lQueryFile : lQueryFiles) {
    
      //Process filename, checking for duplicates
      String lPathFile = lQueryFile.getPath() + lQueryFile.getFilename();      
      //Record the unmodified path
      lFullPaths.add(lPathFile);
      
      //Rename the file path to avoid dupes
      int lExistingPathCount = lFullPaths.count(lPathFile);      
      if(lExistingPathCount > 1) {        
        lPathFile = appendIndexToFilePath(lPathFile, lExistingPathCount);
      } 
      
      try {
        //Create new Zip File Entry using name from current StreamParcelInput and add to output stream
        ZipEntry lZipFileEntry = new ZipEntry(lPathFile);
        lZipOutputStream.putNextEntry(lZipFileEntry);
    
        // Stream data from file LOB to response output stream
        InputStream lLOBInputStream = lQueryFile.getLOBAdaptor().getInputStream();
        IOUtils.copy(lLOBInputStream, lZipOutputStream);
        lLOBInputStream.close();

        //Close zip file entry
        lZipOutputStream.closeEntry();
      }
      catch (SQLException e) {
        throw new ExInternal("Failed to get LOB input stream for zip download", e);
      }
    }
    
    // All file entries successfully written so close zip file
    lZipOutputStream.close();
  }
  
  @Override
  public void closeAfterDownload(UCon pUCon) {
  }

  @Override
  public long getFileSizeBytes() {
    //This can't be known until the stream is complete
    return -1L;
  }

  @Override
  public boolean isSerialiseAllowed() {
    return !mStreamStarted;
  }
}

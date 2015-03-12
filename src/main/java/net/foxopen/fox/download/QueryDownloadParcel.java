package net.foxopen.fox.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.sql.SQLException;

import java.util.List;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.download.QueryDownloadResultDeliverer.QueryFile;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;

import org.apache.commons.io.IOUtils;


/**
 * DownloadParcel for a query which returns a single row containing either a Blob or a Clob.
 */
class QueryDownloadParcel
implements DownloadParcel {
  
  private final String mParcelId;
  private final String mStaticFilename;  
  private final ExecutableQuery mQuery;
  
  private QueryFile mQueryFile = null;

  public QueryDownloadParcel(String pParcelId, String pFilename, ExecutableQuery pQuery) {
    mParcelId = pParcelId;
    mStaticFilename = pFilename;
    mQuery = pQuery;
  }

  @Override
  public void prepareForDownload(UCon pUCon) {
    
    //Run the query and retrieve a single row
    QueryDownloadResultDeliverer lDeliverer = new QueryDownloadResultDeliverer();       
    try {
      mQuery.executeAndDeliver(pUCon, lDeliverer);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to execute query for download", e);
    }  
    
    List<QueryFile> lFiles = lDeliverer.getQueryFiles();    
    if(lFiles.size() != 1) {
      throw new ExInternal("Query " +  mQuery.getParsedStatement().getStatementPurpose() + " for file download must have exactly 1 file if not zipped, got " + lFiles.size());
    }
    
    mQueryFile = lFiles.get(0);    
  }
  
  @Override
  public void streamDownload(UCon pUCon, OutputStream pOutputStream) 
  throws IOException{
    if(mQueryFile == null) {
      throw new ExInternal("Expected query to have been executed at this point");
    }

    try {
      InputStream lInputStream = mQueryFile.getLOBAdaptor().getInputStream();
      IOUtils.copy(lInputStream, pOutputStream);
      lInputStream.close();
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to get input stream from LOB in query " + mQuery.getParsedStatement().getStatementPurpose(), e);
    }    
  }

  @Override
  public void closeAfterDownload(UCon pUCon) {
  }

  @Override
  public long getFileSizeBytes() {
    try {
      return mQueryFile.getLOBAdaptor().getLength();
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to get length of file in query " + mQuery.getParsedStatement().getStatementPurpose(), e);
    }
  }

  @Override
  public boolean isSerialiseAllowed() {
    return mQueryFile == null;
  }

  @Override
  public String getParcelId() {
    return mParcelId;
  }

  @Override
  public String getFilename() {
    //Give preference to the queried filename than the static one specified when this object was created
    return XFUtil.nvl(mQueryFile != null ? mQueryFile.getFilename() : null, mStaticFilename);
  }

  @Override
  public String getContentType() {
    return  mQueryFile.getContentType();
  }
}

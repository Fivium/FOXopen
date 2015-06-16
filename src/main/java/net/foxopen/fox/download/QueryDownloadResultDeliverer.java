package net.foxopen.fox.download;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.SingleRowResultDeliverer;
import net.foxopen.fox.database.sql.out.LOBAdaptor;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Deliverer for query-based downloads which require either Blobs or Clobs to be returned. File data and metadata is
 * retrieved from columns with known names. The results can be retrieved as a list of QueryFiles. Each query row must
 * contain a non-null Blob or Clob column and a non-null filename column. Delivery will fail if this is not the case.
 */
class QueryDownloadResultDeliverer
implements QueryResultDeliverer {

  private static final String COL_FILENAME = "FILENAME";
  private static final String COL_PATH     = "PATH";
  private static final String COL_BLOB     = "BLOB";
  private static final String COL_CLOB     = "CLOB";
  private static final String COL_MIMETYPE = "MIMETYPE";

  private static final String MIMETYPE_OCTET_STREAM = "application/octet-stream";

  private final List<QueryFile> mResultFiles = new ArrayList<>();

  QueryDownloadResultDeliverer() {}

  private String cleanFilePath(String pPath) {
    if (pPath == null || pPath.length() == 0 || pPath.equals("\\")) {
      return "";
    }
    //Replace windows path seperators to unix
    pPath = pPath.replace("/", "\\");

    if (!pPath.endsWith("\\")) {
      return pPath+"\\";
    }
    else {
      return pPath;
    }
  }

  @Override
  public void deliver(ExecutableQuery pQuery)
  throws ExDB {

    try {
      ResultSet lResultSet = pQuery.getResultSet();
      Set<String> lAvailableColumns = null;

      int lRow = 0;
      while(lResultSet.next()) {

        if(lRow == 0) {
          lAvailableColumns = SingleRowResultDeliverer.createObjectMapFromResultSet(lResultSet).keySet();
        }
        lRow++;

        if(!lAvailableColumns.contains(COL_FILENAME)) {
          throw new ExInternal("Mandatory '" + COL_FILENAME + "' column not found in download query " + pQuery.getParsedStatement().getStatementPurpose());
        }
        String lFilename = lResultSet.getString(COL_FILENAME);
        if (XFUtil.isNull(lFilename)) {
          throw new ExInternal("Filename for row " + lRow + " was null in query " + pQuery.getParsedStatement().getStatementPurpose());
        }

        String lPath = "";
        if(lAvailableColumns.contains(COL_PATH)) {
          lPath = cleanFilePath(lResultSet.getString(COL_PATH));
        }

        String lMimeType = MIMETYPE_OCTET_STREAM;
        if(lAvailableColumns.contains(COL_MIMETYPE)) {
          lMimeType = XFUtil.nvl(lResultSet.getString(COL_MIMETYPE), MIMETYPE_OCTET_STREAM);
        }

        Blob lBlob = null;
        if(lAvailableColumns.contains(COL_BLOB)) {
          lBlob = lResultSet.getBlob(COL_BLOB);
        }

        Clob lClob = null;
        if(lAvailableColumns.contains(COL_CLOB)) {
          lClob = lResultSet.getClob(COL_CLOB);
        }

        if (lClob == null && lBlob == null) {
          throw new ExInternal("Download query error. Blob and Clob both return null in query " +  pQuery.getParsedStatement().getStatementPurpose() + "." +
            "Make sure the query returns a column called " + COL_BLOB + " or " + COL_CLOB + " and it is not null.");
        }
        else if (lClob != null && lBlob != null){
          throw new ExInternal("Download query error. Blob and Clob return a value in query " +  pQuery.getParsedStatement().getStatementPurpose() + "." +
            "Make sure the query returns a value in either column and not both.");
        }
        else {
          //Construct a new result wrapper and add it to the list
          mResultFiles.add(new QueryFile(lFilename, lPath, lMimeType, LOBAdaptor.getAdaptor(lBlob != null ? lBlob : lClob)));
        }
      }
    }
    catch (SQLException e) {
      pQuery.convertErrorAndThrow(e);
    }
  }

  public List<QueryFile> getQueryFiles() {
    return mResultFiles;
  }

  @Override
  public boolean closeStatementAfterDelivery() {
    return true;
  }

  /**
   * Encapsulation of a successfully parsed query row.
   */
  public static class QueryFile {
    private final String mFilename;
    private final String mPath;
    private final String mContentType;
    private final LOBAdaptor mLOBAdaptor;

    QueryFile(String pFilename, String pPath, String pContentType, LOBAdaptor pLOBAdaptor) {
      mFilename = pFilename;
      mPath = pPath;
      mContentType = pContentType;
      mLOBAdaptor = pLOBAdaptor;
    }

    public String getFilename() {
      return mFilename;
    }

    /**
     * Path if specified or empty string if not.
     * @return
     */
    public String getPath() {
      return mPath;
    }

    /**
     * Gets the content type as queried, or application/octet-stream if it was null.
     * @return
     */
    public String getContentType() {
      return mContentType;
    }

    public LOBAdaptor getLOBAdaptor() {
      return mLOBAdaptor;
    }
  }
}

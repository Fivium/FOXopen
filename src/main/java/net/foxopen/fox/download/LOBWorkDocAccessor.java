package net.foxopen.fox.download;

import java.io.InputStream;

import java.sql.SQLException;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.storage.lob.LOBWorkDoc;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;

import oracle.sql.Datum;


/**
 * Encapsulation of shared WorkDoc InputStream retrieval behaviour. This class handles Oracle Blobs and Clobs, which share
 * the same Datum interface. Ideally this class would operate on standard JDBC Clobs and Blobs using a LOBAdaptor to retrieve
 * the InputStream.
 */
class LOBWorkDocAccessor {

  private final LOBWorkDoc<Datum> mWorkDoc;

  private static final ParsedStatement LOB_SIZE_PARSED_STATEMENT = StatementParser.parseSafely("BEGIN :size := dbms_lob.getlength(:2); END;", "Get LOB size");

  LOBWorkDocAccessor(UCon pUCon, WorkingFileStorageLocation pWorkingStorageLocation) {
    //Type safety - WFSL should be for an Oracle CLOB or BLOB so this is OK for now. Ideally this would be a LOBWorkDoc<LOBAdaptor> so type safety is preserved.
    mWorkDoc = pWorkingStorageLocation.createWorkDoc(true);
    mWorkDoc.open(pUCon);
  }

  public InputStream getInputStream() {
    try {
      return mWorkDoc.getLOB().binaryStreamValue();
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to deliver WFSL download parcel", e);
    }
  }

  public long getLOBSize(UCon pUCon) {
    try {
      //Make sure the LOB is bound in as an "uncloseable" bind so it's kept open for further reads and not closed as part of statement execution
      UConStatementResult lResult = pUCon.executeAPI(LOB_SIZE_PARSED_STATEMENT, UCon.bindOut(BindSQLType.NUMBER), UCon.bindUncloseableObject(mWorkDoc.getLOB()));
      return lResult.getLong(":size");
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to retrieve size of LOB", e);
    }
  }

  public void close(UCon pUCon) {
    if(mWorkDoc != null) {
      mWorkDoc.close(pUCon);
    }
  }
}

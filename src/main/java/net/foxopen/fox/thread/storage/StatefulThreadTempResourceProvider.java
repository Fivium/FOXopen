package net.foxopen.fox.thread.storage;

import com.esotericsoftware.kryo.io.Input;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.storage.lob.LOBWorkDoc;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.persistence.DatabaseSerialiser;
import net.foxopen.fox.thread.persistence.kryo.KryoManager;
import net.foxopen.fox.track.Track;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * TempResourceProvider for a single XThread.
 */
public class StatefulThreadTempResourceProvider
implements TempResourceProvider {

  //Note: as many static members as possible to prevent serialisation of redundant data
  private static final String STORAGE_LOCATION_NAME = "THREAD_TEMP_RESOURCE";
  private static final String LOB_TYPE_BIND = "!!LOB_TYPE!!";

  private static final String TEMP_SELECT = "SELECT data_" + LOB_TYPE_BIND + " FROM ${schema.fox}.fox_temp_resources WHERE id = :1 FOR UPDATE OF data_" + LOB_TYPE_BIND + " NOWAIT";
  private static final String TEMP_UPDATE = "UPDATE ${schema.fox}.fox_temp_resources SET last_updated_datetime = SYSDATE WHERE id = :1";
  private static final String TEMP_INSERT = "INSERT INTO ${schema.fox}.fox_temp_resources (\n" +
  "  id\n" +
  ", data_" + LOB_TYPE_BIND + "\n" +
  ", created_datetime\n" +
  ", last_updated_datetime\n" +
  ")\n" +
  "VALUES (\n" +
  "  :1\n" +
  ", empty_" + LOB_TYPE_BIND + "()\n" +
  ", SYSDATE\n" +
  ", SYSDATE\n" +
  ")";

  private static final Map<Class, FileStorageLocation> gStorageLocationMap = new HashMap<>(2);

  static {
    gStorageLocationMap.put(Clob.class, new FileStorageLocation(STORAGE_LOCATION_NAME, createStatementMap("clob")));
    gStorageLocationMap.put(Blob.class, new FileStorageLocation(STORAGE_LOCATION_NAME, createStatementMap("blob")));
  }

  public StatefulThreadTempResourceProvider() {}

  private static Map<StatementType, DatabaseStatement> createStatementMap(String pLOBBindType) {

    Map<StatementType, DatabaseStatement> lStatementMap = new HashMap<>();

    //Set up bind list for built-in queries - insert, update and select all use the unique key in position 1, so the same
    //bind list can be used for all 3.
    List<StorageLocationBind> lBindList = new ArrayList<>();
    lBindList.add(new StorageLocationBind(0, UsingType.UNIQUE, null));

    //Replace lob type bind string with CLOB or BLOB as appropriate (only CLOB supported currently)
    String lSelectSQL = TEMP_SELECT.replaceAll(LOB_TYPE_BIND, pLOBBindType);
    lSelectSQL = SQLManager.replaceSQLSubstitutionVariables(lSelectSQL);
    lStatementMap.put(StatementType.QUERY, DatabaseStatement.createInternalStatement(lSelectSQL, lBindList, StatementType.QUERY, STORAGE_LOCATION_NAME));

    String lInsertSQL = TEMP_INSERT.replaceAll(LOB_TYPE_BIND, pLOBBindType);
    lInsertSQL = SQLManager.replaceSQLSubstitutionVariables(lInsertSQL);
    lStatementMap.put(StatementType.INSERT, DatabaseStatement.createInternalStatement(lInsertSQL, lBindList, StatementType.INSERT, STORAGE_LOCATION_NAME));

    String lUpdateSQL = SQLManager.replaceSQLSubstitutionVariables(TEMP_UPDATE);
    lStatementMap.put(StatementType.UPDATE, DatabaseStatement.createInternalStatement(lUpdateSQL, lBindList, StatementType.UPDATE, STORAGE_LOCATION_NAME));

    return lStatementMap;
  }

  public FileStorageLocation getTempStorageLocationForLOBType(Class pLOBType) {
    FileStorageLocation lStorageLocation = gStorageLocationMap.get(pLOBType);
    if(lStorageLocation == null) {
      throw new ExInternal("Cannot provide a storage location for " + pLOBType.getClass().getName());
    }
    return lStorageLocation;
  }

  @Override
  public TempResource<Clob> getClobTempResource() {
    return new TempResource<>(Clob.class, XFUtil.unique(), this);
  }

  @Override
  public TempResource<Blob> getBlobTempResource() {
    return new TempResource<>(Blob.class, XFUtil.unique(), this);
  }

  @Override
  public TempResource<Blob> getBlobTempResource(String pResourceId) {
    return new TempResource<>(Blob.class, pResourceId, this);
  }

  @Override
  public TempResource<?> createTempResource(ActionRequestContext pRequestContext, String pResourceId, TempResourceGenerator pGenerator, boolean pCache) {
    TempResource<Blob> lBlobTempResource = getBlobTempResource(pResourceId);

    //Cache the new resource if required
    if(pCache) {
      CacheManager.getCache(BuiltInCacheDefinition.THREAD_TEMP_RESOURCES).put(pResourceId, pGenerator);
    }

    UCon lUCon = pRequestContext.getContextUCon().getUCon("TempResourceCreate");
    try {
      LOBWorkDoc<Blob> lWorkDoc = lBlobTempResource.createWorkDoc(false);
      try {
        lWorkDoc.open(lUCon);
        //TODO PN shouldn't have a hardcoded reference to the database here, should be serialiser agnsotic
        DatabaseSerialiser.writeObjectToBlob(pGenerator, lWorkDoc.getLOB(), "TempResourceGenerator", true);
        lWorkDoc.close(lUCon);
      }
      catch (SQLException e) {
        throw new ExInternal("Failed to open CLOB stream", e);
      }

    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "TempResourceCreate");
    }

    return lBlobTempResource;
  }

  @Override
  public TempResourceGenerator getExistingResourceGenerator(RequestContext pRequestContext, String pResourceId) {

    //Look up in the local cache first
    TempResourceGenerator lGenerator = CacheManager.<String, TempResourceGenerator>getCache(BuiltInCacheDefinition.THREAD_TEMP_RESOURCES).get(pResourceId);
    if(lGenerator != null) {
      Track.info("TempResourceCacheHit");
      return lGenerator;
    }
    else {
      //Go to the database via the TempResource API if we found nothing
      Track.info("TempResourceCacheMiss");

      TempResource<Blob> lTempResource = getBlobTempResource(pResourceId);
      LOBWorkDoc<Blob> lWorkDoc = lTempResource.createWorkDoc(true);

      UCon lUCon = pRequestContext.getContextUCon().getUCon("TempResourceSelect");

      Track.pushInfo("TempResourceDeserialise");
      try {
        lWorkDoc.open(lUCon);
        Input lInput = new Input(lWorkDoc.getLOB().getBinaryStream());
        lGenerator = (TempResourceGenerator) KryoManager.getKryoInstance().readClassAndObject(lInput);
        lInput.close();
        lWorkDoc.close(lUCon);
      }
      catch (SQLException e) {
        throw new ExInternal("Failed to get binary stream from temp resource", e);
      }
      finally {
        Track.pop("TempResourceDeserialise");
        pRequestContext.getContextUCon().returnUCon(lUCon, "TempResourceSelect");
      }

      return lGenerator;
    }
  }
}

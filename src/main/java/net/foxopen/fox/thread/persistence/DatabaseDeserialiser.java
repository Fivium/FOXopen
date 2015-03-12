package net.foxopen.fox.thread.persistence;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConResultSet;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.download.DownloadParcel;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.facet.ModuleFacet;
import net.foxopen.fox.module.facet.ModuleFacetType;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.ThreadPropertyMap;
import net.foxopen.fox.thread.persistence.data.InternalDOMPersistedData;
import net.foxopen.fox.thread.persistence.data.ModuleCallPersistedData;
import net.foxopen.fox.thread.persistence.data.StateCallPersistedData;
import net.foxopen.fox.thread.persistence.data.StatefulXThreadPersistedData;
import net.foxopen.fox.thread.persistence.kryo.KryoManager;
import net.foxopen.fox.thread.persistence.xstream.XStreamManager;
import net.foxopen.fox.thread.stack.callback.CallbackHandler;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;
import net.foxopen.fox.track.Track;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class DatabaseDeserialiser
implements Deserialiser {

  private static final String SELECT_XTHREAD_FILENAME = "SelectXThread.sql";
  private static final String SELECT_MODULE_CALLS_FILENAME = "SelectModuleCalls.sql";
  private static final String SELECT_STATE_CALLS_FILENAME = "SelectStateCalls.sql";
  private static final String SELECT_INTERNAL_DOM_FILENAME = "SelectInternalDOM.sql";
  private static final String SELECT_DOWNLOAD_PARCEL_FILENAME = "SelectDownloadParcel.sql";
  private static final String SELECT_MODULE_CALL_FACETS_FILENAME = "SelectModuleCallFacets.sql";

  private final PersistenceContext mPersistenceContext;
  private final ContextUCon mContextUCon;

  public DatabaseDeserialiser(PersistenceContext pPersistenceContext, ContextUCon pContextUCon) {
    mPersistenceContext = pPersistenceContext;
    mContextUCon = pContextUCon;
  }

  private static final <T> T kryoDeserialise(Class<T> pObjectClass, UConStatementResult lSelectXThreadResult, String pColumnName, String pDescription, String pThreadId) {

    T lResult;
    Track.pushDebug(pDescription + "Deserialise");
    try {
      Kryo lKryo = KryoManager.getKryoInstance();

      Blob lBlob = lSelectXThreadResult.getBlob(pColumnName);
      if(lBlob.length() > 0) {
        Input lInput = new Input(lBlob.getBinaryStream());
        lResult = lKryo.readObject(lInput, pObjectClass);
        lInput.close();

        return lResult;
      }
      else {
        return null;
      }
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to retrieve " + pDescription + " for thread " + pThreadId, e);
    }
    finally {
      Track.pop(pDescription + "Deserialise");
    }
  }

  @Override
  public StatefulXThreadPersistedData getXThreadPersistedData(String pThreadId) {

    Track.pushInfo("SelectXThread", "Select thread " + pThreadId);
    try {
      UConStatementResult lSelectXThreadResult;
      ThreadPropertyMap lThreadPropertyMap;
      FieldSet lFieldSet;
      AuthenticationContext lAuthContext;

      UCon lUCon = mContextUCon.getUCon("Select XThread");
      try {
        lSelectXThreadResult = lUCon.querySingleRow(SQLManager.instance().getStatement(SELECT_XTHREAD_FILENAME, getClass()), pThreadId);

        Clob lAuthContextClob = lSelectXThreadResult.getClob("AUTHENTICATION_CONTEXT");

        Track.pushDebug("AuthContextDeserialise");
        try {
          lAuthContext = (AuthenticationContext) XStreamManager.getXStream().fromXML(lAuthContextClob.getCharacterStream());
        }
        catch (SQLException e) {
          throw new ExInternal("Failed to retrieved authentication context for thread " + pThreadId, e);
        }
        finally {
          Track.pop("AuthContextDeserialise");
        }

        lThreadPropertyMap = kryoDeserialise(ThreadPropertyMap.class, lSelectXThreadResult, "THREAD_PROPERTY_MAP", "ThreadPropertyMap", pThreadId);
        lFieldSet = kryoDeserialise(FieldSet.class, lSelectXThreadResult, "FIELD_SET", "FieldSet", pThreadId);
      }
      catch (ExDB e) {
        throw new ExInternal("Failed to retrieved thread " + pThreadId, e);
      }
      finally {
        mContextUCon.returnUCon(lUCon, "Select XThread");
      }

      return new StatefulXThreadPersistedData(
        pThreadId
      , lSelectXThreadResult.getString("APP_MNEM")
      , lSelectXThreadResult.getString("USER_THREAD_SESSION_ID")
      , lThreadPropertyMap
      , lFieldSet
      , lAuthContext
      , lSelectXThreadResult.getString("CHANGE_NUMBER")
      , lSelectXThreadResult.getString("FOX_SESSION_ID")
      );
    }
    finally {
      Track.pop("SelectXThread");
    }

  }

  @Override
  public List<ModuleCallPersistedData> getModuleCallPersistedData(String pThreadId) {

    Track.pushInfo("SelectModuleCalls", "Select module calls for thread " + pThreadId);
    try {
      List<ModuleCallPersistedData> lModuleCallDataList = new ArrayList<>();

      try {
        UCon lUCon = mContextUCon.getUCon("Select Module Calls");
        try {
          UConResultSet lUConResultSet = lUCon.queryResultSet(SQLManager.instance().getStatement(SELECT_MODULE_CALLS_FILENAME, getClass()), pThreadId);
          try {
            ResultSet lResultSet = lUConResultSet.getResultSet();
            while (lResultSet.next()) {

              Clob lStorageLocationClob = lResultSet.getClob("STORAGE_LOCATIONS");
              Clob lCallbackHandlerClob = lResultSet.getClob("CALLBACK_HANDLERS");
              Clob lSecurityScopeClob = lResultSet.getClob("SECURITY_SCOPE");

              Map<String, WorkingDataDOMStorageLocation> lLabelToStorageLocationMap = (Map<String, WorkingDataDOMStorageLocation>) XStreamManager.getXStream().fromXML(lStorageLocationClob.getCharacterStream());
              List<CallbackHandler> lCallbackHandlers = (List<CallbackHandler>) XStreamManager.getXStream().fromXML(lCallbackHandlerClob.getCharacterStream());
              SecurityScope lSecurityScope = (SecurityScope) XStreamManager.getXStream().fromXML(lSecurityScopeClob.getCharacterStream());

              ModuleCallPersistedData lModuleCallData = new ModuleCallPersistedData (
                lResultSet.getString("CALL_ID")
              , lResultSet.getInt("STACK_POSITION")
              , lResultSet.getString("APP_MNEM")
              , lResultSet.getString("MODULE_NAME")
              , lResultSet.getString("THEME_NAME")
              , lLabelToStorageLocationMap
              , lCallbackHandlers
              , lSecurityScope
              );

              lModuleCallDataList.add(lModuleCallData);
            }
          }
          catch (SQLException e) {
            throw new ExInternal("Failed to read module call query result for thread " + pThreadId, e);
          }
          finally {
            lUConResultSet.close();
          }
        }
        finally {
          mContextUCon.returnUCon(lUCon, "Select Module Calls");
        }
      }
      catch (ExDB e) {
        throw new ExInternal("Failed to query module calls for thread " + pThreadId, e);
      }

      return lModuleCallDataList;
    }
    finally {
      Track.pop("SelectModuleCalls");
    }
  }

  @Override
  public List<StateCallPersistedData> getStateCallPersistedData(String pModuleCallId) {

    Track.pushInfo("SelectStateCalls", "Select state calls for module call " + pModuleCallId);
    try {
      List<StateCallPersistedData> lStateCallDataList = new ArrayList<>();

      UCon lUCon = mContextUCon.getUCon("Select State Calls");
      try {
        UConResultSet lUConResultSet = lUCon.queryResultSet(SQLManager.instance().getStatement(SELECT_STATE_CALLS_FILENAME, getClass()), pModuleCallId);
        try {
          ResultSet lResultSet = lUConResultSet.getResultSet();

          while (lResultSet.next()) {
            StateCallPersistedData lStateCallData = new StateCallPersistedData (
              lResultSet.getString("CALL_ID")
            , lResultSet.getInt("STACK_POSITION")
            , lResultSet.getString("STATE_NAME")
            , lResultSet.getInt("SCROLL_POSITION")
            , (Collection<ContextUElem.SerialisedLabel>) XStreamManager.getXStream().fromXML(lResultSet.getString("CONTEXT_LABELS"))
            );

            lStateCallDataList.add(lStateCallData);
          }
        }
        catch (SQLException e) {
          throw new ExInternal("Failed to query state calls for module call " + pModuleCallId, e);
        }
        finally {
          lUConResultSet.close();
        }
      }
      finally {
        mContextUCon.returnUCon(lUCon, "Select State Calls");
      }

      return lStateCallDataList;
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to query state calls for module call " + pModuleCallId, e);
    }
    finally {
      Track.pop("SelectStateCalls");
    }
  }

  @Override
  public InternalDOMPersistedData getInternalDOMPersistedData(String pModuleCallId, String pDocumentName) {

    Track.pushInfo("SelectInternalDOM", "Select " + pDocumentName + " DOM for module call " + pModuleCallId);
    try {
      UConStatementResult lResultRow;
      UCon lUCon = mContextUCon.getUCon("Select Internal DOM");
      try {
        lResultRow = lUCon.querySingleRow(SQLManager.instance().getStatement(SELECT_INTERNAL_DOM_FILENAME, getClass()), pModuleCallId, pDocumentName);
        return new InternalDOMPersistedData(pModuleCallId, pDocumentName, lResultRow.getDOMFromSQLXML("XML_DATA", true));
      }
      catch (ExDBTooFew e) {
        //Nothing exists - assume DOM was never serialised and let the consumer deal with it and bootstrap a new one
        return null;
      }
      catch (Throwable th) {
        throw new ExInternal("Failed to retrieve DOM " + pDocumentName + " for module " + pModuleCallId, th);
      }
      finally {
        mContextUCon.returnUCon(lUCon, "Select Internal DOM");
      }
    }
    finally {
      Track.pop("SelectInternalDOM");
    }

  }

  @Override
  public DownloadParcel getDownloadParcel(String pDownloadParcelId) {

    String lThreadId = mPersistenceContext.getThreadId();
    Track.pushInfo("SelectDownloadParcel", "Select download parcel " + pDownloadParcelId + " for thread " + lThreadId);
    try {
      UConStatementResult lResultRow;
      UCon lUCon = mContextUCon.getUCon("Select Download Parcel");
      try {
        lResultRow = lUCon.querySingleRow(SQLManager.instance().getStatement(SELECT_DOWNLOAD_PARCEL_FILENAME, getClass()), pDownloadParcelId, lThreadId);

        return (DownloadParcel) XStreamManager.getXStream().fromXML(lResultRow.getClob("DATA_CLOB").getCharacterStream());
      }
      catch (ExDBTooFew e) {
        //Nothing exists - let the consumer deal with it
        return null;
      }
      catch (ExDB | SQLException e) {
        throw new ExInternal("Failed to retrieve download parcel " + pDownloadParcelId + " for thread " + lThreadId, e);
      }
      finally {
        mContextUCon.returnUCon(lUCon, "Select Download Parcel");
      }
    }
    finally {
      Track.pop("SelectDownloadParcel");
    }
  }

  @Override
  public <T extends ModuleFacet> Collection<T> getModuleCallFacets(String pModuleCallId, ModuleFacetType pFacetType, Class<T> pFacetClass) {

    String lFacetType = pFacetType.toString();

    Track.pushInfo("SelectFacets", "Select pagers for module call " + pModuleCallId + ", type " + lFacetType);
    try {
      Kryo lKryo = KryoManager.getKryoInstance();

      UCon lUCon = mContextUCon.getUCon("Select Module Call Facets");
      try {
        List<UConStatementResult> lResultRows = lUCon.queryMultipleRows(SQLManager.instance().getStatement(SELECT_MODULE_CALL_FACETS_FILENAME, getClass()), lFacetType, pModuleCallId);
        List<T> lFacetList = new ArrayList<>(lResultRows.size());

        for(UConStatementResult lRow : lResultRows) {
          Input lInput = new Input(lRow.getBlob("FACET_OBJECT").getBinaryStream());

          //Do NOT tell Kryo the class, we may have been passed an abstract or interface - instead let Kryo work out the original class itself
          Object lFacetObject = lKryo.readClassAndObject(lInput);

          //Give objects an oppurtunity to do any post-deserialisation initialisation, i.e. for transient fields
          if(lFacetObject instanceof DeserialisationHandler) {
            ((DeserialisationHandler) lFacetObject).handleDeserialisation();
          }

          if(pFacetClass.isInstance(lFacetObject)) {
            lFacetList.add(pFacetClass.cast(lFacetObject));
          }
          else {
            throw new ExInternal("Class of retrieved facet object " + lFacetObject.getClass().getName() + " is not an instace of " + pFacetClass.getName());
          }

          lInput.close();
        }

        return lFacetList;
      }
      catch (ExDB | SQLException e) {
        throw new ExInternal("Failed to retrieve pagers for module call " + pModuleCallId, e);
      }
      finally {
        mContextUCon.returnUCon(lUCon, "Select Module Call Facets");
      }
    }
    finally {
      Track.pop("SelectFacets");
    }
  }
}

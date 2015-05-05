package net.foxopen.fox.thread.persistence;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.sql.bind.DOMBindObject;
import net.foxopen.fox.database.xml.BinaryXMLWriter;
import net.foxopen.fox.database.xml.XMLWriterStrategy;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.download.DownloadParcel;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.facet.ModuleFacet;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.ThreadPropertyMap;
import net.foxopen.fox.thread.persistence.kryo.KryoManager;
import net.foxopen.fox.thread.persistence.xstream.XStreamManager;
import net.foxopen.fox.thread.stack.callback.CallbackHandler;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;
import net.foxopen.fox.track.Track;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class DatabaseSerialiser
implements Serialiser {

  private static final String DELETE_MODULE_CALL_FILENAME = "DeleteModuleCall.sql";
  private static final String DELETE_MODULE_CALL_FACETS_FILENAME = "DeleteModuleCallFacets.sql";
  private static final String DELETE_STATE_CALL_FILENAME = "DeleteStateCall.sql";
  private static final String DELETE_STATE_CALLSTACK_FILENAME = "DeleteStateCallStack.sql";
  private static final String INSERT_DOWNLOAD_PARCEL_FILENAME = "InsertDownloadParcel.sql";
  private static final String INSERT_MODULE_CALL_FILENAME = "InsertModuleCall.sql";
  private static final String INSERT_MODULE_FACET_FILENAME = "InsertModuleFacet.sql";
  private static final String INSERT_STATE_CALL_FILENAME = "InsertStateCall.sql";
  private static final String INSERT_THREAD_FILENAME = "InsertThread.sql";
  private static final String INSERT_USER_THREAD_SESSION_FILENAME = "InsertUserThreadSession.sql";
  private static final String MERGE_INTERNAL_DOM_FILENAME = "MergeInternalDOM.sql";
  private static final String UPDATE_MODULE_CALL_FILENAME = "UpdateModuleCall.sql";
  private static final String UPDATE_MODULE_FACET_FILENAME = "UpdateModuleFacet.sql";
  private static final String UPDATE_STATE_CALL_FILENAME = "UpdateStateCall.sql";
  private static final String UPDATE_THREAD_FILENAME = "UpdateThread.sql";

  private final PersistenceContext mPersistenceContext;
  private final UCon mUCon;

  static String getFacetTypeName(ModuleFacet pModuleFacet) {
    return pModuleFacet.getFacetType().toString();
  }

  //TODO PN this might need to be configurable, and needs to be thoroughly tested
  private final XMLWriterStrategy mInternalDOMWriterStrategy = BinaryXMLWriter.instance();
  DatabaseSerialiser(PersistenceContext pPersistenceContext, UCon pUCon) {
    mPersistenceContext = pPersistenceContext;
    mUCon = pUCon;
  }

  public static void writeObjectToBlob(Object pObject, Blob pBlob, String pObjectName, boolean pWriteClass)
  throws SQLException {
    Track.pushInfo(pObjectName + "Write");
    try {
      Kryo lKryo = KryoManager.getKryoInstance();

      Output lOutput = new Output(pBlob.setBinaryStream(1));
      if(pWriteClass) {
        lKryo.writeClassAndObject(lOutput, pObject);
      }
      else {
        lKryo.writeObject(lOutput, pObject);
      }

      lOutput.close();
    }
    finally {
      Track.pop(pObjectName + "Write");
    }
  }

  @Override
  public void createUserThreadSession(String pSessionId) {
    Track.pushInfo("InsertUserThreadSession", "Insert session " + pSessionId);
    try {
      mUCon.executeAPI(SQLManager.instance().getStatement(INSERT_USER_THREAD_SESSION_FILENAME, getClass()), pSessionId);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to insert user thread session", e);
    }
    finally {
      Track.pop("InsertUserThreadSession");
    }
  }

  @Override
  public void createThread(String pThreadId, String pAppMnem, String pUserThreadSessionId,  ThreadPropertyMap pThreadPropertyMap,
                           FieldSet pFieldSet, AuthenticationContext pAuthenticationContext, String pChangeNumber, String pFoxSessionID) {

    Track.pushInfo("InsertThread", "Insert thread " + pThreadId);
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":thread_id", pThreadId);
      lBindMap.defineBind(":app_mnem", pAppMnem);
      lBindMap.defineBind(":user_thread_session_id", pUserThreadSessionId);
      lBindMap.defineBind(":change_number", pChangeNumber);
      lBindMap.defineBind(":authentication_context", XStreamManager.serialiseObjectToXMLString(pAuthenticationContext));
      lBindMap.defineBind(":fox_session_id", pFoxSessionID);

      lBindMap.defineBind(":field_set_blob", UCon.bindOutBlob());
      lBindMap.defineBind(":property_map_blob", UCon.bindOutBlob());

      UConStatementResult lStatementResult = mUCon.executeAPI(SQLManager.instance().getStatement(INSERT_THREAD_FILENAME, getClass()), lBindMap);

      if(pFieldSet != null) {
        //Fieldset can be null for new orphan threads which haven't generated anything yet
        Blob lFieldSetBlob = lStatementResult.getBlob(":field_set_blob");
        writeObjectToBlob(pFieldSet, lFieldSetBlob, "FieldSet", false);
      }

      Blob lPropertyMapBlob = lStatementResult.getBlob(":property_map_blob");
      writeObjectToBlob(pThreadPropertyMap, lPropertyMapBlob, "PropertyMap", false);
    }
    catch (ExDB | SQLException e) {
      throw new ExInternal("Failed to insert thread", e);
    }
    finally {
      Track.pop("InsertThread");
    }
  }

  @Override
  public void updateThread(String pThreadId, FieldSet pFieldSet, AuthenticationContext pAuthenticationContext, ThreadPropertyMap pThreadPropertyMap,
                           String pChangeNumber, String pFoxSessionID) {

    //TODO this always updates the auth context even if it hasn't changed - wasteful?
    Track.pushInfo("UpdateThread", "Update thread " + pThreadId);
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":field_set_blob", UCon.bindOutBlob());
      lBindMap.defineBind(":property_map_blob", UCon.bindOutBlob());
      lBindMap.defineBind(":authentication_context", XStreamManager.serialiseObjectToXMLString(pAuthenticationContext));
      lBindMap.defineBind(":change_number", pChangeNumber);
      lBindMap.defineBind(":thread_id", pThreadId);
      lBindMap.defineBind(":fox_session_id", pFoxSessionID);

      UConStatementResult lStatementResult = mUCon.executeAPI(SQLManager.instance().getStatement(UPDATE_THREAD_FILENAME, getClass()), lBindMap);

      //Field set blob "always" changes
      Blob lFieldSetBlob = lStatementResult.getBlob(":field_set_blob");
      writeObjectToBlob(pFieldSet, lFieldSetBlob, "FieldSet", false);

      //TODO PN conditional update
      Blob lPropertyMapBlob = lStatementResult.getBlob(":property_map_blob");
      writeObjectToBlob(pThreadPropertyMap, lPropertyMapBlob, "PropertyMap", false);
    }
    catch (ExDB | SQLException e) {
      throw new ExInternal("Failed to insert thread", e);
    }
    finally {
      Track.pop("UpdateThread");
    }
  }

  @Override
  public void deleteThread(String pThreadId) {
  }

  @Override
  public void createModuleCall(String pModuleCallId, int pStackPosition, String pAppMnem, String pModuleName, String pEntryThemeName,
                               Map<String, WorkingDataDOMStorageLocation> pLabelToStorageLocationMap, List<CallbackHandler> pCallbackHandlerList, SecurityScope pSecurityScope) {

    Track.pushInfo("InsertModuleCall", "Insert module call with ID " + pModuleCallId + " (" + pModuleName + ")");
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":call_id", pModuleCallId);
      lBindMap.defineBind(":stack_position", pStackPosition);
      lBindMap.defineBind(":thread_id", mPersistenceContext.getThreadId());
      lBindMap.defineBind(":app_mnem", pAppMnem);
      lBindMap.defineBind(":module_name", pModuleName);
      lBindMap.defineBind(":theme_name", pEntryThemeName);
      lBindMap.defineBind(":storage_locations", XStreamManager.serialiseObjectToXMLString(pLabelToStorageLocationMap));
      lBindMap.defineBind(":callback_handlers", XStreamManager.serialiseObjectToXMLString(pCallbackHandlerList));
      lBindMap.defineBind(":security_scope", XStreamManager.serialiseObjectToXMLString(pSecurityScope));

      mUCon.executeAPI(SQLManager.instance().getStatement(INSERT_MODULE_CALL_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to insert thread", e);
    }
    finally {
      Track.pop("InsertModuleCall");
    }
  }

  @Override
  public void updateModuleCall(String pModuleCallId, SecurityScope pSecurityScope) {

    Track.pushInfo("UpdateModuleCall", "Update module call with ID " + pModuleCallId);
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":security_scope", XStreamManager.serialiseObjectToXMLString(pSecurityScope));
      lBindMap.defineBind(":call_id", pModuleCallId);

      mUCon.executeAPI(SQLManager.instance().getStatement(UPDATE_MODULE_CALL_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to update module call", e);
    }
    finally {
      Track.pop("UpdateModuleCall");
    }

  }

  @Override
  public void deleteModuleCall(String pModuleCallId) {

    Track.pushInfo("DeleteModuleCall", "Delete module call with ID " + pModuleCallId);
    try {
      mUCon.executeAPI(SQLManager.instance().getStatement(DELETE_MODULE_CALL_FILENAME, getClass()), pModuleCallId);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to delete module call", e);
    }
    finally {
      Track.pop("DeleteModuleCall");
    }
  }

  @Override
  public void createStateCall(String pCallId, String pModuleCallId, int pStackPosition, String pStateName, int pScrollPosition, Collection<ContextUElem.SerialisedLabel> pContextualLabels) {

    Track.pushInfo("InsertStateCall", "Insert state call with ID " + pCallId + " (" + pStateName + ")");
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":call_id", pCallId);
      lBindMap.defineBind(":stack_position", pStackPosition);
      lBindMap.defineBind(":module_call_id", pModuleCallId);
      lBindMap.defineBind(":state_name", pStateName);
      lBindMap.defineBind(":scroll_position", pScrollPosition);
      lBindMap.defineBind(":context_labels", XStreamManager.serialiseObjectToXMLString(pContextualLabels));

      mUCon.executeAPI(SQLManager.instance().getStatement(INSERT_STATE_CALL_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to create state call", e);
    }
    finally {
      Track.pop("InsertStateCall");
    }

  }

  @Override
  public void updateStateCall(String pCallId, int pScrollPosition, Collection<ContextUElem.SerialisedLabel> pContextualLabels) {

    Track.pushInfo("UpdateStateCall", "Update state call with ID " + pCallId);
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":scroll_position", pScrollPosition);
      lBindMap.defineBind(":context_labels", XStreamManager.serialiseObjectToXMLString(pContextualLabels));
      lBindMap.defineBind(":call_id", pCallId);

      mUCon.executeAPI(SQLManager.instance().getStatement(UPDATE_STATE_CALL_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to update state call", e);
    }
    finally {
      Track.pop("UpdateStateCall");
    }
  }

  @Override
  public void deleteStateCallStack(String pModuleCallId) {
    Track.pushInfo("DeleteStateCallStack", "Delete state call stack for module call " + pModuleCallId);
    try {
      mUCon.executeAPI(SQLManager.instance().getStatement(DELETE_STATE_CALLSTACK_FILENAME, getClass()), pModuleCallId);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to delete state call stack", e);
    }
    finally {
      Track.pop("DeleteStateCallStack");
    }
  }

  @Override
  public void deleteStateCall(String pStateCallId) {
    Track.pushInfo("DeleteStateCall", "Delete state call with ID " + pStateCallId);
    try {
      mUCon.executeAPI(SQLManager.instance().getStatement(DELETE_STATE_CALL_FILENAME, getClass()), pStateCallId);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to delete state call", e);
    }
    finally {
      Track.pop("DeleteStateCall");
    }
  }

  @Override
  public void updateInternalDOM(String pModuleCallId, String pDocumentName, DOM pDOM){

    Track.pushInfo("UpdateInternalDOM", "Update DOM " + pDocumentName + " for module call " + pModuleCallId);
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":module_call_id", pModuleCallId);
      lBindMap.defineBind(":document_name", pDocumentName);
      //Explicitly define the BindObject here so the correct XML serialiser is used
      lBindMap.defineBind(":xml_data", new DOMBindObject(pDOM, BindDirection.IN, mInternalDOMWriterStrategy));
      mUCon.executeAPI(SQLManager.instance().getStatement(MERGE_INTERNAL_DOM_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to update internal DOM", e);
    }
    finally {
      Track.pop("UpdateInternalDOM");
    }
  }

  @Override
  public void createInternalDOM(String pModuleCallId, String pDocumentName, DOM pDOM) {
    updateInternalDOM(pModuleCallId, pDocumentName, pDOM);
  }

  @Override
  public void createDownloadParcel(DownloadParcel pDownloadParcel) {

    Track.pushInfo("CreateDownloadParcel", "Create download parcel " + pDownloadParcel.getParcelId());
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":parcel_id", pDownloadParcel.getParcelId());
      lBindMap.defineBind(":thread_id", mPersistenceContext.getThreadId());
      lBindMap.defineBind(":data_clob", XStreamManager.serialiseObjectToXMLString(pDownloadParcel));

      mUCon.executeAPI(SQLManager.instance().getStatement(INSERT_DOWNLOAD_PARCEL_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to create download parcel", e);
    }
    finally {
      Track.pop("CreateDownloadParcel");
    }
  }

  @Override
  public void createModuleFacet(ModuleFacet pModuleFacet) {

    String lFacetType = getFacetTypeName(pModuleFacet);

    Track.pushInfo("CreateFacet", "Create facet " + lFacetType + " " + pModuleFacet.getFacetKey());
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":facet_type", lFacetType);
      lBindMap.defineBind(":facet_key", pModuleFacet.getFacetKey());
      lBindMap.defineBind(":module_call_id", pModuleFacet.getModuleCallId());
      lBindMap.defineBind(":facet_object_blob", UCon.bindOut(BindSQLType.BLOB));

      UConStatementResult lStatementResult = mUCon.executeAPI(SQLManager.instance().getStatement(INSERT_MODULE_FACET_FILENAME, getClass()), lBindMap);

      Blob lFacetBlob = lStatementResult.getBlob(":facet_object_blob");
      //We need to write class names as facets have an unknown class hierarchy
      writeObjectToBlob(pModuleFacet, lFacetBlob, "ModuleFacet", true);
    }
    catch (ExDB | SQLException e) {
      throw new ExInternal("Failed to create " + lFacetType + " module facet", e);
    }
    finally {
      Track.pop("CreateFacet");
    }
  }

  @Override
  public void updateModuleFacet(ModuleFacet pModuleFacet) {

    String lFacetType = getFacetTypeName(pModuleFacet);

    Track.pushInfo("UpdateFacet", "Update facet " + lFacetType + " " + pModuleFacet.getFacetKey());
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":facet_type", lFacetType);
      lBindMap.defineBind(":facet_key", pModuleFacet.getFacetKey() );
      lBindMap.defineBind(":module_call_id", pModuleFacet.getModuleCallId());
      lBindMap.defineBind(":facet_object_blob", UCon.bindOut(BindSQLType.BLOB));

      UConStatementResult lStatementResult = mUCon.executeAPI(SQLManager.instance().getStatement(UPDATE_MODULE_FACET_FILENAME, getClass()), lBindMap);

      Blob lFacetBlob = lStatementResult.getBlob(":facet_object_blob");
      //We need to write class names as facets have an unknown class hierarchy
      writeObjectToBlob(pModuleFacet, lFacetBlob, "ModuleFacet", true);
    }
    catch (ExDB | SQLException e) {
      throw new ExInternal("Failed to update " + lFacetType + " module facet", e);
    }
    finally {
      Track.pop("UpdateFacet");
    }
  }

  @Override
  public void deleteModuleCallFacets(String pModuleCallId) {
    Track.pushInfo("DeleteModuleCallFacets", "Delete all facets for call " + pModuleCallId);
    try {
      mUCon.executeAPI(SQLManager.instance().getStatement(DELETE_MODULE_CALL_FACETS_FILENAME, getClass()), pModuleCallId);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to delete facets", e);
    }
    finally {
      Track.pop("DeleteModuleCallFacets");
    }
  }

  /**
   * Temporarily exposed - consumers should prefer a Serialiser interface method (i.e. one of createXXX/updateXXX/deleteXXX).
   * @return Current UCon for this DB serialiser.
   */
  public UCon getUCon() {
    return mUCon;
  }
}

package net.foxopen.fox.thread;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.database.storage.WorkDocValidator;
import net.foxopen.fox.database.storage.dom.XMLWorkDoc;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.dom.handler.InternalDOMHandler;
import net.foxopen.fox.dom.handler.SessionDOMHandler;
import net.foxopen.fox.dom.handler.SysDOMHandler;
import net.foxopen.fox.dom.handler.TempDOMHandler;
import net.foxopen.fox.dom.handler.WorkDocDOMHandler;
import net.foxopen.fox.module.SyncMode;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.data.InternalDOMPersistedData;
import net.foxopen.fox.thread.storage.DataDOMStorageLocation;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;


/**
 * Class for managing the DOMs and DOMHandlers needed by a StatefulXThread.
 */
public class StatefulXThreadDOMProvider
implements DOMHandlerProvider, ThreadEventListener {

  private static final EnumSet<ContextLabel> DEFAULT_CONTEXT_LABELS = EnumSet.of(
    ContextLabel.ENV,
    ContextLabel.ERROR,
    ContextLabel.PARAMS,
    ContextLabel.RESULT,
    ContextLabel.RETURN,
    ContextLabel.THEME
  );

  private final SysDOMHandler mSysDOMHandler;
  private final TempDOMHandler mTempDOMHandler;
  private final SessionDOMHandler mSessionDOMHandler;
  private final DOMHandler mUserDOMHandler;
  private final XThreadWorkDocManager mWorkDocManager;

  private String mLastTempDOMString;

  public static StatefulXThreadDOMProvider createNew(RequestContext pRequestContext, StatefulXThread pXThread, UserThreadSession pOwningSession, AuthenticationContext pAuthenticationContext) {

    SysDOMHandler lSysDOMHandler = SysDOMHandler.createSysDOMHandler(pRequestContext, pXThread);
    TempDOMHandler lTempDOMHandler = TempDOMHandler.createTempDOMHandler(pXThread);
    SessionDOMHandler lSessionDOMHandler = SessionDOMHandler.createHandler(pOwningSession.getSessionDOMManager(), pXThread);
    DOMHandler lUserDOMHandler = pAuthenticationContext.getUserDOMHandler();

    StatefulXThreadDOMProvider lDOMProvider = new StatefulXThreadDOMProvider(lSysDOMHandler, lTempDOMHandler, lSessionDOMHandler, lUserDOMHandler);

    pXThread.registerThreadEventListener(lDOMProvider);

    return lDOMProvider;
  }

  private StatefulXThreadDOMProvider(SysDOMHandler pSysDOMHandler, TempDOMHandler pTempDOMHandler, SessionDOMHandler pSessionDOMHandler, DOMHandler pUserDOMHandler) {
    mSysDOMHandler = pSysDOMHandler;
    mTempDOMHandler = pTempDOMHandler;
    mSessionDOMHandler = pSessionDOMHandler;
    mUserDOMHandler = pUserDOMHandler;
    mWorkDocManager = new XThreadWorkDocManager();
  }


  private void addThreadLevelDOMHandlers(Collection<DOMHandler> pDOMHandlerList) {
    //Sys DOM
    pDOMHandlerList.add(mSysDOMHandler);

    //Temp DOM
    pDOMHandlerList.add(mTempDOMHandler);

    //Session DOM
    pDOMHandlerList.add(mSessionDOMHandler);

    //User DOM
    pDOMHandlerList.add(mUserDOMHandler);
  }


  @Override
  public Collection<DOMHandler> createDefaultDOMHandlers(String pModuleCallId) {
    Collection<DOMHandler> lDOMHandlerList = new ArrayList<>();

    //Create new DOM handlers for default module-level labels
    for(ContextLabel lContextLabel : DEFAULT_CONTEXT_LABELS) {
      lDOMHandlerList.add(InternalDOMHandler.createHandlerForNewDOM(lContextLabel, pModuleCallId));
    }

    //Add handlers for the "special" thread-level DOMs
    addThreadLevelDOMHandlers(lDOMHandlerList);

    return lDOMHandlerList;
  }

  /**
   * Creates an InternalDOMHandler for an existing serialised DOM, based on the module call ID and document name. If no DOM was
   * serialised an empty DOM will be created with the given root element name.
   * @param pPersistenceContext
   * @param pModuleCallId
   * @param pDocumentName
   * @param pRootElementName
   * @return
   */
  private DOMHandler getDeserialisedDOMHandler(PersistenceContext pPersistenceContext, String pModuleCallId, String pDocumentName, String pRootElementName) {
    if(pPersistenceContext == null) {
      throw new IllegalArgumentException("PersistenceContext required to deserialise internal DOM");
    }

    InternalDOMPersistedData lDOMPersistedData = pPersistenceContext.getDeserialiser().getInternalDOMPersistedData(pModuleCallId, pDocumentName);
    if(lDOMPersistedData != null) {
      return InternalDOMHandler.createHandlerForExistingDOM(pDocumentName, pModuleCallId, lDOMPersistedData.getDOM());
    }
    else {
      return InternalDOMHandler.createHandlerForNewDOM(pDocumentName, pRootElementName, pModuleCallId);
    }
  }

  @Override
  public Collection<DOMHandler> restoreDefaultDOMHandlers(PersistenceContext pPersistenceContext, String pModuleCallId) {
    Collection<DOMHandler> lDOMHandlerList = new ArrayList<>();

    //Create new DOM handlers for default module-level labels
    for(ContextLabel lContextLabel : DEFAULT_CONTEXT_LABELS) {
      DOMHandler lDOMHandler = getDeserialisedDOMHandler(pPersistenceContext, pModuleCallId, lContextLabel.asString(), lContextLabel.asString());
      lDOMHandlerList.add(lDOMHandler);
    }

    //Add handlers for the "special" thread-level DOMs
    addThreadLevelDOMHandlers(lDOMHandlerList);

    return lDOMHandlerList;
  }

  private DOMHandler createDOMHandlerForWSL(WorkingDataDOMStorageLocation pWorkingStoreLocation, String pModuleCallId, boolean pIsNewDocument, PersistenceContext pOptionalPersistenceContext) {

    DataDOMStorageLocation lStorageLocation = pWorkingStoreLocation.getStorageLocation();
    String lContextLabel = lStorageLocation.getDocumentContextLabel();
    String lRootElementName = lStorageLocation.getNewDocRootElementName();

    if(!lStorageLocation.hasQueryStatement()){
      //Storage location has no SELECT statement; treat this as an internal DOM
      if(pIsNewDocument) {
        //If this is for a new document we can create a new handler from nothing
        return InternalDOMHandler.createHandlerForNewDOM(lContextLabel, lRootElementName, pModuleCallId);
      }
      else {
        //If this is not for a new document we need to deserialise the DOM handler
        return getDeserialisedDOMHandler(pOptionalPersistenceContext, pModuleCallId, lContextLabel, lRootElementName);
      }
    }
    else if(pWorkingStoreLocation.getSyncMode() == SyncMode.UNSYNCHRONISED) {
      //For an unsynchronised new DOM, create an internal DOM which will be initialised from the RO WorkDoc.
      if(pIsNewDocument) {
        Track.debug("UnsynchronisedWorkDoc", "Creating internal DOM handler for unsynchronised work doc " + lContextLabel);
        XMLWorkDoc lROWorkDoc = XMLWorkDoc.getOrCreateXMLWorkDoc(pWorkingStoreLocation, true);
        return InternalDOMHandler.createHandlerForWorkDoc(lContextLabel, pModuleCallId, lROWorkDoc);
      }
      else {
        //Not new - unsynchronised DOM will have been serialised as a standard internal DOM
        return getDeserialisedDOMHandler(pOptionalPersistenceContext, pModuleCallId, lContextLabel, lRootElementName);
      }
    }
    else {
      //This will handle RO and editable WorkDocs
      return new WorkDocDOMHandler(pWorkingStoreLocation, lContextLabel, mWorkDocManager);
    }
  }

  @Override
  public DOMHandler createDOMHandlerForWSL(WorkingDataDOMStorageLocation pWorkingStoreLocation, String pModuleCallId) {
    return createDOMHandlerForWSL(pWorkingStoreLocation, pModuleCallId, true, null);
  }

  @Override
  public DOMHandler restoreDOMHandlerForWSL(PersistenceContext pPersistenceContext, WorkingDataDOMStorageLocation pWorkingStoreLocation, String pModuleCallId) {
    return createDOMHandlerForWSL(pWorkingStoreLocation, pModuleCallId, false, pPersistenceContext);
  }

  protected SysDOMHandler getSysDOMHandler() {
    return mSysDOMHandler;
  }

  @Override
  public void handleThreadEvent(ActionRequestContext pRequestContext, ThreadEventType pEventType) {
    if(pEventType == ThreadEventType.FINISH_REQUEST_PROCESSING) {
      mLastTempDOMString = mTempDOMHandler.getDOM().outputNodeToString(true);
      mTempDOMHandler.flushDOM();
    }
  }

  @Override
  public WorkDocValidator.Result validatePendingWorkDocs(ActionRequestContext pRequestContext) {
    return mWorkDocManager.getWorkDocValidator().validatePending(pRequestContext);
  }

  String getLastTempDOMString() {
    return mLastTempDOMString;
  }
}

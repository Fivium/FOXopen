package net.foxopen.fox.thread;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.FoxResponseCHARStream;
import net.foxopen.fox.ResponseMethod;
import net.foxopen.fox.URIResourceReference;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.dom.handler.PostableDOMHandler;
import net.foxopen.fox.dom.xpath.saxon.SaxonEnvironment;
import net.foxopen.fox.download.DownloadManager;
import net.foxopen.fox.download.ThreadDownloadManager;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.entrypoint.servlets.FoxMainServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExAlreadyHandled;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.logging.ErrorLogger;
import net.foxopen.fox.module.ActionDefinition;
import net.foxopen.fox.module.AutoActionType;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.action.InternalAction;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.serialiser.ThreadInfoProvider;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.persistence.DatabasePersistenceContext;
import net.foxopen.fox.thread.persistence.Deserialiser;
import net.foxopen.fox.thread.persistence.Persistable;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;
import net.foxopen.fox.thread.persistence.data.StatefulXThreadPersistedData;
import net.foxopen.fox.thread.persistence.xstream.XStreamManager;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.thread.storage.StatefulThreadTempResourceProvider;
import net.foxopen.fox.thread.storage.TempResourceProvider;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;
import net.foxopen.fox.track.TrackProperty;
import net.foxopen.fox.track.TrackTimer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class StatefulXThread
implements XThreadInterface, ThreadInfoProvider, Persistable {
  private static final String ORPHAN_FLAG_PARAM_NAME = "orphan_flag";

  private static final Iterator<String> CHANGE_NO_UNIQUE_ITERATOR = XFUtil.getUniqueIterator();

  private final String mAppMnem;
  private final String mThreadId;
  private final ModuleCallStack mModuleCallStack;

  private final Set<ThreadEventListener> mThreadEventListeners = new HashSet<>();

  private final ThreadPropertyMap mThreadPropertyMap;

  private final AuthenticationContext mAuthenticationContext;

  private final PersistenceContext mPersistenceContext;

  private final DevToolbarContext mDevToolbarContext;

  private final StatefulXThreadDOMProvider mDOMProvider;

  private final UserThreadSession mUserThreadSession;

  private final DownloadManager mDownloadManager;

  private final TempResourceProvider mTempResourceProvider;

  private String mFoxSessionID;

  /** The FieldSet this thread has most recently sent out. May be null if this thread is yet to perform a generate. */
  private FieldSet mFieldSetOut;

  /** The FieldSet this thread has most recently received. May be null if this thread is yet to perform a generate. */
  private FieldSet mFieldSetIn;

  private boolean mFieldSetSuppressApply = false; //transient

  private FoxResponse mCachedResponse = null; //transient

  private boolean mHasBeenMounted = false; //transient - true if this thread has been mounted since it was created/deserialised

  private String mChangeNumber;

  /**
   * NOTE: THIS COMMITS THE TOP UCON
   * @param pRequestContext
   * @param pThreadId
   * @return
   */
  public static StatefulXThread getAndLockXThread(RequestContext pRequestContext, String pThreadId) {
    //Lock the thread symbolically before doing anything else
    String lDbChangeNumber;
    Track.pushInfo("LockThread", pThreadId, TrackTimer.THREAD_LOCK);
    Track.timerStart(TrackTimer.THREAD_LOCK_MANAGEMENT);
    try {
      lDbChangeNumber = StatefulXThreadLocker.acquireLock(pRequestContext, pThreadId);
    }
    finally {
      Track.timerPause(TrackTimer.THREAD_LOCK_MANAGEMENT);
      Track.pop("LockThread", TrackTimer.THREAD_LOCK);
    }

    //Attempt to retrieve thread from cache
    FoxCache<String, StatefulXThread> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.STATEFUL_XTHREADS);
    StatefulXThread lXThread = lFoxCache.get(pThreadId);

    boolean lDeserialiseRequired = false;
    if(lXThread == null){
      //Thread not in cache
      Track.info("ThreadCacheMiss", "Thread not in cache - deserialise required");
      lDeserialiseRequired = true;
    }
    else if(!lDbChangeNumber.equals(lXThread.mChangeNumber)) {
      //Thread in cache but change number in cache does not match database - indicates this thread has been modified on another app server
      Track.info("ThreadChangeNumberMismatch", "Change number on cached thread [" + lXThread.mChangeNumber + "] does not match number on database [" + lDbChangeNumber + "]");
      lDeserialiseRequired = true;
    }
    else if(lXThread.mDevToolbarContext.isFlagOn(DevToolbarContext.Flag.NO_CACHE)) {
      Track.info("ThreadNoCache", "Thread is cached but NO_CACHE dev flag is true - forcing deserialise");
      lDeserialiseRequired = true;
    }
    else {
      Track.info("ThreadCacheHit", "Thread in cache with latest change number");
    }

    if(lDeserialiseRequired) {
      try {
        lXThread = deserialise(pRequestContext, pThreadId);
      }
      catch(Throwable th) {
        //Release the lock on the thread row if an error occurred during deserialise
        try {
          StatefulXThreadLocker.releaseLock(pRequestContext, pThreadId);
        }
        catch(Throwable th2) {
          //Don't allow exception from unlock to overwrite the original exception
          Track.recordSuppressedException("Thread Release Failed", th2);
        }
        throw new ExInternal("Error deserialising thread", th);
      }
      lFoxCache.put(pThreadId, lXThread);
    }
    else {
      //If the thread was already in cache, ensure the persistence context has an up-to-date deserialiser on it
      lXThread.mPersistenceContext.setupDeserialiser(pRequestContext);
    }

    //Check the thread has module calls - if not this indicates it was already exited
    if(lXThread.mModuleCallStack.getStackSize() == 0) {
      throw new ExInternal("Thread " + pThreadId + " has been exited and is no longer accessible");
    }

    return lXThread;
  }

  /**
   * NOTE: THIS COMMITS THE TOP UCON
   * @param pRequestContext
   * @param pXThread
   */
  public static void unlockThread(RequestContext pRequestContext, StatefulXThread pXThread) {
    Track.pushInfo("UnlockThread", "Releasing lock", TrackTimer.THREAD_UNLOCK);
    Track.timerStart(TrackTimer.THREAD_LOCK_MANAGEMENT);
    try {
      StatefulXThreadLocker.releaseLock(pRequestContext, pXThread.getThreadId());
    }
    finally {
      Track.timerPause(TrackTimer.THREAD_LOCK_MANAGEMENT);
      Track.pop("UnlockThread", TrackTimer.THREAD_UNLOCK);
    }
  }

  private static StatefulXThread deserialise(RequestContext pRequestContext, String pThreadId) {

    Track.pushInfo("DeserialiseXThread", "Deserialising thread " + pThreadId + " from database", TrackTimer.THREAD_DESERIALISE);
    try {
      DatabasePersistenceContext lPersistenceContext = new DatabasePersistenceContext(pThreadId);
      Deserialiser lDeserialiser = lPersistenceContext.setupDeserialiser(pRequestContext);

      StatefulXThread lNewThread;
      StatefulXThreadPersistedData lThreadData;
      //Retain a UCon for the whole deserialise process
      pRequestContext.getContextUCon().startRetainingUCon();
      try {
        lThreadData = lDeserialiser.getXThreadPersistedData(pThreadId);

        AuthenticationContext lAuthenticationContext = lThreadData.getAuthenticationContext();
        //This gets a session object but doesn't read anything from the database - if necessary the session could be added to the Deserialiser class
        UserThreadSession lSession = UserThreadSession.getExistingSession(lThreadData.getUserThreadSessionId(), lPersistenceContext);

        //Create an empty callstack
        ModuleCallStack lCallStack = new ModuleCallStack();

        lNewThread = new StatefulXThread(pRequestContext, lSession, lThreadData.getAppMnem(), lThreadData.getThreadId(), lAuthenticationContext,
                                         lPersistenceContext, lThreadData.getThreadPropertyMap(), lCallStack, lThreadData.getFoxSessionID());

        //Deserialise the callstack - we need the thread to do this
        lCallStack.deserialise(pRequestContext, lPersistenceContext, pThreadId, lNewThread.mDOMProvider);
      }
      finally {
        pRequestContext.getContextUCon().stopRetainingUCon();
      }

      //Setup the fieldset the thread is about to receive/just sent
      lNewThread.mFieldSetIn = lThreadData.getFieldSet();
      lNewThread.mFieldSetOut =  lNewThread.mFieldSetIn;

      lNewThread.mChangeNumber = lThreadData.getChangeNumber();

      return lNewThread;
    }
    finally {
      Track.pop("DeserialiseXThread", TrackTimer.THREAD_DESERIALISE);
    }
  }

  public static void purgeThreadFromCache(String pThreadId) {
    FoxCache<String, StatefulXThread> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.STATEFUL_XTHREADS);
    lFoxCache.remove(pThreadId);
  }


  /**
   * Shared
   */
  StatefulXThread(RequestContext pRequestContext, UserThreadSession pUserThreadSession, String pAppMnem, String pThreadId,
                  AuthenticationContext pAuthenticationContext, PersistenceContext pPersistenceContext, ThreadPropertyMap pThreadPropertyMap, ModuleCallStack pModuleCallStack,
                  String pFoxSessionID) {

    mAppMnem = pAppMnem;
    mThreadId = pThreadId;

    mThreadPropertyMap = pThreadPropertyMap;

    mFoxSessionID = pFoxSessionID;

    mPersistenceContext = pPersistenceContext;

    mModuleCallStack = pModuleCallStack;
    mPersistenceContext.registerListeningPersistable(mModuleCallStack);
    registerThreadEventListener(mModuleCallStack);

    mDevToolbarContext = StatefulXThreadDevToolbarContext.createDevToolbarContext(pRequestContext, this);

    mAuthenticationContext = pAuthenticationContext;
    mModuleCallStack.registerStateChangeListener(pAuthenticationContext);

    mUserThreadSession = pUserThreadSession;

    mDOMProvider = StatefulXThreadDOMProvider.createNew(this, pUserThreadSession, pAuthenticationContext);

    mDownloadManager = new ThreadDownloadManager(this);

    mTempResourceProvider = new StatefulThreadTempResourceProvider();
  }

  public String toString() {
    return "StatefulXThread ThreadId: " + mThreadId + " App: " + mAppMnem;
  }

  private void notifyEventListeners(ThreadEventType pEventType) {
    for(ThreadEventListener lListener : mThreadEventListeners) {
      lListener.handleThreadEvent(pEventType);
    }
  }

  public void registerThreadEventListener(ThreadEventListener pEventListener) {
    mThreadEventListeners.add(pEventListener);
  }

  @Override
  public FoxResponse startThread(RequestContext pRequestContext, EntryTheme pEntryTheme, DOM pParamsDOM, boolean pGenerateResponse) {

    ModuleCall.Builder lModuleCallBuilder = new ModuleCall.Builder(pEntryTheme);

    lModuleCallBuilder.setParamsDOM(pParamsDOM);

    //Mount the request context
    ActionRequestContext lActionRequestContext = new XThreadActionRequestContext(pRequestContext, this);

    return startThread(lActionRequestContext, lModuleCallBuilder, pGenerateResponse);
  }

  private void initialiseBeforeActionProcessing(ActionRequestContext pRequestContext){
    //Notify anything depending on the persitence context that a new cycle is starting
    mPersistenceContext.startPersistenceCycle(pRequestContext);

    //Reset any pre existing value for the new churn
    mFieldSetSuppressApply = false;

    //Check we have the latest copy of the User DOM and refresh if not
    pRequestContext.getAuthenticationContext().refreshUserDOM(pRequestContext);

    //Notify any event listeners that a request is about to be processed
    notifyEventListeners(ThreadEventType.START_REQUEST_PROCESSING);

    //Configure Saxon now so any XPaths run which need a request context have access to it
    SaxonEnvironment.setThreadLocalRequestContext(pRequestContext);

    Track.setProperty(TrackProperty.THREAD_ID, mThreadId);
  }

  public FoxResponse startThread(ActionRequestContext pRequestContext, ModuleCall.Builder pModuleCallBuilder, boolean pGenerateResponse) {

    initialiseBeforeActionProcessing(pRequestContext);

    //Mark as requiring a create
    mPersistenceContext.requiresPersisting(this, PersistenceMethod.CREATE);

    //Record entry theme name
    Track.setProperty(TrackProperty.ACTION_TYPE, "EntryTheme");
    Track.setProperty(TrackProperty.ACTION_NAME, pModuleCallBuilder.getEntryTheme().getName());

    FoxResponse lFoxResponse;
    Track.pushInfo("StartThread");
    try {
      //Run the initial entry theme
      Track.timerStart(TrackTimer.ACTION_PROCESSING);
      try {
        mModuleCallStack.doInitialCall(pRequestContext, pModuleCallBuilder);
      }
      finally {
        Track.timerPause(TrackTimer.ACTION_PROCESSING);
      }

      //Do any work required before we respond
      finaliseBeforeResponse(pRequestContext);

      //Generate a response
      if(pGenerateResponse) {
        lFoxResponse = establishResponse(pRequestContext);
      }
      else {
        lFoxResponse = null;
      }

      //Do any post-action work
      finaliseAfterActionProcessing(pRequestContext);
    }
    catch(Throwable th) {
      //Abort DOM Handlers and rethrow
      abort();
      throw new ExInternal("Failed to process entry theme", th);
    }
    finally {
      Track.pop("StartThread");
    }

    return lFoxResponse;
  }

  private void abort() {
    //Abort DOM Handlers
    try {
      getTopModuleCall().getContextUElem().abortDOMHandlers();
    }
    catch(Throwable th){
      Track.recordSuppressedException("Thread DOMHandler Abort", th);
    }

    //Clean up the Saxon environment
    try {
      SaxonEnvironment.clearThreadLocalRequestContext();
    }
    catch(Throwable th){
      Track.recordSuppressedException("Thread Abort clearRequestContext", th);
    }
  }

  public XDoCommandList resolveActionName(String pActionName)  {
    State lState = getTopModuleCall().getTopState();
    return lState.getActionByName(pActionName).getXDoCommandList();
  }

  public void setFoxSessionID(String pFoxSessionID) {
    mFoxSessionID = pFoxSessionID;
  }

  public String getFoxSessionID() {
    return mFoxSessionID;
  }

  /**
   * Processing to be performed when the thread is fully ramped up.
   */
  //TODO PN should be package private
  public interface XThreadRunnable {
    void run(ActionRequestContext pRequestContext) throws ExUserRequest;
  }

  //TODO PN should be package private
  public void rampAndRun(RequestContext pRequestContext, XThreadRunnable pRunnable, String pTrackActionType) {
    rampAndRun(pRequestContext, pRunnable, pTrackActionType, false);
  }

  /**
   * Ramps the thread up and performs any processing provided in the given XThreadRunnable. The thread is ramped down after
   * running the XThreadRunnable. If requested, a response is determined and returned based on the results of any action
   * processing. If any error occurs the thread is aborted.
   * @param pRequestContext
   * @param pRunnable Processing to perform when the thread is ramped.
   * @param pTrackActionType Action type for track and logging purposes.
   * @param pResponseRequired Set to true if the XThread is expected to generate a response from this processing.
   * @return FoxResponse or null.
   */
  private FoxResponse rampAndRun(RequestContext pRequestContext, XThreadRunnable pRunnable, String pTrackActionType, boolean pResponseRequired) {
    ActionRequestContext lActionRequestContext = new XThreadActionRequestContext(pRequestContext, this);

    Track.pushInfo(pTrackActionType);
    try {
      //Check the FOX session token (from a cookie) matches the value on this thread's FOX session ID chain
      validateFoxSession(pRequestContext);

      //Perform any pre-action initialisation (reset stateful members, etc)
      initialiseBeforeActionProcessing(lActionRequestContext);

      //Ramp up the callstack (mount top module)
      mModuleCallStack.rampUp(lActionRequestContext);

      //Run the action/xpath/etc
      pRunnable.run(lActionRequestContext);

      //Unmounts + serialises WorkDoc DOMs. This will make all DOMs RO.
      mModuleCallStack.rampDown(lActionRequestContext);

      //Do any work required before we respond (setting HTTP headers etc)
      finaliseBeforeResponse(pRequestContext);

      //Decide how to respond based on the results of action processing
      FoxResponse lResultResponse;
      if(pResponseRequired) {
        lResultResponse = establishResponse(lActionRequestContext);
      }
      else {
        lResultResponse = null;
      }

      //Clear temp DOM etc, save fieldset
      finaliseAfterActionProcessing(lActionRequestContext);

      return lResultResponse;
    }
    catch(Throwable th){
      //Abort DOMHandlers
      abort();

      //Rethrow the error
      throw new ExInternal("Error caught during " + pTrackActionType, th);
    }
    finally {
      Track.pop(pTrackActionType);
    }
  }

  private void validateFoxSession(RequestContext pRequestContext) {
    //Check that this XThread has a valid session ID and haul it up to date if it's behind the latest
    String lFoxSessionID = pRequestContext.getFoxSession().checkSessionValidity(pRequestContext, mAuthenticationContext, getFoxSessionID());
    setFoxSessionID(lFoxSessionID);
  }

  public FoxResponse processExternalResume(RequestContext pRequestContext) {

    //Validate that we are allowed to resume this thread from an external entry point
    if(!mThreadPropertyMap.getBooleanProperty(ThreadProperty.Type.IS_RESUME_ALLOWED)) {
      throw new ExInternal("Thread is not externally resumable");
    }
    //Previously this set the resume flag to false to prevent re-resumes, this has been disabled because it breaks the idempotent GET rule

    //Check externally resumable
    //Separate flag for "skip session check" (immediately set to false to only allow this behaviour once)
    Track.pushInfo("ThreadExternalResume");
    try {
      if(mThreadPropertyMap.getBooleanProperty(ThreadProperty.Type.IS_SKIP_FOX_SESSION_CHECK)) {
        //Skip the session check for the initial entry and force the thread to have the latest session id
        setBooleanThreadProperty(ThreadProperty.Type.IS_SKIP_FOX_SESSION_CHECK, false);
        setFoxSessionID(pRequestContext.getFoxSession().getSessionId());
      }
      else {
        validateFoxSession(pRequestContext);
      }

      ActionRequestContext lActionRequestContext = new XThreadActionRequestContext(pRequestContext, this);

      initialiseBeforeActionProcessing(lActionRequestContext);

      finaliseBeforeResponse(pRequestContext);

      FoxResponse lFoxResponse = establishResponse(lActionRequestContext);

      mPersistenceContext.requiresPersisting(this, PersistenceMethod.UPDATE);

      finaliseAfterActionProcessing(lActionRequestContext);

      return lFoxResponse;
    }
    catch (Throwable th) {
      abort();
      throw new ExInternal("Error during external resume", th);
    }
    finally {
      Track.pop("ThreadExternalResume");
    }
  }

  public FoxResponse processAction(RequestContext pRequestContext, final String pActionName, final String pActionRef, final Map<String, String[]> pPostedFormValuesMap) {

    XThreadRunnable lActionRunner = new XThreadRunnable() {
      @Override
      public void run(ActionRequestContext pActionRequestContext) throws ExUserRequest {
        //Test the submitted fieldset value matches the expected one - if not, don't apply the field set or run any actions
        //Also checks for orphan thread resumes and returns true if a ramp is needed (even if a field set label isn't specified)
        if(validateFieldSet(pActionRequestContext, pPostedFormValuesMap)) {
          //Apply fieldset
          fieldSetApply(pActionRequestContext, pPostedFormValuesMap);

          //Set the scroll value on the top state from the posted form
          scrollValueApply(pPostedFormValuesMap);

          if(XFUtil.isNull(pActionName)) {
            //Can happen at the moment on modeless resumes
            Track.alert("NullActionName", "Action name not specified; no action performed");
          }
          else if(pActionName.startsWith(mFieldSetIn.getOutwardFieldSetLabel())){
            //Name starting with fieldset label indicates internal action TODO this isn't great
            String lInternalActionName = pActionName.substring(pActionName.lastIndexOf("/")+1);
            processInternalAction(pActionRequestContext, lInternalActionName);
          }
          else {
            //Run a named action
            processExternalAction(pActionRequestContext, pActionName, pActionRef);
          }
        }

        //Always mark as requiring an update (the fieldset will always change)
        mPersistenceContext.requiresPersisting(StatefulXThread.this, PersistenceMethod.UPDATE);
      }
    };

    return rampAndRun(pRequestContext, lActionRunner, "ProcessAction", true);
  }

  private FoxResponse establishResponse(ActionRequestContext pRequestContext){

    //Check for a pre-cached response, i.e. if this is a modeless resume
    FoxResponse lCachedResponse = getAndClearCachedResponse();
    if(lCachedResponse != null){
      Track.setProperty(TrackProperty.RESPONSE_TYPE, "PRECACHED");
      return lCachedResponse;
    }

    //Check for response redirects (caused by exit module / pragma set response etc)
    FoxResponse lResultResponse;
    List<ResponseOverride> lOverrideList = pRequestContext.getXDoResults(ResponseOverride.class);
    if(lOverrideList.size() > 1) {
      throw new ExInternal("ResponseOverride list is wrong size, can only have 1 not " + lOverrideList.size());
    }
    else if (lOverrideList.size() == 1) {
      lResultResponse = lOverrideList.get(0).getFoxResponse();
      Track.setProperty(TrackProperty.RESPONSE_TYPE, "OVERRIDE");
      Track.info("ResponseOverride", "Response overridden by object of type " + lResultResponse.getClass().getName());
    }
    else {
      Track.setProperty(TrackProperty.RESPONSE_TYPE, "HTML");
      lResultResponse = generateHTMLResponse(pRequestContext);
    }

    return lResultResponse;
  }

  private void processInternalAction(ActionRequestContext pRequestContext, String pActionName)
  throws ExUserRequest {

    Track.pushInfo("ProcessInternalAction", "Action: " + pActionName);
    try {
      //Parse any action params which have been passed in
      String lActionParams = pRequestContext.getFoxRequest().getHttpRequest().getParameter("action_params");
      Map<String, String> lParamMap = Collections.emptyMap();
      if(!XFUtil.isNull(lActionParams)) {
        lParamMap = (JSONObject) JSONValue.parse(lActionParams);
      }

      //Resolve action from fieldset
      InternalAction lInternalAction = mFieldSetIn.getInternalAction(pActionName);
      if (lInternalAction == null) {
        throw new ExUserRequest("Internal action '"+pActionName+"' not found for this fieldset or type not specified.");
      }

      //TODO track action type
      lInternalAction.run(pRequestContext, lParamMap);
    }
    finally {
      Track.pop("ProcessInternalAction");
    }
  }

  private void processExternalAction(ActionRequestContext pRequestContext, String pActionName, String pActionContextRef){

    //Check the action is allowed to be run
    if (!mFieldSetIn.isExternalActionRunnable(pActionName, pActionContextRef)) {
      throw new ExInternal("Action '"+pActionName+"' not found for this fieldset for action context " + pActionContextRef);
      //LEGACY:
    //      ExUserRequest e = new ExUserRequest("Action '"+pActionName+"' not found for this fieldset.");
    //      e.setHttpStatusCode(403); // Set 403 (Forbidden) http return code
    //      throw e;
    }


    //Define the action context
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    DOM lActionContext = lContextUElem.getElemByRefOrNull(pActionContextRef);

    //Short circuit if the action context cannot be resolved - indicates the DOM has mutated between churns and the element has been removed
    if(lActionContext == null) {
      Track.alert("MissingActionContext", "Failed to resolve ref " + pActionContextRef + "; aborting action run");
      pRequestContext.addXDoResult(new AlertMessage("This action is no longer available."));
      return;
    }

    lContextUElem.setUElem(ContextLabel.ACTION, lActionContext);

    XDoRunner lActionRunner = pRequestContext.createCommandRunner(true);

    //Resolve action to run
    XDoCommandList lActionToRun = pRequestContext.resolveActionName(pActionName);

    if(lActionToRun != null){
      Track.pushInfo("ProcessExternalAction", pActionName, TrackTimer.ACTION_PROCESSING);
      try {
        Track.setProperty(TrackProperty.ACTION_TYPE, "ExternalAction");
        Track.setProperty(TrackProperty.ACTION_NAME, pActionName);

        //Tell ContextUCon to start retaining its UCon so multiple UCons aren't requested in an action churn
        pRequestContext.getContextUCon().startRetainingUCon();
        try {
          //Record final actions now in case the state changes during action processing (the initial state's actions should always be used - legacy behaviour)
          Collection<ActionDefinition> lFinalActions = pRequestContext.getCurrentState().getAutoActions(AutoActionType.ACTION_FINAL);

          //Run auto action inits
          Collection<ActionDefinition> lInitActions = pRequestContext.getCurrentState().getAutoActions(AutoActionType.ACTION_INIT);
          for (ActionDefinition lAction : lInitActions) {
            lActionRunner.runCommands(pRequestContext, lAction.getXDoCommandList());
          }

          //Run main action (runner will skip if an auto action caused a break)
          lActionRunner.runCommands(pRequestContext, lActionToRun);

          //Run auto action finals
          for (ActionDefinition lAction : lFinalActions) {
            lActionRunner.runCommands(pRequestContext, lAction.getXDoCommandList());
          }

          //Process any callstack transformations etc
          lActionRunner.processCompletion(pRequestContext, mModuleCallStack);
        }
        finally {
          //ContextUCon can now release any connection it has if appropriate
          pRequestContext.getContextUCon().stopRetainingUCon();
        }
      }
      catch(Throwable th) {
        throw new ExInternal("Error caught running action " + pActionName, th);
      }
      finally {
        Track.pop("ProcessExternalAction", TrackTimer.ACTION_PROCESSING);
      }
    }
    else {
      throw new ExInternal("Invalid action name " + pActionName);//TODO (temp) check action name may be allowed to be null!!!
    }
  }

  private boolean validateFieldSet(ActionRequestContext pRequestContext, Map<String, String[]> pPostedFormValuesMap){

    //If this is the first entry into an orphan thread (i.e. a popup), we won't have a fieldset
    if(pPostedFormValuesMap.get(ORPHAN_FLAG_PARAM_NAME) != null) {
      //If we have a cached response, return false so no fieldset processing is done
      if(mCachedResponse != null) {
        return false;
      }
      else {
        //Otherwise return true with suppress set so the mount/apply process continues as normal
        mFieldSetSuppressApply = true;
        return true;
      }
    }

    String[] lFieldSetArray = pPostedFormValuesMap.get("field_set");
    //Check we've been sent a field_set value in a valid format
    if(lFieldSetArray != null && lFieldSetArray.length == 1 && !XFUtil.isNull(lFieldSetArray[0])) {
      //Check the posted field set label against the expected label
      if (!lFieldSetArray[0].equals(mFieldSetIn.getOutwardFieldSetLabel())) {
        //Note: this line used to display an error message if this was not caused by the user hitting refresh, detected by reading the submit_count parameter
        pRequestContext.addXDoResult(new AlertMessage("You appear to have reloaded the page or used one of your browser's back or forward buttons.\n\n" +
                                                      "Please use the navigation options provided on the page, and avoid reloading the page as this may cause data to be lost."));
        return false;
      }
      else {
        return true;
      }
    }
    else {
      throw new ExInternal("Expected submitted form to have a non-null field_set value");
    }
  }

  private void fieldSetApply(ActionRequestContext pRequestContext, Map<String, String[]> pPostedFormValuesMap){
    Track.pushInfo("FieldSetApply", pPostedFormValuesMap.size() + " fields to apply");
    try {
      if(!mFieldSetSuppressApply){
        mFieldSetIn.applyChangesToDOMs(pRequestContext, pPostedFormValuesMap);
      }
      else {
        Track.info("Suppressed", "Skipping field set apply");
      }
    }
    finally {
      Track.pop("FieldSetApply");
    }
  }

  private void scrollValueApply(Map<String, String[]> pPostedFormValuesMap) {

    String[] lScrollVal = pPostedFormValuesMap.get("scroll_position");
    if(lScrollVal != null && lScrollVal.length == 1) {
      getTopModuleCall().setScrollPosition((int)Double.parseDouble(lScrollVal[0]));
    }
  }

  //TODO refactor into "ThreadOutputGenerator" strategy
  private FoxResponse generateHTMLResponse(ActionRequestContext pRequestContext) {

    //If the thread has been restored from the database and an action hasn't been invoked, the top ModuleCall's DOMs may not
    //have been loaded. This call ensures DOMs are loaded and labels are set before attempting HTML generation.
    mModuleCallStack.validateTopModuleCallHasBeenRamped(pRequestContext);

    //Always create a new FieldSet - existing one should not be reused
    mFieldSetOut = FieldSet.createNewFieldSet(pRequestContext);

    //TODO PN awful - shouldn't be talking to foxrequest here (move to SRC?)
    pRequestContext.getFoxRequest().setCurrentFieldSet(getThreadId(), mFieldSetOut.getOutwardFieldSetLabel());

    Track.pushInfo("HTMLGenerator");
    Track.timerStart(TrackTimer.OUTPUT_GENERATION);
    try {
      //Tell ContextUCon to start retaining its UCon so multiple UCons aren't requested in an HTML gen
      pRequestContext.getContextUCon().startRetainingUCon();
      try {
        EvaluatedParseTree lEPT = new EvaluatedParseTree(pRequestContext, mFieldSetOut, this);

        HTMLSerialiser lOutputSerialiser = new HTMLSerialiser(lEPT);
        FoxResponse lFoxResponse;

        long lBrowserCacheTimeMS = establishResponseCacheTimeMS(pRequestContext);

        // TODO - Add in method to switch between streamed and regular response
        ResponseMethod lResponseMethod = pRequestContext.getRequestApp().getResponseMethod();
        if (lResponseMethod == ResponseMethod.STREAMING) {
          // Streaming output mode
          try {
            lFoxResponse = new FoxResponseCHARStream("text/html; charset=UTF-8", pRequestContext.getFoxRequest(), lBrowserCacheTimeMS);
            lFoxResponse.setHttpHeader("Cache-Control", "private");
            Writer lWriter = ((FoxResponseCHARStream) lFoxResponse).getWriter();
            lOutputSerialiser.serialise(lWriter);
            //Immediately flush the writer so user sees the generated page as soon as possible
            lWriter.flush();
          }
          catch (Throwable th) {
            // TODO - NP - The streaming error handling should be handled by the error filter, checking the response method and using an output serialiser set...somewhere
            // Handle streaming error
            long lErrorRef = ErrorLogger.instance().logError(th, ErrorLogger.ErrorType.FATAL, pRequestContext.getFoxRequest().getRequestLogId());
            String lPreviousFieldSetLabel = null;
            if (mFieldSetIn != null) {
              lPreviousFieldSetLabel = mFieldSetIn.getOutwardFieldSetLabel();
            }
            lOutputSerialiser.handleStreamingError(th, lErrorRef, getThreadId(), lPreviousFieldSetLabel);

            throw new ExAlreadyHandled("Error occurred during streaming HTML Generation and has been handled by StatefulXThread.generateHTMLResponse()", th);
          }
        }
        else if (lResponseMethod == ResponseMethod.BUFFERED) {
          // Regular buffer and output mode
          StringWriter lSB = new StringWriter();
          lOutputSerialiser.serialise(lSB);
          lFoxResponse = new FoxResponseCHAR("text/html; charset=UTF-8", lSB.getBuffer(), lBrowserCacheTimeMS);
          lFoxResponse.setHttpHeader("Cache-Control", "private");
        }
        else {
          throw new ExInternal("Don't know how to respond with ResponseMethod: " + lResponseMethod);
        }

        return lFoxResponse;
      }
      finally {
        //ContextUCon can now release any connection it has if appropriate
        pRequestContext.getContextUCon().stopRetainingUCon();
      }
    }
    finally {
      Track.timerPause(TrackTimer.OUTPUT_GENERATION);
      Track.pop("HTMLGenerator");
    }
  }

  private long establishResponseCacheTimeMS(ActionRequestContext pRequestContext) {

    HttpServletRequest lHttpRequest = pRequestContext.getFoxRequest().getHttpRequest();

    long lBrowserCacheTimeMS;
    if(!"GET".equals(lHttpRequest.getMethod()) || FoxMainServlet.isThreadResumeRequest(lHttpRequest)) {
      //If this is a POST, or a thread resume (i.e. a stateful GET), send a proper timeout to prevent browsers re-requesting the page on back/forward navigation
      if(pRequestContext.getAuthenticationContext().isAuthenticated()) {
        //If we're authenticated we should use the user's session timeout value so the page isn't cached after session expiry
        lBrowserCacheTimeMS = TimeUnit.MINUTES.toMillis(pRequestContext.getAuthenticationContext().getSessionTimeoutMins());
      }
      else {
        //Otherwise treat the page like a standard component with a normal timeout value
        lBrowserCacheTimeMS = ComponentManager.getComponentBrowserCacheMS();
      }
    }
    else {
      //If this is a normal GET, set a 0 timeout so the browser doesn't cache dynamic content (i.e. the LOGIN module, or an entry theme with GET params)
      lBrowserCacheTimeMS = 0;
    }

    return lBrowserCacheTimeMS;
  }

  private void finaliseBeforeResponse(RequestContext pRequestContext) {
    //Send the latest Fox session ID as a cookie in the response, and pull this thread's session ID up to date if another
    //thread has caused it to change while this one was running (only if we're actually doing session checks)
    if(!mThreadPropertyMap.getBooleanProperty(ThreadProperty.Type.IS_SKIP_FOX_SESSION_CHECK)) {
      String lFoxSessionID = pRequestContext.getFoxSession().finaliseSession(pRequestContext, getFoxSessionID());
      setFoxSessionID(lFoxSessionID);
    }
  }

  private void finaliseAfterActionProcessing(ActionRequestContext pRequestContext){

    notifyEventListeners(ThreadEventType.FINISH_REQUEST_PROCESSING);

    //Delete the thread on exit if all module calls have been exited
    if(mModuleCallStack.getStackSize() == 0) {
      mPersistenceContext.requiresPersisting(this, PersistenceMethod.DELETE);
    }

    mPersistenceContext.endPersistenceCycle(pRequestContext);

    //Set the expected fieldset for the next incoming request
    mFieldSetIn = mFieldSetOut;

    mHasBeenMounted = true;

    //Clean down the Saxon environment
    SaxonEnvironment.clearThreadLocalRequestContext();
  }

  FoxResponseCHAR debugPage(){

    String lDebugInfo = "<html><body>";

    //Thread state
    lDebugInfo += "<h1>Thread " + mThreadId + " State</h1>";
    lDebugInfo += "<ul>";
    lDebugInfo += "<li>FieldSet in: " + mFieldSetIn.getOutwardFieldSetLabel() +"</li>";
    lDebugInfo += "<li>FieldSet out: " + mFieldSetOut.getOutwardFieldSetLabel() +"</li>";
    lDebugInfo += "<li>Authentication Context: <pre>" + XStreamManager.serialiseObjectToXMLString(mAuthenticationContext).replaceAll("<", "&lt;").replaceAll(">", "&gt;") +"</pre></li>";
    lDebugInfo += "<li>Fox Session id: " + mFoxSessionID +"</li>";
    lDebugInfo += "<li>Property map: " + mThreadPropertyMap.toString() +"</li>";
    lDebugInfo += "</ul>";
    lDebugInfo += mModuleCallStack.debugOutput();

    lDebugInfo += "</body></html>";

    return new FoxResponseCHAR("text/html", new StringBuffer(lDebugInfo), 0);
  }

  static StatefulXThread createNewThreadFromExisting(ActionRequestContext pMainThreadRequestContext, StatefulXThread pExistingThread, ModuleCall.Builder pModuleCallBuilder, boolean pSameSession) {

    App lNewModuleApp = pModuleCallBuilder.getEntryTheme().getModule().getApp();

    XThreadBuilder lThreadBuilder = new XThreadBuilder(lNewModuleApp.getMnemonicName(), pExistingThread.getAuthenticationContext());
    lThreadBuilder.setBooleanThreadProperty(ThreadProperty.Type.IS_ORPHAN, true);
    lThreadBuilder.setBooleanThreadProperty(ThreadProperty.Type.IS_RESUME_ALLOWED, true);

    if(pSameSession){
      //If we need to use the same session, tell the builder - otherwise allow the builder to bootstrap a new one
      lThreadBuilder.setUserThreadSession(pExistingThread.mUserThreadSession);
    }

    StatefulXThread lNewThread = lThreadBuilder.createXThread(pMainThreadRequestContext); //StatefulXThread.createNewXThread(pMainThreadRequestContext, lNewModuleApp.getMnemonicName(), pExistingThread.getAuthenticationContext(), true, lSessionForNewThread);

    //Construct a new RequestContext for the entry theme of the new thread
    XThreadActionRequestContext lNewThreadRequestContext = new XThreadActionRequestContext(pMainThreadRequestContext, lNewThread);

    //We have to temporarily replace the ThreadLocal RequestContext to allow the new thread to start (it'll set this for itself)
    SaxonEnvironment.clearThreadLocalRequestContext();
    try {
      //Start the thread but don't ask for a response
      lNewThread.startThread(lNewThreadRequestContext, pModuleCallBuilder, false);
    }
    finally {
      //Restore the ThreadLocal RequestContext to its original contents (i.e. for the main thread we're currently running).
      SaxonEnvironment.setThreadLocalRequestContext(pMainThreadRequestContext);
    }

    return lNewThread;
  }

  ModuleCall getTopModuleCall(){
    return mModuleCallStack.getTopModuleCall();
  }

  @Override
  public String getThreadRef() {
    return mAppMnem + "/" + mThreadId;
  }

  private ExitResponse getExitResponse(ActionRequestContext pRequestContext){

    String lThreadExitURI = mThreadPropertyMap.getStringProperty(ThreadProperty.Type.EXIT_URI);
    if(!XFUtil.isNull(lThreadExitURI)) {
      //Thread has an explicit exit URI specified on it
      return new ResponseOverride(new URIResourceReference(lThreadExitURI));
    }
    else {
      //Get the exit page from the app definition - it'll be either a module name or a fixed URI
      String lExitPage = pRequestContext.getRequestApp().getExitPage();

      //Belt and braces check that something is defined
      if(XFUtil.isNull(lExitPage)) {
        //Previous behaviour was to go to google.com - raise an error instead
        throw new ExInternal("Exit thread with no destination defined");
      }

      if (!pRequestContext.createURIBuilder().isFixedURI(lExitPage)){
        //Exit page reference is not a "fixed" URI (i.e. does not start http://) - treat it as a module reference and make sure a push to this module occurs
        return new ModulePushExitResponse(lExitPage);
      }
      else {
        //Not a module name, just treat it as an absolute URL and send a redirect
        return new ResponseOverride(new URIResourceReference(lExitPage));
      }
    }
  }

  public ExitResponse getDefaultExitResponse(ActionRequestContext pRequestContext){
    if(isOrphanThread() && XFUtil.isNull(mThreadPropertyMap.getStringProperty(ThreadProperty.Type.EXIT_URI))){
      //Serve out some window closing JS if this is a modeless thread and no exit URI was explcitly specified on it
      return new ResponseOverride(new FoxResponseCHAR("text/html", new StringBuffer("<html><head><script>self.close(); opener.focus();</script></head></html>"), 0L));
    }
    else {
      //Use the thread's exit URI if defined, or defer to the app's default
      return getExitResponse(pRequestContext);
    }
  }

  //TODO THIS SHOULD NOT BE ALLOWED - required for SYS/TEMP DOM
  public ModuleCallStack getModuleCallStack() {
    return mModuleCallStack;
  }

  @Override
  public String getEntryURI(RequestURIBuilder pURIBuilder){
    return FoxMainServlet.buildThreadResumeEntryURI(pURIBuilder, mThreadId, mAppMnem);
  }

  void addSysDOMInfo(String pPath, String pContent) {
    mDOMProvider.getSysDOMHandler().addInfo(pPath, pContent);
  }

  void setScrollPosition(int pNewPosition){
    getTopModuleCall().setScrollPosition(pNewPosition);
  }

  @Override
  public AuthenticationContext getAuthenticationContext() {
    return mAuthenticationContext;
  }

  void postDOM(ActionRequestContext pRequestContext, String pDOMLabel) {
    DOMHandler lDOMHandler = getTopModuleCall().getContextUElem().getDOMHandlerForLabel(pDOMLabel);

    if(lDOMHandler instanceof PostableDOMHandler){
      ((PostableDOMHandler) lDOMHandler).postDOM(pRequestContext);
    }
    else if (lDOMHandler == null) {
      throw new ExInternal("Cannot locate a handler for DOM '" + pDOMLabel + "'");
    }
    else {
      Track.debug("SkipPostDOM", "Cannot post non-postable DOM " + pDOMLabel + "; skipping", TrackFlag.ALERT);
    }
  }

  private void setCachedResponse(FoxResponse pResponse){
    mCachedResponse = pResponse;
  }

  private FoxResponse getAndClearCachedResponse(){
    FoxResponse lCachedResponse = mCachedResponse;
    mCachedResponse = null;
    return lCachedResponse;
  }

  private void newChangeNumber() {
    mChangeNumber = CHANGE_NO_UNIQUE_ITERATOR.next();
  }

  private boolean isOrphanThread() {
    return mThreadPropertyMap.getBooleanProperty(ThreadProperty.Type.IS_ORPHAN);
  }

  private void setBooleanThreadProperty(ThreadProperty.Type pProperty, boolean pValue) {
    mThreadPropertyMap.setBooleanProperty(pProperty, pValue);
    //TODO PN - mark property map as requiring update
    //mPersistenceContext...
  }

  private void setStringThreadProperty(ThreadProperty.Type pProperty, String pValue) {
    mThreadPropertyMap.setStringProperty(pProperty, pValue);
    //TODO PN - mark property map as requiring update
    //mPersistenceContext...
  }

  public String getThreadAppMnem() {
    return mAppMnem;
  }

  boolean hasBeenMounted() {
    return mHasBeenMounted;
  }

  @Override
  public String getThreadId() {
    return mThreadId;
  }

  UserThreadSession getUserThreadSession() {
    return mUserThreadSession;
  }

  @Override
  public String getUserThreadSessionId() {
    return mUserThreadSession.getSessionId();
  }

  @Override
  public int getScrollPosition() {
    return getTopModuleCall().getScrollPosition();
  }

  @Override
  public String getCurrentCallId() {
    return getTopModuleCall().getCallId();
  }

  public PersistenceContext getPersistenceContext() {
    return mPersistenceContext;
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    newChangeNumber();
    pPersistenceContext.getSerialiser().createThread(mThreadId, mAppMnem, mUserThreadSession.getSessionId(), mThreadPropertyMap, mFieldSetOut,
                                                     mAuthenticationContext, mChangeNumber, mFoxSessionID);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.CREATE));
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
    newChangeNumber();
    pPersistenceContext.getSerialiser().updateThread(mThreadId, mFieldSetOut, mAuthenticationContext, mThreadPropertyMap, mChangeNumber, mFoxSessionID);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.UPDATE));
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().deleteThread(mThreadId);

    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.DELETE));
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.THREAD;
  }

  StatefulXThreadDOMProvider getDOMProvider() {
    return mDOMProvider;
  }

  public DevToolbarContext getDevToolbarContext() {
    return mDevToolbarContext;
  }

  FieldSet getFieldSetIn() {
    return mFieldSetIn;
  }

  public boolean checkUploadAllowed(String pTargetRef) {
    return mFieldSetIn.isValidUploadTarget(pTargetRef);
  }

  public DownloadManager getDownloadManager() {
    return mDownloadManager;
  }

  public TempResourceProvider getTempResourceProvider() {
    return mTempResourceProvider;
  }
}

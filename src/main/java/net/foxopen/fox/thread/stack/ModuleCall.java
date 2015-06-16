package net.foxopen.fox.thread.stack;


import com.google.common.collect.ImmutableList;
import net.foxopen.fox.App;
import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.dom.handler.InternalDOMHandler;
import net.foxopen.fox.dom.xpath.saxon.XPathVariableManager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.facet.ModuleFacet;
import net.foxopen.fox.module.facet.ModuleFacetProvider;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.mapset.MapSetManager;
import net.foxopen.fox.thread.DOMHandlerProvider;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.facet.ModuleCallFacetProvider;
import net.foxopen.fox.thread.persistence.Persistable;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceFacet;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;
import net.foxopen.fox.thread.persistence.data.ModuleCallPersistedData;
import net.foxopen.fox.thread.stack.ModuleStateChangeListener.EventType;
import net.foxopen.fox.thread.stack.callback.CallbackHandler;
import net.foxopen.fox.thread.stack.transform.ModelessWindowOptions;
import net.foxopen.fox.thread.storage.DataDOMStorageLocation;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


//TODO doco note about mContextUElem verus pRequestContext.getContextUElem (latter points to former, use former in this class)
public class ModuleCall
implements Persistable {

  private final String mAppMnem;
  private final String mModuleName;
  private final String mEntryThemeName;

  private final String mCallId;
  private final ContextUElem mContextUElem;
  private final ModuleXPathVariableManager mXPathVariableManager;
  private final List<CallbackHandler> mCallbackHandlerList;

  private final ModuleCallStack mOwningCallStack;

  private final Map<Class<? extends ModuleFacetProvider>, ModuleFacetProvider> mFacetProviders = new HashMap<>();

  private final StateCallStack mStateCallStack;

  private SecurityScope mSecurityScope = SecurityScope.defaultInstance();

  private final Map<String, WorkingDataDOMStorageLocation> mLabelToWorkingStorageLocationMap;

  private boolean mEntryThemeProcessed = false;
  private boolean mIsMounted = false;

  private final MapSetManager mMapSetManager = new MapSetManager();

  public static ModuleCall deserialise(PersistenceContext pPersistenceContext, ModuleCallPersistedData pModuleCallData, ModuleCallStack pOwningCallStack, DOMHandlerProvider pDOMHandlerProvider) {

    String lModuleCallId = pModuleCallData.getCallId();
    Map<String, WorkingDataDOMStorageLocation> lLabelToStorageLocationMap = pModuleCallData.getLabelToStorageLocationMap();

    ContextUElem lContextUElem = restoreContextUElem(pPersistenceContext, pDOMHandlerProvider, lModuleCallId, lLabelToStorageLocationMap);

    String lAppMnem = pModuleCallData.getAppMnem();
    String lModuleName = pModuleCallData.getModuleName();
    String lEntryThemeName =  pModuleCallData.getEntryThemeName();

    //Locate the original entry theme
    EntryTheme lEntryTheme;
    try {
      Mod lModule = Mod.getModuleForAppMnemAndModuleName(lAppMnem, lModuleName);
      lEntryTheme = lModule.getEntryTheme(lEntryThemeName);
    }
    catch (Throwable th) {
      throw new ExInternal("Failed to get theme " + lEntryThemeName + " in mod " + lModuleName + " from app " + lAppMnem + " when retrieving module call " + lModuleCallId, th);
    }

    ModuleCall lCall = new ModuleCall(lModuleCallId, lEntryTheme, lContextUElem, pModuleCallData.getCallbackHandlerList(), pOwningCallStack, lLabelToStorageLocationMap,
                                      pModuleCallData.getSecurityScope(), false, pPersistenceContext, pModuleCallData.getXPathVariableManager());

    return lCall;
  }

  private static ContextUElem restoreContextUElem(PersistenceContext pPersistenceContext, DOMHandlerProvider pHandlerProvider, String pCallId, Map<String, WorkingDataDOMStorageLocation> pLabelToWorkingStorageLocationMap){

    ContextUElem lContextUElem = new ContextUElem();

    //Restore default handlers
    for(DOMHandler lHandler : pHandlerProvider.restoreDefaultDOMHandlers(pPersistenceContext, pCallId)) {
      lContextUElem.registerDOMHandler(lHandler);
    }

    //Restore handlers for working storage locations
    for(WorkingDataDOMStorageLocation lWorkingStoreLocation : pLabelToWorkingStorageLocationMap.values()) {
      DOMHandler lRootDOMHandler = pHandlerProvider.restoreDOMHandlerForWSL(pPersistenceContext, lWorkingStoreLocation, pCallId);
      lContextUElem.registerDOMHandler(lRootDOMHandler);
    }

    return lContextUElem;
  }

  private static ModuleCall createNew(EntryTheme pEntryTheme, DOMHandlerProvider pHandlerProvider, DOM pParamsDOM, DOM pEnvironmentDOM,
                                      List<CallbackHandler> pCallbackHandlerList, ModuleCallStack pOwningCallStack, PersistenceContext pPersistenceContext) {
    String lNewCallId = XFUtil.unique();

    Map<String, WorkingDataDOMStorageLocation> lLabelToWorkingStorageLocationMap = new HashMap<>();
    ContextUElem lContextUElem = bootstrapContextUElem(lNewCallId, pEntryTheme, pHandlerProvider, pParamsDOM, pEnvironmentDOM, lLabelToWorkingStorageLocationMap);

    return new ModuleCall(lNewCallId, pEntryTheme, lContextUElem, pCallbackHandlerList, pOwningCallStack, lLabelToWorkingStorageLocationMap, SecurityScope.defaultInstance(),
                          true, pPersistenceContext, new ModuleXPathVariableManager());
  }

  private ModuleCall (String pCallId, EntryTheme pEntryTheme, ContextUElem pContextUElem, List<CallbackHandler> pCallbackHandlerList, ModuleCallStack pOwningCallStack,
                      Map<String, WorkingDataDOMStorageLocation> pLabelToWorkingStorageLocationMap, SecurityScope pSecurityScope, boolean pIsNew, PersistenceContext pPersistenceContext,
                      ModuleXPathVariableManager pXPathVariableManager) {
    mCallId = pCallId;

    mAppMnem = pEntryTheme.getModule().getApp().getAppMnem();
    mModuleName = pEntryTheme.getModule().getName();
    mEntryThemeName = pEntryTheme.getName();

    mOwningCallStack = pOwningCallStack;
    mLabelToWorkingStorageLocationMap = pLabelToWorkingStorageLocationMap;

    mCallbackHandlerList = pCallbackHandlerList;
    mSecurityScope = pSecurityScope;

    mContextUElem = pContextUElem;

    //Bit nasty, but we have to do this logic here as the StateCallStack needs a reference to the module call (i.e. this can't be constructed externally).
    if(pIsNew) {
      mStateCallStack = new StateCallStack(pPersistenceContext, this);

      //Create a new set of Facet providers for all known provider types
      for(ModuleCallFacetProvider.Builder lBuilder : ModuleCallFacetProvider.getAllBuilders()) {
        mFacetProviders.put(lBuilder.getFacetProviderClass(), lBuilder.createNewProvider(pPersistenceContext, this));
      }
    }
    else {
      mStateCallStack = StateCallStack.deserialise(pPersistenceContext, this);

      //Deserialise the module call's existing Facet providers
      for(ModuleCallFacetProvider.Builder lBuilder : ModuleCallFacetProvider.getAllBuilders()) {
        mFacetProviders.put(lBuilder.getFacetProviderClass(), lBuilder.deserialiseExistingProvider(pPersistenceContext, this));
      }

      mEntryThemeProcessed = true;
    }

    mXPathVariableManager = pXPathVariableManager;
    //This must be set here as it needs a reference to this ModuleCall (either when new or deserialised)
    mXPathVariableManager.setPersistenceContextProxy(() -> pPersistenceContext.requiresPersisting(ModuleCall.this, PersistenceMethod.UPDATE, PersistenceFacet.MODULE_CALL_XPATH_VARIABLES));
  }

  private static ContextUElem bootstrapContextUElem(String pCallId, EntryTheme pEntryTheme, DOMHandlerProvider pHandlerProvider, DOM pParamsDOM, DOM pEnvironmentDOM,
                                                    Map<String, WorkingDataDOMStorageLocation> pLabelToWorkingStorageLocationMap){

    ContextUElem lContextUElem = new ContextUElem();

    //TODO should this validate that certain handlers have been set? E.g. must always have theme etc
    for(DOMHandler lHandler : pHandlerProvider.createDefaultDOMHandlers(pCallId)) {

      if(lHandler instanceof InternalDOMHandler){
        InternalDOMHandler lInternalHandler = (InternalDOMHandler) lHandler;

        //Only call setDOMContents if the documents have content - otherwise we're wasting time writing empty DOMs
        if(ContextLabel.PARAMS.asString().equals(lInternalHandler.getContextLabel()) && pParamsDOM.hasContent()){
          lInternalHandler.setDOMContents(pParamsDOM);
        }
        else if(ContextLabel.ENV.asString().equals(lInternalHandler.getContextLabel()) && pEnvironmentDOM.hasContent()){
          lInternalHandler.setDOMContents(pEnvironmentDOM);
        }
      }

      lContextUElem.registerDOMHandler(lHandler);
    }

    //Set a default attach point for WSL bind evaluation
    lContextUElem.setUElem(ContextLabel.ATTACH, lContextUElem.getUElem(ContextLabel.THEME), ContextLabel.THEME.asString());
    lContextUElem.setUElem(ContextLabel.ACTION, lContextUElem.getUElem(ContextLabel.THEME), ContextLabel.THEME.asString());

    //Create a WSL for each SL defined on the entry theme
    List<DataDOMStorageLocation> lStorageLocationList = pEntryTheme.getStorageLocationList();
    for(DataDOMStorageLocation lStorageLocation : lStorageLocationList) {
      WorkingDataDOMStorageLocation lWorkingStoreLocation;
      try {
        //Create a WSL being sure not to ask for temp table binds - the XThread will deal with temporary WSLs seperately.
        lWorkingStoreLocation = lStorageLocation.createWorkingStorageLocation(lContextUElem, pCallId, pEntryTheme);
      }
      catch (ExInternal x) {
        throw new ExInternal("Error evaluating Data Storage Location", x);
      }

      //Get a handler for this WSL and register on the ContextUElem
      DOMHandler lSLDOMHandler = pHandlerProvider.createDOMHandlerForWSL(lWorkingStoreLocation, pCallId);
      lContextUElem.registerDOMHandler(lSLDOMHandler);

      //Make a record of the WSL
      pLabelToWorkingStorageLocationMap.put(lSLDOMHandler.getContextLabel(), lWorkingStoreLocation);
    }

    return lContextUElem;
  }

  public String getCallId() {
    return mCallId;
  }

  public ContextUElem getContextUElem(){
    return mContextUElem;
  }

  public void setSecurityScope(ActionRequestContext pRequestContext, SecurityScope pSecurityScopeParams) {
    mSecurityScope = pSecurityScopeParams;
    mOwningCallStack.notifyStateChangeListeners(pRequestContext, ModuleStateChangeListener.EventType.SECURITY_SCOPE);
    pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE, PersistenceFacet.MODULE_CALL_SECURITY_SCOPE);
  }

  public SecurityScope getSecurityScope() {
    return mSecurityScope;
  }

  public App getApp(){
    try {
      return FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(mAppMnem);
    }
    catch (ExServiceUnavailable | ExApp e) {
      throw new ExInternal("App not available", e);
    }
  }

  public Mod getModule(){
    try {
      return getApp().getMod(mModuleName);
    }
    catch (ExServiceUnavailable | ExModule | ExApp | ExUserRequest e) {
      throw new ExInternal("Mod not available", e);
    }
  }

  public EntryTheme getEntryTheme(){
    try {
      return getModule().getEntryTheme(mEntryThemeName);
    }
    catch (ExUserRequest e) {
      throw new ExInternal("Theme not available", e);
    }
  }

  /**
   * Can be null
   * @return
   */
  public State getTopState(){
    if(mStateCallStack.getStackSize() > 0){
      return getModule().getState(mStateCallStack.getTopStateName());
    }
    else {
      return null;
    }
  }

  public int getScrollPosition(){
    return mStateCallStack.getTopStateCall().getScrollPosition();
  }

  public void setScrollPosition(int pScrollPosition){
    mStateCallStack.getTopStateCall().setScrollPosition(pScrollPosition);
  }

  void processEntryTheme(ActionRequestContext pRequestContext, XDoRunner pEntryThemeRunner){

    if(mEntryThemeProcessed){
      throw new IllegalStateException("Entry theme has already been processed for this module call");
    }

    EntryTheme lEntryTheme = getEntryTheme();

    Track.pushInfo("TopLevelEntryTheme", lEntryTheme);
    try {
      //Get the context name of the default storage location (ROOT if not specified)
      String lDefaultSLContextLabel = lEntryTheme.getDefaultStorageLocation().getDocumentContextLabel();

      //Set the initial context node for attach/action evaluation to the root element of the default storage location
      DOM lAttachPointForAttachEvalulation = mContextUElem.getUElem(lDefaultSLContextLabel);

      mContextUElem.setUElem(ContextLabel.ATTACH, lAttachPointForAttachEvalulation, lDefaultSLContextLabel);
      mContextUElem.setUElem(ContextLabel.ACTION, lAttachPointForAttachEvalulation, lDefaultSLContextLabel);

      DOM lInitialAttachPoint = null;
      // Evaluate entry theme attach XPATH expression
      if(!XFUtil.isNull(lEntryTheme.getAttachXPath())) {
        try {
          lInitialAttachPoint = mContextUElem.extendedXPath1E(lEntryTheme.getAttachXPath(), false, null);
        }
        catch(ExActionFailed | ExCardinality e) {
          throw new ExInternal("Module entry theme attach expression error", e);
        }
      }
      else {
        lInitialAttachPoint = lAttachPointForAttachEvalulation;
        Track.alert("EntryThemeAttach", "Entry theme attach point not defined; assuming default of root");
      }

      //Add state to the call stack
      StateCall lStateCall = mStateCallStack.statePush(pRequestContext, lEntryTheme.getStateName(), lInitialAttachPoint);
      mOwningCallStack.notifyStateChangeListeners(pRequestContext, EventType.STATE);

      // Reset attach points to their required location as per entry theme (attach is set in state constructor)
      mContextUElem.setUElem(ContextLabel.ACTION, lInitialAttachPoint);

      //Check privs - this may result in a CST which should prevent the main do block / auto state init from running
      XDoControlFlow lSecurityResult = lEntryTheme.getEntryThemeSecurity().evaluate(pRequestContext);

      //Only do entry processing if the security result allows a continue
      if(lSecurityResult.canContinue()) {
        //NOTE BEHAVIOUR CHANGE: the entry theme now runs BEFORE the auto state init (regression from FOX4)

        //Run the entry theme do block
        pEntryThemeRunner.runCommands(pRequestContext, lEntryTheme.getXDo());

        //Run auto state init actions
        lStateCall.runInitActions(pRequestContext, pEntryThemeRunner);
      }
      else {
        //Place the CST caused by a failed security check in the runner, so the module exit is processed in the ModuleCallStack
        pEntryThemeRunner.injectResult(lSecurityResult);
      }
    }
    finally {
      Track.pop("TopLevelEntryTheme");
    }

    mEntryThemeProcessed = true;
  }

  public void mount(ActionRequestContext pRequestContext){
    if(mIsMounted){
      throw new ExInternal("Already mounted.");
    }

    mContextUElem.openDOMHandlers(pRequestContext);

    loadContextualLabels();

    mIsMounted = true;
  }

  void unmountIfMounted(ActionRequestContext pRequestContext){
    if(mIsMounted){
     unmount(pRequestContext);
    }
    else {
      Track.info("SkipUnmount", "Call was already unmounted");
    }
  }

  void unmount(ActionRequestContext pRequestContext){
    getStateCallStack().unmount(pRequestContext);
    unmountModuleCall(pRequestContext);
  }

  void unmountModuleCall(ActionRequestContext pRequestContext){

    if(!mIsMounted){
      throw new IllegalStateException("Cannot unmount a ModuleCall if it is not mounted");
    }

    mContextUElem.closeDOMHandlers(pRequestContext);
    //TODO what about inconsitent state ie half unmounted

    mIsMounted = false;
  }

  /**
   * Runs any state or module level auto-final actions for this ModuleCall.
   * @param pRequestContext
   * @param pStateFinalRunner
   */
  void runFinalActions(ActionRequestContext pRequestContext, XDoRunner pStateFinalRunner){
    if(!mIsMounted){
      throw new IllegalStateException("Must be mounted");
    }

    StateCall lCurrentStateCall = mStateCallStack.getTopStateCall();
    lCurrentStateCall.runFinalActions(pRequestContext, pStateFinalRunner);
  }

  public boolean isMounted(){
    return mIsMounted;
  }

  public List<CallbackHandler> getCallbackHandlerList(){
    return Collections.unmodifiableList(mCallbackHandlerList);
  }

  StateCallStack getStateCallStack(){
    return mStateCallStack;
  }

  void fireStateChangeEvent(ActionRequestContext pRequestContext) {
    mOwningCallStack.notifyStateChangeListeners(pRequestContext, EventType.STATE);
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    Collection<PersistenceResult> lImplicated = new HashSet<>();

    //Delete the state call stack
    lImplicated.addAll(mStateCallStack.delete(pPersistenceContext));

    //Mark DOMs as deleted so any attempted updates are skipped
    //TODO PN - actually delete them (if not too slow)
    for(InternalDOMHandler lDOMHandler : mContextUElem.getDOMHandlersByType(InternalDOMHandler.class)) {
      lImplicated.add(new PersistenceResult(lDOMHandler, PersistenceMethod.DELETE));
    }

    //Delete all facets and mark as deleted
    pPersistenceContext.getSerialiser().deleteModuleCallFacets(mCallId);
    for(ModuleFacetProvider lProvider : mFacetProviders.values()) {
      for(ModuleFacet lFacet : lProvider.getAllFacets()) {
        lImplicated.add(new PersistenceResult(lFacet, PersistenceMethod.DELETE));
      }
    }

    //Delete the module call
    pPersistenceContext.getSerialiser().deleteModuleCall(mCallId);
    lImplicated.add(new PersistenceResult(this, PersistenceMethod.DELETE));

    return lImplicated;
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().createModuleCall(mCallId, mOwningCallStack.moduleCallIndex(this), mAppMnem, mModuleName, mEntryThemeName,
                                                         mLabelToWorkingStorageLocationMap, mCallbackHandlerList, mSecurityScope, mXPathVariableManager);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.CREATE));
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().updateModuleCall(mCallId, mSecurityScope, mXPathVariableManager);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.UPDATE));
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.MODULE_CALL;
  }

  void loadContextualLabels() {
    if(mStateCallStack.getStackSize() > 0) {
      mStateCallStack.getTopStateCall().loadContextualLabels(mContextUElem);
    }
  }

  public <T extends ModuleFacetProvider> T getModuleFacetProvider(Class<T> pProviderClass) {
    return pProviderClass.cast(mFacetProviders.get(pProviderClass));
  }

  String debugFacetProviders() {

    StringBuilder lResult = new StringBuilder();
    for( Map.Entry<Class<? extends ModuleFacetProvider>, ModuleFacetProvider> lEntry : mFacetProviders.entrySet()) {
      lResult.append("<li>" + lEntry.getKey().getSimpleName() + ": <br>");
      lResult.append("<pre>" + lEntry.getValue().debugOutput().replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "</pre></li>");
    }

    return lResult.toString();
  }

  public static class Builder{

    private final EntryTheme mEntryTheme;

    private DOM mParamsDOM = null;
    private DOM mEnvironmentDOM = null;

    private ModelessWindowOptions mModelessWindowOptions = null;

    private List<CallbackHandler> mCallbackHandlerList = new ArrayList<>();

    public Builder(EntryTheme pEntryTheme){
      mEntryTheme = pEntryTheme;
    }

    public Builder setParamsDOM(DOM pParamsDOM){
      mParamsDOM = pParamsDOM;
      return this;
    }

    public Builder setEnvironmentDOM(DOM pEnvironmentDOM){
      mEnvironmentDOM = pEnvironmentDOM;
      return this;
    }

    public Builder addCallbackHandler(CallbackHandler pCallbackHandler){
      mCallbackHandlerList.add(pCallbackHandler);
      return this;
    }

    public Builder clearCallbackHandlers(){
      mCallbackHandlerList.clear();
      return this;
    }

    /**
     * Can be null.
     * @return
     */
    public DOM getEnvironmentDOM() {
      return mEnvironmentDOM;
    }

    ModuleCall build(ModuleCallStack pTargetCallStack, DOMHandlerProvider pDOMHandlerProvider, PersistenceContext pPersistenceContext){

      DOM lParamsDOM = mParamsDOM;
      if(lParamsDOM == null){
        lParamsDOM = DOM.createDocument(ContextLabel.PARAMS.asString());
      }

      DOM lEnvironmentDOM = mEnvironmentDOM;
      if(lEnvironmentDOM == null){
        lEnvironmentDOM = DOM.createDocument(ContextLabel.ENV.asString());
      }

      return ModuleCall.createNew(mEntryTheme, pDOMHandlerProvider, lParamsDOM, lEnvironmentDOM, ImmutableList.copyOf(mCallbackHandlerList), pTargetCallStack, pPersistenceContext);
    }

    public EntryTheme getEntryTheme() {
      return mEntryTheme;
    }

    public void setModelessWindowOptions(ModelessWindowOptions pModelessWindowOptions) {
      mModelessWindowOptions = pModelessWindowOptions;
    }

    public ModelessWindowOptions getModelessWindowOptions() {
      return mModelessWindowOptions;
    }
  }

  public MapSetManager getMapSetManager() {
    return mMapSetManager;
  }

  public XPathVariableManager getXPathVariableManager() {
    return mXPathVariableManager;
  }
}

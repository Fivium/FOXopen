package net.foxopen.fox.thread;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoIsolatedRunner;
import net.foxopen.fox.command.XDoResult;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.PathOrDOM;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.download.DownloadManager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.facet.ModuleFacetProvider;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.transform.StateStackTransformation;
import net.foxopen.fox.thread.storage.TempResourceProvider;

import java.util.ArrayList;
import java.util.List;


public class XThreadActionRequestContext
extends RequestContextImpl
implements ActionRequestContext {

  private final StatefulXThread mXThread;

  private final List<XDoResult> mXDoResultList = new ArrayList<>();

  public XThreadActionRequestContext(RequestContext pRequestContext, StatefulXThread pXThread) {
    super(pRequestContext);
    mXThread = pXThread;
  }

  @Override
  public ContextUElem getContextUElem() {
    return mXThread.getTopModuleCall().getContextUElem();
  }

  @Override
  public Mod getCurrentModule(){
    return mXThread.getTopModuleCall().getModule();
  }

  @Override
  public State getCurrentState() {
    return mXThread.getTopModuleCall().getTopState();
  }

  @Override
  public EntryTheme getCurrentTheme() {
    return mXThread.getTopModuleCall().getEntryTheme();
  }

  @Override
  public App getModuleApp(){
    return mXThread.getTopModuleCall().getApp();
  }

  @Override
  public App getRequestApp(){
    try {
      return FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(mXThread.getThreadAppMnem());
    }
    catch (ExServiceUnavailable | ExApp e) {
      throw new ExInternal("Failed to retrieve app " + mXThread.getThreadAppMnem(), e);
    }
  }

  @Override
  public XDoCommandList resolveActionName(String pActionName){
    return mXThread.resolveActionName(pActionName);
  }

  @Override
  public void addXDoResult(XDoResult pXDoResult) {
    mXDoResultList.add(pXDoResult);
  }

  @Override
  public <T extends XDoResult> List<T> getXDoResults(Class<T> pForClass) {
    List<T> lResultList = new ArrayList<>();
    for(XDoResult lXDoResult : mXDoResultList){
      if(pForClass.isInstance(lXDoResult)){
        lResultList.add(pForClass.cast(lXDoResult));
      }
    }
    return lResultList;
  }

  @Override
  public XDoRunner createCommandRunner(boolean pIsTopLevel) {
    XDoRunner lXDoRunner = new XDoRunner();
    if(!pIsTopLevel) {
      lXDoRunner.treatIgnoresAsBreaks();
    }
    return lXDoRunner;
  }

  @Override
  public XDoIsolatedRunner createIsolatedCommandRunner(boolean pErrorOnTransformation) {
    return new XDoIsolatedRunner(pErrorOnTransformation);
  }

  @Override
  public DOMHandlerProvider getDOMHandlerProvider() {
    return mXThread.getDOMProvider();
  }

  @Override
  public ResponseOverride getDefaultExitResponse() {
    return mXThread.getDefaultExitResponse(this);
  }

  @Override
  public XThreadInterface createNewXThread(ModuleCall.Builder pModuleCallBuilder, boolean pSameSession) {
    return StatefulXThread.createNewThreadFromExisting(this, mXThread, pModuleCallBuilder, pSameSession);
  }

  @Override
  public XDoControlFlow handleStateStackTransformation(StateStackTransformation pTransformation) {
    return mXThread.getModuleCallStack().handleStateStackTransformation(this, pTransformation);
  }

  @Override
  public void addSysDOMInfo(String pPath, String pContent) {
    mXThread.addSysDOMInfo(pPath, pContent);
  }

  @Override
  public void changeAttachPoint(String pAttachToXPath) {
    try {
      getContextUElem().repositionExtendedXPath(ContextUElem.ATTACH, ContextualityLevel.STATE, pAttachToXPath, false);
    }
    catch (ExActionFailed | ExTooMany | ExTooFew e) {
      throw new ExInternal("Failed to change attach point", e);
    }
    //TODO PN XTHREAD - should this be managed by xthread
    mXThread.setScrollPosition(0);
  }

  @Override
  public AuthenticationContext getAuthenticationContext() {
    return mXThread.getAuthenticationContext();
  }

  @Override
  public SecurityScope getCurrentSecurityScope() {
    if (mXThread.getModuleCallStack().isEmpty()) {
      return SecurityScope.defaultInstance();
    }
    else {
      return mXThread.getTopModuleCall().getSecurityScope();
    }
  }

  @Override
  public void changeSecurityScope(SecurityScope pSecurityScope) {
    mXThread.getTopModuleCall().setSecurityScope(this, pSecurityScope);
  }

  @Override
  public void postDOM(String pDOMLabel) {
    mXThread.postDOM(this, pDOMLabel);
  }

  @Override
  public PersistenceContext getPersistenceContext() {
    return mXThread.getPersistenceContext();
  }

  @Override
  public MapSet resolveMapSet(String pMapSetName, DOM pItemDOM, String pMapSetAttachXPath) {
    return mXThread.getTopModuleCall().getMapSetManager().getMapSet(this, pMapSetName, pItemDOM, new PathOrDOM(pMapSetAttachXPath), mXThread.getCurrentCallId());
  }

  @Override
  public MapSet resolveMapSet(String pMapSetName, DOM pItemDOM, DOM pMapSetAttachDOM) {
    return mXThread.getTopModuleCall().getMapSetManager().getMapSet(this, pMapSetName, pItemDOM, new PathOrDOM(pMapSetAttachDOM), mXThread.getCurrentCallId());
  }

  @Override
  public void refreshMapSets(String pMapSetName) {
    mXThread.getTopModuleCall().getMapSetManager().refreshMapSets(this, pMapSetName);
  }

  @Override
  public DevToolbarContext getDevToolbarContext() {
    return mXThread.getDevToolbarContext();
  }

  @Override
  public DownloadManager getDownloadManager() {
    return mXThread.getDownloadManager();
  }

  @Override
  public TempResourceProvider getTempResourceProvider() {
    return mXThread.getTempResourceProvider();
  }


  public <T extends ModuleFacetProvider> T getModuleFacetProvider(Class<T> pProviderClass) {
    return mXThread.getTopModuleCall().getModuleFacetProvider(pProviderClass);
  }

  @Override
  public String getCurrentCallId() {
    return mXThread.getCurrentCallId();
  }

  @Override
  public void applyClientActions(String pClientActionJSON) {
    mXThread.getFieldSetIn().applyClientActions(this, pClientActionJSON);
  }
}

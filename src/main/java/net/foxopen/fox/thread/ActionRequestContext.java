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
import net.foxopen.fox.download.DownloadManager;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.facet.ModuleFacetProvider;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.mapset.MapSetDefinition;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.transform.StateStackTransformation;
import net.foxopen.fox.thread.storage.TempResourceProvider;

import java.util.List;


public interface ActionRequestContext
extends RequestContext {

  public ContextUElem getContextUElem();

  public App getModuleApp();

  public Mod getCurrentModule();

  public State getCurrentState();

  public EntryTheme getCurrentTheme();

  public App getRequestApp();

  public AuthenticationContext getAuthenticationContext();

  public XDoCommandList resolveActionName(String pActionName);

  /**
   * Creates a new CommandRunner for running actions and handling the complexities of ACTIONIGNORE/BREAKS, call stack transformations,
   * etc.
   * @param pIsTopLevel True if the runner will be running a "top level" command block, i.e. an entry theme or external
   *                    action. For nested commands this argument should be false. This affects how ACTIONIGNOREs are handled.
   * @return A new CommandRunner in an initial state.
   */
  public XDoRunner createCommandRunner(boolean pIsTopLevel);

  public XDoIsolatedRunner createIsolatedCommandRunner(boolean pErrorOnTransformation);

  public void addXDoResult(XDoResult pXDoResult);

  public <T extends XDoResult> List<T> getXDoResults(Class<T> pForClass);

  public DOMHandlerProvider getDOMHandlerProvider();

  public ResponseOverride getDefaultExitResponse();

  public XDoControlFlow handleStateStackTransformation(StateStackTransformation pTransformation);

  //TODO name
  public XThreadInterface createNewXThread(ModuleCall.Builder pModuleCallBuilder, boolean pSameSession);

  public void addSysDOMInfo(String pPath, String pContent);

  public void changeAttachPoint(String pAttachToXPath);

  public void changeSecurityScope(SecurityScope pSecurityScope);

  /**
   * Resolves the MapSet of the given name for the given item DOM. See {@link MapSetDefinition#getOrCreateMapSet} for
   * implementation details.
   * @param pMapSetName Name of MapSet to resolve.
   * @param pItemDOM MapSet item - can be null if the MapSet definition contains no reference to :{item} or :{itemrec}
   * contexts.
   * @param pMapSetAttachXPath Optional extra attach point for evaluating the MapSet.
   * @return Targeted MapSet.
   */
  public MapSet resolveMapSet(String pMapSetName, DOM pItemDOM, String pMapSetAttachXPath);

  /**
   * Resolves the MapSet of the given name for the given item DOM. See {@link MapSetDefinition#getOrCreateMapSet} for
   * implementation details.
   * @param pMapSetName Name of MapSet to resolve.
   * @param pItemDOM MapSet item - can be null if the MapSet definition contains no reference to :{item} or :{itemrec}
   * contexts.
   * @param pMapSetAttachDOM Optional extra attach point for evaluating the MapSet.
   * @return Targeted MapSet.
   */
  public MapSet resolveMapSet(String pMapSetName, DOM pItemDOM, DOM pMapSetAttachDOM);

  /**
   * Refreshes any MapSets which correspond to the MapSetDefinition resolved by the given MapSet name.
   * @param pMapSetName Name of MapSet to refresh instances of.
   */
  public void refreshMapSets(String pMapSetName);

  //TODO name, should this be allowed here?
  public void postDOM(String pDOMLabel);

  public PersistenceContext getPersistenceContext();

  public DevToolbarContext getDevToolbarContext();

  public DownloadManager getDownloadManager();

  public TempResourceProvider getTempResourceProvider();

  public <T extends ModuleFacetProvider> T getModuleFacetProvider(Class<T> pProviderClass);

  //TODO PN is this OK?
  public String getCurrentCallId();

  public void applyClientActions(String pClientActionJSON);

}

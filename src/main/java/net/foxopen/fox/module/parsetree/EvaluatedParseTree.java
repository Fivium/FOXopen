package net.foxopen.fox.module.parsetree;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.foxopen.fox.App;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthenticatedUser;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.banghandler.InternalAuthentication;
import net.foxopen.fox.command.XDoResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.download.DownloadLinkXDoResult;
import net.foxopen.fox.download.DownloadManager;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilderImpl;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExSecurity;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.PresentationAttribute;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.clientvisibility.ClientVisibilityRule;
import net.foxopen.fox.module.clientvisibility.EvaluatedClientVisibilityRule;
import net.foxopen.fox.module.datadefinition.EvaluatedDataDefinition;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeEvaluationContext;
import net.foxopen.fox.module.facet.ModuleFacetProvider;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.BufferPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.ThreadInfoProvider;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.security.SecurityManager;
import net.foxopen.fox.security.SecurityOperationDescriptor;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.alert.AlertMessage;
import net.foxopen.fox.thread.FocusResult;
import net.foxopen.fox.thread.PopupXDoResult;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.stack.transform.ModelessCall.ModelessPopup;
import net.foxopen.fox.thread.storage.TempResource;
import net.foxopen.fox.thread.storage.TempResourceGenerator;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.util.RandomString;
import org.json.simple.JSONArray;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


/**
 * A tree of Evaluated Presentation Nodes that hold the evaluated information for the presentation of a module
 */
public class EvaluatedParseTree implements SerialisationContext {

  /** Used to sort list of buffers for skip link list */
  private static final Comparator<EvaluatedBufferPresentationNode> BUFFER_REGION_COMPARATOR = new Comparator<EvaluatedBufferPresentationNode>() {
    public int compare(EvaluatedBufferPresentationNode pBufferA, EvaluatedBufferPresentationNode pBufferB) {
      return pBufferA.getRegionOrder() - pBufferB.getRegionOrder();
    }
  };

  /** Amount of recursions allowed before a hard error is thrown */
  public static final int MAX_RECURSION_LEVEL = 500;

  /** Root buffer node for this tree */
  private final EvaluatedPresentationNode<? extends PresentationNode> mRootBuffer;

  /** Holds a reference to the current state to be processed */
  private final State mState;

  private final Map<String, PresentationAttribute> mStateAttributes;

  /** Holds a reference to the module metadata object so that information can be extracted from the schema */
  private final Mod mModule;

  /** Holds a reference to the App */
  private final App mApp;

  /** Holds a reference to the current request context */
  private final ActionRequestContext mRequestContext;

  /** Cached mode-rule security table operation results. */
  private final SecurityOperationDescriptor mModeRulesOpsDescriptor;

  /** Cached view-rule security table operation results. */
  private final SecurityOperationDescriptor mViewRulesOpsDescriptor;

  /** Store references to buffers to use for accessibility skip links */
  private final List<EvaluatedBufferPresentationNode> mBufferRegions = new ArrayList<>();

  /** All WidgetBuilders which will be set out on the page, mapped to all their implicated ENs */
  private final Multimap<WidgetBuilderType, EvaluatedNode> mImplicatedWidgetsToEvaluatedNodeInfos = HashMultimap.create();

  /** Chunks of javascript to run on load if page not expired */
  private final List<String> mConditionalLoadJavascript = new ArrayList<>();

  /** Chunks of javascript to run on load regardless of page expired */
  private final List<String> mUnconditionalLoadJavascript = new ArrayList<>();

  /** Store a reference to the defaultAction so it can be used in the event of a clash to give better debug info */
  private EvaluatedNode mDefaultAction = null;

  private final FieldSet mFieldSet;

  private final ThreadInfoProvider mThreadInfoProvider;

  /** Unmodifiable URIBuilder for generating URIs without params. Cached here to cut down on object creation. */
  private final RequestURIBuilder mURIBuilderInstance;

  private final List<EvaluatedClientVisibilityRule> mEvaluatedClientVisibilityRules = new ArrayList<>();

  private final List<EvaluatedDataDefinition> mEvaluatedDataDefinitions;

  /** Tracks recursion within child evaluation (to prevent infinite buffer recursion) */
  private int mRecursionLevel = 0;

  /** Used to validate uniqueness of certain facets (i.e. tab groups) within a parse tree evaluation cycle */
  private Set<String> mUniqueFacetKeys = new HashSet<>();

  /** Deque of DOM nodes used for context when evaluating buffers dynamically so fm:label can target the prompt-buffer caller */
  private Deque<DOM> mCurrentBufferContextNode = new ArrayDeque<>();

  /** Currently displayed modal popover, or null if none is currently displayed */
  private final EvaluatedModalPopover mOptionalModalPopover;

  public EvaluatedParseTree(ActionRequestContext pRequestContext, FieldSet pFieldSet, List<EvaluatedDataDefinition> pEvaluatedDataDefinitions, ThreadInfoProvider pThreadInfoProvider) {
    mRequestContext = pRequestContext;
    mState = pRequestContext.getCurrentState();
    mStateAttributes = PresentationAttribute.convertAttributeMap(mState.getStateAttributes(), null, false);
    mModule = pRequestContext.getCurrentModule();
    mApp = pRequestContext.getModuleApp();
    mFieldSet = pFieldSet;
    mEvaluatedDataDefinitions = pEvaluatedDataDefinitions;

    //Construct a stateless URI builder which does not allow parameters to be set
    mURIBuilderInstance = RequestURIBuilderImpl.createFromRequestContext(pRequestContext, false);

    mThreadInfoProvider = pThreadInfoProvider;

    // Figure out the mode/view rules
    try {
      mModeRulesOpsDescriptor = SecurityManager.getInstance().determineModeListFromModule(pRequestContext);
      mViewRulesOpsDescriptor = SecurityManager.getInstance().determineViewListFromModule(pRequestContext);
    }
    catch (ExSecurity ex) {
      throw ex.toUnexpected();
    }

    Track.pushInfo("EvaluatedParseTree", "Evaluating the parse tree");
    // Start from the set-page buffer for this module and evaluate our way down the Parse Tree nodes
    try {
      BufferPresentationNode lStartBuffer = getBuffer(null);
      mRootBuffer = evaluateNode(null, lStartBuffer, getContextUElem().attachDOM());
    }
    catch (ExModule e) {
      throw new ExInternal("Failed to find buffer", e);
    }
    finally {
      Track.pop("EvaluatedParseTree");
    }

    //Evaluate the modal popover if one is active
    mOptionalModalPopover = EvaluatedModalPopover.getEvaluatedPopoverOrNull(mRequestContext, this);

    handleClientVisibilityRules();
  }

  /**
   * Get an EvaluatedPresentationNode tree from a PresentationNode tree
   *
   * @param pParent Evaluated parent for pNode
   * @param pNode Presentation Node to evaluate
   * @param pEvalContext DOM context to run our XPaths on
   * @return Evaluated version of the pNode and its children
   */
  public EvaluatedPresentationNode<? extends PresentationNode> evaluateNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, PresentationNode pNode, DOM pEvalContext) {
    EvaluatedPresentationNode<? extends PresentationNode> lEvaluatedNode = pNode.evaluate(pParent, this, pEvalContext);

    if(++mRecursionLevel > MAX_RECURSION_LEVEL) {
      throw generateRecursionCycleException(pParent);
    }

    if (lEvaluatedNode != null && lEvaluatedNode.canEvaluateChildren()) {
      lEvaluatedNode.evaluateChildren(this);
    }

    mRecursionLevel--;

    return lEvaluatedNode;
  }

  /**
   * Get buffer from the state/module. If null Buffer Name is passed in it defaults to the set-page buffer from the
   * current state, or if no set-page on the current state it gets it from the module.<br /><br />
   *
   * pBufferName can be just a buffer name, in which case it tries to get it from the current state or if no buffer
   * exists on the current state with that name it gets it from the module.<br /><br />
   *
   * pBufferName can also be "state/buffer", in which case it tried to find the state specified in the current module
   * and will then look for the buffer in that state or the current module.
   *
   * @param pBufferName Name of the buffer, can be just buffer name or state/buffer or null to default to current state/set-page
   * @return BufferPresentationNode from the State/Module
   * @throws ExModule If state/buffer not found / invalid
   */
  public BufferPresentationNode getBuffer(String pBufferName) throws ExModule {
    // Split up state/buffer
    String lBuffer = null;
    String lState = null;
    if (XFUtil.exists(pBufferName)) {
      int lSlashPosition = pBufferName.indexOf('/');
      if (lSlashPosition != -1) {
        lState = pBufferName.substring(0, lSlashPosition);
        lBuffer = pBufferName.substring(lSlashPosition+1, pBufferName.length());
      }
      else {
        lBuffer = pBufferName;
      }
    }

    // Find the BufferPresentationNode for the State and Buffer in the params
    State lPageState;
    BufferPresentationNode lPageBuffer;

    if (XFUtil.exists(lState)) {
      // If a state was specified, attempt to get it from the current module
      lPageState = mModule.getState(lState); // Throws ExInternal if lState not defined
    }
    else {
      lPageState = mState;
    }

    if (!XFUtil.exists(lBuffer)) {
      // find the set-page at state level
      lPageBuffer = lPageState.getSetPageBuffer();
      if (lPageBuffer == null) {
        // find the set-page at module level
        lPageBuffer = mModule.getSetPageBuffer();
      }
    }
    else {
      // find buffer at state level
      lPageBuffer = lPageState.getParsedBuffer(lBuffer);
      if (lPageBuffer == null) {
        // find buffer at module level
        lPageBuffer = mModule.getParsedBuffer(lBuffer);
      }
    }

    if (lPageBuffer == null) {
      if(pBufferName == null) {
        throw new ExModule("Evaluated Parse Tree error: failed to find the presentation root (fm:set-page is not defined)");
      }
      else {
        throw new ExModule("Evaluated Parse Tree error: There is no html buffer called '" + lBuffer + "' under the current state: " + lPageState.getName());
      }
    }

    return lPageBuffer;
  }

  public SecurityOperationDescriptor getModeRulesOpsDescriptor() {
    return mModeRulesOpsDescriptor;
  }

  public SecurityOperationDescriptor getViewRulesOpsDescriptor() {
    return mViewRulesOpsDescriptor;
  }

  public EvaluatedPresentationNode<? extends PresentationNode> getRootBuffer() {
    return mRootBuffer;
  }

  @Override
  public State getState() {
    return mState;
  }

  public Map<String, PresentationAttribute> getStateAttributes() {
    return mStateAttributes;
  }

  @Override
  public FieldSet getFieldSet() {
    return mFieldSet;
  }

  @Override
  public FocusResult getFocusResult(){
    List<FocusResult> lFocusResults = mRequestContext.getXDoResults(FocusResult.class);
    if (lFocusResults.size() > 0) {
      return lFocusResults.get(lFocusResults.size() - 1);
    }
    else {
      return null;
    }
  }

  @Override
  public List<AlertMessage> getAlertMessages(){
    return mRequestContext.getXDoResults(AlertMessage.class);
  }

  @Override
  public List<DownloadLinkXDoResult> getDownloadLinks(){
    return mRequestContext.getXDoResults(DownloadLinkXDoResult.class);
  }

  @Override
  public List<PopupXDoResult> getPopups(){
    return mRequestContext.getXDoResults(PopupXDoResult.class);
  }

  @Override
  public List<ModelessPopup> getModelessPopups(){
    return mRequestContext.getXDoResults(ModelessPopup.class);
  }

  @Override
  public <T extends XDoResult> List<T> getXDoResultList(Class<T> pForClass){
    return mRequestContext.getXDoResults(pForClass);
  }

  @Override
  public EvaluatedModalPopover getCurrentModalPopoverOrNull() {
    return mOptionalModalPopover;
  }

  public MapSet resolveMapSet(String pMapSetName, DOM pItemDOM, String pMapSetAttachXPath) {
    return mRequestContext.resolveMapSet(pMapSetName, pItemDOM, pMapSetAttachXPath);
  }

  public MapSet resolveMapSet(String pMapSetName, DOM pItemDOM, DOM pMapSetAttachDOM) {
    return mRequestContext.resolveMapSet(pMapSetName, pItemDOM, pMapSetAttachDOM);
  }

  @Override
  public String getImageURI(String pImageURI) {
    return mURIBuilderInstance.buildImageURI(pImageURI);
  }

  @Override
  public Mod getModule() {
    return mModule;
  }

  @Override
  public boolean isAccessibilityMode(){
    // TODO - PN - was mEvalParseTree.getThread().mCurrentOwnerSession.mAccessibilityMode
    return false;
  }

  @Override
  public String getRequestLogId() {
    return mRequestContext.getFoxRequest().getRequestLogId();
  }

  @Override
  public ThreadInfoProvider getThreadInfoProvider(){
    return mThreadInfoProvider;
  }

  @Override
  public ContextUElem getContextUElem() {
    return mRequestContext.getContextUElem();
  }

  public <T extends ModuleFacetProvider> T getModuleFacetProvider(Class<T> pProviderClass) {
    return mRequestContext.getModuleFacetProvider(pProviderClass);
  }

  public void addBufferRegion(EvaluatedBufferPresentationNode pBuffer) {
    mBufferRegions.add(pBuffer);
  }

  @Override
  public List<EvaluatedBufferPresentationNode> getBufferRegions() {
    Collections.sort(mBufferRegions, BUFFER_REGION_COMPARATOR);
    return mBufferRegions;
  }

  public void addImplicatedWidget(WidgetBuilderType pWidget, EvaluatedNode pEvaluatedNode) {
    mImplicatedWidgetsToEvaluatedNodeInfos.put(pWidget, pEvaluatedNode);
  }

  @Override
  public Set<WidgetBuilderType> getImplicatedWidgets() {
    return mImplicatedWidgetsToEvaluatedNodeInfos.keySet();
  }

  @Override
  public Collection<? extends EvaluatedNode> getEvaluatedNodesByWidgetBuilderType(WidgetBuilderType pWidgetBuilderType) {
    return mImplicatedWidgetsToEvaluatedNodeInfos.get(pWidgetBuilderType);
  }

  @Override
  public void addConditionalLoadJavascript(String pJS) {
    mConditionalLoadJavascript.add(pJS);
  }

  @Override
  public void addUnconditionalLoadJavascript(String pJS) {
    mUnconditionalLoadJavascript.add(pJS);
  }

  @Override
  public List<String> getUnconditionalLoadJavascript() {
    return mUnconditionalLoadJavascript;
  }

  @Override
  public List<String> getConditionalLoadJavascript() {
    return mConditionalLoadJavascript;
  }

  public void setDefaultAction(EvaluatedNode pSubmitAction) {
    this.mDefaultAction = pSubmitAction;
  }

  public EvaluatedNode getDefaultAction() {
    return mDefaultAction;
  }

  @Override
  public App getApp() {
    return mApp;
  }

  @Override
  public DevToolbarContext getDevToolbarContext() {
    return mRequestContext.getDevToolbarContext();
  }

  public DownloadManager getDownloadManager() {
    return mRequestContext.getDownloadManager();
  }

  @Override
  public TempResource<?> createTempResource(TempResourceGenerator pGenerator) {
    return mRequestContext.getTempResourceProvider().createTempResource(mRequestContext, RandomString.getString(30), pGenerator, true);
  }

  @Override
  public String getStaticResourceURI(String pResourcePath) {
    return mURIBuilderInstance.buildStaticResourceURI(pResourcePath);
  }

  @Override
  public String getContextResourceURI(String pResourcePath) {
    return mURIBuilderInstance.buildContextResourceURI(pResourcePath);
  }

  @Override
  public String getStaticResourceOrFixedURI(String pResourcePathOrFixedURI) {
    return mURIBuilderInstance.buildStaticResourceOrFixedURI(pResourcePathOrFixedURI);
  }

  @Override
  public String getTempResourceURI(TempResource<?> pTempResource, String pReadableName) {
    return mURIBuilderInstance.buildTempResourceURI(pTempResource, pReadableName);
  }

  @Override
  public RequestURIBuilder createURIBuilder() {
    return RequestURIBuilderImpl.createFromRequestContext(mRequestContext, true);
  }

  @Override
  public InternalAuthLevel getInternalAuthLevel() {
    return InternalAuthentication.instance().getSessionAuthLevel(mRequestContext.getFoxRequest());
  }

  /**
   * Consumers should avoid using this method. Instead they should call the "direct" methods on this class to access
   * RequestContext methods. This method should only be used to proxy a RequestContext from the serialiser to another
   * code area, and should not be directly interacted with in the serialiser. By following this pattern, serialisers
   * run less risk of causing unwanted side effects.
   * @return
   */
  public ActionRequestContext getRequestContext() {
    return mRequestContext;
  }

  private void handleClientVisibilityRules() {
    //Construct a JSON array to contain the top-level list of evaluated rule JSON objects
    JSONArray lMainObj = new JSONArray();

    for(EvaluatedClientVisibilityRule lEvalCVR : mEvaluatedClientVisibilityRules) {
      lEvalCVR.completeEvaluation(mFieldSet);

      //For each evaluated rule, generate the JSON property object and add it to the array
      lMainObj.add(lEvalCVR.getJSON(mFieldSet));
    }

    if(mEvaluatedClientVisibilityRules.size() > 0) {
      addUnconditionalLoadJavascript("registerClientVisibility(" + lMainObj.toJSONString() + "); setupClientVisibility();");
    }
  }

  /**
   * Evaluates a CVR for an include buffer node.
   * @param pRuleName
   * @param pExternalFoxId
   * @param pRelativeDOM
   * @return
   */
  public EvaluatedClientVisibilityRule evaluateClientVisibilityRule(String pRuleName, String pExternalFoxId, DOM pRelativeDOM) {
    try {
      ClientVisibilityRule lCVR = mModule.getClientVisibilityRuleByName(pRuleName);
      EvaluatedClientVisibilityRule lEvalClientVisibilityRule = lCVR.evaluate(pExternalFoxId, pRelativeDOM, getContextUElem());
      mEvaluatedClientVisibilityRules.add(lEvalClientVisibilityRule);
      return lEvalClientVisibilityRule;
    }
    catch (ExModule e) {
      throw new ExInternal("Failed to locate client visibility rule: " + pRuleName, e);
    }
  }

  /**
   * Evaluates a CVR for a target widget.
   * @param pEvalNodeInfo
   * @param pNodeEvaluationContext
   * @return
   */
  public EvaluatedClientVisibilityRule evaluateClientVisibilityRuleOrNull(EvaluatedNodeInfo pEvalNodeInfo, NodeEvaluationContext pNodeEvaluationContext) {

    String lCVRName = pEvalNodeInfo.getStringAttribute(NodeAttribute.CLIENT_VISIBILITY_RULE);

    if(lCVRName != null) {
      try {
        ClientVisibilityRule lCVR = mModule.getClientVisibilityRuleByName(lCVRName);
        EvaluatedClientVisibilityRule lEvalClientVisibilityRule = lCVR.evaluate(pEvalNodeInfo, pNodeEvaluationContext.getDataItem(),
                                                                                pNodeEvaluationContext.getEvaluateContextRuleItem(), getContextUElem());
        mEvaluatedClientVisibilityRules.add(lEvalClientVisibilityRule);
        return lEvalClientVisibilityRule;
      }
      catch (ExModule e) {
        throw new ExInternal("Bad client visibility rule defined on: " + pEvalNodeInfo.getIdentityInformation(), e);
      }
    }
    else {
      return null;
    }
  }

  /**
   * Given a starting EvaluatedPresentationNode look up through the chain of parents for a matching pair of nodes which
   * indicate where the loop was and generate a chain of their toString()'s to show to the user as a runtime exception
   *
   * @param pParent Node where the recursion limit was hit
   * @return
   */
  private ExInternal generateRecursionCycleException(EvaluatedPresentationNode<? extends PresentationNode> pParent) {
    List<String> lCallChain = new ArrayList<>();
    EvaluatedPresentationNode lParent = pParent;
    String lCycleRoot = "";

    // Loop up parent nodes until a repeated buffer include is found - buffer includes are the only nodes which can cause recursion
    for (int i = 0; i < 100; i++) {
      if (lCallChain.contains(lParent.toString()) && lParent instanceof EvaluatedBufferPresentationNode) {
        // We've found the cycling node, store a reference to it and break out of loop
        lCallChain.add(lParent.toString());
        lCycleRoot = lParent.toString();
        break;
      }

      //Repeat with next parent
      lCallChain.add(lParent.toString());

      if (lParent.getParentNode() == null) {
        // If we have no parent node, finish looking
        break;
      }
      else {
        lParent = lParent.getParentNode();
      }
    }

    // Trim the start of the call list so only the cycle is displayed
    Iterator<String> lIterator = lCallChain.iterator();
    while (lIterator.hasNext()) {
      String lNodeDesc = lIterator.next();
      if(lCycleRoot.equals(lNodeDesc)) {
        break;
      }
      else {
        lIterator.remove();
      }
    }

    // Reverse so the call chain is more readable (top of call chain should be top of list)
    Collections.reverse(lCallChain);
    return new ExInternal("Too much recursion in evaluated parse tree - attempt to locate cycle printed below:\n" + Joiner.on("\n").join(lCallChain));
  }

  /**
   * Print the ASCII representation of a Evaluated Parse Tree
   * <pre>
   * e.g.
   * - RootNode
   * -- ChildNode1
   * --- LeafNode
   * -- ChildNode2
   * --- LeafNode1
   * --- LeafNode2
   * </pre>
   *
   * @param pNode Node to start from
   * @return String containing the output
   */
  public static String printDebug(EvaluatedPresentationNode<? extends PresentationNode> pNode) {
    return printDebug(pNode, "");
  }

  public static String printDebug(EvaluatedPresentationNode<? extends PresentationNode> pNode, String pIndent) {
    StringBuilder lDisplayOut = new StringBuilder();
    lDisplayOut.append(pIndent);
    lDisplayOut.append(pNode.toString());
    lDisplayOut.append("\r\n");
    if (pNode.getChildren().size() > 0) {
      for (EvaluatedPresentationNode<? extends PresentationNode> lNestedNode : pNode.getChildren()) {
        lDisplayOut.append(printDebug(lNestedNode, pIndent + "--"));
      }
    }
    return lDisplayOut.toString();
  }

  public Optional<AuthenticatedUser> getAuthenticatedUser() {
    return Optional.ofNullable(mRequestContext.getAuthenticationContext().getAuthenticatedUser());
  }

  /**
   * Checks if the given facet category and identifier tuple has already been recorded by this ParseTree, and records it
   * if not. This can be used to ensure that a ParseTree only renders a certain facet once.
   * @param pFacetCategory Category of facet to check uniqueness for.
   * @param pIdentifier Unique identifier for the facet.
   * @return True if the facet has not been recorded yet (i.e. true if it is unique), false if it already been recorded.
   */
  public boolean validateFacetUniqueness(String pFacetCategory, String pIdentifier) {
    String lKey = pFacetCategory + "/" + pIdentifier;
    if(mUniqueFacetKeys.contains(lKey)) {
      return false;
    }
    else {
      mUniqueFacetKeys.add(lKey);
      return true;
    }
  }


  public List<EvaluatedDataDefinition> getEvaluatedDataDefinitions() {
    return mEvaluatedDataDefinitions;
  }

  /**
   * When going to evaluate sub-buffers push a label target element so that fm:label elements in those sub-buffers can
   * target the calling nodes
   *
   * @param pCurrentTargetElement Data item for the current element which an fm:label should target
   */
  public void pushCurrentBufferLabelTargetElement(DOM pCurrentTargetElement) {
    mCurrentBufferContextNode.push(pCurrentTargetElement);
  }

  /**
   * Pop off the current label target element once evaluation of sub-buffer is done
   *
   * @param pCurrentTargetElement Data item for the current element which an fm:label should no longer be targeted
   */
  public void popCurrentBufferLabelTargetElement(DOM pCurrentTargetElement) {
    DOM lPoppedTargetElement = mCurrentBufferContextNode.pop();
    if (lPoppedTargetElement != pCurrentTargetElement) {
      throw new ExInternal("Popped Target Element from the current buffer label target element stack on evaluated parse tree but it did not match the expected value to be popped: Expected " + pCurrentTargetElement.absolute() + "\r\nPopped: " + lPoppedTargetElement.absolute());
    }
  }

  @Override
  public int getClientVisibilityRuleCount() {
    return mEvaluatedClientVisibilityRules.size();
  }

  @Override
  public EvaluatedBufferPresentationNode evaluateBuffer(String pBufferName, DOM pBufferAttach) {

    BufferPresentationNode lBuffer;
    try {
      lBuffer = getBuffer(pBufferName);
    }
    catch (ExModule e) {
      throw new ExInternal("Failed to resolve buffer " + pBufferName, e);
    }

    return (EvaluatedBufferPresentationNode) evaluateNode(null, lBuffer, pBufferAttach);
  }

  /**
   * Get the current target element for labels to use if one was set
   *
   * @return DOM node for current data item or null if none pushed
   */
  public DOM getCurrentBufferLabelTargetElement() {
    return mCurrentBufferContextNode.peek();
  }
}

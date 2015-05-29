//TODO PN move package
package net.foxopen.fox;

import net.foxopen.fox.dom.ActuateReadOnly;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.DocControl;
import net.foxopen.fox.dom.handler.AbortableDOMHandler;
import net.foxopen.fox.dom.handler.DOMHandler;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.dom.xpath.FoxXPathResultType;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.plugin.api.dom.FxpContextUElem;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * A ContextUElem maintains a mapping of context labels to element nodes in one or more XML documents.
 * It is the main entry point for XPath processing. Any XPath which contains :{label} references needs to be executed
 * using a ContextUElem, and all XPaths executed from a FOX module should use it to establish the correct context node
 * of a path (i.e. the attach point). <br/><br/>
 *
 * To use a ContextUElem you must associate one or more {@link DOMHandler}s with it. These can be opened and closed
 * using convenience methods on the ContextUElem. Each DOMHandler is associated with both a DOM document and a document label.
 * DOMs are only "loaded" from DOMHandlers when requested - if a handler is associated with a ContextUElem but is never
 * requested, it remains "unloaded". Note that this is completely independent of whether the DOMHandler itself has loaded
 * the document. <br/><br/>
 *
 * Labels on nodes within documents which are not documents themselves (i.e. non-root labels) are called <i>contextual labels</i> -
 * these can be serialised using {@link ContextUElem#getSerialisedContextualLabels()}. Consumers should handle the serialisation of document
 * labels themselves (i.e. via their DOMHandlers). Note that the deserialisation process relies on the contextual label's
 * containing document having been loaded by the ContextUElem. It is possible to define contextual labels on unloaded documents,
 * but this is not recommended if the ContextUElem needs to be serialised. <br/><br/>
 *
 * There are two main forms of label mapping. Internal labels, enumerated in {@link ContextLabel}, are set and used by
 * the FOX engine to provide access to different documents and set context labels at relevant points during action and
 * setout processing. Additionally, module developers may set custom labels using commands such as context-set and for-each.
 * If a custom context is being defined on a ContextUElem, you should call {@link #localise}before setting the label and
 * executing XPaths. {@link #delocalise}should then be called to return the ContextUElem to its original state (typically
 * in a finally block).<br/><br/>
 */
public class ContextUElem implements FxpContextUElem<DOM, DOMList> {

  /**
   * Shortcut for getting the "attach" label name.
   */
  public static final String ATTACH = ContextLabel.ATTACH.asString();

  /**
   * Internal class for managing the mapping between a context label and its associated DOM element. Also maintains
   * some stateful information to allow just in time loading/reloading and serialisation logic.
   */
  private class LabelEntry {
    /**
     * The context label.
     */
    final String mLabelName;
    /**
     * DOM reference of the associated element.
     */
    final String mContextRef;
    /**
     * ContextualityLevel of this mapping.
     */
    final ContextualityLevel mContextualityLevel;

    /**
     * The actual DOM object resolved by this label. Can be null if the associated document is stale or if the referenced
     * element has been removed from the DOM tree.
     */
    DOM mCachedDOM;
    /**
     * Document label of this element's containing document. Can be null if the containing document has not been loaded yet.
     */
    String mDocumentLabelName;

    private LabelEntry(String pLabelName, DOM pDOM, ContextualityLevel pContextualityLevel) {
      mLabelName = pLabelName;
      mCachedDOM = pDOM;
      mContextRef = pDOM.getRef();
      mDocumentLabelName = mRefToDocumentLabelMap.get(pDOM.getRootElement().getRef());
      if (mDocumentLabelName == null && !isLocalised()) {
        //Warn that a label has been defined before its document has been loaded - but if we are localised, it shouldn't matter as the label will not be serialised
        Track.alert("LabelEntry.init", "Setting label :{" + pLabelName + "} but root element '" + pDOM.getRootElement().getName() + "' is not recognised");
      }
      mContextualityLevel = pContextualityLevel;
    }

    void clearCachedDOM() {
      mCachedDOM = null;
    }

    //may be null if the label's document is not known to this contextuelem (may not have been loaded yet)
    String getDocumentLabelName() {
      if (mDocumentLabelName == null && mCachedDOM != null) {
        mDocumentLabelName = mRefToDocumentLabelMap.get(mCachedDOM.getRootElement().getRef());
      }
      return mDocumentLabelName;
    }

    DOM getDOM() {
      if (mCachedDOM == null) {
        //Attempt to resolve a DOM node if we don't already have a reference to it
        if (mDocumentLabelName == null) {
          throw new IllegalStateException("Cannot retrieve uncached DOM for label :{" + mLabelName + "} without knowing its document label.");
        }
        Track.debug("LabelEntry.getDOM", "Cache miss for :{" + mLabelName + "} ; remapping");

        try {
          mCachedDOM = getUElem(mDocumentLabelName).getElemByRef(mContextRef); //TODO this could be getByRefOrNull without the catch if happy that not finding anything here is ok
        }
        catch (ExInternal e) {
          throw new ExInternal("Cannot locate DOM for label :{" + mLabelName + "} in document " + mDocumentLabelName, e);
        }
      }
      return mCachedDOM;
    }

    /**
     * Gets the serialised form of this LabelEntry.
     *
     * @return
     */
    SerialisedLabel getSerialisedLabel() {
      String lDocumentLabelName = getDocumentLabelName();
      if (lDocumentLabelName == null) {
        throw new ExInternal("Cannot serialise LabelEntry for label :{" + mLabelName + "} - its owner document was not specified and has not been loaded by this ContextUElem.");
      }

      return new SerialisedLabel(mLabelName, lDocumentLabelName, mContextRef, mContextualityLevel);
    }
  }

  /**
   * Data class containing the data required to serialise a context label.
   */
  public static class SerialisedLabel {
    private final String mLabelName;
    private final String mDocumentLabelName;
    private final String mContextRef;
    private final ContextualityLevel mContextualityLevel;

    SerialisedLabel(String pLabelName, String pDocumentLabelName, String pContextRef, ContextualityLevel pContextualityLevel) {
      mLabelName = pLabelName;
      mDocumentLabelName = pDocumentLabelName;
      mContextRef = pContextRef;
      mContextualityLevel = pContextualityLevel;
    }

    void applyToContextUElem(ContextUElem pContextUElem) {
      //Retrieve the document this label was in - note getUElem may invoke a DOM load if not in cache, depending on the DOMHandler
      DOM lDocumentDOM = pContextUElem.getUElem(mDocumentLabelName);

      //Find the node in the document this label points to
      DOM lLabelDOM = lDocumentDOM.getElemByRefOrNull(mContextRef);

      if (lLabelDOM == null) {
        //Erroring here is too harsh, for instance if a churn has removed an element which was referenced by a context
        Track.alert("DeserialiseLabel", "Error deserialising label :{" + mLabelName + "} - element with ref '" + mContextRef + "' could not be found in :{" + mDocumentLabelName + "}");
      }
      else if (!lLabelDOM.isAttached()) {
        throw new ExInternal("Node found for label :{" + mLabelName + "} is not attached to a document.");
      }
      else {
        Track.debug("ContextUElemDeserialise", "Deserialised label :{" + mLabelName + "} for ref '" + mContextRef + "'");
        //Only create a label entry if we found a corresponding DOM - otherwise we'll have problems later when getUElem returns null
        //Shouldn't need to be global as this operation should only be happening to a non-localised ContextUElem
        pContextUElem.putLabelEntry(mLabelName, lLabelDOM, mContextualityLevel, false);
      }
    }
  }

  /**
   * Map of document labels to their respective DOMHandlers.
   */
  private final Map<String, DOMHandler> mLabelToDOMHandler = new HashMap<>();

  /**
   * Set of DOMHandlers which are currently fully open by this ContextUElem. Used to decide which handlers are appropriate
   * to abort (especially in case an error happens during the open procedure).
   */
  private final Set<DOMHandler> mOpenDOMHandlers = new HashSet<>();

  /**
   * Map of FOXIDs of document root elements to their respective document labels.
   */
  private final Map<String, String> mRefToDocumentLabelMap = new HashMap<>();

  /**
   * Stack of loaded labels mapped to their respective DOMs and ContextualityLevels. A label is only loaded after it
   * is requested or points to an child element in a document which is loaded. The stack is required to support
   * localise/delocalise behaviour.
   */
  private final Deque<Map<String, LabelEntry>> mLabelEntryStack = new ArrayDeque<>();

  /**
   * Stack for capturing the current localisation purpose for this ContextUElem. This information is reported to developers
   * in the event of an error.
   */
  private final Deque<String> mLocalisedPurposeStack = new ArrayDeque<>();

  /**
   * Initialises the label entry stack.
   */
  private void setupLabelEntryStack() {
    mLabelEntryStack.addFirst(new HashMap<String, LabelEntry>());
  }

  /**
   * Initialises the localised purpose stack.
   */
  private void setupPurposeStack() {
    mLocalisedPurposeStack.addFirst("NOT-LOCALISED");
  }

  /**
   * Constructs an empty ContextUElem.
   */
  public ContextUElem() {
    //Create an entry stack with a single default empty entry
    setupLabelEntryStack();
    setupPurposeStack();
  }

  /**
   * Constructs a ContextUElem with initial ATTACH and ACTION contexts.
   *
   * @param pAttach               The initial ATTACH and ACTION DOM.
   * @param pDocumentContextLabel The ContextLabel of the document which the attach node exists on. This is information
   *                              is required by label serialisation and should be supplied in case the attach document itself is not loaded by this
   *                              ContextUElem.
   */
  public ContextUElem(DOM pAttach, ContextLabel pDocumentContextLabel) {
    this();
    defineUElem(ContextLabel.ATTACH, pAttach, pDocumentContextLabel);
    defineUElem(ContextLabel.ACTION, pAttach, pDocumentContextLabel);
  }

  /**
   * Constructs a ContextUElem with initial ATTACH and ACTION contexts.
   *
   * @param pAttach The initial ATTACH and ACTION DOM.
   * @deprecated Need to specify a document context label for serialisation purposes. This constructor is only safe if
   * this ContextUElem is not serialised.
   */
  @Deprecated
  public ContextUElem(DOM pAttach) {
    this();
    defineUElem(ContextLabel.ATTACH, pAttach);
    defineUElem(ContextLabel.ACTION, pAttach);
  }

  /**
   * Clears all mappings from this ContextUElem.
   */
  public void clearAll() {
    mLabelEntryStack.clear();
    setupLabelEntryStack();
    mLocalisedPurposeStack.clear();
    setupPurposeStack();
    mLabelToDOMHandler.clear();
    mRefToDocumentLabelMap.clear();
  }

  /**
   * Clears all contextual labels from this ContextUElem (i.e. non-document labels).
   */
  public void clearContextualLabels() {
    for (ContextUElem.LabelEntry lLabel : getCurrentLabelEntriesCopy()) {
      if (lLabel.mContextualityLevel != ContextualityLevel.DOCUMENT) {
        removeUElem(lLabel.mLabelName, false, false);
      }
    }
  }

  /**
   * Deserialises a set of serialised contextual labels from the given collection back into this ContextUElem.
   */
  public void deserialiseContextualLabels(Collection<SerialisedLabel> pSerialisedLabels) {
    for (SerialisedLabel lSerialisedLabel : pSerialisedLabels) {
      lSerialisedLabel.applyToContextUElem(this);
    }
  }

  /**
   * Gets the current set of contextual labels (i.e. non-document labels) as SerialisedLabels, for external serialisation/storage
   * The label's containing document must have been loaded into this ContextUElem for the serialisation process to work.
   */
  public Collection<SerialisedLabel> getSerialisedContextualLabels() {
    Collection<SerialisedLabel> lLabelSet = new HashSet<>();

    for (LabelEntry lEntry : getCurrentLabelEntriesCopy()) {
      if (lEntry.mContextualityLevel != ContextualityLevel.DOCUMENT) {
        lLabelSet.add(lEntry.getSerialisedLabel());
      }
    }
    return lLabelSet;
  }

  private LabelEntry putLabelEntry(String pLabel, DOM pDOM, ContextualityLevel pContextualityLevel, boolean pGlobal) {
    LabelEntry lNewEntry = new LabelEntry(pLabel, pDOM, pContextualityLevel);
    if(pGlobal) {
      //If setting globally, add the entry to every level of the stack
      for(Map<String, LabelEntry> lStackElement : mLabelEntryStack) {
        lStackElement.put(pLabel, lNewEntry);
      }
    }
    else {
      //Localised set - just add to current level
      mLabelEntryStack.getFirst().put(pLabel, lNewEntry);
    }
    return lNewEntry;
  }

  private void clearLabelEntry(String pLabel, boolean pGlobal) {
    if(pGlobal) {
      //If clearing globally, remove the entry from every level of the stack
      for(Map<String, LabelEntry> lStackElement : mLabelEntryStack) {
        lStackElement.remove(pLabel);
      }
    }
    else {
      //Localised clear - just remove from current level
      mLabelEntryStack.getFirst().remove(pLabel);
    }
  }

  private DOM getDOMForLabel(String pLabel) {
    LabelEntry lEntry = mLabelEntryStack.getFirst().get(pLabel);
    if (lEntry != null) {
      return lEntry.getDOM();
    }
    else {
      return null;
    }
  }

  /**
   * Tests if the given document label has been loaded from its DOMHandler by this ContextUElem.
   *
   * @param pLabel
   * @return
   */
  public boolean isLabelLoaded(String pLabel) {
    return mLabelEntryStack.getFirst().containsKey(pLabel);
  }

  private Collection<LabelEntry> getCurrentLabelEntries() {
    return mLabelEntryStack.getFirst().values();
  }

  /**
   * Gets a copy of the current LabelEntry set - the collection is volatile because it can be modified by common
   * operations such as getUElem. This avoids ConcurrentModificationExceptions when iterating.
   *
   * @return
   */
  private Collection<LabelEntry> getCurrentLabelEntriesCopy() {
    return new HashSet<>(mLabelEntryStack.getFirst().values());
  }

  private ContextualityLevel getStackContextualityLevelForLabel(String pLabel) {
    LabelEntry lEntry = mLabelEntryStack.getFirst().get(pLabel);
    if (lEntry != null) {
      return lEntry.mContextualityLevel;
    }
    else {
      return null;
    }
  }

  /**
   * Get the DOM node referenced by the given context label. If pLabel is null, the ATTACH label is retrieved by default.
   *
   * @param pLabel Labelled element to get.
   * @return The referenced element, or null.
   */
  public final DOM getUElemOrNull(String pLabel) {
    if (pLabel == null) {
      pLabel = ContextLabel.ATTACH.asString();
    }

    DOM lDOM = getDOMForLabel(pLabel);

    //Look up in handler (might need opening)
    if (lDOM == null) {
      DOMHandler lDOMHandler = mLabelToDOMHandler.get(pLabel);
      if (lDOMHandler != null) {
        lDOM = lDOMHandler.getDOM();
        Track.debug("AssignDOMToLabel", "DOM for :{" + pLabel + "} was retrieved from handler as no existing mapping was found");

        //Sanity check map label matches expected label
        if (!pLabel.equals(lDOMHandler.getContextLabel())) {
          throw new ExInternal("Label mismatch: label requested was :{" + pLabel + "} but DOMHandler's label is :{" + lDOMHandler.getContextLabel() + "}");
        }

        //Make a note of this document's root foxid for reverse lookup map
        mRefToDocumentLabelMap.put(lDOM.getRef(), pLabel);
        //Shouldn't need to be global as this operation should only be happening to a non-localised ContextUElem
        putLabelEntry(pLabel, lDOM, ContextualityLevel.DOCUMENT, false);
      }
    }

    //Check the labelled DOM has not been removed from its DOM tree
    if(lDOM != null && !lDOM.isAttached()) {
      //This should probably throw/invalidate label mapping - warn for now so we can assess impact - FOXRD-661
      Track.alert("UnattachedContextLabel", "Context label " + pLabel + " points to an unattached node - you must remap the label to an attached DOM node " +
        "(this message will become an error in a future release)", TrackFlag.CRITICAL);
    }

    return lDOM;
  }

  //PN TODO - is this method appropriate/necessary?
  public <T extends DOMHandler> List<T> getDOMHandlersByType(Class<T> pDOMHandlerType) {
    List<T> lDOMHandlers = new ArrayList<>();
    for (DOMHandler lHandler : getDOMHandlers()) {
      if (pDOMHandlerType.isInstance(lHandler)) {
        lDOMHandlers.add(pDOMHandlerType.cast(lHandler));
      }
    }

    return lDOMHandlers;
  }

  private Collection<DOMHandler> getDOMHandlers() {
    return mLabelToDOMHandler.values();
  }

  /**
   * Gets a set of all the label names which correspond to document labels held by this ContextUElem.
   *
   * @return Set of document labels.
   */
  public Collection<String> getDocumentLabels() {
    return new TreeSet<>(mLabelToDOMHandler.keySet());
  }

  /**
   * Gets a set of all the label names which correspond to contextual labels (i.e. non-document labels) held by this ContextUElem.
   *
   * @return Set of contextual (state, localised, etc) labels.
   */
  public Collection<String> getContextualLabels() {
    Set<String> lLabelStrings = new TreeSet<>();
    for (LabelEntry lEntry : getCurrentLabelEntries()) {
      if (lEntry.mContextualityLevel.asInt() > ContextualityLevel.DOCUMENT.asInt()) {
        lLabelStrings.add(lEntry.mLabelName);
      }
    }

    return lLabelStrings;
  }

  private List<DOMHandler> getDOMHandlersInLoadPrecedenceOrder() {

    List<DOMHandler> lSortedList = new LinkedList<>(mLabelToDOMHandler.values());

    //Sort the DOM handlers with the highest precedence first
    Collections.sort(lSortedList, new Comparator<DOMHandler>() {
      public int compare(DOMHandler pO1, DOMHandler pO2) {
        return (pO1.getLoadPrecedence() - pO2.getLoadPrecedence()) * -1;
      }
    });

    return lSortedList;
  }

  /**
   * Gets the DOMHandler corresponding to the given label, or null if no mapping exists.
   *
   * @param pLabel Label to get DOMHandler for.
   * @return The label's DOMHandler.
   */
  public DOMHandler getDOMHandlerForLabel(String pLabel) {
    return mLabelToDOMHandler.get(pLabel);
  }

  /**
   * Get the DOM node referenced by the given context label. If null, retrieves the "attach" label by default.
   *
   * @param pLabel Context label (optional).
   * @return DOM resolved by pLabel.
   */
  public final DOM getUElem(ContextLabel pLabel) {
    if (pLabel == null) {
      pLabel = ContextLabel.ATTACH;
    }
    return getUElem(pLabel.asString());
  }

  /**
   * Get the DOM node referenced by the given context label. If pLabel is null, retrieves the "attach" label by default.
   *
   * @param pLabel Context label (optional).
   * @return DOM resolved by pLabel.
   * @throws ExInternal If the context label is not defined.
   */
  public final DOM getUElem(String pLabel)
  throws ExInternal {

    DOM lDOM = getUElemOrNull(pLabel != null ? pLabel : ContextUElem.ATTACH);

    if (lDOM == null) {
      throw new ExInternal("Context label not defined: :{" + (pLabel != null ? pLabel : ContextUElem.ATTACH) + "}");
    }
    return lDOM;
  }

  /**
   * Forces the document resolved by or containing pLabel to be loaded into this ContextUElem.
   *
   * @param pLabel Label to load document for.
   */
  public final void loadUElem(String pLabel) {
    getUElem(pLabel);
  }

  /**
   * Tests if the given label is allowed to be cleared from a ContextUElem.
   *
   * @param pLabel The label to test.
   * @return True if this label can be cleared, false otherwise.
   */
  private static boolean isLabelClearable(String pLabel) {
    ContextLabel lContextLabel = ContextLabel.getContextLabel(pLabel);
    if (lContextLabel != null) {
      return lContextLabel.isClearable();
    }
    else {
      return true;
    }
  }

  /**
   * Removes the label mapping for the given label from this ContextUElem. The DOM itself is not affected.
   *
   * @param pLabel         The label to remove.
   * @param pTestClearable If true, a check is performed to see if the label is allowed to be cleared.
   * @param pGlobal        If true, label will be cleared from all levels of the localisation stack. If false it will only
   *                       be cleared from the top of the stack.
   * @throws ExInternal If the label is not clearable and pTestClearable is true.
   */
  private void removeUElem(String pLabel, boolean pTestClearable, boolean pGlobal)
  throws ExInternal {

    if (XFUtil.isNull(pLabel)) {
      throw new ExInternal("pLabel cannot be null or empty");
    }

    if (!pTestClearable || isLabelClearable(pLabel)) {
      clearLabelEntry(pLabel, pGlobal);
    }
    else {
      throw new ExInternal("The context label '" + pLabel + "' is not allowed to be cleared.");
    }
  }

  /**
   * Removes the label mapping for the given label from this ContextUElem. The DOM itself is not affected.
   *
   * @param pLabel The label to remove.
   * @throws ExInternal If the label is not clearable.
   */
  public final void removeUElem(String pLabel) {
    removeUElem(pLabel, true, false);
  }

  /**
   * Removes the label mapping for the given label from every localisation level of this ContextUElem. The DOM itself is not affected.
   *
   * @param pLabel The label to remove.
   * @throws ExInternal If the label is not clearable.
   */
  public final void removeUElemGlobal(String pLabel) {
    removeUElem(pLabel, true, true);
  }

  /**
   * Gets the ATTACH DOM of this ContextUElem.
   *
   * @return The ATTACH DOM.
   */
  public final DOM attachDOM() {
    return getUElem(ContextLabel.ATTACH);
  }

  /**
   * Internal method for setting up a label to DOM mapping.<br/><br/>
   * Called in one of two ways:
   * <ol>
   * <li>With pContextLabel specified if the FOX engine is setting up a label internally.</li>
   * <li>With pStringLabel and pContextualityLevel specified if a user command or external command is setting a label
   * programatically.</li>
   * </ol>
   * @param pContextLabel         The ContextLabel being set (for internal FOX engine use).
   * @param pStringLabel          The String of the label (for external programmatic use).
   * @param pContextualityLevel   The ContextualityLevel of the label. This should only be specified in conjunction with
   *                              pStringLabel. It is used to determine the contextuality of XPaths executed against this ContextUElem. For more info
   *                              see {@link ContextualityLevel}.
   * @param pUElem                The DOM node which the label will point to. The node must be attached to a document.
   * @param pForceFoxIdAssign     If true, forcibly assigns a FOXID attribute to pUElem (if it is an Element).
   * @param pCheckSetability      If true, the method will check to see if the requested label is setable. This is useful
   *                              for validating external input, e.g. to prevent a user from resetting the "root" label.
   * @param pDocumentContextLabel Context label of the new label's owning document, or null if not known.
   * @param pGlobal               If true, the label will apply to every level of the localisation stack. If false it will
   *                              only apply to the top level of the stack.
   */
  private void setUElemInternal(ContextLabel pContextLabel, String pStringLabel, ContextualityLevel pContextualityLevel, DOM pUElem,
                                boolean pForceFoxIdAssign, boolean pCheckSetability, String pDocumentContextLabel, boolean pGlobal) {

    // Internal validation
    if (pContextLabel != null && pStringLabel != null) {
      throw new ExInternal("pContextLabel and pStringLabel are mutually exclusive");
    }
    else if (pContextLabel == null && pStringLabel == null) {
      throw new ExInternal("Exactly one of pContextLabel or pStringLabel must be provided");
    }
    else if (pStringLabel != null && pContextualityLevel == null) {
      throw new ExInternal("pContextualityLevel must be provided if pStringLabel is used for label :{" + pStringLabel + "}");
    }
    else if (pStringLabel != null && pStringLabel.length() == 0) {
      throw new ExInternal("pStringLabel must not be empty if specified");
    }

    String lLabelString = pContextLabel != null ? pContextLabel.asString() : pStringLabel;

    //Validate parameters
    if (pUElem == null) {
      throw new ExInternal("pUElem cannot be null for label :{" + lLabelString + "}");
    }
    else if (!pUElem.isAttached()) {
      throw new ExInternal("Attempted to assign label :{" + lLabelString + "} to an unattached node" + (pUElem.isElement() ? " with name '" + pUElem.getName() + "'" : ""));
    }
    else if (lLabelString.indexOf(',') != -1) {
      throw new IllegalArgumentException("Label string :{" + lLabelString + "} is invalid; cannot contain ',' character");
    }

    //Establish contextuality level
    //Note this still supports a user overloading a built-in context and setting a different contextuality level (e.g.
    //using :{action} in a for-each) as this method will have been called with a null ContextLabel argument
    ContextualityLevel lContextualityLevel = pContextLabel != null ? pContextLabel.contextualityLevel() : pContextualityLevel;

    if (lContextualityLevel == ContextualityLevel.DOCUMENT) {
      throw new ExInternal("Illegal attempt to define a Document level context using setUElem - consider registerDOMHandler instead");
    }

    //If necessary, check that we are allowed to set a label of this name
    if (pCheckSetability) {
      boolean lSetable = true;
      //Use the ContextLabel directly if we have one, or look one up using the provided string
      if (pContextLabel != null) {
        lSetable = pContextLabel.isSetable();
      }
      else {
        ContextLabel lContextLabel = ContextLabel.getContextLabel(pStringLabel);
        if (lContextLabel != null) {
          lSetable = lContextLabel.isSetable();
        }
      }

      if (!lSetable) {
        throw new ExInternal("Illegal attempt to set context label :{" + lLabelString + "}");
      }
    }

    // Assign context
    LabelEntry lNewLabelEntry = putLabelEntry(lLabelString, pUElem, lContextualityLevel, pGlobal);

    //Set up the document label value on the entry now, if available, in case it could not be determined because no documents are loaded yet
    //I.e. when setting the initial attach point before any DOMs are loaded
    if (pDocumentContextLabel != null && lNewLabelEntry.mDocumentLabelName == null) {
      lNewLabelEntry.mDocumentLabelName = pDocumentContextLabel;
    }

    //Ensure the element has a FOXID if necessary.
    if (pForceFoxIdAssign) {
      // Hash the dom FOXID into the document element id cache
      if (pUElem.isElement() && !pUElem.hasAttr(ActuateReadOnly.FOXID)) {
        Track.debug("ContextUElem.setUElem", "Force assigning foxid for context label: " + lLabelString);
        try {
          pUElem.getRef();
        }
        catch (Throwable ignoreEx) {
        } // Will set the foxid, if not already set
      }
    }
  }

  public final void registerDOMHandler(DOMHandler pDOMHandler) {
    mLabelToDOMHandler.put(pDOMHandler.getContextLabel(), pDOMHandler);
  }

  public void openDOMHandlers(ActionRequestContext pRequestContext) {
    for (DOMHandler lHandler : getDOMHandlers()) {
      Track.pushInfo("OpenDOMHandler", lHandler.getContextLabel());
      try {
        lHandler.open(pRequestContext);

        //Record the successful open
        mOpenDOMHandlers.add(lHandler);

        /*
         * Transient DOMs go stale after they close. For instance, a WorkDoc creates a new DOM each time it is opened.
         * References to stale DOMs should not be retained by a ContextUElem across churns. This check clears stale DOMs
         * out now so they are reloaded from their DOMHandler just-in-time if they are requested during the churn. If a DOM
         * is not marked as transient that means it is safe to retain references to it. For example an Internal DOM is
         * cached against a thread, so as long as the thread is not stale the DOM will not be stale either. This has to happen
         * on open as we may need to still reference DOMs after a close.
         */

        if (lHandler.isTransient()) {
          removeUElem(lHandler.getContextLabel(), false, false);

          //TODO is there a better way to do this loop
          //Clear the cached DOMs from LabelEntries implicated by this document - they are also potentially stale.
          //The new DOM for the label will be resolved just in time when needed.
          for (LabelEntry lEntry : getCurrentLabelEntriesCopy()) {
            if (lHandler.getContextLabel().equals(lEntry.getDocumentLabelName())) {
              lEntry.clearCachedDOM();
            }
          }

        }
      }
      finally {
        Track.pop("OpenDOMHandler");
      }
    }
  }

  public void closeDOMHandlers(ActionRequestContext pRequestContext) {
    for (DOMHandler lHandler : getDOMHandlers()) {
      lHandler.close(pRequestContext);
    }
    mOpenDOMHandlers.clear();
  }

  /**
   * Aborts any abortable DOMHandlers on this ContextUElem. Any exceptions from the abort call are suppressed.
   */
  public void abortDOMHandlers() {
    //IMPORTANT: only abort handlers which this ContextUElem has successfully opened! Otherwise we may interfere with another thread.
    for (DOMHandler lHandler : getDOMHandlers()) {
      if (lHandler instanceof AbortableDOMHandler && mOpenDOMHandlers.contains(lHandler)) {
        try {
          ((AbortableDOMHandler) lHandler).abort();
        }
        catch (Throwable th) {
          Track.recordSuppressedException("Caught during DOMHandler abort for " + lHandler.getContextLabel(), th);
        }
      }
    }
  }

  /**
   * "Localises" this ContextUElem to allow the definition of additional labels in a localised manner without affecting its
   * overall state. After calling this method, defining additional labels and executing XPaths, you should return it to
   * its initial state using {@link #delocalise}. It is possible to localise multiple times if necessary - localisation
   * is treated like a stack.
   *
   * @param pPurpose Purpose of the localisation, used for debugging.
   * @return Self reference.
   */
  public ContextUElem localise(String pPurpose) {
    mLabelEntryStack.addFirst(new HashMap<>(mLabelEntryStack.getFirst()));
    mLocalisedPurposeStack.addFirst(pPurpose);
    return this;
  }

  /**
   * Delocalises this ContextUlem, removing any label mappings which were defined since {@link #localise} was called.
   *
   * @param pPurpose Purpose originally used to localise this ContextUElem. Used to ensure implementation errors do not
   *                 occur. The strings used must match exactly.
   * @return Self reference.
   */
  public ContextUElem delocalise(String pPurpose) {
    if (!pPurpose.equals(mLocalisedPurposeStack.getFirst())) {
      throw new IllegalStateException("Cannot delocalise for purpose '" + pPurpose + "' when top of stack has purpose: " + mLocalisedPurposeStack.getFirst());
    }
    mLabelEntryStack.removeFirst();
    mLocalisedPurposeStack.removeFirst();
    return this;
  }

  public boolean isLocalised() {
    return mLabelEntryStack.size() > 1;
  }

  public String getLocalisedPurpose() {
    return mLocalisedPurposeStack.getFirst();
  }

  /**
   * Gets the context label corresponding to the document which pNode is in.
   *
   * @param pNode Node to examine.
   * @return pNode's document label.
   */
  public String getDocumentLabelForNode(DOM pNode) {
    String lDocLabel = mRefToDocumentLabelMap.get(pNode.getRootElement().getRef());
    if (lDocLabel == null) {
      throw new ExInternal("Cannot determine document label for node: " + pNode.getRef());
    }
    else {
      return lDocLabel;
    }
  }

  /**
   * Defines a label to UElem mapping. This method is designed for internal use when initialising a ContextUElem and
   * does not validate if the requested label is setable.
   *
   * @param pLabel The ContextLabel to set.
   * @param pUElem The DOM node to map to the label.
   */
  public final void defineUElem(ContextLabel pLabel, DOM pUElem) {
    setUElemInternal(pLabel, null, null, pUElem, true, false, null, false);
  }

  /**
   * Defines a label to UElem mapping. This method is designed for internal use when initialising a ContextUElem and
   * does not validate if the requested label is setable. Use this variant when initialising a ContextUElem and the
   * document where the label is defined is known.
   *
   * @param pLabel                The ContextLabel to set.
   * @param pUElem                The DOM node to map to the label.
   * @param pDocumentContextLabel The ContextLabel of the document which the target element exists on.
   */
  public final void defineUElem(ContextLabel pLabel, DOM pUElem, ContextLabel pDocumentContextLabel) {
    setUElemInternal(pLabel, null, null, pUElem, true, false, pDocumentContextLabel.asString(), false);
  }

  /**
   * Sets a label to UElem mapping. If the mapping already exists it is repointed. This method is designed for internal
   * use within the FOX engine.
   * For programmatically setting a label according to user input, use {@link #setUElem(String, ContextualityLevel, DOM)}.
   *
   * @param pLabel The ContextLabel to set.
   * @param pUElem The DOM node to map to the label.
   */
  public final void setUElem(ContextLabel pLabel, DOM pUElem) {
    setUElemInternal(pLabel, null, null, pUElem, true, true, null, false);
  }

  /**
   * Sets a label to UElem mapping. If the mapping already exists it is repointed. This method is designed for internal
   * use within the FOX engine. Use this variant when initialising a ContextUElem and the document where the label is defined
   * is known.
   * For programmatically setting a label according to user input, use {@link #setUElem(String, ContextualityLevel, DOM)}.
   *
   * @param pLabel                The ContextLabel to set.
   * @param pUElem                The DOM node to map to the label.
   * @param pDocumentContextLabel The context label of the document which the target element exists on.
   */
  public final void setUElem(ContextLabel pLabel, DOM pUElem, String pDocumentContextLabel) {
    setUElemInternal(pLabel, null, null, pUElem, true, true, pDocumentContextLabel, false);
  }

  /**
   * Sets a label to UElem mapping. If the mapping already exists it is repointed. The proposed label name is validated
   * to ensure it is allowed to be set.
   *
   * @param pLabel              The label String to set.
   * @param pContextualityLevel The ContextualityLevel of this label definition. See {@link ContextualityLevel}.
   * @param pUElem              The DOM node to map to the label.
   */
  public final void setUElem(String pLabel, ContextualityLevel pContextualityLevel, DOM pUElem) {
    setUElemInternal(null, pLabel, pContextualityLevel, pUElem, true, true, null, false);
  }

  /**
   * Sets a label to UElem mapping across the localisation stack. This effectively ignores any current context localisation.
   * If the mapping already exists it is repointed. The proposed label name is validated to ensure it is allowed to be set.
   *
   * @param pLabel              The label String to set.
   * @param pContextualityLevel The ContextualityLevel of this label definition. See {@link ContextualityLevel}.
   * @param pUElem              The DOM node to map to the label.
   */
  public final void setUElemGlobal(String pLabel, ContextualityLevel pContextualityLevel, DOM pUElem) {
    setUElemInternal(null, pLabel, pContextualityLevel, pUElem, true, true, null, true);
  }

  /**
   * Get the ContextualityLevel for the label specified by pLabel. If a custom label has been defined or the ContextualityLevel
   * of a built-in label has been overridden, the custom value is returned. Otherwise if the label is built-in it is used
   * to look up the default.
   *
   * @param pLabel Label name to get the ContextualityLevel for.
   * @return The label's ContextualityLevel.
   * @throws ExInternal If the ContextualityLevel cannot be determined.
   * @see ContextualityLevel
   */
  public ContextualityLevel getLabelContextualityLevel(String pLabel)
  throws ExInternal {

    if (!existsContext(pLabel)) {
      throw new IllegalArgumentException(pLabel + " is not defined on this ContextUElem");
    }

    ContextualityLevel lContextualityLevel = getStackContextualityLevelForLabel(pLabel);
    if (lContextualityLevel != null) {
      return lContextualityLevel;
    }
    else if (mLabelToDOMHandler.containsKey(pLabel)) {
      //If the DOMHandler is registered but not yet loaded, we know it will be a document when loaded
      return ContextualityLevel.DOCUMENT;
    }
    else {
      throw new ExInternal("Failed to determine label contextuality. " +
                           "Label :{" + pLabel + "} was not found in label to contextuality map.");
    }
  }

  private static String safeTrim(String pStringToTrim) {
    if (pStringToTrim == null) {
      throw new ExInternal("XPath cannot be null");
    }
    else {
      return pStringToTrim.trim();
    }
  }

  /**
   * Executes a FOX XPath which returns a single element.
   *
   * @param pFoxExtendedXpath            The XPath expression to execute.
   * @param pCreateMissingNodesOption    If true, creates elements along the path if they are not found. See
   *                                     {@link DOM#getCreate1E} for more information.
   * @param pDefaultContextLabelOptional Specify a context label to use as the initial context node of the XPath expression.
   *                                     If null, the default ATTACH point is used.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed.
   */
  public DOM extendedXPath1E(String pFoxExtendedXpath, boolean pCreateMissingNodesOption, String pDefaultContextLabelOptional)
  throws ExActionFailed, ExTooMany, ExTooFew {
    return extendedXPath1E(getUElem(pDefaultContextLabelOptional), pFoxExtendedXpath, pCreateMissingNodesOption);
  }

  /**
   * Executes a FOX XPath which returns a single element. The ATTACH node is used as the initial context node.
   *
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed..
   */
  public DOM extendedXPath1E(String pFoxExtendedXpath)
  throws ExActionFailed, ExTooMany, ExTooFew {
    return extendedXPath1E(attachDOM(), pFoxExtendedXpath, false);
  }

  /**
   * Executes a FOX XPath which returns a single element. The ATTACH node is used as the initial context node.
   *
   * @param pFoxExtendedXpath         The XPath expression to execute.
   * @param pCreateMissingNodesOption If true, creates elements along the path if they are not found. See
   *                                  {@link DOM#getCreate1E} for more information.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed.
   */
  public DOM extendedXPath1E(String pFoxExtendedXpath, boolean pCreateMissingNodesOption)
  throws ExActionFailed, ExTooMany, ExTooFew {
    return extendedXPath1E(attachDOM(), pFoxExtendedXpath, pCreateMissingNodesOption);
  }

  /**
   * Reassign the given context label based on the result of an XPath expression. If the context label does not already
   * exist an exception is raised.
   *
   * @param pLabel                    The label to reassign.
   * @param pContextualityLevel       The ContextualityLevel of the reassigned label.
   * @param pFoxExtendedXpath         The XPath which resolves a single element to be used for the new context label.
   * @param pCreateMissingNodesOption If true, creates elements along the path if they are not found. See
   *                                  {@link DOM#getCreate1E} for more information.
   * @return The element resolved by the XPath.
   * @throws ExInternal     If the specified context label is not already defined.
   * @throws ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed.
   */
  public DOM repositionExtendedXPath(
  String pLabel
  , ContextualityLevel pContextualityLevel
  , String pFoxExtendedXpath
  , boolean pCreateMissingNodesOption
  )
  throws ExInternal, ExActionFailed, ExTooMany, ExTooFew {
    // Check existing label
    if (!existsContext(pLabel)) {
      throw new ExInternal("repositionExtendedXPath context label :{" + pLabel + "} not defined");
    }
    DOM target = extendedXPath1E(pFoxExtendedXpath, pCreateMissingNodesOption, pLabel);
    setUElem(pLabel, pContextualityLevel, target);
    return target;
  }

  /**
   * Executes a FOX XPath which returns a single element. An ExTooFew exception is thrown if no matching elements are found.
   *
   * @param pRelativeDOM              The initial context node of the XPath expression.
   * @param pFoxExtendedXpath         The XPath expression to execute.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath cannot be executed or nodes cannot be created.
   * @throws ExTooMany      If too many nodes are matched.
   * @throws ExTooFew       If no nodes are matched and pCreateMissingNodesOption is false.
   */
  public DOM extendedXPath1E(final DOM pRelativeDOM, final String pFoxExtendedXpath)
  throws ExTooMany, ExTooFew, ExActionFailed {
    return extendedXPath1E(pRelativeDOM, pFoxExtendedXpath, false);
  }

  /**
   * Executes a FOX XPath which returns a single element.
   * @param pRelativeDOM The initial context node of the XPath expression
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @param pCreateMissingNodesOption If true, creates elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information.
   * @return The element resolved by the XPath.
   * @throws ExActionFailed If the XPath cannot be executed or nodes cannot be created.
   * @throws ExTooMany If too many nodes are matched.
   * @throws ExTooFew If no nodes are matched and pCreateMissingNodesOption is false.
   */
  public DOM extendedXPath1E(final DOM pRelativeDOM, final String pFoxExtendedXpath, final boolean pCreateMissingNodesOption)
  throws ExActionFailed, ExTooMany, ExTooFew {

    XPathResult lXPathResult = extendedXPathResult(pRelativeDOM, pFoxExtendedXpath);
    DOMList lResultDOMList = lXPathResult.asDOMList();
    int lSize = lResultDOMList.getLength();
    if(lSize == 1) {
      return lResultDOMList.item(0);
    }
    if (lSize > 1) {
      throw new ExTooMany("Too many elements (expected 1, got " + lResultDOMList.getLength() + ") match XPath: " + pFoxExtendedXpath);
    }

    // When missing validation
    if(!pCreateMissingNodesOption) {
      throw new ExTooFew("Too few elements (expected 1, got 0) match XPath: " + pFoxExtendedXpath);
    }

    // When missing validation
    if(lXPathResult.getNumberOfImplicatedDocuments() > 1) {
      throw new ExActionFailed("BADPATH", "Unable to create from XPath when more than one document involved for XPath: " + pFoxExtendedXpath);
    }

    // Create elements using XPath expression
    try {
      return lXPathResult.getRelativeDOM().getCreateXpath1E(pFoxExtendedXpath, this);
    }
    catch(ExBadPath x) {
      throw new ExActionFailed("BADPATH", "Bad XPath encountered while attempting element creation for path: " + pFoxExtendedXpath, x);
    }
    catch(ExTooMany x) {
      throw new ExTooMany("Too many nodes encountered while attempting element creation for path: " + pFoxExtendedXpath, x);
    }

  }

  /**
   * Gets the absolute path to the node resolved by the given XPath. The node may not exist, in which case the same rules
   * for path-based node creation are followed to generate a hypothetical path. See {@link DOM#getAbsolutePathForCreateableXPath}
   * for more information.
   * @param pRelativeDOM The initial context node of the XPath expression
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @return Absolute path to the node resolved by the expression (the node may not actually exist).
   * @throws ExActionFailed If the XPath cannot be executed or nodes names for path steps cannot be generated.
   * @throws ExTooMany If too many nodes are matched.
   */
  public String getAbsolutePathForCreateableXPath(DOM pRelativeDOM, final String pFoxExtendedXpath)
  throws ExActionFailed, ExTooMany, ExDOMName {

    //Attempt to resolve a single existing node
    XPathResult lXPathResult = extendedXPathResult(pRelativeDOM, pFoxExtendedXpath);
    DOMList lResultDOMList = lXPathResult.asDOMList();
    int lSize = lResultDOMList.getLength();
    if(lSize == 1) {
      return lResultDOMList.item(0).absolute();
    }
    if (lSize > 1) {
      throw new ExTooMany("Too many elements (expected 1, got " + lResultDOMList.getLength() + ") match XPath: " + pFoxExtendedXpath);
    }

    // As for create, don't allow multiple DOMs to have creation operations run on them
    if(lXPathResult.getNumberOfImplicatedDocuments() > 1) {
      throw new ExActionFailed("BADPATH", "Unable to create from XPath when more than one document involved for XPath: " + pFoxExtendedXpath);
    }

    //No node found and a single DOM implicated - establish what the absolute path would be.
    try {
      return lXPathResult.getRelativeDOM().getAbsolutePathForCreateableXPath(pFoxExtendedXpath, this);
    }
    catch(ExBadPath x) {
      throw new ExActionFailed("BADPATH", "Bad XPath encountered while attempting absolute path evaluation for: " + pFoxExtendedXpath, x);
    }
    catch(ExTooMany x) {
      throw new ExTooMany("Too many nodes encountered while attempting absolute path evaluation for: " + pFoxExtendedXpath, x);
    }
  }


  /**
   * Executes a FOX XPath which returns a node list.
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @param pDefaultContextLabelOptional Specify a context label to use as the initial context node of the XPath expression.
   * If null, the default ATTACH point is used.
   * @return The nodes resolved by the XPath as a DOMList.
   * @throws ExActionFailed If the XPath cannot be executed.
   */
  public DOMList extendedXPathUL(String pFoxExtendedXpath, String pDefaultContextLabelOptional)
  throws  ExActionFailed {

    // Internal validation
    if(pFoxExtendedXpath == null) {
      throw new ExInternal("extendedXPath1E passed null XPath");
    }

    // Locate context node
    DOM lContextDOM = getUElem(pDefaultContextLabelOptional);

    // Short circuit self expression
    if(pFoxExtendedXpath.length() == 0 || pFoxExtendedXpath.equals(".")) {
      DOMList result = new DOMList(1);
      result.add(lContextDOM);
      return result;
    }

    // Return target element
    return extendedXPathUL(lContextDOM, pFoxExtendedXpath);
  }

  /**
   * Executes a FOX XPath which returns a node list.
   * @param pRelativeDOM The initial context node of the XPath expression.
   * @param pFoxExtendedXpath The XPath expression to execute.
   * @return The nodes resolved by the XPath as a DOMList.
   * @throws ExActionFailed If the XPath cannot be executed.
   */
  public DOMList extendedXPathUL(final DOM pRelativeDOM, final String pFoxExtendedXpath)
  throws ExActionFailed {
    XPathResult xpathResult = extendedXPathResult(pRelativeDOM, pFoxExtendedXpath);
    return xpathResult.asDOMList();
  }

  /**
   * Evaluates an XPath expression and returns the result as a boolean. For details on how various result objects are
   * converted into booleans, see {@link XPathResult#asBoolean}.
   * <br/><br/>
   * Note the following arguments always return true and do not incur XPath evaluation:
   * <ul>
   * <li>Empty String</li>
   * <li>.</li>
   * <li>1</li>
   * <li>true()</li>
   * </ul>
   * Similarly the following are always false:
   * <ul>
   * <li>0</li>
   * <li>false()</li>
   * </ul>
   * @param pRelativeDOM Initial context node of the XPath expression.
   * @param pFoxExtendedXpath The XPath to be evaluated.
   * @return Boolean result of the XPath expression.
   * @throws ExActionFailed If XPath evaluation fails.
   */
  public boolean extendedXPathBoolean(final DOM pRelativeDOM, final String pFoxExtendedXpath)
  throws ExActionFailed {

     // Internal validation
    if(pFoxExtendedXpath==null) {
      throw new ExInternal("extendedXPath1E passed null XPath");
    }

    // Short circuit true expressions "." "" "true()" "1" (this does not inc XPATH usage count)
    if(pFoxExtendedXpath.length()==0
    || pFoxExtendedXpath.equals(".")
    || pFoxExtendedXpath.equals("1")
    || pFoxExtendedXpath.equals("true()")
    ) {
      return true;
    }

    // Short circuit false expressions "false()" (this does not inc XPATH usage count)
    if(pFoxExtendedXpath.equals("0") ||  pFoxExtendedXpath.equals("false()")) {
      return false;
    }

    return extendedXPathResult(pRelativeDOM, pFoxExtendedXpath, FoxXPathResultType.BOOLEAN).asBoolean();
  }

  /**
   * Evaluates an XPath expression and returns the result as a String. For details on how various result objects are
   * converted into Strings, see {@link XPathResult#asString}.
   * @param pRelativeDOM Initial context node of the XPath expression.
   * @param pFoxExtendedXpath The XPath to be evaluated.
   * @return The String result of XPath evaluation.
   * @throws ExActionFailed If XPath evaluation fails.
   */
  public String extendedXPathString(final DOM pRelativeDOM, final String pFoxExtendedXpath)
  throws ExActionFailed {
    return extendedXPathResult(pRelativeDOM, pFoxExtendedXpath, FoxXPathResultType.STRING).asString();
  }

  /**
   * Tests whether the given context label exists.
   * @param pLabel The label to check.
   * @return True if the context specified by pLabel is defined, false otherwise.
   */
  public boolean existsContext(String pLabel){
    //Note: mLabelToDOMHandler may contain labels which are yet to be loaded
    return isLabelLoaded(pLabel) || mLabelToDOMHandler.containsKey(pLabel);
  }

  /**
   * Get a raw XPathResult from an XPath expression. By default, Saxon will evaluate the XPath and expect a
   * nodelist as a result. Therefore if  you need a non-nodelist result, i.e. a String, you should use the specific
   * methods provided by ContextUElem for this, or provide a FoxXPathResultType to the other signature of this method.
   * @param pRelativeDOM The relative DOM node.
   * @param pFoxExtendedXpath The Fox Extended XPath to evaluate.
   * @return The wrapper for the XPath result.
   * @throws ExActionFailed If XPath evaluation fails.
   */
  public XPathResult extendedXPathResult(final DOM pRelativeDOM, final String pFoxExtendedXpath)
  throws ExActionFailed {
    return extendedXPathResult(pRelativeDOM, pFoxExtendedXpath, FoxXPathResultType.DOM_LIST);
  }

  /**
   * Get a raw XPathResult from an XPath expression. Saxon will evaluate the XPath and expect to return a result of a
   * type specified by pResultType.
   * This should be used to optimise the XPath evaluation.
   * @param pRelativeDOM The relative DOM node.
   * @param pFoxExtendedXpath The Fox Extended XPath to evaluate.
   * @param pResultType
   * @return The wrapper for the XPath result.
   * @throws ExActionFailed If XPath evaluation fails.
   */
  public XPathResult extendedXPathResult(final DOM pRelativeDOM, final String pFoxExtendedXpath, FoxXPathResultType pResultType)
  throws ExActionFailed {
    return extendedXPathResult(pRelativeDOM, pFoxExtendedXpath, pResultType, XPathWrapper.NO_WRAPPER);
  }

  /**
   * Get a raw XPathResult from an XPath expression. Saxon will evaluate the XPath and expect to return a result of a
   * type specified by pResultType.
   * This should be used to optimise the XPath evaluation.
   * @param pRelativeDOM The relative DOM node.
   * @param pFoxExtendedXpath The Fox Extended XPath to evaluate.
   * @param pResultType
   * @return The wrapper for the XPath result.
   * @throws ExActionFailed If XPath evaluation fails.
   */
  private XPathResult extendedXPathResult(final DOM pRelativeDOM, final String pFoxExtendedXpath, FoxXPathResultType pResultType, XPathWrapper pXPathWrapper)
  throws ExActionFailed {
    setBaseSelf(pRelativeDOM, pFoxExtendedXpath);
    try {
      return FoxGlobals.getInstance().getFoxXPathEvaluator().evaluate(pFoxExtendedXpath, pRelativeDOM, this, pResultType, pXPathWrapper);
    }
    catch (ExBadPath e) {
      throw new ExActionFailed("BADPATH", "Failed to execute XPath", e);
    }
    finally {
      clearBaseSelf();
    }
  }

  /**
   * Evaluates an XPath expression as a String, or returns a constant String.
   * <br/><br/>
   * If the expression in pStringOrFoxExtendedXpath is nested in an {@link XPathWrapper} function call, then the path
   * argument of the function is extracted and evaluated as an XPath expression. Note that this will result in
   * the <i>shallow</i> value of the targeted node, as per {@link ContextUElem#extendedXPathString(DOM, String)}.
   * If the deep value of the node is truly required, a call to the <code>string()</code> function should be double-nested
   * within the XPath wrapper, e.g. <code>string(string(./HTML_NODE))</code>.
   * <br/><br/>
   * If the expression is not nested in a <code>string()</code> function call, the argument is considered to be a string
   * constant and is returned as is.
   *
   * @param pRelativeDOM Initial context node of any XPath expression.
   * @param pStringOrExtendedXPath A constant string or XPath string expression. I.e. "Enter Well" or "string(localname())"
   * @return The String result.
   * @throws ExActionFailed If XPath processing fails.
   */
  public String extendedStringOrXPathString(final DOM pRelativeDOM, final String pStringOrExtendedXPath)
  throws ExActionFailed {
    String lTrimmed = safeTrim(pStringOrExtendedXPath);

    XPathWrapper lWrapper = XPathWrapper.getWrapperForXPathString(lTrimmed);
    if(lWrapper != XPathWrapper.NO_WRAPPER) {
      return extendedConstantOrXPathResult(pRelativeDOM, lTrimmed, lWrapper).asString();
    }
    else {
      //Just return the string directly (don't bother converting to an XPathResult just to call .asString())
      return lTrimmed;
    }
  }

  /**
   * Evaluates an XPath expression or returns a constant value. In either case the result is presented as an {@link XPathResult},
   * which consumers can use to safely convert the result to the desired type.
   * <br/><br/>
   * If the expression in pConstantOrExtendedXPath is nested in an {@link XPathWrapper} function call, then the XPath is
   * extracted and evaluated as an XPath expression. If there is no XPathWrapper the string argument is immediately returned
   * wrapped in an XPathResult. Consumers should prefer {@link #extendedStringOrXPathString} if they just need a string result
   * and don't require the conversion features provided by an XPathResult.
   *
   * @param pRelativeDOM Initial context node of any XPath expression.
   * @param pConstantOrExtendedXPath A constant string or XPath string expression. I.e. "Enter Well" or "string(localname())"
   * @return XPathResult.
   * @throws ExActionFailed If XPath processing fails.
   */
  public XPathResult extendedConstantOrXPathResult(final DOM pRelativeDOM, final String pConstantOrExtendedXPath)
  throws ExActionFailed {
    String lTrimmed = safeTrim(pConstantOrExtendedXPath);
    return extendedConstantOrXPathResult(pRelativeDOM, lTrimmed, XPathWrapper.getWrapperForXPathString(lTrimmed));
  }

  private XPathResult extendedConstantOrXPathResult(final DOM pRelativeDOM, final String pConstantOrExtendedXPath, final XPathWrapper pXPathWrapper)
  throws ExActionFailed {

    if(pXPathWrapper != XPathWrapper.NO_WRAPPER) {
      //Trim string() etc wrapper function calls - this will mean the shallow value of the targeted node is retrieved.
      //If the user genuinely needed the deep value, they can double nest the function call - i.e. string(string(./DEEP_NODE))
      StringBuilder lPathSB = new StringBuilder(pConstantOrExtendedXPath);
      lPathSB.delete(0, pXPathWrapper.getExternalName().length() + 1); //+1 for open parenthesis
      //Sanity check the user closed the parentheses
      if(lPathSB.charAt(lPathSB.length()-1) != ')'){
        throw new ExActionFailed("BADPATH", "Bad path: " + pXPathWrapper.getExternalName() + ") function call missing closing parenthesis.");
      }
      lPathSB.deleteCharAt(lPathSB.length()-1);

      return extendedXPathResult(pRelativeDOM, lPathSB.toString(), FoxXPathResultType.STRING, pXPathWrapper);
    }
    else {
      return XPathResult.getConstantResultFromString(pConstantOrExtendedXPath);
    }
  }

  /**
   * Executes a FOX XPath which returns a single element, creating elements along the path if they are not found. See
   * {@link DOM#getCreate1E} for more information. The ATTACH node is used as the initial context node.
   * @param pFoxExtendedXPath The XPath expression to execute.
   * @return The matched Element.
   * @exception ExActionFailed  If the XPath returns the wrong cardinality or if the XPath cannot be executed.
   */
  public DOM getCreateXPath1E(String pFoxExtendedXPath)
  throws ExActionFailed, ExTooMany {
    try {
      return extendedXPath1E(attachDOM(), pFoxExtendedXPath, true);
    }
    catch (ExTooFew e) {
      throw new ExInternal("Unexpected ExTooFew encountered", e); //shouldn't happen - method will always create nodes
    }
  }

  /**
   * Executes a FOX XPath which returns a single element, creating elements along the path if they are not found. See
   * {@link DOM#getCreate1E} for more information.
   * @param pFoxExtendedXPath The XPath expression to execute.
   * @param pDefaultContextLabel The context label to use as the initial context node. If null, the ATTACH node is used.
   * @return The matched Element.
   * @exception ExActionFailed If the XPath returns the wrong cardinality or if the XPath cannot be executed.
   */
  public DOM getCreateXPath1E(String pFoxExtendedXPath, String pDefaultContextLabel)
  throws ExActionFailed, ExTooMany {
    try {
      return extendedXPath1E(getUElem(pDefaultContextLabel), pFoxExtendedXPath, true);
    }
    catch (ExTooFew e) {
      throw new ExInternal("Unexpected ExTooFew encountered", e); //shouldn't happen - method will always create nodes
    }
  }

  /**
   * Executes a FOX XPath which returns a node list, creating elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information. The ATTACH node is used as the initial context node.
   * @param pFoxExtendedXPath The XPath expression to execute.
   * @return The matched nodes as a DOMList.
   * @exception ExActionFailed If the XPath cannot be executed.
   */
  public DOMList getCreateXPathUL(String pFoxExtendedXPath)
  throws ExActionFailed {
    return getCreateXPathUL(pFoxExtendedXPath, ATTACH);
  }

  /**
   * Executes a FOX XPath which returns a node list, creating elements along the path if they are not found. See
   * {@link DOM#getCreate1E}for more information.
   * @param pFoxExtendedXPath The XPath expression to execute.
   * @param pDefaultContextLabelOptional The context label to use as the initial context node. If null, the
   * ATTACH node is used.
   * @return The matched nodes as a DOMList.
   * @exception ExActionFailed If the XPath cannot be executed.
   */
  public DOMList getCreateXPathUL(String pFoxExtendedXPath,String pDefaultContextLabelOptional)
  throws ExActionFailed {
    // Locate context node
    DOM lContextDOM = getUElem(pDefaultContextLabelOptional);

    XPathResult xpathResult = extendedXPathResult(lContextDOM, pFoxExtendedXPath);
    DOMList lResultDOMList = xpathResult.asDOMList();
    int size = lResultDOMList.getLength();
    if(size >= 1) {
      return lResultDOMList;
    }

    // When missing validation
    if(xpathResult.getNumberOfImplicatedDocuments() > 1) {
      throw new ExActionFailed("BADPATH", "Unable to create xpath when more than one document involved: "+pFoxExtendedXPath);
    }

    // Create elements using XPATH expression
    try {
      return xpathResult.getRelativeDOM().getCreateXPathUL(pFoxExtendedXPath, this);
    }
    catch(ExBadPath x) {
      throw new ExActionFailed("BADPATH", "Bad XPATH expression: "+pFoxExtendedXPath, x);
    }
  }

  /**
   * Gets all DocControls implicated by the non-lazy labels in this ContextUElem.
   */
  private Set<DocControl> getLoadedDocControlSet() {

    Set<DocControl> lDocumentRootDOMSet = new HashSet<>();

    for(DOMHandler lDOMHandler : mLabelToDOMHandler.values()) {

      String lContextLabel = lDOMHandler.getContextLabel();

      if(isLabelLoaded(lContextLabel)) {
        DOM lDOM = getDOMForLabel(lContextLabel);
        DocControl lDocControl = lDOM.getDocControl();
        if(lDocControl.isAttachedDocControl()){
          //TODO PN XTHREAD what is the point of the above test?
          lDocumentRootDOMSet.add(lDocControl);
        }
        else {
          Track.debug("ContextUElem.getLoadedDocControlSet", "Unattached doc control for " + lContextLabel);
        }
      }
    }

    return lDocumentRootDOMSet;
  }


  /**
   * Gets a set of all DocControls implicated by the labels in the given list.
   * @param pLabelList List of labels to use to retrieve DocControls.
   * @return Set of implicated DocControls.
   */
  public Set<DocControl> labelListToDocControlSet(List<String> pLabelList){

    Set<DocControl> lDocControlSet = new HashSet<DocControl>();
    for(String lLabel : pLabelList){
      lDocControlSet.add(
        getUElem(lLabel).getDocControl()
      );
    }

    return lDocControlSet;
  }

  /**
   * Searches for an element with the given FOXID in all the implicated documents of this ContextUElem. If no match is
   * found, an exception is raised. Note that this method will load lazy DOMs if the match cannot be found in non-lazy DOMs.
   * @param pRef The FOXID of the element to retrieve.
   * @return The matched element.
   */
  public DOM getElemByRef(String pRef) {
    DOM lRefElem = getElemByRefOrNull(pRef);
    if (lRefElem != null) {
      return lRefElem;
    }
    throw new ExInternal("ContextUElem.getElemByRef() cannot resolve reference: "+pRef);
  }

  /**
   * Searches for an element with the given FOXID in all the implicated documents of this ContextUElem. Note that this
   * method will load lazy DOMs if the match cannot be found in non-lazy DOMs.
   * @param pRef The FOXID of the element to retrieve.
   * @return The matched element, or null if it could not be found.
   */
  public DOM getElemByRefOrNull(String pRef) {
    DOM lRefElem = null;

    //Search attach DOM first, then loaded documents, before finally loading unloaded documents as a last resort

    //Check attach DOM first
    DOM lAttachDOM = attachDOM();
    DocControl lDataDocControl = lAttachDOM.getDocControl();
    lRefElem = lDataDocControl.getElemByRefOrNull(pRef);
    if (lRefElem != null) {
      return lRefElem;
    }

    //now search loaded documents
    for(DocControl lControl : getLoadedDocControlSet()) {
      if (lControl != lDataDocControl) {
        lRefElem = lControl.getElemByRefOrNull(pRef);
        if (lRefElem != null) {
          return lRefElem;
        }
      }
    }

    //Not found in loaded; search unloaded
    for(DOMHandler lHandler : getDOMHandlersInLoadPrecedenceOrder()) {
      String lContextLabel = lHandler.getContextLabel();
      //Invoke a load (if it's loaded, it's already been searched, so skip in this loop)
      if(!isLabelLoaded(lContextLabel)) {
        DOM lDOM = getUElem(lContextLabel);
        lRefElem = lDOM.getDocControl().getElemByRefOrNull(pRef);
        if(lRefElem != null) {
          return lRefElem;
        }
      }
    }

    //Element not found; return null
    return null;
  }

  /**
   * Sets the :{baseself} label of this ContextUElem to point to pDOM, if the :{baseself} label is used.
   * @param pDOM Node to set as baseself.
   * @param pXPath XPath being evaluated.
   */
  private void setBaseSelf(DOM pDOM, String pXPath) {
    if(pXPath.indexOf(ContextLabel.BASESELF.asColonSquiggly()) != -1){
      setUElemInternal(ContextLabel.BASESELF, null, null, pDOM, true, false, null, false);
    }
  }

  /**
   * Removes the :{baseself} label from this ContextUElem. Invoke this after XPath evaluation.
   */
  private void clearBaseSelf() {
    removeUElem(ContextLabel.BASESELF.asString(), false, false);
  }

  /**
   * Test if the DOM referred to by pLabel is still attached to its document tree.
   * @param pLabel The context label to test.
   * @return True if the label DOM is still attached, false otherwise.
   */
  public boolean isLabelStillAttached(String pLabel){
    return getUElem(pLabel).isAttached();
  }
}

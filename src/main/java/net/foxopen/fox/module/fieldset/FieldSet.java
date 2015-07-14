package net.foxopen.fox.module.fieldset;


import com.thoughtworks.xstream.XStream;
import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.fieldset.action.InternalAction;
import net.foxopen.fox.module.fieldset.action.InternalActionContext;
import net.foxopen.fox.module.fieldset.clientaction.ClientAction;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.InternalFieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.SingleValueFieldInfo;
import net.foxopen.fox.module.fieldset.fieldmgr.ActionFieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeAction;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.persistence.xstream.XStreamManager;
import net.foxopen.fox.track.Track;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FieldSet {

  private static final String LABEL_PREFIX = "fs";

  /* IMPORTANT: all the fields below are serialised using Kryo unless marked as transient. If any more fields are added,
     they MUST NOT implicate a complex object graph. Ensure all objects being referenced by these fields are appropriate
     candidates for serialisation.  */

  private final String mOutwardFieldSetLabel;

  private final Map<String, JITMapSetInfo> mJITMapSetInfoFields = new HashMap<>();
  private final List<PostedValueProcessor> mFields = new ArrayList<>();
  private final Map<String, InternalAction> mInternalRunnableActions = new HashMap<>();
  private final Map<String, ClientAction> mClientActions = new HashMap<>();
  private final Set<String> mExternalRunnableActions = new HashSet<>();
  private final Set<String> mUploadTargets = new HashSet<>();

  private final Map<String, RadioGroup> mRadioGroups = new HashMap<>();
  private final Set<String> mImplicatedDocumentLabels = new HashSet<>();

  private boolean mFieldSetAlreadyApplied = false;

  //TODO ensure this is reset if necessary
  private transient long mFieldSequence = 0;

  private transient FieldSequenceGenerator mFieldSequenceGenerator;

  private transient Set<String> mEditableItemRefs = new HashSet<>();

  private transient Map<String, FieldMgr> mFoxIdToFieldMgrMap = new HashMap<>();

  private transient Map<String, String> mFoxIdToExternalFoxId = new HashMap<>();

  public static FieldSet createNewFieldSet(ActionRequestContext pRequestContext){
    String lOutwardFieldSetLabel = LABEL_PREFIX + XFUtil.unique();
    return new FieldSet(pRequestContext, lOutwardFieldSetLabel);
  }

  private FieldSet(ActionRequestContext pRequestContext, String pOutwardFieldSetLabel) {
    mOutwardFieldSetLabel = pOutwardFieldSetLabel;
    mFieldSequenceGenerator = FieldSequenceGenerator.createGenerator(pRequestContext, this);
  }

  public FieldMgr createFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfoItem) {
    FieldMgr lNewFieldMgr = FieldMgr.createFieldMgr(pEvaluatedNodeInfoItem, this, mFieldSequenceGenerator.nextFieldId(pEvaluatedNodeInfoItem));
    String lFoxId = pEvaluatedNodeInfoItem.getDataItem().getFoxId();
    mFoxIdToFieldMgrMap.put(lFoxId, lNewFieldMgr);
    mFoxIdToExternalFoxId.put(lFoxId, lNewFieldMgr.getExternalFieldId());

    return lNewFieldMgr;
  }

  public ActionFieldMgr createFieldMgr(EvaluatedNodeAction pEvaluatedNodeAction) {
    return FieldMgr.createFieldMgr(pEvaluatedNodeAction, this, mFieldSequenceGenerator.nextFieldId(pEvaluatedNodeAction));
  }

  /**
   * Returns true if the add was allowed.
   * @param pFieldInfo
   * @param pDataDOM
   * @param pContextUElem
   * @return
   */
  public boolean addFieldInfo(FieldInfo pFieldInfo, DOM pDataDOM, ContextUElem pContextUElem) {

    String lDOMRef = pDataDOM.getRef();
    if(mEditableItemRefs.contains(lDOMRef)) {
      return false;
    }
    else {
      if(pFieldInfo != null) {
        mFields.add(pFieldInfo);
      }
      mEditableItemRefs.add(lDOMRef);
      mImplicatedDocumentLabels.add(pContextUElem.getDocumentLabelForNode(pDataDOM));

      return true;
    }
  }

  public void addInternalField(InternalFieldInfo pInternalField) {
    mFields.add(pInternalField);
  }

  //TODO PN HTML consolidate methods, rationalise names

  public InternalActionContext addInternalAction(InternalAction pInternalAction) {
    return addRunnableInternalAction(XFUtil.unique(), pInternalAction);
  }

  public InternalAction getInternalAction(String pActionId) {
    return mInternalRunnableActions.get(pActionId);
  }

  //Check if return needed
  private InternalActionContext addRunnableInternalAction(String pActionId, InternalAction pInternalAction) {
    mInternalRunnableActions.put(pActionId, pInternalAction);
    return new InternalActionContext(mOutwardFieldSetLabel, pActionId);
  }

  //TODO PN HTML consolidate methods, rationalise names
  public void registerExternalRunnableAction(EvaluatedNode pEvaluatedNode) {
    mExternalRunnableActions.add(pEvaluatedNode.getActionName() + "/" + pEvaluatedNode.getActionContextRef());
  }

  //TODO PN remove - added back for error ref stuff
  public void registerExternalRunnableAction(String pActionName, String pActionContextRef)  {
    mExternalRunnableActions.add(pActionName + "/" + pActionContextRef);
  }

  public boolean isExternalActionRunnable(String pActionName, String pActionContextRef) {
    return mExternalRunnableActions.contains(pActionName + "/" + pActionContextRef);
  }

  public void registerClientAction(ClientAction pClientAction) {
    mClientActions.put(pClientAction.getActionKey(), pClientAction);
  }

  public RadioGroup getOrCreateRadioGroup(EvaluatedNodeInfo pEvaluatedNodeInfo) {

    String lRadioGroupName = pEvaluatedNodeInfo.getStringAttribute(NodeAttribute.RADIO_GROUP);
    if(XFUtil.isNull(lRadioGroupName)) {
      throw new ExInternal("Node info " + pEvaluatedNodeInfo.getIdentityInformation() + " missing radio-group definition");
    }

    DOM lEvalContextDOM = pEvaluatedNodeInfo.getEvaluateContextRuleItem();

    String lRadioOwnerXPath = pEvaluatedNodeInfo.getStringAttribute(NodeAttribute.RADIO_OWNER);
    DOM lRadioOwnerDOM;
    if(!XFUtil.isNull(lRadioOwnerXPath)) {
      try {
        lRadioOwnerDOM = pEvaluatedNodeInfo.getContextUElem().extendedXPath1E(lEvalContextDOM, lRadioOwnerXPath, false);
      }
      catch(ExActionFailed | ExCardinality x) {
        throw new ExInternal("radio-owner XPath failed to identify one owner node", x);
      }
    }
    else {
      lRadioOwnerDOM = lEvalContextDOM;
    }

    String lRadioOwnerDOMRef = lRadioOwnerDOM.getRef();

    //Attempt to find a radio group with the given key
    RadioGroup lRadioGroup = getRadioGroup(lRadioOwnerDOMRef, lRadioGroupName);

    //Bootstrap a new radio group if we're the first one in
    if (lRadioGroup == null) {
      String lNewGroupId = mFieldSequenceGenerator.nextFieldId(pEvaluatedNodeInfo);
      lRadioGroup = RadioGroup.createRadioGroup(lRadioOwnerDOMRef, lRadioGroupName, lNewGroupId);
      addRadioGroup(lRadioGroup);
    }

    return lRadioGroup;
  }

  private RadioGroup getRadioGroup(String pOwnerDOMRef, String pGroupName) {
    return mRadioGroups.get(RadioGroup.getRadioGroupKey(pOwnerDOMRef, pGroupName));
  }

  private void addRadioGroup(RadioGroup pRadioGroup) {
    mRadioGroups.put(pRadioGroup.getRadioKey(), pRadioGroup);
  }

  public void serialiseToWriter(Writer pWriter) {
    Track.pushInfo("FieldSetSerialise");
    try {
      XStream lXStream = XStreamManager.getXStream();
      lXStream.toXML(this, pWriter);
    }
    finally {
      Track.pop("FieldSetSerialise");
    }
  }

  public String getOutwardFieldSetLabel() {
    return mOutwardFieldSetLabel;
  }

  public long getNextFieldSequence(){
    return mFieldSequence++;
  }

  public void applyChangesToDOMs(ActionRequestContext pRequestContext, Map<String, String[]> pPostedFormValuesMap) {

    // Skip field set apply if already applied
    if(mFieldSetAlreadyApplied) {
      return; //TODO throw error?
    }

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    List<ChangeActionContext> lChangeActions = new ArrayList<>();

    //Force the ContextUElem to load all implicated documents in advance, so getElemByRef doesn't have to search through unloaded documents
    for(String lDocLabel : mImplicatedDocumentLabels) {
      lContextUElem.loadUElem(lDocLabel);
    }

    //If client actions have been performed, apply them now
    if(pPostedFormValuesMap.get("client_actions") != null) {
      applyClientActions(pRequestContext, SingleValueFieldInfo.singlePostedValue(pPostedFormValuesMap.get("client_actions"), "client_actions"));
    }

    //Apply field changes
    for (PostedValueProcessor lFieldInfo : mFields) {
      List<ChangeActionContext> lApplyActions = lFieldInfo.applyPostedValues(pRequestContext, pPostedFormValuesMap.get(lFieldInfo.getExternalName()));
      lChangeActions.addAll(lApplyActions);
    }

    //Apply radio group changes
    for(RadioGroup lRadioGroup : mRadioGroups.values()) {
      List<ChangeActionContext> lApplyActions = lRadioGroup.applyPostedValues(pRequestContext, pPostedFormValuesMap.get(lRadioGroup.getRadioGroupId()));
      lChangeActions.addAll(lApplyActions);
    }

    // Flag field changes as having been applied
    mFieldSetAlreadyApplied = true;

    // Run change actions
    for (ChangeActionContext lAction : lChangeActions) {
      //Localise for item contexts
      lContextUElem.localise("change-action/" + lAction.getActionName());
      // Run change action
      try {
        // Define contexts :{itemrec} and :{item} used to run change action
        lContextUElem.setUElem(ContextLabel.ITEMREC, lContextUElem.getElemByRef(lAction.getActionContextRef()));
        lContextUElem.setUElem(ContextLabel.ITEM, lContextUElem.getElemByRef(lAction.getItemContextRef()));

        //TODO PN XTHREAD - currently this is just ignoring CSTs/breaks etc, should probably error
        XDoRunner lCommandRunner = pRequestContext.createIsolatedCommandRunner(true);
        XDoCommandList lChangeAction = pRequestContext.resolveActionName((lAction.getActionName()));

        lCommandRunner.runCommands(pRequestContext, lChangeAction);
      }
      catch (Throwable e) {
        throw new ExInternal("action " + lAction.getActionName() + " failed on an item change-action attribute", e);
      }
      finally {
        lContextUElem.delocalise("change-action/" + lAction.getActionName());
      }
    }
  }

  /**
   * Adds the given context ref as a valid upload target in this FieldSet. If the context ref has already been added,
   * this method returns false.
   * @param pContextRef DOM ref/FOXID of the upload target.
   * @return True if the add was allowed, false otherwise.
   */
  public boolean addUploadTarget(String pContextRef) {
    if(mUploadTargets.contains(pContextRef)) {
      return false;
    }
    else {
      mUploadTargets.add(pContextRef);
      return true;
    }
  }

  public boolean isValidUploadTarget(String pContextRef) {
    return mUploadTargets.contains(pContextRef);
  }

  public FieldMgr getFieldMgrForFoxIdOrNull(String pFoxId) {
    return mFoxIdToFieldMgrMap.get(pFoxId);
  }

  public String getExternalFoxId(DOM pRelativeDOM) {
    return getExternalFoxId(pRelativeDOM.getFoxId());
  }

  public String getExternalFoxId(String pFoxId) {
    String lExternalFoxId = mFoxIdToExternalFoxId.get(pFoxId);
    if(lExternalFoxId == null) {
      lExternalFoxId = "f" + getNextFieldSequence();
      mFoxIdToExternalFoxId.put(pFoxId, lExternalFoxId);
    }

    return lExternalFoxId;
  }

  /**
   * Parses a submitted JSON string into a list of 1 or more {@link ClientAction}s to run, validates that the actions
   * are runnable, then applies the actions to the given RequestContext.
   * @param pRequestContext Current RequestContext.
   * @param pClientActionJSONString JSON String which can be parsed into a JSON array of client action objects. This method
   *                                requires that each JSON object contains an action_key property; the action implementation
   *                                may require additional parameters.
   */
  public void applyClientActions(ActionRequestContext pRequestContext, String pClientActionJSONString) {

    Track.pushInfo("ApplyClientActions");
    try {
      //Parse the single posted value into a JSON array
      JSONArray lActionQueue;
      try {
        lActionQueue = (JSONArray) new JSONParser().parse(pClientActionJSONString);
      }
      catch (ParseException e) {
        throw new ExInternal("Failed to parse client action JSON", e);
      }

      //Each item in the queue should be a JSON object containing the action definition
      for(int i=0; i< lActionQueue.size(); i++) {
        JSONObject lActionInfo = (JSONObject) lActionQueue.get(i);

        String lActionKey = (String) lActionInfo.get("action_key");
        if(XFUtil.isNull(lActionKey)) {
          throw new ExInternal("Malformed client action JSON: object at index " + i + " is missing action_key property");
        }

        //Attempt to resolve the action
        ClientAction lClientAction = mClientActions.get(lActionKey);
        if(lClientAction != null) {

          Track.pushInfo("RunClientAction", lClientAction.getClass().getSimpleName());
          try {
            //action_params map is optional, so default to an empty object if not provided
            Object lActionParams = lActionInfo.get("action_params");
            if(lActionParams == null) {
              lActionParams = new JSONObject();
            }

            lClientAction.applyAction(pRequestContext, (JSONObject) lActionParams);
          }
          finally {
            Track.pop("RunClientAction");
          }
        }
        else {
          throw new ExInternal("Attempt to run a client action with key " + lActionKey + " which was not in the FieldSet");
        }
      }
    }
    catch (ClassCastException e) {
      //Catch all for any JSON parsing issues
      throw new ExInternal("Client action JSON does not contain data in expected format", e);
    }
    finally {
      Track.pop("ApplyClientActions");
    }
  }

  /**
   * Add JITMapSetInfo to the FieldSet so that the MapSetWebService requests can re-construct the MapSet with the correct
   * binds to perform the search query
   *
   * @param pFieldID External ID of the field the JITMapSetInfo is for
   * @param pEvaluatedNodeInfoItem EvaluatedNodeInfoItem the JITMapSet is defined on
   */
  public void addJITMapSetInfo(String pFieldID, EvaluatedNodeInfoItem pEvaluatedNodeInfoItem) {
    mJITMapSetInfoFields.put(pFieldID, new JITMapSetInfo(pEvaluatedNodeInfoItem));
  }

  /**
   * Get a JITMapSetInfo for a given External FieldID
   *
   * @param pFieldID External ID of the field the JITMapSetInfo is from
   * @return JITMapSetInfo object
   */
  public JITMapSetInfo getJITMapSetInfo(String pFieldID) {
    return mJITMapSetInfoFields.get(pFieldID);
  }

  /**
   * Flushes transient churn specific data for this FieldSet so it is not retained for longer than necessary.
   */
  public void flushTransientData() {
    //TODO PN: this should be dealt with more gracefully - FOXRD-363
    mEditableItemRefs.clear();
    mFoxIdToFieldMgrMap.clear();
    mFoxIdToExternalFoxId.clear();
  }
}

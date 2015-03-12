package net.foxopen.fox.thread.persistence;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.download.DownloadParcel;
import net.foxopen.fox.module.facet.ModuleFacet;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.thread.ThreadPropertyMap;
import net.foxopen.fox.thread.stack.callback.CallbackHandler;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;

import java.util.Collection;
import java.util.List;
import java.util.Map;


public interface Serialiser {

  public void createUserThreadSession(String pSessionId);

  public void createThread(String pThreadId, String pAppMnem, String pUserThreadSessionId, ThreadPropertyMap pThreadPropertyMap,
                           FieldSet pFieldSet, AuthenticationContext pAuthenticationContext, String pChangeNumber, String pFoxSessionID);

  public void updateThread(String pThreadId, FieldSet pFieldSet, AuthenticationContext pAuthenticationContext, ThreadPropertyMap pThreadPropertyMap,
                           String pChangeNumber, String pFoxSessionID);

  public void deleteThread(String pThreadId);

  public void createModuleCall(String pModuleCallId, int pStackPosition, String pAppMnem, String pModuleName, String pEntryThemeName,
                               Map<String, WorkingDataDOMStorageLocation> pLabelToStorageLocationMap, List<CallbackHandler> pCallbackHandlerList, SecurityScope pSecurityScope);

  public void updateModuleCall(String pModuleCallId, SecurityScope pSecurityScope);

  public void deleteModuleCall(String pModuleCallId);

  public void createStateCall(String pCallId, String pModuleCallId, int pStackPosition, String pStateName, int pScrollPosition, Collection<ContextUElem.SerialisedLabel> pContextualLabels);

  public void updateStateCall(String pCallId, int pScrollPosition, Collection<ContextUElem.SerialisedLabel> pContextualLabels);

  public void deleteStateCallStack(String pModuleCallId);

  public void deleteStateCall(String pStateCallId);

  public void createInternalDOM(String pModuleCallId, String pDocumentName, DOM pDOM);

  public void updateInternalDOM(String pModuleCallId, String pDocumentName, DOM pDOM);

  public void createDownloadParcel(DownloadParcel pDownloadParcel);

  public void createModuleFacet(ModuleFacet pFacet);

  public void updateModuleFacet(ModuleFacet pFacet);

  public void deleteModuleCallFacets(String pCallId);
}

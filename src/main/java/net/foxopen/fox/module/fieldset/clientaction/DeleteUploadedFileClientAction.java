package net.foxopen.fox.module.fieldset.clientaction;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;
import org.json.simple.JSONObject;

public class DeleteUploadedFileClientAction
implements ClientAction {

  private static final String KEY_PREFIX = "DeleteUploadFile/";
  private static final String FILE_CONTAINER_REF_PARAM_NAME = "file_container_ref";

  private final String mUploadContainerRef;
  private final boolean mIsMultiUpload;

  public static String generateActionKey(String pContainerRef) {
    return KEY_PREFIX + pContainerRef;
  }

  public DeleteUploadedFileClientAction(String pUploadContainerRef, boolean pIsMultiUpload) {
    mUploadContainerRef = pUploadContainerRef;
    mIsMultiUpload = pIsMultiUpload;
  }

  @Override
  public String getActionKey() {
    return generateActionKey(mUploadContainerRef);
  }

  @Override
  public void applyAction(ActionRequestContext pRequestContext, JSONObject pParams) {

    String lTargetFileContainerRef = (String) pParams.get(FILE_CONTAINER_REF_PARAM_NAME);
    if(XFUtil.isNull(lTargetFileContainerRef)) {
      throw new ExInternal("DeleteUploadedFile client action missing mandatory " + FILE_CONTAINER_REF_PARAM_NAME + " param");
    }

    //Find the container node (i.e. the upload target node) and the file node within it to be removed
    //This may either be a direct child
    DOM lContainerDOM = pRequestContext.getContextUElem().getElemByRef(mUploadContainerRef);

    if(!mIsMultiUpload) {
      //For single uploads just remove the target's node contents
      Track.info("Removing element contents for ref " + mUploadContainerRef);
      lContainerDOM.removeAllChildren();

      //TODO PN run API delete statement?
    }
    else {
      //For multi uploads remove the repeating element
      DOM lRemoveDOM = pRequestContext.getContextUElem().getElemByRefOrNull(lTargetFileContainerRef);
      if(lRemoveDOM != null) {

        //Only remove the requested DOM if it is a direct child of
        for(DOM lChild : lContainerDOM.getChildElements()) {
          if(lChild.getRef().equals(lRemoveDOM.getRef())) {
            Track.info("Removing element with ref " + lRemoveDOM.getRef());
            lRemoveDOM.remove();
          }
        }

        //TODO PN run API delete statement?
      }
      else {
        Track.alert("Attempted to delete a file but could not resolve reference " + lTargetFileContainerRef);
      }
    }
  }
}

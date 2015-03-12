package net.foxopen.fox.thread.stack.transform;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.thread.ResponseOverride;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.Trackable;


public abstract class CallStackTransformation
implements Trackable {

  public static enum Type {
    EXIT_THIS_PRESERVE_CALLBACKS("exit-this-preserve-callbacks", false),
    EXIT_THIS_CANCEL_CALLBACKS("exit-this-cancel-callbacks", false),
    EXIT_ALL_CANCEL_CALLBACKS("exit-all-cancel-callbacks", false),
    MODAL("modal", false),
    MODAL_REPLACE_ALL_CANCEL_CALLBACKS("modal-replace-all-cancel-callbacks", false),
    MODAL_REPLACE_THIS_CALLBACKS_NOW("modal-replace-this-caller-callbacks-now-then-cancel-callbacks", false),
    MODAL_REPLACE_THIS_CANCEL_CALLBACKS("modal-replace-this-cancel-callbacks", false),
    MODAL_REPLACE_THIS_PRESERVE_CALLBACKS("modal-replace-this-preserve-caller-callbacks-for-exit", false),
    MODAL_RETURN_TO_FIRST_OR_REPLACE_ALL_CANCEL_CALLBACKS("modal-return-to-first-or-replace-all-cancel-callbacks", false),
    MODELESS_ORPHAN_SAME_SESSION("modeless-orphan-same-session", true),
    MODELESS_ORPHAN_NEW_SESSION("modeless-orphan-new-session", true);

    private static final Map<String,Type> gExternalDescriptionsToTypes = new HashMap<>();
    static {
      for(Type lType : values()){
        gExternalDescriptionsToTypes.put(lType.mExternalName, lType);
      }
    }

    public static Type forName(String pExternalName){
      //Switch legacy "modeless" option to a better default
      if("modeless".equals(pExternalName)) {
        Track.alert("ModelessModuleCall", "Defaulting legacy 'modeless' call type to 'modeless-orphan-same-session' - please remove deprecated call type");
        return MODELESS_ORPHAN_SAME_SESSION;
      }

      return gExternalDescriptionsToTypes.get(pExternalName);
    }

    private final String mExternalName;
    private final boolean mIsModeless;

    private Type(String pExternalName, boolean pIsModeless){
      mExternalName = pExternalName;
      mIsModeless = pIsModeless;
    }

    public boolean isModeless() {
      return mIsModeless;
    }

    public String getExternalName() {
      return mExternalName;
    }
  }

  public static ExitModule createExitCallStackTransformation(Type pType, String pExitURI){

    switch(pType){
      case EXIT_THIS_PRESERVE_CALLBACKS:
        return new ExitThis(true, pExitURI);
      case EXIT_THIS_CANCEL_CALLBACKS:
        return new ExitThis(false, pExitURI);
      case EXIT_ALL_CANCEL_CALLBACKS:
        return new ExitAll(pExitURI);
      default:
        throw new IllegalArgumentException(pType + " is not a recognised Exit type CallStackTransformation");
    }
  }

  public static CallStackTransformation createCallStackTransformation(Type pType, ModuleCall.Builder pModuleCallBuilder){

    switch(pType){
      case MODAL:
        return new ModalCall(pModuleCallBuilder);
      case MODAL_REPLACE_THIS_CALLBACKS_NOW:
        return new ModalReplaceThisCall(pModuleCallBuilder, ModalReplaceThisCall.CallbackOption.CALLBACKS_NOW);
      case MODAL_REPLACE_THIS_CANCEL_CALLBACKS:
        return new ModalReplaceThisCall(pModuleCallBuilder, ModalReplaceThisCall.CallbackOption.NO_CALLBACKS);
      case MODAL_REPLACE_THIS_PRESERVE_CALLBACKS:
        return new ModalReplaceThisCall(pModuleCallBuilder, ModalReplaceThisCall.CallbackOption.PRESERVE_CALLBACKS);
      case MODAL_REPLACE_ALL_CANCEL_CALLBACKS:
        return new ModalReplaceAllCall(pModuleCallBuilder);
      case MODAL_RETURN_TO_FIRST_OR_REPLACE_ALL_CANCEL_CALLBACKS:
        return new ModalReplaceReturnToFirstCall(pModuleCallBuilder);
      case MODELESS_ORPHAN_NEW_SESSION:
        return new ModelessCall(pModuleCallBuilder, false);
      case MODELESS_ORPHAN_SAME_SESSION:
        return new ModelessCall(pModuleCallBuilder, true);
      default:
        throw new IllegalArgumentException(pType + " is not a valid CallStackTransformation");
    }
  }

  public abstract void transform(ActionRequestContext pRequestContext, ModuleCallStack pCallStack);

  public ResponseOverride getExitResponseOverride(){
    return null; //TODO PN could this be better
  }
}

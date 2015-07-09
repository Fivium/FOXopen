package net.foxopen.fox.database.storage;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.database.storage.dom.XMLWorkDoc;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.track.Track;

import java.util.HashSet;
import java.util.Set;

/**
 * Object for tracking WorkDocs which require additional validation after they are opened. A WorkDoc may be opened and marked
 * as requiring validation - in this case, the WorkDoc is RO until it is marked as validated. A WorkDocValidator records all
 * WorkDocs which require validation within a churn, and runs the validation logic when {@link #validatePending} is called.<br><br>
 *
 * Module developers signal that a WorkDoc is invalid by raising a CallStackTransformation (e.g. an fm:exit-module). In
 * some circumstances it may not be appropriate to run the CST, for instance if the XThread is being run from a WS request.
 * The {@link Result} interface allows the consumer to choose which behaviour to use in the event of validation failure.
 */
public class WorkDocValidator {

  private final Set<XMLWorkDoc> mWorkDocsPendingValidation = new HashSet<>(3);

  /**
   * Creates a new WorkDocValidator.
   * @return new WorkDocValidator.
   */
  public static WorkDocValidator createValidator() {
    return new WorkDocValidator();
  }

  private WorkDocValidator() {
  }

  /**
   * Marks the given WorkDoc as pending validation. You <strong>must</strong> invoke {@link #validatePending} at a later stage
   * in order to validate any WorkDocs marked as pending by this method.
   * @param pWorkDoc WorkDoc to mark as pending validation.
   */
  public void markAsPendingValidation(XMLWorkDoc pWorkDoc) {
    Track.info("WorkDocPendingValidation", "WorkDoc for SL " + pWorkDoc.getWorkingStoreLocation().getStorageLocationName() + " marked as requiring validation");
    mWorkDocsPendingValidation.add(pWorkDoc);
  }

  /**
   * Validates the pending WorkDocs within this validator by executing their attach SL's fm:validation block.
   * This also clears the pending list to prevent a WorkDoc being validated multiple times. Note that validation is fail fast
   * and subsequent pending WorkDocs are NOT validated if one fails validation.
   * @param pRequestContext Current RequestContext for running validation actions.
   * @return Validation result - the consumer must decided how to behave in the event of an invalid WorkDoc.
   */
  public Result validatePending(ActionRequestContext pRequestContext){

    XDoRunner lCommandRunner = pRequestContext.createCommandRunner(true);
    String lErroneousStorageLocation = null;

    for(XMLWorkDoc lWorkDoc : mWorkDocsPendingValidation) {

      Track.pushInfo("ValidateSL", lWorkDoc.getWorkingStoreLocation().getStorageLocationName());
      try {
        XDoCommandList lValidationCommands = lWorkDoc.getWorkingStoreLocation().getStorageLocation().getValidationCommands();
        ContextUElem lContextUElem = pRequestContext.getContextUElem();

        //Set attach point and run validate command block
        lContextUElem.localise("WorkDocValidate/" + lWorkDoc.getWorkingStoreLocation().getStorageLocationName());
        try {
          lContextUElem.setUElem(ContextLabel.ATTACH, lWorkDoc.getDOM());
          lCommandRunner.runCommands(pRequestContext, lValidationCommands);
        }
        finally {
          lContextUElem.delocalise("WorkDocValidate/" + lWorkDoc.getWorkingStoreLocation().getStorageLocationName());
        }

        //Interpret a CST, break/ignore or error as invalid
        boolean lValid = lCommandRunner.executionAllowed();

        //Tell the WorkDoc it has been validated
        lWorkDoc.markAsValidated(lValid);

        if(!lValid) {
          lErroneousStorageLocation = lWorkDoc.getWorkingStoreLocation().getStorageLocationName();
          Track.info("SLValidationResult", "ValidationFailed");
          break;
        }
        else {
          Track.info("SLValidationResult", "ValidationPassed");
        }
      }
      finally {
        Track.pop("ValidateSL");
      }
    }

    mWorkDocsPendingValidation.clear();

    //Return the appropriate result type
    if(lErroneousStorageLocation != null) {
      return new InvalidResult(lCommandRunner, lErroneousStorageLocation);
    }
    else {
      return new ValidResult();
    }
  }

  /**
   * Result from validating one or more WorkDocs. Consumers can choose if they want to invoke a CST or raise an exception
   * in the event of a validation failure.
   */
  public interface Result {

    /**
     * Finalises the WorkDoc validation result by executing a CallStackTransformation, if the WorkDoc was invalid. If
     * the WorkDoc was valid no action is taken.
     * @param pRequestContext Current RequestConext.
     * @param pModuleCallStack CallStack to transform if the WorkDoc was invalid.
     * @return True if the WorkDoc was valid, false if it was invalid. If false is returned consumers should assume the
     * callstack has been modified.
     */
    boolean finaliseWithTransform(ActionRequestContext pRequestContext, ModuleCallStack pModuleCallStack);

    /**
     * Finalises the WorkDoc validation result by throwing an exception, if the WorkDoc was invalid. If the WorkDoc was
     * valid no action is taken.
     * @return True if the WorkDoc was valid. This will never return false as an exception will be thrown instead. However
     * the boolean return type allows the use of this method within a conditional statement.
     */
    boolean finaliseWithError();
  }

  /**
   * Result implementation for a valid WorkDoc.
   */
  private static class ValidResult
  implements Result {
    @Override
    public boolean finaliseWithTransform(ActionRequestContext pRequestContext, ModuleCallStack pModuleCallStack) {
      return true;
    }

    @Override
    public boolean finaliseWithError() {
      return true;
    }
  }

  /**
   * Result implementation for an invalid WorkDoc.
   */
  private static class InvalidResult
  implements Result {

    private final XDoRunner mCommandRunner;
    private final String mErroneousStorageLocation;

    /** Belt and braces tracker to prevent consumers running the finalise method multiple times */
    private boolean mFinalised = false;

    private InvalidResult(XDoRunner pCommandRunner, String pErroneousStorageLocation) {
      mCommandRunner = pCommandRunner;
      mErroneousStorageLocation = pErroneousStorageLocation;
    }

    private void checkNotFinalised() {
      if(mFinalised) {
        throw new ExInternal("This WorkDocValidatorResult has already been finalised");
      }
      mFinalised = true;
    }

    public boolean finaliseWithTransform(ActionRequestContext pRequestContext, ModuleCallStack pModuleCallStack) {
      checkNotFinalised();

      Track.pushInfo("InvalidStorageLocationTransformation");
      try {
        mCommandRunner.processCompletion(pRequestContext, pModuleCallStack);
      }
      finally {
        Track.pop("InvalidStorageLocationTransformation");
      }

      return false;
    }

    public boolean finaliseWithError() {
      throw new ExInternal("Validation failed for storage location " + mErroneousStorageLocation);
    }
  }
}

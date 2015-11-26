package net.foxopen.fox.ex;


public class ExUploadValidation extends ExRoot {
  static String TYPE = "Fox Upload Validation Error";
  public static enum ValidationErrorType{
    INVALID_CONTENT, UNRECOGNISED_CONTENT, FILE_SIZE;
  }

  private String mMimeType = "";
  private final ValidationErrorType mValidationErrorType;


  public ExUploadValidation(String pMessage, ValidationErrorType pValidationErrorType) {
    super(pMessage, TYPE, null, null);
    mValidationErrorType = pValidationErrorType;
  }

  public ExUploadValidation(String pMessage, ValidationErrorType pValidationErrorType, String pMimeType) {
    super(pMessage, TYPE, null, null);
    mValidationErrorType = pValidationErrorType;
    mMimeType = pMimeType;
  }

  public String getMimeType(){
    return mMimeType;
  }

  public ValidationErrorType getValidationErrorType(){
    return mValidationErrorType;
  }

}

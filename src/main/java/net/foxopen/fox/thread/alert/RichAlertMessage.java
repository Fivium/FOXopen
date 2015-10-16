package net.foxopen.fox.thread.alert;

import org.json.simple.JSONObject;

public abstract class RichAlertMessage
implements AlertMessage {

  public enum DisplayStyle {
    INFO, SUCCESS, WARNING, DANGER;

    public String getCSSClassName() {
      return "modal-alert-" + this.toString().toLowerCase();
    }

    public String getIconName() {
      switch(this) {
        case INFO:
          return "icon-info";
        case SUCCESS:
          return "icon-checkmark";
        case WARNING:
          return "icon-warning";
        case DANGER:
          return "icon-cross";
        default:
          return null;
      }
    }
  }

  private final String mTitle;
  private final DisplayStyle mDisplayStyle;
  private final String mClosePrompt;
  private final String mCSSClass;

  protected RichAlertMessage(String pTitle, DisplayStyle pDisplayStyle, String pClosePrompt, String pCSSClass) {
    mTitle = pTitle;
    mDisplayStyle = pDisplayStyle;
    mClosePrompt = pClosePrompt;
    mCSSClass = pCSSClass;
  }

  public JSONObject getJSONPropertyObject() {
    JSONObject lProperties = new JSONObject();
    lProperties.put("title", mTitle);
    lProperties.put("cssClass", mDisplayStyle.getCSSClassName() + " " + mCSSClass);
    lProperties.put("icon", mDisplayStyle.getIconName());
    lProperties.put("closePrompt", mClosePrompt);

    return lProperties;
  }

}

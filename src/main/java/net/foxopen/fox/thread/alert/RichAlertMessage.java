package net.foxopen.fox.thread.alert;

import org.json.simple.JSONObject;

public abstract class RichAlertMessage
implements AlertMessage {

  public enum DisplayStyle {
    INFO, SUCCESS, WARNING, DANGER;

    public String getCSSClassName() {
      return "modal-alert-" + this.toString().toLowerCase();
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
    lProperties.put("closePrompt", mClosePrompt);

    return lProperties;
  }

}

package net.foxopen.fox.thread.alert;

import net.foxopen.fox.module.NotificationDisplayType;
import org.json.simple.JSONObject;

/**
 * Encapsulation for all modal popover based alert messages. These may contain HTML formatting.
 */
public abstract class RichAlertMessage
implements AlertMessage {

  /** Name of the title property in the JSON property object. */
  public static final String TITLE_JSON_PROPERTY_NAME = "title";

  private final String mTitle;
  //Can be null
  private final NotificationDisplayType mDisplayType;
  private final String mClosePrompt;
  private final String mCSSClass;

  protected RichAlertMessage(String pTitle, NotificationDisplayType pDisplayType, String pClosePrompt, String pCSSClass) {
    mTitle = pTitle;
    mDisplayType = pDisplayType;
    mClosePrompt = pClosePrompt;
    mCSSClass = pCSSClass;
  }

  /**
   * @return A JSON property object for this AlertMessage, for passing to the FOXAlert JavaScript.
   */
  public JSONObject getJSONPropertyObject() {
    JSONObject lProperties = new JSONObject();
    lProperties.put(TITLE_JSON_PROPERTY_NAME, mTitle);
    lProperties.put("alertStyle", mDisplayType == null ? "normal" : mDisplayType.toString().toLowerCase());
    lProperties.put("cssClass", mCSSClass);
    lProperties.put("closePrompt", mClosePrompt);

    return lProperties;
  }
}

package net.foxopen.fox.module.fieldset;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpSession;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Object which manages the FieldSet cookie for a signle churn. <br><br>
 *
 * ****** FieldSet Cookie ****** <br><br>
 *
 * The FieldSet cookie is a JSON Array of JSON Objects in the following format: <br><br>
 *
 * <pre>[{t:"abc", f:"123"}, {t:"def", f:"456"}, ...]</pre><br>
 *
 * Where "t" is the thread ID and "f" is the current field set ID for that thread. The lowest-indexed item in
 * the array is the least recently used (i.e. churned) thread. If the array becomes too large to store in a cookie
 * (i.e. over 4K), least recently used threads can be popped off the start of the array.<br><br>
 *
 * The cookie value is stored on the servlet's session. This mitigates an issue whereby if two screens are churned
 * at the same time, the cookie returned by churn 2 may override that from churn 1, resulting in churn 1 'losing' its
 * correct fieldset value. It is not sufficient to store the value on the FoxSession as the user may have multiple
 * FoxSession for a single browser session.<br><br>
 */
public class FieldSetCookieManager
implements FoxResponse.BeforeResponseAction {

  private static final String FIELD_SET_COOKIE_NAME = "field_set";
  private static final String FIELD_SET_HTTP_SESSION_ATTR_NAME = "net.foxopen.fox.FoxRequestHttp.FieldSet";
  private static final int FIELD_SET_COOKIE_MAX_LENGTH = 3500;

  private final FoxRequest mFoxRequest;
  private final String mThreadId;
  private final String mFieldSetLabel;

  /**
   * Creates a FieldSetCookieManager for a request which does not have access to a FieldSet. If a FieldSet is available,
   * use {@link FieldSet#createCookieManager(FoxRequest, String)}.
   * @param pFoxRequest Request being processed.
   * @param pThreadId ID of thread being processed.
   * @param pFieldSetLabel FieldSet label value to set on the cookie.
   * @return New FieldSetCookieManager for use with the given FoxRequest.
   */
  public static FieldSetCookieManager createForAdHocRequest(FoxRequest pFoxRequest, String pThreadId, String pFieldSetLabel) {
    return new FieldSetCookieManager(pFoxRequest, pThreadId, pFieldSetLabel);
  }

  FieldSetCookieManager(FoxRequest pFoxRequest, String pThreadId, String pFieldSetLabel) {
    mFoxRequest = pFoxRequest;
    mThreadId = pThreadId;
    mFieldSetLabel = pFieldSetLabel;
  }

  /**
   * Serialises the JSON array to a URI-encoded value suitable for setting as a cookie. If the serialised form exceeds
   * pMaxSerialisedLength, items are popped off the front of the array until the length requirement is satisfied.
   * @param pJSONArray The JSON Array to serialise. This object will be modified if length adjustments are required.
   * @param pMaxSerialisedLength Maximum length to allow for the serialised array.
   * @return URI encoded String of the serialised array.
   */
  static String serialiseJSONArrayToCookieString(JSONArray pJSONArray, int pMaxSerialisedLength){
    String lCookieString;
    //URL encode the value as it contains characters that are not valid for a cookie (namely double quotes)
    //This will also prevent Tomcat's cookie serialiser from interfering and adding extra quotes
    lCookieString = URLEncoder.encode(pJSONArray.toString());

    //Attempt to set the cookie without exceeding the maximum size
    while(lCookieString.length() >= pMaxSerialisedLength){
      //Pop the least recently used thread off the start of the array
      if(pJSONArray.size() > 1){
        pJSONArray.remove(0);
      }
      else {
        throw new ExInternal("JSON FieldSet cookie cannot serialise a single entry without exceeding maximum allowed length " +
                               "(allowed = " + pMaxSerialisedLength + ", actual = " + lCookieString.length() + ")");
      }
      lCookieString = URLEncoder.encode(pJSONArray.toString());
    }

    return lCookieString;
  }

  /**
   * Parses the given FoxRequest's FieldSet cookie into a JSONArray object.
   * @return The JSON Array stored in the field set cookie, or null if the cookie is not set.
   */
  private JSONArray getFieldSetJSONArray(){
    try {
      HttpSession lHttpSession = mFoxRequest.getHttpRequest().getSession();

      //Step 1: try reading the session attribute - this should usually be available.
      Object lFieldsetJSON = lHttpSession.getAttribute(FIELD_SET_HTTP_SESSION_ATTR_NAME);

      if(lFieldsetJSON != null){
        //Belt and braces type check
        if(!(lFieldsetJSON instanceof JSONArray)){
          throw new ExInternal("Session JSON attribute should be a JSONArray, was a " + lFieldsetJSON.getClass().getName());
        }

        return (JSONArray) lFieldsetJSON;
      }
      else {
        //Step 2: no session attribute - maybe the session dropped out of cache or a load balancer switched us to a new app server.
        //Parse the cookie that was sent into a JSONArray.
        String lJSONString = mFoxRequest.getCookieValue(FIELD_SET_COOKIE_NAME);
        if(!XFUtil.isNull(lJSONString)){
          return (JSONArray) JSONValue.parse(URLDecoder.decode(lJSONString));
        }
        else {
          //No session attribute and no cookie - return null to signal to the consumer it needs to bootstap one.
          return null;
        }
      }
    }
    catch (ClassCastException e) {
      throw new ExInternal("Unexpected JSON formatting when parsing field set cookie value", e);
    }
  }

  /**
   * Gets the index of the thread entry stored in the JSON array, or -1 if the thread reference is not defined in the array.
   * @param pJSONArray JSONArray to search.
   * @param pThreadId Thread ID to search for.
   * @return Index of the thread's corresponding JSON object, or -1 if it is not defined.
   */
  private int getJSONObjectIndex(JSONArray pJSONArray, String pThreadId){
    for(int i=0; i < pJSONArray.size(); i++){
      JSONObject lEntry = (JSONObject) pJSONArray.get(i);
      if(pThreadId.equals(lEntry.get("t"))){
        return i;
      }
    }
    return -1;
  }

  /**
   * Sets the JSON field set cookie on the given FoxRequest, so the UI JavaScript can see if the page is expired or not.
   */
  public void setCurrentFieldSet() {

    HttpSession lHttpSession = mFoxRequest.getHttpRequest().getSession();

    //Sync on HttpSession in case we are serving multiple requests at once
    synchronized(lHttpSession){

      //Attempt to parse the fieldset cookie
      JSONArray lCookieJSON = getFieldSetJSONArray();
      if(lCookieJSON != null){
        //Remove the existing value so it can be appended to the end (so the order of the JSON object acts like a stack)
        int lIdx = getJSONObjectIndex(lCookieJSON, mThreadId);
        if(lIdx != -1){
          lCookieJSON.remove(lIdx);
        }
      }
      else {
        //The browser didn't send a fieldset cookie and there wasn't a session attribute; bootstrap a new JSON Array
        lCookieJSON = new JSONArray();
      }
      //Construct the JSONObject entry for the cookie
      JSONObject lJSONEntry = new JSONObject();
      lJSONEntry.put("t", mThreadId);
      lJSONEntry.put("f", mFieldSetLabel);
      //Append to the end of the array
      lCookieJSON.add(lJSONEntry);

      //Serialise the modified array and set the cookie value
      String lCookieString = serialiseJSONArrayToCookieString(lCookieJSON, FIELD_SET_COOKIE_MAX_LENGTH);
      mFoxRequest.addCookie(FIELD_SET_COOKIE_NAME, lCookieString);
      //Set the session attribute so other requests can see it straight away
      lHttpSession.setAttribute(FIELD_SET_HTTP_SESSION_ATTR_NAME, lCookieJSON);
    }
  }

  /**
   * Removes the JSON field set cookie entry for the given thread. This should be used to clear up the cookie and prevent
   * it from overflowing with expired thread references.
   * @param pFoxRequest  FoxRequest to set cookie on.
   */
  public void clearCurrentFieldSet() {
    HttpSession lHttpSession = mFoxRequest.getHttpRequest().getSession();

    //Sync on HttpSession in case we are serving multiple requests at once
    synchronized(lHttpSession){
      //Parse the cookie into a JSON array
      JSONArray lCookieJSON = getFieldSetJSONArray();
      if(lCookieJSON != null){
        //Remove the existing value
        int lIdx = getJSONObjectIndex(lCookieJSON, mThreadId);
        if(lIdx != -1){
          lCookieJSON.remove(lIdx);
        }
        //Set the modified cookie value now the thread entry has been removed
        String lCookieString = serialiseJSONArrayToCookieString(lCookieJSON, FIELD_SET_COOKIE_MAX_LENGTH);
        mFoxRequest.addCookie(FIELD_SET_COOKIE_NAME, lCookieString);
        //Set the session attribute so other requests can see it straight away
        lHttpSession.setAttribute(FIELD_SET_HTTP_SESSION_ATTR_NAME, lCookieJSON);
      }
    }
  }

  @Override
  public void beforeResponse(FoxResponse pFoxResponse) {
    //Allows this object to be used as a BeforeResponseAction and set the cookie just before the response is sent.
    setCurrentFieldSet();
  }
}

/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox;

import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URLDecoder;
import java.net.URLEncoder;


public final class FoxRequestHttp extends FoxRequest {

  private static final String FIELD_SET_COOKIE_NAME = "field_set";
  private static final String FIELD_SET_HTTP_SESSION_ATTR_NAME = "net.foxopen.fox.FoxRequestHttp.FieldSet";
  private static final int FIELD_SET_COOKIE_MAX_LENGTH = 3500;

  private final HttpServletRequest mHttpRequest;
  private final HttpServletResponse mHttpResponse;
  private final String mRequestLogId;
  private final boolean mSecureCookies;

  public FoxRequestHttp(HttpServletRequest pHttpServletRequest, HttpServletResponse pHttpServletResponse) {
    super();
    mHttpRequest = pHttpServletRequest;
    mHttpResponse = pHttpServletResponse;
    mRequestLogId = "UNKNOWN";
    mSecureCookies = false;
  }

  public FoxRequestHttp(HttpServletRequest pHttpServletRequest, HttpServletResponse pHttpServletResponse, String pRequestLogId, boolean pSecureCookies) {
    super();
    mHttpRequest = pHttpServletRequest;
    mHttpResponse = pHttpServletResponse;
    mRequestLogId = pRequestLogId;
    mSecureCookies = pSecureCookies;
  }

  private String mRequestURI;
  public final String getRequestURI() {
    if(mRequestURI==null) {
      mRequestURI = getRequestURIStringBuffer().toString();
    }
    return mRequestURI;
  }

  @Deprecated
  public final StringBuffer getRequestURIStringBuffer() {
    StringBuffer lStringBuffer = new StringBuffer();
    XFUtil.pathPushTail(lStringBuffer, mHttpRequest.getPathInfo());
    return lStringBuffer;
  }

  @Override
  public final StringBuilder getRequestURIStringBuilder() {
    StringBuilder lStringBuilderURI = new StringBuilder();
    XFUtil.pathPushTail(lStringBuilderURI, mHttpRequest.getPathInfo());
    return lStringBuilderURI;
  }

  @Override
  public String getRequestLogId() {
    return mRequestLogId;
  }

  /**
   * setCookie
   * @param pName name of cookie
   * @param pValue value to set
   * @param pMaxAge max age in seconds
   * @param pSetPath set path?
   */
  private void addCookie (String pName, String pValue, Integer pMaxAge, boolean pSetPath, boolean pHttpOnly)
  {
    String cookieDomain = null;

    // get the request host - browser perspective rather than the machine name FOX derives
    String hostName = XFUtil.nvl(mHttpRequest.getHeader("host"),"");

    // strip off port (if any)
    hostName = hostName.substring(0, hostName.indexOf(":") > 0 ? hostName.indexOf(":") : hostName.length());

    // not an IP, process host name if at least one dot
    if (!XFUtil.isInet4IPAddress(hostName) && hostName.indexOf('.') > 0) {
      if ("LEGACY".equals(FoxGlobals.getInstance().getFoxEnvironment().getCookieDomainMethod())) {
        // split off what we assume to be a subdomain
        String split = hostName.substring(hostName.indexOf('.')+1);

        // if remaining dots, domain is type "subdomain.domain.com" -> set domain as "domain.com"
        if (split.indexOf('.') > 0) {
          cookieDomain = split;
        }
        // domain is type "domain.com" -> don't set domain
        else {
          cookieDomain = null;
        }
      }
      else {
        cookieDomain = hostName;
      }
    }

    // Sanitise name/value from CRLF injection
    pName = pName.replaceAll("(\r|\n)+", "");
    pValue = pValue.replaceAll("(\r|\n)+", "");

    Cookie cookie = new Cookie(pName, pValue);
    cookie.setHttpOnly(pHttpOnly);

    // Make cookies use secure flag if the app configured to send them
    cookie.setSecure(mSecureCookies);

    if (cookieDomain != null) {
      cookie.setDomain(cookieDomain);
    }
    if (pSetPath) {
      cookie.setPath(mHttpRequest.getContextPath());
    }
    else {
      // set to host root - forces browser to ignore (for example) /eng/fox/db/LOGIN001L/
      cookie.setPath("/");
    }

    if (!XFUtil.isNull(pMaxAge)) {
      cookie.setMaxAge(pMaxAge.intValue());
    }
    mHttpResponse.addCookie(cookie);
  }

  /*
   * Wrappers
   */
  public void addCookie (String pName, String pValue) {
    addCookie(pName, pValue, null, true, false);
  }

  public void addCookie (String pName, String pValue, int pMaxAge) {
    addCookie(pName, pValue, new Integer(pMaxAge), true);
  }

  public void addCookie (String pName, String pValue, boolean pSetPath) {
    addCookie(pName, pValue, null, pSetPath, false);
  }

  public void addCookie(String pName, String pValue, boolean pSetPath, boolean pHttpOnly) {
    addCookie(pName, pValue, null, pSetPath, pHttpOnly);
  }

  public void addCookie (String pName, String pValue, int pMaxAge, boolean pSetPath) {
    addCookie(pName, pValue, new Integer(pMaxAge), pSetPath, false);
  }

  public void addCookie(String pName, String pValue, int pMaxAge, boolean pSetPath, boolean pHttpOnly) {
    addCookie(pName, pValue, new Integer(pMaxAge), pSetPath, pHttpOnly);
  }

  public void removeCookie (String pName, boolean pSetPath) {
    addCookie(pName, "", 0, pSetPath);
  }

  public void removeCookie (String pName) {
    removeCookie(pName, false);
  }

  public String getCookieValue (String pCookieName) {
    return FoxRequestHttp.getCookieValue(getHttpRequest().getCookies(), pCookieName);
  }

  public static String getCookieValue (Cookie[] pCookieArray, String pCookieName){
    String lValue = null;
    if (pCookieArray != null) {
      SEEK_COOKIE: for(int i=0; i<pCookieArray.length; i++) {
        if(pCookieArray[i].getName().equals(pCookieName)) {
           lValue=pCookieArray[i].getValue();
           break SEEK_COOKIE;
        }
      }
    }
    return lValue;
  }

  /*
   * ****** FieldSet Cookie ******
   *
   * The FieldSet cookie is a JSON Array of JSON Objects in the following format:
   *
   * [{t:"abc", f:"123"}, {t:"def", f:"456"}, ...]
   *
   * Where "t" is the thread ID and "f" is the current field set ID for that thread. The lowest-indexed item in
   * the array is the least recently used (i.e. churned) thread. If the array becomes too large to store in a cookie
   * (i.e. over 4K), least recently used threads can be popped off the start of the array.
   *
   * The cookie value is stored on the servlet's session. This mitigates an issue whereby if two screens are churned
   * at the same time, the cookie returned by churn 2 may override that from churn 1, resulting in churn 1 'losing' its
   * correct fieldset value. It is not sufficient to store the value on the xfsession as the user may have multiple
   * xfsessions for a single browser session.
   *
   */

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
   * Parses this FoxRequest's FieldSet cookie into a JSONArray object.
   * @return The JSON Array stored in the field set cookie, or null if the cookie is not set.
   */
  private JSONArray getFieldSetJSONArray(){
    try {
      HttpSession lHttpSession = getHttpRequest().getSession();

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
        String lJSONString = getCookieValue(FIELD_SET_COOKIE_NAME);
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
   * Get the index of the thread entry stored in the JSON array, or -1 if the thread reference is not defined in the array.
   * @param pJSONArray JSONArray to search.
   * @param pThreadId Thread ID to search for.
   * @return Index of the thread's corresponding JSON object, or -1 if it is not defined.
   */
  static int getJSONObjectIndex(JSONArray pJSONArray, String pThreadId){
    for(int i=0; i < pJSONArray.size(); i++){
      JSONObject lEntry = (JSONObject) pJSONArray.get(i);
      if(pThreadId.equals(lEntry.get("t"))){
        return i;
      }
    }
    return -1;
  }

  /**
   * {@inheritDoc}
   */
  public void setCurrentFieldSet(String pThreadId, String pFieldSetLabel) {

    HttpSession lHttpSession = getHttpRequest().getSession();

    //Sync on HttpSession in case we are serving multiple requests at once
    synchronized(lHttpSession){

      //Attempt to parse the fieldset cookie
      JSONArray lCookieJSON = getFieldSetJSONArray();
      if(lCookieJSON != null){
        //Remove the existing value so it can be appended to the end (so the order of the JSON object acts like a stack)
        int lIdx = getJSONObjectIndex(lCookieJSON, pThreadId);
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
      lJSONEntry.put("t", pThreadId);
      lJSONEntry.put("f", pFieldSetLabel);
      //Append to the end of the array
      lCookieJSON.add(lJSONEntry);

      //Serialise the modified array and set the cookie value
      String lCookieString = serialiseJSONArrayToCookieString(lCookieJSON, FIELD_SET_COOKIE_MAX_LENGTH);
      addCookie(FIELD_SET_COOKIE_NAME, lCookieString);
      //Set the session attribute so other requests can see it straight away
      lHttpSession.setAttribute(FIELD_SET_HTTP_SESSION_ATTR_NAME, lCookieJSON);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void clearCurrentFieldSet(String pThreadId) {
    HttpSession lHttpSession = getHttpRequest().getSession();

    //Sync on HttpSession in case we are serving multiple requests at once
    synchronized(lHttpSession){
      //Parse the cookie into a JSON array
      JSONArray lCookieJSON = getFieldSetJSONArray();
      if(lCookieJSON != null){
        //Remove the existing value
        int lIdx = getJSONObjectIndex(lCookieJSON, pThreadId);
        if(lIdx != -1){
          lCookieJSON.remove(lIdx);
        }
        //Set the modified cookie value now the thread entry has been removed
        String lCookieString = serialiseJSONArrayToCookieString(lCookieJSON, FIELD_SET_COOKIE_MAX_LENGTH);
        addCookie(FIELD_SET_COOKIE_NAME, lCookieString);
        //Set the session attribute so other requests can see it straight away
        lHttpSession.setAttribute(FIELD_SET_HTTP_SESSION_ATTR_NAME, lCookieJSON);
      }
    }
  }

  public HttpServletRequest getHttpRequest () {
    return mHttpRequest;
  }

  public HttpServletResponse getHttpResponse () {
    return mHttpResponse;
  }

  @Override
  public String getParameter(String pParameterName) {
    return mHttpRequest.getParameter(pParameterName);
  }
}


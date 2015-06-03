package net.foxopen.fox.entrypoint.engine;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.database.sql.out.ResultSetAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.PathOrDOM;
import net.foxopen.fox.entrypoint.CookieBasedFoxSession;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.servlets.StaticServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.ws.EndPoint;
import net.foxopen.fox.entrypoint.ws.JSONWebServiceResponse;
import net.foxopen.fox.entrypoint.ws.PathParamTemplate;
import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceAuthDescriptor;
import net.foxopen.fox.entrypoint.ws.WebServiceResponse;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.JITMapSetInfo;
import net.foxopen.fox.module.mapset.AJAXQueryDefinition;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.serialiser.widgets.html.SearchSelectorWidgetBuilder;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RampedThreadRunnable;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.ThreadLockManager;
import net.foxopen.fox.track.Track;
import org.json.simple.JSONObject;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class MapSetWebService
implements WebService {
  // TODO - AJMS - Replace this with a proper FOXCache when implemented fully
//  public static Map<String, Object[]> gMapSetCache = new HashMap<>();

  private static final String WEB_SERVICE_NAME = "mapset";
  private static final String THREAD_PARAM = "thread";
  private static final String FIELD_PARAM = "elementId";
  private static final String SEARCH_PARAM = "search";

  @Override
  public String getName() {
    return WEB_SERVICE_NAME;
  }

  @Override
  public WebServiceAuthDescriptor getAuthDescriptor() {
    return WebServiceAuthDescriptor.NO_AUTHENTICATION_REQUIRED;
  }

  @Override
  public String getRequiredConnectionPoolName(FoxRequest pFoxRequest) {
    return null;
  }

  @Override
  public Collection<? extends EndPoint> getAllEndPoints() {
    return Collections.singleton(new AjaxSearchEndPoint());
  }

  public static class AjaxSearchEndPoint
  implements EndPoint {
    private static final String END_POINT_NAME = "query";
    private static final String THREAD_PARAM_TEMPLATE = "/{"+THREAD_PARAM+"}";

    @Override
    public String getName() {
      return END_POINT_NAME;
    }

    /**
     * The AJAX Search EndPoint ramps up a pre-existing thread and so requires a FOX Session so this method overrides
     * the default to provide a CookieBasedFoxSession
     *
     * @param pRequestContext Current RequestContext.
     * @return A FoxSession object
     */
    @Override
    public FoxSession establishFoxSession(RequestContext pRequestContext) {
      return CookieBasedFoxSession.getOrCreateFoxSession(pRequestContext);
    }

    @Override
    public PathParamTemplate getPathParamTemplate() {
      return new PathParamTemplate(THREAD_PARAM_TEMPLATE);
    }

    @Override
    public Collection<String> getMandatoryRequestParamNames() {
      return Collections.emptySet();
    }

    @Override
    public Collection<String> getAllowedHttpMethods() {
      return Collections.singleton("GET");
    }

    public static String buildEndPointURI(RequestURIBuilder pRequestURIBuilder, String pThreadID) {
      pRequestURIBuilder.setParam(THREAD_PARAM, pThreadID);
      return pRequestURIBuilder.buildWebServiceURI(EngineWebServiceCategory.CATEGORY_NAME, WEB_SERVICE_NAME, END_POINT_NAME, new PathParamTemplate(THREAD_PARAM_TEMPLATE));
    }

    @Override
    public WebServiceResponse respond(RequestContext pRequestContext, Map<String, String> pParamMap, String pHttpMethod, WebServiceResponse.Type pDesiredResponseType) {
      Track.pushDebug("MapSetWebServiceQuery");
      try {
        // Find query
        String lSearchTerm = pParamMap.get(SEARCH_PARAM);
        String lFieldID = pParamMap.get(FIELD_PARAM);
        String lThreadId = pParamMap.get(THREAD_PARAM);

        if (false /*gMapSetCache.containsKey(lFieldID)*/) {
          throw new ExInternal("AJAX MapSets currently can't run from cache, needs thread ramping.");
          // TODO - AJMS - Commented out code, an attempt at cached information from set-out being used so no thread ramping needed
//        AJAXQueryDefinition lMapSetDefinition;
//        BindObjectProvider lBinds;
//        Object[] lHorror = gMapSetCache.get(lFieldID);
//        lMapSetDefinition = (AJAXQueryDefinition) ((MapSet)lHorror[0]).getMapSetDefinition();
//        lBinds = (BindObjectProvider)lHorror[1];
//
//        JSONObject lJSONObject = new JSONObject();
//        UCon lUCon = pRequestContext.getContextUCon().getUCon("MapSetWebService");
//        try {
//          // Add on ref bind
//          DecoratingBindObjectProvider lSearchBindProvider = new DecoratingBindObjectProvider() {
//            @Override
//            protected BindObject getBindObjectOrNull(String pBindName, int pIndex) {
//              if (SEARCH_BIND.equals(pBindName)) {
//                return new StringBindObject(lSearchTerm);
//              }
//              return null;
//            }
//          };
//          lSearchBindProvider.decorate(lBinds);
//
//          ParsedStatement lParsedStatement = lMapSetDefinition.getSearchQueryStatement().getParsedStatement();
//          lParsedStatement.createExecutableQuery(lSearchBindProvider).executeAndDeliver(lUCon, new SearchResultDeliverer(lJSONObject, lMapSetDefinition.getRefPath()));
//
//          return new JSONWebServiceResponse(lJSONObject);
//        }
//        catch (Throwable th) {
//          throw new ExInternal("Failed mapset stuff", th);
//        }
//        finally {
//          pRequestContext.getContextUCon().returnUCon(lUCon, "MapSetWebService");
//        }
        }
        else {
          // If mapset information wasn't found in the cache we need to ramp the thread and get the information from the fieldset

          ThreadLockManager<JSONObject> lLockManager = new ThreadLockManager<>(lThreadId, false);
          JSONObject lSearchResult = lLockManager.lockAndPerformAction(pRequestContext, new ThreadLockManager.LockedThreadRunnable<JSONObject>() {
            @Override
            public JSONObject doWhenLocked(RequestContext pRequestContext, StatefulXThread pXThread) {
              Track.info("ThreadLocked");
              // Get information about the field from the outward fieldset on the thread
              FieldSet lOutwardFieldSet = pXThread.getFieldSetOut();
              JITMapSetInfo lJITMapSetInfo = lOutwardFieldSet.getJITMapSetInfo(lFieldID);

              if (lJITMapSetInfo == null) {
                throw new ExInternal("Cannot find a JITMapSetInfo associated with the field '" + lFieldID + "' in the outward fieldset for thread: " + lThreadId);
              }

              JSONObject lJSONObject = new JSONObject();

              // Get mapset and dom node for it
              pXThread.rampAndRun(pRequestContext, new RampedThreadRunnable() {
                @Override
                public void run(ActionRequestContext pRequestContext) throws ExUserRequest {
                  Track.info("RampedAndRunnable");
                  // Add on ref bind
                  DecoratingBindObjectProvider lSearchBindProvider = AJAXQueryDefinition.getSearchBindObjectProvider(lSearchTerm);

                  UCon lUCon = pRequestContext.getContextUCon().getUCon("MapSetWebService::query");
                  pRequestContext.getContextUElem().localise("MapSetWebService::query");
                  try {
                    AJAXQueryDefinition lMapSetDefinition = (AJAXQueryDefinition) pRequestContext.getCurrentModule().getMapSetDefinitionByName(lJITMapSetInfo.getJITMapSetName());
                    DOM lFieldDOM = pRequestContext.getContextUElem().getElemByRef(lJITMapSetInfo.getDOMRef());

                    // Set up item[rec] contexts
                    lMapSetDefinition.setupContextUElem(pRequestContext.getContextUElem(), lFieldDOM, new PathOrDOM(""));
                    String lBaseURL = StaticServlet.getURIWithAppMnem(pRequestContext.createURIBuilder(), pRequestContext.getRequestApp().getAppMnem());

                    SearchResultDeliverer lSearchResultDeliverer = new SearchResultDeliverer(lJSONObject, lMapSetDefinition, lBaseURL);
                    lMapSetDefinition.getSearchQueryStatement(pRequestContext).executeStatement(pRequestContext, lFieldDOM, lUCon, lSearchBindProvider, lSearchResultDeliverer);
                  }
                  catch (Throwable th) {
                    throw new ExInternal("Failed to find and execute the search query for mapset: " + lJITMapSetInfo.getJITMapSetName(), th);
                  }
                  finally {
                    pRequestContext.getContextUElem().delocalise("MapSetWebService::query");
                    pRequestContext.getContextUCon().returnUCon(lUCon, "MapSetWebService::query");
                  }
                }
              }, "MapSetWebService");

              return lJSONObject;
            }
          });

          return new JSONWebServiceResponse(lSearchResult);
        }
      }
      finally {
        Track.pop("MapSetWebServiceQuery");
      }
    }

    // Take a record based MapSet statement result and generate JSON for the search selector
    private class SearchResultDeliverer implements QueryResultDeliverer {
      private final JSONObject mJSONObject;
      private final int mSearchQueryResultLimit;
      private final String mRefPath;
      private final String mBaseURL;

      public SearchResultDeliverer(JSONObject pJSONObject, AJAXQueryDefinition pAJAXQueryDefinition, String pBaseURL) {
        mJSONObject = pJSONObject;
        mSearchQueryResultLimit = pAJAXQueryDefinition.getSearchQueryResultLimit();
        mRefPath = pAJAXQueryDefinition.getRefPath();
        mBaseURL = pBaseURL;
      }

      @Override
      public void deliver(ExecutableQuery pQuery) throws ExDB {
        ResultSet lResultSet = pQuery.getResultSet();
        ResultSetMetaData lMetaData = pQuery.getMetaData();
        ResultSetAdaptor lResultSetAdaptor = new ResultSetAdaptor(lResultSet);

        try {
          int lRefColumnIndex = 0, lRefColumnType = 0, lDataColumnIndex = 0, lDataColumnType = 0, lKeyColumnIndex = 0, lKeyColumnType = 0;

          //Validate the result set
          for(int i = 1; i <= lMetaData.getColumnCount(); i++) {
            if(MapSet.DATA_ELEMENT_NAME.equals(lMetaData.getColumnLabel(i))) {
              lDataColumnIndex = i;
              lDataColumnType = lMetaData.getColumnType(i);
            }
            else if(MapSet.KEY_ELEMENT_NAME.equals(lMetaData.getColumnLabel(i))) {
              lKeyColumnIndex = i;
              lKeyColumnType = lMetaData.getColumnType(i);
            }
            else if(AJAXQueryDefinition.getSearchQueryRefColumn().equals(lMetaData.getColumnLabel(i))) {
              lRefColumnIndex = i;
              lRefColumnType = lMetaData.getColumnType(i);
            }
          }

          if(lDataColumnIndex == 0 && lRefColumnIndex == 0) {
            throw new ExInternal("Missing '" + MapSet.DATA_ELEMENT_NAME + "' column and no '" + AJAXQueryDefinition.getSearchQueryRefColumn() + "' column either");
          }
          else if(lKeyColumnIndex == 0) {
            throw new ExInternal("Missing '" + MapSet.KEY_ELEMENT_NAME + "' column");
          }

          //Loop through each row and create a rec with corresponding key/data entries
          DOM lDataElem = DOM.createUnconnectedElement(MapSet.DATA_ELEMENT_NAME);
          int lRowNum = 1;
          ROW_LOOP:
          while (lResultSet.next()) {
            if (lRowNum > mSearchQueryResultLimit) {
              mJSONObject.put(SearchSelectorWidgetBuilder.LIMITED_JSON_PROPERTY, "true");
              break ROW_LOOP;
            }

            //Create a new rec
            JSONObject lRecItem = new JSONObject();

            //Add the key element as a string
            String lKey = SQLTypeConverter.getValueAsString(lResultSetAdaptor, lKeyColumnIndex, lKeyColumnType);
            if(XFUtil.isNull(lKey)) {
              throw new ExInternal("'" + MapSet.KEY_ELEMENT_NAME + "' column cannot be null on row " + lRowNum);
            }
            lRecItem.put(SearchSelectorWidgetBuilder.KEY_JSON_PROPERTY, lKey);

            String lRef;
            if (lRefColumnIndex != 0) {
              // If there's a reference column, shortcut to get the data from that
              lRef = SQLTypeConverter.getValueAsString(lResultSetAdaptor, lRefColumnIndex, lRefColumnType);
            }
            else if (lDataColumnIndex != 0) {
              // If there was no reference column, but there was a data column, get that and get the reference from it
              if (XFUtil.isNull(lResultSet.getObject(MapSet.DATA_ELEMENT_NAME))) {
                throw new ExInternal("'data' column cannot be null on row " + lRowNum);
              }
              else {
                if (XFUtil.isNull(mRefPath)) {
                  lRef = SQLTypeConverter.getValueAsString(lResultSetAdaptor, lDataColumnIndex, lDataColumnType);
                }
                else {
                  lDataElem.removeAllChildren();
                  SQLTypeConverter.applyValueToDOM(lResultSetAdaptor, lDataColumnIndex, lDataColumnType, lDataElem);
                  try {
                    lRef = lDataElem.get1S(mRefPath);
                  }
                  catch (Exception e) {
                    throw new ExInternal("Failed to find ref in data given ref path", e);
                  }
                }
              }
            }
            else {
              throw new ExInternal("Could not find a reference for row '" + lKey + "' from either a reference column or data column");
            }
            lRecItem.put("id", lRef);

            //For all additional columns, bring the value into the DOM in the most type-appropriate way (i.e. SQLXML as a DOM, dates in xs:date format, varchars as a string, etc)
            for(int i = 1; i <= lMetaData.getColumnCount(); i++) {
              if(i != lDataColumnIndex && i != lKeyColumnIndex) {
                DOM lColTarget = DOM.createUnconnectedElement(lMetaData.getColumnName(i));
                SQLTypeConverter.applyValueToDOM(lResultSetAdaptor, i, lMetaData.getColumnType(i), lColTarget);
                String lColumnName = lMetaData.getColumnName(i);
                if (SearchSelectorWidgetBuilder.SUGGESTION_DISPLAY_MS_PROPERTY.equals(lMetaData.getColumnName(i))) {
                  lRecItem.put(SearchSelectorWidgetBuilder.SUGGESTION_DISPLAY_JSON_PROPERTY, lColTarget.outputNodeContentsToString(false, false).replace("%IMAGE_BASE%", mBaseURL));
                }
                else {
                  lRecItem.put(lColumnName, lColTarget.outputNodeContentsToString(false, false));
                }
              }
            }

            lRecItem.put(SearchSelectorWidgetBuilder.SUGGESTABLE_JSON_PROPERTY, "true");
            lRecItem.put(SearchSelectorWidgetBuilder.SORT_JSON_PROPERTY, lRowNum);

            mJSONObject.put(lRef, lRecItem);

            lRowNum++;
          } // end ROW_LOOP
        }
        catch (SQLException e) {
          throw new ExInternal("Failed to run record query for mapset", e);
        }
      }

      @Override
      public boolean closeStatementAfterDelivery() {
        return true;
      }
    }
  }

}

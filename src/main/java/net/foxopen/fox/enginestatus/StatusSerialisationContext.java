package net.foxopen.fox.enginestatus;

import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilderImpl;
import net.foxopen.fox.ex.ExInternal;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Contextual information required by StatusItem serialisation.
 */
public class StatusSerialisationContext {

  private final HttpServletRequest mRequest;
  private String mCurrentCategory;
  private final Deque<String> mMnemStack = new ArrayDeque<>();

  public StatusSerialisationContext(HttpServletRequest pRequest) {
    mRequest = pRequest;
  }

  public RequestURIBuilder getURIBuilder() {
    return RequestURIBuilderImpl.createFromHttpRequest(mRequest);
  }

  public void pushNestedStatus(String pStatusMnem) {
    mMnemStack.addFirst(pStatusMnem);
  }

  public void popNestedStatus(String pStatusMnem) {
    String lPopped = mMnemStack.removeFirst();
    if(!pStatusMnem.equals(lPopped)) {
      throw new ExInternal("Expected to pop " + pStatusMnem + " but instead popped " + lPopped);
    }
  }

  public void setCurrentCategory(String pCurrentCategory) {
    mCurrentCategory = pCurrentCategory;
  }

  public String getDetailURI(StatusDetail pStatusDetail) {

    StringBuilder lDetailPath  = new StringBuilder();
    Iterator<String> lIterator = mMnemStack.descendingIterator();
    while(lIterator.hasNext()) {
      lDetailPath.append("/");
      lDetailPath.append(lIterator.next());
    }
    lDetailPath.append("/");
    lDetailPath.append(pStatusDetail.getMnem());

    RequestURIBuilder lURIBuilder = RequestURIBuilderImpl.createFromHttpRequest(mRequest);
    lURIBuilder.setParam(StatusBangHandler.DETAIL_PATH_PARAM_NAME, lDetailPath.toString());
    lURIBuilder.setParam(StatusBangHandler.CATEGORY_PARAM_NAME, mCurrentCategory);
    //return lURIBuilder.buildServletURI("ws/rest/status/" + mCurrentCategory +  "/detail");
    return lURIBuilder.buildBangHandlerURI(StatusBangHandler.instance());
  }
}

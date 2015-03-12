package net.foxopen.fox.module.fieldset.action;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collections;
import java.util.Map;


public class PageControlAction implements InternalAction {

  public static final String PAGE_NUM_PARAM = "page";

  private final String mPagerKey;

  public PageControlAction(String pPagerKey) {
    mPagerKey = pPagerKey;
  }

  public static Map<String, String> getParamMapForPageNumber(int pPageNumber) {
    return Collections.singletonMap(PAGE_NUM_PARAM, Integer.toString(pPageNumber));
  }

  @Override
  public void run(ActionRequestContext pRequestContext, Map<String, String> pParams) throws ExUserRequest {

    String lPageNum = pParams.get(PAGE_NUM_PARAM);
    if(XFUtil.isNull(lPageNum)) {
      throw new ExUserRequest("Page control action missing mandatory " + PAGE_NUM_PARAM + " parameter");
    }

    int lPageNumInt;
    try {
      lPageNumInt = Integer.parseInt(lPageNum);
    }
    catch (NumberFormatException e) {
      throw new ExUserRequest("Page control action page number is not a valid integer", e);
    }

    pRequestContext.getModuleFacetProvider(PagerProvider.class).getPagerByKey(mPagerKey).goToPage(pRequestContext, lPageNumInt);
  }
}

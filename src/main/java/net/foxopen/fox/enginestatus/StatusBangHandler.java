package net.foxopen.fox.enginestatus;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.FoxResponseNoOp;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.ex.ExInternal;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;

public class StatusBangHandler
implements BangHandler {

  public static final String DETAIL_PATH_PARAM_NAME = "detailPath";
  public static final String CATEGORY_PARAM_NAME = "category";

  private static final StatusBangHandler INSTANCE = new StatusBangHandler();
  public static StatusBangHandler instance() {
    return INSTANCE;
  }

  private StatusBangHandler() { }

  @Override
  public String getAlias() {
    return "STATUS";
  }

  @Override
  public Collection<String> getParamList() {
    return Collections.emptySet();
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_ADMIN;
  }

  @Override
  public boolean isDevAccessAllowed() {
    return false;
  }

  @Override
  public FoxResponse respond(FoxRequest pFoxRequest) {

    String lDetailPath = pFoxRequest.getParameter(DETAIL_PATH_PARAM_NAME);
    String lCategory = pFoxRequest.getParameter(CATEGORY_PARAM_NAME);

    //If we have a category param we need the HTML for either the whole category or a detail item
    if(!XFUtil.isNull(lCategory)) {
      StringWriter lStringWriter = new StringWriter();

      if(!XFUtil.isNull(lDetailPath)) {
        //Resolve the given detail path and return its contents
        StatusDetail lStatusDetail = EngineStatus.instance().getCategory(lCategory).resolveDetail(lDetailPath);
        lStatusDetail.getContent(pFoxRequest, lStringWriter);
      }
      else {
        //Refresh the whole category
        try {
          EngineStatus.instance().refreshCategory(lCategory);
          EngineStatus.instance().getCategory(lCategory).serialiseHTML(lStringWriter, new StatusSerialisationContext(pFoxRequest.getHttpRequest()));
        }
        catch (IOException e) {
          throw new ExInternal("Error getting category HTML", e);
        }
      }

      return new FoxResponseCHAR("text/html", lStringWriter.getBuffer(), 0L);
    }
    else {
      //No params - forward to the JSP
      EngineStatus.instance().getHTMLResponse(pFoxRequest);
    }

    return new FoxResponseNoOp();
  }
}

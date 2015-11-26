package net.foxopen.fox.ex;

import net.foxopen.fox.StringUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;


public class ExActionFailed extends ExGeneral{
  static String TYPE = "Action Failed";
  private String mCode;
  public ExActionFailed(String pCode, String msg)
  throws ExInternal
  {
    super(msg, TYPE, null, null);
    setCode(pCode);
  }
  public ExActionFailed(String pCode, String msg, DOM xml)
  throws ExInternal
  {
    super(msg, TYPE, xml, null);
    setCode(pCode);
  }
  public ExActionFailed(String pCode, String msg, Throwable e)
  throws ExInternal
  {
    super(msg, TYPE, null, e);
    setCode(pCode);
  }
  public ExActionFailed(String pCode, String msg, DOM xml, Throwable e)
  throws ExInternal
  {
    super(msg, TYPE, xml, e);
    setCode(pCode);
  }
  public ExActionFailed(String pCode, String msg, String type, DOM xml, Throwable exception)
  throws ExInternal
  {
    super(msg,type,xml,exception);
    setCode(pCode);
  }

  public final String getCode()
  {
    return mCode;
  }
  
  public final void setCode(String pCode)
  throws ExInternal
  {
    if(StringUtil.translate(
    pCode.toUpperCase()
    , "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
    , "                                      #"
    )
    .trim().length()!=0) {
      throw new ExInternal("ExActionFailed code must be in format [A-Z0-9-_] only, got: '" + pCode + "'", this);
    }
    mCode=pCode.toUpperCase();
  }
  
}
 

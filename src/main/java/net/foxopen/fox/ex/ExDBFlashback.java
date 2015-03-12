package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

/**
 * Exceptions caused by Oracle flashback functionality.
 */
public class ExDBFlashback extends ExDB {

  public ExDBFlashback(String pMsg) {
    super(pMsg);
  }

  public ExDBFlashback(String pMsg, String pType, DOM pXml, Throwable pException) {
    super(pMsg, pType, pXml, pException);
  }

  public ExDBFlashback(String pMsg, Throwable e) {
    super(pMsg, e);
  }

  public ExDBFlashback(String pMsg, DOM pXml) {
    super(pMsg, pXml);
  }

  public ExDBFlashback(String pMsg, DOM pXml, Throwable e) {
    super(pMsg, pXml, e);
  }
}

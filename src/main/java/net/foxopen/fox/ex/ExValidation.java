package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

/**
 * Validation exception, must be declared as thrown or caught. Should be
 * interpreted by caller of any validation code and passed on wrapped with
 * a usage-specific exception.
 */
public class ExValidation extends ExGeneral 
{
  static String TYPE = "XML Validation Error";
  public ExValidation(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExValidation(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExValidation(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExValidation(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExValidation(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}

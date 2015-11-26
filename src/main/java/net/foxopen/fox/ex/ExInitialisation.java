package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

/**
* An exception that occurs when initialisation of a DOM node
* fails.
*/
public class ExInitialisation extends ExGeneral
{
  static String TYPE = "Node Initialisation Error";
  public ExInitialisation(String msg)
  {
    super(msg, TYPE, null, null);
  }
  public ExInitialisation(String msg, DOM xml)
  {
    super(msg, TYPE, xml, null);
  }
  public ExInitialisation(String msg, Throwable e)
  {
    super(msg, TYPE, null, e);
  }
  public ExInitialisation(String msg, DOM xml, Throwable e)
  {
    super(msg, TYPE, xml, e);
  }
  public ExInitialisation(String msg, String type, DOM xml, Throwable exception)
  {
    super(msg,type,xml,exception);
  }
}

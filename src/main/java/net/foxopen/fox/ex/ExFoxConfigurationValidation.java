package net.foxopen.fox.ex;

import net.foxopen.fox.dom.DOM;

public class ExFoxConfigurationValidation extends ExFoxConfiguration {
  public ExFoxConfigurationValidation(String pString, String pString1, DOM pDom, Throwable pThrowable) {
    super(pString, pString1, pDom, pThrowable);
  }

  public ExFoxConfigurationValidation(String pString, DOM pDom, Throwable pThrowable) {
    super(pString, pDom, pThrowable);
  }

  public ExFoxConfigurationValidation(String pString, Throwable pThrowable) {
    super(pString, pThrowable);
  }

  public ExFoxConfigurationValidation(String pString, DOM pDom) {
    super(pString, pDom);
  }

  public ExFoxConfigurationValidation(String pString) {
    super(pString);
  }
}

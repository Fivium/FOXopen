package net.foxopen.fox.thread.persistence.xstream;


import com.thoughtworks.xstream.converters.SingleValueConverter;

import net.foxopen.fox.dom.DOM;

class DOMXStreamConverter
implements SingleValueConverter {

  @Override
  public boolean canConvert(Class pType) {
    return pType == DOM.class;
  }

  @Override
  public Object fromString(String pStr) {
    return DOM.createDocumentFromXMLString(pStr);
  }

  @Override
  public String toString(Object pObj) {
    DOM lDOM = (DOM) pObj;
    return lDOM.outputNodeToString(false);
  }
}

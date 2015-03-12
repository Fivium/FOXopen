package net.foxopen.fox.thread.persistence.xstream;


import com.thoughtworks.xstream.XStream;

import java.io.StringWriter;

import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.fieldset.FieldSet;


public class XStreamManager {

  private static final XStream XSTREAM_INSTANCE = new XStream();
  static {
    XSTREAM_INSTANCE.registerConverter(new DOMXStreamConverter());
    XSTREAM_INSTANCE.registerConverter(new StandardAuthenticationContextConverter());
    XSTREAM_INSTANCE.registerConverter(new DefaultSecurityScopeConverter());
    XSTREAM_INSTANCE.alias("DefaultSecurityScope", SecurityScope.defaultInstance().getClass());
    XSTREAM_INSTANCE.alias("DOM", DOM.class);
    XSTREAM_INSTANCE.alias("FieldSet", FieldSet.class);
  }

  public static XStream getXStream() {
    return XSTREAM_INSTANCE;
  }

  public static String serialiseObjectToXMLString(Object pObject) {
    StringWriter lWriter = new StringWriter();
    getXStream().toXML(pObject, lWriter);
    return lWriter.toString();
  }

  public static DOM serialiseObjectToDOM(Object pObject) {
    StringWriter lWriter = new StringWriter();
    getXStream().toXML(pObject, lWriter);
    return DOM.createDocumentFromXMLString(lWriter.toString());
  }


  private XStreamManager() {
  }
}

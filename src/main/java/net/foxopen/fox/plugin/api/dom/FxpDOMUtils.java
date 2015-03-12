package net.foxopen.fox.plugin.api.dom;

import java.io.InputStream;
import java.io.Reader;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.xpath.saxon.DefaultNamespaceContext;
import net.foxopen.fox.ex.ExInternal;


public class FxpDOMUtils {
  private FxpDOMUtils() {}

  public static FxpDOMList createFxpDOMList() {
    return new DOMList();
  }

  public static FxpDOMList createFxpDOMList(int pInitialCapacity) {
    return new DOMList(pInitialCapacity);
  }

  public static FxpDOM createDocument(String pRootElementName) {
    return DOM.createDocument(pRootElementName);
  }

  public static FxpDOM createDocument(Reader pReader) {
    return DOM.createDocument(pReader);
  }

  public static FxpDOM createDocument(InputStream pInputStream, boolean pNamespaceAware) {
    return DOM.createDocument(pInputStream, pNamespaceAware);
  }

  public static void stripElementContentWhiteSpace(FxpDOM pNode) {
    DOM.stripElementContentWhiteSpace((DOM) pNode);
  }

  public static FxpDOM createUnconnectedText(String pText) {
    return DOM.createUnconnectedText(pText);
  }

  public static FxpDOM createUnconnectedElement(String pName) {
    return DOM.createUnconnectedElement(pName);
  }

  public static FxpDOM createUnconnectedElement(String pName, FxpDOM pRelatedElement) {
    return DOM.createUnconnectedElement(pName, (DOM) pRelatedElement);
  }

  public static FxpDOM createUnconnectedElement(String pName, String pNamespaceURI) {
    return DOM.createUnconnectedElement(pName, pNamespaceURI);
  }

  public static FxpDOM createDocumentFromXMLString(String pSourceXML) {
    return DOM.createDocumentFromXMLString(pSourceXML);
  }

  public static enum BuiltInNamespace {
    FO;
  }

  public static String getNamespaceURI(BuiltInNamespace pForNamespace) {
    switch (pForNamespace) {
      case FO:
        return DefaultNamespaceContext.FO_URI;
      default:
        throw new ExInternal("Namespace not known: " + pForNamespace);
    }
  }
}

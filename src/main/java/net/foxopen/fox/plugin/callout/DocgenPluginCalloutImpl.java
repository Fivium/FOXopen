package net.foxopen.fox.plugin.callout;

import java.io.OutputStream;

import java.util.Map;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.plugin.api.callout.DocgenPluginCallout;
import net.foxopen.fox.plugin.api.database.FxpContextUCon;
import net.foxopen.fox.plugin.api.dom.FxpContextUElem;


//TODO PN this should probably be in its implementor's package, not this package
public class DocgenPluginCalloutImpl
implements DocgenPluginCallout {

  private final boolean mIsBangCommand;
  private final String mDocumentInstanceId;
  private final FxpContextUElem mContextUElem;
  private final FxpContextUCon mContextUCon;
  private final Map<String, String> mPropertyMap;
  private final OutputStream mDestinationOutputStream;

  public DocgenPluginCalloutImpl(String pDocumentInstanceId, ContextUCon pContextUCon) {
    mIsBangCommand = true;
    mDocumentInstanceId = pDocumentInstanceId;
    mContextUCon = pContextUCon;
    mContextUElem = null;
    mPropertyMap = null;
    mDestinationOutputStream = null;
  }

  public DocgenPluginCalloutImpl(FxpContextUElem pContextUElem, FxpContextUCon pContextUCon, Map<String, String> pPropertyMap, OutputStream pDestinationOutputStream) {
    mDocumentInstanceId = "";
    mIsBangCommand = false;
    mContextUElem = pContextUElem;
    mContextUCon = pContextUCon;
    mPropertyMap = pPropertyMap;
    mDestinationOutputStream = pDestinationOutputStream;
  }

  public FxpContextUElem getContextUElem() {
    return mContextUElem;
  }

  public FxpContextUCon getContextUCon() {
    return mContextUCon;
  }

  public Map<String, String> getPropertyMap() {
    return mPropertyMap;
  }

  public OutputStream getDestinationOutputStream() {
    return mDestinationOutputStream;
  }

  public boolean isBangCommand() {
    return mIsBangCommand;
  }

  public String getDocumentInstanceId() {
    return mDocumentInstanceId;
  }
}
package net.foxopen.fox.plugin.api.callout;

import java.io.OutputStream;

import java.util.Map;

import net.foxopen.fox.plugin.api.database.FxpContextUCon;
import net.foxopen.fox.plugin.api.dom.FxpContextUElem;


public interface DocgenPluginCallout
extends PluginCallout {

  FxpContextUElem getContextUElem();

  FxpContextUCon getContextUCon();

  Map<String, String> getPropertyMap();

  OutputStream getDestinationOutputStream();

  boolean isBangCommand();

  String getDocumentInstanceId();
}

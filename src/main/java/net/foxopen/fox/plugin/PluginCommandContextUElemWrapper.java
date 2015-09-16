package net.foxopen.fox.plugin;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.plugin.api.dom.FxpContextLabel;
import net.foxopen.fox.plugin.api.dom.FxpContextUElem;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Decorator for a ContextUElem which can be exposed by the plugin API. In particular, this allows the plugin API to use
 * the FxpContextLabel enum to refer to built in ContextLabels, preventing the ContextLabel enum being exposed.
 */
public class PluginCommandContextUElemWrapper
implements FxpContextUElem<DOM, DOMList> {

  //Initialise FxpLabel to internal ContextLabel map
  //Note the ContextLabel values can't be members of the FxpContextLabel enum because then they would be exposed through the Plugin API
  private static final Map<FxpContextLabel, ContextLabel> CONTEXT_LABEL_MAP;
  static {
    Map<FxpContextLabel, ContextLabel> lContextLabelMap = new EnumMap<>(FxpContextLabel.class);

    lContextLabelMap.put(FxpContextLabel.ROOT, ContextLabel.ROOT);
    lContextLabelMap.put(FxpContextLabel.THEME, ContextLabel.THEME);
    lContextLabelMap.put(FxpContextLabel.ENV, ContextLabel.ENV);
    lContextLabelMap.put(FxpContextLabel.PARAMS, ContextLabel.PARAMS);
    lContextLabelMap.put(FxpContextLabel.RETURN, ContextLabel.RETURN);
    lContextLabelMap.put(FxpContextLabel.RESULT, ContextLabel.RESULT);
    lContextLabelMap.put(FxpContextLabel.ERROR, ContextLabel.ERROR);
    lContextLabelMap.put(FxpContextLabel.SESSION, ContextLabel.SESSION);
    lContextLabelMap.put(FxpContextLabel.TEMP, ContextLabel.TEMP);
    lContextLabelMap.put(FxpContextLabel.USER, ContextLabel.USER);
    lContextLabelMap.put(FxpContextLabel.SYS, ContextLabel.SYS);
    lContextLabelMap.put(FxpContextLabel.SERVICE_HEADER, ContextLabel.SERVICE_HEADER);
    lContextLabelMap.put(FxpContextLabel.ACTION, ContextLabel.ACTION);
    lContextLabelMap.put(FxpContextLabel.ATTACH, ContextLabel.ATTACH);
    lContextLabelMap.put(FxpContextLabel.ITEM, ContextLabel.ITEM);
    lContextLabelMap.put(FxpContextLabel.ITEMREC, ContextLabel.ITEMREC);

    CONTEXT_LABEL_MAP = Collections.unmodifiableMap(lContextLabelMap);
  }

  private final ContextUElem mContextUElem;

  PluginCommandContextUElemWrapper(ContextUElem pContextUElem) {
    mContextUElem = pContextUElem;
  }

  @Override
  public DOM getUElemOrNull(String pLabel) {
    return mContextUElem.getUElemOrNull(pLabel);
  }

  @Override
  public DOM getUElem(String pLabel)
  throws ExInternal {
    return mContextUElem.getUElem(pLabel);
  }

  @Override
  public DOM getUElem(FxpContextLabel pLabel) {
    return mContextUElem.getUElem(CONTEXT_LABEL_MAP.get(pLabel));
  }

  @Override
  public void setUElem(FxpContextLabel pLabel, DOM pUElem) {
    mContextUElem.setUElem(CONTEXT_LABEL_MAP.get(pLabel), pUElem);
  }

  @Override
  public void setUElem(String pLabelName, DOM pUElem) {
    //For now assume localised to avoid exposing ContextualityLevel to the plugin API
    mContextUElem.setUElem(pLabelName, ContextualityLevel.LOCALISED, pUElem);
  }

  @Override
  public void removeUElem(String pLabel) {
    mContextUElem.removeUElem(pLabel);
  }

  @Override
  public DOM attachDOM() {
    return mContextUElem.attachDOM();
  }

  @Override
  public FxpContextUElem localise(String pPurpose) {
    mContextUElem.localise(pPurpose);
    return this;
  }

  @Override
  public FxpContextUElem delocalise(String pPurpose) {
    mContextUElem.delocalise(pPurpose);
    return this;
  }

  @Override
  public boolean isLocalised() {
    return mContextUElem.isLocalised();
  }

  @Override
  public String getLocalisedPurpose() {
    return mContextUElem.getLocalisedPurpose();
  }

  @Override
  public DOM extendedXPath1E(String pFoxExtendedXpath, boolean pCreateMissingNodesOption, String pDefaultContextLabelOptional)
  throws ExActionFailed, ExTooMany, ExTooFew {
    return mContextUElem.extendedXPath1E(pFoxExtendedXpath, pCreateMissingNodesOption, pDefaultContextLabelOptional);
  }

  @Override
  public DOM extendedXPath1E(String pFoxExtendedXpath)
  throws ExActionFailed, ExTooMany, ExTooFew {
    return mContextUElem.extendedXPath1E(pFoxExtendedXpath);
  }

  @Override
  public DOM extendedXPath1E(String pFoxExtendedXpath, boolean pCreateMissingNodesOption)
  throws ExActionFailed, ExTooMany, ExTooFew {
    return mContextUElem.extendedXPath1E(pFoxExtendedXpath, pCreateMissingNodesOption);
  }

  @Override
  public DOM extendedXPath1E(DOM pRelativeDOM, String pFoxExtendedXpath)
  throws ExActionFailed, ExTooMany, ExTooFew {
    return mContextUElem.extendedXPath1E(pRelativeDOM, pFoxExtendedXpath);
  }

  @Override
  public DOM extendedXPath1E(DOM pRelativeDOM, String pFoxExtendedXpath, boolean pCreateMissingNodesOption)
  throws ExActionFailed, ExTooMany, ExTooFew {
    return mContextUElem.extendedXPath1E(pRelativeDOM, pFoxExtendedXpath, pCreateMissingNodesOption);
  }

  @Override
  public String getAbsolutePathForCreateableXPath(DOM pRelativeDOM, String pFoxExtendedXpath)
  throws ExActionFailed, ExTooMany, ExDOMName {
    return mContextUElem.getAbsolutePathForCreateableXPath(pRelativeDOM, pFoxExtendedXpath);
  }

  @Override
  public DOMList extendedXPathUL(String pFoxExtendedXpath, String pDefaultContextLabelOptional)
  throws ExActionFailed {
    return mContextUElem.extendedXPathUL(pFoxExtendedXpath, pDefaultContextLabelOptional);
  }

  @Override
  public DOMList extendedXPathUL(DOM pRelativeDOM, String pFoxExtendedXpath)
  throws ExActionFailed {
    return mContextUElem.extendedXPathUL(pRelativeDOM, pFoxExtendedXpath);
  }

  @Override
  public boolean extendedXPathBoolean(DOM pRelativeDOM, String pFoxExtendedXpath)
  throws ExActionFailed {
    return mContextUElem.extendedXPathBoolean(pRelativeDOM, pFoxExtendedXpath);
  }

  @Override
  public String extendedXPathString(DOM pRelativeDOM, String pFoxExtendedXpath)
  throws ExActionFailed {
    return mContextUElem.extendedXPathString(pRelativeDOM, pFoxExtendedXpath);
  }

  @Override
  public boolean existsContext(String pLabel) {
    return mContextUElem.existsContext(pLabel);
  }

  @Override
  public String extendedStringOrXPathString(DOM pRelativeDOM, String pStringOrFoxExtendedXpath)
  throws ExActionFailed {
    return mContextUElem.extendedStringOrXPathString(pRelativeDOM, pStringOrFoxExtendedXpath);
  }

  @Override
  public DOM getCreateXPath1E(String pFoxExtendedXPath)
  throws ExActionFailed, ExTooMany {
    return mContextUElem.getCreateXPath1E(pFoxExtendedXPath);
  }

  @Override
  public DOM getCreateXPath1E(String pFoxExtendedXPath, String pDefaultContextLabel)
  throws ExActionFailed, ExTooMany {
    return mContextUElem.getCreateXPath1E(pFoxExtendedXPath, pDefaultContextLabel);
  }

  @Override
  public DOMList getCreateXPathUL(String pFoxExtendedXPath)
  throws ExActionFailed {
    return mContextUElem.getCreateXPathUL(pFoxExtendedXPath);
  }

  @Override
  public DOMList getCreateXPathUL(String pFoxExtendedXPath, String pDefaultContextLabelOptional)
  throws ExActionFailed {
    return mContextUElem.getCreateXPathUL(pFoxExtendedXPath, pDefaultContextLabelOptional);
  }

  @Override
  public DOM getElemByRef(String pRef) {
    return mContextUElem.getElemByRef(pRef);
  }

  @Override
  public DOM getElemByRefOrNull(String pRef) {
    return mContextUElem.getElemByRefOrNull(pRef);
  }

  @Override
  public boolean isLabelStillAttached(String pLabel) {
    return mContextUElem.isLabelStillAttached(pLabel);
  }
}

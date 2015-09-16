package net.foxopen.fox.plugin.api.dom;

/**
 * Built-in context labels which can be retrieved from an {@link FxpContextUElem}. Only non-document level ContextLabels
 * can be set.
 */
public enum FxpContextLabel {

  //Note: additions to this enum will need to be added to the PluginCommandContextUElemWrapper.CONTEXT_LABEL_MAP mapping

  ROOT,
  THEME,
  ENV,
  PARAMS,
  RETURN,
  RESULT,
  ERROR,
  SESSION,
  TEMP,
  USER,
  SYS,
  SERVICE_HEADER,
  ACTION,
  ATTACH,
  ITEM,
  ITEMREC;
}

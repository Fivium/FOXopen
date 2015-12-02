package net.foxopen.fox.module.serialiser.widgets.html;


import net.foxopen.fox.JSONNonEscapedValue;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.engine.MapSetWebService;
import net.foxopen.fox.entrypoint.servlets.StaticServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.fieldset.fvm.FieldValueMapping;
import net.foxopen.fox.module.mapset.JITMapSet;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.OptionWidgetUtils;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class SearchSelectorWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoItem> {
  private static final String SELECTOR_MUSTACHE_TEMPLATE = "html/SelectorWidget.mustache";

  public static final String HIDDEN_SEARCHABLE_MS_PROPERTY = "hidden-searchable";
  public static final String SUGGESTION_DISPLAY_MS_PROPERTY = "suggestion-display";
  public static final String LEVEL_MS_PROPERTY = "level";

  public static final String KEY_JSON_PROPERTY = "key";
  public static final String SORT_JSON_PROPERTY = "sort";
  public static final String SUGGESTION_DISPLAY_JSON_PROPERTY = "suggestion";
  public static final String HIDDEN_SEARCHABLE_JSON_PROPERTY = "hidden";
  public static final String LEVEL_JSON_PROPERTY = "level";
  public static final String SELECTED_JSON_PROPERTY = "selected";
  public static final String HISTORICAL_JSON_PROPERTY = "historical";
  public static final String SUGGESTABLE_JSON_PROPERTY = "suggestable";
  public static final String LIMITED_JSON_PROPERTY = "limited";
  public static final String FREE_TEXT_JSON_PROPERTY = "freetext";
  public static final String DISABLED_JSON_PROPERTY = "disabled";

  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoItem> INSTANCE = new SearchSelectorWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoItem> getInstance() {
    return INSTANCE;
  }

  private SearchSelectorWidgetBuilder () {
  }

  /**
   * Gets a list of FieldSelectOptions for the given ENI, filtered to remove any key-null entries which should not be
   * displayed by a search selector (unless it is a single selector with <tt>searchMandatorySelection</tt> set to true).
   * @param pEvalNode Current ENI.
   * @return List of FieldSelectOptions to be used by the current widget instance.
   */
  private static List<FieldSelectOption> filteredOptionList(EvaluatedNodeInfoItem pEvalNode) {
    //Filter key-null/key-missing entries, unless mandatory selection is enabled and the field is single select and not mandatory (i.e. a key-null exists)
    return OptionWidgetUtils.filteredSearchSelectorOptions(pEvalNode).collect(Collectors.toList());
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoItem pEvalNode) {

    FieldMgr lFieldMgr = pEvalNode.getFieldMgr();
    List<FieldSelectOption> lSelectOptions = filteredOptionList(pEvalNode);

    if (!pEvalNode.isPlusWidget() && lFieldMgr.getVisibility() == NodeVisibility.VIEW) {
      SelectorWidgetBuilder.outputReadOnlyOptions(pSerialiser, pEvalNode);
    }
    else {
      // Create a hidden select element similar to the selector widget
      buildHiddenSelectorWidget(pSerialisationContext, pSerialiser, pEvalNode, lSelectOptions);

      // Find image base URL (the static servlet path)
      RequestURIBuilder lURIBuilder = pSerialisationContext.createURIBuilder();

      // Mapset var name, could be cache key but then possible issues with itemrec
      String lMapsetJSONVariableName = lFieldMgr.getExternalFieldName() + pEvalNode.getStringAttribute(NodeAttribute.MAPSET, "missing-map-set").replaceAll("[^a-zA-Z0-9]*", "");
      //Static servlet requires app mnem appended
      String lJSON = mapsetToJSON(pEvalNode, StaticServlet.getURIWithAppMnem(lURIBuilder, pSerialisationContext.getApp().getAppMnem()));
      // Add the JSON to the page
      pSerialisationContext.addConditionalLoadJavascript("var " + lMapsetJSONVariableName + " = " + lJSON + ";");

      // Add a placeholder element
      String lLoadingElementName = "loading"+lFieldMgr.getExternalFieldName();
      pSerialiser.append("<input type=\"text\" id=\"");
      pSerialiser.append(lLoadingElementName);
      pSerialiser.append("\" class=\"tagger tagger-loading\" />");

      // Use the field width as the tagger width if tight field is specified, otherwise tagger should use 100% of the cell
      String lFieldWidth;
      if (pEvalNode.getBooleanAttribute(NodeAttribute.TIGHT_FIELD, false)) {
        lFieldWidth = "$('#" + lLoadingElementName + "').css('width')";
      }
      else {
        lFieldWidth = "'100%'";
      }

      // Config for the jqueryTagger widget
      JSONObject lTaggerConfig = new JSONObject();
      lTaggerConfig.put("baseURL", lURIBuilder.buildServletURI(StaticServlet.SERVLET_PATH));
      lTaggerConfig.put("imgDownArrow", "/img/tagger-dropdown.png");
      lTaggerConfig.put("imgRemove", "/img/tagger-remove.png");
      lTaggerConfig.put("imgSearch", "/img/tagger-search.png");
      lTaggerConfig.put("fieldWidth", lFieldWidth);

      // Add optinal values to pass to tagger
      lTaggerConfig.putAll(establishExtraParams(pEvalNode));

      // Variable is passed in as a raw variable name to be resolved at runtime
      lTaggerConfig.put("availableTags", new JSONNonEscapedValue(lMapsetJSONVariableName));

      // Pass through lSelectedIdList
      JSONArray lSelectedTags = new JSONArray();
      for (FieldSelectOption lOption : lSelectOptions) {
        if (lOption.isSelected()) {
          lSelectedTags.add(lOption.getExternalFieldValue());
        }
      }
      lTaggerConfig.put("preselectedTags", lSelectedTags);

      boolean lAJAXMapSet = pEvalNode.getMapSet() instanceof JITMapSet;
      if (lAJAXMapSet) {
        lTaggerConfig.put("ajaxURL", MapSetWebService.AjaxSearchEndPoint.buildEndPointURI(pSerialisationContext.createURIBuilder(), pSerialisationContext.getThreadInfoProvider().getThreadId()));
        lTaggerConfig.put("ajaxErrorFunction", new JSONNonEscapedValue("function(self, data){self._showMessageSuggestion('The application has experienced an unexpected error, please try again or contact support. Error reference: <strong>' + data.responseJSON.errorDetails.reference + '</strong>', 'error');}"));
      }

      // Add in the JS to construct the tagger for the select
      pSerialisationContext.addConditionalLoadJavascript("$(function(){\n" +
      "  $('#" + lFieldMgr.getExternalFieldName() + "').tagger(" + lTaggerConfig.toJSONString() + ");\n" +
      "});");
    }
  }

  private void buildHiddenSelectorWidget(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoItem pEvalNode, List<FieldSelectOption> pSelectOptions) {
    Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);

    // Force the style to be off screen
    lTemplateVars.put("Style", "position: absolute; left: -999em;");

    if(pEvalNode.getSelectorMaxCardinality() > 1) {
      lTemplateVars.put("Multiple", true );
    }

    // Always set size greater than 2, otherwise all fields post back with first value
    lTemplateVars.put("Size", Math.max(2, pSelectOptions.size()));

    List<Map<String, Object>> lOptions = new ArrayList<>();
    OPTION_LOOP:
    for (FieldSelectOption lOption : pSelectOptions) {
      if(lOption.isHistorical() && !lOption.isSelected()) {
        //Skip un-selected historical items
        continue OPTION_LOOP;
      }
      Map<String, Object> lOptionVars = new HashMap<>(3);
      lOptionVars.put("Key", lOption.getDisplayKey());
      lOptionVars.put("Value", lOption.getExternalFieldValue());
      if (lOption.isSelected()) {
        lOptionVars.put("Selected", true);
      }
      if (lOption.isDisabled()) {
        lOptionVars.put("Disabled", true);
      }
      lOptions.add(lOptionVars);
    }
    lTemplateVars.put("Options", lOptions);

    MustacheFragmentBuilder.applyMapToTemplate(SELECTOR_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
  }

  /**
   * Establish optional search tagger widget parameters from optional search selector attributes
   *
   * @param pEvalNode Evaluated Node to get attribute values from
   * @return JSONObject with extra tagger parameters
   */
  private JSONObject establishExtraParams(EvaluatedNodeInfo pEvalNode) {
    JSONObject lExtraParams = new JSONObject();
    try {
      String lFieldSuggestionWidth = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_SUGGESTION_WIDTH);
      if (lFieldSuggestionWidth != null) {
        lExtraParams.put("suggestWidth", Float.valueOf(lFieldSuggestionWidth) + "em");
      }

      String lFieldSuggestionMaxWidth = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_SUGGESTION_MAX_WIDTH);
      if (lFieldSuggestionMaxWidth != null) {
        lExtraParams.put("suggestMaxWidth", Float.valueOf(lFieldSuggestionMaxWidth) + "em");
      }

      String lFieldSuggestionMaxHeight = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_SUGGESTION_MAX_HEIGHT);
      if (lFieldSuggestionMaxHeight != null) {
        lExtraParams.put("suggestMaxHeight", Float.valueOf(lFieldSuggestionMaxHeight) + "em");
      }

      String lPlaceholder = pEvalNode.getStringAttribute(NodeAttribute.PLACEHOLDER);
      if (lPlaceholder != null) {
        lExtraParams.put("placeholder", lPlaceholder);
      }

      String lSearchCharacterThreshold = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_CHARACTER_THRESHOLD);
      if (lSearchCharacterThreshold != null) {
        lExtraParams.put("characterThreshold", Integer.valueOf(lSearchCharacterThreshold));
      }

      if (pEvalNode.getMaxDataLength() != Integer.MAX_VALUE) {
        lExtraParams.put("characterLimit", Integer.valueOf(pEvalNode.getMaxDataLength()));
      }

      String lIndentMultiplier = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_INDENT_MULTIPLIER);
      if (lIndentMultiplier != null) {
        lExtraParams.put("indentMultiplier", Float.valueOf(lIndentMultiplier));
      }

      String lSearchTypingTimeout = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_TYPING_TIMEOUT);
      if (lSearchTypingTimeout != null) {
        lExtraParams.put("typingTimeThreshold", Float.valueOf(lSearchTypingTimeout));
      }

      if (pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_DISPLAY_HIERARCHY, false)) {
        lExtraParams.put("displayHierarchy", true);
      }

      if (pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_CASE_SENSITIVE, false)) {
        lExtraParams.put("caseSensitive", true);
      }

      if (pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_CLEAR_FILTER_ON_BLUR, true)) {
        lExtraParams.put("clearFilterOnBlur", true);
      }
      else {
        lExtraParams.put("clearFilterOnBlur", false);
      }

      if (pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_SORTED_OUTPUT, true)) {
        lExtraParams.put("sortedOutput", true);
      }
      else {
        lExtraParams.put("sortedOutput", false);
      }

      if (pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_MANDATORY_SELECTION, false)) {
        lExtraParams.put("mandatorySelection", true);
      }

      if (pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_FREE_TEXT_INPUT, false)) {
        lExtraParams.put("freeTextInput", true);

        lExtraParams.put("freeTextPrefix", pEvalNode.getExternalFieldName() + "/" + FieldValueMapping.FREE_TEXT_PREFIX);

        StringAttributeResult lFreeTextMessage = pEvalNode.getStringAttributeResultOrNull(NodeAttribute.SEARCH_FREE_TEXT_INPUT_MESSAGE);
        if (lFreeTextMessage != null) {
          String lSafeMessage;
          if (lFreeTextMessage.isEscapingRequired()) {
            lSafeMessage = StringEscapeUtils.escapeHtml4(lFreeTextMessage.getString());
          }
          else {
            lSafeMessage = lFreeTextMessage.getString();
          }
          lExtraParams.put("freeTextMessage",  lSafeMessage);
        }

        lExtraParams.put("freeTextSuggest", pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_FREE_TEXT_SUGGEST, false));
      }

      String lNoSuggestionsText = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_NO_SUGGESTIONS_TEXT);
      if (lNoSuggestionsText != null) {
        lExtraParams.put("noSuggestText", lNoSuggestionsText);
      }

      String lEmptyListText = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_EMPTY_SUGGESTIONS_TEXT);
      if (lEmptyListText != null) {
        lExtraParams.put("emptyListText", lEmptyListText);
      }

      String lLimitedText = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_LIMITED_SUGGESTIONS_TEXT);
      if (lLimitedText != null) {
        lExtraParams.put("limitedText", lLimitedText);
      }
    }
    catch (NumberFormatException ex) {
      throw new ExInternal("Failed to convert XPath result to Float for search-selector attributes", ex);
    }

    return lExtraParams;
  }

  /**
   * Take in all the lists from the mapset and generate a JSON object array for each entry, to be used by the
   * search-selector widget
   *
   * @param pEvalNode Current ENI to get options from
   * @param pBaseURL Base URL of static servlet to replace %IMAGE_BASE% with in suggestion text
   * @return String of the JSON array to be set at the top of the page
   */
  private String mapsetToJSON(EvaluatedNodeInfoItem pEvalNode, String pBaseURL) {
    JSONArray lMapsetJSON = new JSONArray();
    JSONObject lMapsetObject = new JSONObject();
    JSONObject lJSONEntry;

    int i = 1;
    for (FieldSelectOption lItem : filteredOptionList(pEvalNode)) {
      FieldSelectOption lSearchableItem = lItem;

      String lHiddenSearchable = lItem.getAdditionalProperty(HIDDEN_SEARCHABLE_MS_PROPERTY);
      String lSuggestion = lItem.getAdditionalProperty(SUGGESTION_DISPLAY_MS_PROPERTY);
      String lLevel = lItem.getAdditionalProperty(LEVEL_MS_PROPERTY);

      if ((pEvalNode.getFieldMgr().getVisibility() == NodeVisibility.VIEW && !lItem.isSelected())) {
        // Skip putting it in the JSON if it's got null entries or in read only mode and not selected
        continue;
      }

      lJSONEntry = new JSONObject();

      lJSONEntry.put("id", lSearchableItem.getExternalFieldValue());
      lJSONEntry.put(KEY_JSON_PROPERTY, lSearchableItem.getDisplayKey());
      lJSONEntry.put(SORT_JSON_PROPERTY, i++);

      // Add suggestion text (if none in mapset the JS will use the key safely escaped)
      if (!XFUtil.isNull(lSuggestion)) {
        lJSONEntry.put(SUGGESTION_DISPLAY_JSON_PROPERTY, StringEscapeUtils.escapeHtml4(lSuggestion.replaceAll("%IMAGE_BASE%", pBaseURL)));
      }

      lJSONEntry.put(HIDDEN_SEARCHABLE_JSON_PROPERTY, XFUtil.nvl(lHiddenSearchable, ""));

      // Add entry for hierarchical data
      if (lLevel != null) {
        lJSONEntry.put(LEVEL_JSON_PROPERTY, lLevel);
      }

      // If it was selected add that entry
      if (lSearchableItem.isSelected()) {
        lJSONEntry.put(SELECTED_JSON_PROPERTY, true);
      }

      // If it's a historical entry add that entry
      if (lSearchableItem.isHistorical()) {
        lJSONEntry.put(HISTORICAL_JSON_PROPERTY, true);
        lJSONEntry.put(SUGGESTABLE_JSON_PROPERTY, false);
      }
      else {
        lJSONEntry.put(SUGGESTABLE_JSON_PROPERTY, true);
      }

      if (lSearchableItem.getAdditionalProperty(OptionFieldMgr.FREE_TEXT_ADDITIONAL_PROPERTY) != null) {
        lJSONEntry.put(FREE_TEXT_JSON_PROPERTY, true);
      }

      lJSONEntry.put(DISABLED_JSON_PROPERTY, lSearchableItem.isDisabled());

      lMapsetJSON.add(lJSONEntry);
      lMapsetObject.put(lSearchableItem.getExternalFieldValue(), lJSONEntry);
    }
    return lMapsetObject.toJSONString();
  }
}

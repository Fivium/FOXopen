package net.foxopen.fox.module.serialiser.widgets.html;


import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.engine.MapSetWebService;
import net.foxopen.fox.entrypoint.servlets.StaticServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.mapset.JITMapSet;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SearchSelectorWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfo> {

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

  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfo> INSTANCE = new SearchSelectorWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfo> getInstance() {
    return INSTANCE;
  }

  private SearchSelectorWidgetBuilder () {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    boolean lSearchMandatorySelection = pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_MANDATORY_SELECTION, false);

    FieldMgr lFieldMgr = pEvalNode.getFieldMgr();
    List<FieldSelectOption> lSelectOptions = lFieldMgr.getSelectOptions();

    boolean lAJAXMapSet = ((OptionFieldMgr)lFieldMgr).getEvaluatedNodeInfoItem().getMapSet() instanceof JITMapSet;

    if (!pEvalNode.isPlusWidget() && lFieldMgr.getVisibility() == NodeVisibility.VIEW) {
      SelectorWidgetBuilder.outputReadOnlyOptions(pSerialiser, lFieldMgr);
    }
    else {
      String lSelectAttributes = "";
      if (pEvalNode.getSelectorMaxCardinality() > 1) {
        lSelectAttributes += " multiple=\"multiple\"";
      }
      if (lFieldMgr.getVisibility() == NodeVisibility.VIEW) {
        lSelectAttributes += " readonly=\"readonly\"";
      }
      if (lFieldMgr.isRunnable()) {
        lSelectAttributes += " onchange=\"javascript:" + StringEscapeUtils.escapeHtml4(getActionSubmitString(pEvalNode)) + "\"";
      }
      pSerialiser.append("<select id=\"" + lFieldMgr.getExternalFieldName() + "\" name=\"" + lFieldMgr.getExternalFieldName() + "\"" + lSelectAttributes + " style=\"display:none;\" size=\"2\">");

      for (FieldSelectOption lOption : lSelectOptions) {
        if (lOption.isSelected()) {
          pSerialiser.append("  <option value=\"" + lOption.getExternalFieldValue() + "\" selected=\"selected\">" + StringEscapeUtils.escapeHtml4(lOption.getDisplayKey()) + "</option>");
        }
        else {
          pSerialiser.append("  <option value=\"" + lOption.getExternalFieldValue() + "\">" + StringEscapeUtils.escapeHtml4(lOption.getDisplayKey()) + "</option>");
        }
      }
      pSerialiser.append("</select>");

      // Find image base URL (the static servlet path)
      RequestURIBuilder lURIBuilder = pSerialisationContext.createURIBuilder();

      // Mapset var name, could be cache key but then possible issues with itemrec
      String lMapsetJSONVariableName = lFieldMgr.getExternalFieldName() + pEvalNode.getStringAttribute(NodeAttribute.MAPSET, "missing-map-set").replaceAll("[^a-zA-Z0-9]*", "");
      //Static servlet requires app mnem appended
      String lJSON = mapsetToJSON(lFieldMgr, StaticServlet.getURIWithAppMnem(lURIBuilder, pSerialisationContext.getApp().getAppMnem()));
      // Add the JSON to the page
      pSerialisationContext.addConditionalLoadJavascript("var " + lMapsetJSONVariableName + " = " + lJSON + ";");

      // Add a placeholder element
      String lLoadingElementName = "loading"+lFieldMgr.getExternalFieldName();
      pSerialiser.append("<input type=\"text\" id=\"");
      pSerialiser.append(lLoadingElementName);
      pSerialiser.append("\" class=\"tagger tagger-loading\" />");

      // Find DIA values to pass to tagger
      String lExtraParams = "";
      try {
        String lFieldSuggestionWidth = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_SUGGESTION_WIDTH);
        if (lFieldSuggestionWidth != null) {
          lExtraParams += "    suggestWidth: '" + Float.parseFloat(lFieldSuggestionWidth) + "em',\n";
        }
        String lFieldSuggestionMaxWidth = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_SUGGESTION_MAX_WIDTH);
        if (lFieldSuggestionMaxWidth != null) {
          lExtraParams += "    suggestMaxWidth: '" + Float.parseFloat(lFieldSuggestionMaxWidth) + "em',\n";
        }
        String lFieldSuggestionMaxHeight = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_SUGGESTION_MAX_HEIGHT);
        if (lFieldSuggestionMaxHeight != null) {
          lExtraParams += "    suggestMaxHeight: '" + Float.parseFloat(lFieldSuggestionMaxHeight) + "em',\n";
        }
        String lPlaceholder = pEvalNode.getStringAttribute(NodeAttribute.PLACEHOLDER);
        if (lPlaceholder != null) {
          lExtraParams += "    placeholder: '" + StringEscapeUtils.escapeHtml4(lPlaceholder) + "',\n";
        }
        String lSearchLimit = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_CHARACTER_THRESHOLD);
        if (lSearchLimit != null) {
          lExtraParams += "    characterThreshold: " + Float.parseFloat(lSearchLimit) + ",\n";
        }
        String lIndentMultiplier = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_INDENT_MULTIPLIER);
        if (lIndentMultiplier != null) {
          lExtraParams += "    indentMultiplier: " + Float.parseFloat(lIndentMultiplier) + ",\n";
        }
        String lSearchTypingTimeout = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_TYPING_TIMEOUT);
        if (lSearchTypingTimeout != null) {
          lExtraParams += "    typingTimeThreshold: " + Float.parseFloat(lSearchTypingTimeout) + ",\n";
        }
        if (pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_DISPLAY_HIERARCHY, false)) {
          lExtraParams += "    displayHierarchy: true,\n";
        }
        if (pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_CASE_SENSITIVE, false)) {
          lExtraParams += "    caseSensitive: true,\n";
        }
        if (pEvalNode.getBooleanAttribute(NodeAttribute.SEARCH_SORTED_OUTPUT, true)) {
          lExtraParams += "    sortedOutput: true,\n";
        }
        else {
          lExtraParams += "    sortedOutput: false,\n";
        }
        if (lSearchMandatorySelection) {
          lExtraParams += "    mandatorySelection: true,\n";
        }
        String lNoSuggestionsText = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_NO_SUGGESTIONS_TEXT);
        if (lNoSuggestionsText != null) {
          lExtraParams += "    noSuggestText: '" + StringEscapeUtils.escapeHtml4(lNoSuggestionsText) + "',\n";
        }
        String lEmptyListText = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_EMPTY_SUGGESTIONS_TEXT);
        if (lEmptyListText != null) {
          lExtraParams += "    emptyListText: '" + StringEscapeUtils.escapeHtml4(lNoSuggestionsText) + "',\n";
        }
        String lLimitedText = pEvalNode.getStringAttribute(NodeAttribute.SEARCH_LIMITED_SUGGESTIONS_TEXT);
        if (lLimitedText != null) {
          lExtraParams += "    limitedText: '" + StringEscapeUtils.escapeHtml4(lLimitedText) + "',\n";
        }

        if (lAJAXMapSet) {
          lExtraParams += "    ajaxURL: '" + MapSetWebService.AjaxSearchEndPoint.buildEndPointURI(pSerialisationContext.createURIBuilder(), pSerialisationContext.getThreadInfoProvider().getThreadId()) + "',\n"
          + "    ajaxErrorFunction: function(self, data){self._showMessageSuggestion('The application has experienced an unexpected error, please try again or contact support. Error reference: <strong>' + data.responseJSON.errorDetails.reference + '</strong>', 'error');},\n";
        }
      }
      catch (NumberFormatException ex) {
        throw new ExInternal("Failed to convert XPath result to Float for search-selector attributes", ex);
      }

      // Pass through lSelectedIdList

      Set<String> lSelectedValues = new HashSet<>();
      for (FieldSelectOption lOption : lSelectOptions) {
        if (lOption.isSelected()) {
          lSelectedValues.add("\"" + lOption.getExternalFieldValue() + "\"");
        }
      }
      String lPreselectedTags = "[" + Joiner.on(", ").join(lSelectedValues) + "]";

      String lBaseURL = lURIBuilder.buildServletURI(StaticServlet.SERVLET_PATH);

      // Use the field width as the tagger width if tight field is specified, otherwise tagger should use 100% of the cell
      String lFieldWidth;
      if (pEvalNode.getBooleanAttribute(NodeAttribute.TIGHT_FIELD, false)) {
        lFieldWidth = "$('#" + lLoadingElementName + "').css('width')";
      }
      else {
        lFieldWidth = "'100%'";
      }

      // Add in the JS to construct the tagger for the select
      pSerialisationContext.addConditionalLoadJavascript("$(function(){\n" +
      "  $('#" + lFieldMgr.getExternalFieldName() + "').tagger({\n" +
      "    availableTags: " + lMapsetJSONVariableName + ",\n" +
      "    preselectedTags: " + lPreselectedTags + ",\n" +
      lExtraParams +
      "    baseURL: '" + lBaseURL + "',\n" +
      "    imgDownArrow: '/img/tagger-dropdown.png',\n" +
      "    imgRemove: '/img/tagger-remove.png',\n" +
      "    imgSearch: '/img/tagger-search.png',\n" +
      "    fieldWidth: " + lFieldWidth + "\n" +
      "  });\n" +
      "});");
    }
  }

  /**
   * Take in all the lists from the mapset and generate a JSON object array for each entry, to be used by the
   * search-selector widget
   *
   * @param pFieldMgr FieldMgr to get options from
   * @param pBaseURL Base URL of static servlet to replace %IMAGE_BASE% with in suggestion text
   * @return String of the JSON array to be set at the top of the page
   */
  private String mapsetToJSON(FieldMgr pFieldMgr, String pBaseURL) {
    JSONArray lMapsetJSON = new JSONArray();
    JSONObject lMapsetObject = new JSONObject();
    JSONObject lJSONEntry;

    int i = 1;
    for (FieldSelectOption lItem : pFieldMgr.getSelectOptions()) {
      FieldSelectOption lSearchableItem = lItem;

      String lHiddenSearchable = lItem.getAdditionalProperty(HIDDEN_SEARCHABLE_MS_PROPERTY);
      String lSuggestion = lItem.getAdditionalProperty(SUGGESTION_DISPLAY_MS_PROPERTY);
      String lLevel = lItem.getAdditionalProperty(LEVEL_MS_PROPERTY);

      if ((pFieldMgr.getVisibility() == NodeVisibility.VIEW && !lItem.isSelected())) {
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
        lJSONEntry.put(SELECTED_JSON_PROPERTY, "true");
      }

      // If it's a historical entry add that entry
      if (lSearchableItem.isHistorical()) {
        lJSONEntry.put(HISTORICAL_JSON_PROPERTY, "true");
        lJSONEntry.put(SUGGESTABLE_JSON_PROPERTY, "false");
      }
      else {
        lJSONEntry.put(SUGGESTABLE_JSON_PROPERTY, "true");
      }

      lMapsetJSON.add(lJSONEntry);
      lMapsetObject.put(lSearchableItem.getExternalFieldValue(), lJSONEntry);
    }
    return lMapsetObject.toJSONString();
  }
}

package net.foxopen.fox.module.datanode;


import java.util.HashMap;
import java.util.Map;

/**
 * Should be ALL attributes allowed in a fox schema node. TODO add default value property
 */
public enum NodeAttribute {
  ACCESSIBLE_PROMPT("accessiblePrompt", ValueType.CONSTANT, ResultType.STRING),
  ACTION("action", ValueType.CONSTANT, ResultType.STRING),
  ACTION_CLASS("actionClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  ACTION_CONTEXT_DOM("action-context", ValueType.EVALUATABLE_XPATH, ResultType.DOM),
  ACTION_STYLE("actionStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  ACTION_TYPE("actionType", ValueType.CONSTANT, ResultType.STRING),
  AUTO_RESIZE("autoResize", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  AUTO_RESIZE_MAX_HEIGHT("autoResizeMaxHeight", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  CANVAS_CACHE_KEY("canvas-cache-key", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  CASE("case", ValueType.CONSTANT, ResultType.STRING),
  CC("cc", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  CELL_CLASS("cellClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  CELL_STYLE("cellStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  CELLMATE_CLASS("cellmateClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  CELLMATE_STYLE("cellmateStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  CELLMATE_KEY("cellmateKey", ValueType.CONSTANT, ResultType.STRING),
  CHANGE_ACTION("change-action", ValueType.CONSTANT, ResultType.STRING),
  CLASS("class", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  CLEAN("clean", ValueType.CONSTANT, ResultType.STRING),
  CLIENT_VISIBILITY_RULE("client-visibility-rule", ValueType.CONSTANT, ResultType.STRING),
  CLIENT_VISIBILITY_RULE_TARGET("client-visibility-rule-target", ValueType.CONSTANT, ResultType.STRING),
  CLIENT_VISIBILITY_RULE_VALUE("client-visibility-rule-value", ValueType.CONSTANT, ResultType.STRING),
  COLLAPSE_COLUMNS("collapseColumns", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  CONFIRM("confirm", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  DEFAULT_ACTION("defaultAction", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  DESCRIPTION("description", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  DESCRIPTION_BUFFER("description-buffer", ValueType.EVALUATABLE_XPATH, ResultType.BUFFER),
  DESCRIPTION_BUFFER_ATTACH_DOM("description-buffer-attach", ValueType.EVALUATABLE_XPATH, ResultType.DOM_OPTIONAL),
  DESCRIPTION_DISPLAY("descriptionDisplay", ValueType.CONSTANT, ResultType.STRING),
  DESCRIPTION_LAYOUT("descriptionLayout", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  DISPLAY_AFTER("displayAfter", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  DISPLAY_BEFORE("displayBefore", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  DISPLAY_ORDER("displayOrder", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  DOWNLOAD_AS_ATTACHMENT("downloadAsAttachment", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  DOWNLOAD_FILENAME("downloadFileName", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  EMAIL("email", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  ERROR_URL("error-url", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  ERROR_URL_PROMPT("error-url-prompt", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  EMPTY_LIST_BUFFER("emptyListBuffer", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  EMPTY_LIST_BUFFER_ATTACH_DOM("emptyListBufferAttach", ValueType.EVALUATABLE_XPATH, ResultType.DOM),
  ENABLE_FOCUS_HINT_DISPLAY("enableFocusHintDisplay", ValueType.CONSTANT, ResultType.BOOLEAN),
  ENABLE_LISTS_IN_FORMS("enableListsInForms", ValueType.CONSTANT, ResultType.BOOLEAN),
  EXTERNAL_URL("externalUrl", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_HEIGHT("fieldHeight", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_WIDTH("fieldWidth", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_SPAN("fieldSpan", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_CLASS("fieldClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_STYLE("fieldStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_CELL_CLASS("fieldCellClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_CELL_STYLE("fieldCellStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_MAX_WIDTH("fieldMaxWidth", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_MIN_WIDTH("fieldMinWidth", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_MAX_HEIGHT("fieldMaxHeight", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FIELD_MIN_HEIGHT("fieldMinHeight", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FILE_STORAGE_LOCATION("file-storage-location", ValueType.CONSTANT, ResultType.STRING),
  FLOW("flow", ValueType.CONSTANT, ResultType.STRING),
  FORM_COL_CHARS("formColChars", ValueType.CONSTANT, ResultType.STRING),
  FORM_MAX_COLUMNS("formMaxCols", ValueType.CONSTANT, ResultType.STRING),
  FORM_COLUMNS("formColumns", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FORM_CLASS("formClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FORM_STYLE("formStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FORM_CELL_CLASS("formCellClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FORM_CELL_STYLE("formCellStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FORM_TABLE_CLASS("formTableClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FORM_TABLE_STYLE("formTableStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  FORMAT_DATE("formatDate", ValueType.CONSTANT, ResultType.STRING),
  HAS_CONTENT("hasContent", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  HINT("hint", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  HINT_BUFFER("hint-buffer", ValueType.EVALUATABLE_XPATH, ResultType.BUFFER),
  HINT_BUFFER_ATTACH_DOM("hint-buffer-attach", ValueType.EVALUATABLE_XPATH, ResultType.DOM_OPTIONAL),
  HINT_URL("hint-url", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  HINT_TITLE("hintTitle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  HREF("href", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  HTML_WIDGET_CONFIG("htmlWidgetConfig", ValueType.CONSTANT, ResultType.STRING),
  IMAGE_DISPLAY_SIZE("image-display-size", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  IMAGE_DISPLAY_HEIGHT("image-display-height", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  IMAGE_DISPLAY_WIDTH("image-display-width", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  IMAGE_URL("imageUrl", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  INPUT_MASK("inputMask", ValueType.CONSTANT, ResultType.STRING),
  ITEMS_PER_ROW("itemsPerRow", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  KEY_CLASS("keyClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  KEY_FALSE("key-false", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  KEY_MISSING("key-missing", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  KEY_NULL("key-null", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  KEY_STYLE("keyStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  KEY_TRUE("key-true", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  KEY_UNRECOGNISED("key-unrecognised", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LAYOUT_METHOD("layoutMethod", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LINK_TITLE("linkTitle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LIST_CLASS("listClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LIST_STYLE("listStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LIST_CELL_CLASS("listCellClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LIST_CELL_STYLE("listCellStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LIST_ROW_CLASS("listRowClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LIST_ROW_STYLE("listRowStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LIST_TABLE_CLASS("listTableClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  LIST_TABLE_STYLE("listTableStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  MAND("mand", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  MANDATORY("mandatory", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  MANDATORY_DISPLAY("mandatoryDisplay", ValueType.CONSTANT, ResultType.STRING), // ENUM of MandatoryDisplayOption
  MANDATORY_TEXT("mandatoryText", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  MANDATORY_CLASS("mandatoryClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  MANDATORY_STYLE("mandatoryStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  MAPSET("map-set", ValueType.CONSTANT, ResultType.STRING),
  MAPSET_ATTACH("map-set-attach", ValueType.EVALUATABLE_XPATH, ResultType.DOM),
  MENU_CLASS("menuClass", ValueType.CONSTANT, ResultType.STRING),
  MENU_STYLE("menuStyle", ValueType.CONSTANT, ResultType.STRING),
  MENU_TABLE_CLASS("menuTableClass", ValueType.CONSTANT, ResultType.STRING),
  MENU_TABLE_STYLE("menuTableStyle", ValueType.CONSTANT, ResultType.STRING),
  NESTED_CLASS("nestedClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  NESTED_STYLE("nestedStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  NESTED_FORM_CLASS("nestedFormClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  NESTED_FORM_STYLE("nestedFormStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  NESTED_TABLE_CLASS("nestedTableClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  NESTED_TABLE_STYLE("nestedTableStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  OFFSET_SPAN("offsetSpan", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  OPTIONAL_TEXT("optionalText", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  PHANTOM_BUFFER("phantom-buffer", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  PHANTOM_BUFFER_ATTACH_DOM("phantom-buffer-attach", ValueType.EVALUATABLE_XPATH, ResultType.DOM_OPTIONAL),
  PHANTOM_DATA_XPATH("phantom-data-xpath", ValueType.EVALUATABLE_XPATH, ResultType.DOM_OPTIONAL),
  PHANTOM_MENU_MODE("phantom-menu-mode", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  PLACEHOLDER("placeholder", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  PROMPT("prompt", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  PROMPT_BUFFER("prompt-buffer", ValueType.EVALUATABLE_XPATH, ResultType.BUFFER),
  PROMPT_BUFFER_ATTACH_DOM("prompt-buffer-attach", ValueType.EVALUATABLE_XPATH, ResultType.DOM_OPTIONAL),
  PROMPT_SHORT("prompt-short", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  PROMPT_SHORT_BUFFER("prompt-short-buffer", ValueType.EVALUATABLE_XPATH, ResultType.BUFFER),
  PROMPT_SHORT_BUFFER_ATTACH_DOM("prompt-short-buffer-attach", ValueType.EVALUATABLE_XPATH, ResultType.DOM_OPTIONAL),
  PROMPT_SPAN("promptSpan", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  PROMPT_LAYOUT("promptLayout", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  PROMPT_WIDTH("promptWidth", ValueType.CONSTANT, ResultType.STRING),
  PROMPT_CLASS("promptClass", ValueType.CONSTANT, ResultType.STRING),
  PROMPT_STYLE("promptStyle", ValueType.CONSTANT, ResultType.STRING),
  RADIO_GROUP("radio-group", ValueType.CONSTANT, ResultType.STRING),
  RADIO_OWNER("radio-owner", ValueType.CONSTANT, ResultType.STRING),
  RESPONSIVE_LIST("responsiveList", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  REVERSE_BOOLEAN_ORDER("reverseBooleanOrder", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  ROW_BREAK("rowBreak", ValueType.CONSTANT, ResultType.STRING),
  ROW_BREAK_BEFORE("rowBreakBefore", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  ROW_BREAK_AFTER("rowBreakAfter", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  SEARCH_CASE_SENSITIVE("searchCaseSensitive", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  SEARCH_CHARACTER_THRESHOLD("searchCharacterThreshold", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SEARCH_DISPLAY_HIERARCHY("searchDisplayHierarchy", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  SEARCH_EMPTY_SUGGESTIONS_TEXT("searchEmptySuggestionsText", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SEARCH_INDENT_MULTIPLIER("searchIndentMultiplier", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SEARCH_LIMITED_SUGGESTIONS_TEXT("searchLimitedSuggestionsText", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SEARCH_MANDATORY_SELECTION("searchMandatorySelection", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  SEARCH_NO_SUGGESTIONS_TEXT("searchNoSuggestionsText", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SEARCH_SORTED_OUTPUT("searchSortedOutput", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  SEARCH_SUGGESTION_MAX_WIDTH("searchSuggestionMaxWidth", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SEARCH_SUGGESTION_MAX_HEIGHT("searchSuggestionMaxHeight", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SEARCH_SUGGESTION_WIDTH("searchSuggestionWidth", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SEARCH_TYPING_TIMEOUT("searchTypingTimeout", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SELECTOR("selector", ValueType.CONSTANT, ResultType.STRING),
  SPATIAL_DEFINITION("spatial-definition", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SPATIAL_INTERFACE("spatial-interface", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  STYLE("style", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SUBJECT("subject", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  SUPPRESS_UNSELECTED_OPTIONS("suppressUnselected", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  TIGHT_FIELD("tightField", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  TIGHT_WIDGETS("tightWidgets", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  TOP_LEVEL_FORM_CLASS("topLevelFormClass", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  TOP_LEVEL_FORM_STYLE("topLevelFormStyle", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  UPLOAD_CHOOSE_PROMPT("upload-choose-prompt", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  UPLOAD_COMPLETE_ACTION("upload-complete-action", ValueType.CONSTANT, ResultType.STRING),
  UPLOAD_FAIL_ACTION("upload-fail-action", ValueType.CONSTANT, ResultType.STRING),
  UPLOAD_FILE_TYPE("upload-file-type", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  UPLOAD_MAX_FILES("uploadMaxFiles", ValueType.EVALUATABLE_XPATH, ResultType.STRING),
  UPLOAD_WHOLE_PAGE_DROPZONE("uploadWholePageDropZone", ValueType.EVALUATABLE_XPATH, ResultType.BOOLEAN),
  UPLOAD_SUCCESS_ACTION("upload-success-action", ValueType.CONSTANT, ResultType.STRING),
  WIDGET("widget", ValueType.CONSTANT, ResultType.STRING);

  public enum ValueType {
    CONSTANT,
    EVALUATABLE_XPATH
  }

  public enum ResultType {
    STRING,
    BOOLEAN,
    DOM, // 1 element only
    DOM_OPTIONAL, // 0 or 1 elements
    DOM_LIST, // 0 or more elements
    BUFFER
  }

  private static Map<String, NodeAttribute> NODE_ATTRIBUTE_LOOKUP_MAP = new HashMap<>(NodeAttribute.values().length);
  static {
    for (NodeAttribute lNodeAttribute : NodeAttribute.values()) {
      NODE_ATTRIBUTE_LOOKUP_MAP.put(lNodeAttribute.mExternalString, lNodeAttribute);
    }
  }

  private final String mExternalString;
  private final ValueType mValueType;
  private final ResultType mResultType;

  NodeAttribute(String pExternalString, ValueType pValueType, ResultType pResultType) {
    mExternalString = pExternalString;
    mValueType = pValueType;
    mResultType = pResultType;
  }

  /**
   * Get a NodeAttribute for a given name, typically from an attribute key
   *
   * @param pNodeAttributeName possible string name of a NodeAttribute
   * @return NodeAttribute associated with pNodeAttributeName
   */
  public static NodeAttribute fromString(String pNodeAttributeName) {
    return NODE_ATTRIBUTE_LOOKUP_MAP.get(pNodeAttributeName);
  }

  public String getExternalString() {
    return mExternalString;
  }

  public boolean isEvaluatableXPath() {
    return mValueType == ValueType.EVALUATABLE_XPATH;
  }

  public ResultType getResultType() {
    return mResultType;
  }
}

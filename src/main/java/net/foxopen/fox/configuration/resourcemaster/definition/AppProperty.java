package net.foxopen.fox.configuration.resourcemaster.definition;


import net.foxopen.fox.module.fieldset.transformer.html.HTMLWidgetConfig;

public enum AppProperty {
  APP_ALIAS_LIST("/*/app-alias-list", true, false),
  APP_DISPLAY_ATTR_LIST ("/*/app-display-attribute-list", true, false),
  CONNECTION_POOL_NAME("/*/connection-pool-name", false, false),
  DEFAULT_DICTIONARY_LIST("/*/dictionary-properties/default-dictionary-list", true, false),
  DEFAULT_HTML_DOCTYPE("/*/default-html-doctype", false, false),
  DICTIONARY_LIST("/*/dictionary-properties/dictionary-list", true, false),
  RESOURCE_TABLE_LIST ("/*/resource-table-list", true),
  VIRUS_SCANNER_LIST("/*/virus-scanner-list", true),
  FILE_TRANSFER_QUEUE_LIST("/*/file-properties/file-transfer-queue-list", true),
  FILE_UPLOAD_TYPE_LIST("/*/file-properties/file-upload-type-list", true),
  IMAGE_PROCESSED_IMAGE_TABLE("/*/image-properties/processed-image-table", false, false),
  IMAGE_PROCESSED_IMAGE_URL("/*/image-properties/processed-image-url", false, false),
  IMAGE_PROCESSED_IMAGE_SERIES_LIST("/*/image-properties/processed-image-series-list", true, false),
  SECURITY_PRE_SESSION_TIMEOUT_PROMPT_SECS("/*/security-properties/pre-session-timeout-prompt-secs", false, false, 5000),
  SECURITY_SECURE_COOKIES("/*/security-properties/secure-cookies", false, false, false),
  SECURITY_EXTERNAL_ENTRY_THEME_SECURITY("/*/security-properties/external-entry-theme-security", false, false),
  MODULE_DEFAULT_MODULE("/*/module-properties/default-module", false),
  MODULE_TIMEOUT_MODULE("/*/module-properties/timeout-module", false),
  MODULE_SECURITY_CHECK_MODULE("/*/module-properties/security-check-module", false),
  ERROR_COMPONENT_NAME("/*/error-component", false),
  RESPONSE_METHOD("/*/response-method", false, false),
  HTML_WIDGET_CONFIG("/*/html-widget-config", false, false, HTMLWidgetConfig.STANDARD_NAME),

  // Application Environment Properties
  LOGOUT_PAGE("/*/logout-page", false, false),
  EXIT_PAGE("/*/exit-page", false, false),
  SPATIAL_RENDERER_LIST("/*/spatial-renderer-list", true, false);

  private final String mPath;
  private final boolean mIsXML;
  private final boolean mIsMandatory;
  private final Object mDefaultValue;

  AppProperty(String pName, boolean pIsXML) {
    this(pName, pIsXML, true, null);
  }

  AppProperty(String pName, boolean pIsXML, boolean pIsMandatory) {
    this(pName, pIsXML, pIsMandatory, null);
  }

  AppProperty(String pName, boolean pIsXML, boolean pIsMandatory, Object pDefaultValue) {
    mPath = pName;
    mIsXML = pIsXML;
    mIsMandatory = pIsMandatory;
    mDefaultValue = pDefaultValue;
  }

  public String getPath() {
    return mPath;
  }

  public boolean isXML() {
    return mIsXML;
  }

  public boolean isMandatory() {
    return mIsMandatory;
  }

  public Object getDefaultValue() {
    return mDefaultValue;
  }
}

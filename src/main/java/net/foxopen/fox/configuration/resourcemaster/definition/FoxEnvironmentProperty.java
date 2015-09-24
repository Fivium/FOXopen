package net.foxopen.fox.configuration.resourcemaster.definition;


import net.foxopen.fox.configuration.resourcemaster.model.DatabaseProperties;

public enum FoxEnvironmentProperty {
  DEFAULT_APPLICATION("/*/default-application", false),
  DATABASE_BINARY_XML_READER_STRATEGY("/*/database/binary-xml-reader-strategy", false, false, DatabaseProperties.XML_STRATEGY_BINARY),
  DATABASE_BINARY_XML_WRITER_STRATEGY("/*/database/binary-xml-writer-strategy", false, false, DatabaseProperties.XML_STRATEGY_STANDARD),
  DATABASE_STANDARD_XML_WRITER_METHOD("/*/database/standard-xml-writer-method", false, false, DatabaseProperties.STANDARD_XML_WRITER_METHOD_BYTES),
  ENV_DISPLAY_ATTR_LIST ("/*/env-display-attribute-list", true, false),
  AUTHENTICATION_PROPERTIES("/*/authentication-properties", true, false),
  COOKIE_DOMAIN_METHOD("/*/cookie-domain-method", false, false, "FULL"),
  FILE_OVERALL_CONCURRENCT_CHANNELS("/*/file-properties/file-transfer-service/overall-concurrent-channels", false, false, 10),
  FILE_CONCURRENT_UPLOAD_CHANNELS("/*/file-properties/file-transfer-service/concurrent-upload-channels", false, false, 5),
  FILE_CONCURRENT_DOWNLOAD_CHANNELS("/*/file-properties/file-transfer-service/concurrent-download-channels", false, false, 5),
  FILE_WORK_SLEEP_TIME_MS("/*/file-properties/file-transfer-service/worker-sleep-time-ms", false, false, 5);

  private final String mPath;
  private final boolean mIsXML;
  private final boolean mIsMandatory;
  private final Object mDefaultValue;

  private FoxEnvironmentProperty(String pName, boolean pIsXML) {
    mPath = pName;
    mIsXML = pIsXML;
    mIsMandatory = true;
    mDefaultValue = null;
  }
  private FoxEnvironmentProperty(String pName, boolean pIsXML, boolean pIsMandatory) {
    mPath = pName;
    mIsXML = pIsXML;
    mIsMandatory = pIsMandatory;
    mDefaultValue = null;
  }

  private FoxEnvironmentProperty(String pName, boolean pIsXML, boolean pIsMandatory, Object pDefaultValue) {
    mPath = pName;
    mIsXML = pIsXML;
    mIsMandatory = pIsMandatory;
    mDefaultValue = pDefaultValue;
  }

  public boolean isMandatory() {
    return mIsMandatory;
  }

  public String getPath() {
    return mPath;
  }

  public boolean isXML() {
    return mIsXML;
  }

  public Object getDefaultValue() {
    return mDefaultValue;
  }
}

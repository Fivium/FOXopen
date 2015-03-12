package net.foxopen.fox.dbinterface;


import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.FoxGbl;


/**
 * DataTypes which are allowed to be used as the datadom-type of an fm:into or fm:using parameter.
 */
public enum DOMDataType {
  STRING("string", FoxGbl.FOX_JAVA_DATE_TIME_FORMAT), //Note this is the legacy default format - if left null, could rely on SQLTypeConverter doing a cleverer conversion - left this way to minimise regression
  DOM("dom", null),
  DATE("date", FoxGbl.FOX_DATE_FORMAT),
  DATETIME("datetime", FoxGbl.FOX_JAVA_DATE_TIME_FORMAT),
  TIME("time", FoxGbl.FOX_TIME_XML_FORMAT);

  private static Map<String, DOMDataType> gExternalStringToType = new HashMap<>(5);
  static {
    for(DOMDataType lType : values()) {
      gExternalStringToType.put(lType.mExternalString, lType);
    }
  }

  /**
   * Case-insensitive lookup.
   * @param pExternalString
   * @return
   */
  public static DOMDataType fromExternalString(String pExternalString) {
    //Trim off optional xs: prefix
    if(pExternalString.startsWith("xs:")) {
      pExternalString = pExternalString.replaceFirst("xs:", "");
    }
    return gExternalStringToType.get(pExternalString.toLowerCase());
  }

  private final String mExternalString;
  private final String mDateFormatMask;

  private DOMDataType(String pExternalString, String pDateFormatMask) {
    mExternalString = pExternalString;
    mDateFormatMask = pDateFormatMask;
  }

  /**
   * Gets the Java date format which this DOM type uses to represent dates.
   * Can be null if this type cannot be sensibly coverted to a date.
   * @return
   */
  public String getDateFormatMask() {
    return mDateFormatMask;
  }
}

package net.foxopen.fox.plugin.api.util;

import java.util.Date;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.plugin.api.database.FxpUCon;
import net.foxopen.fox.util.StringFormatter;


public class FxpStringFormatter {
  private FxpStringFormatter() {}

  public static final String XML_DATE_FORMAT_MASK = StringFormatter.XML_DATE_FORMAT_MASK;
  public static final String ORA_DATE_FORMAT_MASK = StringFormatter.ORA_DATE_FORMAT_MASK;
  public static final String XML_DATETIME_FORMAT_MASK = StringFormatter.XML_DATETIME_FORMAT_MASK;
  public static final String ORA_DATETIME_FORMAT_MASK = StringFormatter.ORA_DATETIME_FORMAT_MASK;
  public static final String ORA_DATETIME_NO_SECONDS_FORMAT_MASK = StringFormatter.ORA_DATETIME_NO_SECONDS_FORMAT_MASK;
  public static final String XML_TIME_FORMAT_MASK = StringFormatter.XML_TIME_FORMAT_MASK;
  public static final String ORA_TIME_FORMAT_MASK = StringFormatter.ORA_TIME_FORMAT_MASK;

  /**
   * Invokes {@link #formatDateString(String, String, String)} and suppresses any exceptions. In the event of an
   * exception, pInputDateString is returned unmodified.
   * @param pInputDateString
   * @param pInputMask
   * @param pOutputMask
   * @return
   */
  public static String formatDateStringSafe (String pInputDateString, String pInputMask, String pOutputMask)  {
    return StringFormatter.formatDateStringSafe (pInputDateString, pInputMask, pOutputMask);
  }

  /**
   * Converts the format of a date string using an input and output mask for
   * conversion. Will fall back to using Oracle to format date if format is not
   * supported in this Java implementation.
   * @param pInputDateString date to convert (as a string)
   * @param pInputMask input date format
   * @param pOutputMask output date format
   * @return date as a string, formatted using output mask
   * @throws ExInternal
   */
  public static String formatDateString (String pInputDateString, String pInputMask, String pOutputMask) {
    return StringFormatter.formatDateString (pInputDateString, pInputMask, pOutputMask);
  }

  /**
   * Parse and return a Java Date object from date string and format mask string.
   * @param pDateString string to convert to date
   * @param pFormatMaskString mask to use in conversion
   * @return passed string as Date
   * @throws ExInternal
   */
  public static Date parseDate (String pDateString, String pFormatMaskString) {
    return StringFormatter.parseDate (pDateString, pFormatMaskString);
  }

  /**
   * Formats a decimal using a given format string.
   * @param pInputDecimalString decimal as a string to format
   * @param pFormatMask format mask to use in conversion
   * @return formatted decimal as string
   * @throws ExInternal
   */
  public static String formatDecimalString (String pInputDecimalString, String pFormatMask) {
    return StringFormatter.formatDecimalString(pInputDecimalString, pFormatMask);
  }

  /**
   * Formats a decimal using a given format string.
   * @param pInputDecimalString decimal as a string to format
   * @param pFormatMask format mask to use in conversion
   * @param pOptionalUCon optional database connection to use if format cannot be handled by this class
   * @return formatted decimal as string
   * @throws ExInternal
   */
  public static String formatDecimalString (String pInputDecimalString, String pFormatMask, FxpUCon pOptionalUCon) {
    return StringFormatter.formatDecimalString (pInputDecimalString, pFormatMask, (UCon) pOptionalUCon);
  }
}

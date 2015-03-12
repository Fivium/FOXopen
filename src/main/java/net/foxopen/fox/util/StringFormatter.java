/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.util;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility class for converting between strings and dates/numbers using masks.
 * Conversion and parse methods can optionally accept a UCon to fallback on
 * Oracle if specific mask isn't catered for in this class.
 */
public abstract class StringFormatter {

  static enum CaseOption {
    UPPER, LOWER, PRESERVE;
  }

  // Publicly declared and supported date format masks
  public static final String XML_DATE_FORMAT_MASK = "YYYY-MM-DD";
  public static final String ORA_DATE_FORMAT_MASK = "DD-MON-YYYY";
  public static final String XML_DATETIME_FORMAT_MASK = "YYYY-MM-DD\"T\"HH24:MI:SS";
  public static final String ORA_DATETIME_FORMAT_MASK = "DD-MON-YYYY HH24:MI:SS";
  public static final String ORA_DATETIME_NO_SECONDS_FORMAT_MASK = "DD-MON-YYYY HH24:MI";
  public static final String XML_TIME_FORMAT_MASK = "HH24:MI:SS";
  public static final String ORA_TIME_FORMAT_MASK = "HH24:MI:SS";

  final static String ORDINAL_USER_STRING_PLACEHOLDER = "{th}";
  final static String ORDINAL_FORMAT_MASK_STRING_PLACEHOLDER = "'"+ORDINAL_USER_STRING_PLACEHOLDER+"'";


  /**
   * Cached results of external format masks to parsed internal representations.
   */
  private static final Map<String, FormatMask> gExternalDateFormatToFormatMaskMap = new ConcurrentHashMap<>();

  /**
   * Converts a date format mask as provided by a module developer into a Java-compatible mask. Developers are allowed
   * to specify format masks in the Oracle database form - this method handles the conversion to a Java mask. Developers
   * may also directly enter a mask in the Java form, provided the mask has a special prefix.
   * Use the ordinal in your java format mask {th} to get the ordinal position of the day of the month, eg 2nd, 3rd, etc.
   * @param pExternalDateMask Date mask to convert.
   * @return Java compatible format mask with case option.
   */
  static FormatMask convertExternalDateMask(String pExternalDateMask) {
    String lExternalDateMask = pExternalDateMask;

    // Don't do any conversion other than ordinal position, if the user prefixes the format mask with a special value (exact value drives case)
    if (pExternalDateMask.startsWith("j/") || pExternalDateMask.startsWith("J/") || pExternalDateMask.startsWith("jl/")) {
      lExternalDateMask = lExternalDateMask.replace(ORDINAL_USER_STRING_PLACEHOLDER, ORDINAL_FORMAT_MASK_STRING_PLACEHOLDER);
    }

    if(pExternalDateMask.startsWith("j/")) {
      return new FormatMask(lExternalDateMask.substring(2), CaseOption.PRESERVE);
    }
    else if(pExternalDateMask.startsWith("J/")) {
      return new FormatMask(lExternalDateMask.substring(2), CaseOption.UPPER);
    }
    else if(pExternalDateMask.startsWith("jl/")) {
      return new FormatMask(lExternalDateMask.substring(3), CaseOption.LOWER);
    }
    else {
      //Default is to preseve the case Java gives us
      CaseOption lCaseOption = CaseOption.PRESERVE;

      //Check for use of UPPER or lower case keywords and choose a case option appropriately
      //Oracle allows granular control of the case of each facet, but we can't do that easily in Java, so this will have to do
      //NB "MON" check covers "Mon" or "Month" (Jan/January)
      if(pExternalDateMask.contains("MON") || pExternalDateMask.contains("DAY") || pExternalDateMask.contains("DY")) {
        lCaseOption = CaseOption.UPPER;
      }
      else if (pExternalDateMask.contains("mon") || pExternalDateMask.contains("day") || pExternalDateMask.contains("dy")) {
        lCaseOption = CaseOption.LOWER;
      }

      final String SINGLE_QUOTE_REPLACE_SEQ = "{{{SQ}}}";

      //Convert existing single quotes to a special string we can replace back later
      pExternalDateMask = pExternalDateMask.replace("'", SINGLE_QUOTE_REPLACE_SEQ);

      //Convert quotes from double to single now so we can split on them
      pExternalDateMask = pExternalDateMask.replace("\"", "'");

      //Split into quoted and unquoted segments - we're only escaping unquoted
      Pattern p = Pattern.compile("([^'][^']*|'.*?')");
      Matcher m = p.matcher(pExternalDateMask);

      StringBuilder lJavaFormatMask = new StringBuilder();
      while(m.find()) {
        String lToken = m.group();
        if(!lToken.startsWith("'")) {
          //Convert string to upper case so replacement logic is simpler
          lToken = lToken.toUpperCase();

          //Replace Oracle mask facets to Java equivalents (only when not in quote marks)
          lToken = lToken
            .replace("HH24", "H")
            .replace("HH12", "h")
            .replace("HH", "h")
            .replace("AM", "a")
            .replace("PM", "a")
            .replace("MI", "mm")
            .replace("SS", "ss")
            .replace("DD", "dd")
            .replace("DY", "EEE")
            .replace("DAY", "EEEEE")
            .replace("MONTH", "MMMMM")
            .replace("MON", "MMM")
            .replace("YYYY", "yyyy")
            .replace("YY", "yy");
        }

        lJavaFormatMask.append(lToken);
      }

      //Replace literal single quotes back in
      String lResultFormatMask = lJavaFormatMask.toString();
      lResultFormatMask = lResultFormatMask.replace(SINGLE_QUOTE_REPLACE_SEQ, "''");

      return new FormatMask(lResultFormatMask, lCaseOption);
    }
  }

  static FormatMask getOrCreateDateFormatMask(String pExternalDateMask) {
    FormatMask lFormatMask = gExternalDateFormatToFormatMaskMap.get(pExternalDateMask);
    if(lFormatMask == null) {
      lFormatMask = convertExternalDateMask(pExternalDateMask);
      gExternalDateFormatToFormatMaskMap.put(pExternalDateMask, lFormatMask);
    }

    return lFormatMask;
  }

  /**
   * Invokes {@link #formatDateString(String, String, String)} and suppresses any exceptions. In the event of an
   * exception, pInputDateString is returned unmodified.
   * @param pInputDateString
   * @param pInputMask
   * @param pOutputMask
   * @return
   */
  public static String formatDateStringSafe (String pInputDateString, String pInputMask, String pOutputMask)   {
    try {
      return formatDateString(pInputDateString, pInputMask, pOutputMask);
    }
    catch (ExInternal e) {
      Track.alert("DateParseException", "Parse " + pInputDateString + " using mask " + pInputMask + " failed");
      return pInputDateString;
    }
  }


  /**
   * Converts the format of a date string using an input and output mask for
   * conversion. A placeholder of {th} can be used in a java format mask to output
   * the
   * @param pInputDateString date to convert (as a string)
   * @param pInputMask input date format
   * @param pOutputMask output date format
   * @param pOptionalUCon optional UCon to use for db lookup
   * @return date as a string, formatted using output mask
   * @throws ExInternal
   */
  public static String formatDateString (String pInputDateString, String pInputMask, String pOutputMask)
  throws ExInternal {

    // Simple bypass for cases where people have put in "" to hide a date
    if ("".equals(pOutputMask)) {
      return pOutputMask;
    }

    // Simple bypass for cases where an empty string is passed in
    if ("".equals(pInputDateString)) {
      return pInputDateString;
    }

    // Convert consumer masks to Java masks
    FormatMask lInputMask = getOrCreateDateFormatMask(pInputMask);
    FormatMask lOutputMask = getOrCreateDateFormatMask(pOutputMask);

    // Read in date from string
    Date lDate;
    try {
      lDate = parseDate(pInputDateString, lInputMask);
    }
    catch (ExInternal ex) {
      throw new ExInternal("Date parsing failed for value '" + pInputDateString + "' using Oracle date mask '" + pInputMask + "'", ex);
    }

    // Belt and braces check
    if (lDate == null) {
      throw new ExInternal("StringFormatter.parseDate returned null in StringFormatter.formatDateString");
    }

    // Format the parsed date, replacing any ordinal positions if needed
    String lConvertedString = formatDate(lDate, lOutputMask);

    // Case conversion on return
    switch (lOutputMask.getFormatCase()) {
      case UPPER: {
        return lConvertedString.toUpperCase();
      }
      case LOWER: {
        return lConvertedString.toLowerCase();
      }
      case PRESERVE:
      default: {
        return lConvertedString;
      }
    }
  }

  /**
   * Formats a given date from a constructed format mask. Encapsulates the logic
   * to replace the day of month with the ordinal position, if the ordinal position holder is placed.
   *
   * @param pDate input value date
   * @param pFormatMask output java date format mask
   * @return the formatted date
   */
  private static String formatDate(Date pDate, FormatMask pFormatMask) {
    // Get the formatted date with the place holder being output as a literal.
    String lConvertedString = pFormatMask.createDateFormat().format(pDate);

    // If an ordinal placeholder exists
    if (pFormatMask.getFormatString().contains(ORDINAL_USER_STRING_PLACEHOLDER)) {
      // Determine the day of the month for the input value
      Calendar lCalendar = Calendar.getInstance();
      lCalendar.setTime(pDate);
      int lDayOfMonth = lCalendar.get(Calendar.DAY_OF_MONTH);

      // Grab the ordinal position of the day of the month
      String lOrdinalPosition = getOrdinalPosition(lDayOfMonth);

      // Replace the ordinal position placeholder with the date
      lConvertedString = lConvertedString.replace(ORDINAL_USER_STRING_PLACEHOLDER, lOrdinalPosition);
    }

    // Return the converted string
    return lConvertedString;
  }

  /**
   * Return the ordinal position of a number as a string. 1 would return "st",
   * 2 would return "nd" and three would return "rd", etc.
   * @param pValue input number
   * @return
   */
  private static String getOrdinalPosition(int pValue) {
    // For any number not in the teens (11,12,13) the ordinal position
    // is decided by the first digit on the right. This strips out all other digits.
    int lTenRemainder = pValue % 10;

    // If the number is 11, 12, 13 then the suffix is th.
    int lHundredRemainder = pValue % 100;
    if(lHundredRemainder - lTenRemainder == 10) {
      return "th";
    }

    switch (lTenRemainder) {
      case 1:
        return "st";
      case 2:
        return "nd";
      case 3:
        return "rd";
      default:
        return "th";
    }
  }

  /**
   * Parse and return a Java Date object from date string and format mask string.
   * @param pDateString string to convert to date
   * @param pFormatMaskString mask to use in conversion
   * @return passed string as Date
   * @throws ExInternal
   */
  public static Date parseDate (String pDateString, String pFormatMaskString)
  throws ExInternal {
    FormatMask lInputMask = getOrCreateDateFormatMask(pFormatMaskString);
    return parseDate(pDateString, lInputMask);
  }

  /**
   * Parse and return a Java Date object from date string and FormatMask instance with optional UCon.
   * @param pDateString string to convert to date
   * @param pFormatMask the FormatMask instance to use in conversion
   * @param pOptionalUCon database connection to use if the format string is not supported in this class
   * @return passed string as Date
   * @throws ExInternal
   */
  private static Date parseDate (String pDateString, FormatMask pFormatMask)
  throws ExInternal {
    // Belt and braces
    if(pFormatMask == null) {
      throw new ExInternal("Format mask cannot be null.");
    }

    try {
      return pFormatMask.createDateFormat().parse(pDateString);
    }
    catch (ParseException ex) {
      throw new ExInternal("Could not parse date '" + pDateString + "' (Java mask '" + pFormatMask.getFormatString() + "')", ex);
    }
  }

  /**
   * Formats a decimal using a given format string.
   * @param pInputDecimalString decimal as a string to format
   * @param pFormatMask format mask to use in conversion
   * @return formatted decimal as string
   * @throws ExInternal
   */
  public static String formatDecimalString (String pInputDecimalString, String pFormatMask)
  throws ExInternal {
    return formatDecimalString (pInputDecimalString, pFormatMask, null);
  }

  /**
   * Formats a decimal using a given format string.
   * @param pInputDecimalString decimal as a string to format
   * @param pFormatMask format mask to use in conversion
   * @param pOptionalUCon optional database connection to use if format cannot be handled by this class
   * @return formatted decimal as string
   * @throws ExInternal
   */
  public static String formatDecimalString (String pInputDecimalString, String pFormatMask, UCon pOptionalUCon)
  throws ExInternal {
    try {
      // Attempt to parse
      double lDouble = Double.parseDouble(pInputDecimalString);

      // We can cope with simple formats with no leading spaces with 0..n decimal places
      if (XFUtil.regexpMatches("FM[09]+(\\.[09]+)?", pFormatMask)) {
        DecimalFormat lDecimalFormat = new DecimalFormat();
        lDecimalFormat.applyPattern(pFormatMask.replaceAll("FM","").replaceAll("9","#"));
        return lDecimalFormat.format(lDouble);
      }
      // For everything else, go to the database
      else {
        return formatDecimalUsingDatabase(pInputDecimalString, pFormatMask, pOptionalUCon);
      }
    }
    catch (ClassCastException ex) {
      throw new ExInternal("Failed to cast result of database number formatting", ex);
    }
  }

  /**
   * Formats a decimal using a given format string.
   * @param pInputDecimalString decimal as a string to format
   * @param pFormatMask format mask to use in conversion
   * @param pUCon required database connection
   * @return formatted decimal as string
   * @throws ExInternal
   */
  //TODO PN clean up
  private static String formatDecimalUsingDatabase (String pInputDecimalString, String pFormatMask, UCon pUCon)
  throws ExInternal {
    if (pUCon ==  null) {
      throw new ExInternal("UCon must be provided to StringFormatter.formatDecimalUsingDatabase");
    }
    // Track, so we can add the most commonly used format strings to this class
    //Track.trackPush("StringFormatter").setAttribute("inputmask", pFormatMask);
    try {
      Object[] lColumns;
      lColumns = pUCon.selectOneRow(
        "SELECT TO_CHAR(TO_NUMBER(:1), :2) FROM DUAL"
      , new String[] {pInputDecimalString, pFormatMask}
      );
      return (String) lColumns[0];
    }
    catch (ExDB ex) {
      throw new ExInternal("Failed to parse and convert decimal format using database, mask '" + pFormatMask + "', value '" + pInputDecimalString + "'");
    }
  }

  /**
   * Convenience subclass for containing format masks.
   */
  static class FormatMask {

    private String mFormatString;
    private CaseOption mFormatCase;

    /**
     * Construct a FormatMask with a string and case conversion specification.
     * @param pFormatString format string to specify
     * @param pFormatCase case conversion flag to specify
     */
    public FormatMask (String pFormatString, CaseOption pFormatCase) {
      mFormatString = pFormatString;
      mFormatCase = pFormatCase;
    }

    /**
     * Returns the format string.
     * @return format string
     */
    public String getFormatString () {
      return mFormatString;
    }

    public DateFormat createDateFormat() {
      SimpleDateFormat lSimpleDateFormat;
      try {
        lSimpleDateFormat = new SimpleDateFormat(mFormatString);
      }
      catch (IllegalArgumentException e) {
        throw new ExInternal("Invalid date format " + mFormatString, e);
      }
      //Disallow silly stuff like the 32nd of January
      lSimpleDateFormat.setLenient(false);

      return lSimpleDateFormat;
    }

    /**
     * Returns the case conversion flag.
     * @return case conversion flag
     */
    public CaseOption getFormatCase () {
      return mFormatCase;
    }

    /**
     * Returns the format string.
     * @return format string
     */
    public String toString () {
      return mFormatString;
    }
  }
}

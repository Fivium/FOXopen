package net.foxopen.fox.database.sql.out;

import net.foxopen.fox.FoxGbl;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.xml.DefaultSQLXMLReader;
import net.foxopen.fox.database.xml.OracleBinaryXMLReader;
import net.foxopen.fox.database.xml.XMLReaderStrategy;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDOMParser;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import oracle.jdbc.OracleTypes;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * Static methods for converting SQL column objects into FOX Java objects.
 */
public final class SQLTypeConverter {

  private SQLTypeConverter () {}

  /**
   * Converts a ResultSet value to a String. For a SQLXML column, the XML string from the JDBC driver is returned. For
   * dates, the default date format mask is used (see {@link #getValueAsDateString(JDBCResultAdaptor, int, String)}).
   * @param pResultSet ResultSet to read from.
   * @param pColumnIndex 1-based index of column to convert.
   * @param pSQLColumnType Column type of the column.
   * @return The column value as a string.
   * @throws SQLException
   */
  public static String getValueAsString(JDBCResultAdaptor pResultSet, int pColumnIndex, int pSQLColumnType)
  throws SQLException {

    switch(pSQLColumnType) {
      case Types.SQLXML:
        if(pResultSet.getSQLXML(pColumnIndex) == null) {
          return null;
        }
        return pResultSet.getSQLXML(pColumnIndex).getString();
      case Types.DATE:
      case Types.TIMESTAMP:
        return getValueAsDateString(pResultSet, pColumnIndex);
      case OracleTypes.INTERVALDS:
      case OracleTypes.INTERVALYM:
        return pResultSet.getObject(pColumnIndex).toString();
      default:
        //Varchars, numbers, etc
        return pResultSet.getString(pColumnIndex);
    }
  }

  /**
   * Converts a ResultSet value into a string representation of a date using the default format mask for the selected
   * column type. See {@link #getValueAsDateString(JDBCResultAdaptor, int, String) here} for a description of the defaults.
   * @param pResultSet ResultSet to read from.
   * @param pColumnIndex 1-based index of column to convert.
   * @return The column value as a string representing a date in the default format mask.
   * @throws SQLException
   */
  public static String getValueAsDateString(JDBCResultAdaptor pResultSet, int pColumnIndex)
  throws SQLException {
    return getValueAsDateString(pResultSet, pColumnIndex, null);
  }

  /**
   * Converts a ResultSet value into a string representation of a date using the default format mask for the selected
   * column type. If supplied, the format mask is used to format the date into a string. Otherwise, for dates with an empty
   * (i.e. all-0) time component, the XSD date format mask is used. For dates with a non-null time component, the XSD
   * dateTime format is used.
   * @param pResultSet ResultSet to read from.
   * @param pColumnIndex 1-based index of column to convert.
   * @param pOptionalFormatMask Java date format mask.
   * @returnThe column value as a string representing a date in the given or default format mask.
   * @throws SQLException
   */
  public static String getValueAsDateString(JDBCResultAdaptor pResultSet, int pColumnIndex, String pOptionalFormatMask)
  throws SQLException {

    Date lDateValue = pResultSet.getTimestamp(pColumnIndex);
    //Short circuit if the column is null
    if(lDateValue == null) {
      return "";
    }

    String lFormatMask;
    if (!XFUtil.isNull(pOptionalFormatMask)) {
      lFormatMask = pOptionalFormatMask;
    }
    else {
      //Establish a default format mask - if the date contains hours/minutes/seconds default to xs:dateTime mask,
      //otherwise default to xs:date mask.
      Calendar lCal = Calendar.getInstance();
      lCal.setTime(lDateValue);
      if((lCal.get(Calendar.MILLISECOND) + lCal.get(Calendar.SECOND) + lCal.get(Calendar.MINUTE) + lCal.get(Calendar.HOUR_OF_DAY)) > 0) {
        //Note this could cause a regression impact
        Track.alert("ImplicitSQLDateConversion", "Converting SQL DATE to xs:dateTime string as time component was found");
        lFormatMask = FoxGbl.FOX_JAVA_DATE_TIME_FORMAT;
      }
      else {
        Track.alert("ImplicitSQLDateConversion", "Converting SQL DATE to xs:date string as no time component was found");
        lFormatMask = FoxGbl.FOX_DATE_FORMAT;
      }
    }

    DateFormat lFormat = new SimpleDateFormat(lFormatMask);
    return lFormat.format(lDateValue);
  }

  /**
   * Converts the value at the given index in the result set to a DOM. This works for SQLXML columns and any column type
   * which can provide a character stream (i.e. Clobs, Varchars, etc). Returns null if the column is null.
   * @param pResultSet ResultAdaptor to read from.
   * @param pColumnIndex 1-based index of the column to retrieve.
   * @param pSQLColumnType The column type of the column to read according to the original result set.
   * @return A new DOM object based on the column's value.
   * @throws SQLException If reading the column fails.
   */
  public static DOM getValueAsDOM(JDBCResultAdaptor pResultSet, int pColumnIndex, int pSQLColumnType)
  throws SQLException {

    if (pSQLColumnType == Types.SQLXML) {
      SQLXML lSQLXML = pResultSet.getSQLXML(pColumnIndex);
      return SQLXMLToDOM(lSQLXML);
    }
    else {
      // If it's anything else (including CLOBs) just use the default conversion
      Reader lReader = pResultSet.getCharacterStream(pColumnIndex);

      //Skip null readers (i.e. if column is null)
      if(lReader == null) {
        Track.debug("GetValueAsDOM", "Skipping character stream read to DOM as reader was null");
        return null;
      }

      try {
        return DOM.createDocument(lReader);
      }
      catch (ExDOMParser lDomEx) {
        // If the XML cannot be parsed but, data is blank then ignore the error and just return a null value
        // otherwise raise the original parse error
        try {
          String lColumnStringValue = pResultSet.getString(pColumnIndex);
          if (XFUtil.isNull(lColumnStringValue)) {
            return null;
          }
          else {
            throw lDomEx;
          }
        }
        catch (SQLException e) {
          //Exception hit getting the column string value following a parser failure
          //Preserve the original exception but report the new one
          throw new ExInternal("Failed to parse XML using column reader and encountered an additional exception when checking the column (see nested). " +
            "Parser exception was " + lDomEx.getMessage(), e);
        }
      }
    }
  }

  /**
   * Applies the value at the given index of the ResultSet to a target DOM in the way most appropriate to the column's type.
   * <ul>
   *   <li>For SQLXML, the sub-root nodes of the column are copied to the target node.</li>
   *   <li>For dates with no time component, the column is formatted as an xs:date.</li>
   *   <li>For dates with a time component, the column is formatted as an xs:dateTime.</li>
   *   <li>For all other column types, the string value of the column is used.</li>
   * </ul>
   * @param pResultSet ResultAdaptor to read from.
   * @param pColumnIndex 1-based index of column to retrieve.
   * @param pSQLColumnType The column type of the column to read according to the original result set.
   * @param pTargetDOM The destination node for the read column.
   * @throws SQLException
   */
  public static void applyValueToDOM(JDBCResultAdaptor pResultSet, int pColumnIndex, int pSQLColumnType, DOM pTargetDOM)
  throws SQLException {

    if (pSQLColumnType == Types.SQLXML) {
      SQLXML lSQLXML = pResultSet.getSQLXML(pColumnIndex);
      DOM lColumnDOM = SQLXMLToDOM(lSQLXML);
      lColumnDOM.copyContentsTo(pTargetDOM);
    }
    else {
      String lStringValue = getValueAsString(pResultSet, pColumnIndex, pSQLColumnType);
      pTargetDOM.setText(lStringValue);
    }
  }

  private static StringWriter readClob(Clob pClob) {
    StringWriter lWriter = new StringWriter();
    try {
      IOUtils.copy(pClob.getCharacterStream(), lWriter);
    }
    catch (IOException | SQLException e) {
      throw new ExInternal("Failed to read CLOB into a StringWriter", e);
    }
    return lWriter;
  }

  /**
   * Reads a Clob into a String. Note: only use this for small Clobs.
   * @param pClob Clob to read.
   * @return Clob contents as a String.
   */
  public static String clobToString(Clob pClob) {
    return readClob(pClob).toString();
  }

  /**
   * Reads a Clob into a StringBuffer. Note: only use this for small Clobs.
   * @param pClob Clob to read.
   * @return Clob contents as a StringBuffer.
   */
  public static StringBuffer clobToStringBuffer(Clob pClob) {
    return readClob(pClob).getBuffer();
  }

  /**
   * Reads a Clob representation of XML into a DOM.
   * @param pClob Clob to read.
   * @return Clob contents as a DOM.
   */
  public static DOM clobToDOM(Clob pClob) {
    try {
      return DOM.createDocument(pClob.getCharacterStream());
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to open CLOB character stream", e);
    }
  }

  /**
   * Reads a SQLXML object into a DOM.
   * @param pSQLXML XML to read.
   * @return New DOM.
   */
  public static DOM SQLXMLToDOM(SQLXML pSQLXML) {
    return SQLXMLToDOM(pSQLXML, false);
  }

  /**
   * Reads a SQLXML object into a DOM.
   * @param pSQLXML XML to read.
   * @param pBinaryXML If true, the specialist binary XML reader is used to read the XML. This may be required to avoid
   * the standard Oracle XML deserialiser introducing whitespace.
   * @return
   */
  public static DOM SQLXMLToDOM(SQLXML pSQLXML, boolean pBinaryXML) {

    XMLReaderStrategy lXMLReaderStrategy = pBinaryXML ? OracleBinaryXMLReader.instance() : DefaultSQLXMLReader.instance();
    try {
      if(pSQLXML == null) {
        return null;
      }
      else {
        return lXMLReaderStrategy.read(pSQLXML);
      }
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to read SQLXML", e);
    }
  }

  /**
   * Retrieves an object from a ResultSet using the most appropriate JDBC method, based on the BindSQLType provided. This
   * ensures that selected Objects are always of the same class for a given BindSQLType.
   * @param pResultSet ResultSet to read from.
   * @param pColumnIndex 1-based index of the column to retrieve.
   * @param pBindSQLType Expected column type, used to determine the correct getXXX method to use.
   * @return The object at the corresponding index.
   * @throws SQLException
   */
  public static Object getValueAsObjectForBindSQLType(JDBCResultAdaptor pResultSet, int pColumnIndex, BindSQLType pBindSQLType)
  throws SQLException {
    switch(pBindSQLType) {
      case STRING:
        return pResultSet.getString(pColumnIndex);
      case XML:
        return pResultSet.getSQLXML(pColumnIndex);
      case BLOB:
        return pResultSet.getBlob(pColumnIndex);
      case CLOB:
        return pResultSet.getClob(pColumnIndex);
      case TIMESTAMP:
        return pResultSet.getTimestamp(pColumnIndex);
      case NUMBER:
        return pResultSet.getDouble(pColumnIndex);
      default:
        throw new ExInternal("Cannot convert a " + pBindSQLType);
    }
  }
}

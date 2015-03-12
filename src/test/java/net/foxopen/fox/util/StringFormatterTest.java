package net.foxopen.fox.util;

import net.foxopen.fox.util.StringFormatter.CaseOption;
import net.foxopen.fox.util.StringFormatter.FormatMask;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StringFormatterTest {

  private static void testDateFormatConversion(String pInputFormat, FormatMask pExpectedOutput) {
    StringFormatter.FormatMask lActualOutput = StringFormatter.convertExternalDateMask(pInputFormat);

    assertEquals("Converted FormatMask has expected string",  pExpectedOutput.getFormatString(), lActualOutput.getFormatString());
    assertEquals("Converted FormatMask has expected case option",  pExpectedOutput.getFormatCase(), lActualOutput.getFormatCase());
  }

  @Test
  public void testOracleToJavaDateFormatConversion() {

    testDateFormatConversion("YYYY-MM-DD", new FormatMask("yyyy-MM-dd", CaseOption.PRESERVE));
    testDateFormatConversion("DD-MON-YYYY", new FormatMask("dd-MMM-yyyy", CaseOption.UPPER));
    testDateFormatConversion("YYYY-MM-DD\"T\"HH24:MI:SS", new FormatMask("yyyy-MM-dd'T'H:mm:ss", CaseOption.PRESERVE));
    testDateFormatConversion("DD-MON-YYYY HH24:MI:SS", new FormatMask("dd-MMM-yyyy H:mm:ss", CaseOption.UPPER));
    testDateFormatConversion("DD-MON-YYYY HH24:MI", new FormatMask("dd-MMM-yyyy H:mm", CaseOption.UPPER));
    testDateFormatConversion("HH24:MI:SS", new FormatMask("H:mm:ss", CaseOption.PRESERVE));
    testDateFormatConversion("HH24:MI:SS", new FormatMask("H:mm:ss", CaseOption.PRESERVE));

    // Non-standard datetime format masks
    testDateFormatConversion("DD-MON-YYYY HH24:MI", new FormatMask("dd-MMM-yyyy H:mm", CaseOption.UPPER));
    testDateFormatConversion("DD MON YYYY HH24:MI", new FormatMask("dd MMM yyyy H:mm", CaseOption.UPPER));
    testDateFormatConversion("DD/MON/YYYY HH24:MI", new FormatMask("dd/MMM/yyyy H:mm", CaseOption.UPPER));
    testDateFormatConversion("DD-MON-YYYY HH:MI", new FormatMask("dd-MMM-yyyy h:mm", CaseOption.UPPER));
    testDateFormatConversion("DD MON YYYY HH:MI", new FormatMask("dd MMM yyyy h:mm", CaseOption.UPPER));
    testDateFormatConversion("DD/MON/YYYY HH:MI", new FormatMask("dd/MMM/yyyy h:mm", CaseOption.UPPER));

    // Non-standard date format masks
    testDateFormatConversion("DD MON YYYY", new FormatMask("dd MMM yyyy", CaseOption.UPPER));
    testDateFormatConversion("DD/MON/YYYY", new FormatMask("dd/MMM/yyyy", CaseOption.UPPER));
    testDateFormatConversion("DD Month YYYY", new FormatMask("dd MMMMM yyyy", CaseOption.PRESERVE));
    testDateFormatConversion("DD MONTH YYYY", new FormatMask("dd MMMMM yyyy", CaseOption.UPPER));
    testDateFormatConversion("DD MM YYYY", new FormatMask("dd MM yyyy", CaseOption.PRESERVE));
    testDateFormatConversion("DD-MM-YYYY", new FormatMask("dd-MM-yyyy", CaseOption.PRESERVE));
    testDateFormatConversion("DD/MM/YYYY", new FormatMask("dd/MM/yyyy", CaseOption.PRESERVE));
    testDateFormatConversion("DD MM YY", new FormatMask("dd MM yy", CaseOption.PRESERVE));
    testDateFormatConversion("DD-MM-YY", new FormatMask("dd-MM-yy", CaseOption.PRESERVE));
    testDateFormatConversion("DD/MM/YY", new FormatMask("dd/MM/yy", CaseOption.PRESERVE));
    testDateFormatConversion("DD", new FormatMask("dd", CaseOption.PRESERVE));
    testDateFormatConversion("MM", new FormatMask("MM", CaseOption.PRESERVE));
    testDateFormatConversion("YYYY", new FormatMask("yyyy", CaseOption.PRESERVE));
    testDateFormatConversion("YY", new FormatMask("yy", CaseOption.PRESERVE));

    // Non-standard time format masks
    testDateFormatConversion("HH:MI:SS", new FormatMask("h:mm:ss", CaseOption.PRESERVE));
    testDateFormatConversion("MI:SS", new FormatMask("mm:ss", CaseOption.PRESERVE));
    testDateFormatConversion("HH24:MI", new FormatMask("H:mm", CaseOption.PRESERVE));
    testDateFormatConversion("HH:MI", new FormatMask("h:mm", CaseOption.PRESERVE));

    testDateFormatConversion("DD Mon YYYY", new FormatMask("dd MMM yyyy", CaseOption.PRESERVE));
    testDateFormatConversion("DD MON YYYY", new FormatMask("dd MMM yyyy", CaseOption.UPPER));
    testDateFormatConversion("DD mon YYYY", new FormatMask("dd MMM yyyy", CaseOption.LOWER));

    testDateFormatConversion("DD Dy", new FormatMask("dd EEE", CaseOption.PRESERVE));
    testDateFormatConversion("DD DY", new FormatMask("dd EEE", CaseOption.UPPER));
    testDateFormatConversion("DD dy", new FormatMask("dd EEE", CaseOption.LOWER));

    testDateFormatConversion("DD Day", new FormatMask("dd EEEEE", CaseOption.PRESERVE));
    testDateFormatConversion("DD DAY", new FormatMask("dd EEEEE", CaseOption.UPPER));
    testDateFormatConversion("DD day", new FormatMask("dd EEEEE", CaseOption.LOWER));

    //Java preserved, standard case
    testDateFormatConversion("j/dd MM yyyy", new FormatMask("dd MM yyyy", CaseOption.PRESERVE));
    //Preserved, upper case
    testDateFormatConversion("J/dd MM yyyy", new FormatMask("dd MM yyyy", CaseOption.UPPER));
    //Preserved, lower case
    testDateFormatConversion("jl/dd MM yyyy", new FormatMask("dd MM yyyy", CaseOption.LOWER));

  }

  //PN TODO parsing tests

  @Test
  public void testDateFormatting() {

    final String DATE_STRING_XML = "2014-02-01";
    final String XML_DATE_FORMAT = "YYYY-MM-DD";

    assertEquals("Format date string", "01 Feb 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD Mon YYYY"));
    assertEquals("Format date string", "01 FEB 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD MON YYYY"));
    assertEquals("Format date string", "01/FEB/2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD/MON/YYYY"));
    assertEquals("Format date string", "01/Feb/2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD/Mon/YYYY"));
    assertEquals("Format date string", "01/feb/2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD/mon/YYYY"));
    assertEquals("Format date string", "Sat 01 Feb 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "Dy DD Mon YYYY"));
    assertEquals("Format date string", "01 February 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD Month YYYY"));
    assertEquals("Format date string", "01 FEBRUARY 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD MONTH YYYY"));
    assertEquals("Format date string", "01 february 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD month YYYY"));
    assertEquals("Format date string", "SATURDAY 01 FEBRUARY 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DAY DD MONTH YYYY"));
    assertEquals("Format date string", "01 02 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD MM YYYY"));
    assertEquals("Format date string", "01-02-2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD-MM-YYYY"));
    assertEquals("Format date string", "01/02/2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD/MM/YYYY"));
    assertEquals("Format date string", "01 02 14", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD MM YY"));
    assertEquals("Format date string", "01-02-14", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD-MM-YY"));
    assertEquals("Format date string", "01/02/14", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD/MM/YY"));
    assertEquals("Format date string", "01", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD"));
    assertEquals("Format date string", "SAT", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DY"));
    assertEquals("Format date string", "Sat", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "Dy"));
    assertEquals("Format date string", "sat", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "dy"));
    assertEquals("Format date string", "SATURDAY", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DAY"));
    assertEquals("Format date string", "Saturday", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "Day"));
    assertEquals("Format date string", "saturday", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "day"));
    assertEquals("Format date string", "02", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "MM"));
    assertEquals("Format date string", "2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "YYYY"));
    assertEquals("Format date string", "14", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "YY"));

    //Weird stuff
    //Same facet repeats
    assertEquals("Format date string", "01 Feb 2014 Feb", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD Mon YYYY Mon"));
    //Literal in string
    assertEquals("Format date string", "01 Feb XYZ 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD Mon \"XYZ\" YYYY"));
    //Keyword in literal
    assertEquals("Format date string", "01 Feb Mon 2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "DD Mon \"Mon\" YYYY"));

  }

  @Test
  public void testDateTimeFormatting() {

    final String DATETIME_STRING_XML = "2014-02-01T22:23:24";
    final String XML_DATETIME_FORMAT = "YYYY-MM-DD\"T\"HH24:MI:SS";

    assertEquals("Format datetime string", "2014-02-01T22:23:24", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "YYYY-MM-DD\"T\"HH24:MI:SS"));
    assertEquals("Format datetime string", "01 FEB 2014 22:23:24", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "DD MON YYYY HH24:MI:SS"));
    assertEquals("Format datetime string", "01-FEB-2014 22:23", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "DD-MON-YYYY HH24:MI"));
    assertEquals("Format datetime string", "22:23:24", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "HH24:MI:SS"));
    assertEquals("Format datetime string", "10:23:24", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "HH12:MI:SS"));
    assertEquals("Format datetime string", "10:23:24", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "HH:MI:SS"));
    assertEquals("Format datetime string", "10:23:24 PM", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "HH:MI:SS AM"));
    assertEquals("Format datetime string", "10:23:24 PM", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "HH:MI:SS PM"));
    assertEquals("Format datetime string", "10:23:24 PM", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "HH12:MI:SS PM"));

    //Weird stuff
    //Single quote in literal
    assertEquals("Format datetime string", "10 o'clock PM", StringFormatter.formatDateString(DATETIME_STRING_XML, XML_DATETIME_FORMAT, "HH12 \"o'clock\" PM"));
  }

  @Test
  public void testJavaDateFormatting() {

    final String DATE_STRING_XML = "2014-02-01";
    final String XML_DATE_FORMAT = "YYYY-MM-DD";

    assertEquals("Format java date string", "01/Feb/2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "j/dd/MMM/YYYY"));
    //Upper case
    assertEquals("Format java date string", "01/FEB/2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "J/dd/MMM/YYYY"));
    //Lower case
    assertEquals("Format java date string", "01/feb/2014", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "jl/dd/MMM/YYYY"));

    //Single quotes and literal
    assertEquals("Format java date string", "1 o' the month", StringFormatter.formatDateString(DATE_STRING_XML, XML_DATE_FORMAT, "j/d 'o'' the month'"));
  }

  @Test
   public void testOrdinalDateFormat() {
    final String JAVA_INPUT_DATE_FORMAT_MASK = "YYYY-MM-DD";

    final String JAVA_PRESERVE_CASE_ORDINAL_MASK = "j/d{th} MMMM YYYY";
    // Longest month for preserve case
    assertEquals("Format date string", "1st January 2014", StringFormatter.formatDateString("2014-01-01", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "2nd January 2014", StringFormatter.formatDateString("2014-01-02", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "3rd January 2014", StringFormatter.formatDateString("2014-01-03", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "4th January 2014", StringFormatter.formatDateString("2014-01-04", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "5th January 2014", StringFormatter.formatDateString("2014-01-05", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "6th January 2014", StringFormatter.formatDateString("2014-01-06", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "7th January 2014", StringFormatter.formatDateString("2014-01-07", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "8th January 2014", StringFormatter.formatDateString("2014-01-08", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "9th January 2014", StringFormatter.formatDateString("2014-01-09", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "10th January 2014", StringFormatter.formatDateString("2014-01-10", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "11th January 2014", StringFormatter.formatDateString("2014-01-11", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "12th January 2014", StringFormatter.formatDateString("2014-01-12", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "13th January 2014", StringFormatter.formatDateString("2014-01-13", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "14th January 2014", StringFormatter.formatDateString("2014-01-14", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "15th January 2014", StringFormatter.formatDateString("2014-01-15", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "16th January 2014", StringFormatter.formatDateString("2014-01-16", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "17th January 2014", StringFormatter.formatDateString("2014-01-17", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "18th January 2014", StringFormatter.formatDateString("2014-01-18", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "19th January 2014", StringFormatter.formatDateString("2014-01-19", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "20th January 2014", StringFormatter.formatDateString("2014-01-20", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "21st January 2014", StringFormatter.formatDateString("2014-01-21", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "22nd January 2014", StringFormatter.formatDateString("2014-01-22", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "23rd January 2014", StringFormatter.formatDateString("2014-01-23", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "24th January 2014", StringFormatter.formatDateString("2014-01-24", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "25th January 2014", StringFormatter.formatDateString("2014-01-25", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "26th January 2014", StringFormatter.formatDateString("2014-01-26", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "27th January 2014", StringFormatter.formatDateString("2014-01-27", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "28th January 2014", StringFormatter.formatDateString("2014-01-28", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "29th January 2014", StringFormatter.formatDateString("2014-01-29", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "30th January 2014", StringFormatter.formatDateString("2014-01-30", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "31st January 2014", StringFormatter.formatDateString("2014-01-31", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));

    final String JAVA_LOWER_CASE_ORDINAL_MASK = "jl/d{th} MMMM YYYY";
    assertEquals("Format date string", "1st january 2014", StringFormatter.formatDateString("2014-01-01", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_LOWER_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "2nd january 2014", StringFormatter.formatDateString("2014-01-02", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_LOWER_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "3rd january 2014", StringFormatter.formatDateString("2014-01-03", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_LOWER_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "4th january 2014", StringFormatter.formatDateString("2014-01-04", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_LOWER_CASE_ORDINAL_MASK));

    // Test upper case
    final String JAVA_UPPER_CASE_ORDINAL_MASK = "J/d{th} MMMM YYYY";
    assertEquals("Format date string", "1ST JANUARY 2014", StringFormatter.formatDateString("2014-01-01", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_UPPER_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "2ND JANUARY 2014", StringFormatter.formatDateString("2014-01-02", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_UPPER_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "3RD JANUARY 2014", StringFormatter.formatDateString("2014-01-03", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_UPPER_CASE_ORDINAL_MASK));
    assertEquals("Format date string", "4TH JANUARY 2014", StringFormatter.formatDateString("2014-01-04", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_UPPER_CASE_ORDINAL_MASK));
  }

  @Test
  public void testOrdinalDateTimeFormat() {
    final String JAVA_INPUT_DATE_FORMAT_MASK = "YYYY-MM-DD HH24:MI:SS";

    final String JAVA_PRESERVE_CASE_ORDINAL_MASK = "j/d{th} MMMM YYYY HH:mm:ss";
    // Longest month for preserve case
    assertEquals("Format datetime string", "1st January 2014 14:00:00", StringFormatter.formatDateString("2014-01-01 14:00:00", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format datetime string", "1st January 2014 09:52:00", StringFormatter.formatDateString("2014-01-01 09:52:00", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));
    assertEquals("Format datetime string", "1st January 2014 20:00:46", StringFormatter.formatDateString("2014-01-01 20:00:46", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_PRESERVE_CASE_ORDINAL_MASK));

    final String JAVA_LOWER_CASE_ORDINAL_MASK = "jl/d{th} MMMM YYYY HH:mm:ss";
    assertEquals("Format datetime string", "1st january 2014 14:01:01", StringFormatter.formatDateString("2014-01-01 14:01:01", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_LOWER_CASE_ORDINAL_MASK));

    // Test upper case
    final String JAVA_UPPER_CASE_ORDINAL_MASK = "J/d{th} MMMM YYYY HH:mm:ss";
    assertEquals("Format datetime string", "1ST JANUARY 2014 14:01:01", StringFormatter.formatDateString("2014-01-01 14:01:01", JAVA_INPUT_DATE_FORMAT_MASK, JAVA_UPPER_CASE_ORDINAL_MASK));
  }
}

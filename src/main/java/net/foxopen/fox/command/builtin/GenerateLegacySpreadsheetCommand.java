package net.foxopen.fox.command.builtin;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.StringUtil;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.GeneratorDestination;
import net.foxopen.fox.command.util.GeneratorDestinationUtils;
import net.foxopen.fox.command.util.OutputStreamGenerator;
import net.foxopen.fox.command.util.WriterGenerator;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.thread.ActionRequestContext;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class GenerateLegacySpreadsheetCommand extends BuiltInCommand {

  private static final Set gSchemaIntegerTypeNamesSet = new HashSet(Arrays.asList(StringUtil.commaDelimitedListToIterableString("int,integer,long,nonPositiveInteger,nonNegativeInteger,negativeInteger," +
    "int,short,byte,unsignedLong,positiveInteger,unsignedInt,unsignedShort," +
    "unsignedByte")));
  private static final Set gSchemaRealTypeNamesSet = new HashSet(Arrays.asList(StringUtil.commaDelimitedListToIterableString("decimal,float,double")));

  private static final Set gSchemaDateTimeTypeNamesSet = new HashSet(Arrays.asList(StringUtil.commaDelimitedListToIterableString("date,time,dateTime")));

  public static final String OUTPUT_TYPE_CSV = "CSV";
  public static final String OUTPUT_TYPE_XLS = "XLS";

  private final String mOutputType;
  private final GeneratorDestination mGeneratorDestination;
  private final Sheet mSheets[];

  public GenerateLegacySpreadsheetCommand(DOM pMarkupDOM) throws ExDoSyntax {
    super(pMarkupDOM);

    mOutputType = pMarkupDOM.getAttrOrNull("output-type");

    String lContentType = ("CSV".equalsIgnoreCase(mOutputType) ? "text/csv" : "application/vnd.ms-excel");
    //String popupWindowName = "TODO"; //TODO getUniqueWindowName();

    mGeneratorDestination = GeneratorDestinationUtils.getDestinationFromGenerateCommandMarkup(pMarkupDOM, mOutputType.toLowerCase(), lContentType);

    mSheets = parseSheets(pMarkupDOM);
  }

  private final Sheet[] parseSheets(DOM pMarkupDOM) {

    // Look for embedded sheet entries, in the case of Excel output formats.
    DOMList sheetListElems = pMarkupDOM.getULByLocalName("generate-sheet");
    List<Sheet> sheetList = new ArrayList<>(sheetListElems.getLength());
    for (int s = 0; s < sheetListElems.getLength(); s++) {
      DOM sheetElem = sheetListElems.item(s);
      Sheet sheet = new Sheet();
      sheetList.add(sheet);

      String sheetName = XFUtil.nvl(sheetElem.getAttrOrNull("name"), "Sheet-" + s);
      String rowExpr = sheetElem.getAttrOrNull("row-expr");
      String showHeadersExpr = sheetElem.getAttrOrNull("show-headers");

      if (rowExpr == null) {
        throw new ExInternal("fm:generate: A sheet (number " + (s + 1) + ") has no row expression attribute - please specify one in order to " + "supply data for the worksheet.");
      }
      sheet.setName(sheetName);
      sheet.setRowExpression(rowExpr);
      sheet.setShowHeadersExpr(showHeadersExpr);

      DOMList columnListElems = sheetElem.getULByLocalName("generate-column");
      List<Sheet.Column> columnList = new ArrayList<>(columnListElems.getLength());
      for (int c = 0; c < columnListElems.getLength(); c++) {
        DOM columnElem = columnListElems.item(c);

        Sheet.Column column = new Sheet.Column();
        columnList.add(column);

        String columnName = XFUtil.nvl(columnElem.getAttrOrNull("name"), "Column-" + s);
        String columExpr = columnElem.getAttrOrNull("column-expr");
        String type = columnElem.getAttrOrNull("datatype");
        String inputFormat = columnElem.getAttrOrNull("input-format");
        String outputFormat = columnElem.getAttrOrNull("output-format");
        String visibleExpr = columnElem.getAttrOrNull("visible-expr");

        if (columExpr == null) {
          throw new ExInternal("fm:generate: A sheet column (sheet=" + (s + 1) + ", column=" + (c + 1) + ") has no column expression attribute - please specify one in order to " + "supply data for the worksheet column.");
        }
        column.setName(columnName);
        column.setColumnExpression(columExpr);
        column.setType(type);
        column.setInputFormatSpecification(inputFormat);
        column.setOutputFormatSpecification(outputFormat);
        column.setVisibleExpression(visibleExpr);
      }
      sheet.setColumns(columnList);
    }

    return sheetList.toArray(new Sheet[sheetList.size()]);
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    if (OUTPUT_TYPE_CSV.equalsIgnoreCase(mOutputType)) {
      WriterGenerator lCSVGenerator = getCSVGenerator(pRequestContext);
      mGeneratorDestination.generateToWriter(pRequestContext, lCSVGenerator);
    }
    else if (OUTPUT_TYPE_XLS.equalsIgnoreCase(mOutputType)) {
      OutputStreamGenerator lXLSGenerator = getXLSGenerator(pRequestContext);
      mGeneratorDestination.generateToOutputStream(pRequestContext, lXLSGenerator);
    }
    else {
      throw new ExInternal("Generate legacy spreadsheet must have an output type of CSV or XLS");
    }

    return XDoControlFlowContinue.instance();
  }

  private WriterGenerator getCSVGenerator(final ActionRequestContext pRequestContext) {

    return new WriterGenerator() {

      @Override
      public void writeOutput(Writer pWriter) throws IOException {
        ContextUElem lContextUElem = pRequestContext.getContextUElem();
        try {
          for (int s = 0; s < mSheets.length; s++) {
            boolean visibleColumns[] = new boolean[mSheets[s].getColumns().length];
            boolean lShowHeaders;
            if (XFUtil.isNull(mSheets[s].getShowHeadersExpr())) {
              //default is show for CSVs
              lShowHeaders = true;
            }
            else {
              lShowHeaders = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mSheets[s].getShowHeadersExpr());
            }
            // Establish visibilities before outputting headers
            for (int c = 0; c < mSheets[s].getColumns().length; c++) {
              Sheet.Column column = mSheets[s].getColumns()[c];
              visibleColumns[c] = column.getVisibleExpression() == null || lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), column.getVisibleExpression());
            }

            // Generate column headers
            if (lShowHeaders) {
              for (int c = 0; c < mSheets[s].getColumns().length; c++) {
                Sheet.Column column = mSheets[s].getColumns()[c];
                String cellValue = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), column.getNameExpr());

                if (visibleColumns[c]) {
                  pWriter.write(escapeCSVFieldValue(cellValue));
                  if (c < mSheets[s].getColumns().length - 1)
                    pWriter.write(",");
                }
              }
              pWriter.write(System.getProperty("line.separator", "\r\n"));
            }

            DOMList rowElems = lContextUElem.extendedXPathUL(mSheets[s].getRowExpression(), ContextUElem.ATTACH);
            for (int r = 0; r < rowElems.getLength(); r++) {
              DOM rowElem = rowElems.item(r);
              for (int c = 0; c < mSheets[s].getColumns().length; c++) {
                if (!visibleColumns[c])
                  continue;

                Sheet.Column column = mSheets[s].getColumns()[c];
                XPathResult cellValue = lContextUElem.extendedXPathResult(rowElem, column.getColumnExpression());

                pWriter.write(getOrConvertCSVCellValue(pRequestContext.getCurrentModule(), cellValue, column));
                if (c < mSheets[s].getColumns().length - 1)
                  pWriter.write(",");
              }
              pWriter.write(System.getProperty("line.separator", "\r\n"));
            }
          }

//          pWriter.close();
        }
        catch (ExActionFailed | ExBadPath e) {
          throw new ExInternal("XPath evaluation failed during CSV generation", e);
        }
      }
    };
  }

  private OutputStreamGenerator getXLSGenerator(final ActionRequestContext pRequestContext) {
    return new OutputStreamGenerator() {

      @Override
      public void writeOutput(OutputStream pOutputStream) throws IOException {
        ContextUElem pContextUElem = pRequestContext.getContextUElem();

        try {
          HSSFWorkbook wb = new HSSFWorkbook();
          for (int s = 0; s < mSheets.length; s++) {
            HSSFSheet sheet = wb.createSheet(pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), mSheets[s].getName()));
            boolean visibleColumns[] = new boolean[mSheets[s].getColumns().length];
            boolean lShowHeaders;
            if (XFUtil.isNull(mSheets[s].getShowHeadersExpr())) {
              //default is hide for XLSs
              lShowHeaders = false;
            }
            else {
              lShowHeaders = pContextUElem.extendedXPathBoolean(pContextUElem.attachDOM(), mSheets[s].getShowHeadersExpr());
            }

            //Work out visibilities before generation headers
            for (int c = 0; c < mSheets[s].getColumns().length; c++) {
              Sheet.Column column = mSheets[s].getColumns()[c];
              visibleColumns[c] = column.getVisibleExpression() == null || pContextUElem.extendedXPathBoolean(pContextUElem.attachDOM(), column.getVisibleExpression());
            }

            int rowOffset = 0;
            // Generate column headers
            HSSFRow headerRow = null;
            if (lShowHeaders) {
              headerRow = sheet.createRow(0);
              rowOffset++;

              for (int c = 0, visCols = 0; c < mSheets[s].getColumns().length; c++) {
                if (!visibleColumns[c])
                  continue;

                Sheet.Column column = mSheets[s].getColumns()[c];
                String cellValue = pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), column.getNameExpr());

                HSSFCell cell = headerRow.createCell((short) visCols);
                cell.setCellValue(cellValue);
                visCols++;
              }
            }

            DOMList rowElems = pContextUElem.extendedXPathUL(mSheets[s].getRowExpression(), ContextUElem.ATTACH);
            for (int r = 0; r < rowElems.getLength(); r++) {
              DOM rowElem = rowElems.item(r);
              HSSFRow row = sheet.createRow(r + rowOffset);
              for (int c = 0, visCols = 0; c < mSheets[s].getColumns().length; c++) {
                if (!visibleColumns[c])
                  continue;

                Sheet.Column column = mSheets[s].getColumns()[c];
                XPathResult cellValue = pContextUElem.extendedXPathResult(rowElem, column.getColumnExpression());

                HSSFCell cell = row.createCell((short) visCols);
                setXLSCellValue(pRequestContext.getCurrentModule(), wb, cell, cellValue, column);
                visCols++;
                //cell.setCellValue(cellValue);
              }
            }
          }

          wb.write(pOutputStream);
        }
        catch(ExActionFailed | ExBadPath e) {
          throw new ExInternal("XPath evaluation failed during XLS generation", e);
        }
      }
    };
  }

  private String getOrConvertCSVCellValue(Mod pModule, XPathResult cellValue, Sheet.Column column) throws ExBadPath {
    String cellStrValue = cellValue.asString();
    DOM resultDOM = cellValue.asResultDOMOrNull();
    NodeInfo nodeInfo = resultDOM != null ? pModule.getNodeInfo(resultDOM) : null;
    String columnDataType = column.getType();

    if (cellStrValue == null)
      cellStrValue = "";

    //------------------------------------------------------------------------
    // If we don't have the data type specified directly for the column, try
    // and get it from the module specification, if applicable.
    //------------------------------------------------------------------------
    if (columnDataType == null && nodeInfo != null) {
      String moduleDataType = nodeInfo.getDataType();
      int colonPos = moduleDataType.indexOf(":");
      if (colonPos >= 0)
        moduleDataType = moduleDataType.substring(colonPos + 1);

      if (gSchemaIntegerTypeNamesSet.contains(moduleDataType))
        columnDataType = "integer";
      else if (gSchemaRealTypeNamesSet.contains(moduleDataType))
        columnDataType = "real";
      else if (gSchemaDateTimeTypeNamesSet.contains(moduleDataType))
        columnDataType = moduleDataType.toLowerCase(); // Bit of a cheat
    }

    //------------------------------------------------------------------------
    // Convert the cell value.
    //------------------------------------------------------------------------
    if ("integer".equalsIgnoreCase(columnDataType) || "real".equalsIgnoreCase(columnDataType)) {
      Number number = null;
      try {
        // Parse incoming value
        number = cellValue.asNumber();

        // Parse format outgoing value
        String excelFormatPattern = XFUtil.nvl(column.getOutputFormatSpecification(), ("real".equalsIgnoreCase(columnDataType) ? "0.00" : "0"));
        DecimalFormat df = new DecimalFormat(excelFormatPattern);
        cellStrValue = df.format(number.doubleValue());
      }
      catch (Throwable ignoreTh) {
      }

    }
    else if ("boolean".equalsIgnoreCase(columnDataType)) {
      Number number = null;
      try {
        cellStrValue = cellValue.asBoolean() ? "true" : "false";
      }
      catch (Throwable ignoreTh) {
      }
    }
    else if ("date".equalsIgnoreCase(columnDataType) || "time".equalsIgnoreCase(columnDataType) || "datetime".equalsIgnoreCase(columnDataType)) {
      try {
        // Parse incoming value.
        // This assumes an incoming XML date format.
        String inputDateFormat = column.getInputFormatSpecification();

        if (inputDateFormat == null) {
          if ("date".equalsIgnoreCase(columnDataType))
            inputDateFormat = "yyyy-MM-dd";
          else if ("time".equalsIgnoreCase(columnDataType))
            inputDateFormat = "HH:mm:ss";
          else if ("datetime".equalsIgnoreCase(columnDataType))
            inputDateFormat = "yyyy-MM-dd'T'HH:mm:ss";
          else
            inputDateFormat = "yyyy-MM-dd";
        }

        SimpleDateFormat df = new SimpleDateFormat(inputDateFormat);

        Date inputDate = df.parse(cellValue.asString());

        // Parse format outgoing value
        String excelFormatPattern = column.getOutputFormatSpecification();

        if (excelFormatPattern == null) {
          if ("date".equalsIgnoreCase(columnDataType))
            excelFormatPattern = "dd-MMM-YYYY";
          else if ("time".equalsIgnoreCase(columnDataType))
            excelFormatPattern = "hh:mm:ss a";
          else if ("datetime".equalsIgnoreCase(columnDataType))
            excelFormatPattern = "dd-MMM-YYYY hh:mm:ss a";
          else
            excelFormatPattern = "dd-MMM-YYYY";
        }

        df.applyPattern(excelFormatPattern);
        cellStrValue = df.format(inputDate);
      }
      catch (Throwable ignoreTh) {
      }
    }

    return escapeCSVFieldValue(cellStrValue);
  }

  private String escapeCSVFieldValue(String cellStrValue) {
    //------------------------------------------------------------------------
    // Escape or detect any CSV reserved charcater entities.
    // 1) Double quotes should be escaped with to sets of double quotes.
    // 2) Commas, for obvious reasons.
    //
    // If we did need to escape entities, place the whole cell value in
    // double quotes.
    //
    // Rules Reference: http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm
    //------------------------------------------------------------------------
    boolean containsCommas = cellStrValue.indexOf(',') >= 0;
    boolean containsDoubleQuotes = cellStrValue.indexOf('\"') >= 0;
    boolean containsLeadingOrTrailingSpaces = cellStrValue.startsWith(" ") || cellStrValue.endsWith(" ");
    boolean containsLineBreaks = cellStrValue.indexOf("\r") > 0 || cellStrValue.indexOf("\n") > 0;
    boolean requiresDoubleQuoteDelimiters = containsCommas || containsDoubleQuotes || containsLeadingOrTrailingSpaces || containsLineBreaks;

    if (containsDoubleQuotes) {
      cellStrValue = StringUtil.replace(cellStrValue, "\"", "\"\"");
    }

    if (requiresDoubleQuoteDelimiters)
      cellStrValue = "\"" + cellStrValue + "\"";

    return cellStrValue;
  }

  private void setXLSCellValue(Mod pModule, HSSFWorkbook workBook, HSSFCell cell, XPathResult cellValue, Sheet.Column column) throws ExBadPath {
    DOM resultDOM = cellValue.asResultDOMOrNull();
    NodeInfo nodeInfo = resultDOM != null ? pModule.getNodeInfo(resultDOM) : null;
    String columnDataType = column.getType();

    //------------------------------------------------------------------------
    // If we don't have the data type specified directly for the column, try
    // and get it from the module specification, if applicable.
    //------------------------------------------------------------------------
    if (columnDataType == null && nodeInfo != null) {
      String moduleDataType = nodeInfo.getDataType();
      int colonPos = moduleDataType.indexOf(":");
      if (colonPos >= 0)
        moduleDataType = moduleDataType.substring(colonPos + 1);

      if (gSchemaIntegerTypeNamesSet.contains(moduleDataType))
        columnDataType = "integer";
      else if (gSchemaRealTypeNamesSet.contains(moduleDataType))
        columnDataType = "real";
      else if (gSchemaDateTimeTypeNamesSet.contains(moduleDataType))
        columnDataType = moduleDataType.toLowerCase(); // Bit of a cheat
    }

    //------------------------------------------------------------------------
    // Convert the cell value.
    //------------------------------------------------------------------------
    if ("integer".equalsIgnoreCase(columnDataType) || "real".equalsIgnoreCase(columnDataType)) {
      Number number = null;
      try {
        // Parse incoming value
        number = cellValue.asNumber();
        cell.setCellValue(number.doubleValue());

        // Parse format outgoing value
        String excelFormatPattern = XFUtil.nvl(column.getOutputFormatSpecification(), ("real".equalsIgnoreCase(columnDataType) ? "0.00" : "0"));
        HSSFCellStyle cellStyle = workBook.createCellStyle();
        cellStyle.setDataFormat(workBook.createDataFormat().getFormat(excelFormatPattern));
        cell.setCellStyle(cellStyle);
      }
      catch (Throwable ignoreTh) {
        cell.setCellValue(cellValue.asString());
      }

    }
    else if ("boolean".equalsIgnoreCase(columnDataType)) {
      try {
        cell.setCellValue(cellValue.asBoolean() ? "true" : "false");
      }
      catch (Throwable ignoreTh) {
      }
    }
    else if ("date".equalsIgnoreCase(columnDataType) || "time".equalsIgnoreCase(columnDataType) || "datetime".equalsIgnoreCase(columnDataType)) {
      try {
        // Parse incoming value.
        // This assumes an incoming XML date format.
        String inputDateFormat = column.getInputFormatSpecification();

        if (inputDateFormat == null) {
          if ("date".equalsIgnoreCase(columnDataType))
            inputDateFormat = "yyyy-MM-dd";
          else if ("time".equalsIgnoreCase(columnDataType))
            inputDateFormat = "HH:mm:ss";
          else if ("datetime".equalsIgnoreCase(columnDataType))
            inputDateFormat = "yyyy-MM-dd'T'HH:mm:ss";
          else
            inputDateFormat = "yyyy-MM-dd";
        }

        SimpleDateFormat df = new SimpleDateFormat(inputDateFormat);

        cell.setCellValue(df.parse(cellValue.asString()));

        // Parse format outgoing value
        String excelFormatPattern = column.getOutputFormatSpecification();

        if (excelFormatPattern == null) {
          if ("date".equalsIgnoreCase(columnDataType))
            excelFormatPattern = "d-mmm-yyyy";
          else if ("time".equalsIgnoreCase(columnDataType))
            excelFormatPattern = "h:mm:ss AM/PM";
          else if ("datetime".equalsIgnoreCase(columnDataType))
            excelFormatPattern = "d-mmm-yyyy h:mm:ss AM/PM";
          else
            excelFormatPattern = "d-mmm-yyyy";
        }

        HSSFCellStyle cellStyle = workBook.createCellStyle();
        cellStyle.setDataFormat(workBook.createDataFormat().getFormat(excelFormatPattern));
        cell.setCellStyle(cellStyle);
      }
      catch (Throwable ignoreTh) {
        cell.setCellValue(cellValue.asString());
      }
    }
    else {
      // set it as a string, by default
      cell.setCellValue(cellValue.asString());
    }
  }

  private static class Sheet {
    protected String name;
    protected String rowExpr;
    protected Column columns[];
    protected String showHeadersExpr;

    public static class Column {
      protected String name;
      protected String columnExpression;
      protected String type;
      protected String inputFormatSpecification;
      protected String outputFormatSpecification;
      protected String visibleExpression;

      public Column() {
      }

      public String getNameExpr() {
        return name;
      }

      public void setName(String name) {
        this.name = name;
      }

      public String getColumnExpression() {
        return columnExpression;
      }

      public void setColumnExpression(String columnExpression) {
        this.columnExpression = columnExpression;
      }

      public String getType() {
        return type;
      }

      public void setType(String type) {
        this.type = type;
      }

      public String getInputFormatSpecification() {
        return inputFormatSpecification;
      }

      public void setInputFormatSpecification(String inputFormatSpecification) {
        this.inputFormatSpecification = inputFormatSpecification;
      }

      public String getOutputFormatSpecification() {
        return outputFormatSpecification;
      }

      public void setOutputFormatSpecification(String outputFormatSpecification) {
        this.outputFormatSpecification = outputFormatSpecification;
      }

      public String getVisibleExpression() {
        return visibleExpression;
      }

      public void setVisibleExpression(String visibleExpression) {
        this.visibleExpression = visibleExpression;
      }
    }

    public Sheet() {
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getRowExpression() {
      return rowExpr;
    }

    public void setRowExpression(String rowExpr) {
      this.rowExpr = rowExpr;
    }

    public Column[] getColumns() {
      return columns;
    }

    public void setColumns(Column columns[]) {
      this.columns = columns;
    }

    public void setColumns(List columns) {
      this.columns = (Column[]) columns.toArray(new Column[columns.size()]);
    }

    public void setShowHeadersExpr(String showHeadersExpr) {
      this.showHeadersExpr = showHeadersExpr;
    }

    public String getShowHeadersExpr() {
      return showHeadersExpr;
    }
  }

  //  public static class Factory
  //  implements CommandFactory {
  //
  //    @Override
  //    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
  //      return new xxxCommand(pMarkupDOM);
  //    }
  //
  //    @Override
  //    public Collection<String> getCommandElementNames() {
  //      return Collections.singleton();
  //    }
  //  }
}

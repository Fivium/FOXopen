package net.foxopen.fox.configuration.resourcemaster;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExParser;

import java.util.List;

/**
 * Manages the construction of resource queries for an app.
 */
public class AppResourceQueryDefinitions {

  private final ParsedStatement mResourceNamesParsedStatement;
  private final ParsedStatement mResourceTableParsedStatement;

  public AppResourceQueryDefinitions(String pAppMnem, List<String> pResourceTableList) throws ExApp, ExFoxConfiguration {
    mResourceNamesParsedStatement = processResourceNamesParsedStatement(pResourceTableList, pAppMnem);
    mResourceTableParsedStatement = processResourceTableStatement(pResourceTableList, pAppMnem);
  }

  public ParsedStatement getResourceNamesParsedStatement() {
    return mResourceNamesParsedStatement;
  }

  public ParsedStatement getResourceTableParsedStatement() {
    return mResourceTableParsedStatement;
  }

  private ParsedStatement processResourceNamesParsedStatement(List<String> pResourceTableList, String pAppMnem) throws ExApp {

    StringBuffer lResourceNamesQuery = new StringBuffer();

    if(pResourceTableList.size()>1) {
      lResourceNamesQuery.append("SELECT name\nFROM (\n");
    }

    int lResourceTableCount = 0;
    for (String pResourceTable : pResourceTableList) {
      if(XFUtil.isNull(pResourceTable)) {
        throw new ExApp("Bad Application resource-table");
      }

      lResourceNamesQuery.append("SELECT name FROM "+pResourceTable+"\n");

      if(lResourceTableCount != pResourceTableList.size()-1) {
        lResourceNamesQuery.append("UNION\n");
      }

      lResourceTableCount++;
    }

    if(pResourceTableList.size()>1) {
      lResourceNamesQuery.append(")");
    }

    try {
      return StatementParser.parse(lResourceNamesQuery.toString(), "App Resource Names");
    }
    catch (ExParser e) {
      throw new ExInternal("Failed to parse resource names query on app " + pAppMnem, e);
    }
  }

  // Build fox components query
  private ParsedStatement processResourceTableStatement(List<String> pResourceTableList, String pAppMnem) throws ExApp, ExFoxConfiguration {
    if (pResourceTableList == null) {
      throw new ExFoxConfiguration("The resource tables provided was null. ");
    }

    // Construct query from list of resource tables (don't validate here)
    StringBuilder lComponentsQuery = new StringBuilder();

    // Add start of query
    lComponentsQuery.append("WITH q1 AS (SELECT :1 bind_p1 FROM  dual)\n");
    if (pResourceTableList.size() > 1) {
      lComponentsQuery.append("SELECT data, bindata, type, name, engine_mirror\nFROM (\n");
    }

    // Add select for each resource table
    int lResourceTableCount = 0;
    for (String lTableName : pResourceTableList) {

      if (lResourceTableCount != 0) {
        lComponentsQuery.append("UNION ALL\n");
      }

      lComponentsQuery.append("SELECT data, bindata, type, name, engine_mirror, ");
      lComponentsQuery.append(lResourceTableCount);
      lComponentsQuery.append(" pos FROM ").append(lTableName).append(" WHERE name = (SELECT bind_p1 from q1)\n");
      lResourceTableCount++;
    }

    // Add ending of query
    if (pResourceTableList.size() > 1) {
      lComponentsQuery.append("ORDER BY pos\n)\nWHERE ROWNUM <=1");
    }

    ParsedStatement lResourceTableParsedStatement;
    try {
      lResourceTableParsedStatement = StatementParser.parse(lComponentsQuery.toString(), "App Resource Query");
    }
    catch (ExParser e) {
      throw new ExInternal("Cannot parse resource table query for app " + pAppMnem);
    }

    return lResourceTableParsedStatement;
  }
}

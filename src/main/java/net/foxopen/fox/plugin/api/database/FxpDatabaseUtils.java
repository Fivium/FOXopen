package net.foxopen.fox.plugin.api.database;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.plugin.api.database.parser.FxpParsedStatement;

public class FxpDatabaseUtils {

  //PN TODO is this the best way to encourage proper sql management?
  public static FxpParsedStatement parseStatement(String pStatement, String pPurpose)
  throws ExParser {
    return StatementParser.parse(pStatement, pPurpose);
  }

  public static FxpParsedStatement parseStatementSafely(String pStatement, String pPurpose) {
    return StatementParser.parseSafely(pStatement, pPurpose);
  }

  public static FxpUConBindMap createBindMap() { return new UConBindMap(); }

  public static Object bindOutXML() { return UCon.bindOutXML(); }

  public static Object bindOutClob() { return UCon.bindOutClob(); }

  public static Object bindOutBlob() { return UCon.bindOutBlob(); }

  public static Object bindOutString() { return UCon.bindOutString(); }
}

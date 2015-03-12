package net.foxopen.fox.sql;

import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.plugin.api.database.parser.FxpParsedStatement;
import net.foxopen.fox.plugin.api.database.parser.FxpSQLManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;


/**
 * Manages loading and caching of internal SQL statements.
 */
public class SQLManager extends FxpSQLManager {

  private static final SQLManager INSTANCE = new SQLManager();

  private SQLManager() {}

  public static SQLManager instance() {
    return INSTANCE;
  }

  /** Map of SQL filenames to corresponding statements. */
  private ParsedStatement readSQLFile(String pSQLFilename, Class pClassForClassPath) {

    String lSQL;
    try {
      InputStream lSQLFileStream = pClassForClassPath.getResourceAsStream(pSQLFilename);
      if (lSQLFileStream == null) {
        throw new ExInternal("Failed to read SQL file " + pSQLFilename);
      }
      lSQL = IOUtils.toString(lSQLFileStream);
    }
    catch (IOException e) {
      throw new ExInternal("Failed to read SQL file " + pSQLFilename);
    }

    lSQL = replaceSQLSubstitutionVariables(lSQL);

    ParsedStatement lParsedStatement;
    try {
      lParsedStatement = StatementParser.parse(lSQL, pSQLFilename + " (internal)");
    }
    catch (ExParser e) {
      throw new ExInternal("Failed to parse internal SQL file " + pSQLFilename);
    }

    FoxCache<String, ParsedStatement> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.INTERNAL_SQL_STATEMENTS);
    lFoxCache.put(pSQLFilename, lParsedStatement);

    return lParsedStatement;
  }

  public static String replaceSQLSubstitutionVariables(String pSQLText) {
    pSQLText = pSQLText.replace("${schema.fox}", FoxGlobals.getInstance().getFoxBootConfig().getMainDatabaseUsername());
    //TODO PN make this configurable
    pSQLText = pSQLText.replace("${schema.auth}", "securemgr");

    return pSQLText;
  }

  /**
   * Gets the SQL statement for the given filename and classpath (as determined by the provided Class). The raw statement
   * string has substitution variables replaced and is parsed and cached.
   * @param pSQLFilename Filename of the .sql file in the classpath.
   * @param pClassForClassPath Classpath to load SQL file from.
   * @return The parsed statement representation of the given SQL file.
   */
  public ParsedStatement getStatement(String pSQLFilename, Class pClassForClassPath) {
    return getStatementInternal(pSQLFilename, pClassForClassPath);
  }

  private ParsedStatement getStatementInternal(String pSQLFilename, Class pClassForClassPath) {
    FoxCache<String, ParsedStatement> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.INTERNAL_SQL_STATEMENTS);
    ParsedStatement lStatement = lFoxCache.get(pSQLFilename);
    if (lStatement == null) {
      lStatement = readSQLFile(pSQLFilename, pClassForClassPath);
    }

    return lStatement;
  }

  /**
   * Gets the SQL statement for the given filename and classpath (as determined by the provided Class). The raw statement
   * string has substitution variables replaced and is parsed and cached.
   * @param pSQLFilename Filename of the .sql file in the classpath.
   * @param pClassForClassPath Classpath to load SQL file from.
   * @return The parsed statement representation of the given SQL file.
   */
  public FxpParsedStatement getFxpStatement(String pSQLFilename, Class pClassForClassPath) {
    return getStatementInternal(pSQLFilename, pClassForClassPath);
  }

  public String getFoxSchemaName() {
    return FoxGlobals.getInstance().getFoxBootConfig().getMainDatabaseUsername();
  }

  public void flushCache() {
    FoxCache<String, ParsedStatement> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.INTERNAL_SQL_STATEMENTS);
    lFoxCache.clear();
  }
}

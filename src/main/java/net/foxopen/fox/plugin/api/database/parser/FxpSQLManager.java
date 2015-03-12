package net.foxopen.fox.plugin.api.database.parser;

import net.foxopen.fox.sql.SQLManager;

public abstract class FxpSQLManager {

  public static FxpSQLManager instance (){
     return SQLManager.instance();
  };

  public abstract FxpParsedStatement getStatement(String pSQLFilename, Class pClassForClassPath);

  public abstract String getFoxSchemaName();

  public abstract void flushCache();
}
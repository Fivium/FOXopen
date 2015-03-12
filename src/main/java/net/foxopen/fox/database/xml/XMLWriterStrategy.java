package net.foxopen.fox.database.xml;

import java.sql.SQLException;
import java.sql.SQLXML;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;


public interface XMLWriterStrategy {
  
  public SQLXML writeToObject(UCon pUCon, DOM pDOM)
  throws SQLException;
  
}

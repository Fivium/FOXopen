package net.foxopen.fox.thread.storage;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StorageLocationTest {
  
  private static final String TARGET_SELECT_STRING = "SELECT xml_data\n" + 
  "FROM table_name\n";

  @Test
  public void testBasicStatement_1() {
    
    String lStatement = "SELECT xml_data\n" + 
    "FROM table_name\n" + 
    "FOR UPDATE NOWAIT";
    
    assertEquals("FOR UPDATE NOWAIT removed from statement", TARGET_SELECT_STRING, StorageLocation.removeForUpdateClause(lStatement));
  }
  
  @Test
  public void testBasicStatement_2() {
    
    String lStatement = "SELECT xml_data\n" + 
    "FROM table_name\n" + 
    "FOR UPDATE";
    
    assertEquals("FOR UPDATE removed from statement", TARGET_SELECT_STRING, StorageLocation.removeForUpdateClause(lStatement));
  }
  
  @Test
  public void testBasicStatement_3() {
    
    String lStatement = "SELECT xml_data\n" + 
    "FROM table_name\n" + 
    "FOR UPDATE OF xml_data";
    
    assertEquals("FOR UPDATE OF xml_data removed from statement", TARGET_SELECT_STRING, StorageLocation.removeForUpdateClause(lStatement));
  }
  
  @Test
  public void testBasicStatement_4() {
    
    String lStatement = "SELECT xml_data\n" + 
    "FROM table_name\n" + 
    "FOR UPDATE OF xml_data NOWAIT";
    
    assertEquals("FOR UPDATE OF xml_data NOWAIT removed from statement", TARGET_SELECT_STRING, StorageLocation.removeForUpdateClause(lStatement));
  }
  
  @Test
  public void testBadlyFormattedStatement_1() {
    
    String lStatement = "SELECT xml_data\n" + 
    "FROM table_name\n" + 
    "for      update  of     \"xml_data\"  \n" + 
    "  nowait";
    
    assertEquals("FOR UPDATE OF xml_data NOWAIT removed from statement", TARGET_SELECT_STRING, StorageLocation.removeForUpdateClause(lStatement));
  }
  
  @Test
  public void testBadlyFormattedStatement_2() {
    
    String lStatement = "SELECT xml_data\n" + 
    "FROM table_name\n" + 
    "FOR NO UPDATE OF xml_data NOWAIT"; //Note string is wrong: FOR _NO_ UPDATE
    
    assertEquals("Statement is unmodified", lStatement, StorageLocation.removeForUpdateClause(lStatement));
  }
}

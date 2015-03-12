package net.foxopen.fox.database.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.foxopen.fox.ex.ExDBSyntax;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExDBTooMany;


/**
 * ResultDeliverer for acquiring a single column from a single row. The object is held in the deliverer and can be retrieved
 * using the {@link #getResult} method. Class implementors define the behaviour for retrieving the result object, asserting type safety.
 * @param <T> Type of object to be retrieved by the deliverer.
 */
public abstract class ScalarResultDeliverer<T>
implements QueryResultDeliverer {
  
  private T mResultObject;

  @Override
  public void deliver(ExecutableQuery pQuery) 
  throws ExDBTooFew, ExDBSyntax, ExDBTooMany {

    ResultSet lResultSet = pQuery.getResultSet();
      
    try {
      if(!lResultSet.next()) {
        throw new ExDBTooFew("ScalarResultDeliverer requires exactly 1 row, got 0 for statement " + pQuery.getParsedStatement().getStatementPurpose());
      }
      else if(lResultSet.getMetaData().getColumnCount() != 1) {
        throw new ExDBSyntax("ScalarResultDeliverer requires exactly 1 column, got " + lResultSet.getMetaData().getColumnCount());
      }
      
      mResultObject = readResultObject(lResultSet);
      
      if(lResultSet.next()) {
        //Null out due to failure
        mResultObject = null;
        throw new ExDBTooMany("ScalarResultDeliverer requires exactly 1 row, got more than 1 for query " + pQuery.getParsedStatement().getStatementPurpose());
      }
      
    }
    catch (SQLException e) {
      //TODO error conversion
      throw new ExDBSyntax("SQL error", e);
    }
  }
  
  protected abstract T readResultObject(ResultSet pResultSet)  
  throws SQLException;

  /**
   * Gets the result object as it was retrieved by this deliverer.
   * @return Result Object.
   */
  public T getResult() {
    return mResultObject;
  }

  @Override
  public boolean closeStatementAfterDelivery() {
    return true;
  }
}

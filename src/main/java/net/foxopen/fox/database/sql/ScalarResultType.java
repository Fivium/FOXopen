package net.foxopen.fox.database.sql;

import net.foxopen.fox.database.sql.out.ResultSetAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dom.DOM;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;


/**
 * Enum-like class representing scalar types which can be retrieved by a {@link ScalarResultDeliverer}. This provides
 * type safety when running queries which select a single result. Note that a ScalarResultDeliverer is stateful and as
 * such cannot be shared across threads as a singleton, hence the overloading pattern seen in this class.
 * @param <T> Class which is returned by the ScalarDeliverer associated with this ScalarResultType.
 */
public abstract class ScalarResultType<T> {

  /** Retrieves an object from the ResultSet without any type conversion. */
  public static final ScalarResultType<Object> SQL_OBJECT = new ScalarResultType<Object>() {
    public ScalarResultDeliverer<Object> getResultDeliverer() { return new SQLObjectDeliverer(); }
  };

  /** Retrieves a String from the ResultSet. */
  public static final ScalarResultType<String> STRING = new ScalarResultType<String>() {
    public ScalarResultDeliverer<String> getResultDeliverer() { return new StringDeliverer(); }
  };

  /** Converts the retrieved object into a DOM (see {@link SQLTypeConverter#getValueAsDOM}). */
  public static final ScalarResultType<DOM> DOM = new ScalarResultType<DOM>() {
    public ScalarResultDeliverer<DOM> getResultDeliverer() { return new DOMDeliverer(); }
  };

  /** Retrieves a Clob from the ResultSet. */
  public static final ScalarResultType<Clob> CLOB = new ScalarResultType<Clob>() {
    public ScalarResultDeliverer<Clob> getResultDeliverer() { return new ClobDeliverer(); }
  };

  /** Retrieves a Blob from the ResultSet. */
  public static final ScalarResultType<Blob> BLOB = new ScalarResultType<Blob>() {
    public ScalarResultDeliverer<Blob> getResultDeliverer() { return new BlobDeliverer(); }
  };

  /** Retrieves a Decimal from the ResultSet. */
  public static final ScalarResultType<Double> DECIMAL = new ScalarResultType<Double>() {
    public ScalarResultDeliverer<Double> getResultDeliverer() { return new DecimalDeliverer(); }
  };

  /** Retrieves an Integer from the ResultSet. */
  public static final ScalarResultType<Integer> INTEGER = new ScalarResultType<Integer>() {
    public ScalarResultDeliverer<Integer> getResultDeliverer() { return new IntegerDeliverer(); }
  };

  /** Retrieves a java.util.date (with time) from the ResultSet. */
  public static final ScalarResultType<Date> DATE = new ScalarResultType<Date>() {
    public ScalarResultDeliverer<Date> getResultDeliverer() { return new DateDeliverer(); }
  };

  /** This abstract class should not be overloaded externally. */
  private ScalarResultType() { }

  public abstract ScalarResultDeliverer<T> getResultDeliverer();

  private static class SQLObjectDeliverer extends ScalarResultDeliverer<Object> {
    @Override
    protected Object readResultObject(ResultSet pResultSet) throws SQLException {
      return pResultSet.getObject(1);
    }
  }

  private static class StringDeliverer extends ScalarResultDeliverer<String> {
    @Override
    protected String readResultObject(ResultSet pResultSet) throws SQLException {
      return pResultSet.getString(1);
    }
  }

  private static class DOMDeliverer extends ScalarResultDeliverer<DOM> {
    @Override
    protected DOM readResultObject(ResultSet pResultSet) throws SQLException {
      return SQLTypeConverter.getValueAsDOM(new ResultSetAdaptor(pResultSet), 1, pResultSet.getMetaData().getColumnType(1));
    }
  }

  private static class ClobDeliverer extends ScalarResultDeliverer<Clob> {
    @Override
    protected Clob readResultObject(ResultSet pResultSet) throws SQLException {
      return pResultSet.getClob(1);
    }
  }

  private static class BlobDeliverer extends ScalarResultDeliverer<Blob> {
    @Override
    protected Blob readResultObject(ResultSet pResultSet) throws SQLException {
      return pResultSet.getBlob(1);
    }
  }

  private static class DecimalDeliverer extends ScalarResultDeliverer<Double> {
    @Override
    protected Double readResultObject(ResultSet pResultSet) throws SQLException {
      return pResultSet.getDouble(1);
    }
  }

  private static class IntegerDeliverer extends ScalarResultDeliverer<Integer> {
    @Override
    protected Integer readResultObject(ResultSet pResultSet) throws SQLException {
      return pResultSet.getInt(1);
    }
  }

  private static class DateDeliverer extends ScalarResultDeliverer<Date> {
    @Override
    protected Date readResultObject(ResultSet pResultSet) throws SQLException {
      return pResultSet.getTimestamp(1);
    }
  }
}

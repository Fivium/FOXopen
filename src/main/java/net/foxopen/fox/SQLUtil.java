package net.foxopen.fox;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
* A SQL utility class.
*
* <p>Provides some useful utility methods for:
* <ol>
* <li>Clean-up of <code>java.sql.Connection</code>,
      <code>java.sql.Statement</code>, <code>java.sql.ResultSet</code> objects.
* </ol>
*
* <p><b>Note:</b> DO NOT introduce any other dependencies into this utility
* class other than <code>java.sql.*</code>. Rather than introduce EJB or
* JavaMail concepts, instead, create similar utility classes that extend on the
* features offerred by this one. For example, EJBUtil or MailUtil.
*
*/
@Deprecated
public class SQLUtil
{
  /**
   * Safely closes a <code>java.sql.ResultSet</code>, ignoing any errors that
   * occur as a result.
   *
   * <p>Not many developers know that ResultSet objects can be closed by
   * the application after use. While it's true that ResultSet objects are
   * cleaned-up automatically, by their owning <code>java.sql.Statement</code>
   * object (the Statement from which it was obtained), closing the ResultSet
   * after use immediately releases precious system resources that much earlier.
   *
   * <p><b>Notes:</b> You can supply <i>null</i> values for any of the parameters
   * to this method. The dispensing order and order of ownership is:<br>
   * <br>
   * <i>Connection->Statement*-><u>ResultSet*</u></i>
   * <br>
   * where * indicates zero or more. Dispensed items are cleaned up automatically
   * <u>only</u> when the dispenser parent is cleaned up. So, for example, a ResultSet
   * is only automatically cleaned when the owning Statement is closed.
   *
   * @param rs the <code>java.sql.ResultSet</code> object to be closed, discarding
   * any errors. Can be null, in which case no action is taken.
   */
  public static void cleanUp(ResultSet rs) {
    if (rs != null) {
      try { rs.close(); } catch (Throwable th){}
    }
  }

  /**
   * Safely closes a <code>java.sql.Statement</code>, ignoing any errors that
   * occur as a result.
   *
   * @param stmt the <code>java.sql.Statement</code> object to be closed, discarding
   * any errors. Can be null, in which case no action is taken.
   */
  public static void cleanUp(Statement stmt) {
    if (stmt != null) {
      try { stmt.close(); } catch (Throwable th){}
    }
  }

  /**
   * Safely closes a <code>java.sql.Connection</code>, ignoing any errors that
   * occur as a result.
   *
   * @param con the <code>java.sql.Connection</code> object to be closed, discarding
   * any errors. Can be null, in which case no action is taken.
   */
  public static void cleanUp(Connection con) {
    if (con != null) {
      try {
        con.close();
      } catch (Throwable th){
        th.printStackTrace();
      }
    }
  }

  /**
   * Safely closes a <code>java.sql.Connection</code> and a
   * <code>java.sql.Statement</code>, ignoing any errors that
   * occur as a result.
   *
   * @param con the <code>java.sql.Connection</code> object to be closed, discarding
   * any errors. Can be null, in which case no action is taken.
   * @param stmt the <code>java.sql.Statement</code> object to be closed, discarding
   * any errors. Can be null, in which case no action is taken.
   * @see #cleanUp(java.sql.Connection)
   * @see #cleanUp(java.sql.Statement)
   * @see #cleanUp(java.sql.ResultSet)
   */
  public static void cleanUp(Connection con, Statement stmt) {
    cleanUp(stmt);
    cleanUp(con);
  }

  /**
   * Safely closes a <code>java.sql.Connection</code>,
   * <code>java.sql.Statement</code> and a
   * <code>java.sql.ResultSet</code>, ignoing any errors that
   * occur as a result.
   *
   * @param con the <code>java.sql.Connection</code> object to be closed, discarding
   * any errors. Can be null, in which case no action is taken.
   * @param stmt the <code>java.sql.Statement</code> object to be closed, discarding
   * any errors. Can be null, in which case no action is taken.
   * @param rs the <code>java.sql.ResultSet</code> object to be closed, discarding
   * @see #cleanUp(java.sql.Connection)
   * @see #cleanUp(java.sql.Statement)
   * @see #cleanUp(java.sql.ResultSet)
   */
  public static void cleanUp(Connection con, Statement stmt, ResultSet rs) {
    cleanUp(rs);
    cleanUp(stmt);
    cleanUp(con);
  }

  /**
   * Safely closes a <code>java.sql.Statement</code> and a
   * <code>java.sql.ResultSet</code>, ignoing any errors that
   * occur as a result.
   *
   * @param stmt the <code>java.sql.Statement</code> object to be closed, discarding
   * any errors. Can be null, in which case no action is taken.
   * @param rs the <code>java.sql.ResultSet</code> object to be closed, discarding
   * @see #cleanUp(java.sql.Statement)
   * @see #cleanUp(java.sql.ResultSet)
   */
  public static void cleanUp(Statement stmt, ResultSet rs) {
    cleanUp(rs);
    cleanUp(stmt);
  }
}

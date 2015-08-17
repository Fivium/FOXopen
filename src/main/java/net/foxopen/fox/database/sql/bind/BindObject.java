package net.foxopen.fox.database.sql.bind;

import net.foxopen.fox.database.UCon;

import java.sql.SQLException;


/**
 * This interface provides a wrapping mechanism which allows objects from different sources to be bound into a JDBC
 * PreparedStatement using a common process. On construction, an ExecutableStatement will request BindObjects from its
 * BindProvider so it can prepare the JDBC statement accordingly. On execution, the BindObjects assigned to the ExecutableStatement
 * are asked to provide the actual objects to be bound into the statement. At this point the BindObject implementation may
 * choose to construct a temporary resource on the database (i.e. a temporary CLOB) to create a bindable object.<br/><br/>
 *
 * A BindObject should only be used once and then discarded. BindObjects may become tied to a specific ExecutableStatement
 * if they need access to temporary database resources, so they are not suitable for caching or persistence. Caching mechanisms
 * should instead cache a {@link BindObjectBuilder}, which can then be used to create the BindObjects just-in-time.<br/><br/>
 *
 * To avoid confusion, OUT binds should return null for {@link #getObject} and provide a seperate method to retrieve the
 * resultant out bind following statement execution.
 */
public interface BindObject {

  /**
   * Gets the actual object to be bound into a JDBC PreparedStatement. This <i>can</i> be null for IN binds and <i>should</i>
   * be null for OUT binds.
   * @param pUCon UCon being used to execute the statement. This may be used for the creation of temporary resources, etc.
   * @return Object to be bound into a PreparedStatement.
   * @throws SQLException If the object cannot be constructed.
   */
  Object getObject(UCon pUCon)
  throws SQLException;

  /**
   * Gets a string representation of the bind object for debugging purposes. If no object is bound IN by this BindObject,
   * this method should return null.
   * @return String representation or null.
   */
  String getObjectDebugString();

  /**
   * Gets the SQL type which should be used when setting or getting this bind object.
   * @return SQL Type.
   */
  BindSQLType getSQLType();

  /**
   * Gets the direction of this bind variable.
   * @return In, Out or In Out.
   */
  BindDirection getDirection();

}

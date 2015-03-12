package net.foxopen.fox.plugin.api.database;

public interface FxpUConBindMap<UBM extends FxpUConBindMap> {
  /**
   * Defines a named bind object in this bind map. See {@link UCon} for details of which objects can be bound into a bind map.
   * @param pBindName Name of the bind as it appears in the SQL statement (":" prefix optional but encouraged).
   * @param pBindObject Object to be bound.
   */
  UBM defineBind(String pBindName, Object pBindObject);
}

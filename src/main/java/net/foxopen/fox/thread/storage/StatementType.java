package net.foxopen.fox.thread.storage;

/**
 * Types of DatabaseStatement which are permitted in a {@link StorageLocation} definition.
 */
public enum StatementType {
  QUERY,
  READ_ONLY_QUERY,
  INSERT,
  UPDATE,
  DELETE,
  API;
}

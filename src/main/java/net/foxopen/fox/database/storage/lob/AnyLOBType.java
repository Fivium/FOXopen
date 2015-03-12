package net.foxopen.fox.database.storage.lob;


/**
 * Special "marker" class for LOB access code, used to indicate to a {@link LOBWorkDoc} that the consumer does not know
 * what LOB type the WorkDoc should contain.
 */
public class AnyLOBType {
  private AnyLOBType() {
  }
}

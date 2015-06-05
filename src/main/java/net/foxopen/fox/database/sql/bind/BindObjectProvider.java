package net.foxopen.fox.database.sql.bind;

/**
 * An implementor of this interface should be able to provide bind variables for a certain ParsedStatement, which can then
 * be converted into an ExecutableStatement with all bind variables assigned actual values.
 */
public interface BindObjectProvider {

  /**
   * Tests if this provider uses names to resolve a statement's bind variables, rather than just the bind indices.
   * @return True if the provider is aware of bind names.
   */
  public boolean isNamedProvider();

  /**
   * Gets the object to be bound into an ExecutableStatement at the given name or index.
   * @param pBindName Name of the bind variable, including ":" prefix. Implementors should allow the name to be null or empty
   *                  if they return false for {@link #isNamedProvider()}.
   * @param pIndex 0-based bind index.
   * @return The BindObject to be bound. This must not be null.
   */
  public BindObject getBindObject(String pBindName, int pIndex);

}

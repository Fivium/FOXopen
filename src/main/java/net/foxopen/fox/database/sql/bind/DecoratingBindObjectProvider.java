package net.foxopen.fox.database.sql.bind;

import net.foxopen.fox.ex.ExInternal;

/**
 * A BindObjectProvider which decorates another. With this mechanism, BindObjectProviders can be stacked on top of each
 * other. Bind lookup is delegated to decorated objects if the decorating object cannot provide a bind. For instance in the
 * following chain:<br/><br/>
 *
 * <tt>BindObjectProvider lDecoratingBOP = new DecoratingBindObjectProvider().decorate(lExistingBOP);</tt><br/><br/>
 *
 * The <tt>lDecoratingBOP</tt> object is used first. Only if the bind cannot be found will the lookup be delgated to the
 *  <tt>lExistingBOP</tt> object.
 */
public abstract class DecoratingBindObjectProvider
implements BindObjectProvider {

  private BindObjectProvider mDecoratedProvider;

  @Override
  public boolean isNamedProvider() {
    return mDecoratedProvider.isNamedProvider();
  }

  @Override
  public BindObject getBindObject(String pBindName, int pIndex) {
    BindObject lBindObject = getBindObjectOrNull(pBindName, pIndex);
    if(lBindObject == null) {
      if(mDecoratedProvider == null) {
        throw new ExInternal("No provider is currently decorated by this object.");
      }

      return mDecoratedProvider.getBindObject(pBindName, pIndex);
    }
    else {
      return lBindObject;
    }
  }

  /**
   * Decorates the given provider. Calls to getBindObject will be delegated to this object first, followed by the decorated
   * object.
   * @param pProviderToDecorate
   * @return Self-reference to this object for method chaining.
   */
  public BindObjectProvider decorate(BindObjectProvider pProviderToDecorate) {
    if(mDecoratedProvider == null) {
      mDecoratedProvider = pProviderToDecorate;
    }
    else {
      throw new ExInternal("This object is already decorating a bind provider");
    }

    return this;
  }

  /**
   * Implementors should return null if they cannot resolve the bind.
   * @param pBindName Bind name including ":" prefix.
   * @param pIndex 0-based bind index.
   * @return An appropriate bind object, or null.
   */
  protected abstract BindObject getBindObjectOrNull(String pBindName, int pIndex);
}

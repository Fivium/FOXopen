package net.foxopen.fox.database.sql.bind;

/**
 * Builder which can create a BindObject multiple times for use in different ExecutableStatements. A BindObjectBuilder
 * should be safe to persist and/or cache because it does not contain any stateful information related to a query execution.
 * A BindObjectBuilder is not necessary if BindObjects do not need to be stored - instead, BindObjectProviders should
 * create the BindObject directly. <br><br>
 *
 * For convenience, this abstract implementation stores a BindDirection, which should be a requirement of any subclass.
 *
 * @param <T> Type of BindObject this builder can create.
 */
public abstract class BindObjectBuilder<T extends BindObject> {

  private final BindDirection mBindDirection;

  protected BindObjectBuilder(BindDirection pBindDirection) {
    mBindDirection = pBindDirection;
  }

  /**
   * Constructs a new BindObject from this Builder.
   * @return New BindObject.
   */
  public abstract T build();

  /**
   * @return BindDirection for a created BindObject.
   */
  protected BindDirection getBindDirection() {
    return mBindDirection;
  }
}

package net.foxopen.fox.database.sql.bind;

import net.foxopen.fox.database.sql.bind.template.TemplateVariableObjectProvider;
import net.foxopen.fox.ex.ExInternal;

import java.util.Collections;
import java.util.Map;

/**
 * A BindObjectProvider which decorates another. With this mechanism, BindObjectProviders can be stacked on top of each
 * other. Bind lookup is delegated to decorated objects if the decorating object cannot provide a bind. For instance in the
 * following chain:<br/><br/>
 *
 * <tt>BindObjectProvider lDecoratingBOP = new DecoratingBindObjectProvider().decorate(lExistingBOP);</tt><br/><br/>
 *
 * The <tt>lDecoratingBOP</tt> object is used first. Only if the bind cannot be found will the lookup be delegated to the
 *  <tt>lExistingBOP</tt> object.<br/><br/>
 *
 * The decorator may also overload template variables by overriding the {@link #getTemplateVariableMap} method.
 */
public abstract class DecoratingBindObjectProvider
implements TemplateVariableObjectProvider {

  private BindObjectProvider mDecoratedProvider;

  @Override
  public final boolean isNamedProvider() {
    return mDecoratedProvider.isNamedProvider();
  }

  @Override
  public final BindObject getBindObject(String pBindName, int pIndex) {
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
  public final DecoratingBindObjectProvider decorate(BindObjectProvider pProviderToDecorate) {
    if(mDecoratedProvider == null) {
      mDecoratedProvider = pProviderToDecorate;
    }
    else {
      throw new ExInternal("This object is already decorating a bind provider");
    }

    return this;
  }

  @Override
  public final boolean isTemplateVariableDefined(String pVariableName) {
    //Check the map from this decorator, or delegate to the decorated object if it's a TemplateVariable provider
    return getTemplateVariableMap().containsKey(pVariableName) ||
      (mDecoratedProvider instanceof TemplateVariableObjectProvider && ((TemplateVariableObjectProvider) mDecoratedProvider).isTemplateVariableDefined(pVariableName));
  }

  @Override
  public final Object getObjectForTemplateVariable(String pVariableName) {

    Map<String, Object> lTemplateVariableMap = getTemplateVariableMap();
    if(lTemplateVariableMap.containsKey(pVariableName)) {
      //If this decorator has a variable definition, use that
      return lTemplateVariableMap.get(pVariableName);
    }
    else if(mDecoratedProvider instanceof TemplateVariableObjectProvider) {
      //Otherwise use the decorated provider if it's of the correct type
      return ((TemplateVariableObjectProvider) mDecoratedProvider).getObjectForTemplateVariable(pVariableName);
    }
    else {
      return null;
    }
  }

  /**
   * Implementors should return null if they cannot resolve the bind.
   * @param pBindName Bind name including ":" prefix.
   * @param pIndex 0-based bind index.
   * @return An appropriate bind object, or null.
   */
  protected abstract BindObject getBindObjectOrNull(String pBindName, int pIndex);

  /**
   * Subclasses should overload this method to return a map containing any extra TemplateVariable objects. The object type
   * should be converted for use in the target template.
   * @return Map containing any default template variables.
   */
  protected Map<String, Object> getTemplateVariableMap() {
    return Collections.emptyMap();
  }
}

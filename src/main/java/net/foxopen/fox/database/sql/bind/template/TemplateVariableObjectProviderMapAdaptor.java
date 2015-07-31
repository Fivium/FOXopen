package net.foxopen.fox.database.sql.bind.template;

import net.foxopen.fox.ex.ExInternal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Adaptor which converts a {@link TemplateVariableObjectProvider} into a {@link Map}, for use by a compiled Mustache template.
 * Only Map methods which are used within Mustache are supported.
 */
class TemplateVariableObjectProviderMapAdaptor
implements Map<String, Object> {

  private final MustacheVariableConverter mVariableConverter = MustacheVariableConverter.INSTANCE;
  private final TemplateVariableObjectProvider mVariableProvider;

  TemplateVariableObjectProviderMapAdaptor(TemplateVariableObjectProvider pVariableProvider) {
    mVariableProvider = pVariableProvider;
  }

  @Override
  public int size() {
    //Indicates non-emptiness
    return 1;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean containsKey(Object key) {
    if(key == null) {
      throw new ExInternal("Key cannot be null");
    }

    return mVariableProvider.isTemplateVariableDefined(key.toString());
  }

  @Override
  public boolean containsValue(Object value) {
    throw new ExInternal("Unsupported operation");
  }

  @Override
  public Object get(Object key) {
    if(key == null) {
      throw new ExInternal("Key cannot be null");
    }

    //Note: converter could be converted to a strategy interface if required
    return mVariableConverter.convertVariableObject(key.toString(), mVariableProvider.getXPathResultForTemplateVariable(key.toString()));
  }

  @Override
  public String put(String key, Object value) {
    throw new ExInternal("Unsupported operation");
  }

  @Override
  public String remove(Object key) {
    throw new ExInternal("Unsupported operation");
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> m) {
    throw new ExInternal("Unsupported operation");
  }

  @Override
  public void clear() {
    throw new ExInternal("Unsupported operation");
  }

  @Override
  public Set<String> keySet() {
    throw new ExInternal("Unsupported operation");
  }

  @Override
  public Collection<Object> values() {
    throw new ExInternal("Unsupported operation");
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    throw new ExInternal("Unsupported operation");
  }
}

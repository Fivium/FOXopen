package net.foxopen.fox.thread.persistence.xstream;


import com.thoughtworks.xstream.converters.SingleValueConverter;

import net.foxopen.fox.auth.SecurityScope;

/**
 * Converter for the default SecurityScope object, which is represented as a singleton. As all properties of the singleton
 * are known, it is wasteful to serialise the whole object, so this converter is used to suppress the excess serialisation.
 * SingleValueConverter seems to be the best mechanism in XStream to do this simply.
 */
class DefaultSecurityScopeConverter
implements SingleValueConverter {

  @Override
  public boolean canConvert(Class pType) {
    return pType.equals(SecurityScope.defaultInstance().getClass());
  }

  @Override
  public Object fromString(String pStr) {
    return SecurityScope.defaultInstance();
  }

  @Override
  public String toString(Object pObj) {
    return "";
  }

}

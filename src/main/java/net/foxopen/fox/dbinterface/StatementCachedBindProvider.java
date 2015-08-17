package net.foxopen.fox.dbinterface;

import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectBuilder;
import net.foxopen.fox.database.sql.bind.CachedBindObjectProvider;
import net.foxopen.fox.database.sql.bind.template.TemplateVariableObjectProvider;
import net.foxopen.fox.ex.ExInternal;

import java.util.Map;

/**
 * Cacheable BindObjectProvider created from an {@link InterfaceStatement}. This caches {@link BindObjectBuilder} objects
 * and creates new BindObjects just-in-time for every statement execution. Template variables for the statement are also cached.
 */
public class StatementCachedBindProvider
implements CachedBindObjectProvider, TemplateVariableObjectProvider {

  private final Map<String, BindObjectBuilder> mBuilderMap;
  private final Map<String, Object> mTemplateVariableMap;

  StatementCachedBindProvider(Map<String, BindObjectBuilder> pBuilderMap, Map<String, Object> pTemplateVariableMap) {
    mBuilderMap = pBuilderMap;
    mTemplateVariableMap = pTemplateVariableMap;
  }

  @Override
  public boolean isNamedProvider() {
    return true;
  }

  @Override
  public BindObject getBindObject(String pBindName, int pIndex) {
    String lBindName = pBindName.replace(":", "");

    BindObjectBuilder lBindObjectBuilder = mBuilderMap.get(lBindName);
    if(lBindObjectBuilder == null) {
      throw new ExInternal("Cannot find a cached bind called '" + lBindName + "'");
    }

    return lBindObjectBuilder.build();
  }

  @Override
  public boolean isTemplateVariableDefined(String pVariableName) {
    return mTemplateVariableMap.containsKey(pVariableName);
  }

  @Override
  public Object getObjectForTemplateVariable(String pVariableName) {
    return mTemplateVariableMap.get(pVariableName);
  }
}

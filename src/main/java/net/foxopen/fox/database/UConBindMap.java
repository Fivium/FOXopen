package net.foxopen.fox.database;

import java.util.HashMap;

import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.plugin.api.database.FxpUConBindMap;


/**
 * Map-like object for binding parameters to the UCon statement execution methods by name. See {@link UCon} for usage details.
 * Consumers should create a new BindMap for every statement execution using the no-args constructor.
 */
public class UConBindMap implements FxpUConBindMap<UConBindMap>, BindObjectProvider  {

  private final HashMap<String, Object> mBindMap = new HashMap<>();

  /**
   * Constructs a new empty UConBindMap.
   */
  public UConBindMap() {}

  /**
   * Defines a named bind object in this bind map. See {@link UCon}for details of which objects can be bound into a bind map.
   * @param pBindName Name of the bind as it appears in the SQL statement (":" prefix optional but encouraged).
   * @param pBindObject Object to be bound.
   * @return Self-reference.
   */
  public UConBindMap defineBind(String pBindName, Object pBindObject) {

    //Ensure bind name is prefixed with the colon character
    if(!pBindName.startsWith(":")) {
      pBindName = ":" + pBindName;
    }

    if(mBindMap.containsKey(pBindName)) {
      throw new ExInternal("Bind " + pBindName + " has already been defined in this bind map");
    }
    mBindMap.put(pBindName, pBindObject);

    return this;
  }

  /**
   * Redefines a named bind object in this bind map. See {@link UCon}for details of which objects can be bound into a bind map.
   * This skips a check whether a bind is already present. The bind will be created if it is not present.
   * @param pBindName Name of the bind as it appears in the SQL statement (":" prefix optional but encouraged).
   * @param pBindObject Object to be bound.
   * @return Self-reference.
   */
  public UConBindMap redefineBind(String pBindName, Object pBindObject) {

    //Ensure bind name is prefixed with the colon character
    if(!pBindName.startsWith(":")) {
      pBindName = ":" + pBindName;
    }

    mBindMap.put(pBindName, pBindObject);

    return this;
  }

  /**
   * Gets the object for the bind parameter of the given name.
   * @param pBindName
   * @return
   */
  Object getBindObject(String pBindName) {
    if(!mBindMap.containsKey(pBindName)) {
      throw new ExInternal("Bind " + pBindName + " not found in map");
    }
    return mBindMap.get(pBindName);
  }

  @Override
  public boolean isNamedProvider() {
    return true;
  }

  @Override
  public BindObject getBindObject(String pBindName, int pIndex) {
    return UCon.convertToBindObject(getBindObject(pBindName));
  }
}

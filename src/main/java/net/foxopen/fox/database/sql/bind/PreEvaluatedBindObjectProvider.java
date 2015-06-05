package net.foxopen.fox.database.sql.bind;

import net.foxopen.fox.ex.ExInternal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BindObject provider which evaluates binds from an existing provider and caches the resultant BindObjects for later use.
 * This should be used to evaluate binds for a query which needs to be executed at a later time.
 */
public abstract class PreEvaluatedBindObjectProvider
implements BindObjectProvider {

  /**
   * Evaluates the binds from the given BindObjectProvider and caches them in a new BindObjectProvider, which can be stored
   * for later use. Note that the BindObjects returned from the original BindObjectProvider are stored without any modification.
   * This may cause memory leaks if the BindObjects contain references to complex objects such as DOM nodes, so care should
   * be taken when retaining references to the PreEvaluatedBindObjectProvider.
   *
   * @param pBindNames Names of binds to retrieve and cache from the BindObjectProvider. This will typically be the names
   *                   of all bind variables in a query.
   * @param pBindObjectProvider Source of BindObjects to be cached.
   * @return BindObjectProvider containing cached binds.
   */
  public static PreEvaluatedBindObjectProvider preEvaluateBinds(Collection<String> pBindNames, BindObjectProvider pBindObjectProvider) {

    if(pBindObjectProvider.isNamedProvider()) {
      return PreEvaluatedNamedBindObjectProvider.create(pBindNames, pBindObjectProvider);
    }
    else {
      return PreEvaluatedIndexBindObjectProvider.create(pBindNames, pBindObjectProvider);
    }
  }

  /**
   * PreEvaluated provider created from a named bind provider which stores BindObjects in a map.
   */
  private static class PreEvaluatedNamedBindObjectProvider
  extends PreEvaluatedBindObjectProvider {

    private final Map<String, BindObject> mBindNameToBindObject;

    private static PreEvaluatedBindObjectProvider create(Collection<String> pBindNames, BindObjectProvider pBindObjectProvider) {

      Map<String, BindObject> lBindNameToBindObject = new HashMap<>(pBindNames.size());
      for(String lBind : pBindNames) {
        //Don't re-evaluate binds that have already been done (i.e. de-duplicate the bind list)
        if(!lBindNameToBindObject.containsKey(lBind)) {
          lBindNameToBindObject.put(lBind, pBindObjectProvider.getBindObject(lBind, -1));
        }
      }

      return new PreEvaluatedNamedBindObjectProvider(lBindNameToBindObject);
    }

    private PreEvaluatedNamedBindObjectProvider(Map<String, BindObject> pBindNameToBindObject) {
      mBindNameToBindObject = pBindNameToBindObject;
    }

    @Override
    public boolean isNamedProvider() {
      return true;
    }

    @Override
    public BindObject getBindObject(String pBindName, int pIndex) {
      //Strip leading colon from bind name
      BindObject lBindObject = mBindNameToBindObject.get(pBindName.replace(":", ""));
      if(lBindObject == null) {
        throw new ExInternal("No pre-evaluated bind found for " + pBindName);
      }
      else {
        return lBindObject;
      }
    }

    @Override
    public String toString() {
      return "PreEvaluatedBinds " + mBindNameToBindObject;
    }
  }

  /**
   * PreEvaluated provider created from a non-named bind provider which stores BindObjects in a list.
   */
  private static class PreEvaluatedIndexBindObjectProvider
  extends PreEvaluatedBindObjectProvider {

    private final List<BindObject> mBindObjectList;

    private static PreEvaluatedBindObjectProvider create(Collection<String> pBindNames, BindObjectProvider pBindObjectProvider) {

      List<BindObject> lBindObjectList = new ArrayList<>(pBindNames.size());
      for (int i = 0; i < pBindNames.size(); i++) {
        lBindObjectList.add(pBindObjectProvider.getBindObject(null, i));
      }

      return new PreEvaluatedIndexBindObjectProvider(lBindObjectList);
    }

    private PreEvaluatedIndexBindObjectProvider(List<BindObject> pBindObjectList) {
      mBindObjectList = pBindObjectList;
    }

    @Override
    public boolean isNamedProvider() {
      return false;
    }

    @Override
    public BindObject getBindObject(String pBindName, int pIndex) {
      if(pIndex >= mBindObjectList.size()) {
        throw new ExInternal("Index " + pIndex + " not available in this bind provider");
      }
      return mBindObjectList.get(pIndex);
    }
  }
}

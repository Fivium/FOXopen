package net.foxopen.fox.thread.stack;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.dom.xpath.saxon.XPathVariableManager;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.persistence.PersistenceContextProxy;
import net.foxopen.fox.track.Track;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.value.AtomicValue;
import nu.xom.Node;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ModuleXPathVariableManager
implements XPathVariableManager {

  /** Purpose given to the initial stack entry */
  private static final String GLOBAL_PURPOSE = "GLOBAL";

  private transient PersistenceContextProxy mPersistenceContextProxy;
  private final Deque<StackEntry> mVariableStack = new ArrayDeque<>(3);

  ModuleXPathVariableManager() {
    //Default persistence action does nothing - ModuleCallStack should use setPersistenceContextProxy to overload this
    mPersistenceContextProxy = () -> {};
    mVariableStack.addFirst(new StackEntry(GLOBAL_PURPOSE));
  }

  void setPersistenceContextProxy(PersistenceContextProxy pProxy) {
    mPersistenceContextProxy = pProxy;
  }

  private static class StackEntry {
    private final String mPurpose;
    private final Map<String, Object> mVariables = new HashMap<>(6);

    public StackEntry(String pPurpose) {
      mPurpose = pPurpose;
    }
  }

  /**
   * Finds the highest entry on the stack containing a variable of the given name.
   * @param pVariableName Name to search for.
   * @return Highest stack entry.
   */
  private StackEntry highestEntryWithVariableDefined(String pVariableName) {

    for (StackEntry lStackEntry : mVariableStack) {
      if (lStackEntry.mVariables.containsKey(pVariableName)) {
        return lStackEntry;
      }
    }
    return null;
  }

  @Override
  public Object resolveVariable(String pVariableName) {
    StackEntry lFindEntry = highestEntryWithVariableDefined(pVariableName);
    return lFindEntry == null ? null : lFindEntry.mVariables.get(pVariableName);
  }

  @Override
  public void setVariableFromXPathResult(String pVariableName, XPathResult pXPathResult) {
    setVariable(pVariableName, pXPathResult.asObject());
  }

  @Override
  public void setVariable(String pVariableName, Object pValue) {
    validateName(pVariableName);
    mVariableStack.getLast().mVariables.put(pVariableName, convertResultObject(pVariableName, pValue));
    mPersistenceContextProxy.updateRequired();
  }

  /**
   * Validates the given variable name and throws an exception if it is not valid.
   * @param pVariableName Name to validate.
   */
  private void validateName(String pVariableName) {
    if(XFUtil.isNull(pVariableName)) {
      throw new ExInternal("Variable names cannot be null");
    }
    if(!NameChecker.isValidNCName(pVariableName)) {
      throw new ExInternal("'" + pVariableName + "' is not a valid variable name");
    }
  }

  @Override
  public void clearVariable(String pVariableName) {
    if(mVariableStack.getLast().mVariables.containsKey(pVariableName)) {
      //Only remove/mark for update if there was a variable mapped to this name (even if the mapped object was null)
      mVariableStack.getLast().mVariables.remove(pVariableName);
      mPersistenceContextProxy.updateRequired();
    }
  }

  @Override
  public boolean isVariableSet(String pVariableName, boolean pLocalOnly) {

    StackEntry lFoundEntry = highestEntryWithVariableDefined(pVariableName);

    //If we've been asked to search local only, exclude the global stack entry
    if(lFoundEntry == null || (pLocalOnly && GLOBAL_PURPOSE.equals(lFoundEntry.mPurpose))) {
      return false;
    }
    else {
      return true;
    }
  }

  @Override
  public void localise(String pPurpose, Map<String, Object> pLocalVariables) {
    mVariableStack.addFirst(new StackEntry(pPurpose));

    for (Map.Entry<String, Object> lLocalVar : pLocalVariables.entrySet()) {

      String lVariableName = lLocalVar.getKey();
      validateName(lVariableName);

      Object lValueObject = lLocalVar.getValue();
      if(lValueObject instanceof XPathResult) {
        lValueObject = ((XPathResult) lValueObject).asObject();
      }

      mVariableStack.getFirst().mVariables.put(lVariableName, convertResultObject(lVariableName, lValueObject));
    }
  }

  @Override
  public void delocalise(String pPurpose) {
    String lTopPurpose = mVariableStack.peekFirst().mPurpose;
    if(!pPurpose.equals(lTopPurpose)) {
      throw new ExInternal("Purpose mismatch: asked to delocalise " + pPurpose + " but top purpose was " + lTopPurpose);
    }
    else {
      mVariableStack.removeFirst();
    }
  }

  @Override
  public Collection<String> getAllVariableNames() {
    //Go through all stack entries, flattening each individual key set into a single collection
    return mVariableStack
      .stream()
      .flatMap(e -> e.mVariables.keySet().stream())
      .collect(Collectors.toSet());
  }

  /**
   * Converts an object from an XPath result into a "cacheable" object which can be stored in an XPathVariableManager.
   * DOM/Node objects are cloned into unconnected elements, and collections are handled recursively.
   *
   * @param pVariableName Variable currently being converted for debug purposes.
   * @param pResultObject Object to convert.
   * @return Conversion result.
   */
  private static Object convertResultObject(String pVariableName, Object pResultObject) {

    if(pResultObject instanceof String || pResultObject instanceof Number || pResultObject instanceof Boolean || pResultObject instanceof AtomicValue) {
      return pResultObject;
    }
    else if(pResultObject instanceof Collection) {
      return convertCollection(pVariableName, (Collection) pResultObject);
    }
    else if(pResultObject instanceof DOM) {
      return convertDOM(pVariableName, (DOM) pResultObject);
    }
    else if(pResultObject instanceof Node) {
      return convertDOM(pVariableName, new DOM((Node) pResultObject));
    }
    else {
      throw new ExInternal("Can't convert object of type " + pResultObject.getClass().getName() + " for variable $" + pVariableName);
    }
  }

  /**
   * Converts a DOM object into an appropriate format for storage/serialisation by this manager object.
   * <ul>
   *   <li>Elements are cloned into an unconnected (no document) copy</li>
   *   <li>Text nodes and attributes are converted to strings</li>
   *   <li>All other node types throw an exception</li>
   * </ul>
   * @param pVariableName Variable currently being converted for debug purposes.
   * @param pDOM Object to convert.
   * @return Conversion result.
   */
  private static Object convertDOM(String pVariableName, DOM pDOM) {

    if(pDOM.isElement()) {
      if(!pDOM.isSimpleElement()) {
        Track.alert("SetVariableToDOM", "Setting variable $" + pVariableName + " to a complex DOM may have adverse performance impact; consider fm:context-set/localise");
      }
      DOM lClone = DOM.createUnconnectedElement(pDOM.getName());
      pDOM.copyContentsTo(lClone);
      return lClone;
    }
    else if(pDOM.isAttribute() || pDOM.isText()) {
      return pDOM.value();
    }
    else {
      throw new ExInternal("Cannot convert DOM node of type " + pDOM.nodeType() + " for variable $" + pVariableName);
    }
  }

  /**
   * Recursively converts the content of the given collection.
   * @param pVariableName Variable currently being converted for debug purposes.
   * @param pResultCollection Collection to be converted.
   * @return New collection containing conversion result.
   */
  private static Collection<?> convertCollection(String pVariableName, Collection<?> pResultCollection) {
    return pResultCollection
      .stream()
      .map(e -> ModuleXPathVariableManager.convertResultObject(pVariableName, e))
      .collect(Collectors.toList());
  }

}

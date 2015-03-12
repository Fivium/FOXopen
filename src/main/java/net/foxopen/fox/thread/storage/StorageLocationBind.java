package net.foxopen.fox.thread.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;


/**
 * Property class mapping a bind name to a UsingType and optional using string (e.g. the XPath string for an XPATH UsingType).
 */
public class StorageLocationBind {
  
  private final String mBindName;
  private final UsingType mUsingType;
  /** Can be null. */
  private final String mUsingString;
  
  private static final String BIND_NAME_INDEX_PREFIX = "BIND_";
  /**
   * Converts an numberic index into a string bind name. All binds are given names internally even if they have no external name.
   * @param pIndex
   * @return
   */
  private static String bindNameForIndex(int pIndex) {
    return BIND_NAME_INDEX_PREFIX + pIndex;
  }  

  static List<StorageLocationBind> createListFromDOMDefinition(DOM pDefinitionDOM) 
  throws ExModule, ExInternal {
    
    List<StorageLocationBind> lResult = new ArrayList<>();
    int i = 0;

    // Process each using clause    
    for(DOM lUsingDOM : pDefinitionDOM.getUL("fm:using")) {
      
      String lUsingTypeString = lUsingDOM.getAttr("using-type");
      
      //Default using type is XPATH if not specified in the DOM
      UsingType lUsingType;
      if(XFUtil.isNull(lUsingTypeString)) {
        lUsingType = UsingType.XPATH;
      }
      else {
        lUsingType = UsingType.fromExternalString(lUsingTypeString);
      }
      
      //Check this is a valid using type
      if(lUsingType == null) {
        throw new ExModule("Storage Location using-type value not recognised: "+lUsingTypeString);
      }
      
      //Read the XPATH string if this is an XPath or Static bind
      String lUsingString;
      if(lUsingType == UsingType.XPATH || lUsingType == UsingType.STATIC) {
        lUsingString = lUsingDOM.value();
        //Validate that an XPath has actually been provided
        if(lUsingString.length()==0){
          throw new ExModule("Empty using clause not allowed", pDefinitionDOM);
        }
      }
      else {
        lUsingString = null;
      }
      
      //Establish name for this bind 
      String lBindName;
      if(!XFUtil.isNull(lUsingDOM.getAttr("name"))) {
        lBindName = lUsingDOM.getAttr("name");
      }
      else {
        lBindName = bindNameForIndex(i);
      }
      
      lResult.add(new StorageLocationBind(lBindName, lUsingType, lUsingString));
    }
    
    return lResult;
  } 
  
  /**
   * Evaluates the string-based bind variables from pBindList using the given ContextUElem and unique value. The returned
   * map contains the given StorageLocationBinds mapped to their evaluated results.
   * @param pBindList
   * @param pContextUElem
   * @param pUniqueValue
   * @return
   */
  public static Map<StorageLocationBind, String> evaluateStringBinds(List<StorageLocationBind> pBindList, ContextUElem pContextUElem, String pUniqueValue) {
    
    Map<StorageLocationBind, String> lResult = new HashMap<>();
    
    for(StorageLocationBind lSLBind : pBindList) {
      String lEvaluatedString;
      if(lSLBind.getUsingType() == UsingType.XPATH) {
        try {
          lEvaluatedString = pContextUElem.extendedXPathString(pContextUElem.attachDOM(), lSLBind.getUsingString());
        }
        catch (ExActionFailed e) {
          throw new ExInternal("Failed to run XPath for storage location bind", e);
        }
      }
      else if(lSLBind.getUsingType() == UsingType.STATIC) { 
        lEvaluatedString = lSLBind.getUsingString();
      }
      else if(lSLBind.getUsingType() == UsingType.UNIQUE) { 
        lEvaluatedString = pUniqueValue;
      }
      else {
        //This is a LOB bind or uknown using type - no need to record anything about it
        continue;
      }
      
      //Record the evaluated string result
      lResult.put(lSLBind, lEvaluatedString);
    }
    
    return lResult;    
  }
  
  StorageLocationBind(int pBindIndex, UsingType pUsingType, String pUsingString) {
    this(bindNameForIndex(pBindIndex), pUsingType, pUsingString);
  }

  StorageLocationBind(String pBindName, UsingType pUsingType, String pUsingString) {
    mBindName = pBindName;
    mUsingType = pUsingType;
    mUsingString = pUsingString;
  }

  public String getBindName() {
    return mBindName;
  }

  public UsingType getUsingType() {
    return mUsingType;
  }

  public String getUsingString() {
    return mUsingString;
  }

  public BindSQLType getSQLType() {
    return mUsingType.getSQLType();
  }
  
  /*
   * This class is used as a map key and it is possible that multiple objects will be created which represent the same
   * tuple. Therefore hashcode and equals require overloading.
   */

  @Override
  public boolean equals(Object pObject) {
    if (this == pObject) {
      return true;
    }
    if (!(pObject instanceof StorageLocationBind)) {
      return false;
    }
    final StorageLocationBind lOther = (StorageLocationBind) pObject;
    if (!(mBindName == null ? lOther.mBindName == null : mBindName.equals(lOther.mBindName))) {
      return false;
    }
    if (!(mUsingType == null ? lOther.mUsingType == null : mUsingType.equals(lOther.mUsingType))) {
      return false;
    }
    if (!(mUsingString == null ? lOther.mUsingString == null : mUsingString.equals(lOther.mUsingString))) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    final int PRIME = 37;
    int lResult = 1;
    lResult = PRIME * lResult + ((mBindName == null) ? 0 : mBindName.hashCode());
    lResult = PRIME * lResult + ((mUsingType == null) ? 0 : mUsingType.hashCode());
    lResult = PRIME * lResult + ((mUsingString == null) ? 0 : mUsingString.hashCode());
    return lResult;
  }
}

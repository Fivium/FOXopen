package net.foxopen.fox.dbinterface;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.track.Track;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * A container for DbInterface statements
 */
public class DatabaseInterface {

  private final String mInterfaceName;

  private final Map<String, InterfaceQuery> mInterfaceQueryMap;
  private final Map<String, InterfaceAPI> mInterfaceAPIMap;

  /**
   * Construct a new DbInterface Object from db-interface definition xml
   *
   * @param pDbInterfaceXML  FOX XML defining db-interface
   */
  public DatabaseInterface(DOM pDbInterfaceXML, Mod pOwningMod)
  throws ExModule {

    if (!"db-interface".equals(pDbInterfaceXML.getLocalName())) {
      throw new ExInternal("New DbInterface expected fm:db-interface XML, found " + pDbInterfaceXML.getName());
    }

    mInterfaceName = pDbInterfaceXML.getAttr("name");
    if(XFUtil.isNull(mInterfaceName)) {
      throw new ExInternal("fm:db-interface must specify a name");
    }

    Map<String, InterfaceQuery> lQueryMap = new HashMap<>();
    Map<String, InterfaceAPI> lAPIMap = new HashMap<>();

    //Parse child nodes as DbStatements
    for(DOM lStatementDOM : pDbInterfaceXML.getChildElements()) {

      String lElemName = lStatementDOM.getLocalName().toLowerCase();
      if(InterfaceQuery.DBINT_ELEMENT_NAME.equals(lElemName)){
        InterfaceQuery lQuery = new InterfaceQuery(lStatementDOM, mInterfaceName, pOwningMod);
        if(lQueryMap.containsKey(lQuery.getStatementName())) {
          throw new ExInternal("Duplicate query definition for " + lQuery.getStatementName() + " in DB Interface " + mInterfaceName);
        }
        lQueryMap.put(lQuery.getStatementName(), lQuery);
      }
      else if(InterfaceAPI.DBINT_ELEMENT_NAME.equals(lElemName)) {
        InterfaceAPI lAPI = new InterfaceAPI(lStatementDOM, mInterfaceName, pOwningMod);
        if(lAPIMap.containsKey(lAPI.getStatementName())) {
          throw new ExInternal("Duplicate API definition for " + lAPI.getStatementName() + " in DB Interface " + mInterfaceName);
        }
        lAPIMap.put(lAPI.getStatementName(), lAPI);
      }
      else if ("table".equals(lElemName)) {
        Track.alert("DbInterfaceTable", "Unsupported fm:table syntax found - definition skipped");
      }
      else {
        throw new ExModule("Unknown statement type " + lElemName);
      }
    }

    mInterfaceQueryMap = Collections.unmodifiableMap(lQueryMap);
    mInterfaceAPIMap = Collections.unmodifiableMap(lAPIMap);
  }

  /**
   * Gets the query of the given name from this DatabaseInterface. Throws an exception if the query does not exist.
   * @param pQueryName Name of query to retrieve.
   * @return The corresponding InterfaceQuery of the given name.
   */
  public InterfaceQuery getInterfaceQuery(String pQueryName) {

    InterfaceQuery lStatement = mInterfaceQueryMap.get(pQueryName);
    if(lStatement == null){
      throw new ExInternal("No query found with the name " + pQueryName + " in DB interface " + getInterfaceName());
    }
    else {
      return lStatement;
    }
  }

  /**
   * Gets the API of the given name from this DatabaseInterface. Throws an exception if the API does not exist.
   * @param pAPIName Name of query to retrieve.
   * @return The corresponding InterfaceQuery of the given name.
   */
  public InterfaceAPI getInterfaceAPI(String pAPIName) {

    InterfaceAPI lStatement = mInterfaceAPIMap.get(pAPIName);
    if(lStatement == null){
      throw new ExInternal("No API found with the name " + pAPIName + " in DB interface " + getInterfaceName());
    }
    else {
      return lStatement;
    }
  }

  public String getInterfaceName() {
    return mInterfaceName;
  }
}

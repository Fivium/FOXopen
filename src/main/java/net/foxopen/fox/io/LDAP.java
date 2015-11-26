package net.foxopen.fox.io;

import com.google.common.base.CharMatcher;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class LDAP {
  private DirContext mServerContext;

  /**
   * Create a new instance of an LDAP connection
   *
   * @param pServerURL URL of the LDAP server: ldap://example.com
   * @param pUserDN User to authenticate with
   * @param pPassword Password to authenticate with
   */
  public LDAP(String pServerURL, String pUserDN, String pPassword)
  throws NamingException
  {
    // Connect to LDAP server
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, pServerURL);

    //Auth code
    if (pUserDN != null) {
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, pUserDN);
      env.put(Context.SECURITY_CREDENTIALS, pPassword);
    }

    mServerContext = new InitialDirContext(env);
  }

  /**
   * Get a XML DOM of attributes back for a given DN <br />
   * If pAttributes is null then it will return all attributes associated with pDN
   *
   * @param pDN DN of entry
   * @param pAttributes String array of attributes to return, or null for all
   * @return XML DOM of attributes specified, or all if pAttributes is null
   */
  public DOM getAttributesXMLFromDN (String pDN, String[] pAttributes) {
    try {
      NamingEnumeration lAttributes = mServerContext.getAttributes(pDN, pAttributes).getAll();
      DOM lAttributeDOM = DOM.createDocument("LDAP_RESULTS");
      while (lAttributes.hasMore()) {
        Attribute lAttr = (Attribute)lAttributes.next();
        if (lAttr.size() > 1) {
          DOM lListDOM = lAttributeDOM.addElem(lAttr.getID()+"_list");

          NamingEnumeration lAttributes2 = lAttr.getAll();
          while (lAttributes2.hasMore()) {
            try {
              lListDOM.addElem(lAttr.getID(), sanitiseLDAPAttributeValueForXML(lAttr.getID(), (String)lAttributes2.next()));
            } catch (ClassCastException e) {
              // Should we raise error
            }
          }
        }
        else {
          try {
            lAttributeDOM.addElem(lAttr.getID(), sanitiseLDAPAttributeValueForXML(lAttr.getID(), (String)lAttr.get()));
          } catch (ClassCastException e) {
            // Should we raise error
          }
        }
      }
      return lAttributeDOM;
    }
    catch (NamingException e) {
      throw new ExInternal("No data found", e);
    }
  }

  /**
   * Make sure no control characters are getting in to the LDAP attribute values
   *
   * @param lAttributeID ID of the attribute for track purposes
   * @param lAttributeValue RawAttribute value
   * @return Safer attribute value for XML
   */
  private String sanitiseLDAPAttributeValueForXML(String lAttributeID, String lAttributeValue) {
    String lControlStrippedValue = CharMatcher.JAVA_ISO_CONTROL.removeFrom(lAttributeValue);
    if (!lAttributeValue.equals(lControlStrippedValue)) {
      Track.pushAlert("ControlCharactersInLDAPAttribute", "Key: " + lAttributeID);
      try {
        Track.debug("OriginalValue", StringEscapeUtils.escapeJava(lAttributeValue));
        Track.debug("SanitisedValue", lControlStrippedValue);
      }
      finally {
        Track.pop("ControlCharactersInLDAPAttribute");
      }
    }

    return lControlStrippedValue;
  }
}

/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.io;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;

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
              lListDOM.addElem(lAttr.getID(), (String)lAttributes2.next());
            } catch (ClassCastException e) {
              // Should we raise error
            }
          }
        }
        else {
          try {
            lAttributeDOM.addElem(lAttr.getID(), (String)lAttr.get());
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
   * Get a XML DOM of attributes back for a given DN <br />
   * If pAttributes is null then it will return all attributes associated with pDN
   *
   * @param pDN DN of entry
   * @param pAttributes String array of attributes to return, or null for all
   * @return XML DOM of attributes specified, or all if pAttributes is null
   */
  public DOM getAttributesXMLFromDNNoEx (String pDN, String[] pAttributes) {
    try {
      return getAttributesXMLFromDN(pDN, pAttributes);
    }
    catch (ExInternal e) {
      return DOM.createDocument("LDAP_RESULTS");
    }
  }

  /**
   * Get a XML DOM of attributes back for a given DN <br />
   * If pAttributes is null then it will return all attributes associated with pDN
   *
   * @param pDN DN of entry
   * @param pAttributes String array of attributes to return, or null for all
   * @return XML DOM of attributes specified, or all if pAttributes is null
   */
  public DOM getAttributesXMLFromDNOrNull (String pDN, String[] pAttributes) {
    try {
      return getAttributesXMLFromDN(pDN, pAttributes);
    }
    catch (ExInternal e) {
      return null;
    }
  }

  /**
   * Get attributes in a Naming Enumeration for custom parsing
   *
   * @param pDN DN of entry
   * @param pAttributes String array of attributes to return, or null for all
   * @return NamingEnumeration os returned attributes
   */
  public NamingEnumeration getAttributesFromDN (String pDN, String[] pAttributes) {
    try {
      return mServerContext.getAttributes(pDN, pAttributes).getAll();
    }
    catch (NamingException e) {
      throw new ExInternal("No data found", e);
    }
  }

  /**
   * Get attributes in a Naming Enumeration for custom parsing
   *
   * @param pDN DN of entry
   * @param pAttributes String array of attributes to return, or null for all
   * @return NamingEnumeration os returned attributes
   */
  public NamingEnumeration getAttributesFromDNOrNull (String pDN, String[] pAttributes) {
    try {
      return mServerContext.getAttributes(pDN, pAttributes).getAll();
    }
    catch (NamingException e) {
      return null;
    }
  }

}

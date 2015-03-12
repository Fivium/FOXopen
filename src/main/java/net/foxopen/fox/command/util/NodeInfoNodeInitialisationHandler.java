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
package net.foxopen.fox.command.util;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dbinterface.QueryMode;
import net.foxopen.fox.dbinterface.deliverer.InterfaceQueryResultDeliverer;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInitialisation;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExRoot;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.thread.ActionRequestContext;


public class NodeInfoNodeInitialisationHandler
implements NodeInitialisationHandler
{
  public NodeInfoNodeInitialisationHandler() {
  }

  /*
   * Visit the specified Data DOM node and initialise its value
   * using the <code>NodeInfo</code> for the corresponding schema node.
   *
   * <p>By definition, this initialisation handler only initialises
   * DOM elements that have no content - text or otherwise.
   *
   * <p>This method obtains the <code>NodeInfo</code> for the specified
   * node, obtains the registered initialisation handler(s) for that node type
   * and delegates the initialisation task to any handler(s) found.
   *
   * @param node the Data DOM node to initialise
   * @param userThread the current user thread context
   * @param contextUElem the attach contexts
   * @param contextUCon the current connection context
   * @exception ExInternal thrown if an unexpected error occurs during initialisation.
   * @exception InitialisationException thrown if an initialisation error occurs.
   */
  public void initialise(ActionRequestContext pRequestContext, DOM node)
  throws ExInternal, ExInitialisation {
    if (node.hasContent()) {
      return;
    }

    ContextUElem contextUElem = pRequestContext.getContextUElem();
    ContextUCon contextUCon = pRequestContext.getContextUCon();

    //------------------------------------------------------------------------
    // Now initialise the node by checking for the occurrence of initialisation
    // attributes on the Model DOM (NodeInfo).
    //
    // The order of the checks beloew reflects the order or precedence for
    // the various types of initialisation.
    //------------------------------------------------------------------------
    NodeInfo nodeInfo = pRequestContext.getCurrentModule().getNodeInfo(node.absolute());

    if (nodeInfo.getAttribute("fox", "init-db-interface") != null ||
        nodeInfo.getAttribute("fox", "init-query") != null )
    {
      String interfaceName = nodeInfo.getAttribute("fox", "init-db-interface");
      String queryName     = nodeInfo.getAttribute("fox", "init-query");

      // Localise the ContextUElem here so that we can rewrite the attach point
      // and action contexts just for this query execution
      contextUElem.localise("fm:init with init-query");
      try {
        contextUElem.setUElem(ContextLabel.ATTACH, node);
        contextUElem.setUElem(ContextLabel.ACTION, node);

        //Get interface and run query
        //TODO PN - could this be nicer? Relies on query selecting column called "."...
        InterfaceQuery lDBQuery = pRequestContext.getCurrentModule().getDatabaseInterface(interfaceName).getInterfaceQuery(queryName);
        UCon lUCon = contextUCon.getUCon("fm:init db query");
        try {
          InterfaceQueryResultDeliverer lDeliverer = InterfaceQueryResultDeliverer.getDeliverer(pRequestContext, lDBQuery, QueryMode.ADD_TO, node, null);
          lDBQuery.executeStatement(pRequestContext, node, lUCon, lDeliverer);
        }
        catch (ExDB e) {
          throw new ExInternal("Failed to initialise node from query " + lDBQuery.getQualifiedName(), e);
        }
        finally {
          contextUCon.returnUCon(lUCon, "fm:init db query");
        }
      }
      finally {
        contextUElem.delocalise("fm:init with init-query");
      }
    }
    else if (nodeInfo.getAttribute("fox", "init-xpath") != null) {
      // Perform your XPath expression initialisation handling code here !
      String xpathExpr = nodeInfo.getAttribute("fox", "init-xpath");

      //---------------------------------------------------------------------
      // Temporarily change the attch point to the node we're initialising as
      // the runQuery processing requires it.
      //---------------------------------------------------------------------
      try {
        String xpathStrValue = contextUElem.extendedXPathString(node, xpathExpr);
        node.setText(xpathStrValue);
      }
      catch (ExRoot ex) {
        throw new ExInitialisation("Error during xpath initialisation of node, \"" + node.absolute() + "\" see nested exception for further information.", ex);
      }
    }
    else if (nodeInfo.getAttribute("", "fixed") != null) {
      node.setText(nodeInfo.getAttribute("", "fixed"));
    }
    else if (nodeInfo.getAttribute("", "default") != null) {
      node.setText(nodeInfo.getAttribute("", "default"));
    }
  }
}

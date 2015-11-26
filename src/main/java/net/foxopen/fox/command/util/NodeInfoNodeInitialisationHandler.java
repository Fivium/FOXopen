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
    // The order of the checks below reflects the order or precedence for
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
      // Temporarily change the attach point to the node we're initialising as
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

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

import java.util.ArrayList;

import net.foxopen.fox.PathDescriptor;
import net.foxopen.fox.PathElementDescriptor;
import net.foxopen.fox.command.builtin.BuiltInCommand;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInitialisation;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.thread.ActionRequestContext;


/**
 * Utility methods for FOX commands.
 */
public class CommandUtil
{
   /** Determines whether the specified XPath expression is a simple
    * expression. Refer to <code>DOM.isSimpleXPath</code> for the
    * rules regarding simple XML names.
    *
    * <p>This utility method conveniently throws an exception if
    * the specified XPath expression is not simple.
    *
    * @see net.foxopen.fox.dom.DOM#isSimpleXPath(String) for further information.
    * @param commandContext the command requesting this functionality
    * @param xPathExpr a simple XPath expression
    * @throws ExInternal thrown if an unexpeted error occurs
    * @exception ExActionFailed thrown if the command cannot proceed because
    *            the specified XPath expression is not deemed simple.
    */
   public static final void ensureXPathIsSimple(BuiltInCommand commandContext,
                                                String xPathExpr)
      throws ExInternal, ExActionFailed
   {
      if ( !DOM.isSimpleXPath(xPathExpr) )
      {
         throw new ExActionFailed("XPATH", "Error in use of \""+commandContext.getName()+"\" command - the specified XPath, \""+xPathExpr+
                                  "\" is not simple (it contains predicates and/or sub-terms)!");
      }
   }

   /**
    * Initialises a Data DOM node from a node in the schema Model DOM that
    * is relative (is the node or an ancestor) to the Data DOM Node.
    *
    * <p>This method accepts an instance of <code>NodeInitialisationHandler</code>, exploiting
    * the <b>visitor</b> pattern to control initial on a per-node basis if the caller
    * requires it.
    *
    * @param dataDOMNode the Data DOM node to initialise
    * @param relativeModelDOMNode a Model DOM node to initialise, that must be relative (in context) to the
    *        specified Data DOm node.
    * @param userThread the user thread performing this function
    * @param contextUElem the attach context of this function
    * @param contextUCon the DB connection context of this function
    * @param initialisationHandler an initialiser to perform the initialisation function for the
    *        Data DOM node(s) created.
    * @throws ExInternal thrown if an unexpeted error occurs
    * @exception ExActionFailed thrown if an error occurs performing the initialisation.
    * @see initialiseDataDOMFromMetaModelDOM(DOM,DOM,XThread,ContextUElem,ContextUCon)
    */
   public static final void initialiseDataDOMFromMetaModelDOM(ActionRequestContext pRequestContext,
                                                              DOM dataDOMNode,
                                                              DOM relativeModelDOMNode, NodeInitialisationHandler initialisationHandler)
      throws ExInternal, ExActionFailed
   {
      String dataDOMPath  = dataDOMNode.absolute();
      String modelDOMPath = relativeModelDOMNode.absolute();

      //------------------------------------------------------------------------
      // Ensure the specified Model DOM node resides below the Data DOM node.
      //------------------------------------------------------------------------
      if ( !modelDOMPath.startsWith(dataDOMPath) )
      {
         throw new ExActionFailed("COMMAND",
                                  "Error initialising node \""+modelDOMPath+"\" from Model DOM (Schema), "+
                                  " - the node is not relative to the requested parent node, \""+
                                  dataDOMPath+"\".");
      }
      else if (modelDOMPath.equals(dataDOMPath))
      {
         //---------------------------------------------------------------------
         // The specified Data DOM and Model DOM nodes relate to the same element.
         // Get in there and initialise the Data DOM node right away.
         //---------------------------------------------------------------------
         try
         {
            initialisationHandler.initialise(pRequestContext, dataDOMNode);
         }
         catch (ExInitialisation ex)
         {
            throw new ExActionFailed("INIT", "Failed to initialise Data DOM node \""+dataDOMNode.absolute()+"\":", ex);
         }
      }
      else
      {
         //---------------------------------------------------------------------
         // The Model DOM node exists as some descendant of the Data DOM node
         // in theory. Iterate down the hierarchy and find/create the path
         // below the Data DOM node recursively.
         //---------------------------------------------------------------------
         PathDescriptor dataDOMNodePath      = new PathDescriptor(dataDOMPath,  "/");
         PathDescriptor modelDOMNodePath     = new PathDescriptor(modelDOMPath,  "/");
         PathDescriptor modelDOMRelativePath = modelDOMNodePath.toRelativePath(dataDOMNodePath);

         DOM currentDOMNode = dataDOMNode;
         StringBuffer currentNodePath = new StringBuffer();
         currentNodePath.append(dataDOMPath);

         PathElementDescriptor pathElement = modelDOMRelativePath.getPathElement(0);
         currentNodePath.append("/").append(pathElement.getName());

         //------------------------------------------------------------------
         // Locate the relevant meta data of the current descendant node type.
         //------------------------------------------------------------------
         NodeInfo currentNodeInfo = pRequestContext.getCurrentModule().getNodeInfo(currentNodePath.toString());
         if ("phantom".equals(currentNodeInfo.getDataType()))
            return; // Do not initialise "phantom" types ref: FOX/IMP/BUG3003031438

         //------------------------------------------------------------------
         // Locate any existing nodes and calculate how many more we need
         // to create to meet the minimum cardinality rule.
         //------------------------------------------------------------------
         int minOccurs = currentNodeInfo.getMinCardinality();
         int maxOccurs = currentNodeInfo.getMaxCardinality();
         DOMList currentDOMNodes = currentDOMNode.getUL(pathElement.getName());
         int numberOfNewNodesToCreate = Math.min(Math.max(minOccurs, 1), maxOccurs) - currentDOMNodes.getLength();
         for (int i=0; i < numberOfNewNodesToCreate; i++)
         {
            currentDOMNodes.add(currentDOMNode.addElem(pathElement.getName()));
         }

         for (int i=0; i < currentDOMNodes.getLength(); i++)
         {
               initialiseDataDOMFromMetaModelDOM(pRequestContext,
                                                 currentDOMNodes.item(i),
                                                 relativeModelDOMNode,
                                                 initialisationHandler);
         }
      }
   }

   /**
    * Initialises a Data DOM node from a node in the schema Model DOM that
    * is relative (is the node or an ancestor) to the Data DOM Node.
    *
    * <p>This method <u>uses</u> an instance of <code>NodeInfoNodeInitialisationHandler</code> as the
    * <code>NodeInitialisationHandler</code> to use to initialise individual DOM node(s) created
    * along the way. Refer to the documentation on <code>NodeInfoNodeInitialisationHandler</code> for
    * further information on how this handler initialises nodes.
    *
    * @param dataDOMNode the Data DOM node to initialise
    * @param relativeModelDOMNode a Model DOM node, that must be relative (in context) to the
    *        specified Data DOm node.
    * @param userThread the user thread performing this function
    * @param contextUElem the attach context of this function
    * @param contextUCon the DB connection context of this function
    * @throws ExInternal thrown if an unexpeted error occurs
    * @exception ExActionFailed thrown if an error occurs performing the initialisation.
    */
   public static final void initialiseDataDOMFromMetaModelDOM(ActionRequestContext pRequestContext,
                                                              DOM dataDOMNode,
                                                              DOM relativeModelDOMNode)
      throws ExInternal, ExActionFailed
   {
      initialiseDataDOMFromMetaModelDOM(pRequestContext,
                                        dataDOMNode,
                                        relativeModelDOMNode,
                                        new NodeInfoNodeInitialisationHandler());
   }

   /**
    * Initialises a Data DOM node from a node in the schema Model DOM that
    * is relative (is the node or an ancestor) to the Data DOM Node.
    *
    * <p>This method accepts an instance of <code>NodeInitialisationHandler</code>, exploiting
    * the <b>visitor</b> pattern to control initial on a per-node basis if the caller
    * requires it.
    *
    * @param dataDOMNode the Data DOM node to initialise
    * @param relativeModelDOMNodes the Model DOM nodes to initialise, that must be relative (in context) to the
    *        specified Data DOm node.
    * @param userThread the user thread performing this function
    * @param contextUElem the attach context of this function
    * @param contextUCon the DB connection context of this function
    * @param initialisationHandler an initialiser to perform the initialisation function for the
    *        Data DOM node(s) created.
    * @throws ExInternal thrown if an unexpeted error occurs
    * @exception ExActionFailed thrown if an error occurs performing the initialisation.
    * @see initialiseDataDOMFromMetaModelDOM(DOM,DOM,XThread,ContextUElem,ContextUCon)
    *
    */
   public static final void initialiseDataDOMFromMetaModelDOMList(ActionRequestContext pRequestContext,
                                                                  DOM dataDOMNode,
                                                                  DOMList relativeModelDOMNodes, NodeInitialisationHandler initialisationHandler)
      throws ExInternal, ExActionFailed
   {
      for (int n=0; n < relativeModelDOMNodes.getLength(); n++)
      {
         initialiseDataDOMFromMetaModelDOM(pRequestContext,
                                           dataDOMNode,
                                           relativeModelDOMNodes.item(n),
                                           initialisationHandler);
      }
   }

   /**
    * Initialises a Data DOM node from a node in the schema Model DOM that
    * is relative (is the node or an ancestor) to the Data DOM Node.
    *
    * <p>This method <u>uses</u> an instance of <code>NodeInfoNodeInitialisationHandler</code> as the
    * <code>NodeInitialisationHandler</code> to use to initialise individual DOM node(s) created
    * along the way. Refer to the documentation on <code>NodeInfoNodeInitialisationHandler</code> for
    * further information on how this handler initialises nodes.
    *
    * @param dataDOMNode the Data DOM node to initialise
    * @param relativeModelDOMNodes the Model DOM nodes to initialise, that must be relative (in context) to the
    *        specified Data DOm node.
    * @param userThread the user thread performing this function
    * @param contextUElem the attach context of this function
    * @param contextUCon the DB connection context of this function
    * @throws ExInternal thrown if an unexpeted error occurs
    * @exception ExActionFailed thrown if an error occurs performing the initialisation.
    * @see initialiseDataDOMFromMetaModelDOM(DOM,DOM,XThread,ContextUElem,ContextUCon)
    *
    */
   public static final void initialiseDataDOMFromMetaModelDOMList(ActionRequestContext pRequestContext,
                                                                  DOM dataDOMNode,
                                                                  DOMList relativeModelDOMNodes)
      throws ExInternal, ExActionFailed
   {
      initialiseDataDOMFromMetaModelDOMList(pRequestContext,
                                            dataDOMNode,
                                            relativeModelDOMNodes,
                                            new NodeInfoNodeInitialisationHandler());
   }

   /** Augments any existing context, under the specified parent node context, with the
    * content node(s) specified.
    *
    * @param parentNode the parent node, under which new content will be created.
    * @param contentNodes the nodes to add, if not currently present under the parent.
    * @param deeply true if content is to be deeply created (all ancestors), false if only
    *        the top-level nodes of <i>contentNodes</i> are to be used.
    * @throws ExInternal thrown if an unexpeted error occurs.
    */
   public static final void augmentContent(DOM parentNode,
                                           DOMList contentNodes,
                                           boolean deeply)
      throws ExInternal
   {
      for (int n=0; n < contentNodes.getLength(); n++)
      {
         DOM contentNode = contentNodes.item(n);

         DOMList existingNodesOfSameName = parentNode.getUL(contentNode.getName());

         if (existingNodesOfSameName.getLength() == 0)
         {
            if (deeply)
               contentNode.copyToParent(parentNode);
            else
               parentNode.addElem(contentNode.getName());
         }
         else
         {
            // We have one or more existig nodes with the same name as this one
            // Decide whether we need to augment or add as new, depending on
            // the current node index.
            ArrayList contentNodesWithSameNameList = new ArrayList(contentNodes.getLength());
            for (int i=0; i < contentNodes.getLength(); i++)
            {
               if (contentNodes.item(i).getName().equals(contentNode.getName()))
                  contentNodesWithSameNameList.add(contentNodes.item(i));
            }
            int currentContentNodeIndexInList = contentNodesWithSameNameList.indexOf(contentNode);

            if (currentContentNodeIndexInList < existingNodesOfSameName.getLength())
            {
               // Augment the existing node with the same name with the corresponding
               // relativecontent node
               augmentContent(existingNodesOfSameName.item(currentContentNodeIndexInList),
                              contentNode.getChildElements(),
                              deeply);
            }
            else
            {
               if (deeply)
                  contentNode.copyToParent(parentNode);
               else
                  parentNode.addElem(contentNode.getName());
            }
         }

         /*
         if (existingNodesOfSameName.getLength() == 0 && deeply)
            contentNode.copyToParent(parentNode);
         else if (existingNodesOfSameName.getLength() == 0 && !deeply)
            parentNode.addElem(contentNode.getName());
         else if (existingNodesOfSameName.getLength() == 1 && deeply)
            augmentContent(existingNodesOfSameName.item(0), contentNode.getChildElements(), deeply);
            */
      }
   }

   /** Deeply augments any existing context, under the specified parent node context, with the
    * content node(s) specified.
    *
    * @param parentNode the parent node, under which new content will be created.
    * @param contentNodes the nodes to add, if not currently present under the parent.
    * @throws ExInternal thrown if an unexpeted error occurs.
    */
   public static final void augmentContentDeeply(DOM parentNode,
                                                 DOMList contentNodes)
      throws ExInternal
   {
      augmentContent(parentNode, contentNodes, true);
   }
}

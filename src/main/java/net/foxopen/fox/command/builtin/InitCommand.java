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
package net.foxopen.fox.command.builtin;

import java.util.Collection;
import java.util.Collections;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.module.Template;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.CommandUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * Simple command that logs some message.
 *
 * @author Gary Watson
 */
public class InitCommand
extends BuiltInCommand {

  private static final char[] VALIDATE_PATH_NOT_PIPE =
    "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ- _+[](){}<>*.:/'=!@".toCharArray();

  private final String templateName;
  private final String originalXPathExpression;
  private final String mForSchemaXPath;
  private final String minOccursStr;
  private final String maxOccursStr;
  private final String newCountStr;
  private final String method;
  private final String mTargetBaseName;
  private final String mTargetParentXPATH;

  /**
  * Contructs the command from the XML element specified.
  *
  * @param module the module where the command resides
  * @param commandElement the element from which the command will
  *      be constructed.
  * @exception ExInternal thrown if an unexpected error occurs initialising the command
  */
  private InitCommand(DOM commandElement) {
    super(commandElement);

    // Extract required values from command
    originalXPathExpression = getAttribute("target", ".");
    templateName  = getAttribute("template");
    mForSchemaXPath = getAttribute("for-schema");
    minOccursStr  = getAttribute("min-occurs");
    maxOccursStr  = getAttribute("max-occurs");
    newCountStr   = getAttribute("new-target-count");
    method      = getAttribute("method", "both");

    // Strip out target xpath prefix and basename
    StringBuffer lSB = new StringBuffer(originalXPathExpression);
    mTargetBaseName = XFUtil.pathPopTail(lSB);
    mTargetParentXPATH = XFUtil.nvl(lSB.toString(), originalXPathExpression.startsWith("/") ? "/" : ".");
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    try {

      ContextUElem contextUElem = pRequestContext.getContextUElem();

      //------------------------------------------------------------------------
      // WELCOME to the init command. Here are some assumptions:
      // - The 'target' could refer to existing Data DOM node(s) or, if none
      //  are found, it is assumed the target references a Model DOM node
      //  from which to construct new Data DOM node instances.
      //------------------------------------------------------------------------
      int minOccurs   = minOccursStr != null ? contextUElem.extendedConstantOrXPathResult(contextUElem.attachDOM(), minOccursStr).asNumber().intValue() : -1;
      int maxOccurs   = maxOccursStr != null ? contextUElem.extendedConstantOrXPathResult(contextUElem.attachDOM(), maxOccursStr).asNumber().intValue() : -1;
      int newCount    = newCountStr != null ? contextUElem.extendedConstantOrXPathResult(contextUElem.attachDOM(), newCountStr).asNumber().intValue() : 0;

      // Ensure exactly one parent target node exists - create or return if required
      DOMList lTargetParentDOMList;
      if("new".equals(method) || "both".equals(method)) {
        lTargetParentDOMList = contextUElem.getCreateXPathUL(mTargetParentXPATH);
        if(  lTargetParentDOMList.getLength()==0) {
         throw new ExInternal("no target parent elements internal error");
        }
      }
      else {
        lTargetParentDOMList = contextUElem.extendedXPathUL(mTargetParentXPATH, ContextUElem.ATTACH);
      }

      //------------------------------------------------------------------------
      // Obtain all required information:
      // 1) The current attach point (from which to run XPaths and reference
      //   the Model DOM).
      // 2) Any existing targets in the Data DOM
      // 3) The Model DOM node that corresponds to the targetted node(s) type.
      //------------------------------------------------------------------------
      DOM lTargetParentDOM;
      TARGET_PARENT_LOOP: while((lTargetParentDOM = lTargetParentDOMList.popHead())!=null ) {

        // 1)
        //DOM    currentAttachPoint  = contextUElem.getUElem(ContextUElem.ATTACH);
        // 2)

        //------------------------------------------------------------------------
        // If in "both" or "augment" modes, we need to look for existing targets.
        //------------------------------------------------------------------------
        DOMList  lTargetElementsDOMList;
        if ("both".equals(method) || "augment".equals(method)){
          if(XFUtil.isNull(mTargetBaseName)){
            //This can happen if user enters a dodgy target path e.g. ":{root}/ELEMENT/" - throw a descriptive error (xpath will fail)
            throw new ExActionFailed("COMMAND", "Unable to determine the name of the target node in InitCommand - check target path is valid: '" + originalXPathExpression + "'");
          }
          lTargetElementsDOMList = contextUElem.extendedXPathUL(lTargetParentDOM, mTargetBaseName);
        }
        else {
          lTargetElementsDOMList = new DOMList();
        }

        //---------------------------------------------------------------------
        // If in "both" or "new" modes, we may be required to create some new
        // target instances.
        //
        // Calculate the number of new target instances and create them.
        //---------------------------------------------------------------------
        if ("new".equals(method) || "both".equals(method)) {

          // Create a number of new occurrences - the greater of:
          // 1) the required new number of targets
          // 2) the range of maxOccurs to minOccurs taking into account any
          //   existing nodes.
          int lNewCount = newCount;
          if (lTargetElementsDOMList.getLength() == 0)
            lNewCount = Math.max(newCount, 1); // Adjust new target instances tp at least 1, if no targets currently exist

          int numberOfNewTargetsCreate = calculateNumberOfNewTargetsToCreate(lNewCount,
                                                      minOccurs,
                                                      maxOccurs,
                                                      lTargetElementsDOMList.getLength());

          for (int n=0; n < numberOfNewTargetsCreate; n++) {
            lTargetElementsDOMList.add(
              lTargetParentDOM.addElem(mTargetBaseName)
            );
          }
        }

        // When no target elements - stop processing
        if(lTargetElementsDOMList.getLength() == 0) {
         continue TARGET_PARENT_LOOP;
        }

        //------------------------------------------------------------------------
        // At this point we have valid target nodes of the required number and,
        // possibly, the corresponding Model DOM node.
        //
        // Obtain a tree sub-structure of the nodes to initialise beneath each
        // of the targets - either from the requested template, schema or none.
        //------------------------------------------------------------------------

        // Process for-schema clause
        if (mForSchemaXPath != null) {

          NodeInfo lTargetModelNodeInfo = ensureNodesAreOfSameType(pRequestContext.getCurrentModule(), lTargetElementsDOMList);

          //---------------------------------------------------------------------
          // Ensure, as we're performing a from schema initialisation request,
          // that we have a Model DOM context to work from.
          //---------------------------------------------------------------------
          if (lTargetModelNodeInfo == null) {
            throw new ExActionFailed("COMMAND"
            , "The \""+getName()+"\" command has been incorrectly used. The specified target XPath, \""+originalXPathExpression+
             "\", did not find any Data or Model DOM nodes so the \"for-schema\" attribute used, \""+mForSchemaXPath+"\",  "+
             "cannot be interpreted in this instance (not relative to any Model DOM node). Try specifying the "+
             "new-target-count, min-occurs atribute in order to create some new target instances.");
          }

          DOMList modelDOMTargets;
          try {
            modelDOMTargets = lTargetModelNodeInfo.getModelDOMElem().xpathUL(mForSchemaXPath, null);
          }
          catch(ExBadPath x) {
            throw new ExActionFailed("XPATH",
                            "Bad XPath expression used for \""+mForSchemaXPath+
                            "\" attribute in \""+getName()+"\" command.", x);
          }

          for (int n=0; n < lTargetElementsDOMList.getLength(); n++) {
            CommandUtil.initialiseDataDOMFromMetaModelDOMList(
              pRequestContext
            , lTargetElementsDOMList.item(n)
            , modelDOMTargets
            );
          }

        } // end process for-schema clause
        // Process template clause
        else if (templateName != null) {
          Template template = pRequestContext.getCurrentModule().getTemplate(templateName);
          if (template == null) {
            throw new ExInternal("Template with name '" + templateName + "' not found in module at top of callstack: " +  pRequestContext.getCurrentModule().getName());
          }

          DOM templateElementToCopy =  template.getTemplateElement();
          DOMList templateNodes = templateElementToCopy.getChildElements();

          for (int n=0; n < lTargetElementsDOMList.getLength(); n++) {
            CommandUtil.augmentContentDeeply(lTargetElementsDOMList.item(n),
                                  templateNodes);
          }
        }

      } // TARGET_PARENT_LOOP
    }
    catch (ExActionFailed e){
      //TODO fix this hack
      throw new ExInternal("init command", e);
    }

    return XDoControlFlowContinue.instance();
  }

  /**
  * Validates the syntax of the command.
  *
  * @param module the module where the component resides
  * @param commandElement the XML element that comprises the command
  * @throws ExInternal if the component syntax is invalid.
  */
  public void validate(Mod module) {
    final String errorPrefix = "Error parsing \""+getName()+"\" command in module \""+module.getName()+"\" - ";

    //String method      = getAttribute("method", "both");
    //String templateName  = getAttribute("template");
    //String forSchemaXPath = getAttribute("for-schema");
    //String newCount     = getAttribute("new-target-count");

    if (templateName != null) {
      Template template = module.getTemplate(templateName);
      if (template == null)
        throw new ExInternal(errorPrefix+"a template with name \""+templateName+"\" could not be found in the module.");
    }
    if (templateName != null && mForSchemaXPath != null) {
      throw new ExInternal(errorPrefix+"expected only one of the \"template\" or \"for-schema\" attributes but found both!");
    }

    if (method != null &&
       !"both".equals(method) &&
       !"new".equals(method) &&
       !"augment".equals(method)) {
      throw new ExInternal(errorPrefix+"unknown initialisation method encountered, \""+method+"\".");
    }

    if ("augment".equals(method) && newCountStr != null) {
      throw new ExInternal(errorPrefix+"method of \"augment\" used but \"new-target-count\" attribute specified. Perhaps the mixed mode of \"both\" is intended?");
    }
    if(!XFUtil.isCharacters(mTargetParentXPATH,VALIDATE_PATH_NOT_PIPE, 0, mTargetParentXPATH.length())) {
      throw new ExInternal("Init target-path must pipe-or (|): "
      + originalXPathExpression);
    }
  }

  private NodeInfo ensureNodesAreOfSameType(
   Mod module
  , DOMList nodes
  ) throws ExInternal, ExActionFailed
  {
    //userThread.getTopModule().getNodeInfo(contextUElem.getUElem(ContextUElem.ATTACH).absolute());
    NodeInfo firstNodeNodeInfo = null;

    if (nodes.getLength() > 0)
    {
      try {
        firstNodeNodeInfo = module.getNodeInfo(nodes.item(0));
        NodeInfo thisNodeInfo;
        boolean allSameType = true;

        for (int n=1; n < nodes.getLength() && allSameType; n++) {
          try {
            thisNodeInfo = module.getNodeInfo(nodes.item(n)); // currently throws ExInternal if not in the Model
            allSameType  = (firstNodeNodeInfo == null && thisNodeInfo == null) ||
                      (firstNodeNodeInfo != null && thisNodeInfo != null &&
                       firstNodeNodeInfo.getModelDOMElem().getName().equals(thisNodeInfo.getModelDOMElem().getName()));
            if ( !allSameType ) {
              throw new ExActionFailed("COMMAND",
                               "The use of the \"init\" command is in error - the targetted nodes, using XPath \""+originalXPathExpression+
                               ", are not all of the same type. The schema type of the first target node is \""+firstNodeNodeInfo.getModelDOMElem()+
                               "\" which conflicts with that of another node that has schema type \""+thisNodeInfo.getModelDOMElem()+"\".");
            }
          }
          catch (ExInternal ex) {
            throw new ExActionFailed("COMMAND",
                             "The use of the \"init\" command is in error - the targetted nodes, using XPath \""+originalXPathExpression+
                             ", are not all of the same type. The schema type of the first target node is \""+firstNodeNodeInfo.getModelDOMElem()+
                             "\" which conflicts with that of another node that has an unknown schema type \""+nodes.item(n).absolute()+"\".");
          }
        }
      }
      catch (ExInternal ex)
      {} // Ignore it as the node(s) are not in our Model DOM
    }

    return firstNodeNodeInfo;
  }

  /**
   * Determines, from the attribute values to the command, how many additional
   * target nodes should be created, under a common parent container.
   *
   * @param newTargetRequestCount the number of explicitly requested new targets
   * @param minOccursOrNegative the ninimum number of required occurrences, including
   *      any existing nodes. Can be negative.
   * @param maxOccursOrNegative the maximum number of required occurrences, including
   *      any existing nodes. Can be negative.
   * @param numberOfCurrentOccurrences the number of current instances of the target node
   *      type known to exist.
   * @return the calculated number of brand new target nodes to create.
   */
  private int calculateNumberOfNewTargetsToCreate(int newTargetRequestCount,
                                  int minOccursOrNegative,
                                  int maxOccursOrNegative,
                                  int numberOfCurrentOccurrences)
  {
    int numberOfNewTargets;
    int requestedTotalNumber = numberOfCurrentOccurrences + newTargetRequestCount;
    if (minOccursOrNegative >= 0 && maxOccursOrNegative >= 0) {
      if (requestedTotalNumber < minOccursOrNegative)
        numberOfNewTargets = minOccursOrNegative - numberOfCurrentOccurrences;
      else if (requestedTotalNumber > maxOccursOrNegative)
        numberOfNewTargets = maxOccursOrNegative - numberOfCurrentOccurrences;
      else
        numberOfNewTargets = newTargetRequestCount;
    }
    else if (minOccursOrNegative >= 0 && maxOccursOrNegative < 0)
      numberOfNewTargets = Math.max(minOccursOrNegative, requestedTotalNumber)-numberOfCurrentOccurrences;
    else if (minOccursOrNegative < 0 && maxOccursOrNegative >= 0)
      numberOfNewTargets = Math.min(maxOccursOrNegative, requestedTotalNumber)-numberOfCurrentOccurrences;
    else
      numberOfNewTargets = newTargetRequestCount;

    return numberOfNewTargets;
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new InitCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("init");
    }
  }
}

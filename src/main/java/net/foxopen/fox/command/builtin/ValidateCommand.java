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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FoxGbl;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.module.datanode.NodeType;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;


/**
 *  Validates and/or formats the a dom structure against its associated schema
 *  and inserts the DOM error Nodes where erros are found. Errors will
 *  accumulate the error in member variables for later query. This Validation
 *  has two broad categories: 1) Data or value validation - checks the values
 *  againts datatypes, ranges , mandatory vals etc the data to be validated is
 *  determined by a passed xpath expression 2) Structure validation - checks
 *  min/max occurences for basic xpath only Restrictions: a) Note that structure
 *  validation is determined by the same xpath expression as data validation
 *  which imposes limitations the Fox developer must be aware of e.g. given a
 *  definition of &lt;xs:element name="invoices"...&gt; &lt;xs:element name="lineno"
 *  minOccurs="3"&gt;... and a validate command of : &lt;fm:validate
 *  match="//invoices/lineno" check="ALL" ...&gt; an Xpath expression
 *  "invoices/lineno" will validate that at least 3 lineno's exists (and the
 *  values) however &lt;fm:validate match="//invoices/lineno[2]" check="ALL" ...&gt;
 *  will not validate that the 2nd lineno exists. It will check min/max
 *  structure of siblings only if a 2nd line number exists in the first place so
 *  beware The work around in this case is to use two commands as follows:
 *  &lt;fm:validate match="//invoices/lineno[2]" check="CONTENT" ...&gt; &lt;fm:validate
 *  match="//invoices/lineno" check="CARDINALITY" ...&gt;
 */
/*
 * TODO Perhaps data and structure validation be separate commands
 * to avoid confusion Also we could simplify code by just having a "validate
 * children structure" instead b) Based on references (global type definitions)
 * are not currently supported. c) Complex xs type definitions (xs:group,
 * xs:choice etc) are not currently supported.
 */
public class ValidateCommand
       extends BuiltInCommand {

  public final static String DATE_FORMAT_ERROR_MSG = "Please enter a valid date (e.g. 19-05-2015 or 09-MAY-15)";
  public final static String DATE_TIME_FORMAT_ERROR_MSG = "Please enter a valid date and time (e.g. 19-05-2015T13:16:39  or 09-MAY-15T13:16:39)";
  public final static String TIME_FORMAT_ERROR_MSG = "Please enter a valid time (e.g. 13:16:39 )";

  public final static String DATE_TIME_ERROR_MSG_FORMAT = "'Please enter a valid date and time (e.g.' " + FoxGbl.FOX_JAVA_DATE_TIME_FORMAT;

  private static class ExTooManyErrors extends RuntimeException {
   }

  /**
   *  Xpath selection string
   */
  private String mMatch = ".";
  /**
   *  True if element value needs to be validated
   */
  private boolean mCheckValue = true;
  /**
   *  True if element cardinality rules are to be checked
   */
  private boolean mCheckCardinality = true;
  /**
   *  True to indicate any missing requred nodes when validating (e.g. if
   *  minoccurs =3 and no nodes exist then 3 will be created)
   */
  private boolean mCreateWhenMissing = true;
  /**
   *  Any existing /error nodes will be deleted
   */
  private boolean mClearOldNodeErrors = false;
  /**
   *  Any existing <summary target>/error nodes will be deleted
   */
  private boolean mClearOldSummaryErrors = false;
  /**
   *  An xpath evaluating to one node where all summary errors will be inserted
   */
  private String mSummaryTarget;

  private String mUseWhenConditionsXPath;

  /**
   *  The maximum number of errors allowed before processing will abort. This
   *  saves the user waiting for the system to validate say 5000 errors when
   *  100 will indicate the whole problem anyway
   */
  private int mErrorLimit = 100;

  /**
   * Processes the commant to validate and/or format the a dom structure
   * against its associated schema
   *
   * @param pMod The current module
   * @param pCommandElement The validate command
   * @exception ExInternal Description of the Exception
   * @exception ExModule Description of the Exception
   */
  private ValidateCommand(DOM pCommandElement) throws ExDoSyntax {
    super(pCommandElement);
    parseCommand();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ValidateCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("validate");
    }
  }

  /**
   *  Parses the command structure. Relies on the XML Schema to ensure the
   *  command adheres to the required format.
   *
   * @exception  ExInternal  Description of the Exception
   * @exception  ExModule    Description of the Exception
   */
  private void parseCommand() throws ExDoSyntax {
    mMatch = getNonBlankAttribute("match", ".");
    mErrorLimit = new Integer(getNonBlankAttribute("error-limit", "100")).intValue();

    // Check param
    String param = getNonBlankAttribute("check", "ALL");
    if (param.equalsIgnoreCase("NONE")) {
      mCheckValue = false;
      mCheckCardinality = false;
    }
    else if (param.equalsIgnoreCase("CONTENT")) {
      mCheckValue = true;
      mCheckCardinality = false;
    }
    else if (param.equalsIgnoreCase("CARDINALITY")) {
      mCheckValue = false;
      mCheckCardinality = true;
    }
    else if (param.equalsIgnoreCase("ALL")) {
      // the default
      mCheckValue = true;
      mCheckCardinality = true;
    }
    else {
      throw new ExDoSyntax("Validate command contains Unknown check type " + param);
    }

    // init command
    if (getNonBlankAttribute("init", "y").equalsIgnoreCase("n")) {
      mCreateWhenMissing = false;
      // default is true
    }

    // Look at the Clear parameter
    param = getNonBlankAttribute("clear", "NONE");
    if (param.equalsIgnoreCase("NONE")) {
      // the default
      mClearOldNodeErrors = false;
      mClearOldSummaryErrors = false;
    }
    else if (param.equalsIgnoreCase("CLEAR-NODE")) {
      mClearOldNodeErrors = true;
      mClearOldSummaryErrors = false;
    }
    else if (param.equalsIgnoreCase("CLEAR-SUMMARY")) {
      mClearOldNodeErrors = false;
      mClearOldSummaryErrors = true;
    }
    else if (param.equalsIgnoreCase("BOTH")) {
      // TODO: get JB to change this to all
      mClearOldNodeErrors = true;
      mClearOldSummaryErrors = true;
    }
    else {
      throw new ExDoSyntax("Validate command contains Unknown clear type " + param);
    }

    // Target defaults to docroot/error-list
    mSummaryTarget = getNonBlankAttribute("summary-target", null);

    mUseWhenConditionsXPath = getNonBlankAttribute("use-when-conditions", "false()");
  }

   /**
   * Tests whether validation should be skipped for this node, based on the result of running its <tt>validate-when</tt>
   * XPath (or that of its parent).
   * @param pNode Node to be evaluated.
   * @param pModule Current Mod.
   * @param pContext Current ContextUElem.
   * @return True if validation should be skipped, false otherwise.
   */
   private static final boolean skipValidationTest(DOM pNode, Mod pModule, ContextUElem pContext){

     NodeInfo lNodeDfn = pModule.getNodeInfo(pNode);
     NodeInfo lParentDfn = pModule.getNodeInfo(pNode.getParentOrSelf());

     //Establish the target node for the test - either the node itself or its parent, depending on which has a condition defined on it
     //NodeInfos may be null if nodes are not in the schema
     NodeInfo lTargetNodeDfn;
     DOM lTargetNode;
     if(lNodeDfn != null && XFUtil.exists(lNodeDfn.getAttribute("fox", "validate-when"))){
       lTargetNodeDfn = lNodeDfn;
       lTargetNode = pNode;
     }
     else if(lParentDfn != null && XFUtil.exists(lParentDfn.getAttribute("fox", "validate-when"))){
       lTargetNodeDfn = lParentDfn;
       lTargetNode = pNode.getParentOrSelf();
     }
     else {
       return false;
     }

     String lValidateWhen = lTargetNodeDfn.getAttribute("fox", "validate-when");

     DOM lContextNode;
     if(lTargetNodeDfn.getNodeType() == NodeType.ITEM){
       lContextNode = lTargetNode.getParentOrNull();
     }
     else {
       lContextNode = lTargetNode;
     }

     boolean lResult;
     try {
       lResult = pContext.extendedXPathBoolean(lContextNode, lValidateWhen);
     }
     catch (ExInternal e) {
       throw new ExInternal("Error evaluating validate-when XPath ('" + lValidateWhen + "')", e);
     }
     catch (ExActionFailed e) {
       throw new ExInternal("Error evaluating validate-when XPath ('" + lValidateWhen + "')", e);
     }
     return !lResult;
   }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    ContextUCon lContextUCon = pRequestContext.getContextUCon();
    Mod lModule = pRequestContext.getCurrentModule();

    boolean lUseWhenConditions;
    try {
      lUseWhenConditions = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mUseWhenConditionsXPath);
    }
    catch(ExActionFailed e){
      throw new ExInternal("Error running use-when-conditions XPath " + mUseWhenConditionsXPath, e);
    }

    // TODO - Check if its possible that the same instance will be reentered? when will it be deleted?
    try {
      // set up Member variables
      //      currentAttach = pContextUElem.getUElem(ContextUElem.ATTACH); // current attach point
      if (mClearOldSummaryErrors) {
        // delete all errors
        DOMList errorNodes = null;
        if (mSummaryTarget == null) {
          errorNodes = lContextUElem.getUElem(ContextLabel.ERROR).getUL("error-list");
        }
        else {
          errorNodes = lContextUElem.getUElem(ContextLabel.ATTACH).getUL(mSummaryTarget);
        }
        errorNodes.removeFromDOMTree();
      }
    }
    catch (ExInternal x) {
      throw new ExInternal("Summary error target path invalid " + mSummaryTarget, x);
    }

    // parse through structure and/or data  picking up any validation errors as we go
    try {
      ErrorCtx errorCtx = new ErrorCtx(lContextUElem, lContextUCon);
      DOMList checkNodes = lContextUElem.extendedXPathUL(mMatch,ContextUElem.ATTACH);
      //DOMList checkNodes = errorCtx.mCurrentAttach.xpathUL(mMatch, null); // TODO: remove if new contexts work
      NODE_LOOP:
      for (int loop = 0; loop < checkNodes.getLength(); loop++) {
        if (checkNodes.item(loop).getName().equals("fox-error")) {
          // ignore error checking for fox-error nodes
        }
        else {
          //If we're using when conditions, check the node's when condition (and its parent) - skip node if necessary
          if(lUseWhenConditions && (skipValidationTest(checkNodes.item(loop), lModule, lContextUElem))){
            continue NODE_LOOP;
          }

          // assuming the xpath dosnt pick the same node twice clear any old errors
          if (mClearOldNodeErrors) {
            // delete all node errors
            checkNodes.item(loop).xpathUL("fox-error", null).removeFromDOMTree();
          }

          // Check this node/element for value errors
          if (mCheckValue) {
            checkValue(pRequestContext, checkNodes.item(loop), lModule, errorCtx, lContextUElem);
          }

          // Check this node and siblings min / max occurences
          if (mCheckCardinality) {
            checkOccurs(checkNodes.item(loop), lModule, errorCtx);
          }

          //TODO PN moved here to avoid running the XPath twice - however it seems redundant as it does largely the same as checkOccurs - need to QA and potentially remove
          if (mCheckCardinality){
            checkMinOccurs(checkNodes.item(loop), lModule, errorCtx);
          }
        }
      }
    }
    catch (ExBadPath x) {
      throw new ExInternal("Invalid match clause " + mMatch, x);
    }
    catch (ExTooManyErrors x) {
      // Fine just log it and get out of here
      Track.debug("Validating and more than " + mErrorLimit + " errors found. Checking stopped early");
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Error running validate command",e);
    }

    return XDoControlFlowContinue.instance();
  }



   /**
    *  Validates the given UELEM against its associated schema and sets the
    *  UELEM error Nodes where appropriate. At the moment we only validate the
    *  element value and not any of the attribute values against the
    *
    *@param  pAttrName  Description of the Parameter
    *@param  pDefault   Description of the Parameter
    *@param  pDataDfn   Description of the Parameter
    *@return            The attr value
    */
   private String getAttr(String pAttrName, String pDefault, NodeInfo pDataDfn) {
      String val = pDataDfn.getAttribute("fox", pAttrName);
      if (val != null)
      {
         val.trim();
      }
      if (val == null || val.equals(""))
      {
         val = pDefault;
      }
      return val;
   }


   /**
    *  Gets the attr attribute of the ValidateCommand object
    *
    *@param  pAttrName  Description of the Parameter
    *@param  pDataDfn   Description of the Parameter
    *@return            The attr value
    */
   private String getAttr(String pAttrName, NodeInfo pDataDfn) {
      String val = pDataDfn.getAttribute("fox", pAttrName);
      if (val != null)
      {
         val.trim();
      }
      else
      {
         val = "";
      }
      return val;
   }


   /**
    *  Gets the attrAsInteger attribute of the ValidateCommand object
    *
    *@param  pAttrName  Description of the Parameter
    *@param  pDataDfn   Description of the Parameter
    *@return            The attrAsInteger value
    */
   private Integer getAttrAsInteger(String pAttrName, NodeInfo pDataDfn) {
      String val = pDataDfn.getAttribute("", pAttrName);
      if (val != null)
      {
         return new Integer(val);
      }
      else
      {
         return null;
      }
   }


  /**
   * Adds the error to the summary error list
   *
   * @param pMessage The feature to be added to the SummaryError attribute
   * @param pErrorCtx The feature to be added to the SummaryError attribute
   * @return Description of the Return Value
   * @exception ExInternal Thrown if error summary already exists
   * @exception ExTooManyErrors Thrown if processed more errors than mErrorLimit
   */
  private DOM addSummaryError (String pMessage, ErrorCtx pErrorCtx)
    throws ExInternal, ExTooManyErrors
  {
    // first add an error to the summary target
    try {
      DOM errorRoot;
      if (pErrorCtx.mErrorsFound.size() > mErrorLimit) {
        if (mSummaryTarget == null) {
          errorRoot = pErrorCtx.mContextUElem.getUElem(ContextLabel.ERROR).getCreate1E("error-list");
        }
        else {
          errorRoot = pErrorCtx.mContextUElem.getUElem(ContextLabel.ATTACH).getCreate1E(mSummaryTarget);
        }
        errorRoot = errorRoot.addElem("fox-error");
        errorRoot.addElem("msg", "More than " + mErrorLimit + " errors found. Checking stopped");
        throw new ExTooManyErrors();
      }
      if (mSummaryTarget == null) {
        errorRoot = pErrorCtx.mContextUElem.getUElem(ContextLabel.ERROR).getCreate1E("error-list");
      }
      else {
        errorRoot = pErrorCtx.mContextUElem.getUElem(ContextLabel.ATTACH).getCreate1E(mSummaryTarget);
      }
      errorRoot = errorRoot.addElem("fox-error");
      errorRoot.addElem("msg", pMessage);
      return errorRoot;
    }
    catch (ExTooMany e) {
      throw new ExInternal("Duplicate error summary nodes exist", e);
    }
  }


   /**
    *  Adds the error to the summary and the node
    *
    *@param  pNode           The feature to be added to the Error attribute
    *@param  pMessage        The feature to be added to the Error attribute
    *@param  pErrorCtx       The feature to be added to the Error attribute
    *@exception  ExInternal  Description of the Exception
    */
   private void addError
         (DOM pNode
         , String pMessage
         , ErrorCtx pErrorCtx) throws ExInternal {
      // first add an error to the summary target
      DOM errorNode = addSummaryError(pMessage, pErrorCtx);

      // Reference the DOM element the error came from
      errorNode.addElem("reference", pNode.getRef());

      // Now add in a single error
      pNode.addElem("fox-error").addElem("msg", pMessage);
      pErrorCtx.mErrorsFound.add(new StoreErrors(pNode, pMessage));
   }


   /**
    *  Checks if the value is a valid decimal
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkDecimal(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx) throws ExInternal {
      // first add an error to the summary target
      try
      {
         double itemVal = Double.parseDouble(pDataToCheck.value());
         // check any value ranges
         if (pDataDfn.getMinInclusive() != null
               && itemVal < pDataDfn.getMinInclusive().doubleValue())
         {
            addError(pDataToCheck, "Value must be greater than or equal to " + pDataDfn.getMinInclusive(), pErrorCtx);
         }
         if (pDataDfn.getMinExclusive() != null
               && itemVal <= pDataDfn.getMinExclusive().doubleValue())
         {
            addError(pDataToCheck, "Value must be greater than " + pDataDfn.getMinExclusive(), pErrorCtx);
         }
         if (pDataDfn.getMaxInclusive() != null
               && itemVal > pDataDfn.getMaxInclusive().doubleValue())
         {
            addError(pDataToCheck, "Value must be less than or equal to " + pDataDfn.getMaxInclusive(), pErrorCtx);
         }
         if (pDataDfn.getMaxExclusive() != null
               && itemVal >= pDataDfn.getMaxExclusive().doubleValue())
         {
            addError(pDataToCheck, "Value must be less than " + pDataDfn.getMaxExclusive(), pErrorCtx);
         }
      }
      catch (Exception e)
      {
         addError(pDataToCheck, "Invalid decimal value " + pDataToCheck.value(), pErrorCtx);
      }
   }


   /**
    *  Checks if the value is a valid long data type
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkLong(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx) throws ExInternal {
      // first add an error to the summary target
      try
      {
         long itemVal = Long.parseLong(pDataToCheck.value());
         // check any value ranges
         checkDecimal(pDataToCheck, pDataDfn, pErrorCtx);
      }
      catch (Exception e)
      {
         addError(pDataToCheck, "Invalid number " + pDataToCheck.value(), pErrorCtx);
      }
   }


   /**
    *  Checks if the value is a valid integer data type
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkInteger(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx) throws ExInternal {
      // first add an error to the summary target
      try
      {

         double itemVal = Double.parseDouble(pDataToCheck.value());
         // check any Integ er max/min ranges
         if (itemVal > Integer.MAX_VALUE)
         {
            addError(pDataToCheck, "Value must be less than " + Integer.MAX_VALUE, pErrorCtx);
         }
         if (itemVal < Integer.MIN_VALUE)
         {
            addError(pDataToCheck, "Value must be greater than " + Integer.MIN_VALUE, pErrorCtx);
         }
         // check any value ranges
         checkDecimal(pDataToCheck, pDataDfn, pErrorCtx);

      }
      catch (Exception e)
      {
         addError(pDataToCheck, "Invalid integer value " + pDataToCheck.value(), pErrorCtx);
      }
   }


   /**
    *  Checks if the value is a valid whole number in xml an integer is
    *  different from an int in that it can have any maximum value
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkWholeNumber(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx) throws ExInternal {
      // although an integer can have a large value for FOX the maximum is as per a long
      // TODO: add in infinity checking
      checkLong(pDataToCheck, pDataDfn, pErrorCtx);
   }


   /**
    *  Checks if the value is a positive integer data type
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkPositiveInteger(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx) throws ExInternal {
      // first add an error to the summary target
      try
      {

         double itemVal = Double.parseDouble(pDataToCheck.value());
         // check any Integer max/min ranges
         if (itemVal <= 0)
         {
            addError(pDataToCheck, "Value must be positive ", pErrorCtx);
         }
         // ok its positive but is it an integer?
         checkInteger(pDataToCheck, pDataDfn, pErrorCtx);
      }
      catch (Exception e)
      {
         addError(pDataToCheck, "Invalid numeric value " + pDataToCheck.value(), pErrorCtx);
      }
   }


   /**
    *  Checks if the value is a negative integer data type
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkNegativeInteger(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx) throws ExInternal {
      // first add an error to the summary target
      try
      {

         double itemVal = Double.parseDouble(pDataToCheck.value());
         // check any Integer max/min ranges
         if (itemVal >= 0)
         {
            addError(pDataToCheck, "Value must be negative ", pErrorCtx);
         }
         // ok its positive but is it an integer?
         checkInteger(pDataToCheck, pDataDfn, pErrorCtx);

      }
      catch (Exception e)
      {
         addError(pDataToCheck, "Invalid numeric value " + pDataToCheck.value(), pErrorCtx);
      }
   }


   /**
    *  Checks the date format according to the fox date format string
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkDate(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx) throws ExInternal {
    try {
      DateFormat foxFormat = new SimpleDateFormat(FoxGbl.FOX_DATE_FORMAT);
      foxFormat.setLenient(false);
      Date itemVal = foxFormat.parse(pDataToCheck.value());
      // check any Integer max/min ranges
      checkDateRange(pDataToCheck, pDataDfn, pErrorCtx, itemVal);
    }
    catch (Exception e) {
      addError(pDataToCheck, DATE_FORMAT_ERROR_MSG, pErrorCtx);
    }
  }


  /**
   *  Checks the boolean true/false format (note y/n not allowed)
   *
   *@param  pDataToCheck    Dom containing the data to validate
   *@param  pErrorCtx       Description of the Parameter
   *@exception  ExInternal  Description of the Exception
   */
  private void checkBoolean(DOM pDataToCheck, ErrorCtx pErrorCtx) throws ExInternal {
    try {
      boolean itemVal = Boolean.getBoolean(pDataToCheck.value());
    }
    catch (Exception e) {
      addError(pDataToCheck, "Please Enter either 'true', 'false', '1' or '0' here", pErrorCtx);
    }
  }


   /**
    *  Checks the date format according to the fox date format string
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkTime(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx) throws ExInternal {
    try {
      DateFormat foxFormat = new SimpleDateFormat(FoxGbl.FOX_JAVA_TIME_FORMAT);
      foxFormat.setLenient(false);
      Date itemVal = foxFormat.parse(pDataToCheck.value());
      // check any Integer max/min ranges
      checkDateRange(pDataToCheck, pDataDfn, pErrorCtx, itemVal);
      // todo: check how the time component is stored in a double
      // if it adds on the current date or time then this will not work as time values will change
      // each time a module is parsed
    }
    catch (Exception e) {
      addError(pDataToCheck, TIME_FORMAT_ERROR_MSG, pErrorCtx);
    }
  }


   /**
    *  Checks the date time format ranges for a given date
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@param  pDateVal        Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkDateRange(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx, Date pDateVal) throws ExInternal {
      // check any  max/min ranges
      if (pDataDfn.getMinInclusive() != null
            && pDateVal.getTime() < pDataDfn.getMinInclusive().doubleValue())
      {
         addError(pDataToCheck, "Value must be greater than or equal to " + pDataDfn.getMinInclusive(), pErrorCtx);
      }
      if (pDataDfn.getMinExclusive() != null
            && pDateVal.getTime() <= pDataDfn.getMinExclusive().doubleValue())
      {
         addError(pDataToCheck, "Value must be greater than " + pDataDfn.getMinInclusive(), pErrorCtx);
      }
      if (pDataDfn.getMaxInclusive() != null
            && pDateVal.getTime() > pDataDfn.getMaxInclusive().doubleValue())
      {
         addError(pDataToCheck, "Value must be less than or equal to " + pDataDfn.getMinInclusive(), pErrorCtx);
      }
      if (pDataDfn.getMaxExclusive() != null
            && pDateVal.getTime() >= pDataDfn.getMaxExclusive().doubleValue())
      {
         addError(pDataToCheck, "Value must be less than " + pDataDfn.getMaxExclusive(), pErrorCtx);
      }
   }


   /**
    *  Checks the date time format according to the fox date format string
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pDataDfn        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkDateTime(DOM pDataToCheck, NodeInfo pDataDfn, ErrorCtx pErrorCtx) throws ExInternal {
      try
      {
         DateFormat foxFormat = new SimpleDateFormat(FoxGbl.FOX_JAVA_DATE_TIME_FORMAT);
         foxFormat.setLenient(false);
         Date itemVal = foxFormat.parse(pDataToCheck.value());
         checkDateRange(pDataToCheck, pDataDfn, pErrorCtx, itemVal);
      }
      catch (Exception e)
      {
         addError(pDataToCheck, DATE_TIME_FORMAT_ERROR_MSG, pErrorCtx);
      }
   }


   /**
    *  Checks the minimum occurences of the model DOM element against the data
    *  dom element
    *
    *@param  pNodeToCheck    Description of the Parameter
    *@param  pXThread        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkMinOccurs(DOM pNodeToCheck, Mod pModule, ErrorCtx pErrorCtx) throws ExInternal {
      // TODO: this is a model dom so can I still use nodeinfo?
      NodeInfo nodeDfn = pModule.getNodeInfo(pNodeToCheck);
      if (nodeDfn == null)
      {
         return;
         // For now just exit if no node info found
         // TODO: ask JB if this should be an exception
      }

      Integer minOccurs = null;
      try
      {
         try
         {
            // get min occurs
            minOccurs = getAttrAsInteger("minOccurs", nodeDfn);
         }
         catch (NumberFormatException ex)
         {
            throw new ExInternal("Error validating min/max occurs structure", ex);
         }
         // only check cardinality if its specified in the element definition
         if (minOccurs != null && minOccurs.intValue() > 0)
         {
            //grab a node list of all the parent nodes
            DOMList parentNodes = pErrorCtx.mCurrentAttach.getUL(pNodeToCheck.getParentOrNull().absolute());

            if (parentNodes.getLength() == 0)
            {
               // create parent nodes as well
               pErrorCtx.mCurrentAttach.create1E(pNodeToCheck.absolute());
            }
            for (int loop = 0; loop < parentNodes.getLength(); loop++)
            {
               if (pErrorCtx.mProcessedNodes.contains(parentNodes.item(loop).getRef() + "/" + pNodeToCheck.getName()))
               {
                  // dont check a node if you have already checked one of its siblings
               }
               else
               {
                  /* OK now we CHEAT
                   * if we are here then we have not found a single occurrence of this element in the previous pass
                   * so the item does not exists so flag an error or create it
                   */

                  // Do we need to create an empty structure
                  if (mCreateWhenMissing)
                  {
                     // add Default structure. Note no defaults or any fancy pants stuff just add a blank node

                     for (int i = 0; i <= minOccurs.intValue(); i++)
                     {
                        parentNodes.item(loop).create1E(pNodeToCheck.getName());
                     }
                  }
                  else
                  {
                     addError(pNodeToCheck.getParentOrNull(), "You must enter at least " + minOccurs + " " + pNodeToCheck.getName() + " entries ", pErrorCtx);
                  }
                  // Avoid processing this node again
                  pErrorCtx.mProcessedNodes.add(parentNodes.item(loop).getRef() + "/" + pNodeToCheck.getName());
               }

            }


         }
         // its a wrap register that we have already processed this node and all its siblings
         pErrorCtx.mProcessedNodes.add(pNodeToCheck.absolute());
      }
      catch (ExInternal ex)
      {
         throw new ExInternal("Error validating structure for node", pNodeToCheck, ex);
      }
      catch (ExTooMany ex)
      {
         throw new ExInternal("Error validating structure for node duplicate parents found", pNodeToCheck, ex);
      }
   }


   /**
    *  Checks the minimum/maximum occurences of the DOM element
    *
    *@param  pNodeToCheck    Description of the Parameter
    *@param  pXThread        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkOccurs(DOM pNodeToCheck, Mod pModule, ErrorCtx pErrorCtx) throws ExInternal {
      if (pErrorCtx.mProcessedNodes.contains(pNodeToCheck.getParentOrNull().getRef() + "/" + pNodeToCheck.getName()))
      {
         return;
         // dont check a node if you have already checked one of its siblings
      }
      NodeInfo nodeDfn = pModule.getNodeInfo(pNodeToCheck);
      if (nodeDfn == null)
      {
         return;
         // For now just exit if no node info found
         // todo: ask jason if this should be an exception
      }

      Integer minOccurs = null;
      Integer maxOccurs = null;
      try
      {
         try
         {
            // get min occurs
            minOccurs = getAttrAsInteger("minOccurs", nodeDfn);
            // get max occurs
            maxOccurs = getAttrAsInteger("maxOccurs", nodeDfn);
         }
         catch (NumberFormatException ex)
         {
            throw new ExInternal("Error validating min/max occurs structure", ex);
         }
         // only check cardinality if its specified in the element definition
         if (minOccurs != null || maxOccurs != null)
         {
            // find exact number of elements
            int siblingCount = pNodeToCheck.getParentOrNull().getUL(pNodeToCheck.getName()).getLength();
            // TODO: above check is inefficient. May need to create a getSiblingCount method but
            // have a chat to JB first about the other todo's

            // minimum check
            if (minOccurs != null && siblingCount < minOccurs.intValue())
            {
               // Do we need to create an empty structure
               if (mCreateWhenMissing)
               {
                  // add Default structure. Note no defaults or any fancy pants stuff just add a blank node
                  // todo: ask Jason if he wants this to recurse to the nth degree, what about defaults etc?
                  for (int i = siblingCount; i <= minOccurs.intValue(); i++)
                  {
                     pNodeToCheck.getParentOrNull().addElem(pNodeToCheck.getName());
                  }
               }
               else
               {
                  addError(pNodeToCheck.getParentOrNull(), "You must enter at least " + minOccurs + " entries ", pErrorCtx);
               }
            }
            // maximum check
            if (maxOccurs != null && siblingCount > maxOccurs.intValue())
            {
               addError(pNodeToCheck.getParentOrNull(), "You must enter no more than " + maxOccurs + " entries ", pErrorCtx);
            }
         }
         // its a wrap! so register that we have already processed this node and all its siblings
         pErrorCtx.mProcessedNodes.add(pNodeToCheck.getParentOrNull().getRef() + "/" + pNodeToCheck.getName());
      }
      catch (ExInternal ex)
      {
         throw new ExInternal("Error validating cardinality for node", pNodeToCheck, ex);
      }
   }


   /**
    *  Validates the given UELEM data bvalue against its attribute directives
    *
    *@param  pDataToCheck    Dom containing the data to validate
    *@param  pXThread        Description of the Parameter
    *@param  pErrorCtx       Description of the Parameter
    *@exception  ExInternal  Description of the Exception
    */
   private void checkValue(ActionRequestContext pRequestContext, DOM pDataToCheck, Mod pModule, ErrorCtx pErrorCtx, ContextUElem pContextUElem)
   throws ExActionFailed
   {
      // todo: think about adding a "check if already proccessed" here like in checkOccurs
      // however it may actually be more processing work than not doing anything

      String dataVal = pDataToCheck.value();
      NodeInfo dataDfn = pModule.getNodeInfo(pDataToCheck);
      if (dataDfn == null)
      {
         return;
         // For now just exit if no node info found
         // TODO: ask JB if this should be an exception
      }
      String attrVal = null;

      // Validate the element first
      try
      {

         boolean blankEntry = false;
         // true if the data item is blank or null
         boolean mandatory = false;
         // true if the data item must be entered
         if (dataVal == null || dataVal.trim().equals(""))
         {
            // Flag item as blank
            // NOTE: Code added so flag only set when item contains no significant elements - JB
            // NOTE: This was required for validation of complex items derrived from complex "data" map-sets
            // NOTE: that where not marked up as complex in the modules schema.
            if(!dataDfn.getIsItem()
            || 0 == pDataToCheck.getChildElements().removeAllNamesFromList("fox-error").removeAllNamesFromList("fox-history").getLength()
            )
            {
               blankEntry = true;
            }

            String mandatoryXpath = dataDfn.getAttribute("fox", "mand");
            // Mandatory validate
            if (!(mandatoryXpath == null || mandatoryXpath.equals("")))
            {
               try
               {
                  // if the key is defined then validate for first complex node using "THE RULE" se jason about "THE RULE"
                  if (dataDfn.getIsItem())
                  {
                     mandatory = pContextUElem.extendedXPathBoolean(pDataToCheck, mandatoryXpath);
                  }
                  else
                  {
                      mandatory = pContextUElem.extendedXPathBoolean(pDataToCheck.getParentOrSelf(), mandatoryXpath);
                  }
               }
               catch (ExInternal ex)
               {
                  throw new ExInternal("Invalid xpath (" + mandatoryXpath + ") given for fox:mand attribute ", ex);
               }
            }
         }
         else
         {
            //       if dataVal != null

            // Check min field width
            if (dataDfn.getMinDataLength() != null && dataDfn.getMinDataLength().intValue() > 0)
            {
               if (dataVal == null || dataVal.length() < dataDfn.getMinDataLength().intValue())
               {
                  addError(pDataToCheck, "You must enter at least " + dataDfn.getMinDataLength() + " characters", pErrorCtx);
               }
            }

            // Maximum length (covers length as well)
            if (dataDfn.getMaxDataLength() != null)
            {
               if (dataVal.length() > dataDfn.getMaxDataLength().intValue())
               {
                  addError(pDataToCheck, "You can not enter more than " + dataDfn.getMaxDataLength() + " characters here", pErrorCtx);
               }
            }

            // total digits
            if (dataDfn.getTotalDigits() != null)
            {
               if (dataVal.length() > dataDfn.getTotalDigits().intValue())
               {
                  int pos = dataVal.indexOf(".");
                  if (pos > 0 && dataVal.length() - 1 <= dataDfn.getTotalDigits().intValue())
                  {
                     // No problem here as one of the digits is a decimal point
                  }
                  else
                  {
                     addError(pDataToCheck, "You can not enter more than " + dataDfn.getTotalDigits() + " digits here", pErrorCtx);

                  }
               }
            }

            // fraction digits
            if (dataDfn.getFractionDigits() != null)
            {
               int pos = dataVal.indexOf("."); // could be 0 if number is of form ".1234" instead of "0.1234"
               if (pos >= 0 &&
                     (dataVal.substring(pos + 1).length() > dataDfn.getFractionDigits().intValue()))
               {
                  addError(pDataToCheck, "You can not enter more than " + dataDfn.getFractionDigits() + " digits after the decimal point", pErrorCtx);
               }
            }

            // Check numeric data types
            String lDataType = dataDfn.getDataType();
            if ("xs:decimal".equals(lDataType)) {
               checkDecimal(pDataToCheck, dataDfn, pErrorCtx);
            }
            else if ("xs:long".equals(lDataType)) {
               checkLong(pDataToCheck, dataDfn, pErrorCtx);
            }
            else if ("xs:int".equals(lDataType)) {
               checkInteger(pDataToCheck, dataDfn, pErrorCtx);
            }
            else if ("xs:positiveInteger".equals(lDataType)) {
               checkPositiveInteger(pDataToCheck, dataDfn, pErrorCtx);
            }
            else if ("xs:negativeInteger".equals(lDataType)) {
               checkNegativeInteger(pDataToCheck, dataDfn, pErrorCtx);
            }
            else if ("xs:integer".equals(lDataType)) {
               checkWholeNumber(pDataToCheck, dataDfn, pErrorCtx);
            }
            else if ("xs:date".equals(lDataType)) {
               checkDate(pDataToCheck, dataDfn, pErrorCtx);
            }
            else if ("xs:dateTime".equals(lDataType)) {
               checkDateTime(pDataToCheck, dataDfn, pErrorCtx);
            }
            else if ("xs:time".equals(lDataType)) {
               checkTime(pDataToCheck, dataDfn, pErrorCtx);
            }
            else if ("xs:boolean".equals(lDataType)) {
               checkBoolean(pDataToCheck, pErrorCtx);
            }

         }

         // No need to do list validation if the item is blank and its not mandatory
         if (!mandatory && blankEntry)
         {
         }
         // do nothing
         else
         {
            // Ennumeration (valid values)
            if (dataDfn.getSchemaEnumerationOrNull() != null)
            {
               if (dataDfn.getSchemaEnumerationOrNull().contains(dataVal))
               {
                  // its found in the list so its no longer mandatory
                  mandatory = false;
               }
               else
               {
                  // only add this error if the entry is not blank
                  // blank entries willl be validated later on in the mandatory validation
                  if (!blankEntry)
                  {
                     addError(pDataToCheck, "Unrecognized entry", pErrorCtx);
                  }
               }
            }

            // Pattern (valid values dictated by regular expressions)
            List<Pattern> schemaPatterns = dataDfn.getSchemaPatternOrNull();
            if (schemaPatterns != null) {
              boolean patternMatched = false;

              for (Pattern lPattern : schemaPatterns) {
                Matcher lMatcher = lPattern.matcher(dataVal);
                patternMatched = lMatcher.matches();
                if (patternMatched == true) {
                  break;
                }
              }

              if (patternMatched == false) {
                // only add this error if the entry is not blank
                // blank entries willl be validated later on in the mandatory validation
                if (!blankEntry) {
                  addError(pDataToCheck, "Entry is in an incorrect format", pErrorCtx);
                }
              }
            }

            // validate xpath details
            String validateXpath = dataDfn.getAttribute("fox", "validate-xpath");
            // Mandatory validate
            if (!(validateXpath == null || validateXpath.equals("")))
            {
               try
               {
                  if (!pContextUElem.extendedXPathBoolean(pDataToCheck.getParentOrSelf(), validateXpath))
                  {
                     String lValidateXPathMsg = dataDfn.getAttribute("fox", "validate-xpath-msg");
                     if(!XFUtil.isNull(lValidateXPathMsg)){
                       lValidateXPathMsg = pContextUElem.extendedStringOrXPathString(pDataToCheck.getParentOrSelf(), lValidateXPathMsg);
                     }
                     addError(pDataToCheck, XFUtil.nvl(lValidateXPathMsg, "Invalid entry (no xpath match)"), pErrorCtx);
                  }
               }
               catch (ExInternal ex)
               {
                  throw new ExInternal("Invalid xpath (" + validateXpath + ") given for fox:validate-xpath attribute ", ex);
               }
            }
            // Map-set validate
            try
            {
               if (!getAttr("map-set", dataDfn).equals(""))
               {
                  if (getAttr("validate-map-set", "y", dataDfn).equalsIgnoreCase("y"))
                  {

                    MapSet lMapSet = pRequestContext.resolveMapSet(getAttr("map-set", dataDfn), pDataToCheck, getAttr("map-set-attach", dataDfn));

                     if(lMapSet.containsData(pDataToCheck)) {
                        // its found in the list so its no longer mandatory
                        mandatory = false;
                     }
                     else
                     {
                        addError(pDataToCheck, XFUtil.nvl(dataDfn.getAttribute("fox", "validate-map-set-msg"), "Invalid entry (not on list)"), pErrorCtx);
                     }
                  }
               }
            }
            catch (ExInternal ex)
            {
               throw new ExInternal("Error validating item against map set", ex);
            }

         }

         // Is the item mandatory?
         if (blankEntry && mandatory)
         {
            String lValidateGeneralMsg = dataDfn.getAttribute("fox", "validate-general-msg");
            if(!XFUtil.isNull(lValidateGeneralMsg)){
              lValidateGeneralMsg = pContextUElem.extendedStringOrXPathString(pDataToCheck.getParentOrSelf(), lValidateGeneralMsg);
            }
            addError(pDataToCheck, XFUtil.nvl(lValidateGeneralMsg, "You must enter this item"), pErrorCtx);
         }
      }
      catch (ExInternal ex)
      {
         throw new ExInternal("Error validating element data", pDataToCheck, ex);
      }

      // Do we need to validate child nodes?
      /*    if (!pDataToCheck.hasContent()
       *while (pDataToCheck.hasContent()) {
       *if (!mModuleDfn.getNodeInfo(currentNode.absolute()).f_list) {
       *relativePath = "../"+currentNode.getName();
       *subSubRecursive(mapID, relativePath);
       *}
       *        set current_node to match of current_node/..
       *}
       */
   }


   /**
    *  Nested class to store error context information
    *
    *@author     Clayton
    *@created    June 9, 2003
    */
   private static class ErrorCtx {
      /**
       *  The current attach point
       */
      DOM mCurrentAttach;
      /**
       *  The current dom context
       */
      ContextUElem mContextUElem;
      /**
       *  The current database attach
       */
      ContextUCon mContextUCon;
      /**
       *  All errors found during the validate used aminly for debugging
       *  Purposes
       */
      ArrayList mErrorsFound = new ArrayList();
      /**
       *  Stores processed nodes to avoid re-validating validated nodes
       */
      ArrayList mProcessedNodes = new ArrayList();


      /**
       *  Various context elements used to shortcut passing several elements to
       *  each function
       *
       *@param  pContextUElem  The current dom context
       *@param  pContextUCon   Description of the Parameter
       */
      ErrorCtx(ContextUElem pContextUElem, ContextUCon pContextUCon) {
         mContextUElem = pContextUElem;
         mCurrentAttach = pContextUElem.getUElem(ContextLabel.ATTACH);
         mContextUCon = pContextUCon;
      }


      /**
       *  Retuns the number of errors currently loaded in the instance
       *
       *@return    The total number of errors
       */
      int getErrorCount() {
         return mErrorsFound.size();
      }

   }


   /**
    *  Nested class to store information about errors recorded
    *
    *@author     Clayton
    *@created    June 9, 2003
    */

   /**
    *  A local error store (used mainly for debugging)
    *
    *@author     Clayton
    *@created    June 9, 2003
    */
   private static class StoreErrors {
      /**
       *  Xpath to retreive nodes to validate
       */
      String mXpath;
      // Xpath to tag name todo: see if this includes iteration e.g. [2]
      //    String mID ; // Unique Message id
      //    String types ; // type of error such as warning, fatal error etc
      /**
       *  the error message
       */
      String mMessage;
      // The message for the error


      /**
       *  add in the error message and the location todo: disable this when not
       *  debugging
       *
       *@param  pUElem          The element in error
       *@param  pMessage        The message
       *@exception  ExInternal  Description of the Exception
       *@throws  ExInternal     Internal error
       */
      StoreErrors(DOM pUElem, String pMessage)
             throws
            ExInternal {
         mXpath = pUElem.absolute();
         //      mID = pID;
         mMessage = pMessage;
      }

   }
   // class storeErrors

  public boolean isCallTransition() {
   return false;
  }

}

/*

Copyright (c) 2010, ENERGY DEVELOPMENT UNIT (INFORMATION TECHNOLOGY)
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

*/

package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Fox Command replicating standard Case statement behaviour based purely on text value evaluation and comparison.
 *
 * Attributes:
 *  "value" - Text value to compare with (Mutually Exclusive with "expr").
 *  "expr" - XPath expression which, when evaluated, should return a text value to compare with.
 *  "continue-matching" - Text boolean "true"/"false" specifies whether more than one "when" should be matched upon
 *
 *Elements:
 * Case - Root element, attributes specified act as the switch for other "when" statements
 *      - Valid Attributes: "value", "expr"
 *
 * When - Child of Case, Contains the match cases for switch to trigger and subsequent Do clause.
 *      - Valid Attributes: "value", "expr", "continue-matching"
 *
 * Default - Child of Case, after When. Do clause is triggered when either a previous When "continue-matching" resolves true
 *           Or no When "expr"/"value" matches the Case "expr"/"value"
 */
public class CaseCommand extends BuiltInCommand {

  /** Child elements contained in this "case" element (including elements not part of "case").*/
  DOMList mChildElems;

  /** Occurrences of "when" and "default" elements as part of the "case" clause.*/
  private int mWhenCount = 0;
  private int mDefaultCount = 0;

  /** List containing the CaseComponent of each "when" child element of "case".*/
  private List<CaseComponent> mWhens = new ArrayList<CaseComponent>();

  /** CaseComponent of the "default" element.*/
  private CaseComponent mDefault;

  /** Limited CaseComponent containing only the "expr" and "value" of root "case" element.*/
  private CaseComponent mCase;

  /**
   * Utility class for describing any element component of "case".
   */
  private class CaseComponent {
    /** "expr", "value", and "continue-matching" attributes of the component */
    protected String mExpr;
    protected String mValue;
    protected String mCont;

    /** Do clause of the element */
    protected XDoCommandList mClause;

    /** Fully constructs a CaseComponent object */
    protected CaseComponent (Mod pMod, DOM pDOM)
    throws ExInternal, ExDoSyntax {
      mExpr = pDOM.getAttr("expr");
      mValue = pDOM.getAttr("value");
      mCont = pDOM.getAttr("continue-matching");
      mClause = new XDoCommandList(pMod, pDOM);
      mClause.validate(pMod);
    }

    /** Partially constructs a CaseComponent object, specifically only the "expr" and "value" attributes  */
    protected CaseComponent (String pExpr, String pValue){
      mExpr = pExpr;
      mValue = pValue;
    }

    /**
     * Returns either either the XPath evaluation of "expr" or the string value of "value", these are mutually exclusive.
     *
     * @param pContextUElem
     * @return Text evaluation of "expr" or (when "expr" is null) the contents of "value"
     * @throws ExActionFailed
     */
    protected String getComparator(ContextUElem pContextUElem) throws ExActionFailed {
      return !XFUtil.isNull(mExpr) ? pContextUElem.extendedXPathString(pContextUElem.attachDOM(), mExpr) : mValue;
    }

    /**
     * Checks for mutual exclusivity between "expr" and "value".
     * @return
     */
    protected boolean isMutuallyExclusive(){
      return XFUtil.isNull(mExpr) ^ XFUtil.isNull(mValue);
    }
  }

  /**
  * Constructs a case command from the XML element specified.
  *
  * @param pCommandElement the element from which the command will
  *        be constructed.
  */
  private CaseCommand(Mod pModule, DOM pCommandElement) throws ExDoSyntax {
    super(pCommandElement);

    mCase = new CaseComponent(pCommandElement.getAttr("expr"), pCommandElement.getAttr("value"));

    if(!mCase.isMutuallyExclusive()){
      throw new ExInternal("Error parsing \"case\" command in module \""+pModule.getName()+
                           "\" - Attributes \"expr\" and \"value\" are mutually exclusive, either are required when defining a \"case\" clause.", pCommandElement);
    }
    mChildElems = pCommandElement.getChildElements();

    for (int i=0; i<mChildElems.getLength(); i++) {
      if ("when".equals(mChildElems.item(i).getLocalName())) {
        mWhens.add(new CaseComponent(pModule, mChildElems.item(i)));
      }
      if ("default".equals(mChildElems.item(i).getLocalName())) {
        mDefault = new CaseComponent (pModule, mChildElems.item(i));
        mDefaultCount++;
      }
    }
    mWhenCount = mWhens.size();

    if ((mWhenCount + mDefaultCount) != mChildElems.getLength()) {
      throw new ExInternal("Error parsing \"case\" command in module \""+pModule.getName()+
                           "\" - Unexpected element defined in \"case\" clause.", pCommandElement);
    }

    if (mWhenCount == 0) {
      throw new ExInternal("Error parsing \"case\" command in module \""+pModule.getName()+
                           "\" - \"when\" clause has not been defined in \"case\" clause.", pCommandElement);
    }

    if(mDefaultCount > 1){
      throw new ExInternal("Error parsing \"case\" command in module \""+pModule.getName()+
                           "\" - Expected only one \"default\" clause in \"case\" clause.", pCommandElement);
    }

    if (mChildElems.item(mChildElems.getLength() - 1).getLocalName().equals("when") && mDefaultCount == 1) {
      throw new ExInternal("Error parsing \"case\" command in module \""+pModule.getName()+
                           "\" - \"default\" defined, but not found at the end of \"case\" clause.", pCommandElement);
    }

    for (CaseComponent lWhen : mWhens) {
      if (!lWhen.isMutuallyExclusive()){
        throw new ExInternal("Error parsing \"case\" command in module \""+pModule.getName()+
                             "\" - Attributes \"expr\" and \"value\" are mutually exclusive, one is required where defined on a \"when\" clause.", pCommandElement);
      }
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    XDoRunner lRunner = pRequestContext.createCommandRunner(false);
    XDoControlFlow lFlowResult = XDoControlFlowContinue.instance();

    try {
      String lPivot = mCase.getComparator(lContextUElem);
      if (!XFUtil.isNull(lPivot)) {
        for (CaseComponent lWhen : mWhens) {
          if (lPivot.equals(lWhen.getComparator(lContextUElem))) {
            lRunner.runCommands(pRequestContext, lWhen.mClause);
            if ("true".equals(lWhen.mCont) && lFlowResult.canContinue()) {
              continue;
            }
            else {
              return lFlowResult;
            }
          }
        }
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Error evaluating case command", e);
    }

    if(!XFUtil.isNull(mDefault)){
      lFlowResult = lRunner.runCommands(pRequestContext, mDefault.mClause);
    }

    return lFlowResult;
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new CaseCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("case");
    }
  }
}

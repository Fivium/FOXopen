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

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dbinterface.QueryMode;
import net.foxopen.fox.dbinterface.deliverer.InterfaceQueryResultDeliverer;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.paging.DatabasePager;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.dom.paging.PagerSetup;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Arrays;
import java.util.Collection;

//TODO pn rename to run-query when original command deleted
public class RunQueryCommand
extends BuiltInCommand {

  private final String mInterfaceName;
  private final String mQueryName;
  private final String mMatchPath;
  private final QueryMode mQueryMode;

  //Can be null if no pagination setup defined
  private final PagerSetup mPagerSetup;

  // Parse command returning tokenised representation
  public RunQueryCommand(Mod pMod, DOM pParseUElem)
  throws ExDoSyntax {
    super(pParseUElem);

    mInterfaceName = pParseUElem.getAttr("interface");
    mQueryName = pParseUElem.getAttr("query");
    mMatchPath = pParseUElem.getAttr("match");

    if(XFUtil.isNull(mInterfaceName)) {
      throw new ExInternal("Interface name must be specified");
    }
    else if(XFUtil.isNull(mQueryName)) {
      throw new ExInternal("Query name must be specified");
    }

    String lModeString = pParseUElem.getAttr("mode");
    if(!XFUtil.isNull(lModeString)) {
      try {
        mQueryMode = QueryMode.fromExternalString(lModeString);
      }
      catch (ExModule e) {
        throw new ExInternal("Invalid query mode on query " + mQueryName, e);
      }
    }
    else {
      mQueryMode = null;
    }

    //Validate the query is defined
    InterfaceQuery lInterfaceQuery = pMod.getDatabaseInterface(mInterfaceName).getInterfaceQuery(mQueryName);

    try {
      mPagerSetup = PagerSetup.fromDOMMarkupOrNull(pParseUElem, "pagination-definition", "page-size", "pagination-invoke-name", lInterfaceQuery.getPaginationDefnitionName());
    }
    catch (ExModule e) {
      throw new ExInternal("Error in run-query definition for query " + mQueryName, e);
    }
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    InterfaceQuery lInterfaceQuery = pRequestContext.getCurrentModule().getDatabaseInterface(mInterfaceName).getInterfaceQuery(mQueryName);

    //Default the match path to current attach point if not specified
    String lMatch = mMatchPath;
    if(XFUtil.isNull(lMatch)) {
      lMatch = ".";
    }

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    //Evaluate the match XPath
    DOMList lMatchList;
    try {
      lMatchList = lContextUElem.extendedXPathUL(lMatch, null);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to run query match XPath for query " + mQueryName, e);
    }

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Run Query " + mQueryName);
    try {
      //Loop through all matched nodes - execute the query and deliver to the match node
      for (DOM lMatchNode : lMatchList) {
        try {
          //Establish a pager (and possibly bind provider for the pager) if there is a pager definition
          DatabasePager lPager = null;
          DecoratingBindObjectProvider lBindProvider = null;
          if(mPagerSetup != null) {
            lPager = pRequestContext.getModuleFacetProvider(PagerProvider.class).getOrCreateDatabasePager(mPagerSetup.evalute(pRequestContext, lMatchNode.getFoxId()), lInterfaceQuery);
            //The pager might need to provide additional bind details (this can be null)
            lBindProvider = lPager.getDecoratingBindProviderOrNull(lInterfaceQuery);
          }

          //Construct a new deliverer based on the query mode (ADD-TO, PURGE-ALL, etc)
          QueryResultDeliverer lDeliverer = InterfaceQueryResultDeliverer.getDeliverer(pRequestContext, lInterfaceQuery, mQueryMode, lMatchNode, lPager);

          //Run the statement into the deliverer
          lInterfaceQuery.executeStatement(pRequestContext, lMatchNode, lUCon, lBindProvider, lDeliverer);
        }
        catch (ExDB e) {
          throw new ExInternal("Failed to run query " + mQueryName, e);
        }
      }
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Run Query " + mQueryName);
    }

    return XDoControlFlowContinue.instance();
  }

  public boolean isCallTransition() {
   return false;
  }
  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new RunQueryCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Arrays.asList("run-query", "run-query2");
    }
  }
}

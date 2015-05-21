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

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dbinterface.DatabaseInterface;
import net.foxopen.fox.dbinterface.InterfaceAPI;
import net.foxopen.fox.dbinterface.QueryMode;
import net.foxopen.fox.dbinterface.deliverer.InterfaceAPIResultDeliverer;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.Collection;
import java.util.Collections;


public class RunApiCommand
extends BuiltInCommand {

  private final String mInterfaceName;
  private final String mApiName;
  private final String mMatchPath;
  private final QueryMode mMode;

  // Parse command returning tokenised representation
  private RunApiCommand(DOM pParseUElem) {
    super(pParseUElem);

    mInterfaceName = pParseUElem.getAttr("interface");
    mApiName = pParseUElem.getAttr("api");
    mMatchPath = XFUtil.nvl(pParseUElem.getAttr("match"), ".");

    String lModeString = pParseUElem.getAttr("mode");
    if(!XFUtil.isNull(lModeString)) {
      try {
        mMode = QueryMode.fromExternalString(lModeString);
      }
      catch (ExModule e) {
        throw new ExInternal("Invalid mode specified on fm:run-api " + mApiName, e);
      }
    }
    else {
      mMode = null;
    }

    if(XFUtil.isNull(mInterfaceName)) {
      throw new ExInternal("Interface name must be specified");
    }
    else if(XFUtil.isNull(mApiName)) {
      throw new ExInternal("API name must be specified");
    }
  }

  public boolean isCallTransition() {
   return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    DatabaseInterface lDbInterface = pRequestContext.getCurrentModule().getDatabaseInterface(mInterfaceName);
    InterfaceAPI lInterfaceAPI = lDbInterface.getInterfaceAPI(mApiName);
    try {

      ContextUElem lContextUElem = pRequestContext.getContextUElem();
      ContextUCon lContextUCon = pRequestContext.getContextUCon();

      DOMList lMatchedNodes = lContextUElem.extendedXPathUL(lContextUElem.attachDOM(), mMatchPath);

      UCon lUCon = lContextUCon.getUCon("Run API " + mApiName);
      try {
        //Execute the API once for every matched node
        for(DOM lMatchedNode : lMatchedNodes) {

          //Legacy behaviour: purge-all mode removed child elements of the matched DOM
          //TODO look up mode on api definition if null here
          if (mMode == QueryMode.PURGE_ALL) {
            lMatchedNode.removeAllChildren();
          }

          //In we're in dev mode and in a transaction we're not allowed to commit, take the transaction ID now so we can check this API doesn't commit internally
          boolean lDoTransactionCheck = FoxGlobals.getInstance().isDevelopment() && !lContextUCon.isTransactionControlAllowed();
          String lTransactionIdBefore = "";
          if(lDoTransactionCheck) {
            lTransactionIdBefore  = lUCon.getTransactionId();
          }

          //Run the API and deliver the results to the target DOM
          lInterfaceAPI.executeStatement(pRequestContext, lMatchedNode, lUCon, new InterfaceAPIResultDeliverer(pRequestContext, lInterfaceAPI, lMatchedNode));

          try {
            //We're only concerned about the API changing (i.e. committing/rolling back) an existing transaction - starting a new transaction is OK
            if(lDoTransactionCheck && !"".equals(lTransactionIdBefore)) {
              String lTransactionIdAfter = lUCon.getTransactionId();
              if(!lTransactionIdBefore.equals(lTransactionIdAfter)) {
                Track.alert("APITransactionCheck", "API " + mApiName + " in interface " + mInterfaceName + " (match node " + lMatchedNode.getFoxId() + ") modified the FOX transaction! " +
                  "Transaction ID before: " + lTransactionIdBefore + ", Transaction ID after: " + XFUtil.nvl(lTransactionIdAfter, "[none]"), TrackFlag.CRITICAL);
              }
            }
          }
          catch (Throwable th) {
            Track.recordSuppressedException("Suppressed exception caused by transaction ID check", th);
          }
        }
      }
      finally {
        lContextUCon.returnUCon(lUCon, "Run API " + mApiName);
      }
    }
    catch (Exception ex) {
      throw new ExInternal("XDo run-api " + lInterfaceAPI.getQualifiedName() + " failed in module " + pRequestContext.getCurrentModule().getName(), ex);
    }

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new RunApiCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("run-api");
    }
  }
}

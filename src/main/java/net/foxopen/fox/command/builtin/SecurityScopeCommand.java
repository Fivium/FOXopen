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

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.StringUtil;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;


public class SecurityScopeCommand
  extends BuiltInCommand {

  /**
   * The default system-level privileges to apply for the current user; defaults to all.
   */
  private final String mCSVSystemPrivileges;

  /**
   * An XPath that identifies one or more Universal References to objects whose privileges are to be deteremined.
   */
  private final String mURefXPath;

  /**
   * The restriction subset, if any, of object-level priveleges to determine for the specified object URefs for the current user. Can be an XPATH
   */
  private final String mCSVObjectPrivileges;

  /**
   * The restriction subset, if any, of object Universal Reference types to determine for the specified object URefs for the current user.
   */
  private final String mCSVURefTypes;

  /**
   * Contructs an if command from the XML element specified.
   *
   * @param commandElement the element from which the command will be constructed.
   */
  private SecurityScopeCommand(DOM commandElement) throws ExInternal {
    super(commandElement);

    mCSVSystemPrivileges = commandElement.getAttrOrNull("csv-system-privs");

    mURefXPath = commandElement.getAttrOrNull("uref-xpath");

    mCSVObjectPrivileges = commandElement.getAttrOrNull("csv-object-privs");

    mCSVURefTypes = commandElement.getAttrOrNull("csv-uref-types");

  }

  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem contextUElem = pRequestContext.getContextUElem();
    String lCSVURefList = "";
    String lCSVObjectPrivileges = "";
    String lCSVSystemPrivileges = "*";
    String lCSVURefTypes = "";

    if (mURefXPath != null) {
      XPathResult xpResult;
      try {
        xpResult = contextUElem.extendedXPathResult(contextUElem.getUElem(ContextLabel.ATTACH), mURefXPath);
        lCSVURefList = StringUtil.collectionToDelimitedString(xpResult.asStringList(), ",");
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate UREF XPath for security-scope command", e);
      }
    }

    if (mCSVObjectPrivileges!= null) {
      try {
        lCSVObjectPrivileges = contextUElem.extendedStringOrXPathString(contextUElem.attachDOM(), mCSVObjectPrivileges);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate Object Privileges XPath for security-scope command", e);
      }
    }


    if (mCSVSystemPrivileges!=null) {
      try {
        lCSVSystemPrivileges = contextUElem.extendedStringOrXPathString(contextUElem.attachDOM(), mCSVSystemPrivileges);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate System Privileges XPath for security-scope command", e);
      }
    }

    if(mCSVURefTypes!=null) {
      try {
        lCSVURefTypes = contextUElem.extendedStringOrXPathString(contextUElem.attachDOM(), mCSVURefTypes);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate URef Types XPath for security-scope command", e);
      }
    }

    SecurityScope lSecurityScope = new SecurityScope(lCSVSystemPrivileges, mURefXPath, lCSVURefList, lCSVObjectPrivileges, lCSVURefTypes);
    pRequestContext.changeSecurityScope(lSecurityScope);

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
    implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new SecurityScopeCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("security-scope");
    }
  }
}

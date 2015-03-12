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


public class SecurityScopeCommand
extends BuiltInCommand {

  /** The default system-level privileges to apply for the current user; defaults to all. */
  private String csvSystemPrivileges = "*";

  /** An XPath that identifies one or more Universal References to objects whose privileges are to be deteremined. */
  private String URefXPath           = null;

  /** The restriction subset, if any, of object-level priveleges to determine for the specified object URefs for the current user. */
  private String csvObjectPrivileges = "";

  /** The restriction subset, if any, of object Universal Reference types to determine for the specified object URefs for the current user. */
  private String csvURefTypes        = "";

  /**
  * Contructs an if command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private SecurityScopeCommand(DOM commandElement) throws ExInternal {
    super(commandElement);

    if ( commandElement.getAttrOrNull("csv-system-privs") != null )
      csvSystemPrivileges = commandElement.getAttrOrNull("csv-system-privs");
    if ( commandElement.getAttrOrNull("uref-xpath") != null )
      URefXPath = commandElement.getAttrOrNull("uref-xpath");
    if ( commandElement.getAttrOrNull("csv-object-privs") != null )
      csvObjectPrivileges = commandElement.getAttrOrNull("csv-object-privs");
    if ( commandElement.getAttrOrNull("csv-uref-types") != null )
      csvURefTypes = commandElement.getAttrOrNull("csv-uref-types");
  }

  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem contextUElem = pRequestContext.getContextUElem();
    String csvURefList = "";
    if ( URefXPath != null ) {
      XPathResult xpResult;
      try {
        xpResult = contextUElem.extendedXPathResult(contextUElem.getUElem(ContextLabel.ATTACH), URefXPath);
        csvURefList = StringUtil.collectionToDelimitedString(xpResult.asStringList(), ",");
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluated UREF XPath for security-scope command", e);
      }
    }

    SecurityScope lSecurityScope = new SecurityScope(csvSystemPrivileges, URefXPath, csvURefList, csvObjectPrivileges, csvURefTypes);
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

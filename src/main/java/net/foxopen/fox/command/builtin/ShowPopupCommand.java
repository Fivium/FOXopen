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

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.PopupXDoResult;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;


/**
 * Implementation of the fm:show-poup command; triggers browser popup windows with a
 * document or other binary payload.
 */
public class ShowPopupCommand
extends BuiltInCommand {

  /** The URI (relative or full URL) of the resource to retrieve and display
    * in the popup. */
  private String mURI;

  /** The browser client window name. */
  private String mWindowName;

  /** The javascript window features. */
  private String mJsWinFeatures;

  /**
   * Contructs an if command from the XML element specified.
   * @param pMod a reference to the parent module
   * @param commandElement the element from which the command will
   * be constructed.
   * @throws ExInternal
   */
  private ShowPopupCommand(DOM commandElement) throws ExInternal {
    super(commandElement);

    mURI = commandElement.getAttrOrNull("uri");
    mWindowName = commandElement.getAttrOrNull("window-name");
    mJsWinFeatures = commandElement.getAttrOrNull("js-win-features");
  }

  /**
    * Returns a unique name for a popup window.
    *
    * @return a unique window name
    */
  private String getUniqueWindowName() {
    return "popup" + XFUtil.unique();
  }

  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    String lPopupWindowName = (mWindowName != null ? mWindowName : getUniqueWindowName());

    String lResolvedLink = pRequestContext.createURIBuilder().buildStaticResourceOrFixedURI(mURI);
    pRequestContext.addXDoResult(new PopupXDoResult(lResolvedLink, lPopupWindowName, mJsWinFeatures));

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {

      String lURI = pMarkupDOM.getAttrOrNull("uri");
      String lStorageLocation = pMarkupDOM.getAttrOrNull("storage-location");
      String lDocTemplateName = pMarkupDOM.getAttrOrNull("doc-template-name");
      String lDbInterface = pMarkupDOM.getAttrOrNull("db-interface");

      //Validate only one show-popup type is specified
      int lNumUsages = (lURI != null ? 1 : 0) + (lStorageLocation != null ? 1 : 0) + (lDocTemplateName != null ? 1 : 0) + (lDbInterface != null ? 1 : 0);
      if (lNumUsages != 1) {
        throw new ExInternal("The show-popup command has been used incorrectly in module \"" + pModule.getName() +
                             " - the command is URI, storage location, documnent template or query based and, as such, exactly one of " +
                             "\"uri\", \"storage-location\", \"document-template-name\" or \"db-interface\" should be specified.");
      }

      if(lURI != null) {
        return new ShowPopupCommand(pMarkupDOM);
      }
      else if(lStorageLocation != null) {
        return new ShowPopupStorageLocationCommand(pMarkupDOM);
      }
      else if(lDocTemplateName != null) {
        throw new ExInternal("The show-popup command is no longer supported for document templates. To preview a document template"
                             + " please use the Document Generation plugin.");
      }
      else if(lDbInterface != null) {
        return new ShowPopupQueryCommand(pModule, pMarkupDOM);
      }
      else {
        throw new ExInternal("No show-popup command available");
      }
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("show-popup");
    }
  }
}

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
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.CompareXml;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;


/**
 * Simple command that sends alerts to the users browser.
 *
 * @author Philip Simpson
 */
public class CompareCommand
extends BuiltInCommand
{
  private String mContextOne;
  private String mContextTwo;
  private String mContextOut;
  private String mContextOneVersion;
  private String mContextTwoVersion;
  private String mModuleName;
  private String mDisplayStyle;

  /**
  * Contructs the command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private CompareCommand(DOM commandElement)
  throws ExInternal {
    super(commandElement);
    parseCommand(commandElement);
  }

  /**
  * Parses the command structure. Relies on the XML Schema to
  * ensure the command adheres to the required format.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  private void parseCommand(DOM commandElement)
  throws ExInternal {
    mContextOne = commandElement.getAttrOrNull("context-one");
    mContextTwo = commandElement.getAttrOrNull("context-two");
    if (mContextOne == null || mContextTwo == null) {
      throw new ExInternal("Compare command must have the attributes context-one and context-two");
    }
    mContextOut = commandElement.getAttrOrNull("context-out");
    if (mContextOut == null) {
      throw new ExInternal("Compare command must have the attribute context-out");
    }
    mContextOneVersion = commandElement.getAttrOrNull("version-one");
    mContextTwoVersion = commandElement.getAttrOrNull("version-two");
    if (mContextTwoVersion == null) {
      throw new ExInternal("Compare command must have the version numbers for the old document (version-two attribute)");
    }
    mModuleName = commandElement.getAttrOrNull("schema-module");

    mDisplayStyle = commandElement.getAttrOrNull("display-style");
    if (mDisplayStyle == null) {
      mDisplayStyle = CompareXml.COMPARE_DISPLAY_LEGACY;
    }
    else if (!CompareXml.COMPARE_DISPLAY_LEGACY.equals(mDisplayStyle) &&  !CompareXml.COMPARE_DISPLAY_HINT.equals(mDisplayStyle)) {
      throw new ExInternal("Specified display-style '" + mDisplayStyle + "' is invalid");
    }
  }

  /**
   * Runs the command with the specified user thread and session.
   *
   * @param userThread the user thread context of the command
   * @return userSession the user's session context
   */
  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext){

    ContextUElem pContextUElem = pRequestContext.getContextUElem();
    try {
      DOM elementOne;
      try {
        elementOne = pContextUElem.extendedXPath1E(mContextOne);
      }
      catch (ExCardinality e) {
        throw new ExInternal("The compare function can not find the element specified in context-one");
      }

      DOM elementTwo;
      try {
        elementTwo = pContextUElem.extendedXPath1E(mContextTwo);
      }
      catch (ExCardinality e) {
        throw new ExInternal("The compare function can not find the element specified in context-two");
      }

      String versionTwo = pContextUElem.extendedStringOrXPathString(elementTwo,mContextTwoVersion);

      Mod schemaModule;
      if (mModuleName == null) {
        schemaModule =  pRequestContext.getCurrentModule();
      } else {
        mModuleName = pContextUElem.extendedStringOrXPathString(pContextUElem.getUElem(ContextLabel.ATTACH),mModuleName);
        try {
          schemaModule = pRequestContext.getModuleApp().getMod(mModuleName);
        } catch (ExServiceUnavailable ex) {
          throw new ExInternal("Can not find module for the compare command",ex);
        } catch (ExUserRequest ex) {
          throw new ExInternal("Can not find module for the compare command",ex);
        } catch (ExApp ex) {
          throw new ExInternal("Can not find app for the compare command",ex);
        } catch (ExModule ex) {
          throw new ExInternal("Can not find the module which is used the compare command: "+mModuleName,ex);
        }
      }
      try {
        CompareXml comparexml = new CompareXml(pContextUElem, mDisplayStyle);
        DOM elementOutResult = comparexml.compareElements(
          elementOne,
          elementTwo,
          versionTwo==null?"":versionTwo,
          schemaModule
        );
         DOM elementOut = pContextUElem.extendedXPath1E(mContextOut);
        elementOut.removeAllChildren();
        elementOutResult.copyContentsTo(elementOut);
      } catch (ExModule ex) {
        throw new ExInternal("The compare has create an error",ex);
      }
      catch (ExCardinality e){
        throw new ExActionFailed("BADPATH", "Cardinality exception when outputting comparison result", e);
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to run compare command",e);
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
      return new CompareCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("compare");
    }
  }
}

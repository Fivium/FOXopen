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
package net.foxopen.fox.command;

import com.google.common.base.Joiner;
import net.foxopen.fox.command.builtin.ActionCallCommand;
import net.foxopen.fox.command.builtin.AlertCommand;
import net.foxopen.fox.command.builtin.AssertCommand;
import net.foxopen.fox.command.builtin.AssertEqualsCommand;
import net.foxopen.fox.command.builtin.AssertFailsCommand;
import net.foxopen.fox.command.builtin.AssignmentCommand;
import net.foxopen.fox.command.builtin.AttachCommand;
import net.foxopen.fox.command.builtin.AugmentCommand;
import net.foxopen.fox.command.builtin.CaseCommand;
import net.foxopen.fox.command.builtin.ClearVariableCommand;
import net.foxopen.fox.command.builtin.ClosePopoverCommand;
import net.foxopen.fox.command.builtin.CompareCommand;
import net.foxopen.fox.command.builtin.ContextClearCommand;
import net.foxopen.fox.command.builtin.ContextLocaliseCommand;
import net.foxopen.fox.command.builtin.ContextSetCommand;
import net.foxopen.fox.command.builtin.CopyCommand;
import net.foxopen.fox.command.builtin.EvalCommand;
import net.foxopen.fox.command.builtin.ExitModuleCommand;
import net.foxopen.fox.command.builtin.FocusCommand;
import net.foxopen.fox.command.builtin.ForEachCommand;
import net.foxopen.fox.command.builtin.GenerateCommand;
import net.foxopen.fox.command.builtin.GeneratePDFCommand;
import net.foxopen.fox.command.builtin.GoToPageCommand;
import net.foxopen.fox.command.builtin.IfCommand;
import net.foxopen.fox.command.builtin.InitCommand;
import net.foxopen.fox.command.builtin.LogCommand;
import net.foxopen.fox.command.builtin.ModuleCallCommand;
import net.foxopen.fox.command.builtin.MoveCommand;
import net.foxopen.fox.command.builtin.OrderCommand;
import net.foxopen.fox.command.builtin.PostDOMCommand;
import net.foxopen.fox.command.builtin.PragmaCommand;
import net.foxopen.fox.command.builtin.RefreshMapsetCommand;
import net.foxopen.fox.command.builtin.RefreshPagerCommand;
import net.foxopen.fox.command.builtin.RemoveCommand;
import net.foxopen.fox.command.builtin.RenameCommand;
import net.foxopen.fox.command.builtin.RunApiCommand;
import net.foxopen.fox.command.builtin.RunQueryCommand;
import net.foxopen.fox.command.builtin.SecurityScopeCommand;
import net.foxopen.fox.command.builtin.SetVariableCommand;
import net.foxopen.fox.command.builtin.ShowPopoverCommand;
import net.foxopen.fox.command.builtin.ShowPopupCommand;
import net.foxopen.fox.command.builtin.StateCommand;
import net.foxopen.fox.command.builtin.SwitchTabCommand;
import net.foxopen.fox.command.builtin.ThrowCommand;
import net.foxopen.fox.command.builtin.TransactionCommand;
import net.foxopen.fox.command.builtin.TryCommand;
import net.foxopen.fox.command.builtin.UserLoginCommand;
import net.foxopen.fox.command.builtin.UserLogoutCommand;
import net.foxopen.fox.command.builtin.ValidateCommand;
import net.foxopen.fox.command.builtin.WhileCommand;
import net.foxopen.fox.command.builtin.XSLTransformCommand;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.StatusDestination;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.plugin.PluginManager;
import net.foxopen.fox.plugin.api.command.FxpCommandFactory;

import java.util.Map;
import java.util.TreeMap;


/**
 * A factory that manufactures FOX commands by name.
 *
 * @author Gary Watson
 */
public class CommandProvider {

  /** The singleton instance of the factory. */
  private static final CommandProvider INSTANCE = new CommandProvider();

  private Map<String, CommandFactory> mCommandNameToFactoryMap = new TreeMap<>();

  private void registerCommandFactory(CommandFactory pCommandFactory) {
    for(String lElementName : pCommandFactory.getCommandElementNames()) {
      if(mCommandNameToFactoryMap.containsKey(lElementName)) {
        throw new ExInternal("Command element " + lElementName + " already has a definition corresponding to Factory " + mCommandNameToFactoryMap.get(lElementName).getClass().getName() +
                             " - cannot add a duplicate definition for " + pCommandFactory.getClass().getName());
      }
      else {
        mCommandNameToFactoryMap.put(lElementName, pCommandFactory);
      }
    }
  }

  /**
  * Singleton instance constructor for CommandFactory.
  */
  private CommandProvider() {
    EngineStatus.instance().registerStatusProvider(new CommandStatusProvider());

    registerCommandFactory(new ActionCallCommand.Factory());
    registerCommandFactory(new AlertCommand.Factory());
    registerCommandFactory(new AssertCommand.Factory());
    registerCommandFactory(new AssertEqualsCommand.Factory());
    registerCommandFactory(new AssertFailsCommand.Factory());
    registerCommandFactory(new AssignmentCommand.Factory());
    registerCommandFactory(new AttachCommand.Factory());
    registerCommandFactory(new AugmentCommand.Factory());
    registerCommandFactory(new CaseCommand.Factory());
    registerCommandFactory(new ClearVariableCommand.Factory());
    registerCommandFactory(new ClosePopoverCommand.Factory());
    registerCommandFactory(new CompareCommand.Factory());
    registerCommandFactory(new ContextClearCommand.Factory());
    registerCommandFactory(new ContextLocaliseCommand.Factory());
    registerCommandFactory(new ContextSetCommand.Factory());
    registerCommandFactory(new CopyCommand.Factory());
    registerCommandFactory(new EvalCommand.Factory());
    registerCommandFactory(new ExitModuleCommand.Factory());
    registerCommandFactory(new FocusCommand.Factory());
    registerCommandFactory(new ForEachCommand.Factory());
    registerCommandFactory(new GenerateCommand.Factory());
    registerCommandFactory(new GeneratePDFCommand.Factory());
    registerCommandFactory(new GoToPageCommand.Factory());
    registerCommandFactory(new IfCommand.Factory());
    registerCommandFactory(new InitCommand.Factory());
    registerCommandFactory(new LogCommand.Factory());
    registerCommandFactory(new ModuleCallCommand.Factory());
    registerCommandFactory(new MoveCommand.Factory());
    registerCommandFactory(new OrderCommand.Factory());
    registerCommandFactory(new PostDOMCommand.Factory());
    registerCommandFactory(new PragmaCommand.Factory());
    registerCommandFactory(new RefreshMapsetCommand.Factory());
    registerCommandFactory(new RefreshPagerCommand.Factory());
    registerCommandFactory(new RemoveCommand.Factory());
    registerCommandFactory(new RenameCommand.Factory());
    registerCommandFactory(new RunApiCommand.Factory());
    registerCommandFactory(new RunQueryCommand.Factory());
    registerCommandFactory(new SecurityScopeCommand.Factory());
    registerCommandFactory(new SetVariableCommand.Factory());
    registerCommandFactory(new ShowPopoverCommand.Factory());
    registerCommandFactory(new ShowPopupCommand.Factory());
    registerCommandFactory(new StateCommand.Factory());
    registerCommandFactory(new SwitchTabCommand.Factory());
    registerCommandFactory(new ThrowCommand.Factory());
    registerCommandFactory(new TransactionCommand.Factory());
    registerCommandFactory(new TryCommand.Factory());
    registerCommandFactory(new UserLoginCommand.Factory());
    registerCommandFactory(new UserLogoutCommand.Factory());
    registerCommandFactory(new ValidateCommand.Factory());
    registerCommandFactory(new WhileCommand.Factory());
    registerCommandFactory(new XSLTransformCommand.Factory());
  }

  /**
  * Returns the factory singleton instance.
  *
  * @return the factory instance
  */
  public static CommandProvider getInstance() {
    return INSTANCE;
  }

  /**
  * Returns an instance of a <code>Command</code> represented by
  * the XML element specified.
  *
  * @param pMarkupDOM the element that represents the command
  * @return an instance of the command that encapsulates the command logic
  */
  public Command getCommand(Mod pMod, DOM pMarkupDOM){

    String lCommandName  = pMarkupDOM.getLocalName();

    CommandFactory lCommandFactory = mCommandNameToFactoryMap.get(lCommandName);
    if(lCommandFactory != null) {
      try {
        return lCommandFactory.create(pMod, pMarkupDOM);
      }
      catch (Throwable th) {
        throw new ExInternal("Error instantiating command \"" + lCommandName + "\"\n(" + Joiner.on(", ").withKeyValueSeparator("=").join(pMarkupDOM.getAttributeMap()) + ")" , th);
      }
    }
    else {
      //Check plugins
      Command lPluginCommand = PluginManager.instance().resolvePluginCommand(lCommandName, pMarkupDOM);

      if(lPluginCommand == null) {
        throw new ExInternal("Serious error: no Factory registered for command \"" + lCommandName + "\"");
      }
      else {
        return lPluginCommand;
      }
    }
  }

  private class CommandStatusProvider
  implements StatusProvider {

    @Override
    public void refreshStatus(StatusDestination pDestination) {


      final Map<String, String> lCommandNamesToFactories = new TreeMap<>();
      for(Map.Entry<String, CommandFactory> lEntry : mCommandNameToFactoryMap.entrySet()) {
        lCommandNamesToFactories.put(lEntry.getKey(), lEntry.getValue().getClass().getName());
      }

      for(FxpCommandFactory lCommandFactory : PluginManager.instance().getAllCommandFactories()) {
        for(String lCommandName : lCommandFactory.getCommandElementNames()) {
          lCommandNamesToFactories.put(lCommandName, lCommandFactory.getClass().getName());
        }
      }

      StatusTable lTable = pDestination.addTable("Available commands", "Command name", "Providing factory");
      lTable.setRowProvider(new StatusTable.RowProvider() {
        @Override
        public void generateRows(StatusTable.RowDestination pRowDestination) {
          for(Map.Entry<String, String> lMapEntry : lCommandNamesToFactories.entrySet()) {
            pRowDestination.addRow()
              .setColumn(lMapEntry.getKey())
              .setColumn(lMapEntry.getValue());
          }
        }
      });
    }

    @Override
    public String getCategoryTitle() {
      return "Command Provider";
    }

    @Override
    public String getCategoryMnemonic() {
      return "foxCommands";
    }

    @Override
    public boolean isCategoryExpandedByDefault() {
      return false;
    }
  }
}

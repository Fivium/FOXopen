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

import java.io.IOException;
import java.io.Writer;

import java.util.Collection;
import java.util.Collections;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.GeneratorDestination;
import net.foxopen.fox.command.util.GeneratorDestinationUtils;
import net.foxopen.fox.command.util.WriterGenerator;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.State;
import net.foxopen.fox.thread.ActionRequestContext;


/**
 * Generates a buffer and sends the output somewhere.
 *
 * @author Philip Simpson
 */
public class GenerateCommand
extends BuiltInCommand {

  private final GeneratorDestination mGeneratorDestination;

  /** The buffer to generate */
  private final String mBuffer;
  private final String mState;

  private GenerateCommand(DOM pMarkupDOM) throws ExDoSyntax {
    super(pMarkupDOM);

    mBuffer = pMarkupDOM.getAttrOrNull("buffer");
    mState = pMarkupDOM.getAttrOrNull("state");

    mGeneratorDestination = GeneratorDestinationUtils.getDestinationFromGenerateCommandMarkup(pMarkupDOM, "html", "text/html");
  }


  @Override
  public XDoControlFlow run(final ActionRequestContext pRequestContext) {

    //Default state = current state
    final State lState;
    if(!XFUtil.isNull(mState)) {
      lState = pRequestContext.getCurrentModule().getState(mState);
    }
    else {
      lState = pRequestContext.getCurrentState();
    }

    mGeneratorDestination.generateToWriter(pRequestContext, new WriterGenerator(){
      @Override
      public void writeOutput(Writer pWriter) throws IOException {
        pWriter.append("HTML Gen TODO: state " + lState.getName() + " buffer " + mBuffer);
      }
    });

    return XDoControlFlowContinue.instance();
  }


  public boolean isCallTransition() {
    return false;
  }

  public static class Factory implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {

      String lOutputType = pMarkupDOM.getAttr("output-type");
      switch (lOutputType) {
        case GenerateLegacySpreadsheetCommand.OUTPUT_TYPE_CSV:
        case GenerateLegacySpreadsheetCommand.OUTPUT_TYPE_XLS:
          return new GenerateLegacySpreadsheetCommand(pMarkupDOM);
        case "SPATIAL-EMF":
          return new GenerateSpatialEMFCommand(pMarkupDOM);
        default:
          return new GenerateCommand(pMarkupDOM);
      }
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("generate");
    }
  }
}

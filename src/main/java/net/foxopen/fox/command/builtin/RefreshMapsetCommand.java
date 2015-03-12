package net.foxopen.fox.command.builtin;

import java.util.Collection;
import java.util.Collections;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

public class RefreshMapsetCommand
extends BuiltInCommand {

  private final String mMapsetName;

  // Parse command returning tokenised representation
  private RefreshMapsetCommand(DOM pParseUElem)
  throws ExInternal, ExDoSyntax {
    super(pParseUElem);
    mMapsetName = pParseUElem.getAttr("name");

    if (XFUtil.isNull(mMapsetName)) {
      throw new ExInternal("Bad syntax: Mapset name not present in refresh-mapset");
    }
  }

  public boolean isCallTransition() {
   return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    pRequestContext.refreshMapSets(mMapsetName);
    //TODO this needs to tell other engines about the mapset refresh
    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new RefreshMapsetCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("refresh-map-set");
    }
  }
}

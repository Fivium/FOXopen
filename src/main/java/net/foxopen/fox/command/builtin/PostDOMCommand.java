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

public class PostDOMCommand
extends BuiltInCommand {

  private final String mDOMLabel;

  private PostDOMCommand(DOM pParseUElem)
  throws ExInternal, ExDoSyntax {
    super(pParseUElem);

    mDOMLabel = pParseUElem.getAttr("context-label");
    if(XFUtil.isNull(mDOMLabel)) {
      throw new ExDoSyntax("fm:post-dom must specify context-label attribute");
    }
  }

  public boolean isCallTransition() {
   return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    pRequestContext.postDOM(mDOMLabel);
    return XDoControlFlowContinue.instance();
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new PostDOMCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("post-dom");
    }
  }
}

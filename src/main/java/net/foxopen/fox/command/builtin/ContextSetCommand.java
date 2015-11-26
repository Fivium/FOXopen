package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;

public class ContextSetCommand
extends BuiltInCommand {

  static final String SCOPE_STATE = "state";
  static final String SCOPE_LOCALISED = "localised";

  private final String mScope;
  private final String mName;
  private final String mXPath;

  /** Constructs the command from the XML element specified */
  private ContextSetCommand(DOM pCmdDOM)
  throws ExDoSyntax {
    super(pCmdDOM);
    mScope = pCmdDOM.getAttr("scope");
    mName = pCmdDOM.getAttr("name");
    mXPath = pCmdDOM.getAttr("xpath");
    if(XFUtil.isNull(mScope) || XFUtil.isNull(mName) || XFUtil.isNull(mXPath)) {
      throw new ExDoSyntax("context-set: missing attrs: scope, name or xpath");
    }
    else if(!mScope.equals(SCOPE_STATE) && !mScope.equals(SCOPE_LOCALISED)) {
      throw new ExDoSyntax("context-set: scope must be 'state' or 'localised'");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    //Cannot set a localised label when ContextUElem is not localised
    if(!lContextUElem.isLocalised() && SCOPE_LOCALISED.equals(mScope)) {
      throw new ExInternal("context-set: scope='localised' (consider state) not permitted when not inside: fm:context-localise; fm:for-each");
    }

    // Get context name - convert when in string(...) format
    String lName;
    try {
      lName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mName);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("context-set: Error getting context name: "+mName, e);
    }
    if(XFUtil.isNull(lName)) {
      throw new ExInternal("context-set: name attr returned null: "+mName);
    }

    if(ContextUElem.ATTACH.equals(lName)) {
      //Special handling for attach point as it requires scroll position to be reset (legacy behaviour)
      pRequestContext.changeAttachPoint(mXPath);
    }
    else {
      //Set a state label
      DOM lDOM;
      try {
        lDOM = lContextUElem.extendedXPath1E(mXPath);
      }
      catch (ExActionFailed | ExCardinality  e) {
        throw new ExInternal("context-set: Error getting context element location: "+ mXPath, e);
      }

      boolean lLocalised = SCOPE_LOCALISED.equals(mScope);

      Track.info("ContextSet", "Setting :{" + lName + "} to node with ID " + lDOM.getFoxId() + " at path " + lDOM.absolute() + (lLocalised ? " (local)" : " (global)"));

      if(lLocalised) {
        lContextUElem.setUElem(lName, ContextualityLevel.LOCALISED, lDOM);
      }
      else {
        lContextUElem.setUElemGlobal(lName, ContextualityLevel.STATE, lDOM);
      }
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
      return new ContextSetCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("context-set");
    }
  }
}

package net.foxopen.fox.command.builtin;

import java.util.Collection;
import java.util.Collections;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.thread.ActionRequestContext;

public class ContextLocaliseCommand
extends BuiltInCommand
{

  private final String mName;
  private final String mXPATH;
  private final String mMapSetName;
  private final String mMapSetItem;
  private final String mMapSetAttach;
  private final String mStorageLocation;
  private final String mOperation;
  private final XDoCommandList mXDo;

  /**
  * Constructs the command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  public ContextLocaliseCommand(Mod pMod, DOM pCmdDOM)
  throws ExInternal, ExDoSyntax {
    super(pCmdDOM);

    mName = XFUtil.nvl(pCmdDOM.getAttr("name"), "local");

    mXPATH = XFUtil.nvl(pCmdDOM.getAttr("xpath"), ".");

    mMapSetName = XFUtil.nvl(pCmdDOM.getAttr("map-set"), "");
    mMapSetItem = XFUtil.nvl(pCmdDOM.getAttr("map-set-item"), "");
    mMapSetAttach = XFUtil.nvl(pCmdDOM.getAttr("map-set-attach"), "");

    mOperation = XFUtil.nvl(pCmdDOM.getAttr("operation"), "");

    mStorageLocation = XFUtil.nvl(pCmdDOM.getAttr("storage-location"), "");

    if ((!mXPATH.equals(".")&&mMapSetName.length()>0)
      || (!mXPATH.equals(".")&&mStorageLocation.length()>0)
      || (mMapSetName.length()>0&&mStorageLocation.length()>0)) {
      throw new ExDoSyntax("context-localise: the attributes xpath, map-set and storage-location can only be used one at a time.");
    }

    try {
      mXDo = XDoCommandList.parseNestedDoOrChildElements(pMod, pCmdDOM);
    }
    catch(ExDoSyntax x) {
      throw new ExDoSyntax("context-localise command has bad nested commands", x);
    }
  }

   public void validate(Mod module)
   throws ExInternal{
     mXDo.validate(module);
   }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    // Create local context
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    // storage-location attribute processing
    if(!XFUtil.isNull(mStorageLocation)) {
      //TODO PN - localise to SL (FOXRD-438)
      throw new ExInternal("context-localise to a storage location not yet supported");
    }
    else {
      // Evaluate xpath location
      DOM lLocateDOM;
      try {
        lLocateDOM = lContextUElem.extendedXPath1E(mXPATH);
      }
      catch (ExCardinality | ExActionFailed e) {
        throw new ExInternal("Cardinality exception when evaluating target XPath for context-localise", e);
      }

      // Assign mapset location
      if(!XFUtil.isNull(mMapSetName)) {

        DOM lMapSetItem;
        if(!XFUtil.isNull(mMapSetItem)) {
          try {
            lMapSetItem = lContextUElem.extendedXPath1E(mMapSetItem);
          }
          catch (ExActionFailed | ExCardinality e) {
            throw new ExInternal("Failed to evaluate mapset item XPath for context-localise", e);
          }
        }
        else {
          lMapSetItem = null;
        }

        MapSet lMapSet = pRequestContext.resolveMapSet(mMapSetName, lMapSetItem, mMapSetAttach);
        lLocateDOM = lMapSet.getMapSetAsDOM();
      }

      XDoRunner lRunner = pRequestContext.createCommandRunner(false);

      //TODO PN - in context-set/clear context name is stringifiable - should be here too for consistency

      // Assign localised context
      lContextUElem.localise("fm:context-localise/" + mName).setUElem(mName, ContextualityLevel.LOCALISED, lLocateDOM);
      try {
        // Execute commands
        return lRunner.runCommands(pRequestContext, mXDo);
      }
      finally {
        lContextUElem.delocalise("fm:context-localise/" + mName);
      }
    }
  }

  public boolean isCallTransition() {
    return false;
  }
  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ContextLocaliseCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("context-localise");
    }
  }
}

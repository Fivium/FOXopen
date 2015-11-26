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
 * Implementation of the fm:show-popup command; triggers browser popup windows with a
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
   * Constructs Show Popup command from the XML element specified.
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
                             " - the command is URI, storage location, document template or query based and, as such, exactly one of " +
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

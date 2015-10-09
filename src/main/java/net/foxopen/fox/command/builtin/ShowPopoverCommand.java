package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.facet.ModalPopoverOptions;
import net.foxopen.fox.module.facet.ModalPopoverProvider;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;

/**
 * Command for showing a modal popover to the user.
 */
public class ShowPopoverCommand
extends BuiltInCommand {

  private final String mBufferNameExpr;
  private final String mBufferAttachExpr;
  private final ModalPopoverOptions.PopoverSize mPopoverSize;
  private final String mTitleExpr;
  private final String mCSSClassExpr;

  private ShowPopoverCommand(DOM pCommandDOM)
  throws ExDoSyntax {
    super(pCommandDOM);
    mBufferNameExpr = pCommandDOM.getAttr("buffer");
    mBufferAttachExpr = pCommandDOM.getAttr("bufferAttach");
    mTitleExpr = pCommandDOM.getAttr("title");
    mCSSClassExpr = pCommandDOM.getAttr("class");

    String lSizeAttr = pCommandDOM.getAttr("size");
    if(!XFUtil.isNull(lSizeAttr)) {
      try {
        mPopoverSize = ModalPopoverOptions.PopoverSize.valueOf(lSizeAttr.toUpperCase());
      }
      catch (IllegalArgumentException e) {
        throw new ExDoSyntax("Invalid value '"  + lSizeAttr +  "' for 'size' attribute specified on fm:show-popover command", e);
      }
    }
    else {
      mPopoverSize = ModalPopoverOptions.PopoverSize.REGULAR;
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    //Evaluate buffer name and attach point

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    String lBufferName;
    try {
      lBufferName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mBufferNameExpr);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate buffer name for show-popover command", e);
    }

    if(XFUtil.isNull(lBufferName)) {
      throw new ExInternal("show-popover buffer expression '" + mBufferNameExpr + "' evaluated to an empty string");
    }

    //Default buffer attach point is current state attach point
    String lBufferAttachFoxId = lContextUElem.attachDOM().getFoxId();
    if(!XFUtil.isNull(mBufferAttachExpr)) {
      try {
        lBufferAttachFoxId = lContextUElem.extendedXPath1E(lContextUElem.attachDOM(), mBufferAttachExpr).getFoxId();
      }
      catch (ExTooFew | ExTooMany | ExActionFailed e) {
        throw new ExInternal("Failed to evaluate buffer attach for show-popover command", e);
      }
    }

    //Display the popover
    pRequestContext.getModuleFacetProvider(ModalPopoverProvider.class).showPopover(lBufferName, lBufferAttachFoxId, getPopoverOptions(lContextUElem));

    return XDoControlFlowContinue.instance();
  }


  /**
   * Constructs a ModalPopoverOptions based on the markup on this command.
   * @param pContextUElem For XPath evalaution.
   * @return New ModalPopoverOptions.
   */
  private ModalPopoverOptions getPopoverOptions(ContextUElem pContextUElem) {
    String lPopoverTitle;
    try {
      lPopoverTitle = pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), mTitleExpr);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate title for show-popover command", e);
    }

    String lCSSClass;
    try {
      lCSSClass = pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), mCSSClassExpr);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate class for show-popover command", e);
    }

    return new ModalPopoverOptions(lPopoverTitle, mPopoverSize, lCSSClass);
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
    implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ShowPopoverCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("show-popover");
    }
  }
}

package net.foxopen.fox.command.builtin;

import java.util.Collection;
import java.util.Collections;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * Simple command that copies elements and overwrites equivalently named elements if they
 * exist in the target parent node.
 */
public class AugmentCommand
extends BuiltInCommand {

  private final String mFromXPath;
  private final String mToXPath;
  private final boolean mValuesOnly;

  /**
   * Constructs an instance of the fm:augment command.
   * @param pMod the parent module
   * @param pParseUElem the DOM command syntax
   * @throws ExInternal internal error occurred
   */
  public AugmentCommand(DOM pParseUElem) {
    super(pParseUElem);
    mFromXPath = pParseUElem.getAttrOrNull("from");
    mToXPath = pParseUElem.getAttrOrNull("to");
    String lValuesOnly = pParseUElem.getAttrOrNull("values-only");
    mValuesOnly = lValuesOnly != null ? Boolean.valueOf(lValuesOnly.toLowerCase()) : false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    // Belt and braces validation
    if (XFUtil.isNull(mFromXPath)) {
      throw new ExInternal("'from' XPath must be specified on the fm:augment command");
    }
    if (XFUtil.isNull(mToXPath)) {
      throw new ExInternal("'to' XPath must be specified on the fm:augment command");
    }

    // Just use the current attach point as the evaluation context
    DOM lAttachPoint = lContextUElem.getUElem(ContextUElem.ATTACH);
    DOMList lSourceElems;
    DOMList lTargetParentElems;

    // Attempt to get the source elements
    try {
      lSourceElems = lContextUElem.extendedXPathUL(lAttachPoint, mFromXPath);
    }
    catch (ExActionFailed ex) {
      throw new ExInternal("Bad XPath in fm:augment attribute 'from': '" + mFromXPath + "'");
    }

    // Attempt to get the target elements
    try {
      lTargetParentElems = lContextUElem.extendedXPathUL(lAttachPoint, mToXPath);
    }
    catch (ExActionFailed ex) {
      throw new ExInternal("Bad XPath in fm:augment attribute 'to': '" + mFromXPath + "'");
    }

    // Check to see if we have any work to do - belt and braces null check
    if (lSourceElems != null && lTargetParentElems != null) {
      // Step through each target parent (if any)
      TARGET_PARENT_LOOP: for (int i = 0; i < lTargetParentElems.getLength(); i++) {
        DOM lTargetParent = lTargetParentElems.item(i);
        // Step through each source element and copy contents to equivalent node
        SOURCE_ELEM_LOOP: for (int j = 0; j < lSourceElems.getLength(); j++) {
          DOM lSourceElem = lSourceElems.item(j);
          if (lSourceElem != null) {
            DOM lTargetElem = lTargetParent.get1EOrNull(lSourceElem.getLocalName());
            // If the element already exists, just overwrite the contents
            // (preserves DOM ordering of outer elements)
            if (lTargetElem != null) {
              lTargetElem.getChildNodes().removeFromDOMTree();
              lSourceElem.copyContentsTo(lTargetElem);
            }
            // Otherwise copy the element over
            else if (!mValuesOnly) {
              lSourceElem.copyToParent(lTargetParent);
            }
          }
        } // SOURCE_ELEM_LOOP
      } // TARGET_PARENT_LOOP
    }

    return XDoControlFlowContinue.instance();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new AugmentCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("augment");
    }
  }
}

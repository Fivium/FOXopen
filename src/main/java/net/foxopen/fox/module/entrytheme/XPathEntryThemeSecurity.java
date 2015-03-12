package net.foxopen.fox.module.entrytheme;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

/**
 * Basic security check which runs an XPath. A "true" result is interpreted as valid.
 */
class XPathEntryThemeSecurity
extends DefinedEntryThemeSecurity {

  private final String mValidateXPath;

  XPathEntryThemeSecurity(XDoCommandList pBeforeValidateCommands, String pValidateXPath) {
    super(pBeforeValidateCommands);
    mValidateXPath = pValidateXPath;
  }

  @Override
  protected boolean runCheck(ActionRequestContext pRequestContext) {
    Track.info("ValidateXPath", mValidateXPath);
    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    try {
      return lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mValidateXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to execute entry theme security validation XPath", e);
    }
  }
}

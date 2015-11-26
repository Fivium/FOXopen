package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.NotificationDisplayType;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.alert.BasicAlertMessage;
import net.foxopen.fox.thread.alert.BufferAlertMessage;
import net.foxopen.fox.thread.alert.RichTextAlertMessage;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;

/**
 * Simple command that sends alerts to the users browser.
 */
public class AlertCommand
extends BuiltInCommand {

  private static final String DEFAULT_CLOSE_PROMPT = "OK";
  private static final String DEFAULT_ALERT_DISPLAY_ATTR_NAME = "defaultAlertDisplay";
  private static final DisplayType DEFAULT_ALERT_DISPLAY_TYPE = DisplayType.POPOVER;

  private enum DisplayType {
    NATIVE, POPOVER;
  }

  private final DisplayType mDisplayType;
  private final NotificationDisplayType mAlertType;

  private final String mMessageXPath;

  private final String mBufferXPath;
  private final String mBufferAttachXPath;

  private final String mTitleXPath;
  private final String mClosePromptXPath;
  private final String mCSSClassXPath;

  private AlertCommand(DOM pCommandElement, Mod pModule)
  throws ExDoSyntax {
    super(pCommandElement);

    mMessageXPath = pCommandElement.getAttr("message");
    mBufferXPath = pCommandElement.getAttr("buffer");

    if(!(XFUtil.isNull(mMessageXPath) ^ XFUtil.isNull(mBufferXPath))) {
      throw new ExDoSyntax("message and buffer attributes are mutually exclusive and exactly one must be specified");
    }

    mBufferAttachXPath = pCommandElement.getAttr("bufferAttach");

    String lDisplayType = pCommandElement.getAttr("display");
    try {
      if (XFUtil.isNull(lDisplayType)) {
        lDisplayType = XFUtil.nvl(pModule.getModuleAttributes().get(DEFAULT_ALERT_DISPLAY_ATTR_NAME), DEFAULT_ALERT_DISPLAY_TYPE.toString()).toUpperCase();
      }
      mDisplayType = DisplayType.valueOf(lDisplayType.toUpperCase());
    }
    catch (IllegalArgumentException e) {
      throw new ExDoSyntax("Not a valid display attribute for fm:alert: '" + lDisplayType + "'", e);
    }

    String lAlertType = pCommandElement.getAttr("alertType");
    mAlertType = XFUtil.isNull(lAlertType) || "normal".equals(lAlertType.toLowerCase()) ? null : NotificationDisplayType.fromExternalString(lAlertType);

    mTitleXPath = pCommandElement.getAttr("title");
    mClosePromptXPath = pCommandElement.getAttr("closePrompt");
    mCSSClassXPath = pCommandElement.getAttr("class");
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    if (XFUtil.exists(mBufferXPath)) {
      displayBufferAlert(pRequestContext);
    }
    else {
      if (mDisplayType == DisplayType.NATIVE) {
        displayBasicAlert(pRequestContext);
      }
      else {
        displayRichTextAlert(pRequestContext);
      }
    }

    return XDoControlFlowContinue.instance();
  }

  private void displayBasicAlert(ActionRequestContext pRequestContext) {

    String lMessage = getMessageXPathResult(pRequestContext.getContextUElem()).asString();

    if (!XFUtil.isNull(lMessage)) {
      pRequestContext.addXDoResult(new BasicAlertMessage(lMessage));
    }
    else {
      Track.info("Skipping alert registration as string result was empty");
    }
  }

  private void displayRichTextAlert(ActionRequestContext pRequestContext) {
    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    XPathResult lMessage = getMessageXPathResult(lContextUElem);

    if (!XFUtil.isNull(lMessage.asString())) {
      RichTextAlertMessage lAlert = new RichTextAlertMessage(lMessage.asString(), getTitle(lContextUElem), mAlertType,
                                                             getClosePrompt(lContextUElem), getCSSClass(lContextUElem), lMessage.isEscapingRequired());
      pRequestContext.addXDoResult(lAlert);
    }
    else {
      Track.info("Skipping alert registration as string result was empty");
    }
  }

  private void displayBufferAlert(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    String lBufferName;
    try {
      lBufferName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mBufferXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Bad buffer XPath in alert command", e);
    }

    if(XFUtil.isNull(lBufferName)) {
      throw new ExInternal("Alert buffer name cannot be null");
    }

    DOM lAttachDOM = lContextUElem.attachDOM();
    if(XFUtil.exists(mBufferAttachXPath)) {
      try {
        lAttachDOM = lContextUElem.extendedXPath1E(lContextUElem.attachDOM(), mBufferAttachXPath);
      }
      catch (ExTooMany | ExTooFew | ExActionFailed e) {
        throw new ExInternal("Failed to evaluate bufferAttach XPath in alert command", e);
      }
    }

    BufferAlertMessage lMessage = new BufferAlertMessage(lBufferName, lAttachDOM.getFoxId(), getTitle(lContextUElem),
                                                         mAlertType, getClosePrompt(lContextUElem), getCSSClass(lContextUElem));
    pRequestContext.addXDoResult(lMessage);

  }

  private String getTitle(ContextUElem pContextUElem) {
    try {
      return pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), mTitleXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluated alert title XPath", e);
    }
  }

  private String getClosePrompt(ContextUElem pContextUElem) {
    try {
      return XFUtil.nvl(pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), mClosePromptXPath), DEFAULT_CLOSE_PROMPT);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluated alert closePrompt XPath", e);
    }
  }

  private String getCSSClass(ContextUElem pContextUElem) {
    try {
      return pContextUElem.extendedStringOrXPathString(pContextUElem.attachDOM(), mCSSClassXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluated alert class XPath", e);
    }
  }

  private XPathResult getMessageXPathResult(ContextUElem pContextUElem) {
    XPathResult lMessage;
    try {
      lMessage = pContextUElem.extendedConstantOrXPathResult(pContextUElem.attachDOM(), mMessageXPath);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Bad XPath in alert command '" + mMessageXPath + "'", e);
    }
    return lMessage;
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new AlertCommand(pMarkupDOM, pModule);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("alert");
    }
  }
}

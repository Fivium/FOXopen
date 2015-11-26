package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.StringUtil;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;


public class SecurityScopeCommand
  extends BuiltInCommand {

  /**
   * The default system-level privileges to apply for the current user; defaults to all.
   */
  private final String mCSVSystemPrivileges;

  /**
   * An XPath that identifies one or more Universal References to objects whose privileges are to be determined.
   */
  private final String mURefXPath;

  /**
   * The restriction subset, if any, of object-level privileges to determine for the specified object URefs for the current user. Can be an XPATH
   */
  private final String mCSVObjectPrivileges;

  /**
   * The restriction subset, if any, of object Universal Reference types to determine for the specified object URefs for the current user.
   */
  private final String mCSVURefTypes;

  /**
   * Constructs a Security Scope command from the XML element specified.
   *
   * @param commandElement the element from which the command will be constructed.
   */
  private SecurityScopeCommand(DOM commandElement) throws ExInternal {
    super(commandElement);

    mCSVSystemPrivileges = commandElement.getAttrOrNull("csv-system-privs");

    mURefXPath = commandElement.getAttrOrNull("uref-xpath");

    mCSVObjectPrivileges = commandElement.getAttrOrNull("csv-object-privs");

    mCSVURefTypes = commandElement.getAttrOrNull("csv-uref-types");

  }

  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem contextUElem = pRequestContext.getContextUElem();
    String lCSVURefList = "";
    String lCSVObjectPrivileges = "";
    String lCSVSystemPrivileges = "*";
    String lCSVURefTypes = "";

    if (mURefXPath != null) {
      XPathResult xpResult;
      try {
        xpResult = contextUElem.extendedXPathResult(contextUElem.getUElem(ContextLabel.ATTACH), mURefXPath);
        lCSVURefList = StringUtil.collectionToDelimitedString(xpResult.asStringList(), ",");
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate UREF XPath for security-scope command", e);
      }
    }

    if (mCSVObjectPrivileges!= null) {
      try {
        lCSVObjectPrivileges = contextUElem.extendedStringOrXPathString(contextUElem.attachDOM(), mCSVObjectPrivileges);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate Object Privileges XPath for security-scope command", e);
      }
    }


    if (mCSVSystemPrivileges!=null) {
      try {
        lCSVSystemPrivileges = contextUElem.extendedStringOrXPathString(contextUElem.attachDOM(), mCSVSystemPrivileges);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate System Privileges XPath for security-scope command", e);
      }
    }

    if(mCSVURefTypes!=null) {
      try {
        lCSVURefTypes = contextUElem.extendedStringOrXPathString(contextUElem.attachDOM(), mCSVURefTypes);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate URef Types XPath for security-scope command", e);
      }
    }

    SecurityScope lSecurityScope = new SecurityScope(lCSVSystemPrivileges, mURefXPath, lCSVURefList, lCSVObjectPrivileges, lCSVURefTypes);
    pRequestContext.changeSecurityScope(lSecurityScope);

    return XDoControlFlowContinue.instance();
  }

  public static class Factory
    implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new SecurityScopeCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("security-scope");
    }
  }
}

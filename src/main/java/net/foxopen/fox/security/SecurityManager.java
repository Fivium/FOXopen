package net.foxopen.fox.security;

import com.google.common.collect.Table;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExSecurity;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.PresentationAttribute;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.xhtml.NameValuePair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Manages the mode/view namespace rules in FOX and other security related module code.
 */
public class SecurityManager
{
   /** The context for an existing mode list. **/
   public static final String MODE_LIST_PROP = "mode-list-ctx";
   /** The context for an existing view list. **/
   public static final String VIEW_LIST_PROP = "view-list-ctx";

   /** The singleton instance. */
   private static final SecurityManager INSTANCE = new SecurityManager();

   /**
    * Constructs the singleton instance.
    */
   private SecurityManager() {
   }

  /**
   * Returns the singleton instance configured security manager.
   */
  public static SecurityManager getInstance() {
    return INSTANCE;
  }

  /**
   * Determines the active modes from the mode rule security table defined in the
   * current module.
   *
   * @param pRequestContext the parameters needed for the security table scan.
   * @return SecurityOperationDescriptor a descriptor containing the results of
   *         the security table scan.
   */
  public SecurityOperationDescriptor determineModeListFromModule(ActionRequestContext pRequestContext)
    throws ExSecurity
  {
    return evaluateModeOrViewsListAgainstSecurityTable(MODE_LIST_PROP, pRequestContext);
  }

  /**
   * Determines the active views from the mode rule security table defined in the
   * current module.
   *
   * @param pRequestContext the parameters needed for the security table scan.
   * @return SecurityOperationDescriptor a descriptor containing the results of
   *         the security table scan.
   */
  public SecurityOperationDescriptor determineViewListFromModule(ActionRequestContext pRequestContext)
    throws ExSecurity
  {
    return evaluateModeOrViewsListAgainstSecurityTable(VIEW_LIST_PROP, pRequestContext);
  }

  /**
   * Determines the active modes/views from the mode/view rule security table
   * defined in the current module.
   *
   * @param modeOrViewsProperty a flag to indicate whether the mode or view security
   *        table is to be scanned.
   * @param pRequestContext the parameters needed for the security table scan.
   * @return SecurityOperationDescriptor a descriptor containing the results of
   *         the security table scan.
   * @see    #determineModeListFromModule
   * @see    #determineViewListFromModule
   */
  private SecurityOperationDescriptor evaluateModeOrViewsListAgainstSecurityTable(String modeOrViewsProperty,
                                                                                  ActionRequestContext pRequestContext)
    throws ExSecurity
  {
    SecurityTable secTable = (modeOrViewsProperty == MODE_LIST_PROP ? pRequestContext.getCurrentModule().getModeRulesTable() : pRequestContext.getCurrentModule().getViewRulesTable());
    SecurityOperationDescriptor opDescriptor = secTable.evaluateRules(pRequestContext);

    return opDescriptor;
  }

  /**
   * Inspects a fox 'visual' processing command, such as set-out, and determines
   * the modes and views specified on the command.
   *
   * @param modeXPathList returns the modes on the command as <code>NameValuePair</code>
   *        objects that each contain the mode name and corresponding XPath
   *        expression.
   * @param viewXPathList returns the views on the command as <code>NameValuePair</code>
   *        objects that each contain the view name and corresponding XPath
   *        expression.
   * @param pCommand the command to inspect
   */
  @Deprecated
  public void getPotentialModesAndViewsFromCommand(List modeXPathList,
                                                   List viewXPathList,
                                                   DOM pCommand)
  {
     List attrs = pCommand.getAttrNames(); // relying on this returning the attributes in the correct order
     for (int n=0; n < attrs.size(); n++)
     {
        String attr = (String)attrs.get(n);
        int nss = attr.indexOf(':');
        if (nss!=-1)
        {
           String namespace = attr.substring(0,nss);
           String function = attr.substring(nss+1);
           String value = pCommand.getAttr(attr);
           if(value.equals("."))
           {
              value = "";
           }
           if (function.equals("mode"))
           {
              modeXPathList.add(new NameValuePair(namespace,value));
           }
           else if (function.equals("view"))
           {
              viewXPathList.add(new NameValuePair(namespace,value));
           }
        }
     }
  }

  /**
   * Inspects a fox 'visual' processing command, such as set-out, and determines
   * the modes and views specified on the command.
   *
   * @param modeXPathList returns the modes on the command as <code>NameValuePair</code>
   *        objects that each contain the mode name and corresponding XPath
   *        expression.
   * @param viewXPathList returns the views on the command as <code>NameValuePair</code>
   *        objects that each contain the view name and corresponding XPath
   *        expression.
   * @param pEvaluatedPresentationNode the EvaluatedPresentationNode to get mode/view attributes from
   */
  public void getPotentialModesAndViewsFromEvaluatedPresentationNode( List<NameValuePair> modeXPathList,
                                                                      List<NameValuePair> viewXPathList,
                                                                      GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode)
  {
    for (Table.Cell<String, String, PresentationAttribute> lAttribute : pEvaluatedPresentationNode.getNamespaceAttributes().cellSet()) {
      String lNamespace = lAttribute.getRowKey();
      String lAttributeName = lAttribute.getColumnKey();
      String lAttributeValue = lAttribute.getValue().getValue();

      if(".".equals(lAttributeValue)) {
        lAttributeValue = "";
      }

      if ("mode".equals(lAttributeName)) {
        modeXPathList.add(new NameValuePair(lNamespace, lAttributeValue));
      }
      else if ("view".equals(lAttributeName)) {
        viewXPathList.add(new NameValuePair(lNamespace, lAttributeValue));
      }
    }
  }

  /**
   * Determines the applicable modes and views from the specified mode and
   * view XPath expressions and from the module security table entries.
   *
   * <p>This is the main entry point for security within Fox.
   *
   * @param modeTableOpsDescriptor the evaluated modes Security Table that provides, for
   *        each mode, the set of applicable operations.
   * @param viewTableOpsDescriptor the evaluated views Security Table that provides, for
   *        each mode, the set of applicable operations.
   * @param modeList a list of the applicable modes, as determined by the
   *        security subsystem.
   * @param viewList a list of the applicable views, as determined by the
   *        security subsystem.
   * @param commandModeXPathList a list of the eligible modes and their XPath
   *        expressions, observed from some Fox component running the security
   *        check.
   * @param commandViewXPathList a list of the eligible view and their XPath
   *        expressions, observed from some Fox component running the security
   *        check.
   * @param pModule the current module.
   * @param contextUElem the data contexts.
   * @param displayItem the item that pertains to this check.
   */
  public void getApplicableModeViewLists(SecurityOperationDescriptor modeTableOpsDescriptor,
                                         SecurityOperationDescriptor viewTableOpsDescriptor,
                                         List<String> modeList,
                                         List<String> viewList,
                                         List<NameValuePair> commandModeXPathList,
                                         List<NameValuePair> commandViewXPathList,
                                         Mod pModule,
                                         ContextUElem contextUElem,
                                         DOM displayItem)
    throws ExSecurity
  {
    //------------------------------------------------------------------------
    // Evaluate command mode and view XPaths.
    //------------------------------------------------------------------------
    Set<String> commandEnabledModesSet = evaluateCommandNamespaceXPaths(pModule, contextUElem, displayItem, commandModeXPathList);
    Set<String> commandEnabledViewSet  = evaluateCommandNamespaceXPaths(pModule, contextUElem, displayItem, commandViewXPathList);
    Set<String> commandEnabledNamespaces = new HashSet<>(commandEnabledModesSet);
    commandEnabledNamespaces.addAll(commandEnabledViewSet);

    //------------------------------------------------------------------------
    // Determine applicable modes and views on command and from teh table.
    //------------------------------------------------------------------------
    if (commandEnabledModesSet.isEmpty() && commandEnabledViewSet.isEmpty()) { // if command has no modes or views - don't return any
      return;
    }

    for (String lNamespace : commandEnabledNamespaces) {
      Set modeTableResultSet = modeTableOpsDescriptor.getNamespaceOperationResultsSet(lNamespace);
      Set viewTableResultSet = viewTableOpsDescriptor.getNamespaceOperationResultsSet(lNamespace);

      if (!modeTableResultSet.isEmpty() || !viewTableResultSet.isEmpty()) {
        // if security table has an entry for the namespace
        if (commandEnabledModesSet.contains(lNamespace)) {
          // if command has mode enabled
          if (modeTableResultSet.contains(SecurityTableEntry.OP_ENABLE) && !modeTableResultSet.contains(SecurityTableEntry.OP_DISABLE)) {
            modeList.add(lNamespace);
          }
          else if (viewTableResultSet.contains(SecurityTableEntry.OP_ENABLE) && !viewTableResultSet.contains(SecurityTableEntry.OP_DISABLE)) {
            viewList.add(lNamespace);
          }
        }
        else if (commandEnabledViewSet.contains(lNamespace)) {
          // if command has view enabled
          if (modeTableResultSet.contains(SecurityTableEntry.OP_ENABLE) && !modeTableResultSet.contains(SecurityTableEntry.OP_DISABLE)) {
            viewList.add(lNamespace);
          }
          else if (viewTableResultSet.contains(SecurityTableEntry.OP_ENABLE) && !viewTableResultSet.contains(SecurityTableEntry.OP_DISABLE)) {
            viewList.add(lNamespace);
          }
        }
      }
      else if (commandEnabledModesSet.contains(lNamespace)) {
        // if command has mode enabled
        modeList.add(lNamespace);
      }
      else if (commandEnabledViewSet.contains(lNamespace)) {
        // if command has view enabled
        viewList.add(lNamespace);
      }
    }
  }

  private Set<String> evaluateCommandNamespaceXPaths(Mod pModule,
                                                     ContextUElem contextUElem,
                                                     DOM domContext,
                                                     List<NameValuePair> namespaceXPathNVPs)
    throws ExSecurity
  {
    try {
      Set<String> applicableNamespaces = new HashSet<>();
      for (NameValuePair lNVP : namespaceXPathNVPs) {
        String namespace    = lNVP.getName();
        String xpathExpr    = (String)lNVP.getValue();

        if (applicableNamespaces.contains(namespace)) {
          throw new ExInternal("Error in fox module, " + pModule.getName() +
                               " - a set-out or similar action contains more than one XPath expression for mode/view namespace, " + namespace +
                               ". This is not allowed so please combine the XPath expressions for all instances using boolean logic!");
        }

        // Check against 0 as getPotentialModesAndViewsFromEvaluatedPresentationNode() puts a value of empty string in if it was "." originally
        boolean xpathResult = (xpathExpr.length() == 0 || contextUElem.extendedXPathBoolean(domContext, xpathExpr));

        if (xpathResult) {
          applicableNamespaces.add(namespace);
        }
      }

      return applicableNamespaces;
    }
    catch (ExActionFailed ex) {
      throw new ExSecurity("Unexpected error encountered during evaluation of visual command XPaths - see nested exception for further information.", ex);
    }
  }
}

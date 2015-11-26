package net.foxopen.fox.security;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.StringUtil;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A model of a security table mode/view entry.
 * @see SecurityTable
 */
public class SecurityTableEntry implements Validatable
{
   /** Enumeration of all of the security operations, as defined by 'operations' type in the fox_module schema. */
   public static final String OP_NONE                       = "NONE";
   public static final String OP_ENABLE                     = "ENABLE";
   public static final String OP_DISABLE                    = "DISABLE";
   public static final String OP_DEFAULT_ENABLED            = "DEFAULT-ENABLED";
   public static final String OP_DEFAULT_DISABLED           = "DEFAULT-DISABLED";
   public static final String OP_COMMAND_ENABLE_ALLOWED     = "COMMAND-ENABLE-ALLOWED";
   public static final String OP_COMMAND_ENABLE_DISALLOWED  = "COMMAND-ENABLE-DISALLOWED";
   public static final String OP_COMMAND_DISABLE_ALLOWED    = "COMMAND-DISABLE-ALLOWED";
   public static final String OP_COMMAND_DISABLE_DISALLOWED = "COMMAND-DISABLE-DISALLOWED";

   private DOM     tableXMLElement;
   private Set     namespaces = new HashSet();
   private String  operation;
   private Set     privileges = new HashSet();
   private Set     themes     = new HashSet();
   private Set     states     = new HashSet();
   private String  xpathExpr;
   private String  datumRecPath;
   private String  datumType;
   private String  datumId;
   private String  datumScope;
   private boolean isBuildOnly;

   /**
    * Constructs a SecurityTableEntry with no initial state.
    */
   public SecurityTableEntry()
   {
   }

   /**
    * Constructs a SecurityTableEntry from the specified entry XMl element
    *
    * @param entryElement
    */
   public SecurityTableEntry(DOM entryElement, Map<String, Set<String>> pNamespaceGroups)
   {
      setNamespaces(
        entryElement.getAttr("namespace")
      , getSpecifiedAttributeOrNull(entryElement, "namespace-groups")
      , pNamespaceGroups
      );

      setOperation(entryElement.getAttr("operation"));
      setPrivileges(XFUtil.nvl(getSpecifiedAttributeOrNull(entryElement, "privilege"), ""));
      setThemes(XFUtil.nvl(getSpecifiedAttributeOrNull(entryElement, "theme"), ""));
      setStates(XFUtil.nvl(getSpecifiedAttributeOrNull(entryElement, "state"), ""));
      setXPathExpr(getSpecifiedAttributeOrNull(entryElement, "xpath"));
      setDatumRecordPath(getSpecifiedAttributeOrNull(entryElement, "datum-rec-path"));
      setDatumType(getSpecifiedAttributeOrNull(entryElement, "datum-type"));
      setDatumId(getSpecifiedAttributeOrNull(entryElement, "datum-id"));
      setDatumScope(getSpecifiedAttributeOrNull(entryElement, "datum-scope"));
   }

   /**
    * Gets the value of the specified attribute or null.
    *
    * @param element the XML element containing the attribute.
    * @param attribute the name of the attribute whose value is to be determined.
    * @return the value of the named attribute, or null if the attribute has not
    *         been specified.
    */
   private String getSpecifiedAttributeOrNull(DOM element, String attribute)
   {
      return (element.hasAttr(attribute) && element.getAttr(attribute).trim().length() > 0 ?
              element.getAttr(attribute).trim() : null);
   }

   public Set getNamespaces()
   {
      return namespaces;
   }

   public void setNamespaces(String pNamespacesCSV, String pNamespaceGroupsCSV, Map<String, Set<String>> pNamespaceGroups)
   {
     //Add all namespaces from element definition to the namespace set
     namespaces.clear();
     namespaces.addAll(StringUtil.commaDelimitedListToSet(pNamespacesCSV));
     //Add all namespaces in each referenced namespace group to the namespace set (expanding the group for this security entry)
     if (XFUtil.exists(pNamespaceGroupsCSV)) {
       StringUtil.commaDelimitedListToSet(pNamespaceGroupsCSV)
         .stream()
         .forEach(s -> {
           if (!pNamespaceGroups.containsKey(s)) {
             throw new ExInternal("Referenced namespace-group '" + s + "' is not defined");
           }
           namespaces.addAll(pNamespaceGroups.get(s));
         });
     }
   }

   public String getOperation()
   {
      return operation;
   }

   public void setOperation(String newValue)
   {
      operation     = newValue;
   }

   public Set getPrivileges()
   {
      return privileges;
   }
   public void setPrivileges(String newValue)
   {
      privileges.clear();
      privileges.addAll(StringUtil.commaDelimitedListToSet(newValue));
   }

   public Set getThemes()
   {
      return themes;
   }
   public void setThemes(String newValue)
   {
      themes.clear();
      themes.addAll(StringUtil.commaDelimitedListToSet(newValue));
   }

   public Set getStates()
   {
      return states;
   }
   public void setStates(String newValue)
   {
      states.clear();
      states.addAll(StringUtil.commaDelimitedListToSet(newValue));
   }

   public String getXPathExpression()
   {
      return xpathExpr;
   }
   public void setXPathExpr(String newValue)
   {
      xpathExpr = newValue;
   }

   public String getDatumRecordPath()
   {
      return datumRecPath;
   }
   public void setDatumRecordPath(String newValue)
   {
      datumRecPath = newValue;
   }

   public String getDatumType()
   {
      return datumType;
   }
   public void setDatumType(String newValue)
   {
      datumType = newValue;
   }

   public String getDatumId()
   {
      return datumId;
   }
   public void setDatumId(String newValue)
   {
      datumId = newValue;
   }

   public String getDatumScope()
   {
      return datumScope;
   }
   public void setDatumScope(String newValue)
   {
      datumScope = newValue;
   }

   public boolean getIsBuildOnly()
   {
      return isBuildOnly;
   }
   public void setIsBuildOnly(boolean newValue)
   {
      isBuildOnly = newValue;
   }

   /**
   * Validates the entry.
   *
   * @param module the module where the component resides
   * @throws ExInternal if the component syntax is invalid.
   */
   public void validate(Mod module) throws ExInternal
   {
      if (tableXMLElement.getAttr("namespace") == null && tableXMLElement.getAttr("namespace-groups") == null)
      {
         throw new ExInternal("Error parsing mode or view security-list table in module, "+module.getName()+
                              " - there are one or more entries with no \"namespace\" attribute or \"namespace-groups\" definition!");
      }

      if (tableXMLElement.getAttr("operation") == null)
      {
         throw new ExInternal("Error parsing mode or view security-list table in module, "+module.getName()+
                              " - there are one or more entries with no \"operation\" attribute definition!");
      }
   }

   /**
   * Evaluates the entry with the specified context.
   *
   * @param contextProperties the context properties with which to evaluate this
   *        security table entry.
   * @return the operation that will result, if any, from this entry. Refer to the
   * list of operation codes defined by this class - with <i>OP_NONE</i> returned for an
   * entry that has no effect (does not either enable or disable the namespace).
   */
   public String evaluate(ActionRequestContext pRequestContext) throws ExInternal
   {
      try
      {
         boolean themeTest = (themes.size()     == 0 ? true : themes.contains(pRequestContext.getCurrentTheme().getName()));
         boolean stateTest = (states.size()     == 0 ? true :  states.contains(pRequestContext.getCurrentState().getName()));;
         boolean privTest  = privileges.size()  == 0;
         boolean xpathTest = xpathExpr == null;

         if (themeTest && stateTest) // No need to check privileges if either theme or state tests failed
         {
            if (privileges.size() > 0) // We have privileges in this entry?
            {
               // Intersect the privileges of this entry with those of the user.
               Set userPrivileges_n_ThisEntryPrivileges = new HashSet(privileges);
               //TODO PN HTML - needs testing
               userPrivileges_n_ThisEntryPrivileges.retainAll(pRequestContext.getAuthenticationContext().getAuthenticatedUser().getPrivileges());
               privTest  = userPrivileges_n_ThisEntryPrivileges.size() > 0;
            }

             // No need to check privileges if theme, state or privilege tests failed or we haven't any xpath to test in this entry
            if (privTest && xpathExpr != null)
            {
               ContextUElem contextUElem = pRequestContext.getContextUElem();
               //TODO PN HTML - attach DOM was always used before but check regression just to be safe
               DOM          itemContext  = contextUElem.attachDOM();
               xpathTest = contextUElem.extendedXPathBoolean(itemContext, xpathExpr);
            }
         }

         return (privTest && themeTest && stateTest && xpathTest ? operation : OP_NONE);
      }
      catch (ExActionFailed ex)
      {
         throw new ExInternal("Error running security table entry rule - see nested exception for further information.", ex);
      }
   }
}

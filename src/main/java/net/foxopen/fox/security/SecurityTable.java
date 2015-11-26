package net.foxopen.fox.security;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExSecurity;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A model of the modes/views list security table in the Fox module. There is one
 * of these tables for the modes/views security policies in each Fox Module.
 *
 * <p>Note that this class is reentrant in that it only stores its configuration
 * from the Fox module and no state regarding any specified client is stored
 * and no changes to its state occurs as a result of client invocations.
 */
public class SecurityTable implements Validatable
{
   /** The  security table entries. */
   List entries = new ArrayList();

   /** The caches set of all table namespace entries. */
   HashSet cachedNamespaceEntriesSet;
   /** The caches list of all table namespace entries. */
   ArrayList cachedNamespaceEntriesList;

   /**
    * Constructs a SecurityTable with no initial entries.
    */
   public SecurityTable()
   {
   }

   /**
    * Sets the entries in the table from the corresponding XML elements.
    *
    * @param pTableEntries the XML security entries.
    * @param pNamespaceGroups the module's map of namespace groups for lookup if necessary.
    */
   public void setEntries(DOMList pTableEntries, Map<String, Set<String>> pNamespaceGroups)
   {
      entries.clear();
      for (int n=0; n < pTableEntries.getLength(); n++)
      {
         DOM entry = pTableEntries.item(n);

         SecurityTableEntry tableEntry = new SecurityTableEntry(entry, pNamespaceGroups);
         entries.add(tableEntry);
      }
   }
   /**
    * Returns the number of entries in the table.
    *
    * @return the size of the table
    */
   public int size()
   {
      return entries.size();
   }
   /**
    * Returns an entry from the table.
    *
    * @return the entry.
    */
   public SecurityTableEntry getEntry(int index)
   {
      return (SecurityTableEntry)entries.get(index);
   }

   /**
   * Validates that all table entries are valid.
   *
   * @param module the module where the component resides
   * @exception ExInternal if the component syntax is invalid.
   */
   public void validate(Mod module) throws ExInternal
   {
      for (int n=0; n < entries.size(); n++)
      {
         SecurityTableEntry entry = (SecurityTableEntry)entries.get(n);
         entry.validate(module);
      }
   }

   /**
   * Runs through the table to determine the security operation permissions on the
   * specified mode or view item - whether the item should be enabled or disabled.
   *
   * @return the set of operations that will result, if any, from the table scan. Refer to the
   * @exception ExSecurity if the security operation fails.
   * @see SecurityTableEntry
   */
   public SecurityOperationDescriptor evaluateRules(ActionRequestContext pRequestContext)
      throws ExSecurity
   {
      SecurityOperationDescriptor opDescriptor = new SecurityOperationDescriptor();

      //------------------------------------------------------------------------
      // Perform a one-pass scan of the table, running each rule and compiling
      // a set, for each namespace, of the valid operations for that namespace.
      //------------------------------------------------------------------------
      for (int n=0; n < entries.size(); n++)
      {
         SecurityTableEntry entry = getEntry(n);
         String operationResult = entry.evaluate(pRequestContext);
         opDescriptor.addOperationResult(entry.getNamespaces(), operationResult);
      }

      return opDescriptor;
   }

   /**
    * Determines the set of all namespace names that appear in rules in the
    * security table. That is, a set of all namespaces where each namespace
    * appears one or more times in the security table.
    *
    * @return a set of namespace names
    */
   public Set getNamespaceEntriesSet()
   {
      if (cachedNamespaceEntriesSet == null)
      {
         cachedNamespaceEntriesSet = new HashSet();
         for (int n=0; n < entries.size(); n++)
         {
            cachedNamespaceEntriesSet.addAll(((SecurityTableEntry)entries.get(n)).getNamespaces());
         }
      }

      return cachedNamespaceEntriesSet;
   }
}

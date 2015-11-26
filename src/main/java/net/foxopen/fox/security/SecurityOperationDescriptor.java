package net.foxopen.fox.security;

import java.util.*;

/**
 * Used to describe the outcome of a security operation.
 *
 * <p>The operation may have been to determine the view or modes that
 * apply as a result of the security operation.
 */
public class SecurityOperationDescriptor
{
   /** A mapping of namespace to the security operation results for the namespace. */
   private Map namespaceToResultsSet = new HashMap();

   /**
    * Constructs an empty <code>SecurityOperationDescriptor</code>.
    */
   public SecurityOperationDescriptor()
   {
   }

   /**
    * Adds the specified result to the results set for the specified namespaces.
    *
    * @param namespaces the namespaces to which this result pertains.
    * @param resultCode the result of the security operation for the namespace.
    */
   public void addOperationResult(Set namespaces, String resultCode)
   {
      for (Iterator iter=namespaces.iterator(); iter.hasNext(); )
      {
         addOperationResult((String)iter.next(), resultCode);
      }
   }

   /**
    * Adds the specified result to the results set for the specified namespace.
    *
    * @param namespace the namespace to which this result pertains.
    * @param resultCode the result of the security operation for the namespace.
    */
   public void addOperationResult(String namespace, String resultCode)
   {
      Set resultsSet = (Set)namespaceToResultsSet.get(namespace);
      if (resultsSet == null)
      {
         resultsSet = new HashSet();
         namespaceToResultsSet.put(namespace, resultsSet);
      }
      resultsSet.add(resultCode);
   }

   /**
    * Returns a set of all the namespaces hels in this descriptor.
    *
    * @return a set of all namespaces described in this descriptor.
    */
   public Set getNamespaceEntriesSet()
   {
      return namespaceToResultsSet.keySet();
   }

   /**
    * Returns a set of results for the specified namespace.
    *
    * @param namespace the namespace for which the results are to be obtained.
    * @return a set of the results of the security operation for the namespace.
    */
   public Set getNamespaceOperationResultsSet(String namespace)
   {
      Set resultsSet = (Set)namespaceToResultsSet.get(namespace);
      if (resultsSet == null)
      {
         resultsSet = new HashSet();
      }
      return resultsSet;
   }
}

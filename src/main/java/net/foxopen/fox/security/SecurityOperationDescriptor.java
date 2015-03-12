/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE - 
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, 
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
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

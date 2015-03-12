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
package net.foxopen.fox.ex;

import net.foxopen.fox.XFUtil;

/**
* Handy class for wrapping <code>RuntimeException</code>s so to allow a method to throw
* just the wrapper and not have to throw a whole bunch of Exceptions.
*
* <p>A root cause exception can be supplied to this wrapper
* exception so that the original cause of the exception
* can be discovered.
*
* @author Gary Watson
*/
public class RuntimeExceptionWrapperException extends RuntimeException
{
   /**
    * Constructs a <code>ExceptionWrapperException</code> with no specified
    * detail message or wrapped exception.
    */
   public RuntimeExceptionWrapperException()
   {}

   /**
    * Constructs a <code>RemoteException</code> with the specified
    * detail message.
    *
    * @param s the detail message
    */
   public RuntimeExceptionWrapperException(String s)
   {
      super(s);
   }


   /**
    * Constructs a <code>RemoteException</code> with the specified
    * detail message and nested exception.
    *
    * @param s the detail message
    * @param ex the nested exception
    */
   public RuntimeExceptionWrapperException(String s, Throwable ex)
   {
      super(s, ex);
   }

    /**
     * Returns the detail message, including the message from the nested
     * exception if there is one.
     */
   public String getMessage()
   {
      return super.getMessage()+(getCause() != null ? (" See nested exception: \n\t"+ XFUtil.nvl(getCause().getMessage(), "[no message available]")) : "");
   }
}

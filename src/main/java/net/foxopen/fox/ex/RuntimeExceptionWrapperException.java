package net.foxopen.fox.ex;

import net.foxopen.fox.XFUtil;

/**
* Handy class for wrapping <code>RuntimeException</code>s so to allow a method to throw
* just the wrapper and not have to throw a whole bunch of Exceptions.
*
* <p>A root cause exception can be supplied to this wrapper
* exception so that the original cause of the exception
* can be discovered.
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

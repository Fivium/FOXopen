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

import net.foxopen.fox.App;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.entrytheme.EntryTheme;


/**
 * A top-level exception that wraps other exceptions caught at the upper levels of
 * an main entry call to Fox. This exception can be used to record properties
 * concerning the error or exception that it wraps.
 *
 * <p>If this exception is thrown, we are unable to recover from some unexpected
 * system error and the client will be sent to a 'System Busy or Encountered an
 * unexpected error' page.
 *
 * @author Gary Watson
 */
public class ExSystemError extends RuntimeException
{
  private transient ErrorContext mErrorContext = new ErrorContext();

  public ExSystemError() {
    mErrorContext.setErrorID(Long.toString(System.currentTimeMillis()));
  }

  public String getErrorId() {
    return Long.toString(System.currentTimeMillis());
  }

  public final ErrorContext getErrorProperties() {
    return mErrorContext;
  }

  public final void setMessage(String pMessage) {
    mErrorContext.setMessage(pMessage);
  }

  public final void setException(Throwable pException) {
    mErrorContext.setException(pException);
  }

  public final void setState(State pState) {
    mErrorContext.setState(pState);
  }

  public final void setTheme(EntryTheme pTheme) {
    mErrorContext.setTheme(pTheme);
  }

  public final void setModule(Mod pModule) {
    mErrorContext.setModule(pModule);
  }

  public final void setApp(App pApp) {
    mErrorContext.setApp(pApp);
  }

  public final void setErrorID(String pErrorID) {
    mErrorContext.setErrorID(pErrorID);
  }

  public String getMessage () {
    // Get the context exception
    Throwable lThrowable = mErrorContext.getException();

    // If it's another ExSystemError, message will be null, so we need to get to the real exception
    // Recurse while lThrowable is still an ExSystemError and while we have a readable property
    while (lThrowable instanceof ExSystemError && ((ExSystemError)lThrowable).getErrorProperties().getException() != null) {
      lThrowable = ((ExSystemError)lThrowable).getErrorProperties().getException();
    }

    return lThrowable.getMessage();
  }
}

package net.foxopen.fox.thread;

import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.command.XDoResult;

public class ResponseOverride 
implements XDoResult {
  
  private final FoxResponse mFoxResponse;
  
  public ResponseOverride (FoxResponse pFoxResponse){
    mFoxResponse = pFoxResponse;
  }
  
  public FoxResponse getFoxResponse(){
    return mFoxResponse;  
  }
  
}

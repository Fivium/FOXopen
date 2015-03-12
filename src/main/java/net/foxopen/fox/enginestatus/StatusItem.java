package net.foxopen.fox.enginestatus;


import java.io.IOException;
import java.io.Writer;

public interface StatusItem {

  public String getMnem();

  public MessageLevel getMaxMessageSeverity();

  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
  throws IOException;

}

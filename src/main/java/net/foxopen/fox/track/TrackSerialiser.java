package net.foxopen.fox.track;

import java.io.IOException;
import java.io.Writer;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;


public interface TrackSerialiser {

  public void serialiseToWriter(TrackLogger pTrackLogger, Writer pWriter, boolean pPrintTimerDetail)
  throws IOException;

  public FoxResponse createFoxResponse(TrackLogger pTrackLogger, FoxRequest pFoxRequest, boolean pPrintTimerDetail);

}

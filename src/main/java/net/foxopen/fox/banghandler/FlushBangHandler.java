package net.foxopen.fox.banghandler;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.queue.ServiceQueueHandler;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class FlushBangHandler
implements BangHandler {

  private static final FlushBangHandler INSTANCE = new FlushBangHandler();
  public static FlushBangHandler instance() {
    return INSTANCE;
  }

  private FlushBangHandler() {}

  @Override
  public String getAlias() {
    return "FLUSH";
  }

  @Override
  public Collection<String> getParamList() {
    return Collections.emptySet();
  }

  @Override
  public InternalAuthLevel getRequiredAuthLevel() {
    return InternalAuthLevel.INTERNAL_ADMIN;
  }

  @Override
  public boolean isDevAccessAllowed() {
    return true;
  }

  @Override
  public FoxResponse respond(FoxRequest pFoxRequest) {
    //Sync'd to prevent issues in App/XThread where a QueueHandler is requested during a flush
    synchronized(ServiceQueueHandler.class) {
      ServiceQueueHandler.destroyAllQueueHandlers();
      //TODO PN clean up this so property object isn't doing the init
      FoxGlobals.getInstance().getFoxEnvironment().getFileServiceProperties().initFileTransferServiceQueueHandler();
    }
    //Important - flush AFTER recreating ServiceQueueHandlers, otherwise new Apps could
    //assign ServiceQueues to a handler which is just about to be destroyed.
    try {
      FoxGlobals.getInstance().getFoxEnvironment().flushApplicationCache();
    }
    catch (ExServiceUnavailable | ExFoxConfiguration | ExApp e) {
      throw new ExInternal("An error occured while trying to flush the applications. ", e);
    }

    FoxLogger.getLogger().info("Flush Completed");

    SimpleDateFormat lDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    return new FoxResponseCHAR("text/html", new StringBuffer("\n" +
                                           "<!DOCTYPE html>\n" +
                                           "<html>\n" +
                                           "  <head>\n" +
                                           "    <title>FOX - Flush</title>\n" +
                                           "  </head>\n" +
                                           "  <body onload=\"document.body.style.backgroundColor = 'rgb(' + (Math.floor(Math.random()*100) + 155) + ',' + (Math.floor(Math.random()*100) + 155) + ',' + (Math.floor(Math.random()*100) + 155) + ')';\">\n" +
                                           "    <h1>FLUSH success - " + lDateFormatter.format(new Date()) + "</h1>\n" +
                                           "  </body>\n" +
                                           "</html>\n"), 0);//textResponse("FLUSH success - " + lDateFormatter.format(new Date()));
  }
}

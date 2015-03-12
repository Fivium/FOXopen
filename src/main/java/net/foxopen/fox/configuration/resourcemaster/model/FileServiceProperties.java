package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentDefinition;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentProperty;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.queue.ServiceQueueHandler;

public class FileServiceProperties {
  private final int mOverallConcurrentChannels;
  private final int mConcurrentUploadChannels;
  private final int mConcurrentDownloadChannels;
  private final int mWorkerSleepTimeMs;

  public static FileServiceProperties createFileServiceProperties(FoxEnvironmentDefinition pFoxEnvironmentDefinition) throws ExApp {
    return new FileServiceProperties(pFoxEnvironmentDefinition);
  }

  private FileServiceProperties(FoxEnvironmentDefinition pFoxEnvironmentDefinition) throws ExApp{
    mOverallConcurrentChannels = pFoxEnvironmentDefinition.getPropertyAsInteger(FoxEnvironmentProperty.FILE_OVERALL_CONCURRENCT_CHANNELS);
    mConcurrentUploadChannels = pFoxEnvironmentDefinition.getPropertyAsInteger(FoxEnvironmentProperty.FILE_CONCURRENT_UPLOAD_CHANNELS);
    mConcurrentDownloadChannels = pFoxEnvironmentDefinition.getPropertyAsInteger(FoxEnvironmentProperty.FILE_CONCURRENT_DOWNLOAD_CHANNELS);
    mWorkerSleepTimeMs = pFoxEnvironmentDefinition.getPropertyAsInteger(FoxEnvironmentProperty.FILE_WORK_SLEEP_TIME_MS);
    initFileTransferServiceQueueHandler();
  }

  public void initFileTransferServiceQueueHandler() {
    // Set up a FileServiceQueueHandler for use with the file transfer service.
    ServiceQueueHandler lFiletransferQueueHandler = new ServiceQueueHandler(mOverallConcurrentChannels, "FileTransfer",  mWorkerSleepTimeMs);

    // initialise some attributes on the file transfer handler (default max values to 1)
    lFiletransferQueueHandler.setHandlerAttribute(ServiceQueueHandler.UPLOAD_WORKITEM_TYPE+"-MAX", XFUtil.nvl(mConcurrentUploadChannels,1));
    lFiletransferQueueHandler.setHandlerAttribute(ServiceQueueHandler.DOWNLOAD_WORKITEM_TYPE+"-MAX", XFUtil.nvl(mConcurrentDownloadChannels,1));
    lFiletransferQueueHandler.setHandlerAttribute(ServiceQueueHandler.UPLOAD_WORKITEM_TYPE+"-CURRENT", 0);
    lFiletransferQueueHandler.setHandlerAttribute(ServiceQueueHandler.DOWNLOAD_WORKITEM_TYPE+"-CURRENT", 0);
  }

  public int getConcurrentDownloadChannels() {
    return mConcurrentDownloadChannels;
  }

  public int getWorkerSleepTimeMs() {
    return mWorkerSleepTimeMs;
  }

  public int getOverallConcurrentChannels() {
    return mOverallConcurrentChannels;
  }

  public int getConcurrentUploadChannels() {
    return mConcurrentUploadChannels;
  }
}

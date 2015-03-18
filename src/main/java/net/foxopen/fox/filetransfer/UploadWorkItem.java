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
package net.foxopen.fox.filetransfer;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.VirusScannerDefinition;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUpload;
import net.foxopen.fox.ex.ExUploadValidation;
import net.foxopen.fox.io.NonBlockingInputStream;
import net.foxopen.fox.queue.ServiceQueueHandler;
import net.foxopen.fox.queue.WorkItem;
import net.foxopen.fox.track.Track;
import org.apache.commons.fileupload.FileItemStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;


public class UploadWorkItem extends WorkItem {

  private FoxRequest mFoxRequest;
  private UploadInfo mUploadInfo;

  private static final int BYTE_READ_QUANTITY = 1024 * 4; // buffer size of 4k - seems to read http packet at a time of roughly 4k
  private static final int MAX_SUBSEQUENT_BUFFER_READS = 30;
  private static final int READ_TIMEOUT_MS = 1000 * 60 * 5;

  private static final int READING_FORM_DATA = 0;
  private static final int READING_FILE_DATA = 1;
  private static final int COMPLETE = 2;
  private static final int FAILED = 3;
  private int mStatus = READING_FORM_DATA;

  private byte [] mBuffer;

  private UCon mUCon;
  private Blob mUploadDestinationBlob;
  private OutputStream mBlobOutputStream;
  private InputStream mItemInputStream;
  private NonBlockingInputStream mItemNonBlockingInputStream;
  private MultipartUploadReader mMultipartUploadReader;
//  private FileItemIterator mItemIter;
//  private FileItemStream mCurrentItem;
  private int mItemTotalBytesRead = 0;
  private long mLastReadTime;
  private long[] mReadLoopIterations = new long[MAX_SUBSEQUENT_BUFFER_READS+1];

  private VirusScanner[] mVirusScannerArray;

  public Throwable mErrorException;

  public UploadWorkItem(FoxRequest pFoxRequest, UploadInfo pUploadInfo) {
    super(ServiceQueueHandler.UPLOAD_WORKITEM_TYPE, "UploadWorkItem FileId=" + pUploadInfo.getFileId());
    mFoxRequest = pFoxRequest;
    mUploadInfo = pUploadInfo;
    setAttribute("MultipartContentLength", pFoxRequest.getHttpRequest().getContentLength());
    setAttribute("FileId",pUploadInfo.getFileId());
    mUploadInfo.setHttpContentLength(pFoxRequest.getHttpRequest().getContentLength());
    mBuffer = new byte[BYTE_READ_QUANTITY];
  }

  private void initialiseUpload()
  throws Throwable {

    try {
      FileItemStream lCurrentItem = mMultipartUploadReader.getUploadFileItem();
      if(lCurrentItem == null) {
        throw new ExInternal("No file available on the multipart upload reader - either reader is in an invalid state, or the upload contained no file field");
      }

      mItemInputStream = lCurrentItem.openStream();

      if (lCurrentItem.isFormField()) {
        //Skip form fields, they should have been read by the MultipartUploadReader
        Track.alert("UploadWorkItem", "Unexpected form field encountered when streaming upload");
      }
      else {
        mItemNonBlockingInputStream = new NonBlockingInputStream(mItemInputStream, BYTE_READ_QUANTITY, MAX_SUBSEQUENT_BUFFER_READS);

        String lFilename = lCurrentItem.getName();
        mUploadInfo.setOriginalFileLocation(lFilename);
        int lBeginningIndex = lFilename.lastIndexOf("\\");
        if ( lFilename != null && lBeginningIndex != -1 ) {
          // substr from that last occurance of a back slash
          lFilename = lFilename.substring(lBeginningIndex + 1);
        }
        mUploadInfo.setFilename(lFilename);

        String lContentType = lCurrentItem.getContentType();
        mUploadInfo.setBrowserContentType(lContentType != null ? lContentType : "" );

        mStatus = READING_FILE_DATA;
        mLastReadTime = System.currentTimeMillis();
      }
    }
    catch (Throwable ex1) {
      throw ex1;
    }
  }

  private void readNextFileSegment()
  throws Throwable {

    int lItemBytesRead = 0;
    mStatus = READING_FILE_DATA;

    if(System.currentTimeMillis() - mLastReadTime > READ_TIMEOUT_MS)
      throw new ExUpload("Upload timed out. No data received on input channel for " + (READ_TIMEOUT_MS / 1000) + " seconds.", "a network timeout occurred");

    try {
      boolean lBreak = false;
      //read until the NonBlockingInputStream's buffer is empty (i.e. bytes read = 0)
      READ_LOOP:
      for (int i = 0; i < MAX_SUBSEQUENT_BUFFER_READS && lItemBytesRead != -1 && !lBreak; i++) {

        if ((lItemBytesRead = mItemNonBlockingInputStream.read(mBuffer)) != -1) {

          //Perform a content type check if this is the first packet read
          if(mItemTotalBytesRead == 0 && (lItemBytesRead > 0 || lItemBytesRead == -1)){
            performContentCheck();
          }

          if(lItemBytesRead > 0){
            mLastReadTime = System.currentTimeMillis();
          }

          mItemTotalBytesRead += lItemBytesRead;

          mBlobOutputStream.write(mBuffer, 0, lItemBytesRead); // write to blob

          //loop all available VirusScanners and write to each one's OutputStream
          for(int j=0; j<mVirusScannerArray.length; j++){
            //belt & braces - check for error before stream write
            if(mVirusScannerArray[j].isError()){
              throw new ExInternal("Virus Scanner " + mVirusScannerArray[j].getName() + " threw exception: " + mVirusScannerArray[j].getErrorMessage() + ". Upload cannot complete without Virus Detection.");
            }

            try{
              mVirusScannerArray[j].getOutputStream().write(mBuffer, 0, lItemBytesRead);
            } catch (IOException ex){
              throw new ExInternal("Virus Scanner encountered an input problem. Upload cannot continue.", ex);
            }
          }

          if(lItemBytesRead==0){
            //Record how many iterations we have done and break out of the loop,
            //the logic is this allows network IO to catch up with the WorkItem
            mReadLoopIterations[i]++;
            lBreak = true;
          }

        } else {
          // Once the file has been streamed adjust the status to read form data for any susequent form fields on stream
          mUploadInfo.setFileSize(mItemTotalBytesRead);
          mStatus = READING_FORM_DATA;
          mBlobOutputStream.close(); // wont need to read any more data to BLOB

          //Set virus scan status here for AJAX display purposes but DO NOT FIRE the event.
          //It is fired in XThread after control is returned. This prevents it from
          //firing twice in the case that we fire here, throw an ExUpload on virus detection,
          //catch in XThread and attempt to refire.
          mUploadInfo.setStatus(UploadStatus.VIRUS_CHECK);
          //mUploadInfo.getFileWSL().fireUploadEvent(null, mUCon, mUploadInfo);

          //close all VirusScanner OutputStreams and wait on all their threads.
          for(int j=0; j<mVirusScannerArray.length; j++){
            mVirusScannerArray[j].getOutputStream().close();
            // Wait for all the scanners to complete. This may happen after the
            // upload has completed.
            mVirusScannerArray[j].getCurrentThread().join(mVirusScannerArray[j].getTimeoutSecs()*1000);
            if(!mVirusScannerArray[j].isComplete()){
              throw new ExInternal("Virus Scanner " + mVirusScannerArray[j].getName() + " did not complete after waiting " + mVirusScannerArray[j].getTimeoutSecs() + " seconds.");
            }

          }

          mReadLoopIterations[i]++;
          lBreak = true;
        }

        // Test for virus detection/errors at the end of every output buffer write
        // ClamAV may terminate early if it detects a virus in the middle of the stream
        for(int j=0; j<mVirusScannerArray.length; j++){
          if(mVirusScannerArray[j].isVirusFound()){
            mUploadInfo.setStatus(UploadStatus.VIRUS_CHECK_FAILED);
            throw new ExUpload("Virus Detected in uploaded file: " + mVirusScannerArray[j].getScanResultString(), "a virus was detected in the file");
          }
          else if(mVirusScannerArray[j].isError()){
            throw new ExInternal("Virus Detection threw exception: " + mVirusScannerArray[j].getErrorMessage() + ". Upload cannot complete without Virus Detection.");
          }
        }
      }//for i

       if(!lBreak){
         mReadLoopIterations[MAX_SUBSEQUENT_BUFFER_READS]++;
       }
    } catch (IOException ioex) {
      //Nonblocking stream should throw IOExceptions from the true stream read, i.e.
      //broken pipe etc. Such errors may need to be presented to user and dealt with by application.
      //IOException may have occurred because the upload window was forcefully redirected
      //by JS. We want to record the root cause of this, i.e. a force fail request. Below
      //method optionally throws the correct exception if relevant.
      //throwForceFailException(); PN TEMP COMMENTED OUT FOR BETTER DIAGNOSTICS ON LIVE
      throw new ExUpload("I/O exception encountered while receiving data", "an unexpected network problem occurred", ioex);
    } catch (Throwable ex1) {
      throw ex1;
    }

  }

  private void performContentCheck(){
    //Content checking using the mulitpart form data and magic mime from the first packet
    //API event not raised here but either on exception or as part of the post-upload process
    //to maintain consistency with old routine
    try {
      mUploadInfo.getFileUploadType().validateContent(mUploadInfo,mBuffer);
    }
    catch (ExUploadValidation e) {
      mUploadInfo.setStatus(UploadStatus.CONTENT_CHECK_FAILED);
      throw new ExUpload(e.getMessage(), e.getMessage(), e);
    }
  }

  private boolean isUploadFailRequested () {
    return mUploadInfo.isForceFailRequested();
  }

  /**
   * Initialises this UploadWorkItem and prepares the file upload stream for reading before the item is added to a work queue.
   * This may read the first few packets from the file.
   * @param pUploadReader UploadReader which has an upload file ready to be retrieved. Leading form values should have
   *                      already been read.
   * @throws ExInternal
   */
  public void init(MultipartUploadReader pUploadReader)
  throws ExInternal {
    try {
      mMultipartUploadReader = pUploadReader;

      mUCon = (UCon)getAttribute("UCon");
      //TODO PN UPLOAD - better way than this surely
      if (mUCon == null)
        throw new ExInternal("Failed to find a database conection when trying to stream to Blob.");

      // Attach the progress listener to this upload info
      mUploadInfo.setProgressListener(pUploadReader.getFiletransferProgressListener());

      // Start the file upload
      initialiseUpload();
    }
    catch (Throwable ex) {
      throw new ExInternal("Error encountered while trying to initialise a file upload work item.\nOriginal error: " + ex.getMessage());
    }
  }

  public void initBLOB(Blob pBlob){
    try {
      mUploadDestinationBlob = pBlob;

      if (mUploadDestinationBlob == null) {
        throw new ExInternal("Failed to find a temporary Blob in work items attributes.  Unable to stream upload without Blob location to stream to.");
      }

      // Truncate the BLOB to stop multiple files which write into the same BLOB location merging together
      if (mUploadDestinationBlob.length() > 0) {
        mUploadDestinationBlob.truncate(0);
      }

      mBlobOutputStream = mUploadDestinationBlob.setBinaryStream(0L);

    } catch (Throwable ex) {
      throw new ExInternal("Error encountered while trying to initialise a file upload work item BLOB.\nOriginal error: " + ex.getMessage());
    }
  }

  public void execute()
  throws Throwable {

    // see if someone has requested that this upload fails
    if ( isUploadFailRequested() ){
      if(mVirusScannerArray != null){
        for(int i=0; i<mVirusScannerArray.length; i++){
          try{
            //close streams so virus scanners do not hang waiting for input
            mVirusScannerArray[i].getOutputStream().close();
            mVirusScannerArray[i].getInputStream().close();
          }catch(IOException ignore){}
        }
      }
      //throw new ExInternal("Upload work item failed to complete.  A fail was requested on the upload work item.");
      throwForceFailException();
    }

    if(mVirusScannerArray == null){
      initialiseVirusScanners();
    }

    // process some portion the request
    if (mStatus == READING_FILE_DATA) {
      readNextFileSegment(); // read next segment of file stream
    }
    else if (mStatus == READING_FORM_DATA) {
      //Delegate reading the rest of the form to the MultipartReader then mark as completed
      boolean lMoreFiles =  mMultipartUploadReader.readFormData();
      //Error if there is another file in the queue, we can't deal with this
      if(lMoreFiles) {
        throw new ExInternal("Tried to stream multiple file items for single request.  This is currently not implemented.");
      }

      mStatus = COMPLETE;
    }
  }

  /**
   * Throws an exception with the relevant text if a force fail has ocurred.
   */
  private void throwForceFailException()
  throws ExUpload{
    throw new ExUpload(mUploadInfo.getForceFailReason().getMessage());
  }

  private void initialiseVirusScanners(){

    //Ask the app to create a fresh set of virus scanners
    List<VirusScanner> lVirusScannerList = new ArrayList<>();
    for (VirusScannerDefinition lVirusScannerDefinition : mUploadInfo.getApp().getVirusScannerMap().values()) {
      lVirusScannerList.add(VirusScanner.createVirusScanner(lVirusScannerDefinition));
    }
    mVirusScannerArray = lVirusScannerList.toArray(new VirusScanner[lVirusScannerList.size()]);

    for(int i=0; i<mVirusScannerArray.length;i++){
      //Construct the pipe - outputstream is written to inputstream
      PipedInputStream lInputStream = new PipedInputStream();
      OutputStream lOutputStream;
      try {
        lOutputStream = new PipedOutputStream(lInputStream);
      } catch (IOException e) {
        throw new ExInternal("Virus Scanner pipe construction: " + e);
      }
      mVirusScannerArray[i].setInputStream(lInputStream);
      mVirusScannerArray[i].setOutputStream(lOutputStream);

      //Start the virus scan thread, which will wait for input on its InputStream
      new Thread(mVirusScannerArray[i],"VirusScanThread-" + i + "-" + Thread.currentThread().getName()).start();
    }

  }

  public boolean isComplete() {
    if (mStatus == COMPLETE) return true;
    return false;
  }

  public boolean isFailed() {
    if (mStatus == FAILED) return true;
    return false;
  }

  public void finaliseOnSuccess(){
    if(mItemNonBlockingInputStream != null)
      mItemNonBlockingInputStream.destroy();
  }

  public void finaliseOnError(Throwable pEx) {
    mStatus = FAILED;
    mErrorException = pEx;
    if (mUploadInfo != null) {
      if(mUploadInfo.getStatus().isInProgress() || mUploadInfo.getStatus() == UploadStatus.NOT_STARTED) {
        mUploadInfo.setStatus(UploadStatus.FAILED);
      }
      mUploadInfo.setSystemMsg(XFUtil.getJavaStackTraceInfo(pEx));
    }
    if(mItemNonBlockingInputStream != null)
      mItemNonBlockingInputStream.destroy();

  }
}


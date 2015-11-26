package net.foxopen.fox.io;

import net.foxopen.fox.ex.ExInternal;
import org.apache.commons.io.output.NullOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;


/**
 * A NonBlockingInputStream provides methods to read into a byte array without
 * causing the consuming thread to block whilst the underlying stream waits
 * for input from the OS. Both supported <code>read</code> methods may return
 * 0, indicating that the stream is still reading data. Consumers should wait
 * and retry reading later if this occurs.
 * <br/><br/>
 * This class is implemented as a list of Frame objects. The list has 2 pointers,
 * one indicating the frame currently being read from by the consumer, the other
 * which frame is either ready to be loaded or is being loaded. When the consumer
 * calls the read method, part of a single Frame's buffer is read. This may complete
 * the read of the Frame. At this point the ReadThread is woken up and attempts
 * to load the newly cleared Frame with more data from the underlying stream.
 * <br/><br/>
 * Two additional CheckedStreams can be used to ensure no corruption occurs. One
 * wraps the true InputStream and one is an OutputStream which is populated with
 * the bytes which the consumer reads. These streams can be disabled at compile time.
 * <br/><br/>
 * <b>IMPORTANT:</b> When this class is instantiated, it immediately begins to read from
 * the supplied InputStream. Ensure you have performed all the reads you require on
 * the stream before passing it to this class.
 */
public class NonBlockingInputStream extends InputStream {
  private static final int DEFAULT_FRAME_SIZE = 4096;
  private static final int DEFAULT_FRAME_COUNT = 2;
  private static final boolean CHECKED_STREAMS = true;

  private static final ThreadGroup gThreadGroup = new ThreadGroup("NonBlockingInputStreamThreads");
  private static int gThreadCount = 0;

  //Comments indicate fields requiring thread-safety considerations
  private final InputStream mInputStream; //ReadThread
  private final CheckedOutputStream mCheckedOutputStream;
  private final int mFrameSize;
  private final int mFrameCount;

  private final Thread mReadThread;

  private int mCurrentFramePointer = 0; //Main thread
  private int mLoadingFramePointer = -1; //ReadThread
  private final Frame[] mFrameArray; //both threads but immutable; sync on Frames

  private Throwable mException = null; //both threads

  public NonBlockingInputStream(InputStream pInputStream) {
    this(pInputStream, DEFAULT_FRAME_SIZE, DEFAULT_FRAME_COUNT);
  }

  public NonBlockingInputStream(InputStream pInputStream, int pFrameSize, int pFrameCount) {

    if(CHECKED_STREAMS){
      mInputStream = new CheckedInputStream(pInputStream, new CRC32());
      mCheckedOutputStream = new CheckedOutputStream(new NullOutputStream(), new CRC32());
    } else
      mInputStream = pInputStream;

    mFrameSize = pFrameSize;
    mFrameCount = pFrameCount;

    //Construct frames
    //Some validation
    if(pFrameCount < 2)
      throw new ExInternal("A NonBlockingInputStream requires at least 2 frames to operate correctly.");
    else if(pFrameSize < 1)
      throw new ExInternal("A NonBlockingInputStream requires a frame size of at least 1.");

    mFrameArray = new Frame[pFrameCount];
    for(int i=0; i < pFrameCount; i++)
      mFrameArray[i] = new Frame(i);

    //Construct the thread which does the true reads and set it running.
    //The first read attempt will occur immediately after it starts.
    //Sync on the class for read consistency when controlling the gThreadCount variable.
    //TODO could factor this out to a separate method or do on first read
    synchronized(NonBlockingInputStream.class){
      mReadThread = new Thread(gThreadGroup, new ReadThread(this), "NonBlockingInputStreamReadThread" + gThreadCount++);
      mReadThread.start();
    }
  }

  public int read(){
    throw new ExInternal("Single-byte read not yet supported in this class. Use read(byte[]) or read(byte[], int, int) instead.");
  }

  public int read(byte b[])
  throws IOException{
    return read(b, 0, b.length);
  }

  public int read(byte pByteArray[], int pOffset, int pLength)
  throws IOException{
   //TODO move to a finally so any remaining frame is read
    synchronized(this){
      //The ReadThread may have encountered an IOException or a general RuntimeException; check and throw here.
      if(mException != null){
        if(mException instanceof IOException)
          throw (IOException) mException;
        else if(mException instanceof RuntimeException)
          throw (RuntimeException) mException;
      }
    }

    Frame lCurFrame = getCurrentFrame();
    int lBytesRead = 0;

    synchronized(lCurFrame){
      switch(lCurFrame.getStatus()){
        case Frame.STATUS_LOADED:
          //If the frame is loaded its buffer is full and ready to be read, so set it
          //to current.
          lCurFrame.setAsCurrent();
          break;
        case Frame.STATUS_CLEAR:
        case Frame.STATUS_LOADING:
          //If the current frame is clear or loading, the ReadThread has not yet
          //loaded into it from the true InputStream. Return 0 immediately.
          return 0;
      }

      //If the EOF marker has been set on this frame, return -1 to indicate
      //EOF to consumer, computing the checksums first if necessary.
      if(lCurFrame.isEOF()){
        validateChecksumResult();
        return -1;
      }

      lBytesRead = lCurFrame.readBytes(pByteArray, pOffset, pLength);

      if(CHECKED_STREAMS)
        mCheckedOutputStream.write(pByteArray, pOffset, lBytesRead);

      //If the end of the frame has been reached, the consumer has read all the
      //data from it. It can be cleared and made available for population by
      //the read thread.
      if(lCurFrame.isReadComplete())
        clearCurrentFrame();
    }

    return lBytesRead;
  }

  private void validateChecksumResult()
  throws IOException{

    if(CHECKED_STREAMS && ((CheckedInputStream) mInputStream).getChecksum().getValue() != mCheckedOutputStream.getChecksum().getValue())
      throw new IOException("NonBlockingInputStream fatal error: stream checksums do not match.");

  }

  /**
   *
   * @return The frame currently being read from by the read() method.
   */
  private Frame getCurrentFrame(){
    return mFrameArray[mCurrentFramePointer];
  }

  /**
   * Gets the frame which the loadingFrame pointer points to. The frame will not
   * necessarily have a status of CLEAR - i.e. it may not be possible to load into
   * it. Use nextLoadingFrame() to get the next loadable frame.
   * @return The next frame which should be loaded into.
   */
  private Frame getLoadingFrame(){
    return mFrameArray[mLoadingFramePointer];
  }

  /**
   * Clears the current frame, making it available for loading, and adjusts the
   * current frame pointer to point to the next frame. The ReadThread is then
   * notified that a frame has become available.
   */
  private synchronized void clearCurrentFrame(){
    getCurrentFrame().clear();
    mCurrentFramePointer = ++mCurrentFramePointer % mFrameCount;
    this.notify();
  }


  /**
   * This method will be called by the ReadThread when it requires a new frame
   * to load data into. If the next frame along in the frame list is CLEAR, the
   * loadingFrame pointer is incremented and the new loadingFrame is returned. If
   * no CLEAR frame exists, the method returns null.
   * @return a Frame available for loading, or null if none are.
   */
  private synchronized Frame nextLoadingFrame(){
    if(mFrameArray[(mLoadingFramePointer + 1) % mFrameCount].getStatus() == Frame.STATUS_CLEAR){
      mLoadingFramePointer = ++mLoadingFramePointer % mFrameCount;
      return getLoadingFrame();
    }
    else
      return null;
  }

  /**
   * If an exception occurs in the ReadThread, it is set here and thrown to the caller
   * when it requests the next read.
   * @param pException the exception which occurred.
   */
  private void setException(Throwable pException){
    mException = pException;
  }

  /**
   * Call to force removal from memory.
   */
  public void destroy(){
    mReadThread.interrupt();
  }

  private class Frame{

    public static final int STATUS_CLEAR = 1;
    public static final int STATUS_LOADING = 2;
    public static final int STATUS_LOADED = 3;
    public static final int STATUS_CURRENT = 4;

    private byte[] mBuffer;
    private int mStatus;
    private int mBytesLoaded;
    private int mOffset;
    private boolean mEOF = false;
    private final int mId;

    public Frame(int pId){
      //Initialise this frame for its first load from the ReadThread.
      mId = pId;
      mBuffer = new byte[NonBlockingInputStream.this.mFrameSize];
      clear();
    }

    public int getStatus(){
      return mStatus;
    }

    public synchronized void setAsLoading(){
      if(mStatus != STATUS_CLEAR)
        throw new ExInternal("Cannot set a non-clear frame as loading");
      mStatus = STATUS_LOADING;
    }

    public void setAsLoaded(){
      if(mStatus != STATUS_LOADING)
        throw new ExInternal("Cannot set a non-loading frame as loaded");
      mStatus = STATUS_LOADED;
    }

    public void setAsCurrent(){
      if(mStatus != STATUS_LOADED)
        throw new ExInternal("Cannot set a non-loaded frame as current");
      mStatus = STATUS_CURRENT;
    }

    /**
     * Sets this Frame as the EOF marker and marks it as loaded.
     */
    public synchronized void setEOF(){
      mEOF = true;
      setAsLoaded();
    }

    public synchronized boolean isEOF(){
      if(mStatus != STATUS_CURRENT)
        throw new ExInternal("EOF status is only applicable to current frames");
      return mEOF;
    }

    private void validateRead()
    throws ExInternal{
      if(mStatus != STATUS_CURRENT)
        throw new ExInternal("Invalid read attempt from non-current frame");
      else if(mOffset >= mBytesLoaded)
        throw new ExInternal("Attempted to read after all bytes read from frame");
    }

    private synchronized void validateLoad()
    throws ExInternal{
      if(mStatus != STATUS_LOADING)
        throw new ExInternal("Invalid load attempt into frame which is not loading");
      else if(mBytesLoaded > 0 || mOffset > 0)
        throw new ExInternal("Load attempted on a frame which had bytesLoaded " + mBytesLoaded + " and offset " + mOffset + ". Both should be 0.");
    }

    /**
     * Read a single byte from this frame and increment the offset pointer.
     * Not currently used as parent class does not support single byte reads.
     * @return the read byte
     */
    public byte readByte(){
      validateRead();
      return mBuffer[mOffset++];
    }

    /**
     * Read the requested number of bytes from this frame's buffer into the target
     * array at the specified offset, then increment the frame's offset pointer.
     * @param pTargetArray
     * @param pTargetOffset
     * @param pLength
     * @return The actual number of bytes read.
     */
    public synchronized int readBytes(byte[] pTargetArray, int pTargetOffset, int pLength){
      validateRead();

      pLength = Math.min(pLength, mBytesLoaded - mOffset);
      System.arraycopy(mBuffer, mOffset, pTargetArray, pTargetOffset, pLength);
      mOffset += pLength;

      return pLength;
    }

    /**
     * Loads the bytes from the supplied InputStream into this frame's buffer,
     * and marks the frame as loaded.
     * @param pInputStream input stream to load from
     * @return number of bytes loaded
     * @throw any IOException thrown by the read of the true input stream
     */
    public int loadBytes(InputStream pInputStream)
    throws IOException{
      validateLoad();

      mBytesLoaded = pInputStream.read(mBuffer,0,mBuffer.length);
      if(mBytesLoaded == -1)
        setEOF();

      return mBytesLoaded;
    }

    public boolean isReadComplete(){
      return mOffset==mBytesLoaded;
    }

    public void clear(){
      mBytesLoaded = 0;
      mOffset = 0;
      mStatus = STATUS_CLEAR;
    }

  }

  private static class ReadThread implements Runnable{

    private WeakReference mParentNBISRef;

    public ReadThread(NonBlockingInputStream pParentNBIS){
      mParentNBISRef = new WeakReference(pParentNBIS);
    }

    public void run(){

      int lBytesRead = 0;
      boolean lForceFail = false;

      try{
        //Kill thread if it's interrupted, read is EOF, an exception occurred or the parent object was GC'd
        NonBlockingInputStream lParentNBIS;
        while(!Thread.interrupted() && lBytesRead != -1 && !lForceFail  && (lParentNBIS = (NonBlockingInputStream) mParentNBISRef.get())!=null){

          //Try and reserve a clear frame for loading.
          Frame lTargetFrame = lParentNBIS.nextLoadingFrame();
          if(lTargetFrame != null){

            lTargetFrame.setAsLoading(); //synchronized on frame

            //IMPORTANT: NOT synchronized. If the stream read itself were sync'ed on the
            //Frame, the main thread would hang waiting for the monitor on this Frame
            //when the consumer requested a read. Syncing on status changes and checks
            //is sufficient for thread safety. I.e.
            // sync{ LOADING } ... (read) ...  sync { LOADED }
            lBytesRead = lTargetFrame.loadBytes(lParentNBIS.mInputStream);
            //System.out.println("Stream read "+ lBytesRead + " into frame " + lTargetFrame.mId);

            if(lBytesRead != -1)
              lTargetFrame.setAsLoaded(); //synchronized on frame

          } else {
            //No frame is ready for loading - the consumer needs to completely read
            //at least one frame to clear it. Wait on the parent object - it will notify
            //this thread when a clear frame is available.
            synchronized(lParentNBIS){
              try {
                lParentNBIS.wait();
                //System.out.println("Thread was Notified"); // debug remove
              } catch (InterruptedException e) {
                lForceFail = true;
              }
            }
          }

        }
      } catch (Throwable e) {
        if(mParentNBISRef.get() != null)
          ((NonBlockingInputStream) mParentNBISRef.get()).setException(e);
      } finally {
        synchronized(NonBlockingInputStream.class){
          NonBlockingInputStream.gThreadCount--;
        }
      }
      //The thread will die gracefully when the InputStream is completely read
      //or an exception occurred.
    }
  }


}

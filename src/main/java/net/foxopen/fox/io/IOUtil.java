package net.foxopen.fox.io;

import net.foxopen.fox.ex.ExInternal;
import oracle.sql.CLOB;
import oracle.sql.CharacterSet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * I/O Stream utility class.
 */
public class IOUtil {
  private static HashMap ORACLE_TO_JAVA_CHARSETS = new HashMap(5);
  static {
    ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.WE8MSWIN1252_CHARSET), "windows-1252");
    ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.AL32UTF8_CHARSET), "UTF-8");
    ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.AL16UTF16_CHARSET), "UTF-16");
    ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.US7ASCII_CHARSET), "US-ASCII");
    ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.WE8ISO8859P1_CHARSET), "ISO-8859-1");
  }

   /**
    * Closes an input stream ignoring any errors.
    *
    * @param is the stream to close, can be null
    */
   public static void close(InputStream is)
   {
      if (is == null) return;
      try { is.close(); } catch (IOException ignoreEx){}
   }

   /**
    * Closes an output stream ignoring any errors.
    *
    * @param os the stream to close, can be null
    */
   public static void close(OutputStream os)
   {
      if (os == null) return;
      try { os.close(); } catch (IOException ignoreEx){}
   }

   /**
    * Closes a reader ignoring any errors.
    *
    * @param reader the reader to close, can be null
    */
   public static void close(Reader reader)
   {
      if (reader == null) return;
      try { reader.close(); } catch (IOException ignoreEx){}
   }

   /**
    * Closes a writer ignoring any errors.
    *
    * @param writer the writer to close, can be null
    */
   public static void close(Writer writer)
   {
      if (writer == null) return;
      try { writer.close(); } catch (IOException ignoreEx){}
   }

   /**
    * Automatically transfers data from the specified
    * <code>InputStream</code> to the <code>OutputStream</code>
    * until End-Of-File (EOF) is encountered on the input stream.
    *
    * <p>Note that the input and output streams are not closed
    * by this method.
    *
    * @param is the input stream from which the data will be read
    * @param os the output stream where the data will be written.
    * @param bufferSize the transfer buffer size of the buffer to use
    *        during the transfer for efficiency.
    * @exception IOException thrown if an error occurs
    * during the transfer.
    */
   public static void transfer(InputStream is,
                               OutputStream os,
                               int bufferSize) throws IOException
   {
      byte transferBuf[] = new byte[bufferSize];
      int readCount;
      while ( (readCount = is.read(transferBuf)) != -1 )
      {
         os.write(transferBuf, 0, readCount);
         os.flush();
      }
   }

   /**
    * Automatically transfers data from the specified
    * <code>InputStream</code> to the <code>OutputStream</code>
    * until End-Of-File (EOF) is encountered on the input stream.
    *
    * <p>Note that the input and output streams are not closed
    * by this method.
    *
    * @param is the input stream from which the data will be read
    * @param os the output stream where the data will be written.
    * @exception IOException thrown if an error occurs
    * during the transfer.
    */
   public static void transfer(InputStream is,
                               OutputStream os) throws IOException
   {
      transfer(is, os, 2048);
   }

   /**
    * Automatically transfers data from the specified
    * <code>Reader</code> to the <code>Writer</code>
    * until End-Of-File (EOF) is encountered on the input stream.
    *
    * <p>Note that the reader and writer streams are not closed
    * by this method.
    *
    * @param reader the reader from which the character data will be read
    * @param writer the writer where the data will be written.
    * @param bufferSize the transfer buffer size of the buffer to use
    *        during the transfer for efficiency.
    * @exception IOException thrown if an error occurs
    * during the transfer.
    */
   public static void transfer(Reader reader,
                               Writer writer,
                               int bufferSize) throws IOException
   {
      char transferBuf[] = new char[bufferSize];
      int readCount;
      while ( (readCount = reader.read(transferBuf)) != -1 )
      {
         writer.write(transferBuf, 0, readCount);
         writer.flush();
      }
   }

   /**
    * Automatically transfers data from the specified
    * <code>Reader</code> to the <code>Writer</code>
    * until End-Of-File (EOF) is encountered on the input stream.
    *
    * <p>Note that the reader and writer streams are not closed
    * by this method.
    *
    * @param reader the reader from which the character data will be read
    * @param writer the writer where the data will be written.
    * @exception IOException thrown if an error occurs
    * during the transfer.
    */
   public static void transfer(Reader reader,
                               Writer writer) throws IOException
   {
      transfer(reader, writer, 2048);
   }

  /**
   * Automatically transfers data from the specified
   * <code>Reader</code> to the <code>OutputStream</code>
   * until End-Of-File (EOF) is encountered on the input stream.
   *
   * <p>Note that the reader and OutputStream streams are not closed
   * by this method.</p>
   *
   * @param reader the reader from which the character data will be read
   * @param os the OutputStream where the data will be written.
   * @exception IOException thrown if an error occurs
   * during the transfer.
   */
   public static void transfer(Reader reader, OutputStream os, int bufferSize) throws IOException
   {
     char transferBuf[] = new char[bufferSize];
     int readCount;
     while ((readCount = reader.read(transferBuf)) != -1 )
     {
       String lString = String.valueOf(transferBuf);
       os.write(lString.getBytes(), 0, readCount);
       os.flush();
     }
   }

   /**
    * Reads and returns a line from the specified <code>Reader</code>. A line is
    * considered to be terminated by any one of a line feed ('\n'),
    * a carriage return ('\r'), or a carriage return followed immediately by a linefeed.
    *
    * @param reader the reader from which the line will be read.
    * @return a line of text, without the line termination characters, or null if End-of-file
    *         reached.
    * @exception IOException thrown if an error occurs during the read operation.
    */
   public static String readLine(Reader reader) throws IOException
   {
      int ch = reader.read();
      if (ch == '\n')
         return "";

      StringBuffer lineBuf = new StringBuffer();
      while (ch != -1 && ((char)ch) != '\n')
      {
         lineBuf.append((char)ch);
         ch = reader.read();
      }

      if (lineBuf.length() > 0 && lineBuf.charAt(lineBuf.length()-1) == '\r')
         lineBuf.deleteCharAt(lineBuf.length()-1);

      return (lineBuf.length() == 0 ? null : lineBuf.toString());
   }

   /**
    * Reads and returns a line from the specified <code>InputStream</code>. A line is
    * considered to be terminated by any one of a line feed ('\n'),
    * a carriage return ('\r'), or a carriage return followed immediately by a linefeed.
    *
    * @param is the stream from which the line will be read.
    * @return a line of text, without the line termination characters, or null if End-of-file
    *         reached. The default system character encoding is assumed for the input stream.
    * @exception IOException thrown if an error occurs during the read operation.
    */
   public static String readLine(InputStream is) throws IOException
   {
      return readLine(is, new ByteArrayOutputStream());
   }

    /**
    * Reads and returns a line from the specified <code>InputStream</code>. A line is
    * considered to be terminated by any one of a line feed ('\n'),
    * a carriage return ('\r'), or a carriage return followed immediately by a linefeed.
    *
    * @param is the stream from which the line will be read.
    * @param baos an existing buffer that will be reset and reused for efficiency
    * @return a line of text, without the line termination characters, or null if End-of-file
    *         reached. The default system character encoding is assumed for the input stream.
    * @exception IOException thrown if an error occurs during the read operation.
    */
   public static String readLine(InputStream is, ByteArrayOutputStream baos) throws IOException
   {
      return readLine(is, "US-ASCII", baos);
   }

   /**
    * Reads and returns a line from the specified <code>InputStream</code>. A line is
    * considered to be terminated by any one of a line feed ('\n'),
    * a carriage return ('\r'), or a carriage return followed immediately by a linefeed.
    *
    * @param is the stream from which the line will be read.
    * @param encoding the character encoding of the byte stream.
    * @return a line of text, without the line termination characters, or null if End-of-file
    *         reached.
    * @exception IOException thrown if an error occurs during the read operation.
    * @see java.lang.String for the supported character encodings.
    */
   public static String readLine(InputStream is,
                                 String encoding) throws IOException
   {
      return readLine(is, encoding, new ByteArrayOutputStream());
   }

      /**
    * Reads and returns a line from the specified <code>InputStream</code>. A line is
    * considered to be terminated by any one of a line feed ('\n'),
    * a carriage return ('\r'), or a carriage return followed immediately by a linefeed.
    *
    * @param is the stream from which the line will be read.
    * @param encoding the character encoding of the byte stream.
    * @param baos an existing buffer that will be reset and reused for efficiency
    * @return a line of text, without the line termination characters, or null if End-of-file
    *         reached.
    * @exception IOException thrown if an error occurs during the read operation.
    * @see java.lang.String for the supported character encodings.
    */
   public static String readLine(InputStream is,
                                 String encoding,
                                 ByteArrayOutputStream baos) throws IOException
   {
      String line;
      int byteRead = is.read();

      while (byteRead != -1 && byteRead != (byte)'\n')
      {
         baos.write(byteRead);
         byteRead = is.read();
      }

      if (baos.size() == 0)
         line =  (byteRead == -1 ? null : ""); // EOF or \n as first byte read
      else
      {
         byte byteBuf[] = baos.toByteArray();
         line = new String(byteBuf, 0, (byteBuf[byteBuf.length-1] == '\r' ? byteBuf.length-1 : byteBuf.length), encoding);
      }

      return line;
   }

  public static InputStream clobCharacterSetConversion (CLOB pClobRef, String pOutputEncoding) {
    String lEncodingOut = "UTF-8";
    if (pOutputEncoding != null) {
      lEncodingOut = pOutputEncoding;
    }

    try {
      Short CharacterSetID = new Short(pClobRef.getConnection().getPhysicalConnection().getDbCsId());
      String lDBCharacterSet = (String)ORACLE_TO_JAVA_CHARSETS.get(CharacterSetID);

      if (lDBCharacterSet == null) {
        throw new ExInternal("Database character set not supported");
      }

      StringBuffer lDBBuffer = new StringBuffer();
      try {
        InputStreamReader lInputReader = new InputStreamReader(pClobRef.binaryStreamValue(), lDBCharacterSet);
        Reader lReader = new BufferedReader(lInputReader);
        int lChar;
        while ((lChar = lReader.read()) > -1) {
          lDBBuffer.append((char)lChar);
        }
        lReader.close();
      }
      catch (IOException e) {
        throw new ExInternal("Error processing CLOB data from StreamParcel", e);
      }

      try {
        return new ByteArrayInputStream(lDBBuffer.toString().getBytes(lEncodingOut));
      }
      catch (UnsupportedEncodingException e) {
        throw new ExInternal("Error processing CLOB data from StreamParcel", e);
      }

      //return mClobRef.getAsciiStream();
    }
    catch (SQLException e) {
      throw new ExInternal("Error accessing CLOB data from StreamParcel", e);
    }
  }
}

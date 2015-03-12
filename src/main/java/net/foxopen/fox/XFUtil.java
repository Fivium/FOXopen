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
package net.foxopen.fox;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExRoot;
import net.foxopen.fox.ex.ExRuntimeRoot;
import net.foxopen.fox.io.IOUtil;
import net.foxopen.fox.logging.FoxLogger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public abstract class XFUtil {
  public static final int SANITISE_ALERTS = 1;
  public static final int SANITISE_HINTS = 2;
  public static final int SANITISE_HTMLENTITIES = 3;
  public static final int SANITISE_ALT = 4;

  public static final String XML_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
  public static final String XML_DATE_FORMAT = "yyyy-MM-dd";

  public static final Integer INTEGER_MAX_VALUE = new Integer(Integer.MAX_VALUE);
  private static final int MAX_ALPHA_RADIX = 62;

  private static final long UNIQUE_CACHE_SIZE = 1000;
  static final long UNIQUE_RESET_BOUNDARY = Long.MAX_VALUE - Integer.MAX_VALUE;

  // static fields used to generate unique keys across web servers
  // these are set in 'initialise' to avoid recursive init problems
  static char _gUniquePrefix;
  static String _gUniqueSuffix;
  static long _gUniqueCount = UNIQUE_RESET_BOUNDARY+1; // Set high to force reset on first unique
  private static final Iterator _gObsoleteIterator = getUniqueIterator(10000);


  /**
   * All possible chars for representing a number as a String
   */
  final static char[] ALPHA_DIGETS = {
    '0' , '1' , '2' , '3' , '4' , '5' ,
    '6' , '7' , '8' , '9' , 'a' , 'b' ,
    'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
    'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
    'o' , 'p' , 'q' , 'r' , 's' , 't' ,
    'u' , 'v' , 'w' , 'x' , 'y' , 'z' ,
    'A' , 'B' , 'C' , 'D' , 'E' , 'F' ,
    'G' , 'H' , 'I' , 'J' , 'K' , 'L' ,
    'M' , 'N' , 'O' , 'P' , 'Q' , 'R' ,
    'S' , 'T' , 'U' , 'V' , 'W' , 'X' ,
    'Y' , 'Z'
  };

  private final static char[] lAlphabetChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

  final static String[] mons = {
                "JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"
                ,"JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE","JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER"
              };

  static {
    _gUniquePrefix =  FoxGlobals.getInstance().getServerHostName().charAt(0);
  }
  private static String gHostIP = null;

  // Initialise class (this now only preloads class)
  public static void init() {
  }

  public static final Object _gUniqueSyncObject = new Object();

  /** @depricate This should be obsoleted in preference for using instances from getUniqueIterator */
  @Deprecated
  public static final String unique() {
    String lUnique = (String) _gObsoleteIterator.next();
    //Removed check for A-Z as the first character of a unique string
    return lUnique;
  }

  public static final Iterator<String> getUniqueIterator() {
    return new UniqueIterator(UNIQUE_CACHE_SIZE);
  }

  public static final Iterator<String> getUniqueIterator(long pCacheSize) {
    return new UniqueIterator(pCacheSize);
  }

  public static <T> T nvl(T pValue, T pReplaceNullsWith){
    if (pValue != null){
      return pValue;
    }
    return pReplaceNullsWith;
  }

  /** (OVERLOADED) SQL like Null value replace utility */
  public static String nvl(String pValue, String pReplaceNullsWith){
    if (pValue != null && pValue.length() != 0){
      return pValue;
    }
    return pReplaceNullsWith;
  }

//  /** (OVERLOADED) SQL like Null value replace utility */
//  public static final int nvl(Integer pValue, Integer pReplaceNullsWith){
//    if (pValue!=null){
//      return pValue.intValue();
//    }
//    return pReplaceNullsWith.intValue();
//  }

//  /** (OVERLOADED) SQL like Null value replace utility */
//  public static final int nvl(Integer pValue, int pReplaceNullsWith){
//    if (pValue!=null){
//      return pValue.intValue();
//    }
//    return pReplaceNullsWith;
//  }

  /** (OVERLOADED) SQL like Null value replace utility */
  public static final int nvl(String pValue, int pReplaceNullsWith) {
    if (pValue!=null && pValue.length()!=0){
      return Integer.valueOf(pValue).intValue();
    }
    return pReplaceNullsWith;
  }

  /** (OVERLOADED) SQL like Null value replace utility converts to "" */
  public static String nvl(String pValue) {
    return nvl(pValue, "");
  }

  /**
   * This initialise all the first letters to uppercase and the rest to lowercase.
   * <strong>Note:</strong> This also replaces underscores with spaces
   *
   * @param pInputString String to initcap
   * @return Initcapped version of pInputString, with underscores replaced with spaces, or null if pInputString was null
   */
  public static String initCap(String pInputString) {
    if (pInputString == null) {
      return null;
    }

    StringBuilder lInitCappedResult = new StringBuilder(pInputString.length());
    String lCurrentLetter;
    lInitCappedResult.append(pInputString.substring(0, 1).toUpperCase());
    for (int i = 1; i < pInputString.length(); i++) {
      lCurrentLetter = pInputString.substring(i, i + 1);
      if (" ".equals(lCurrentLetter) || "_".equals(lCurrentLetter)) {
        lInitCappedResult.append(" ");
        i++;
        lInitCappedResult.append(pInputString.substring(i, i + 1).toUpperCase());
      }
      else {
        lInitCappedResult.append(lCurrentLetter.toLowerCase());
      }
    }

    return lInitCappedResult.toString();
  }

  /** Global replace all occurances of string */
  public static final String replace(String find, String replace, String str)
  {
    if(str.indexOf(find) == -1) { // Duplicated initial search NOT EFFICIENT
      return str;
    }
    StringBuffer sBuf = new StringBuffer();
    sBuf.append(str);
    replace(find, replace, sBuf);
    return sBuf.toString();
  }

  public static final int replace(String find, String replace, StringBuilder buf) {
    // NOT EFFICIENT (nested char loops)
    int c = 0;
    int l = find.length();
    int r = replace.length();
    int p = 0;
    int i;
    while((i = buf.indexOf(find, p)) != -1) {
      buf.replace(i,i+l,replace);
      c++;
      p=i+r; // resume after this replacement
    }
    return c;
  }

  /** Global replace all occurances of string */
  /**
   * @deprecated use StringBuilder version
   */
  @Deprecated
  public static final int replace(String find, String replace, StringBuffer buf) {
    // NOT EFFICIENT (nested char loops)
    int c = 0;
    int l = find.length();
    int r = replace.length();
    int p = 0;
    int i;
    while((i = buf.indexOf(find, p)) != -1) {
      buf.replace(i,i+l,replace);
      c++;
      p=i+r; // resume after this replacement
    }
    return c;
  }

  /** Global replace all occurances of string */
  public static final int replaceRegexpExhaustive(String pRegexp, String pReplacement, StringBuffer pBuf) {
    // Compile regexp and matcher with buffer contents
    Pattern p = Pattern.compile(pRegexp);
    Matcher m = p.matcher(pBuf.toString());
    int count = 0;

    // If there's a result, clear out buffer for reprocessing
    boolean result = m.find();
    if (result) {
      pBuf.delete(0, pBuf.capacity());
    }
    while (result) {
      count++;
      m.appendReplacement(pBuf, pReplacement);
      result = m.find();
    }
    // Add the last segment of input to
    // the new String
    m.appendTail(pBuf);
    return count;
  }

  /** Global replace all occurances of string iteratively until all occurances exhausted */
  //DO NOT REMOVE - used by docgen plugin
  public static final int replaceExhaustive (String find, String replace, StringBuffer buf) {
    // NOT EFFICIENT (nested char loops)
    int c = 0;
    int l = find.length();
    int p = 0;
    int i;
    while((i = buf.indexOf(find, p)) != -1) {
      buf.replace(i,i+l,replace);
      c++;
      p=i; // resume before this replacement
    }
    return c;
  }

  @Deprecated
  public static final String pathPopHead(StringBuffer buf, boolean skip_lead_slashes) {
    // Remove leading slashs
    if(skip_lead_slashes) {
      while (buf.length()>0 && buf.charAt(0)=='/') {
        buf.deleteCharAt(0);
      }
    }
    // See first slash
    if(buf.length()==0) return "";
    int p = buf.indexOf("/");
    if (p==-1) p=buf.length();
    // Extract and remove head string
    String str = buf.substring(0,p);
    buf.delete(0,p);
    // Remove intermediate slashs
    while (buf.length()>0 && buf.charAt(0)=='/') {
      buf.deleteCharAt(0);
    }
    // Return extraced string
    return str;
  }

  @Deprecated
  public static final String pathPopTail(StringBuffer buf) {
    if(buf.length()==0) return "";
    int p = buf.lastIndexOf("/");
    String str;
    if (p == -1) {
       str = buf.toString();
       buf.delete(0, buf.length());
    }
    else if (p+1==buf.length()) {
      str = "";
      buf.delete(p, buf.length());
    }
    else {
       str = buf.substring(p+1,buf.length());
       buf.delete(p, buf.length());
    }
    return str;
  }

  @Deprecated
  public static final StringBuffer pathPushTail(StringBuffer poPathStringBuffer, String pTailWordString) {
    if(pTailWordString!=null && pTailWordString.length()!=0) {
      if(poPathStringBuffer.length()>0) {
        poPathStringBuffer.append('/');
      }
      poPathStringBuffer.append(pTailWordString);
    }
    return poPathStringBuffer;
  }

  @Deprecated
  public static final StringBuffer pathPushHead(StringBuffer poPathStringBuffer, String pHeadWordString) {
    if(pHeadWordString!=null && pHeadWordString.length()!=0) {
      if(poPathStringBuffer.length()>0) {
        poPathStringBuffer.insert(0,'/');
      }
      poPathStringBuffer.insert(0, pHeadWordString);
    }
    return poPathStringBuffer;
  }

  public static String tagPopNS(StringBuffer buf) {
    if(buf.length()==0) return "";
    int p = buf.lastIndexOf(":");
    if(p==-1) return "";
    String str = buf.substring(0,p);
    buf.delete(0,p+1);
    return str;
  }

  /**
   * Remove all forward slash characters from the beginning of a StringBuilder in-place
   *
   * @param pPath StringBuilder path potentially starting with forward slash characters to be removed
   */
  public static final void pathStripLeadSlashes(StringBuilder pPath) {
    while (pPath.length() > 0 && pPath.charAt(0) == '/') {
      pPath.deleteCharAt(0);
    }
  }

  public static final String pathStripLeadSlashes(String pPath) {
    StringBuilder lBuilder = new StringBuilder(pPath);
    pathStripLeadSlashes(lBuilder);
    return lBuilder.toString();
  }


  /**
   * Pop the head off a path and return it. pPath is modified to pop the head off and the result is returned.
   *
   * @param pPath StringBuilder containing a path
   * @param pSkipLeadSlashes If the function should remove any slashes from the start of the path before processing
   * @return The head of the path that was popped from pPath
   */
  public static final String pathPopHead(StringBuilder pPath, boolean pSkipLeadSlashes) {
    if(pPath.length() == 0) {
      return "";
    }

    // Remove leading slashs
    if(pSkipLeadSlashes) {
      pathStripLeadSlashes(pPath);
    }

    // Find first slash
    int lSlashIndex = pPath.indexOf("/");

    if (lSlashIndex == -1) {
      lSlashIndex = pPath.length();
    }

    // Extract and remove head string
    String lPoppedHead = pPath.substring(0, lSlashIndex);
    pPath.delete(0, lSlashIndex);

    // Remove intermediate slashes from the remaining path
    pathStripLeadSlashes(pPath);

    // Return extraced string
    return lPoppedHead;
  }

  /**
   *  Pop the tail off a path and return it. pPath is modified to pop the tail off and the result is returned.
   *
   * @param pPath StringBuilder containing a path
   * @return The tail of the path that was popped from pPath
   */
  public static final String pathPopTail(StringBuilder pPath) {
    if(pPath.length()==0) {
      return "";
    }

    int lSlashIndex = pPath.lastIndexOf("/");

    String lPoppedTail;
    if (lSlashIndex == -1) {
      // No slash, so anything in path is "tail"
      lPoppedTail = pPath.toString();
      // Clear out path
      pPath.delete(0, pPath.length());
    }
    else if ((lSlashIndex + 1) == pPath.length()) {
      // Slash at end of path, so no tail
      lPoppedTail = "";
      // Strip trailing slash on pPath
      pPath.delete(lSlashIndex, pPath.length());
    }
    else {
      // Slash found with some tail after it, get tail
      lPoppedTail = pPath.substring(lSlashIndex+1,pPath.length());
      // And remove the tail and trailing slash from the path
      pPath.delete(lSlashIndex, pPath.length());
    }

    return lPoppedTail;
  }

  /**
   * Prefix pHead to pPath, including a forward slash if needed
   *
   * @param pPath Path to push head on to
   * @param pHead Head part to prefix to pPath
   * @return pPath (though this is also modified in-place)
   */
  public static final StringBuilder pathPushHead(StringBuilder pPath, String pHead) {
    if(!XFUtil.isNull(pHead)) {
      if(pPath.length() > 0) {
        pPath.insert(0, '/');
      }
      pPath.insert(0, pPath);
    }

    return pPath;
  }

  /**
   * Append pTail to pPath, including a forward slash if needed
   *
   * @param pPath Path to push tail on to
   * @param pTail Tail part to append to pPath
   * @return pPath (though this is also modified in-place)
   */
  public static final StringBuilder pathPushTail(StringBuilder pPath, String pTail) {
    if(!XFUtil.isNull(pTail)) {
      if(pPath.length() > 0 && pPath.charAt(pPath.length() - 1) != '/') {
        pPath.append('/');
      }

      pPath.append(pTail);
    }
    return pPath;
  }

  /** Evaluate Fox String Boolean */
  public static boolean stringBoolean(String pString) {
    if(pString.length()==0
    || pString.equals(".")
    || pString.equalsIgnoreCase("y")
    ||  pString.equalsIgnoreCase("yes")
    ||  pString.equalsIgnoreCase("true")
    ) {
      return true;
    }
    return false;
  }

  public static String getJavaStackTraceInfo(Throwable th) {
    String stackTraceInfo;

    if (th instanceof ExRoot) {
      stackTraceInfo = ((ExRoot)th).getMessageStack();
    }
    else if (th instanceof ExRuntimeRoot) {
      stackTraceInfo = ((ExRuntimeRoot)th).getMessageStack();
    }
    else {
      StringWriter sw = new StringWriter();
      th.printStackTrace(new PrintWriter(sw));
      stackTraceInfo = sw.toString();
    }

    return stackTraceInfo;
  }

  public static boolean isNull(Object pStringOrObject) {
    if(pStringOrObject==null || pStringOrObject.equals("")) {
      return true;
    }
    return false;
  }

  public static String[] toStringArray(ArrayList pArrayList) {
    String[] prototype = {};
    return (String[]) pArrayList.toArray(prototype);
  }

  public static final List toArrayList(Object pObject) {
    List array;

    // Catch null object
    if(pObject==null) {
      array = new ArrayList(1);
    }

    // Catch single String passed (Servlet parameter bug workaround)
    else if(pObject instanceof String) {
      array = new ArrayList(1);
      array.add(pObject);
    }

    // Process normal string array (Documented servlet processing)
    else {
      String[] lObjectArray = (String[]) pObject;
      array = new ArrayList(lObjectArray.length);
      for(int i=0; i<lObjectArray.length; i++) {
        array.add(lObjectArray[i]);
      }
    }
    return array;
  }


  /** Reads an Input Stream into a new String Buffer */
  public static StringBuffer toStringBuffer(
    InputStream pInputStream
  , int pChunkSize
  , int pMaxSize
  )
  {
    return toStringBuffer(
      new InputStreamReader(pInputStream)
    , pChunkSize
    , pMaxSize
    );
  }

  /** Reads a Reader into a new String Buffer */
  public static StringBuffer toStringBuffer(
    Reader pReader
  , int pChunkSize
  , long pMaxSize
  )
  {

    // Default max size to very big
    if(pMaxSize < 0) {
      pMaxSize = Integer.MAX_VALUE;
    }

    // Target data (starts out empty), and chunk buffer
    StringBuffer lTargetBuffer = new StringBuffer();
    char lReadBuffer[] = new char[pChunkSize];

    // While max size not exhausted
    int lReadSize;
    int lReadMax = pChunkSize;
    READ_LOOP: while(lTargetBuffer.length()<pMaxSize) {

      // Compute read size
      if(lTargetBuffer.length()+pChunkSize>=pMaxSize) {
        lReadMax= (int) (pMaxSize-lTargetBuffer.length());
        if(lReadMax<1) {
          throw new ExInternal("Read size compute error");
        }
      }

      // Read data into buffer
      try {
        lReadSize = pReader.read(lReadBuffer, 0, lReadMax);
        if(lReadSize==-1) {
          break READ_LOOP;
        }
        else if(lReadSize==0) {
          throw new ExInternal("Unexpected zero read size");
        }
      }
      catch (IOException e) {
        throw new ExInternal("Error converting InputStream into ByteArrayr", e);
      }

      // When data read, append to target
      lTargetBuffer.append(lReadBuffer, 0, lReadSize);
    }

    // Return target
    return lTargetBuffer;

  }

  /** Reads a InputStream into a new byteArray */
  public static byte[] toByteArray(
    InputStream pInputStream
  , int pChunkSize
  , long pMaxSize
  )
  {

    // Default max size to very big
    if(pMaxSize < 0) {
      pMaxSize = Long.MAX_VALUE;
    }

    // Target data (starts out empty), and chunk buffer
    byte[] lTargetArray = new byte[0];
    byte lReadBuffer[] = new byte[pChunkSize];

    // While max size not exhausted
    int lReadSize;
    int lReadMax = pChunkSize;
    READ_LOOP: while(lTargetArray.length<pMaxSize) {

      // Compute read size
      if(lTargetArray.length+pChunkSize>=pMaxSize) {
        lReadMax=(int) (pMaxSize-lTargetArray.length);
        if(lReadMax<1) {
          throw new ExInternal("Read size compute error");
        }
      }

      // Read data into buffer
      try {
        lReadSize = pInputStream.read(lReadBuffer, 0, lReadMax);
        if(lReadSize==-1) {
          break READ_LOOP;
        }
        else if(lReadSize==0) {
          throw new ExInternal("Unexpected zero read size");
        }
      }
      catch (IOException e) {
        throw new ExInternal("Error converting InputStream into ByteArrayr", e);
      }

      // When data read, append to target
      byte[] lNewArray = new byte[lTargetArray.length+lReadSize];
      System.arraycopy(lTargetArray, 0, lNewArray, 0, lTargetArray.length);
      System.arraycopy(lReadBuffer, 0, lNewArray, lTargetArray.length, lReadSize);
      lTargetArray = lNewArray;

    }

    // Return target
    return lTargetArray;

  }

  /** Interigates java call stack returning method names as an array - not efficient use only for diagnostic code */
  public static final String[] getJavaStackMethods(int pMaxArraySize) {
    Exception x = new Exception();

    StringWriter sw = new StringWriter();
    x.printStackTrace(new PrintWriter(sw));
    StringTokenizer st = new StringTokenizer(sw.toString(),"\n");
    ArrayList al = new ArrayList();
    st.nextToken();
    st.nextToken();
    st.nextToken();
    int p1;
    String call;
    while (al.size()<pMaxArraySize && st.hasMoreTokens()) {
      call = st.nextToken().trim();
      st.nextToken();
      p1=call.indexOf(' ')+1;
      al.add(call.substring(p1,call.indexOf('(',p1)));
    }
     return (String[]) al.toArray(new String[0]);
  }

  /** Interigates java call stack returning class names as an array - not efficient use only for diagnostic code */
  public static final String[] getJavaStackClasses(int pMaxArraySize) {
    Exception x = new Exception();

    StringWriter sw = new StringWriter();
    x.printStackTrace(new PrintWriter(sw));
    StringTokenizer st = new StringTokenizer(sw.toString(),"\n");
    ArrayList al = new ArrayList();
    st.nextToken();
    st.nextToken();
    st.nextToken();
    int p1;
    String call;
    while (al.size()<pMaxArraySize && st.hasMoreTokens()) {
      call = st.nextToken().trim();
      st.nextToken();
      p1=call.indexOf(' ')+1;
      call=call.substring(p1,call.indexOf('(',p1));
      p1=call.lastIndexOf('.');
      al.add(call.substring(0,p1));
    }
     return (String[]) al.toArray(new String[0]);
  }

  public static final Integer toInt(String pString) {
    if(isNull(pString)) {
      return null;
    }
    if(pString.equals("unbounded")) {
      return INTEGER_MAX_VALUE;
    }
    return Integer.valueOf(pString);
  }

  public static final Integer nvlInteger(String pString, Integer pInteger) {
    if(isNull(pString)) {
      return pInteger;
    }
    if(pString.equals("unbounded")) {
      return INTEGER_MAX_VALUE;
    }
    return Integer.valueOf(pString);
  }

  public static final String toStr(int pInt) {
    return Integer.toString(pInt);
  }

  public static final boolean isCharacters(
    String pSourceString
  , char[] pCharSet
  , int pSubStringFrom
  , int pSubStringTo
  ) {
    return isCharacters(pSourceString.toCharArray(), pCharSet, pSubStringFrom, pSubStringTo);
  }
  public static final boolean isCharacters(
    char[] pSourceChars
  , char[] pCharSet
  , int pSubStringFrom
  , int pSubStringTo
  )
  throws ExInternal
  {
    // Internal validation
    if(pSourceChars==null || pCharSet==null) {
      throw new ExInternal("null value passed to isCharacters");
    }
    // Process string
    int i, j;
    CHAR_LOOP: for(i=pSubStringFrom; i<pSubStringTo; i++) {
      MAP_LOOP: for(j=0; j<pCharSet.length; j++) {
        if(pSourceChars[i]==pCharSet[j]) {
          continue CHAR_LOOP;
        }
      } // end MAP_LOOP
      return false;
    } // end CHAR_LOOP
    return true;
  }

  public String removeSpaces(String s) {
    StringTokenizer st = new StringTokenizer(s," ",false);
    String t="";
    while (st.hasMoreElements()) {
      t += st.nextElement();
    }
    return t;
  }

  /** Tests whether the passed string is a valid value
   * @param pTest the string to be tested
   * @return true if the string passed is not null or an empty string
   */
  public static boolean exists(String pTest) {
    if (pTest == null || pTest.length() == 0) {
      return false;
    }
    return true;
  }

  /**
  * Creates a string representation of the first argument in the
  * radix specified by the second argument.
  * <p>
  * If the radix is smaller than <code>Character.MIN_RADIX</code> or
  * larger than <code>Character.MAX_RADIX</code>, then the radix
  * <code>10</code> is used instead.
  * <p>
  * If the first argument is negative, the first element of the
  * result is the ASCII minus sign <code>'-'</code>
  * (<code>'&#92;u002d'</code>. If the first argument is not negative,
  * no sign character appears in the result.
  * <p>
  * The remaining characters of the result represent the magnitude of
  * the first argument. If the magnitude is zero, it is represented by
  * a single zero character <code>'0'</code>
  * (<code>'&#92;u0030'</code>); otherwise, the first character of the
  * representation of the magnitude will not be the zero character.
  * The following ASCII characters are used as digits:
  * <blockquote><pre>
  *   0123456789abcdefghijklmnopqrstuvwxyz
  * </pre></blockquote>
  * These are <tt>'&#92;u0030'</tt> through <tt>'&#92;u0039'</tt>
  * and <tt>'&#92;u0061'</tt> through <tt>'&#92;u007a'</tt>. If the
  * radix is <var>N</var>, then the first <var>N</var> of these
  * characters are used as radix-<var>N</var> digits in the order
  * shown. Thus, the digits for hexadecimal (radix 16) are
  * <tt>0123456789abcdef</tt>. If uppercase letters
  * are desired, the {@link java.lang.String#toUpperCase()} method
  * may be called on the result:
  * <blockquote><pre>
  * Long.toString(n, 16).toUpperCase()
  * </pre></blockquote>
  *
  * @param   i       a long.
  * @param   radix   the radix.
  * @return  a string representation of the argument in the specified radix.
  * @see     java.lang.Character#MAX_RADIX
  * @see     java.lang.Character#MIN_RADIX
  */
  public static String toAlphaString(long i/*, int radix*/) {

    final int radix = MAX_ALPHA_RADIX;

    //if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX){
    //  radix = 10;
    //}

    char[] buf = new char[65];
    int charPos = 64;
    boolean negative = (i < 0);

    if (!negative) {
      i = -i;
    }

    while (i <= -radix) {
      try {
        buf[charPos--] = ALPHA_DIGETS[(int)(-(i % radix))];
      }
      catch(IndexOutOfBoundsException x) { // JDEV HOTSPOT SERVER BUG - Debug code
        System.out.println("= "+(int)(-i));
        throw x;
      }
      i = i / radix;
    }

    buf[charPos] = ALPHA_DIGETS[(int)(-i)];

    if (negative) {
      buf[--charPos] = '-';
    }

	  return new String(buf, charPos, (65 - charPos));
  }


  /**
   * Creates a string representation of the number passed
   *
   * @param pNumber Integer in
   * @return String out in the format: a, b, c, ... x, y, z, aa, ab, ac, ... ax, ay, az, ba, bb, bc, ...
   */
  public static String getAlphaNumbering(int pNumber) {
    // Allow negative numbers as the old function did (invert, get chars, add minus symbol)
    boolean lNegative = false;
    if (pNumber < 0) {
      lNegative = true;
      pNumber = -pNumber;
    }

    // Generate char array for final string
    char[] buf = new char[(pNumber / lAlphabetChars.length)+1];

    // Loop through, building up the string backwards as we go
    int i = 0;
    do {
      buf[buf.length-i-1] = lAlphabetChars[pNumber%lAlphabetChars.length];
      if (pNumber < lAlphabetChars.length) {
        break;
      }
      i++;
      pNumber = (pNumber / lAlphabetChars.length)-1;
    }
    while (pNumber >= 0);

    // Return result as a String, adding minus symbol when needed
    if (lNegative) {
      return "-" + new String(buf);
    }
    else {
      return new String(buf);
    }
  }

   /**
    * Returns the value of the specified parameter from the <code>Map</code>.
    *
    * <p>The map has been obtained via an <code>HttpServletRequest</code> and
    * should contain <code>String[]</code> values. However, the OC4J (9.0.3)
    * implementation has a bug and may return <code>String</code> for single
    * value parameters. - GAW
    *
    * @param paramsMap the map of request parameters
    * @param name the name of the parameter whose value is to be obtained.
    * @return the value of the parameter.
    */
  public static String getParamMapValue(Map paramsMap, String name)
  {
      Object valueObj = paramsMap.get(name);
      if (valueObj instanceof String[])
         return ((String[])valueObj)[0];
      else
         return (String)valueObj;
  }


  public static final boolean isWhiteSpace(String pString) {
    char[] val = pString.toCharArray();
    int len = val.length; // limit use of opcode (JVM)
    int st=0;
    while (st < len) {
      if(val[st] > ' ') {
        return false;
      }
    st++;
    }
    return true;
  }

  public static String sanitiseStringForOutput(String pString, int sanitiseType) {
    String lSanitisedString = pString;
    if (sanitiseType == SANITISE_ALERTS) { //Escape for alerts (adding in backslashes)
     lSanitisedString = lSanitisedString.replaceAll("(\r|\n){1,2}", "\\\\n");
     lSanitisedString = lSanitisedString.replaceAll("'", "\\\\'");
     lSanitisedString = lSanitisedString.replaceAll("\"", "\\\\\"");
     lSanitisedString = lSanitisedString.replaceAll("\\\\r","\\\\n");
    }
    else if (sanitiseType == SANITISE_HINTS) { //Escape for hints (Add backslashes except for newlines where it's a br tag)
      lSanitisedString = lSanitisedString.replaceAll("\\\\", "\\\\\\\\");
      lSanitisedString = lSanitisedString.replaceAll("(\r|\n){1,2}", "<br />");
      lSanitisedString = lSanitisedString.replaceAll("'", "\\\\'");
      lSanitisedString = lSanitisedString.replaceAll("\"", "\\\\\"");
    }
    else if (sanitiseType == SANITISE_HTMLENTITIES) {
      lSanitisedString = lSanitisedString.replaceAll("&", "&amp;");
      lSanitisedString = lSanitisedString.replaceAll("<", "&lt;");
      lSanitisedString = lSanitisedString.replaceAll(">", "&gt;");
      lSanitisedString = lSanitisedString.replaceAll("'", "&apos;");
      lSanitisedString = lSanitisedString.replaceAll("\"", "&quot;");
    }
    else if (sanitiseType == SANITISE_ALT) {
      lSanitisedString = lSanitisedString.replaceAll("<br( */?)>", " ");
      lSanitisedString = lSanitisedString.replaceAll("\n", " ");
    }

    return lSanitisedString;
  }

  public static boolean isValidChar(char[] chars, int[] positions, int min, int max) {
    for (int i=0; i<positions.length; i++) {
      if (positions[i]+1>chars.length) {
        return false;
      }
      if (!(chars[positions[i]] > min && chars[positions[i]] < max)) {
        return false;
      }
    }
    return true;
  }

  public static String formatDate(Date lDate, String lFormatMask) {
    SimpleDateFormat lDateFormat = new SimpleDateFormat(lFormatMask);
    return lDateFormat.format(lDate);
  }

  public static String formatDateXML(Date lDate) {
    return formatDate(lDate, XML_DATE_FORMAT);
  }

  public static String formatDatetimeXML(Date lDate) {
    return formatDate(lDate, XML_DATETIME_FORMAT); //  XMLUtil.XML_DATETIME_FORMAT
  }

  /* Converts from the format from user input DD-MON-YYYY or other into YYYY-MM-DD*/
  public static String dateTimeReader(String pDate, String pDateFormat, String pXsType) {
    if (pXsType.equals("xs:time")) {
      return pDate;
    } else if (pXsType.equals("xs:dateTime")) {
      // TODO: currently only works with the pattern DD-MON-YYYY hh:mm:ss converting to YYYY-MM-DDThh:mm:ss
      char[] date = pDate.toCharArray();
      if (date.length < 12 && date.length > 20) {
        return pDate;
      }
      char[] day = new char[] {' ',' '};
      char[] month = new char[] {' ',' '};
      char[] year = new char[] {' ',' ',' ',' '};
      int diff = 0; // difference in length for latter elements depending on the
      if (isValidChar(date,new int[] {0,1},47,58)) {
        day[0] = date[0];
        day[1] = date[1];
      } else {
        // in case of 9-02-2003
        if (isValidChar(date,new int[] {0},47,58) && !(isValidChar(date,new int[] {1},64,91) || isValidChar(date,new int[] {1},96,123))) {
          day[0] = '0';
          day[1] = date[0];
          diff--;
        } else {
          return pDate;
        }
      }
      if (isValidChar(date,new int[] {3+diff},47,58)) {
        if (isValidChar(date,new int[] {4+diff},47,58)) {
          month[0] = date[3+diff];
          month[1] = date[4+diff];
        } else {
          month[0] = '0';
          month[1] = date[3+diff];
          diff--;
        }
        diff--;
      } else if (
        (isValidChar(date,new int[] {3+diff},64,91) || isValidChar(date,new int[] {3+diff},96,123))
        && (isValidChar(date,new int[] {4+diff},64,91) || isValidChar(date,new int[] {4+diff},96,123))
        && (isValidChar(date,new int[] {5+diff},64,91) || isValidChar(date,new int[] {5+diff},96,123))
      ) {

        String sb = new String((""+date[3+diff]+date[4+diff]+date[5+diff]).toUpperCase());
        int count;
        boolean found = true;
        for (count=0; count<mons.length; count++) {
          String m = mons[count];
          if (sb.equals(m)) {
            int p = (count+1) % 12;
            if (p==0) {
              p=12;
            }
            if (p < 10) {
              month[0] = '0';
              month[1] = (char)(p+48);
            } else {
              month[0] = '1';
              p = p-10;
              month[1] = (char)(p+48);
            }
            found = true;
            break;
          }
        }
        if (!found) {
          return pDate;
        }
      } else {
        return pDate;
      }

      if (isValidChar(date,new int[] {7+diff,8+diff,9+diff,10+diff},47,58)) {
        year[0] = date[7+diff];
        year[1] = date[8+diff];
        year[2] = date[9+diff];
        year[3] = date[10+diff];
      } else {
        return pDate;
      }

      int dateSize = date.length;

      char[] hour = new char[] {'0','0'};
      char[] min = new char[] {'0','0'};
      char[] sec = new char[] {'0','0'};

      if (dateSize > 12) {
        if (isValidChar(date,new int[] {12+diff,13+diff},47,58)) {
          hour[0] = date[12+diff];
          hour[1] = date[13+diff];
        }
        if (dateSize > 14) {
          if (isValidChar(date,new int[] {15+diff,16+diff},47,58)) {
            min[0] = date[15+diff];
            min[1] = date[16+diff];
          }
          if (dateSize > 16) {
            if (isValidChar(date,new int[] {18+diff,19+diff},47,58)) {
              sec[0] = date[18+diff];
              sec[1] = date[19+diff];
            }
          }
        }
      }

      return ""+year[0]+year[1]+year[2]+year[3]+'-'+month[0]+month[1]+'-'+day[0]+day[1]+'T'+hour[0]+hour[1]+':'+min[0]+min[1]+':'+sec[0]+sec[1];
    } else if (pXsType.equals("xs:date")) {
      // TODO: currently only works with the pattern DD-MON-YYYY
      char[] date = pDate.toCharArray();
      char[] day = new char[] {' ',' '};
      char[] month = new char[] {' ',' '};
      char[] year = new char[] {' ',' ',' ',' '};
      int upos = 1; // day = 1, month = 2, validate month = 3, year = 4
      int apos = 0; // array positions
      int cpos = 0; // actual positions in current section DELETE
      int sep = 0; // Number of seperators found
      StringBuffer monthSB = new StringBuffer();
      boolean valid = true;
      if (date.length < 6) {
        valid = false;
      }
      char monthFormat = 'N';
      for (int i=0; i<date.length && valid; i++) {
        char l = date[i];
        switch(upos) {
          case 1: // DAY
            // if a digit add to the day array
            if (l > 47 && l < 58) {
              day[apos] = (char)l;
              apos++;
              cpos++;
            } else {
              // if a seperator or letter move to month
              if (cpos > 0) {
                day[1]=day[0];
                day[0]='0';
                upos++;
                i--;
                apos=0;
                cpos=0;
              } else {
                valid = false;
                break;
              }
            }
            // if going into the 3 char then move to month
            if (apos >= 2) {
              upos++;
              apos=0;
              cpos=0;
            }
            break;
          case 2: // MONTH
            // if a digit add to the month array
            if (l > 47 && l < 58) {
              // if the month has started off as chars
              if (monthFormat == 'C') {
                // validate the character month
                upos++;
                i--;
                break;
              } else {
                // add the digit to the month section
                monthFormat = 'D';
                month[cpos] = (char)l;
                apos++;
                cpos++;
             }
            } else if ((l > 64 && l < 91) || (l > 96 && l < 123)) {
              if (monthFormat == 'D') {
                valid = false;
                break;
              } else {
                monthFormat = 'C';
                monthSB.append((char)l);
                apos++;
              }
            } else {
              sep++;
              apos++;
            }
            if (sep > 1 || cpos >= 2) {
              upos++;
            }
            break;
          case 3: // Validate character month
              if (monthFormat == 'N') {
                valid = false;
                break;
              } else if (monthFormat == 'C') {
                // validate char made month
                String sb = new String(monthSB.toString().toUpperCase());
                int count;
                for (count=0; count<mons.length; count++) {
                  String m = mons[count];
                  if (sb.equals(m)) {
                    int p = (count+1) % 12;
                    if (p==0) {
                      p=12;
                    }
                    if (p < 10) {
                      month[0] = '0';
                      month[1] = (char)(p+48);
                    } else {
                      month[0] = '1';
                      p = p-10;
                      month[1] = (char)(p+48);
                    }
                    break;
                  }
                }
                if (count==mons.length) {
                  valid = false;
                  break;
                }
              } else if (monthFormat == 'D' && cpos == 1) {
                month[1]=month[0];
                month[0]='0';
              }
              upos++;
              i--;
              apos=0;
              cpos=0;
            break;
          case 4: // YEAR
            if (l > 47 && l < 58) {
              year[cpos] = (char)l;
              apos++;
              cpos++;
            } else if ((l > 64 && l < 91) || (l > 96 && l < 123)) {
              valid = false;
              break;
            } else {
              apos++;
            }
            if (cpos >= 4) {
              upos++;
            }
            break;
        }

      }
      if (valid) {
        if (cpos == 2) {
          int y = Integer.parseInt(""+year[0]+year[1]);
          year[2] = year[0];
          year[3] = year[1];
          if (y > 50) {
            year[0]='1';
            year[1]='9';
          } else {
            year[0]='2';
            year[1]='0';
          }
        } else if (cpos == 1) {
          return pDate;
        }
        return ""+year[0]+year[1]+year[2]+year[3]+"-"+month[0]+month[1]+"-"+day[0]+day[1];
      } else {
        return pDate;
      }
    } else {
      return pDate;
    }
  }

  public static void testDateTime(String[] dates, String type, String format) {
    System.out.print(type+" processing\n---------------\n");
    int errors = 0;
    for (int j=0; j<dates.length/2; j++) {
      String date = dateTimeReader(dates[j*2],format,type);
      String orgDate = dates[j*2];
      String resDate = dates[j*2+1];
      boolean datesEqual = date.equals(resDate);
      System.out.println("["+orgDate+"]"+((orgDate.length()>8)?"\t":"\t\t")+"["+date+"]"+((date.length()>8)?"\t":"\t\t")+datesEqual);
      if (!datesEqual) {
        errors++;
      }
    }
    System.out.print("----------------\nNumber of errors: "+errors+"\n");
  }

  public static int createBoxTextOutput(StringBuffer output, int pWidth) {
    StringTokenizer tokens = new StringTokenizer(output.toString(),"\n ",true);
    output.delete(0,output.length());
    int lWidth = 0;
    int rows = 1;
    while(tokens.hasMoreTokens()) {
      String next = tokens.nextToken();
      // put the return carriage straight in
      if (next.equals("\n")) {
        lWidth = 0;
        rows++;
        output.append('\n');
        continue;
      }
      int tokenLength = next.length();
      // deal with the token which is bigger than the width
      if (tokenLength + lWidth > pWidth) {
        lWidth = 0;
        rows++;
        output.append('\n');
        while (tokenLength > pWidth) {
          // the token is bigger than one rows so split into parts
          String part = next.substring(0,pWidth);
          output.append(part+'\n');
          next = next.substring(pWidth,tokenLength);
          tokenLength -= pWidth;
          rows++;
        }
        if (tokenLength != 0 && !next.equals(" ")) {
          lWidth += tokenLength;
          output.append(next);
        }
      } else {
        // add a token into the row
        if (!next.equals(" ") || (next.equals(" ") && lWidth > 0)) {
          lWidth += tokenLength;
          output.append(next);
        }
      }
    }
    return rows;
  }

  public static void replaceNewlineWithBreak(String textValue, DOM newAttachPoint) {
    StringTokenizer tokens = new StringTokenizer(textValue,"\n\r",true);
    boolean brokeLast = false;
    while (tokens.hasMoreElements()) {
      String token = tokens.nextToken();
      if (token.equals("\n")) {
        newAttachPoint.addElem("br");
        brokeLast = true;
      } else if (token.equals("\r")) {
        if (brokeLast != true) {
          newAttachPoint.addElem("br");
        } else {
          brokeLast = false;
        }
      } else {
        DOM.createUnconnectedText(token).moveToParent(newAttachPoint);
        brokeLast = false;
      }
    }
  }

  /**
   * Determines if the address host address specified is a valid IP4 IP address, of
   * the form:
   *
   *     d.d.d.d
   *     d.d.d
   *     d.d
   *     d
   *
   * @param inetAddress the host address to be checked as an IP address
   * @return true if the host address specified is a valid IP address, false otherwise.
   */
  public static boolean isInet4IPAddress(String inetAddress) {
    StringTokenizer sTok = new StringTokenizer(inetAddress, ".");
    boolean isIPAddress = inetAddress.length() > 0;
    while ( sTok.hasMoreTokens() && isIPAddress ) {
      String addressPart = sTok.nextToken();
      for (int c=0; c < addressPart.length() && isIPAddress; c++) {
        char addressChar = addressPart.charAt(c);
        isIPAddress = addressChar >= '0' && addressChar <= '9'; // isNumeric? ...
      }
    }

    return isIPAddress;
  }

  public static final String getHostIP() {
    if(gHostIP == null) {
      // Get IP Address
      InetAddress addr;
      try {
        addr = InetAddress.getLocalHost();
      }
      catch (UnknownHostException x) {
        throw new ExInternal("Cannot get getLocalHost", x);
      }
      gHostIP = addr.getHostAddress();
    }
    return gHostIP;
  }

  public static final String stringNormaliseEndOfLine(String pInputString, String pOutputEOLDelimiter) {
    return stringNormaliseEndOfLineStringBuffer(pInputString, pOutputEOLDelimiter).toString();
  }

  public static final StringBuffer stringNormaliseEndOfLineStringBuffer(String pInputString, String pOutputEOLDelimiter) {
    StringBuffer lStringBuffer = new StringBuffer(pInputString.length());
    StringTokenizer lStringTokenizer = new StringTokenizer(pInputString, "\r\n", true);
    boolean lCR = false;
    while(lStringTokenizer.hasMoreTokens()) {
      String lString = lStringTokenizer.nextToken();
      if("\r".equals(lString)) {
        if(lCR) {
          lStringBuffer.append(pOutputEOLDelimiter);
        }
        lCR=true;
      }
      else if("\n".equals(lString)) {
        lStringBuffer.append(pOutputEOLDelimiter);
        lCR=false;
      }
      else {
        if(lCR) {
          lStringBuffer.append(pOutputEOLDelimiter);
        }
        lCR=false;
        lStringBuffer.append(lString);
      }
    }
    if(lCR) {
      lStringBuffer.append(pOutputEOLDelimiter);
    }
    return lStringBuffer;
  }

  private static final Pattern regexpParse(String pRegExp, int pFlags)
  throws ExInternal {
    try {
      return Pattern.compile(pRegExp, pFlags);
    }
    catch (PatternSyntaxException e) {
      throw new ExInternal("Regexp syntax error", e);
    }
    catch (IllegalArgumentException e) {
      throw new ExInternal("Illegal flags value passed to Pattern.compile", e);
    }
  }
//
  public static final Pattern regexpParse(String pRegExp) {
    return regexpParse(pRegExp, 0);
  }

  public static final Pattern regexpParseIgnoreCase(String pRegExp) {
    return regexpParse(pRegExp, Pattern.CASE_INSENSITIVE);
  }

  public static final String[] regexpMatch (String pRegExp, String pValue) {
    Pattern lPattern = regexpParse(pRegExp);
    Matcher lMatcher = lPattern.matcher(pValue);

    // NB: find() doesn't require the entire input string to match regexp
    // i.e. it functions as regexp match instring rather than regexp equals
    // matches() requires a complete match
    // lookingAt() requires that the start of the string matches
    if (!lMatcher.find()) {
      return null; // not matched
    }

    // Group 0 is capturing group () - i.e. entire match and is not included
    // in groupCount()
    int lGroupCount = lMatcher.groupCount() + 1;
    String[] lResults = new String[lGroupCount];
    for (int g = 0; g < lGroupCount; g++) {
      lResults[g] = lMatcher.group(g);
    }

    return lResults;
  }

  public static final boolean regexpMatches (String pRegExp, String pValue) {
    return Pattern.matches(pRegExp, pValue);
  }

  public static final boolean isInteger(String pString) {
    try {
      Integer.parseInt(pString);
      return true;
    }
    catch(NumberFormatException e) {
      return false;
    }
  }

  public static final double roundDouble(double pDouble, int pPlaces) {
    return Math.round(pDouble * Math.pow(10, (double) pPlaces)) / Math.pow(10,(double) pPlaces);
  }

  public static final int toInteger(String pString) {
    try {
      return Integer.parseInt(pString);
    }
    catch(NumberFormatException e) {
      return Integer.MIN_VALUE;
    }
  }

  public static final String obfuscateValue(String str) {
    return str.replaceAll(".", String.valueOf((char)160));
  }

  public static String md5 (String pString) {
    try {
      MessageDigest lMessageDigest = MessageDigest.getInstance("MD5");
      byte[] lByteArray = lMessageDigest.digest(pString.getBytes());
      return new BigInteger(1, lByteArray).toString(16);
    }
    catch (NoSuchAlgorithmException ex) {
      throw new ExInternal("MD5 not implemented", ex);
    }
  }

  public static KeyPair generateRSAKeys(){
    KeyPairGenerator lKeyGen;
    try {
      lKeyGen = KeyPairGenerator.getInstance("RSA");
    }
    catch (NoSuchAlgorithmException e) {
      FoxLogger.getLogger().error("Failed to generate RSA keypair", e);
      throw new ExInternal(e.getMessage(), e);
    }
    lKeyGen.initialize(2048); //Hard coded key size
    KeyPair lKeys = lKeyGen.generateKeyPair();
    return lKeys;
  }
  public static boolean checkRSAKeyPair(PrivateKey pPrivate, PublicKey pPublic){
    byte[] lTestData = {'H','e','l','l','o',' ','W','o','r','l','d'};
    byte[] lEncrypted, lDecrypted;
    try {
      lEncrypted = encryptBytes(lTestData, pPublic, "RSA/ECB/PKCS1Padding");
      lDecrypted = decryptBytes(lEncrypted, pPrivate, "RSA/ECB/PKCS1Padding");
      if(Arrays.equals(lDecrypted,lTestData)){
        return true;
      }
      else {
        return false;
      }
    }
    catch (Exception e) {
      return false;
    }
  }
  public static PrivateKey decodePrivateKey(String pKey, String pAlgorithm){
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance(pAlgorithm);
      EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decodeBASE64(pKey));
      return keyFactory.generatePrivate(privateKeySpec);
    }
    catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      FoxLogger.getLogger().error("Failed to decode private key", e);
      throw new ExInternal(e.getMessage(), e);
    }
  }
  public static PublicKey decodePublicKey(String pKey, String pAlgorithm){
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance(pAlgorithm);
      EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodeBASE64(pKey));
      return keyFactory.generatePublic(publicKeySpec);
    }
    catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      FoxLogger.getLogger().error("Failed to decode public key", e);
      throw new ExInternal(e.getMessage(), e);
    }
  }
  //These base 64 functions are Sun JRE specific classes, beware!
  public static String encodeBASE64(byte[] pBytes)
  {
    BASE64Encoder b64 = new BASE64Encoder();
    return b64.encode(pBytes);
  }
  public static byte[] decodeBASE64(String pEncoded)
  {
    BASE64Decoder b64 = new BASE64Decoder();
    try {
      return b64.decodeBuffer(pEncoded);
    }
    catch (IOException e) {
      return null;
    }
  }
  public static void encryptRSAStream(InputStream pIn, OutputStream pOut, PublicKey pKey)
  throws Exception
  {
    byte[] lBuffer = new byte[200]; //Hard coded to 200 for a 2048 bit(256 byte) RSA key
    int numRead = 0;

    while ((numRead = pIn.read(lBuffer)) != -1) {
      pOut.write(encryptBytes(trimBytes(lBuffer, numRead), pKey, "RSA/ECB/PKCS1Padding"));
    }

    if (pOut != null) {
      pOut.close();
    }
  }
  public static void decryptRSAStream(InputStream pIn, OutputStream pOut, PrivateKey pKey)
  throws Exception
  {
    byte[] lBuffer = new byte[256]; //Hard coded to 256 for a 2048 bit (256 byte) RSA key
    int numRead = 0;

    while ((numRead = pIn.read(lBuffer)) != -1) {
      pOut.write(decryptBytes(trimBytes(lBuffer, numRead), pKey, "RSA/ECB/PKCS1Padding"));
    }

    if (pOut != null) {
      pOut.close();
    }
  }
  public static void encryptAESStream(InputStream pIn, OutputStream pOut, Key pKey, AlgorithmParameterSpec pParamSpec)
  throws Exception
  {
    byte[] lBuffer = new byte[11]; //Hard coded for now, 256 bit (16 byte) AES key
    int numRead = 0;

    while ((numRead = pIn.read(lBuffer)) != -1) {
      pOut.write(encryptBytes(trimBytes(lBuffer, numRead), pKey, "AES/CBC/PKCS5Padding", pParamSpec));
    }

    if (pOut != null) {
      pOut.close();
    }
  }
  public static void decryptAESStream(InputStream pIn, OutputStream pOut, Key pKey, AlgorithmParameterSpec pParamSpec)
  throws Exception {
    byte[] lBuffer = new byte[16]; //Hard coded for now, 256 bit (16 byte) AES key
    int numRead = 0;

    while ((numRead = pIn.read(lBuffer)) != -1) {
      pOut.write(decryptBytes(trimBytes(lBuffer, numRead), pKey, "AES/CBC/PKCS5Padding", pParamSpec));
    }

    if (pOut != null) {
      pOut.close();
    }
  }
  //These encrypt/decrypt functions can only encrypt/decrypt ((RSA keysize) / 8)-padding bytes at a time
  public static byte[] encryptBytes (byte[] pBytes, Key key, String pAlgorithm)
  throws Exception {
    byte[] cipherText = null;
    Security.addProvider(new BouncyCastleProvider());
    Cipher cipher = Cipher.getInstance(pAlgorithm);
    cipher.init(Cipher.ENCRYPT_MODE, key);
    cipherText = cipher.doFinal(pBytes);
    return cipherText;
  }
  public static byte[] decryptBytes (byte[] pBytes, Key key, String pAlgorithm)
  throws Exception {
    byte[] dectyptedText = null;
    Security.addProvider(new BouncyCastleProvider());
    Cipher cipher = Cipher.getInstance(pAlgorithm);
    cipher.init(Cipher.DECRYPT_MODE, key);
    dectyptedText = cipher.doFinal(pBytes);
    return dectyptedText;
  }
  //These encrypt/decrypt functions work with AES
  public static byte[] encryptBytes (byte[] pBytes, Key key, String pAlgorithm, AlgorithmParameterSpec pParamSpec)
  throws Exception {
    byte[] cipherText = null;
    Security.addProvider(new BouncyCastleProvider());
    Cipher cipher = Cipher.getInstance(pAlgorithm);
    cipher.init(Cipher.ENCRYPT_MODE, key, pParamSpec);
    cipherText = cipher.doFinal(pBytes);
    return cipherText;
  }
  public static byte[] decryptBytes (byte[] pBytes, Key key, String pAlgorithm, AlgorithmParameterSpec pParamSpec)
  throws Exception {
    byte[] dectyptedText = null;
    Security.addProvider(new BouncyCastleProvider());
    Cipher cipher = Cipher.getInstance(pAlgorithm);
    cipher.init(Cipher.DECRYPT_MODE, key, pParamSpec);
    dectyptedText = cipher.doFinal(pBytes);
    return dectyptedText;
  }
  //Trim a byte array
  public static byte[] trimBytes (byte[] pBytes, int pLength) {
    byte[] lNewBuffer = null;
    if (pBytes.length == pLength) {
      lNewBuffer = pBytes;
    }
    else {
      lNewBuffer = new byte[pLength];
      System.arraycopy(pBytes, 0, lNewBuffer, 0, pLength);
    }
    return lNewBuffer;
  }

  //Because the normal initcap function lowercases the string first
  private static String upperFirstchar( String pString) {
    return pString.substring(0,1).toUpperCase() + pString.substring(1);
  }

  public static void writeFile (File pFile, InputStream pInputStream)
  throws IOException {
    FileOutputStream destination = new FileOutputStream(pFile);
    try {
      IOUtil.transfer(pInputStream, destination);
    }
    finally {
      destination.close();
    }
  }

  public static void writeFile (File pFile, Reader pReader)
  throws IOException {
    FileWriter lWriter = null;
    try {
      lWriter = new FileWriter(pFile);
      int c;
      while ((c = pReader.read()) != -1) {
          lWriter.write(c);
      }
    }
    finally {
      lWriter.close();
    }
  }

  public static void writeFile (File pFile, String pData)
  throws IOException {
    LineNumberReader lLineNumberReader = new LineNumberReader(new StringReader(pData));
    FileWriter lWriter = null;
    PrintWriter lPrintWriter;
    String lLineBuffer;
    try {
      lWriter = new FileWriter(pFile);
      lPrintWriter = new PrintWriter(lWriter);
      lLineBuffer = lLineNumberReader.readLine();
      while(lLineBuffer!=null) {
        lPrintWriter.println(lLineBuffer);
        lLineBuffer = lLineNumberReader.readLine();
      }
    }
    finally {
      IOUtil.close(lWriter);
    }
  }

  public static File createDir (String pDirName, File pParentFolder)
  throws IOException {
    File lNewDir = new File(pParentFolder, pDirName);
    lNewDir.delete();
    lNewDir.mkdir();
    return lNewDir;
  }

  public static File createTempDir (String pDirName, String pTempDirSuffix)
  throws IOException {
    File lNewDir = File.createTempFile(pDirName, pTempDirSuffix);
    lNewDir.delete();
    lNewDir.mkdir();
    return lNewDir;
  }

  public static void copyDir (File pSourceDir, File pDestinationDir) {
    if (pSourceDir.isDirectory()) {
      if (!pDestinationDir.exists()) {
        pDestinationDir.mkdir();
      }
      String[] children = pSourceDir.list();
      for (int i=0; i<children.length; i++) {
        copyDir(new File(pSourceDir, children[i]), new File(pDestinationDir, children[i]));
      }
    }
    else {
      copyFile(pSourceDir, pDestinationDir);
    }
  }

  public static boolean copyFile (File pSource, File pDestination) {
    InputStream in;
    OutputStream out;
    try {
      in = new FileInputStream(pSource);
      out = new FileOutputStream(pDestination);
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      in.close();
      out.close();
      return true;
    }
    catch (IOException e) {
      return false;
    }
  }

  public static boolean deleteDir (File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
    }
    return dir.delete();
  }

  /**
   * Gets the index in a string of the closing parenthesis.
   * e.g. if you have a string  "test( the ( quick ( brown ( fox ) jumped ) over ) the ) lazy dog"
   *      and you want only the text inside test() you would remove "test(" and pass the string
   *      as argument. The index returned is then of the matching ). You can then use this in
   *      a substring (but remember to add the offset of the length of "test(" to the result
   * @param pStr a String with the open parenthesis you wish to match removed
   * @return the index of the closing parenthesis
   * @throws ExInternal
   */
  public static int getCloseParenthesisIndex(String pStr)
  throws ExInternal {
    return getCloseParenthesisIndex(pStr, 1);
  }

  public static int getCloseParenthesisIndex(String pStr, int pDepth)
  throws ExInternal {
    int openIndex = pStr.indexOf("(");
    int closeIndex = pStr.indexOf(")");
    if(closeIndex==-1) {
      throw new RuntimeException("mismatched parenthesis");
    } else if (openIndex!=-1 && openIndex < closeIndex) {
      // Nested level, get the index after the open and proceed with the remaining string
      return openIndex+1 + getCloseParenthesisIndex(pStr.substring(openIndex+1),pDepth+1);
    } else if (pDepth == 1) {
      // We are at our top level, return our accumulated index
      return closeIndex;
    } else {
      // Found a close before an open or failed to find an open, get the index after the close and proceed with the remaining string
      return closeIndex+1 + getCloseParenthesisIndex(pStr.substring(closeIndex+1),pDepth-1);
    }
  }

  public static boolean checkURLStatus(String pURL) {
    URL url = null;
    HttpURLConnection conn = null;
    try {
      url = new URL(pURL);
      conn = (HttpURLConnection)url.openConnection();
      // Commented these out for java 1.4, when moving to 1.6 comment these in again
      //conn.setReadTimeout(200);
      //conn.setConnectTimeout(200);
      conn.connect();
      return true;
    }
    catch (MalformedURLException e) {
      return false;
    }
    catch (IOException e) {
      return false;
    }
    finally {
      conn.disconnect();
      conn = null;
      url = null;
    }
  }

} // class XFUtil

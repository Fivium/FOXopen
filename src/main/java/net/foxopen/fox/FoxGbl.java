package net.foxopen.fox;


// TODO - These formats should be moved somewhere else and agreed upon. Hard to do with so much old code around
//        StringFormatter has kind of taken over and has its own internal consts, though not the same.
//        Would be nice to have the same set of masks for XML, Oracle, Serialiser Display...
@Deprecated
public interface FoxGbl {
   /**
    *  Default date format for xml
    */
   public final static String FOX_DATE_FORMAT = "yyyy-MM-dd";
   /**
    *  Default time format for xml
    */
   public final static String FOX_JAVA_TIME_FORMAT = "HH:mm:ss";
   /**
    *  Default date/time format for xml
    */
   public final static String FOX_JAVA_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

   public final static String FOX_DATE_XML_FORMAT = "YYYY-MM-DD";
   public final static String FOX_TIME_XML_FORMAT = "hh:mm:ss";
}

package net.foxopen.fox.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A handy utility class that contains some well-used sizes and formatting
 * helpers.
 */
public class SizeUtil
{
   public static long KBYTES     = 1024;
   public static long MEGA_BYTES = KBYTES * KBYTES;
   public static long GIGA_BYTES = MEGA_BYTES * KBYTES;
   public static long TERA_BYTES = GIGA_BYTES * KBYTES;

   public static long SECONDS_IN_MS = 1000;
   public static long MINUTES_IN_MS = 60*SECONDS_IN_MS;
   public static long HOURS_IN_MS   = 60*MINUTES_IN_MS;
   public static long DAYS_IN_MS    = 24*HOURS_IN_MS;
   public static long WEEKS_IN_MS   = 7*DAYS_IN_MS;
   public static long YEARS_IN_MS   = 52*WEEKS_IN_MS;

   /**
    * Returns a short string description of the size, in bytes, specified.
    *
    * @param sizeInBytes the number of bytes whose size if to be described.
    * @param formatSpec the <code>DecimalFormal</code> format specification.
    * @return a short-hand string description of the size in bytes.
    * @see java.text.DateFormat
    */
   public static String getBytesSpecificationDescription(double sizeInBytes,
                                                         String formatSpec)
   {
      DecimalFormat df = new DecimalFormat(formatSpec);
      df.setDecimalSeparatorAlwaysShown(false);
      double teraBytes = sizeInBytes / TERA_BYTES;
      if (teraBytes >= 1.0d)
      {
         return df.format(teraBytes)+" TB";
      }

      double gigaBytes = sizeInBytes / GIGA_BYTES;
      if (gigaBytes >= 1.0d)
      {
         return df.format(gigaBytes)+" GB";
      }

      double megaBytes = sizeInBytes / MEGA_BYTES;
      if (megaBytes >= 1.0d)
      {
         return df.format(megaBytes)+" MB";
      }

      double kBytes = sizeInBytes / KBYTES;
      if (kBytes >= 1.0d)
      {
         return df.format(kBytes)+" KB";
      }

      return ((long)sizeInBytes)+" Bytes";
   }

   /**
    * Returns a short string description of the size, in bytes, specified.
    *
    * @param sizeInBytes the number of bytes whose size if to be described.
    * @return a short-hand string description of the size in bytes.
    */
   public static String getBytesSpecificationDescription(double sizeInBytes)
   {
      return getBytesSpecificationDescription(sizeInBytes, "#.##");
   }

   /**
    * Returns a short string description of the elapsed time, in bytes, specified.
    *
    * @param timeInMS the number of bytes whose size if to be described.
    * @param formatSpec the <code>DecimalFormal</code> format specification.
    * @return a short-hand string description of the size in bytes.
    * @see java.text.DateFormat
    */
   public static String getElapsedTimeSpecificationDescription(long timeInMS,
                                                               String formatSpec)
   {
      StringBuffer sBuf = new StringBuffer();
      NumberFormat df = new DecimalFormat(formatSpec);
      long years = timeInMS / YEARS_IN_MS;
      if (years > 0)
      {
         sBuf.append(years).append(years > 1 ? " years" : " year");
      }

      long weeks = timeInMS % YEARS_IN_MS / WEEKS_IN_MS;
      if (weeks > 0)
      {
         sBuf.append(years > 0 ? ", " : " ").append(weeks).append(weeks > 1 ? " weeks" : " week");
      }

      long days = (timeInMS-(years*YEARS_IN_MS)-(weeks*WEEKS_IN_MS)) / DAYS_IN_MS;
      if (days > 0)
      {
         sBuf.append(years > 0 || weeks > 0 ? ", " : " ").append(days).append(days > 1 ? " days" : " day");
      }

      long hours = (timeInMS-(years*YEARS_IN_MS)-(weeks*WEEKS_IN_MS)-(days*DAYS_IN_MS)) / HOURS_IN_MS;
      if (hours > 0)
      {
         sBuf.append(years > 0 || weeks > 0 || days > 0 ? ", " : " ").append(hours).append(hours > 1 ? " hours" : " hour");
      }

      long minutes = (timeInMS-(years*YEARS_IN_MS)-(weeks*WEEKS_IN_MS)-(days*DAYS_IN_MS)-(hours*HOURS_IN_MS)) / MINUTES_IN_MS;
      if (minutes > 0)
      {
         sBuf.append(years > 0 || weeks > 0 || days > 0 || hours > 0 ? ", " : " ").append(minutes).append(minutes > 1 ? " minutes" : " minute");
      }

      long secs = (timeInMS-(years*YEARS_IN_MS)-(weeks*WEEKS_IN_MS)-(days*DAYS_IN_MS)-(hours*HOURS_IN_MS)-(minutes*MINUTES_IN_MS)) / SECONDS_IN_MS;
      if (secs > 0)
      {
         sBuf.append(years > 0 || weeks > 0 || days > 0 || hours > 0 || minutes > 0 ? ", " : " ").append(secs).append(secs > 1 ? " secs" : " sec");
      }

      long ms = timeInMS-(years*YEARS_IN_MS)-(weeks*WEEKS_IN_MS)-(days*DAYS_IN_MS)-(hours*HOURS_IN_MS)-(minutes*MINUTES_IN_MS)-(secs*SECONDS_IN_MS);
      if (ms > 0)
      {
         sBuf.append(years > 0 || weeks > 0 || days > 0 || hours > 0 || minutes > 0 || secs > 0 ? ", " : " ").append(ms).append(" ms");
      }

      return sBuf.toString();
   }

   /**
    * Returns a short string description of the size, in bytes, specified.
    *
    * @param sizeInBytes the number of bytes whose size if to be described.
    * @return a short-hand string description of the size in bytes.
    */
   public static String getElapsedTimeSpecificationDescription(long sizeInBytes)
   {
      return getElapsedTimeSpecificationDescription(sizeInBytes, "#.##");
   }
}

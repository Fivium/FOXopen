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
package net.foxopen.fox.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A handly utility class that contains some well-used sizes and formatting
 * helpers.
 *
 * @author  Gary Watson
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

/*
MIT License

Copyright (c) 2024 Alex Høffner

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package utils;

import java.util.Date;
import messages.Messages;
import java.time.Instant;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;


public class Dates
{
   private static String DAY = "yyyy-MM-dd";
   private static String DTM = "yyyy-MM-dd HH:mm:ss";

   private static ZoneOffset UTC = ZoneOffset.UTC;
   private static DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;


   public static String toString(Date date)
   {
      if (date == null) return(null);
      Instant time = date.toInstant();
      ZonedDateTime utc = ZonedDateTime.ofInstant(time,UTC);
      return(utc.format(formatter));
   }


   public static String toString(Timestamp sqldate)
   {
      if (sqldate == null) return(null);
      Date date = new Date(sqldate.getTime());
      Instant time = date.toInstant();
      ZonedDateTime utc = ZonedDateTime.ofInstant(time,UTC);
      return(utc.format(formatter));
   }


   public static String toString(java.sql.Date sqldate)
   {
      if (sqldate == null) return(null);
      Date date = new Date(sqldate.getTime());
      Instant time = date.toInstant();
      ZonedDateTime utc = ZonedDateTime.ofInstant(time,UTC);
      return(utc.format(formatter));
   }


   public static String toString(Object value) throws Exception
   {
      if (value == null)
         return(null);

      if (value instanceof java.sql.Date)
         return(Dates.toString((java.sql.Date) value));

      if (value instanceof Date)
        return(Dates.toString((Date) value));

      if (value instanceof Timestamp)
        return(Dates.toString((Timestamp) value));

      throw new Exception(Messages.get("CANNOT_CONVERT_DATE",value.getClass().getSimpleName()));
   }


   public static Date toDate(Object value) throws Exception
   {
      SimpleDateFormat fmt = null;

      if (value == null)
         return(null);

      if (value instanceof Date)
         return((Date) value);

      if (value instanceof java.sql.Date)
         return(new Date(((java.sql.Date) value).getTime()));

      if (!(value instanceof String))
         throw new Exception(Messages.get("CANNOT_CONVERT_DATE",value.getClass().getSimpleName()));

      String date = value.toString();

      if (date.length() > 20)
      {
         ZonedDateTime zdt = ZonedDateTime.parse(date);
         return(Date.from(zdt.toInstant()));
      }

      date = guess(date);

      // Assume datetime
      fmt = new SimpleDateFormat(DTM);

      // Only date no time
      if (date.length() == 10)
         fmt = new SimpleDateFormat(DAY);

      return(fmt.parse(date));
   }


   private static String guess(String date)
   {
      int pos = 0;

      date = date.trim();
      date = date.replaceAll("  ","");

      byte[] bstr = date.getBytes();

      String t1 = token(pos,bstr);
      pos += t1.length() + 1;

      String t2 = token(pos,bstr);
      pos += t2.length() + 1;

      String t3 = token(pos,bstr);
      pos += t3.length();

      String format = null;

      if (pos < date.length() && date.charAt(pos) == 'T')
         date = date.substring(0,pos)+" "+date.substring(pos+1);

      if (t1.length() == 4) format = t1+"-"+t2+"-"+t3+date.substring(pos);
      else if (t3.length() == 4) format = t3+"-"+t2+"-"+t1+date.substring(pos);

      return(format);
   }


   private static String token(int pos, byte[] bstr)
   {
      int ts = pos;

      while(pos < bstr.length)
      {
         if(bstr[pos] < '0' || bstr[pos] > '9')
            break;

         pos++;
      }

      return(new String(bstr,ts,pos-ts));
   }
}
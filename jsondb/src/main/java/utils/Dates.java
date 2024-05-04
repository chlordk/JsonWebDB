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
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class Dates
{
   public static String DAY = "yyyy-MM-dd";
   public static String DTM = "yyyy-MM-dd HH:mm:ss";
   public static String JSF = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

   private static ZoneOffset UTC = ZoneOffset.UTC;
   private static DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;


   public static String shouldbe = "2024-05-04T08:45:31.254Z";

   public static void main(String[] args) throws Exception
   {
      Date date = new Date();
      System.out.println(date.toInstant());

      String conv = convert(date);
      System.out.println(conv);

      date = convert(conv);
      System.out.println(date);
   }

   public static String convert(Date date)
   {
      Instant time = date.toInstant();
      ZonedDateTime utc = ZonedDateTime.ofInstant(time,UTC);
      return(utc.format(formatter));
   }


   public static Date convert(String dstr) throws Exception
   {
      SimpleDateFormat fmt = null;

      if (dstr.length() > 20)
      {
         LocalDateTime ldt = LocalDateTime.parse(dstr,formatter);
         ZonedDateTime utc = ldt.atZone(ZoneId.systemDefault());
         return(Date.from(utc.toInstant()));
      }

      dstr = format(dstr);

      if (dstr.length() == 10)
      {
         fmt = new SimpleDateFormat(DAY);
         return(fmt.parse(dstr));
      }

      fmt = new SimpleDateFormat(DTM);
      return(fmt.parse(dstr));
   }

   private static String format(String dstr)
   {
      int ts = 0;
      int pos = 0;

      dstr = dstr.trim();
      dstr = dstr.replaceAll("  ","");

      byte[] bstr = dstr.getBytes();

      while(bstr[pos] < '0' || bstr[pos] > '9')
         pos++;

      ts = pos;

      while(pos < bstr.length)
      {
         if(bstr[pos] < '0' || bstr[pos] > '9')
            break;

         pos++;
      }

      String t1 = new String(bstr,ts,pos-ts);

      while(bstr[pos] < '0' || bstr[pos] > '9')
         pos++;

      ts = pos;

      while(pos < bstr.length)
      {
         if(bstr[pos] < '0' || bstr[pos] > '9')
            break;

         pos++;
      }

      String t2 = new String(bstr,ts,pos-ts);

      while(bstr[pos] < '0' || bstr[pos] > '9')
         pos++;

      ts = pos;

      while(pos < bstr.length)
      {
         if(bstr[pos] < '0' || bstr[pos] > '9')
            break;

         pos++;
      }

      String t3 = new String(bstr,ts,pos-ts);

      String format = null;

      if (t1.length() == 4) format = t1+"-"+t2+"-"+t3+dstr.substring(pos);
      else if (t3.length() == 4) format = t3+"-"+t2+"-"+t1+dstr.substring(pos);

      return(format);
   }
}

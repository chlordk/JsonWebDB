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

package jsondb.messages;

import java.io.File;
import jsondb.Config;
import java.util.Locale;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.PropertyResourceBundle;


public class Messages
{
   private static final String folder = "messages";
   private static final String bundle = "Messages";
   private static final PropertyResourceBundle messages = load();


   public static String get(String name, Object ...args) throws Exception
   {
      if (messages == null)
         return("No message files were found. Language: "+Locale.getDefault().getLanguage());

      else return(messages.getString(name)+concat(args));
   }

   private static String concat(Object ...args) throws Exception
   {
      String str = "";

      for (int i = 0; i < args.length; i++)
         str += " " + toString(args[i]);

      return(str);
   }

   private static String toString(Object obj)
   {
      return(obj+"");
   }

   @SuppressWarnings("unused")
   private static String toString(Exception e) throws Exception
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      e.printStackTrace(new PrintStream(out));
      out.close();
      return(new String(out.toByteArray()));
   }

   private static PropertyResourceBundle load()
   {
      PropertyResourceBundle messages = null;

      try
      {
         String path = Config.path(folder,bundle);
         String lang = Locale.getDefault().getLanguage();

         lang = lang.toUpperCase();
         File location = new File(path+lang+".msg");
         if (!location.exists()) location = new File(path+"EN.msg");

         messages = new PropertyResourceBundle(new FileInputStream(location));
      }
      catch (Exception e)
      {
         e.getStackTrace();
      }

      return(messages);
   }
}

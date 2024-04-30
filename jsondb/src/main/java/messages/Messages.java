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

package messages;

import java.io.File;
import jsondb.Config;
import java.util.Locale;
import java.io.PrintStream;
import java.util.logging.Level;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.PropertyResourceBundle;


public class Messages
{
   private static final String folder = "messages";
   private static final String bundle = "Messages";
   private static final PropertyResourceBundle messages = load();


   public static String flatten(Object[] args)
   {
      String list = null;

      for (int i = 0; i < args.length; i++)
      {
         if (i == 0) list = ""+args[i];
         else list += ", "+args[i];
      }

      return(list);
   }


   public static String get(String name, Object ...args)
   {
      if (messages == null)
      {
         Config.logger().severe("No message files were found");
         return("No message files were found");
      }

      try
      {
         return(format(messages.getString(name),args));
      }
      catch (Exception e)
      {
         Config.logger().log(Level.SEVERE,name,e);
         return("No message found for "+name);
      }
   }

   private static String format(String msg, Object ...args) throws Exception
   {
      for (int i = 0; i < args.length; i++)
      {
         if (args[i] instanceof Exception)
            args[i] = toString((Exception) args[i]);

         msg = msg.replace("{%"+(i+1+"}"),args[i]+"");
      }

      return(msg);
   }

   private static String toString(Exception e) throws Exception
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      e.printStackTrace(new PrintStream(out)); out.close();
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
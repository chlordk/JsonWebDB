/*
  MIT License

  Copyright © 2023 Alex Høffner

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the “Software”), to deal in the Software without
  restriction, including without limitation the rights to use, copy, modify, merge, publish,
  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or
  substantial portions of the Software.

  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package jsondb.logger;

import java.io.File;
import jsondb.Config;
import org.json.JSONObject;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import java.util.logging.Handler;
import java.util.logging.FileHandler;

public class Applogger
{
   private static final boolean REDIRECT = false;

   private static final String SIZE = "size";
   private static final String LEVEL = "level";
   private static final String FILES = "files";

   private static final String LOGFLD = "logs";
   private static final String LOGDEF = "logger";
   private static final String LOGFILE = "server.log";


   public static Logger setup() throws Exception
   {
      String inst = Config.inst();
      JSONObject logd = Config.get(LOGDEF);

      int files = Config.get(logd,FILES);
      int fsize = logsize((String) Config.get(logd,SIZE));

      String levl = Config.get(logd,LEVEL);
      String path = Config.path(LOGFLD,inst);

      Level level = Level.parse(levl.toUpperCase());

      File file = new File(path);
      if (!file.exists()) file.mkdirs();

      path += File.separator + LOGFILE;
      file = new File(path+".0"); // Logger appends .0 ...

      if (file.exists())
      {
         FileOutputStream out = new FileOutputStream(file,true);
         for (int i = 0; i < 3; i++) out.write(System.lineSeparator().getBytes());
         out.close();
      }

      FileHandler handler = new FileHandler(path,fsize,files,true);

      handler.setFormatter(new Formatter());
      Logger logger = Logger.getLogger("JsonWebDB");

      logger.setLevel(level);
      logger.addHandler(handler);
      logger.setUseParentHandlers(false);

      Logger root = Logger.getLogger("");

      for (Handler hdl : root.getHandlers())
         root.removeHandler(hdl);

      root.setLevel(level);
      root.addHandler(handler);
      root.setUseParentHandlers(false);

      if (REDIRECT)
      {
         PrintStream out = new PrintStream(new FileOutputStream(file,true));
         System.setOut(out);
         System.setErr(out);
      }

      return(logger);
   }

   private static int logsize(String def)
   {
      int mp = 1;
      def = def.trim();
      if (def.endsWith("KB")) mp = 1024;
      if (def.endsWith("MB")) mp = 1024*1024;
      if (def.endsWith("GB")) mp = 1024*1024*1024;
      if (mp > 1) def = def.substring(0,def.length()-2);
      return(Integer.parseInt(def.trim()) * mp);
   }
}

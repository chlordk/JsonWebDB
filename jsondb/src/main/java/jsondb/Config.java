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

package jsondb;

import java.io.File;
import org.json.JSONObject;
import org.json.JSONTokener;
import jsondb.files.FileConfig;
import jsondb.logger.Applogger;
import java.io.FileInputStream;
import java.util.logging.Logger;


public class Config
{
   private static final String PATH = "path";
   private static final String CONF = "config";
   private static final String APPL = "application";

   private final String inst;
   private final String appl;

   private final Logger logger;
   private final JSONObject config;
   private final FileConfig fconfig;

   private static String root = null;
   private static Config instance = null;

   public static void main(String[] args) throws Exception
   {
      Config.load(args[0],args[1]);
   }

   public static synchronized Config load(String root, String inst) throws Exception
   {
      if (instance != null)
         return(instance);

      Config.root = root;
      String path = path(CONF,"config.json");
      FileInputStream in = new FileInputStream(path);
      instance = new Config(inst,new JSONObject(new JSONTokener(in)));
      return(instance);
   }

   private Config(String inst, JSONObject config) throws Exception
   {
      this.inst = inst;
      this.config = config;
      this.appl = get(get(APPL),PATH);
      this.logger = Applogger.setup(this);
      this.fconfig = FileConfig.load(this);
   }

   public String inst()
   {
      return(inst);
   }

   public String appl()
   {
      return(appl);
   }

   public Logger logger()
   {
      return(logger);
   }

   public JSONObject get()
   {
      return(config);
   }

   public String getMimeType(String file)
   {
      return(FileConfig.getMimeType(file));
   }

   @SuppressWarnings({ "unchecked" })
   public <T> T get(String section)
   {
      return((T) config.get(section));
   }

   @SuppressWarnings({ "unchecked" })
   public <T> T get(JSONObject section, String attr)
   {
      return((T) section.get(attr));
   }

   public static String path(String... parts)
   {
      String path = root;

      if (parts != null)
      {
         for (int i = 0; i < parts.length; i++)
            path += File.separator + parts[i];
      }

      return(path);
   }
}
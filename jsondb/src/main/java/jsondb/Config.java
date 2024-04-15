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
import jsondb.messages.Messages;
import jsondb.database.JsonDBPool;


public class Config
{
   private static final String PATH = "path";
   private static final String CONF = "config";
   private static final String SESS = "session";
   private static final String STTL = "timeout";
   private static final String FILE = "config.json";
   private static final String APPL = "application";

   private final int sttl;
   private final String inst;
   private final String appl;

   private final Logger logger;
   private final JSONObject config;

   private static String root = null;
   private static Config instance = null;
   private static JsonDBPool pool = null;
   private static FileConfig fconfig = null;

   /**
    *
    * @param root the root directory of JsonWebDB
    * @param inst the instance name. Most often more servers will run
    * @return The parsed config with some helper methods to navigate the config.
    * @throws Exception
    */
    public static synchronized Config load(String root, String inst) throws Exception
    {
      return(load(root,inst,FILE));
    }


   /**
    *
    * @param root the root directory of JsonWebDB
    * @param inst the instance name. Most often more servers will run
    * @param file the configuration file
    * @return The parsed config with some helper methods to navigate the config.
    * @throws Exception
    */
   public static synchronized Config load(String root, String inst, String file) throws Exception
   {
      if (instance != null)
         return(instance);

      if (inst.contains(":"))
         throw new Exception(Messages.get("ILLEGALE_INSTANCE_NAME",inst));

      Config.root = root;
      inst = inst.toLowerCase();
      String path = path(CONF,file);
      FileInputStream in = new FileInputStream(path);
      instance = new Config(inst,new JSONObject(new JSONTokener(in)));
      in.close(); return(instance);
   }

   private Config(String inst, JSONObject config) throws Exception
   {
      this.inst = inst;
      this.config = config;
      this.appl = get(get(APPL),PATH);
      this.sttl = get(get(SESS),STTL);
      this.logger = Applogger.setup(this);
      Config.fconfig = FileConfig.load(this);
   }

   /** The instance name */
   public String inst()
   {
      return(inst);
   }

   /** Path to application */
   public String appl()
   {
      return(appl);
   }

   /** Session timeout (or TimeToLive) */
   public int sesttl()
   {
      return(sttl);
   }

   /** The logger */
   public Logger logger()
   {
      return(logger);
   }

   /** The config json */
   public JSONObject get()
   {
      return(config);
   }

   /** Get the pool */
   public static JsonDBPool pool()
   {
      return(pool);
   }

   /** Inject the pool */
   public static void pool(JsonDBPool pool)
   {
      Config.pool = pool;
   }

   /** The FileConfig configuration */
   public static FileConfig files()
   {
      return(fconfig);
   }

   /** Get the mimetype from file(name) */
   public String getMimeType(String file)
   {
      return(FileConfig.getMimeType(file));
   }

   /** Get an object from the config json */
   @SuppressWarnings({ "unchecked" })
   public <T> T get(String section)
   {
      return((T) config.get(section));
   }

   /** Get an object from a json */
   @SuppressWarnings({ "unchecked" })
   public <T> T get(JSONObject section, String attr)
   {
      return((T) section.get(attr));
   }

   /** Get path to a given file or folder */
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
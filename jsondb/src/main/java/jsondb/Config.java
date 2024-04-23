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

import http.Options;
import http.Cluster;
import java.io.File;
import org.json.JSONObject;
import org.json.JSONTokener;
import jsondb.files.FileConfig;
import jsondb.logger.Applogger;
import java.io.FileInputStream;
import java.util.logging.Logger;
import jsondb.state.StateHandler;
import database.definitions.AdvancedPool;


public class Config
{
   private static final String CONF = "config";
   private static final String POOL = "enabled";
   private static final String SESS = "session";
   private static final String PATH = "location";
   private static final String DBSC = "database";
   private static final String FILE = "config.json";
   private static final String APPL = "application";

   private static final String SESTMOUT = "session-timeout";
   private static final String CONTMOUT = "connection-timeout";
   private static final String TRXTMOUT = "transaction-timeout";

   private static int sestmout = 0;
   private static int trxtmout = 0;
   private static int contmout = 0;

   private static String inst = null;
   private static String appl = null;

   private static Logger logger = null;
   private static JSONObject config = null;

   private static String root = null;
   private static AdvancedPool pool = null;


   /**
    *
    * @param root the root directory of JsonWebDB
    * @param inst the instance name.
    * @return The parsed config with some helper methods to navigate the config.
    * @throws Exception
    */
    protected static synchronized void load(String root, String inst) throws Exception
    {
      load(root,inst,FILE,true);
    }


   /**
    *
    * @param root the root directory of JsonWebDB
    * @param inst the instance name
    * @param file the configuration file name
    * @param logall Also handle messages written to the root logger
    * @return The parsed config with some helper methods to navigate the config
    * @throws Exception
    */
   protected static synchronized void load(String root, String inst, String file, boolean logall) throws Exception
   {
      if (file == null)
         file = FILE;

      if (Config.root != null)
         return;

      if (file.indexOf('.') < 0)
         file += ".json";

      Config.root = root;
      inst = inst.toLowerCase();
      String path = path(CONF,file);

      FileInputStream in = new FileInputStream(path);
      JSONTokener tokener = new JSONTokener(in);
      JSONObject config = new JSONObject(tokener);
      in.close();

      Config.inst = inst;
      Config.config = config;

      Config.appl = get(get(APPL),PATH);
      Config.sestmout = get(get(SESS),SESTMOUT);
      Config.trxtmout = get(get(SESS),TRXTMOUT);
      Config.contmout = get(get(SESS),CONTMOUT);

      Config.logger = Applogger.setup(logall);

      JSONObject dbsc = config.getJSONObject(DBSC);

      if (dbsc.getBoolean(POOL))
         Config.pool = new database.AdvancedPool(dbsc);
   }

   public static void initialize() throws Exception
   {
      Admins.initialize();
      Cluster.initialize();
      Options.initialize();
      Sources.initialize();
      FileConfig.initialize();
      StateHandler.initialize();

      Monitor.monitor();
   }

   /** The instance name */
   public static String inst()
   {
      return(inst);
   }

   /** Path to application */
   public static String appl()
   {
      return(appl);
   }

   /** Connection idle timeout */
   public static int conTimeout()
   {
      return(contmout);
   }

   /** Session timeout */
   public static int sesTimeout()
   {
      return(sestmout);
   }

   /** Transaction timeout */
   public static int trxTimeout()
   {
      return(trxtmout);
   }

   /** The logger */
   public static Logger logger()
   {
      return(logger);
   }

   /** The config json */
   public static JSONObject get()
   {
      return(config);
   }

   /** Get the pool */
   public static AdvancedPool pool()
   {
      return(pool);
   }

   /** Inject the pool */
   protected static void pool(AdvancedPool pool)
   {
      Config.pool = pool;
   }

   /** Get the mimetype from file(name) */
   public static String getMimeType(String file)
   {
      return(FileConfig.getMimeType(file));
   }

   /** Get an object from the config json */
   @SuppressWarnings({ "unchecked" })
   public static <T> T get(String section)
   {
      return((T) config.get(section));
   }

   /** Get an object from a json */
   @SuppressWarnings({ "unchecked" })
   public static <T> T get(JSONObject section, String attr)
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
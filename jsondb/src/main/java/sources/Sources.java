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

package sources;

import utils.Misc;
import http.Server;
import java.io.File;
import jsondb.Config;
import java.util.Date;
import java.util.HashMap;
import org.json.JSONArray;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import java.nio.file.WatchKey;
import java.util.logging.Level;
import java.io.FileInputStream;
import java.nio.file.WatchEvent;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import static java.nio.file.StandardWatchEventKinds.*;


public class Sources extends Thread
{
   private static final String TYPE = "type";
   private static final String CONF = "config";
   private static final String DBSC = "database";
   private static final String SOURCES = "sources";
   private static final String TABLES = "datasources";
   private static final String TRIGGER = ".reload.trg";

   private static Path path = null;
   private static WatchKey watcher = null;
   private static WatchService service = null;

   private static HashMap<String,Source> sources =
      new HashMap<String,Source>();

   private static HashMap<String,Source> preload =
      new HashMap<String,Source>();

   public static void main(String[] args) throws Exception
   {
      if (args.length == 0)
         syntax();

      if (args[0].equals("reload"))
      {
         String path = Misc.url(Server.findAppHome(),CONF,SOURCES);
         File trigger = new File(path,TRIGGER);

         if (!trigger.exists()) trigger.createNewFile();
         else trigger.setLastModified((new Date()).getTime());
      }

      else

      if (args[0].equals("list"))
      {
         System.out.println("Not implemented yet");
      }
   }

   private static void syntax()
   {
      System.out.println();
      System.out.println();
      System.out.println("usage: sources reload|list");
      System.out.println();
      System.exit(-1);
   }


   public static void initialize() throws Exception
   {
      path = Paths.get(Config.path(CONF,SOURCES));

      File trigger = new File(path.toFile(),TRIGGER);
      if (!trigger.exists()) trigger.createNewFile();

      service = FileSystems.getDefault().newWatchService();
      watcher = path.register(service,ENTRY_CREATE,ENTRY_MODIFY,ENTRY_DELETE);

      Sources instance = new Sources();

      JSONObject db = Config.get(DBSC);
      String dbtype = Config.get(db,TYPE);

      dbtype = dbtype.toLowerCase();
      File root = new File(Misc.url(path.toString(),dbtype));

      Config.logger().info("Load source definitions");
      preload = instance.loadStatic(root);
      sources = instance.loadSources(root);
      Config.logger().info("Source definitions loaded");

      instance.start();
   }

   @SuppressWarnings("unchecked")
   public static <T extends Source> T get(String id)
   {
      id = id.toLowerCase();
      Source source = sources.get(id);
      if (source == null) source = preload.get(id);
      return((T) source);
   }

   private Sources()
   {
      this.setDaemon(true);
      this.setName(this.getClass().getSimpleName());
   }


   @Override
   public void run()
   {
      WatchKey key;

      while (true)
      {
         try
         {
            while ((key = service.take()) != null)
            {
               boolean reload = false;

               for (WatchEvent<?> event : key.pollEvents())
               {
                  if (event.context().toString().equals(TRIGGER))
                     reload = true;
               }

               key.reset();

               if (reload)
               {
                  Config.logger().info("Reload source definitions");
                  sources = loadSources(path.toFile());
                  Config.logger().info("Source definitions reloaded");
               }
            }
         }
         catch (Throwable t)
         {
            Config.logger().log(Level.SEVERE,t.toString(),t);
            watcher.reset();
         }
      }
   }

   private HashMap<String,Source> loadSources(File root) throws Exception
   {
      HashMap<String,Source> sources = new HashMap<String,Source>();

      for(File file : root.listFiles())
      {
         if (file.isDirectory())
         {
            sources.putAll(loadSources(file));
            continue;
         }

         if (file.getName().endsWith(".json"))
         {
            FileInputStream in = new FileInputStream(file);
            String content = new String(in.readAllBytes()); in.close();
            JSONObject defs = new JSONObject(content);
            sources.putAll(load(defs));
         }
      }

      return(sources);
   }

   private HashMap<String,Source> loadStatic(File root) throws Exception
   {
      HashMap<String,Source> sources = new HashMap<String,Source>();

      for(File file : root.getParentFile().listFiles())
      {
         if (file.isDirectory())
            continue;

         if (file.getName().endsWith(".json"))
         {
            FileInputStream in = new FileInputStream(file);
            String content = new String(in.readAllBytes()); in.close();
            JSONObject defs = new JSONObject(content);
            sources.putAll(load(defs));
         }
      }

      return(sources);
   }

   private HashMap<String,Source> load(JSONObject defs) throws Exception
   {
      JSONArray arr = null;
      HashMap<String,Source> sources = new HashMap<String,Source>();

      arr = defs.optJSONArray(TABLES);

      for (int i = 0; i < arr.length(); i++)
      {
         TableSource source = new TableSource(arr.getJSONObject(i));
         sources.put(source.id.toLowerCase(),source);
         Config.logger().info(source.toString());
      }

      return(sources);
   }
}
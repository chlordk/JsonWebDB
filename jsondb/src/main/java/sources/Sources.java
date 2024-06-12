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
import database.SQLTypes;
import org.json.JSONArray;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.nio.file.WatchKey;
import java.util.logging.Level;
import java.io.FileInputStream;
import java.util.logging.Logger;
import java.nio.file.WatchEvent;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import database.implementations.DatabaseType;
import static java.nio.file.StandardWatchEventKinds.*;


public class Sources extends Thread
{
   private static final String SQL = "sql";
   private static final String TYPE = "type";
   private static final String CONF = "config";
   private static final String DBSC = "database";
   private static final String FUNC = "function";
   private static final String PROC = "procedure";
   private static final String SOURCES = "sources";
   private static final String ENTRIES = "datasources";
   private static final String TRIGGER = ".reload.trg";

   private static Path path = null;
   private static WatchKey watcher = null;
   private static WatchService service = null;

   private static HashMap<String,Source> sources =
      new HashMap<String,Source>();

   public static void main(String[] args) throws Exception
   {
      if (args.length == 0) syntax();
      String path = Misc.url(Server.findAppHome(),CONF,SOURCES);

      if (args[0].equals("reload"))
      {
         File trigger = new File(path,TRIGGER);

         if (!trigger.exists()) trigger.createNewFile();
         else trigger.setLastModified((new Date()).getTime());
      }

      else

      if (args[0].equals("list"))
      {
         SQLTypes.initialize();

         Logger logger = Logger.getLogger("JsonWebDB");
         logger.setUseParentHandlers(false); Config.logger(logger);

         File root = new File(path);
         Sources instance = new Sources();

         for(DatabaseType type :  DatabaseType.values())
         {
            ArrayList<Source> allsrc = new ArrayList<Source>();
            File folder = new File(root,type.name().toLowerCase());
            HashMap<String,Source> sources = instance.loadSources(folder);

            if (sources.size() == 0) continue;
            allsrc.addAll(sources.values()); allsrc.sort(new SourceSort());

            System.out.println(folder.toString());
            for(Source src : allsrc) System.out.println(src.toString());
         }
      }
   }

   private static void syntax()
   {
      System.out.println();
      System.out.println();
      System.out.println("usage: sources reload|list <database-folder>");
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
      sources = instance.loadSources(root);
      Config.logger().info("Source definitions loaded");

      instance.start();
   }

   @SuppressWarnings("unchecked")
   public static <T extends Source> T get(String id)
   {
      id = id.toLowerCase();
      Source source = sources.get(id);
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

      if (!root.exists())
         return(sources);

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


   private HashMap<String,Source> load(JSONObject defs) throws Exception
   {
      JSONArray arr = null;
      HashMap<String,Source> sources = new HashMap<String,Source>();

      arr = defs.getJSONArray(ENTRIES);

      for (int i = 0; i < arr.length(); i++)
      {
         JSONObject def = arr.getJSONObject(i);

         if (def.has(SQL))
         {
            SQLSource source = new SQLSource(def);
            sources.put(source.id().toLowerCase(),source);
            Config.logger().info(source.toString());
         }
         else if (def.has(FUNC))
         {
            ProcedureSource source = new ProcedureSource(def,true);
            sources.put(source.id().toLowerCase(),source);
            Config.logger().info(source.toString());
         }
         else if (def.has(PROC))
         {
            ProcedureSource source = new ProcedureSource(def,false);
            sources.put(source.id().toLowerCase(),source);
            Config.logger().info(source.toString());
         }
         else
         {
            TableSource source = new TableSource(def);
            sources.put(source.id().toLowerCase(),source);
            Config.logger().info(source.toString());
         }
      }

      return(sources);
   }


   private static class SourceSort implements Comparator<Source>
   {
      @Override
      public int compare(Source o1, Source o2)
      {
         return(o1.toString().compareTo(o2.toString()));
      }
   }
}
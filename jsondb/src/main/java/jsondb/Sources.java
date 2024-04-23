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

import java.util.HashMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import java.nio.file.WatchKey;
import java.util.logging.Level;
import java.nio.file.WatchEvent;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.FileInputStream;


public class Sources extends Thread
{
   private static final String TYPE = "type";
   private static final String CONF = "config";
   private static final String DBSC = "database";
   private static final String SOURCES = "sources";
   private static final String TRIGGER = "reload.trg";

   private static Path path = null;
   private static WatchKey watcher = null;
   private static WatchService service = null;

   private static HashMap<String,Source> sources =
      new HashMap<String,Source>();


   public static void initialize() throws Exception
   {
      path = Paths.get(path());
      service = FileSystems.getDefault().newWatchService();
      watcher = path.register(service,ENTRY_CREATE,ENTRY_MODIFY,ENTRY_DELETE);

      Sources sources = new Sources();
      sources.start();
   }

   public static Source get(String id)
   {
      return(sources.get(id.toLowerCase()));
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
                  sources = load(path.toFile());
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

   private HashMap<String,Source> load(File root) throws Exception
   {
      HashMap<String,Source> sources = new HashMap<String,Source>();

      for(File file : root.listFiles())
      {
         if (file.isDirectory())
         {
            sources.putAll(load(file));
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
      HashMap<String,Source> sources = new HashMap<String,Source>();
      return(sources);
   }

   private static String path()
   {
      JSONObject db = Config.get(DBSC);
      String dbtype = Config.get(db,TYPE);
      return(Config.path(CONF,SOURCES,dbtype.toLowerCase()));
   }
}

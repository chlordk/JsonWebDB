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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import java.nio.file.WatchKey;
import java.util.logging.Level;
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

   private static Path path = null;
   private static WatchKey watcher = null;
   private static WatchService service = null;


   public static void initialize() throws Exception
   {
      path = Paths.get(path());
      service = FileSystems.getDefault().newWatchService();
      watcher = path.register(service,ENTRY_CREATE,ENTRY_MODIFY,ENTRY_DELETE);

      Sources sources = new Sources();
      sources.start();
   }

   private Sources()
   {
      this.setDaemon(true);
      this.setName(this.getClass().getSimpleName());
   }

   public void run()
   {
      WatchKey key;

      while (true)
      {
         try
         {
            while ((key = service.take()) != null)
            {
               for (WatchEvent<?> event : key.pollEvents())
               {
                  System.out.println(event);
               }

               key.reset();
            }
         }
         catch (Throwable t)
         {
            Config.logger().log(Level.SEVERE,t.toString(),t);
            watcher.reset();
         }
      }
   }

   private void load() throws Exception
   {

   }

   private static String path()
   {
      JSONObject db = Config.get(DBSC);
      String dbtype = Config.get(db,TYPE);

      String path = Config.path(CONF,SOURCES,dbtype.toLowerCase());
      Config.logger().info("Checking sources at "+path);

      return(path);
   }
}

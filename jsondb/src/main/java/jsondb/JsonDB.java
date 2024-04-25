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

import java.util.Date;
import org.json.JSONObject;
import java.util.logging.Level;
import jsondb.files.FileHandler;
import jsondb.files.FileResponse;
import jsondb.objects.ObjectHandler;
import database.definitions.AdvancedPool;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Public interface to the backend
 */
public class JsonDB
{
   private static String instance = null;

   public static final String version = "4.0.1";
   public static final long started = (new Date()).getTime();

   private static final AtomicInteger dbreqs = new AtomicInteger(0);
   private static final AtomicInteger fireqs = new AtomicInteger(0);

   /**
    * Finalizes setup and starts necessary services.
    * After this, the server is ready to accept requests.
    * @param root the root directory of JsonWebDB
    * @param inst the instance name
    * @throws Exception
    */
    public static void initialize(String root, String inst) throws Exception
    {
      initialize(root,inst,null,true);
    }


   /**
    * Finalizes setup and starts necessary services.
    * After this, the server is ready to accept requests.
    * @param root the root directory of JsonWebDB
    * @param inst the instance name
    * @param file the configuration file name
    * @param logall Also handle messages written to the root logger
    * @throws Exception
    */
   public static void initialize(String root, String inst, String file, boolean logall) throws Exception
   {
      Config.load(root,inst,file,logall);

      Config.logger().info("......................................................");
      Config.logger().info("Starting JsonDB version "+version);
      Config.logger().info("......................................................");

      Config.initialize();
      instance = Config.inst();
   }


   /**
    * Inject custom pool implementation
    * @param pool the custom pool
    */
   public static void setPool(AdvancedPool pool)
   {
      Config.pool(pool);
   }


   /**
    * Get the mimetype for a given filetype.
    * Mimetypes are defined in the config.json file.
    * @param filetype
    * @return the mimetype
    */
   public String mimetype(String filetype)
   {
      return(Config.getMimeType(filetype));
   }

   /**
    *
    * @param path uri
    * @param date gmt formatted date
    * @return if the file was modified
    */
   public boolean modified(String path, String date)
   {
      if (date == null) return(true);
      boolean modified = !date.equals(FileHandler.lastModified(path));

      if (!modified)
      {
         fireqs.incrementAndGet();
         log(path,"Not Modified");
      }

      return(modified);
   }

   /**
    * Get a file from the application
    * @param path
    * @return data and meta-data for the file
    * @throws Exception
    */
   public FileResponse get(String path) throws Exception
   {
      fireqs.incrementAndGet();
      FileResponse response = FileHandler.get(path);
      log(response); return(response);
   }


   /**
    * Executes request
    * @param request (json)
    * @return response
    * @throws Exception
    */
   public Response execute(String request) throws Exception
   {
      return(execute(new JSONObject(request)));
   }


   /**
    * Executes request
    * @param request
    * @return the response
    * @throws Exception
    */
   public Response execute(JSONObject request) throws Exception
   {
      dbreqs.incrementAndGet();
      Response response = ObjectHandler.handle(request);
      response.put("version",version);
      response.put("instance",instance);
      log(request,response);
      return(response);
   }


   public static int getFileRequests()
   {
      return(fireqs.get());
   }


   public static int getJsonRequests()
   {
      return(dbreqs.get());
   }


   private void log(String path, String status)
   {
      Config.logger().info(String.format("%-40s %s",path,status));
   }

   private void log(FileResponse response)
   {
      Config.logger().info(response.toString());
   }

   private void log(JSONObject request, Response response)
   {
      if (response.exception() != null)
      Config.logger().log(Level.WARNING,response.get("message")+"",response.exception());
      Config.logger().info("/jsondb\n\n"+request.toString(2)+"\n\n"+response.toString(2)+"\n");
   }
}
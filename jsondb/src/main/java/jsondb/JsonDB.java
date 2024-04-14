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

import org.json.JSONObject;
import jsondb.files.FileHandler;
import jsondb.files.FileResponse;
import jsondb.state.StateHandler;
import jsondb.database.JsonDBPool;

/**
 * Public interface to the backend
 */
public class JsonDB
{
   public static String version = "4.0.1";

   private static Config config = null;
   private static JsonDBPool pool = null;


   /**
    * Finalizes setup and starts necessary services.
    * After this, the server is ready to accept requests.
    * @throws Exception
    */
   public static void start() throws Exception
   {
      JsonDB.config.logger().info(".......................................");
      JsonDB.config.logger().info("Starting JsonDB version "+version);
      JsonDB.config.logger().info(".......................................");

      StateHandler.handle(config);
   }


   /**
    * Register the database pool
    * @param pool
    */
   public static void register(JsonDBPool pool)
   {
      if (JsonDB.pool == null)
         JsonDB.pool = pool;
   }


   /**
    * Register the configuration
    * @param config
    */
   public static void register(Config config)
   {
      if (JsonDB.config == null)
         JsonDB.config = config;
   }


   /**
    * Get the mimetype for a given filetype.
    * Mimetypes are defined in the config.json file.
    * @param filetype
    * @return mimetype
    */
   public String mimetype(String filetype)
   {
      return(config.getMimeType(filetype));
   }


   /**
    * Get a file from the application
    * @param path
    * @return data and meta-data for the file
    * @throws Exception
    */
   public FileResponse get(String path) throws Exception
   {
      FileHandler handler = new FileHandler();
      FileResponse response = handler.get(path);
      log(response); return(response);
   }


   /**
    * Executes request
    * @param request (json)
    * @return response
    * @throws Exception
    */
   public JSONObject execute(String request) throws Exception
   {
      return(execute(new JSONObject(request)));
   }


   /**
    * Executes request
    * @param request
    * @return response
    * @throws Exception
    */
   public JSONObject execute(JSONObject request) throws Exception
   {
      JSONObject response = new JSONObject();
      response.put("success",true);
      response.put("version",version);
      log(request,response);
      return(response);
   }


   private void log(FileResponse response)
   {
      config.logger().info(response.toString());
   }

   private void log(JSONObject request,JSONObject response)
   {
      config.logger().info("/jsondb\n\n"+request.toString(2)+"\n\n"+response.toString(2)+"\n");
   }
}
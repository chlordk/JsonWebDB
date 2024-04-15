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

import jsondb.database.JsonDBPool;
import jsondb.files.FileHandler;
import jsondb.files.FileResponse;
import jsondb.state.StateHandler;

/**
 * Public interface to the backend
 */
public class JsonDB
{
   public static String version = "4.0.1";


   /**
    * Finalizes setup and starts necessary services.
    * After this, the server is ready to accept requests.
    * @throws Exception
    */
   public static void initialize(String root, String inst) throws Exception
   {
      Config.load(root,inst);
      StateHandler.initialize();

      Config.logger().info(".......................................");
      Config.logger().info("Starting JsonDB version "+version);
      Config.logger().info(".......................................");
   }


   /**
    * Inject custom pool implementation
    * @param pool the custom pool
    */
   public static void setPool(JsonDBPool pool)
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
    * Get a file from the application
    * @param path
    * @return data and meta-data for the file
    * @throws Exception
    */
   public FileResponse get(String path) throws Exception
   {
      FileResponse response = FileHandler.get(path);
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
    * @return the response
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
      Config.logger().info(response.toString());
   }

   private void log(JSONObject request,JSONObject response)
   {
      Config.logger().info("/jsondb\n\n"+request.toString(2)+"\n\n"+response.toString(2)+"\n");
   }
}
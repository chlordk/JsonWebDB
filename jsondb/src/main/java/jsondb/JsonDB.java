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
import jsondb.database.Pool;
import jsondb.files.FileHandler;
import jsondb.files.FileResponse;


public class JsonDB
{
   public static String version = "4.0.1";

   private static Pool pool = null;
   private static Config config = null;


   public static void start() throws Exception
   {
      JsonDB.config.logger().info(".......................................");
      JsonDB.config.logger().info("Starting JsonDB version "+version);
      JsonDB.config.logger().info(".......................................");
   }

   public static void register(Pool pool)
   {
      if (JsonDB.pool == null)
         JsonDB.pool = pool;
   }

   public static void register(Config config)
   {
      if (JsonDB.config == null)
         JsonDB.config = config;
   }

   public String mimetype(String filetype)
   {
      return(config.getMimeType(filetype));
   }

   public FileResponse get(String path) throws Exception
   {
      FileHandler handler = new FileHandler();
      FileResponse response = handler.get(path);
      log(response);
      return(response);
   }

   public JSONObject execute(String request) throws Exception
   {
      return(execute(new JSONObject(request)));
   }

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
      config.logger().info("/jsonwebdb\n\n"+request.toString(2)+"\n\n"+response.toString(2)+"\n");
   }
}
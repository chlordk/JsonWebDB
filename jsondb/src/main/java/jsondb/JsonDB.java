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
import java.io.OutputStream;
import jsondb.files.FileHandler;
import java.io.ByteArrayOutputStream;


public class JsonDB
{
   public static String version = "4.0.1";

   private static Pool pool = null;
   private static Config config = null;

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

   public Response getFile(String path) throws Exception
   {
      return(getFile(path,new ByteArrayOutputStream()));
   }

   public Response getFile(String path, OutputStream out) throws Exception
   {
      FileHandler handler = new FileHandler();
      Response response = handler.load(path,out);
      return(response);
   }

   public JSONObject execute(String request) throws Exception
   {
      return(execute(request,new ByteArrayOutputStream()));
   }

   public JSONObject execute(String request, OutputStream out) throws Exception
   {
      return(execute(new JSONObject(request),out));
   }

   public JSONObject execute(JSONObject request) throws Exception
   {
      return(execute(request,new ByteArrayOutputStream()));
   }

   public JSONObject execute(JSONObject request, OutputStream out) throws Exception
   {
      return(request);
   }
}
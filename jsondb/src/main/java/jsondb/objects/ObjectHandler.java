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

package jsondb.objects;

import java.io.File;
import jsondb.Config;
import org.json.JSONObject;
import java.io.FileInputStream;
import jsondb.messages.Messages;
import java.util.concurrent.ConcurrentHashMap;


public class ObjectHandler
{
   private static final String EXAMPLES = "examples";

   private static final ConcurrentHashMap<String,Class<DatabaseRequest>> classes =
      new ConcurrentHashMap<String,Class<DatabaseRequest>>();

   private static final String location = ObjectHandler.class.getPackage().getName();


   public static void main(String[] args) throws Exception
   {
      Config.load(args[0],args[1]);
      Test(new File(Config.path(EXAMPLES)));
   }


   public static void Test(File repos) throws Exception
   {
      for(File test : repos.listFiles())
      {
         FileInputStream in = new FileInputStream(test);
         String json = new String(in.readAllBytes());
         in.close();

         JSONObject response = handle(new JSONObject(json));
         System.out.println(response);
      }
   }


   public static JSONObject handle(JSONObject request) throws Exception
   {
      String names[] = JSONObject.getNames(request);
      JSONObject payload = request.getJSONObject(names[0]);
      if (names != null && names.length == 1) return(getInstance(names[0],payload).invoke());
      else throw new Exception(Messages.get("UNKNOWN_REQUEST_TYPE","\n",request.toString(2)));
   }


   @SuppressWarnings("unchecked")
   private static DatabaseRequest getInstance(String name, JSONObject definition) throws Exception
   {
      String cname = location+"."+name;
      Class<DatabaseRequest> clazz = classes.get(cname);

      if (clazz == null)
      {
         clazz = (Class<DatabaseRequest>) Class.forName(cname);
         classes.put(cname,clazz);
      }

      return(clazz.getConstructor(JSONObject.class).newInstance(definition));
   }
}

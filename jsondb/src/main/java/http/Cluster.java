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

package http;

import jsondb.Config;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;


public class Cluster
{
   private static final String URL = "url";
   private static final String CLUS = "cluster";
   private static final String INST = "instance";
   private static final String APPL = "application";


   private static HashMap<String,String> addresses =
      new HashMap<String,String>();


   public static void initialize()
   {
      JSONObject ap = Config.get(APPL);
      JSONArray arr = Config.get(ap,CLUS);

      for (int i = 0; i < arr.length(); i++)
      {
         JSONObject def = arr.getJSONObject(i);

         String addr = def.getString(URL);
         String inst = def.getString(INST);

         addresses.put(inst.toLowerCase(),addr);
      }
   }


   public static String getServer()
   {
      return(addresses.get(Config.inst()));
   }


   public static String getServer(String inst)
   {
      return(addresses.get(inst.toLowerCase()));
   }
}
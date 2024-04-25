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

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ConcurrentHashMap;


public class Admins
{
   private static final String USER = "entity";
   private static final String SIGN = "signature";
   private static final String PRIV = "super-users";

   private static final ConcurrentHashMap<String,String> admins =
      new  ConcurrentHashMap<String,String>();

   private static final ConcurrentHashMap<String,String> signatures =
      new  ConcurrentHashMap<String,String>();


   public static String getAdmin(String signature)
   {
      if (signature == null) return(null);
      return(signatures.get(signature));
   }


   public static String getSignature(String entity)
   {
      if (entity == null) return(null);
      return(admins.get(entity.toLowerCase()));
   }


   public static void initialize()
   {
      String entity = null;
      String signature = null;

      JSONObject def = null;
      JSONArray arr = Config.get(PRIV);

      for (int i = 0; i < arr.length(); i++)
      {
         def = arr.getJSONObject(i);
         entity = def.getString(USER);
         signature = def.getString(SIGN);

         entity = entity.toLowerCase();
         admins.put(entity,signature);
         signatures.put(signature,entity);
      }
   }
}
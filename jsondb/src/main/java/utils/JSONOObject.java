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

package utils;

import java.util.Set;
import java.util.Map.Entry;
import org.json.JSONObject;
import org.json.JSONException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;


public class JSONOObject extends org.json.JSONObject
{
   public JSONOObject()
   {
      super();
   }

   public JSONOObject(String json)
   {
      super(json);
   }

   @Override
   public JSONOObject put(String key, Object value) throws JSONException
   {
      try
      {
         Field map = JSONObject.class.getDeclaredField("map");
         map.setAccessible(true);

         Object mapval = map.get(this);

         if (!(mapval instanceof LinkedHashMap))
            map.set(this,new LinkedHashMap<>());
      }
      catch (NoSuchFieldException | IllegalAccessException e)
      {
         throw new RuntimeException(e);
      }

      return((JSONOObject) super.put(key, value));
   }

   @SuppressWarnings("unchecked")
   public JSONOObject put(int pos, String key, Object value) throws JSONException
   {
      try
      {
         Field map = JSONObject.class.getDeclaredField("map");
         map.setAccessible(true);

         LinkedHashMap<String,Object> mapval = (LinkedHashMap<String,Object>) map.get(this);
         LinkedHashMap<String,Object> cloned = (LinkedHashMap<String,Object>) mapval.clone();

         mapval.clear();
         Set<Entry<String,Object>> entries = cloned.entrySet();

         int p = 0;
         for(Entry<String,Object> entry : entries)
         {
            if (p++ == pos) mapval.put(key,value);
            mapval.put(entry.getKey(),entry.getValue());
         }

         return(this);
      }
      catch (NoSuchFieldException | IllegalAccessException e)
      {
         throw new RuntimeException(e);
      }
   }
}
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

package jsondb.sources;

import org.json.JSONArray;
import org.json.JSONObject;

import jsondb.messages.Messages;


public class Source
{
   public static String getString(JSONObject def, String attr) throws Exception
   {
      return(getString(def,attr,false,false));
   }

   public static String getString(JSONObject def, String attr, boolean mandatory) throws Exception
   {
      return(getString(def,attr,mandatory,false));
   }


   public static String[] getStringArray(JSONObject def, String attr) throws Exception
   {
      return(getStringArray(def,attr,false,false));
   }


   public static String[] getStringArray(JSONObject def, String attr, boolean mandatory) throws Exception
   {
      return(getStringArray(def,attr,mandatory,false));
   }

   public static String getString(JSONObject def, String attr, boolean mandatory, boolean lower) throws Exception
   {
      String value = null;

      if (!def.has(attr))
      {
         if (mandatory) throwNotExist(def,attr);
         return(null);
      }

      Object object = def.get(attr);

      if (object instanceof JSONArray)
      {
         JSONArray arr = (JSONArray) object;

         for (int i = 0; i < arr.length(); i++)
         {
            if (i == 0) value = ""; else value += " ";
            value += arr.getString(i);
         }
      }
      else
         value = ""+object;

      if (lower)
         value = value.toLowerCase();

      return(value);
   }

   public static String[] getStringArray(JSONObject def, String attr, boolean mandatory, boolean lower) throws Exception
   {
      String[] value = null;

      if (!def.has(attr))
      {
         if (mandatory) throwNotExist(def,attr);
         return(null);
      }

      Object object = def.get(attr);

      if (object instanceof JSONArray)
      {
         JSONArray arr = (JSONArray) object;
         value = new String[arr.length()];

         for (int i = 0; i < arr.length(); i++)
            value[i] = arr.getString(i);
      }
      else value = new String[] {object+""};

      if (lower)
      {
         for (int i = 0; i < value.length; i++)
            value[i] = value[i].toLowerCase();
      }

      return(value);
   }

   private static void throwNotExist(JSONObject json, String attr) throws Exception
   {
      throw new Exception(Messages.get("MANDATORY_ATTR_MISSING",attr,json.toString(2)));
   }
}